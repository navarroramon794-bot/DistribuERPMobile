package com.distribuerp.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.models.ItemVentaRequest
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.PayloadVenta
import com.distribuerp.mobile.repository.VentaPendienteRepository
import com.google.gson.Gson
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VentaOfflinePersistenciaTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: VentaPendienteRepository
    private val gson = Gson()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = VentaPendienteRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun linea(
        productoId: Int = 1,
        cantidad: Double = 2.0,
        precio: Double = 10.0
    ) = LineaSnapshot(
        productoId = productoId,
        cantidad = cantidad,
        precioUnitario = precio,
        subtotal = cantidad * precio
    )

    @Test
    fun `crea venta pendiente y operacion outbox en la misma transaccion`() = runBlocking {
        val creada = repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea())
        )

        val venta = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)
        assertNotNull(venta)
        assertEquals("PENDIENTE_SYNC", venta!!.estadoLocal)

        val ops = db.outboxDao().obtenerTodas()
        assertEquals(1, ops.size)
        assertEquals(TiposOutbox.VENTA, ops[0].tipo)
        assertEquals(EstadoOutbox.PENDIENTE.valor, ops[0].estado)
    }

    @Test
    fun `no genera folio comercial antes de sincronizar`() = runBlocking {
        val creada = repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea())
        )

        val venta = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!
        assertNull(venta.folioBackend)
        assertNull(venta.ventaIdBackend)
    }

    @Test
    fun `el payload conserva la clientOperationId usada como clave de idempotencia`() = runBlocking {
        val creada = repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = "clave-fija-123"
        )

        assertEquals("clave-fija-123", creada.clientOperationId)

        val payload = gson.fromJson(
            db.outboxDao().obtenerTodas()[0].payload,
            PayloadVenta::class.java
        )
        assertEquals("clave-fija-123", payload.clientOperationId)
        assertEquals(1, payload.empresaId)
        assertEquals(creada.ventaIdLocal, payload.ventaIdLocal)
    }

    @Test
    fun `dos ventas generan claves distintas`() = runBlocking {
        val a = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()))
        val b = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()))

        assertTrue(a.clientOperationId != b.clientOperationId)
        assertTrue(a.ventaIdLocal != b.ventaIdLocal)
    }

    @Test
    fun `reintentar con la misma clave no crea una segunda venta`() = runBlocking {
        val clave = "clave-reutilizada"

        val primera = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), clave)
        val segunda = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), clave)

        assertEquals(clave, primera.clientOperationId)
        assertEquals(clave, segunda.clientOperationId)
        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
    }

    @Test
    fun `reintentar con la misma clave no duplica la operacion outbox`() = runBlocking {
        val clave = "clave-outbox"

        repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), clave)
        repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), clave)

        assertEquals(1, db.outboxDao().contar())
    }

    @Test
    fun `aislamiento por tenant no mezcla empresas`() = runBlocking {
        repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = "compartida"
        )
        repo.crearVentaOffline(
            empresaId = 2,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = "compartida"
        )

        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(2).size)

        val deEmpresa1 = db.ventaPendienteDao().obtenerPorClientOperationId(1, "compartida")
        val deEmpresa2 = db.ventaPendienteDao().obtenerPorClientOperationId(2, "compartida")

        assertEquals(1, deEmpresa1!!.empresaId)
        assertEquals(2, deEmpresa2!!.empresaId)
        assertTrue(deEmpresa1.id != deEmpresa2.id)
    }

    @Test
    fun `los precios quedan congelados como snapshot`() = runBlocking {
        val creada = repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea(productoId = 3, cantidad = 3.0, precio = 7.5))
        )

        val detalles = db.ventaPendienteDao().obtenerDetallesPorVentaId(creada.ventaIdLocal)

        assertEquals(1, detalles.size)
        assertEquals(3, detalles[0].productoId)
        assertEquals(3.0, detalles[0].cantidad, 0.0001)
        assertEquals(7.5, detalles[0].precioUnitario, 0.0001)
        assertEquals(22.5, detalles[0].subtotal, 0.0001)

        val venta = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!
        assertEquals(22.5, venta.subtotal, 0.0001)
        assertEquals(22.5, venta.total, 0.0001)
    }

    @Test
    fun `el payload envia productos con cantidad snapshot y sin precio forzado`() = runBlocking {
        repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea(productoId = 3, cantidad = 3.0, precio = 7.5))
        )

        val payload = gson.fromJson(
            db.outboxDao().obtenerTodas()[0].payload,
            PayloadVenta::class.java
        )

        assertEquals(9, payload.request.cliente_id)
        assertEquals(5, payload.request.vendedor_id)
        assertEquals("CONTADO", payload.request.forma_pago)
        assertEquals(1, payload.request.productos.size)
        assertEquals(ItemVentaRequest(producto_id = 3, cantidad = 3.0), payload.request.productos[0])
    }

    @Test
    fun `rechaza una venta sin productos`() = runBlocking {
        var fallo = false
        try {
            repo.crearVentaOffline(1, 5, 9, "CONTADO", emptyList())
        } catch (e: IllegalArgumentException) {
            fallo = true
        }

        assertTrue(fallo)
        assertEquals(0, db.outboxDao().contar())
        assertEquals(0, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
    }

    @Test
    fun `rechaza empresa o vendedor invalidos sin escribir nada`() = runBlocking {
        var falloEmpresa = false
        try {
            repo.crearVentaOffline(0, 5, 9, "CONTADO", listOf(linea()))
        } catch (e: IllegalArgumentException) {
            falloEmpresa = true
        }

        var falloVendedor = false
        try {
            repo.crearVentaOffline(1, 0, 9, "CONTADO", listOf(linea()))
        } catch (e: IllegalArgumentException) {
            falloVendedor = true
        }

        assertTrue(falloEmpresa)
        assertTrue(falloVendedor)
        assertEquals(0, db.outboxDao().contar())
    }

    @Test
    fun `los estados de la venta pendientes se actualizan por id`() = runBlocking {
        val creada = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()))

        db.ventaPendienteDao().marcarEnviando(creada.ventaIdLocal)
        assertEquals(
            "ENVIANDO",
            db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!.estadoLocal
        )

        db.ventaPendienteDao().marcarSincronizada(
            id = creada.ventaIdLocal,
            folio = "VTA-000123",
            ventaId = 77
        )
        val tras = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!
        assertEquals("SINCRONIZADA", tras.estadoLocal)
        assertEquals("VTA-000123", tras.folioBackend)
        assertEquals(77, tras.ventaIdBackend)
        assertNull(tras.error)
    }

    @Test
    fun `un error definitivo conserva la venta y su mensaje`() = runBlocking {
        val creada = repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()))

        db.ventaPendienteDao().marcarError(
            id = creada.ventaIdLocal,
            estado = EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor,
            error = "sin existencias"
        )

        val tras = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!
        assertEquals("ERROR_DEFINITIVO", tras.estadoLocal)
        assertEquals("sin existencias", tras.error)
        assertNull(tras.folioBackend)
    }
}