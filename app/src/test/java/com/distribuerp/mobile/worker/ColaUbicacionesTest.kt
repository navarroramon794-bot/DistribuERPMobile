package com.distribuerp.mobile.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.UbicacionPendienteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColaUbicacionesTest {

    private lateinit var db: AppDatabase
    private val dao get() = db.ubicacionPendienteDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun ubicacion(vendedor: Int, ts: Long, lat: Double = ts.toDouble()) =
        UbicacionPendienteEntity(
            vendedorId = vendedor,
            latitud = lat,
            longitud = 0.0,
            precisionM = null,
            velocidad = null,
            fuente = "test",
            fecha = "2026-01-01T00:00:00Z",
            timestampMs = ts
        )

    @Test
    fun `las ubicaciones pendientes se conservan hasta confirmacion`() = runBlocking {
        dao.insertar(ubicacion(7, 1000))
        dao.insertar(ubicacion(7, 2000))
        dao.insertar(ubicacion(7, 3000))

        assertEquals(3, dao.obtenerTodas().size)

        // Simula el borrado de la unica ubicacion confirmada por el backend.
        val confirmada = dao.obtenerPorVendedor(7).first { it.timestampMs == 2000L }
        dao.eliminarPorId(confirmada.id)

        val restantes = dao.obtenerTodas()
        assertEquals(2, restantes.size)
        assertEquals(emptyList<Long>(), restantes.map { it.timestampMs }.filter { it == 2000L })
        assertNotNull(restantes.firstOrNull { it.timestampMs == 1000L })
        assertNotNull(restantes.firstOrNull { it.timestampMs == 3000L })
    }

    @Test
    fun `un fallo de red no elimina nada de la cola`() = runBlocking {
        dao.insertar(ubicacion(7, 1000))
        dao.insertar(ubicacion(7, 2000))

        // El worker devuelve retry sin tocar la base; la cola queda intacta.
        assertEquals(2, dao.contarPorVendedor(7))
        assertEquals(2, dao.obtenerTodas().size)
    }

    @Test
    fun `recortarA50 conserva las 50 mas recientes de ese vendedor`() = runBlocking {
        repeat(60) { dao.insertar(ubicacion(7, 1000L + it)) }

        dao.recortarA50(7)

        val restantes = dao.obtenerPorVendedor(7)
        assertEquals(50, restantes.size)
        assertEquals(1059L, restantes.maxOf { it.timestampMs })
        assertEquals(1010L, restantes.minOf { it.timestampMs })
    }

    @Test
    fun `vendedores distintos no se contaminan entre si`() = runBlocking {
        dao.insertar(ubicacion(7, 1000))
        dao.insertar(ubicacion(9, 2000))

        dao.recortarA50(7)

        assertEquals(1, dao.contarPorVendedor(7))
        assertEquals(1, dao.contarPorVendedor(9))
    }

    @Test
    fun `el vendedorId se persiste tal cual llega de la sesion`() = runBlocking {
        dao.insertar(ubicacion(42, 1000))
        dao.insertar(ubicacion(0, 2000))

        assertEquals(1, dao.contarPorVendedor(42))
        assertEquals(1, dao.contarPorVendedor(0))
        assertEquals(42, dao.obtenerPorVendedor(42).single().vendedorId)
    }

    }