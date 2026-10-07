package com.distribuerp.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.LEASE_INTENTO_OUTBOX_MS
import com.distribuerp.mobile.data.local.OutboxOperation
import com.distribuerp.mobile.data.local.TiposOutbox
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Recuperacion de operaciones Outbox abandonadas en ENVIANDO.
 *
 * Escenario real: el proceso muere entre `registrarIntento` (que deja la fila
 * en ENVIANDO con su lease sellado) y el resultado del envio. Sin reaper esa
 * fila nunca vuelve a entrar, porque el unico selector es
 * `obtenerElegibles(PENDIENTE, ...)`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxRecuperacionEnviandoTest {

    private lateinit var db: AppDatabase

    /** Instante fijo para que los tests no dependan del reloj real. */
    private val ahora = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun registroEnviador(
        onEnviar: (OutboxOperation) -> ResultadoEnvio
    ): Map<String, EnviadorOutbox> = mapOf(
        TiposOutbox.TEST to EnviadorOutbox { onEnviar(it) }
    )

    /**
     * Deja la operacion como la dejaria una ejecucion que murio durante el
     * envio: estado ENVIANDO, un intento contado y el lease sellado en el
     * instante en que empezo.
     */
    private suspend fun marcarEnviandoComoSiHubieseMuerto(
        op: OutboxOperation,
        intentoInicio: Long
    ) {
        db.outboxDao().insertar(op)
        db.outboxDao().registrarIntento(
            uuid = op.uuid,
            enviarDespues = intentoInicio + LEASE_INTENTO_OUTBOX_MS
        )
    }

    // --- TEST 1: una ENVIANDO antigua se recupera a PENDIENTE ---

    @Test
    fun `una operacion ENVIANDO con lease vencido se recupera a PENDIENTE`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")

            // Intento empezo hace mucho mas que un lease.
            marcarEnviandoComoSiHubieseMuerto(
                op,
                intentoInicio = ahora - LEASE_INTENTO_OUTBOX_MS - 1_000L
            )

            val liberadas = db.outboxDao().recuperarEnviandoAntiguos(ahora)

            assertEquals("Debe liberar 1 operacion", 1, liberadas)

            val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
            assertEquals(
                "Debe volver a PENDIENTE",
                EstadoOutbox.PENDIENTE.valor,
                tras.estado
            )
            assertEquals(
                "Debe quedar elegible de inmediato",
                0L,
                tras.enviar_despues
            )
            assertEquals(
                "La traza de intentos se conserva",
                1,
                tras.intentos
            )
        }

    // --- TEST 2: una ENVIANDO reciente NO se recupera ---

    @Test
    fun `una operacion ENVIANDO con lease vigente NO se recupera`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")

        // Intento empezo hace un minuto: su ejecucion sigue viva.
        marcarEnviandoComoSiHubieseMuerto(op, intentoInicio = ahora - 60_000L)

        val liberadas = db.outboxDao().recuperarEnviandoAntiguos(ahora)

        assertEquals("No debe liberar nada", 0, liberadas)

        val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
        assertEquals(
            "Debe seguir ENVIANDO: no se toca una ejecucion viva",
            EstadoOutbox.ENVIANDO.valor,
            tras.estado
        )
    }

    // --- TEST 2b: el limite exacto del umbral ---

    @Test
    fun `el umbral es exacto y no una aproximacion`() = runBlocking {
        val justoVencido = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        val recienVencido = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")

        // Lease que vence justo en `ahora`: recuperado.
        marcarEnviandoComoSiHubieseMuerto(
            justoVencido,
            intentoInicio = ahora - LEASE_INTENTO_OUTBOX_MS
        )
        // Lease que vence un milisegundo despues: intacto.
        marcarEnviandoComoSiHubieseMuerto(
            recienVencido,
            intentoInicio = ahora - LEASE_INTENTO_OUTBOX_MS + 1L
        )

        val liberadas = db.outboxDao().recuperarEnviandoAntiguos(ahora)

        assertEquals("Solo debe liberarse la vencida", 1, liberadas)
        assertEquals(
            EstadoOutbox.PENDIENTE.valor,
            db.outboxDao().obtenerPorUuid(justoVencido.uuid)!!.estado
        )
        assertEquals(
            EstadoOutbox.ENVIANDO.valor,
            db.outboxDao().obtenerPorUuid(recienVencido.uuid)!!.estado
        )
    }

    // --- TEST 3: una PENDIENTE no es alterada por el reaper ---

    @Test
    fun `una operacion PENDIENTE no es alterada por el reaper`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        val liberadas = db.outboxDao().recuperarEnviandoAntiguos(ahora)

        assertEquals(0, liberadas)

        val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.PENDIENTE.valor, tras.estado)
        assertEquals("No debe tocar otros campos", 0, tras.intentos)
    }

    // --- TEST 4: una ERROR no es alterada ---

    @Test
    fun `una operacion en ERROR no es alterada por el reaper`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)
        db.outboxDao().registrarResultado(
            op.uuid,
            EstadoOutbox.ERROR.valor,
            "400 rechazado"
        )

        val liberadas = db.outboxDao().recuperarEnviandoAntiguos(ahora)

        assertEquals(0, liberadas)

        val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ERROR.valor, tras.estado)
        assertEquals("400 rechazado", tras.ultimo_error)
    }

    // --- TEST 5: recuperada, la operacion se envia en el ciclo siguiente ---

    @Test
    fun `tras recuperarse la operacion abandonada se envia y se elimina`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
            marcarEnviandoComoSiHubieseMuerto(
                op,
                intentoInicio = ahora - LEASE_INTENTO_OUTBOX_MS - 1_000L
            )

            var enviadas = 0
            val envio = registroEnviador {
                enviadas++
                ResultadoEnvio.Confirmado
            }

            // Un solo ciclo: el reaper corre y la operacion se envia en el.
            assertFalse(procesarPendientes(db.outboxDao(), envio, ahora))

            assertEquals("Debe haberse enviado una vez", 1, enviadas)
            assertNull(
                "Una vez confirmada se elimina",
                db.outboxDao().obtenerPorUuid(op.uuid)
            )
        }

    // --- TEST 10/11: el flujo completo, como tras un reinicio del proceso ---

    /**
     * Reproduce el defecto original de punta a punta:
     *
     *   intento -> ENVIANDO -> (proceso muere) -> nueva ejecucion
     *     -> reaper -> PENDIENTE -> envio -> eliminado
     */
    @Test
    fun `proceso muerto tras marcar ENVIANDO se recupera y sincroniza al reiniciar`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
            db.outboxDao().insertar(op)

            // --- Ejecucion 1: empieza a enviar y el proceso muere. ---
            db.outboxDao().registrarIntento(
                uuid = op.uuid,
                enviarDespues = ahora + LEASE_INTENTO_OUTBOX_MS
            )
            assertEquals(
                EstadoOutbox.ENVIANDO.valor,
                db.outboxDao().obtenerPorUuid(op.uuid)!!.estado
            )
            assertFalse(
                "Con el lease vigente no debe enviarse nada",
                procesarPendientes(
                    db.outboxDao(),
                    registroEnviador { ResultadoEnvio.Confirmado },
                    ahora
                )
            )

            // --- El tiempo pasa y el lease vence. ---
            val trasReinicio = ahora + LEASE_INTENTO_OUTBOX_MS

            // --- Ejecucion 2: el reaper recupera y el envio ocurre. ---
            var enviadas = 0
            val envio = registroEnviador {
                enviadas++
                ResultadoEnvio.Confirmado
            }

            assertFalse(
                "Confirmar no debe pedir reintento",
                procesarPendientes(db.outboxDao(), envio, trasReinicio)
            )

            assertEquals("Debe enviarse en el reinicio", 1, enviadas)
            assertNull(
                "La operacion se elimina al confirmarse",
                db.outboxDao().obtenerPorUuid(op.uuid)
            )
        }

    /**
     * El caso caro: una operacion abandonada cuyo envio vuelve a fallar por
     * transporte debe terminar en PENDIENTE con el lease re sellado, para que
     * el siguiente sync la reintente y no se quede atascada.
     */
    @Test
    fun `una operacion abandonada que vuelve a fallar queda reintentable`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
            marcarEnviandoComoSiHubieseMuerto(
                op,
                intentoInicio = ahora - LEASE_INTENTO_OUTBOX_MS - 1_000L
            )

            var enviadas = 0
            val envio = registroEnviador {
                enviadas++
                ResultadoEnvio.Retry("timeout")
            }

            assertTrue(
                "Un fallo recuperable debe pedir reintento",
                procesarPendientes(db.outboxDao(), envio, ahora)
            )

            val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
            assertEquals(
                "Debe volver a PENDIENTE para el siguiente sync",
                EstadoOutbox.PENDIENTE.valor,
                tras.estado
            )
            assertEquals("timeout", tras.ultimo_error)
            assertEquals("Dos intentos acumulados", 2, tras.intentos)
            assertEquals(1, enviadas)
        }

    // --- El lease se sella al iniciar cada intento ---

    @Test
    fun `cada intento sella un lease nuevo mayor que ahora`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        db.outboxDao().registrarIntento(
            op.uuid,
            enviarDespues = ahora + LEASE_INTENTO_OUTBOX_MS
        )

        val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ENVIANDO.valor, tras.estado)
        assertEquals(ahora + LEASE_INTENTO_OUTBOX_MS, tras.enviar_despues)
    }

    /**
     * El worker es quien sella el lease. Sin esto, dos ejecuciones concurrentes
     * podrian recuperar y reenviar la misma operacion.
     */
    @Test
    fun `el worker sella el lease mientras la operacion esta ENVIANDO`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
            db.outboxDao().insertar(op)

            var estadoEnVuelo = ""
            var leaseEnVuelo = -1L

            // El enviador se invoca con la fila ya en ENVIANDO: es el momento de
            // inspeccionar el estado intermedio.
            val envio = registroEnviador {
                val enVuelo = runBlocking {
                    db.outboxDao().obtenerPorUuid(it.uuid)!!
                }
                estadoEnVuelo = enVuelo.estado
                leaseEnVuelo = enVuelo.enviar_despues
                ResultadoEnvio.Confirmado
            }

            procesarPendientes(db.outboxDao(), envio, ahora)

            assertEquals(
                "Durante el envio debe estar ENVIANDO",
                EstadoOutbox.ENVIANDO.valor,
                estadoEnVuelo
            )
            assertEquals(
                "Durante el envio debe haber un lease en el futuro",
                ahora + LEASE_INTENTO_OUTBOX_MS,
                leaseEnVuelo
            )
        }

    /**
     * El lease es solo para el intento en curso. Si un fallo recuperable
     * dejara el lease puesto, la operacion quedaria PENDIENTE pero no seria
     * elegible hasta vencer el lease y se perderia el reintento.
     */
    @Test
    fun `un fallo recuperable deja la operacion elegible de inmediato`() =
        runBlocking {
            val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
            db.outboxDao().insertar(op)

            val envio = registroEnviador { ResultadoEnvio.Retry("timeout") }

            assertTrue(
                "Un fallo recuperable debe pedir reintento",
                procesarPendientes(db.outboxDao(), envio, ahora)
            )

            val tras = db.outboxDao().obtenerPorUuid(op.uuid)!!
            assertEquals(EstadoOutbox.PENDIENTE.valor, tras.estado)
            assertEquals(
                "No debe quedar un lease pendiente tras el intento",
                0L,
                tras.enviar_despues
            )
            assertEquals(
                "Debe ser elegible de inmediato en el siguiente sync",
                listOf(op.uuid),
                db.outboxDao()
                    .obtenerElegibles(EstadoOutbox.PENDIENTE.valor, ahora)
                    .map { it.uuid }
            )
        }

    // --- Sin lease (ENVIANDO heredado) se recupera de inmediato ---

    @Test
    fun `un ENVIANDO sin lease se recupera de inmediato`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)
        // ENVIANDO escrito sin lease: caso atascado de una version anterior.
        db.outboxDao().registrarIntento(op.uuid)

        val tras1 = db.outboxDao().obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ENVIANDO.valor, tras1.estado)
        assertEquals(0L, tras1.enviar_despues)

        assertEquals(
            "Sin lease no hay prueba de vida: se recupera",
            1,
            db.outboxDao().recuperarEnviandoAntiguos(ahora)
        )
        assertEquals(
            EstadoOutbox.PENDIENTE.valor,
            db.outboxDao().obtenerPorUuid(op.uuid)!!.estado
        )
    }
}
