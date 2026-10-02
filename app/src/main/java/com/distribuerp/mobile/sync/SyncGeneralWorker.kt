package com.distribuerp.mobile.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
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

    /** Fallo permanente (4xx): no se reintentara, pero se conserva para traza. */
    data class Descartar(val motivo: String) : ResultadoEnvio()
}

fun interface EnviadorOutbox {
    suspend fun enviar(op: OutboxOperation): ResultadoEnvio
}

object RegistroEnvio {

    private val enviados = mutableMapOf<String, EnviadorOutbox>()

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
        val dao = AppDatabase.getInstance(applicationContext).outboxDao()
        if (procesarPendientes(dao)) Result.retry() else Result.success()
    }
}

/**
 * Procesa las operaciones PENDIENTES de forma individual, una por uuid.
 *
 * Devuelve `true` si hubo que pedir reintento. Nunca se elimina una operacion
 * sin confirmacion real del backend.
 */
suspend fun procesarPendientes(
    dao: OutboxDao,
    registro: Map<String, EnviadorOutbox> = RegistroEnvio.enviados()
): Boolean {
    var huboRetry = false

    for (op in dao.obtenerPorEstado(EstadoOutbox.PENDIENTE.valor)) {
        val enviador = registro[op.tipo]

        // Sin handler en esta fase: la operacion queda PENDIENTE intacta.
        if (enviador == null) continue

        dao.registrarIntento(op.uuid)

        val resultado = try {
            enviador.enviar(op)
        } catch (e: Exception) {
            ResultadoEnvio.Retry(e.message ?: "excepcion")
        }

        when (resultado) {
            is ResultadoEnvio.Confirmado -> dao.eliminarPorUuid(op.uuid)
            is ResultadoEnvio.Descartar ->
                dao.registrarResultado(op.uuid, EstadoOutbox.ERROR.valor, resultado.motivo)
            is ResultadoEnvio.Retry -> {
                dao.registrarResultado(op.uuid, EstadoOutbox.ERROR.valor, resultado.motivo)
                huboRetry = true
            }
        }
    }

    return huboRetry
}
