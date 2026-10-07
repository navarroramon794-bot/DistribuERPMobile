package com.distribuerp.mobile.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.LEASE_INTENTO_OUTBOX_MS
import com.distribuerp.mobile.data.local.OutboxDao
import com.distribuerp.mobile.data.local.OutboxOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado de intentar enviar una operacion al backend. */
sealed class ResultadoEnvio {
    /** El backend confirmo la operacion. Solo aqui se elimina. */
    data object Confirmado : ResultadoEnvio()

    /** Fallo transitorio: se reintentara con backoff, no se elimina. */
    data class Retry(val motivo: String) : ResultadoEnvio()

    /** Falta autenticacion HTTP (401/302): no se elimina y no se reintenta con
     * backoff; el reintento lo dispara un nuevo login. */
    data class AuthRequerida(val codigo: Int) : ResultadoEnvio()

    /** Fallo permanente (4xx): no se reintentara, pero se conserva para traza. */
    data class Descartar(val motivo: String) : ResultadoEnvio()
}

fun interface EnviadorOutbox {
    suspend fun enviar(op: OutboxOperation): ResultadoEnvio
}

object RegistroEnvio {

    private val enviados = java.util.concurrent.ConcurrentHashMap<String, EnviadorOutbox>()

    fun registrar(tipo: String, enviador: EnviadorOutbox) {
        enviados[tipo] = enviador
    }

    fun obtener(tipo: String): EnviadorOutbox? = enviados[tipo]

    fun enviados(): Map<String, EnviadorOutbox> = enviados.toMap()

    fun limpiar() = enviados.clear()
}

class SyncGeneralWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Se registra aqui porque este worker es el unico consumidor del
        // registro: resolverlo antes de procesar garantiza que `doWork()` use
        // el mecanismo real y no un mapa inyectado a mano.
        registrarEnviadorVenta(applicationContext)

        val dao = AppDatabase.getInstance(applicationContext).outboxDao()

        // Sin cookie de sesion no se envia nada al backend: las operaciones se
        // conservan y la sincronizacion se retoma tras un nuevo login.
        val autenticado = RetrofitClient.tieneCookies()

        if (procesarPendientes(dao, autenticado = autenticado)) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

/**
 * Procesa las operaciones PENDIENTES de forma individual, una por uuid.
 *
 * Antes de nada recupera las operaciones que quedaron abandonadas en ENVIANDO
 * por una muerte del proceso, de modo que puedan entrar en este mismo ciclo.
 *
 * Devuelve `true` si hubo que pedir reintento. Nunca se elimina una operacion
 * sin confirmacion real del backend.
 *
 * Estados usados (sin inventar nuevos):
 * - `PENDIENTE`: elegible para envio. Un fallo transitorio devuelve la
 *   operacion aqui, de modo que vuelve a entrar en el siguiente sync.
 * - `ENVIANDO`: intento en curso, con un lease en `enviar_despues` que impide
 *   que otra ejecucion lo toque. Si el proceso muere aqui, el lease vence y el
 *   reaper devuelve la operacion a PENDIENTE.
 * - `OK`: confirmada (la operacion se elimina).
 * - `ERROR`: fallo definitivo. Se conserva para traza pero NO se reintenta.
 *
 * El ritmo entre reintentos lo gobierna el backoff de WorkManager, que el
 * worker activa al devolver `Result.retry()`.
 */
suspend fun procesarPendientes(
    dao: OutboxDao,
    registro: Map<String, EnviadorOutbox> = RegistroEnvio.enviados(),
    ahora: Long = System.currentTimeMillis(),
    leaseIntentoMs: Long = LEASE_INTENTO_OUTBOX_MS,
    autenticado: Boolean = true
): Boolean {
    var huboRetry = false

    // Primero las abandonadas: un ENVIANDO con lease vencido vuelve a PENDIENTE
    // y, al estar `enviar_despues` a 0, entra en el ciclo de este mismo sync.
    dao.recuperarEnviandoAntiguos(ahora = ahora)

    for (op in dao.obtenerElegibles(EstadoOutbox.PENDIENTE.valor, ahora)) {
        // Sin autenticacion no se envia nada: la operacion sigue PENDIENTE
        // intacta hasta que un login regrese una cookie valida. El worker
        // produce este valor con `RetrofitClient.tieneCookies()`.
        if (!autenticado) continue

        val enviador = registro[op.tipo]

        // Sin handler en esta fase: la operacion queda PENDIENTE intacta.
        if (enviador == null) continue

        // El lease sella este intento: si el proceso muere, la fila solo se
        // recupera cuando `enviar_despues` venza.
        dao.registrarIntento(
            uuid = op.uuid,
            enviarDespues = ahora + leaseIntentoMs
        )

        val resultado = try {
            enviador.enviar(op)
        } catch (e: Exception) {
            ResultadoEnvio.Retry(e.message ?: "excepcion")
        }

        when (resultado) {
            is ResultadoEnvio.Confirmado -> dao.eliminarPorUuid(op.uuid)
            is ResultadoEnvio.Descartar ->
                dao.registrarResultado(op.uuid, EstadoOutbox.ERROR.valor, resultado.motivo)
            is ResultadoEnvio.AuthRequerida -> {
                // PENDIENTE con motivo auth_x: el reintento lo dispara el
                // login, no el backoff de WorkManager.
                dao.registrarRetry(op.uuid, ultimoError = "auth_${resultado.codigo}")
            }
            is ResultadoEnvio.Retry -> {
                // Recupérable: vuelve a PENDIENTE para el siguiente sync.
                dao.registrarRetry(op.uuid, ultimoError = resultado.motivo)
                huboRetry = true
            }
        }
    }

    return huboRetry
}
