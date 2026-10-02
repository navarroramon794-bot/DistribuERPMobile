package com.distribuerp.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: OutboxDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.outboxDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `uuid se genera automaticamente y es unico`() = runBlocking {
        val a = OutboxOperation(tipo = TiposOutbox.TEST, payload = """{"a":1}""")
        val b = OutboxOperation(tipo = TiposOutbox.TEST, payload = """{"a":2}""")

        dao.insertar(a)
        dao.insertar(b)

        assertTrue(a.uuid != b.uuid)
        assertEquals(2, dao.contar())
        assertEquals(2, dao.obtenerTodas().map { it.uuid }.toSet().size)
    }

    @Test
    fun `insertar duplicado por uuid se ignora`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")

        val primero = dao.insertar(op)
        val segundo = dao.insertar(op)

        assertTrue(primero > 0)
        assertEquals(-1L, segundo)
        assertEquals(1, dao.contar())
    }

    @Test
    fun `operacion nace PENDIENTE con contadores en cero`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        dao.insertar(op)

        val guardada = dao.obtenerPorUuid(op.uuid)
        assertNotNull(guardada)
        assertEquals(EstadoOutbox.PENDIENTE.valor, guardada!!.estado)
        assertEquals(EstadoOutbox.PENDIENTE, guardada.estadoActual)
        assertEquals(0, guardada.intentos)
        assertNull(guardada.ultimo_error)
        assertEquals(0L, guardada.enviar_despues)
        assertTrue(guardada.creado_en > 0L)
    }

    @Test
    fun `registrarIntento incrementa intentos y limpia el error previo`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        dao.insertar(op)

        dao.registrarIntento(op.uuid)
        val durante = dao.obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ENVIANDO.valor, durante.estado)
        assertEquals(1, durante.intentos)
        assertNull(durante.ultimo_error)

        dao.registrarResultado(op.uuid, EstadoOutbox.ERROR.valor, "timeout")
        val conError = dao.obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ERROR.valor, conError.estado)
        assertEquals("timeout", conError.ultimo_error)

        // El siguiente intento limpia el error anterior y suma un intento mas.
        dao.registrarIntento(op.uuid)
        val reintento = dao.obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.ENVIANDO.valor, reintento.estado)
        assertEquals(2, reintento.intentos)
        assertNull(reintento.ultimo_error)
    }

    @Test
    fun `registrarResultado no altera el contador de intentos`() = runBlocking {
        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = "{}")
        dao.insertar(op)

        dao.registrarIntento(op.uuid)
        dao.registrarResultado(op.uuid, EstadoOutbox.OK.valor, null)

        val tras = dao.obtenerPorUuid(op.uuid)!!
        assertEquals(EstadoOutbox.OK.valor, tras.estado)
        assertEquals(1, tras.intentos)
    }

    @Test
    fun `obtenerPorEstado devuelve solo las coincidentes en orden de creacion`() = runBlocking {
        val viejo = OutboxOperation(tipo = TiposOutbox.TEST, payload = "1", creado_en = 1000L)
        val nuevo = OutboxOperation(tipo = TiposOutbox.TEST, payload = "2", creado_en = 2000L)
        dao.insertar(nuevo)
        dao.insertar(viejo)
        dao.registrarIntento(viejo.uuid)
        dao.registrarResultado(viejo.uuid, EstadoOutbox.ERROR.valor, "fallo")

        assertEquals(listOf(nuevo.uuid), dao.obtenerPorEstado(EstadoOutbox.PENDIENTE.valor).map { it.uuid })
        assertEquals(listOf(viejo.uuid), dao.obtenerPorEstado(EstadoOutbox.ERROR.valor).map { it.uuid })
        assertEquals(listOf(viejo.uuid, nuevo.uuid), dao.obtenerTodas().map { it.uuid })
    }

    @Test
    fun `eliminarPorUuid solo borra la operacion indicada`() = runBlocking {
        val confirmada = OutboxOperation(tipo = TiposOutbox.TEST, payload = "ok")
        val pendiente = OutboxOperation(tipo = TiposOutbox.TEST, payload = "espera")
        val error = OutboxOperation(tipo = TiposOutbox.TEST, payload = "fallo")
        dao.insertar(confirmada)
        dao.insertar(pendiente)
        dao.insertar(error)
        dao.registrarIntento(error.uuid)
        dao.registrarResultado(error.uuid, EstadoOutbox.ERROR.valor, "500")

        dao.eliminarPorUuid(confirmada.uuid)

        assertNull(dao.obtenerPorUuid(confirmada.uuid))
        assertNotNull(dao.obtenerPorUuid(pendiente.uuid))
        assertNotNull(dao.obtenerPorUuid(error.uuid))
        assertEquals(2, dao.contar())
    }

    @Test
    fun `payload y tipo se conservan tal cual`() = runBlocking {
        val payload = """{"id":10,"detalle":"ñ-á-😀","monto":15.5}"""
        val op = OutboxOperation(tipo = TiposOutbox.GPS, payload = payload)
        dao.insertar(op)

        assertEquals(payload, dao.obtenerPorUuid(op.uuid)!!.payload)
        assertEquals(TiposOutbox.GPS, dao.obtenerPorUuid(op.uuid)!!.tipo)
    }

    @Test
    fun `contarPorEstado refleja cada transicion`() = runBlocking {
        val a = OutboxOperation(tipo = TiposOutbox.TEST, payload = "a")
        val b = OutboxOperation(tipo = TiposOutbox.TEST, payload = "b")
        dao.insertar(a)
        dao.insertar(b)

        dao.registrarIntento(a.uuid)
        dao.registrarIntento(b.uuid)
        dao.registrarResultado(b.uuid, EstadoOutbox.ERROR.valor, "x")

        assertEquals(0, dao.contarPorEstado(EstadoOutbox.PENDIENTE.valor))
        assertEquals(1, dao.contarPorEstado(EstadoOutbox.ENVIANDO.valor))
        assertEquals(1, dao.contarPorEstado(EstadoOutbox.ERROR.valor))
        assertEquals(0, dao.contarPorEstado(EstadoOutbox.OK.valor))
    }

    @Test
    fun `uuid explicito del servidor se respeta`() = runBlocking {
        val fijo = UUID.randomUUID().toString()
        val op = OutboxOperation(uuid = fijo, tipo = TiposOutbox.TEST, payload = "{}")

        dao.insertar(op)

        assertEquals(fijo, dao.obtenerPorUuid(fijo)!!.uuid)
    }
}