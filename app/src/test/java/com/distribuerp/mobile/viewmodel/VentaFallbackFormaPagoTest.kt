package com.distribuerp.mobile.viewmodel

import android.content.Context
import android.os.Looper
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.test.core.app.ApplicationProvider
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.room.Room
import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.UsuarioGuardado
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.network.ConectividadGlobal
import com.distribuerp.mobile.network.EstadoConectividad
import com.distribuerp.mobile.network.NetworkMonitorInmutable
import com.distribuerp.mobile.repository.ClienteRepository
import com.distribuerp.mobile.repository.CobranzaRepository
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.VentaRepository
import com.distribuerp.mobile.sync.SyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException

/**
 * Regla de OFFLINE-2B: CONTADO es offline-first, CREDITO es ONLINE_ONLY.
 *
 * Se ejercita `registrarVenta()` con la API publica del ViewModel y una
 * `ApiService` real construida con Retrofit cuyo interceptor falla por
 * transporte: eso produce el `IOException` que dispara el fallback.
 *
 * El estado local se lee de la misma base que usa el ViewModel
 * (`AppDatabase.getInstance`), no de un doble.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VentaFallbackFormaPagoTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private var encoladorOriginal: (
        Context, String, ExistingWorkPolicy, OneTimeWorkRequest
    ) -> Unit = { _, _, _, _ -> }

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()

        // Room despacha cada consulta a un hilo distinto de su pool y
        // Robolectric rastrea el puntero de conexion POR HILO, asi que una
        // segunda hebra muerde "Illegal connection pointer". Se fija el
        // ejecutor de disco en linea para que todo corra en un solo hilo.
        ArchTaskExecutor.getInstance().setDelegate(ExecutorEnLinea)

        // `guardarEnLocal` pide la sincronizacion al terminar. Aqui solo se
        // registra la peticion, sin levantar WorkManager.
        encoladorOriginal = SyncManager.encolar
        SyncManager.encolar = { _, _, _, _ -> }

        // `AppDatabase.getInstance` es un singleton con la configuracion de
        // produccion (sin `allowMainThreadQueries`) y su pool de hilos hace que
        // Robolectric pierda el puntero de conexion. Se inyecta una base en
        // memoria por test: con ejecutores en linea todo corre en un hilo y
        // `allowMainThreadQueries` quita el veto de Room al hilo principal.
        usarBaseEnMemoria()

        db = AppDatabase::class.java.getDeclaredField("INSTANCE")
            .apply { isAccessible = true }
            .get(null) as AppDatabase

        // Cada test arranca desde cero.
        db.outboxDao().obtenerTodas().forEach { db.outboxDao().eliminarPorUuid(it.uuid) }
        db.ventaPendienteDao().obtenerPorEmpresa(1).forEach {
            db.ventaPendienteDao().eliminar(it.id)
        }

        SessionManager(context).guardarSesion(
            UsuarioGuardado(
                id = "1",
                nombre = "Juan",
                correo = "juan@acme.mx",
                rol = "VENDEDOR",
                vendedor_id = "5",
                vendedor = "Juan",
                empresa_id = "1",
                password_temporal = false
            )
        )

        ConectividadGlobal.detener()

        assertNotNull(
            "La sesion debe quedar legible para el ViewModel",
            SessionManager(context).sesion.first()
        )
    }

    @After
    fun tearDown() {
        ConectividadGlobal.detener()
        SyncManager.encolar = encoladorOriginal
        usarBaseEnMemoria()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    /**
     * Coloca una base en memoria limpia en el singleton que consume el
     * ViewModel, para poder afirmar sobre lo que realmente se escribio.
     */
    private fun usarBaseEnMemoria() {
        val campo = AppDatabase::class.java.getDeclaredField("INSTANCE")

        campo.isAccessible = true
        (campo.get(null) as? AppDatabase)?.close()

        campo.set(
            null,
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        )
    }

    /**
     * Ejecutor que corre todo en el hilo que llama.
     *
     * `isMainThread()` devuelve false a proposito: la base de produccion no
     * usa `allowMainThreadQueries`, y asi Room no exige estar fuera del hilo
     * principal para las consultas.
     */
    private object ExecutorEnLinea : TaskExecutor() {
        override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
        override fun postToMainThread(runnable: Runnable) = runnable.run()
        override fun isMainThread(): Boolean = false
    }

    /** `ApiService` real que falla siempre por transporte. */
    private fun apiQueFallaPorTransporte(): ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    Interceptor { throw SocketTimeoutException("timeout") }
                )
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    /** `ApiService` real que responde un rechazo de negocio del backend. */
    private fun apiQueRechaza(mensaje: String): ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(400)
                        .message("Error")
                        .header("Content-Type", "application/json")
                        .body(
                            ResponseBody.create(
                                MediaType.parse("application/json"),
                                """{"ok":false,"mensaje":"$mensaje"}"""
                            )
                        )
                        .build()
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private fun nuevoViewModel(
        ventaRepository: VentaRepository = VentaRepository()
    ) = VentaViewModel(
        vendedorRepository = VendedorRepository(),
        clienteRepository = ClienteRepository(),
        productoRepository = ProductoRepository(),
        ventaRepository = ventaRepository,
        cobranzaRepository = CobranzaRepository(),
        sessionManager = SessionManager(context),
        appContext = context
    )

    /**
     * `registrarVenta()` lanza en `viewModelScope`, que en Robolectric corre
     * sobre el looper principal en modo pausado. El callback de OkHttp llega en
     * otro hilo, asi que hay que bombear el looper hasta que la venta resuelva.
     *
     * `guardarEnLocal` y el camino ONLINE_FIRST escriben en la base ANTES de
     * poner `guardando = false`, asi que esta senal significa "terminado".
     */
    private fun esperarA(condicion: () -> Boolean) {
        val limite = System.currentTimeMillis() + 10_000

        while (System.currentTimeMillis() < limite) {
            shadowOf(Looper.getMainLooper()).idle()

            if (condicion()) return

            Thread.sleep(10)
        }

        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Prepara el ticket por la API publica del ViewModel.
     *
     * `vendedorId` por defecto 5, que es tambien el vendedor de la sesion en
     * `setUp`. Los tests que necesitan divergir pasan uno explicito.
     */
    private fun VentaViewModel.prepararVenta(
        formaPago: String,
        vendedorId: Int = 5
    ): VentaViewModel {
        seleccionarVendedor(
            Vendedor(
                id = vendedorId,
                nombre = "Juan",
                telefono = null,
                activo = true,
                fecha_creacion = null
            )
        )

        seleccionarCliente(
            Cliente(
                id = 9,
                nombre = "ACME",
                direccion = null,
                telefono = null,
                limite_credito = 100_000.0,
                activo = true,
                fecha_creacion = null,
                credito_autorizado = true,
                dias_credito = 30,
                bloqueado = false
            )
        )

        seleccionarFormaPago(formaPago)

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

    private fun registrarYEsperar(vm: VentaViewModel) {
        vm.registrarVenta()
        esperarA { !vm.guardando }
    }

    // --- 1: CREDITO ONLINE + IOException no cae a la cola local ---

    @Test
    fun `credito con IOException no crea venta pendiente ni outbox y muestra error`() =
        runBlocking {
            ConectividadGlobal.iniciar(
                NetworkMonitorInmutable(EstadoConectividad.ONLINE)
            )

            val vm = nuevoViewModel(
                VentaRepository(apiQueFallaPorTransporte())
            ).prepararVenta("CREDITO")

            registrarYEsperar(vm)

            // No debe existir nada en local: CREDITO es ONLINE_ONLY.
            assertEquals("No debe crearse VentaPendiente", 0, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
            assertEquals("No debe crearse Outbox", 0, db.outboxDao().contar())

            assertNotNull("Debe mostrarse un error", vm.error)
            assertEquals(
                "No se pudo completar la venta a crédito por falta de conexión. " +
                    "Inténtalo de nuevo cuando tengas internet.",
                vm.error
            )
            assertTrue(
                "No debe encolarse sincronización",
                vm.mensaje.isNullOrBlank()
            )
        }

    // --- 2: CONTADO ONLINE + IOException si cae a la cola local ---

    @Test
    fun `contado con IOException crea venta pendiente y outbox con la misma clave`() =
        runBlocking {
            ConectividadGlobal.iniciar(
                NetworkMonitorInmutable(EstadoConectividad.ONLINE)
            )

            val vm = nuevoViewModel(
                VentaRepository(apiQueFallaPorTransporte())
            ).prepararVenta("CONTADO")

            registrarYEsperar(vm)

            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
            val op = db.outboxDao().obtenerTodas().single()

            assertEquals(TiposOutbox.VENTA, op.tipo)
            assertEquals(
                "La clave debe viajar en la cabecera de la venta pendiente",
                venta.clientOperationId,
                op.payload
                    .substringAfter("\"clientOperationId\":\"")
                    .substringBefore('"')
            )
            assertTrue(
                "Sin clave de idempotencia no hay reenvío seguro",
                venta.clientOperationId.isNotBlank()
            )
            assertTrue(
                "Debe avisar que quedó pendiente",
                vm.mensaje!!.contains("guardada")
            )
            assertEquals("Sin error: no fue un rechazo", null, vm.error)
        }

    // --- 3: CREDITO OFFLINE sigue rechazándose como ONLINE_ONLY ---

    @Test
    fun `credito sin conexion se rechaza sin tocar la base`() = runBlocking {
        ConectividadGlobal.iniciar(
            NetworkMonitorInmutable(EstadoConectividad.OFFLINE)
        )

        val vm = nuevoViewModel().prepararVenta("CREDITO")

        registrarYEsperar(vm)

        assertEquals(0, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(0, db.outboxDao().contar())
        assertNotNull(vm.mensaje)
        assertTrue(vm.mensaje!!.contains("requiere conexión a internet"))
    }

    // --- 4: CONTADO OFFLINE se guarda localmente sin tocar la red ---

    @Test
    fun `contado sin conexion se guarda localmente`() = runBlocking {
        ConectividadGlobal.iniciar(
            NetworkMonitorInmutable(EstadoConectividad.OFFLINE)
        )

        // Este repositorio falla si se usa: en OFFLINE no debe haber intento.
        val vm = nuevoViewModel(
            VentaRepository(apiQueFallaPorTransporte())
        ).prepararVenta("CONTADO")

        registrarYEsperar(vm)

        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(1, db.outboxDao().contar())
        assertEquals(TiposOutbox.VENTA, db.outboxDao().obtenerTodas().single().tipo)
    }

    // --- El vendedor persistido es el SELECCIONADO, no el de la sesion ---

    /**
     * Regresion del Blocker 1: `guardarEnLocal` tomaba `vendedor_id` de la
     * sesion, asi que una venta offline creada eligiendo otro vendedor se
     * sincronizaba atribuida al equivocado.
     *
     * Sesion: `vendedor_id = 5`. Seleccionado en el ticket: 8.
     */
    @Test
    fun `la venta offline guarda el vendedor seleccionado y no el de la sesion`() =
        runBlocking {
            // `setUp` deja la sesion con vendedor_id = 5.
            assertEquals(
                "La sesion del test debe tener vendedor 5",
                5,
                SessionManager(context).sesion.first()!!.vendedor_id!!.toInt()
            )

            ConectividadGlobal.iniciar(
                NetworkMonitorInmutable(EstadoConectividad.OFFLINE)
            )

            val vm = nuevoViewModel().prepararVenta("CONTADO", vendedorId = 8)

            registrarYEsperar(vm)

            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()

            assertEquals(
                "Debe persistirse el vendedor SELECCIONADO (8)",
                8,
                venta.vendedorId
            )
            assertNotEquals(
                "No debe persistirse el vendedor de la sesion (5)",
                5,
                venta.vendedorId
            )

            // El tenant sigue viniendo de la sesion.
            assertEquals(1, venta.empresaId)

            // El payload que se enviara al backend debe llevar tambien el 8.
            val payload = db.outboxDao().obtenerTodas().single().payload
            assertEquals(TiposOutbox.VENTA, db.outboxDao().obtenerTodas().single().tipo)

            assertTrue(
                "El payload debe llevar vendedor_id 8, no 5: $payload",
                payload.contains("\"vendedor_id\":8")
            )
            assertTrue(
                "El payload debe llevar la misma clave de idempotencia",
                payload.contains(venta.clientOperationId)
            )
            assertTrue(
                "La clave de idempotencia no puede ir vacia",
                venta.clientOperationId.isNotBlank()
            )
        }

    // --- Un rechazo del backend no genera cola local en ninguna forma de pago ---

    @Test
    fun `un rechazo del backend no encola nada tampoco en contado`() = runBlocking {
        ConectividadGlobal.iniciar(
            NetworkMonitorInmutable(EstadoConectividad.ONLINE)
        )

        val vm = nuevoViewModel(
            VentaRepository(apiQueRechaza("stock insuficiente"))
        ).prepararVenta("CONTADO")

        registrarYEsperar(vm)

        assertEquals(0, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(0, db.outboxDao().contar())
        assertNotNull(vm.error)
    }

    @Test
    fun `un rechazo del backend no encola nada tampoco en credito`() = runBlocking {
        ConectividadGlobal.iniciar(
            NetworkMonitorInmutable(EstadoConectividad.ONLINE)
        )

        val vm = nuevoViewModel(
            VentaRepository(apiQueRechaza("limite de crédito excedido"))
        ).prepararVenta("CREDITO")

        registrarYEsperar(vm)

        assertEquals(0, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(0, db.outboxDao().contar())
        assertNotNull(vm.error)
    }
}
