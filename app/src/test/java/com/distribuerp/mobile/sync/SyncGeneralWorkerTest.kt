package com.distribuerp.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.OutboxOperation
import com.distribuerp.mobile.data.local.TiposOutbox
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncGeneralWorkerTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun registro(vararg pares: Pair<String, EnviadorOutbox>): Map<String, EnviadorOutbox> =
        pares.toMap()

    @Test
    fun `sin operaciones no pide reintento`() = runBlocking {
        assertFalse(procesarPendientes(db.outboxDao(), registro()))
    }

    @Test
    fun `sin handler la operacion queda PENDIENTE sin tocar`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        val huboRetry = procesarPendientes(db.outboxDao(), registro())

        assertFalse(huboRetry)
        val tras = db.outboxDao().obtenerPorUuid(op.uuid)
        assertNotNull(tras)
        assertEquals(EstadoOutbox.PENDIENTE.valor, tras!!.estado)
        assertEquals(0, tras.intentos)
        assertNull(tras.ultimo_error)
    }

    @Test
    fun `confirmacion elimina la operacion confirmada`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "ok")
        db.outboxDao().insertar(op)

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { ResultadoEnvio.Confirmado })
        )

        assertFalse(huboRetry)
        assertNull(db.outboxDao().obtenerPorUuid(op.uuid))
        assertEquals(0, db.outboxDao().contar())
    }

    @Test
    fun `cada uuid se procesa por separado sin colapso de cola`() = runBlocking {
        val primera = OutboxOperation(tipo = TiposOutbox.TEST, payload = "1", creado_en = 1L)
        val segunda = OutboxOperation(tipo = TiposOutbox.TEST, payload = "2", creado_en = 2L)
        val tercera = OutboxOperation(tipo = TiposOutbox.TEST, payload = "3", creado_en = 3L)
        db.outboxDao().insertar(primera)
        db.outboxDao().insertar(segunda)
        db.outboxDao().insertar(tercera)

        val enviados = mutableListOf<String>()
        procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { op ->
                enviados += op.payload
                ResultadoEnvio.Confirmado
            })
        )

        assertEquals(listOf("1", "2", "3"), enviados)
        assertEquals(0, db.outboxDao().contar())
    }

    @Test
    fun `fallo transitorio conserva la operacion y pide reintento`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { ResultadoEnvio.Retry("timeout") })
        )

        assertTrue(huboRetry)
        val tras = db.outboxDao().obtenerPorUuid(op.uuid)
        assertNotNull(tras)
        assertEquals(EstadoOutbox.ERROR.valor, tras!!.estado)
        assertEquals(1, tras.intentos)
        assertEquals("timeout", tras.ultimo_error)
    }

    @Test
    fun `error permanente conserva la operacion con su motivo`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { ResultadoEnvio.Descartar("400") })
        )

        assertFalse(huboRetry)
        val tras = db.outboxDao().obtenerPorUuid(op.uuid)
        assertNotNull(tras)
        assertEquals(EstadoOutbox.ERROR.valor, tras!!.estado)
        assertEquals("400", tras.ultimo_error)
        assertEquals(1, tras.intentos)
    }

    @Test
    fun `excepcion del enviador se traduce a retry sin borrar datos`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { throw IllegalStateException("boom") })
        )

        assertTrue(huboRetry)
        assertNotNull(db.outboxDao().obtenerPorUuid(op.uuid))
        assertEquals(EstadoOutbox.ERROR.valor, db.outboxDao().obtenerPorUuid(op.uuid)!!.estado)
    }

    @Test
    fun `tipos distintos no se mezclan entre si`() = runBlocking {
        val conHandler = OutboxOperation(tipo = TiposOutbox.TEST, payload = "1")
        val sinHandler = OutboxOperation(tipo = TiposOutbox.GPS, payload = "2")
        db.outboxDao().insertar(conHandler)
        db.outboxDao().insertar(sinHandler)

        procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { ResultadoEnvio.Confirmado })
        )

        assertNull(db.outboxDao().obtenerPorUuid(conHandler.uuid))
        assertNotNull(db.outboxDao().obtenerPorUuid(sinHandler.uuid))
        assertEquals(
            EstadoOutbox.PENDIENTE.valor,
            db.outboxDao().obtenerPorUuid(sinHandler.uuid)!!.estado
        )
        assertEquals(1, db.outboxDao().contar())
    }

    @Test
    fun `un fallo no impide procesar las operaciones siguientes`() = runBlocking {
        val falla = OutboxOperation(tipo = TiposOutbox.TEST, payload = "1", creado_en = 1L)
        val ok = OutboxOperation(tipo = TiposOutbox.TEST, payload = "2", creado_en = 2L)
        db.outboxDao().insertar(falla)
        db.outboxDao().insertar(ok)

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { op ->
                if (op.payload == "1") ResultadoEnvio.Retry("x") else ResultadoEnvio.Confirmado
            })
        )

        assertTrue(huboRetry)
        assertNotNull(db.outboxDao().obtenerPorUuid(falla.uuid))
        assertNull(db.outboxDao().obtenerPorUuid(ok.uuid))
    }

    @Test
    fun `solo procesa PENDIENTES y no toca OK ni ERROR`() = runBlocking {
        val enOk = OutboxOperation(tipo = TiposOutbox.TEST, payload = "ok-state")
        val enError = OutboxOperation(tipo = TiposOutbox.TEST, payload = "error-state")
        db.outboxDao().insertar(enOk)
        db.outboxDao().insertar(enError)
        db.outboxDao().actualizarEstado(enOk.uuid, EstadoOutbox.OK.valor)
        db.outboxDao().registrarIntento(enError.uuid)
        db.outboxDao().registrarResultado(enError.uuid, EstadoOutbox.ERROR.valor, "previo")

        val enviados = mutableListOf<String>()
        procesarPendientes(
            db.outboxDao(),
            registro(TiposOutbox.TEST to EnviadorOutbox { op ->
                enviados += op.payload
                ResultadoEnvio.Confirmado
            })
        )

        assertTrue(enviados.isEmpty())
        assertNotNull(db.outboxDao().obtenerPorUuid(enOk.uuid))
        assertNotNull(db.outboxDao().obtenerPorUuid(enError.uuid))
        assertEquals("previo", db.outboxDao().obtenerPorUuid(enError.uuid)!!.ultimo_error)
    }

    @Test
    fun `una operacion confirmada nunca se procesa dos veces`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        db.outboxDao().insertar(op)
        var llamadas = 0
        val reg = registro(TiposOutbox.TEST to EnviadorOutbox {
            llamadas++
            ResultadoEnvio.Confirmado
        })

        procesarPendientes(db.outboxDao(), reg)
        procesarPendientes(db.outboxDao(), reg)

        assertEquals(1, llamadas)
    }

    @Test
    fun `el registro de enviadores se resuelve por tipo`() {
        val enviador = EnviadorOutbox { ResultadoEnvio.Confirmado }
        RegistroEnvio.limpiar()

        RegistroEnvio.registrar(TiposOutbox.TEST, enviador)

        assertEquals(enviador, RegistroEnvio.obtener(TiposOutbox.TEST))
        assertNull(RegistroEnvio.obtener(TiposOutbox.GPS))
        RegistroEnvio.limpiar()
    }

    @Test
    fun `el nombre del trabajo general es sync_general`() {
        assertEquals("sync_general", SyncManager.TRABAJO_GENERAL)
    }
}