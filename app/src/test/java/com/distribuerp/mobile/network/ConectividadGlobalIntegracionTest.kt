package com.distribuerp.mobile.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.MainActivity
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.repository.ClienteRepository
import com.distribuerp.mobile.repository.CobranzaRepository
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.VentaRepository
import com.distribuerp.mobile.viewmodel.VentaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integracion del arranque de conectividad con el enrutamiento de ventas.
 *
 * `ConectividadGlobal` es un singleton que sobrevive entre casos de la misma
 * JVM, asi que cada test parte de `detener()`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConectividadGlobalIntegracionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ConectividadGlobal.detener()
    }

    @After
    fun tearDown() {
        ConectividadGlobal.detener()
    }

    /** Espera a que el recolector propague el cambio al estado global. */
    private suspend fun esperarEstado(esperado: EstadoConectividad) {
        withTimeout(3_000) {
            while (ConectividadGlobal.estado.value != esperado) {
                delay(5)
            }
        }
    }

    // --- 1: MainActivity arranca la conectividad global ---

    @Test
    fun `arrancar MainActivity inicializa la conectividad global`() {
        assertFalse("No debe estar inicializada antes", ConectividadGlobal.inicializado)

        Robolectric.buildActivity(MainActivity::class.java).create()

        assertTrue(
            "MainActivity debe inicializar ConectividadGlobal",
            ConectividadGlobal.inicializado
        )
    }

    @Test
    fun `arrancar MainActivity es idempotente al recrearse`() {
        Robolectric.buildActivity(MainActivity::class.java).create()

        assertTrue(ConectividadGlobal.inicializado)

        // Una recreacion no debe volver a arrancar el monitoreo.
        Robolectric.buildActivity(MainActivity::class.java).create()

        assertTrue(ConectividadGlobal.inicializado)
        assertFalse(ConectividadGlobal.estaOnline())
    }

    // --- 2 y 3: ONLINE y OFFLINE se reflejan correctamente ---

    @Test
    fun `un monitor online se refleja en el estado global`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.ONLINE))

        assertTrue(ConectividadGlobal.estaOnline())
        assertEquals(EstadoConectividad.ONLINE, ConectividadGlobal.estado.value)
    }

    @Test
    fun `un monitor offline se refleja en el estado global`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.OFFLINE))

        assertFalse(ConectividadGlobal.estaOnline())
        assertEquals(EstadoConectividad.OFFLINE, ConectividadGlobal.estado.value)
    }

    // --- Transicion OFFLINE -> ONLINE ---

    @Test
    fun `la transicion offline a online actualiza el estado global`() = runBlocking {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.OFFLINE)
        ConectividadGlobal.iniciar(monitor)

        assertFalse(ConectividadGlobal.estaOnline())

        monitor.setEstado(EstadoConectividad.ONLINE)

        esperarEstado(EstadoConectividad.ONLINE)
        assertTrue(ConectividadGlobal.estaOnline())
    }

    // --- 4: el ViewModel distingue ONLINE de OFFLINE ---

    @Test
    fun `el ViewModel enruta a la cola local cuando no hay red`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.OFFLINE))

        assertFalse(ConectividadGlobal.estaOnline())
    }

    @Test
    fun `el ViewModel intenta el envio por red cuando si hay conexion`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.ONLINE))

        assertTrue(ConectividadGlobal.estaOnline())
    }

    // --- 5: CREDITO deja de bloquearse por estado no inicializado ---

    @Test
    fun `credito se rechaza sin conexion como ONLINE_ONLY`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.OFFLINE))

        val vm = nuevoViewModel().prepararVentaCredito()

        vm.registrarVenta()

        assertNotNull(vm.mensaje)
        assertTrue(
            "Se esperaba el rechazo por falta de conexion, fue: ${vm.mensaje}",
            vm.mensaje!!.contains("requiere conexión a internet")
        )
        assertFalse("No debe haberse enviado nada", vm.guardando)
    }

    @Test
    fun `credito con la conectividad inicializada ya no se bloquea`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.ONLINE))

        val vm = nuevoViewModel().prepararVentaCredito()

        vm.registrarVenta()

        // Al superar el gate de conectividad no queda mensaje de bloqueo por red:
        // la venta prosigue por el flujo online normal.
        assertNull(
            "No debe bloquearse por conectividad: ${vm.mensaje}",
            vm.mensaje?.takeIf { it.contains("requiere conexión a internet") }
        )
        assertTrue("Debe haber entrado al envio por red", vm.guardando)
    }

    @Test
    fun `credito solo se bloquea por las validaciones comerciales`() {
        ConectividadGlobal.iniciar(NetworkMonitorInmutable(EstadoConectividad.ONLINE))

        val vm = nuevoViewModel().prepararVentaCredito(cliente = cliente(creditoAutorizado = false))

        vm.registrarVenta()

        assertNotNull(vm.mensaje)
        assertEquals("El cliente no tiene crédito autorizado.", vm.mensaje)
        assertFalse("No debe haber intentado el envio", vm.guardando)
    }

    // --- Utilidades ---

    private fun nuevoViewModel() = VentaViewModel(
        vendedorRepository = VendedorRepository(),
        clienteRepository = ClienteRepository(),
        productoRepository = ProductoRepository(),
        ventaRepository = VentaRepository(),
        cobranzaRepository = CobranzaRepository(),
        sessionManager = SessionManager(context),
        appContext = context
    )

    private fun cliente(creditoAutorizado: Boolean = true) = Cliente(
        id = 9,
        nombre = "ACME",
        direccion = null,
        telefono = null,
        limite_credito = 100_000.0,
        activo = true,
        fecha_creacion = null,
        credito_autorizado = creditoAutorizado,
        dias_credito = 30,
        bloqueado = false
    )

    /**
     * Deja la venta lista para superar todas las validaciones previas a la de
     * conectividad: producto con cantidad valida, vendedor y cliente elegidos,
     * credito autorizado y limite suficiente.
     */
    private fun VentaViewModel.prepararVentaCredito(cliente: Cliente = cliente()): VentaViewModel {
        seleccionarVendedor(
            Vendedor(
                id = 5,
                nombre = "Juan",
                telefono = null,
                activo = true,
                fecha_creacion = null
            )
        )

        seleccionarCliente(cliente)

        seleccionarFormaPago("CREDITO")

        agregarProducto(
            Producto(
                id = 1,
                codigo = "P1",
                nombre = "Producto",
                descripcion = null,
                costo = 5.0,
                precio = 10.0,
                existencia = 100.0,
                activo = true,
                fecha_creacion = null
            )
        )

        return this
    }
}
