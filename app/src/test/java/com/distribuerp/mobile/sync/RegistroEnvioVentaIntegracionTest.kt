package com.distribuerp.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.EstadoVentaSincronizacion
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.VentaPendienteRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Integracion de `EnviadorVenta` con el mecanismo real de envio.
 *
 * A diferencia de `EnviadorVentaTest`, aqui NO se inyecta el mapa de enviadores:
 * se registra con `registrarEnviadorVenta()` y se ejecuta `procesarPendientes`
 * con su registro por defecto, de modo que la resolucion de `TiposOutbox.VENTA`
 * ocurre dentro de `RegistroEnvio`, igual que en `SyncGeneralWorker.doWork()`.
 *
 * La peticion se captura con un `Interceptor` de OkHttp sobre una `ApiService`
 * real construida con Retrofit, por lo que se ejercitan el payload, los headers
 * y el conversor JSON de verdad, sin red y sin dependencias nuevas.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegistroEnvioVentaIntegracionTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: VentaPendienteRepository
    private lateinit var capturador: Capturador

    private data class Peticion(
        val metodo: String,
        val ruta: String,
        val idempotencyKey: String?,
        val cuerpo: String
    )

    /**
     * Intercepta la llamada y devuelve la respuesta preparada, o lanza el error
     * configurado para simular fallos de red.
     */
    private class Capturador(
        private val cuerpo: () -> String = { EXITO },
        private val codigo: () -> Int = { 201 },
        private val fallo: () -> Throwable? = { null }
    ) : Interceptor {

        val peticiones = mutableListOf<Peticion>()

        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()

            val buffer = okio.Buffer()
            request.body()?.writeTo(buffer)

            // Se registra el intento antes de responder, para que tambien queden
            // capturados los que fallan por red.
            peticiones += Peticion(
                metodo = request.method(),
                ruta = request.url().encodedPath(),
                idempotencyKey = request.header("X-Idempotency-Key"),
                cuerpo = buffer.readUtf8()
            )

            fallo()?.let { throw it }

            return okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(codigo())
                .message(if (codigo() < 400) "OK" else "Error")
                .header("Content-Type", "application/json")
                .body(ResponseBody.create(MediaType.parse("application/json"), cuerpo()))
                .build()
        }
    }

    private fun api(capturador: Capturador): ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(OkHttpClient.Builder().addInterceptor(capturador).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = VentaPendienteRepository(db)
        capturador = Capturador()
        RegistroEnvio.limpiar()
    }

    @After
    fun tearDown() {
        RegistroEnvio.limpiar()
        db.close()
    }

    private fun linea() = LineaSnapshot(
        productoId = 1,
        cantidad = 2.0,
        precioUnitario = 10.0,
        subtotal = 20.0
    )

    private suspend fun crearVenta(clave: String) {
        repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = clave
        )
    }

    // --- 1 y 2: el registro real resuelve VENTA hacia EnviadorVenta ---

    @Test
    fun `el registro real resuelve VENTA hacia EnviadorVenta`() {
        registrarEnviadorVenta(
            ApplicationProvider.getApplicationContext(),
            db = db,
            api = api(capturador)
        )

        val resuelto = RegistroEnvio.obtener(TiposOutbox.VENTA)

        assertNotNull("VENTA debe resolverse en el registro real", resuelto)
        assertTrue(
            "El enviador registrado debe ser EnviadorVenta, fue ${resuelto!!::class.java.name}",
            resuelto is EnviadorVenta
        )
    }

    // --- 3 a 9: flujo completo por el mecanismo real ---

    @Test
    fun `una operacion VENTA pendiente se envia por el registro real y actualiza la venta`() =
        runBlocking {
            val clave = "clave-integracion"
            crearVenta(clave)

            val pendiente = db.outboxDao().obtenerTodas().single()
            assertEquals(TiposOutbox.VENTA, pendiente.tipo)
            assertEquals(EstadoOutbox.PENDIENTE.valor, pendiente.estado)

            registrarEnviadorVenta(
                ApplicationProvider.getApplicationContext(),
                db = db,
                api = api(capturador)
            )

            // Sin inyectar mapa: se usa RegistroEnvio.enviados() por defecto.
            assertFalse(procesarPendientes(db.outboxDao()))

            // 4 y 6: la peticion real llega a POST /api/ventas con la clave.
            val enviada = capturador.peticiones.single()
            assertEquals("POST", enviada.metodo)
            assertEquals("/api/ventas", enviada.ruta)
            assertEquals(clave, enviada.idempotencyKey)

            // El payload enviado conserva la operacion original.
            assertTrue(enviada.cuerpo.contains("\"cliente_id\":9"))
            assertTrue(enviada.cuerpo.contains("\"vendedor_id\":5"))
            assertTrue(enviada.cuerpo.contains("\"forma_pago\":\"CONTADO\""))

            // 5: el folio del backend queda almacenado (9).
            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
            assertEquals(EstadoVentaSincronizacion.SINCRONIZADA.valor, venta.estadoLocal)
            assertEquals("VTA-000777", venta.folioBackend)
            assertEquals(77, venta.ventaIdBackend)

            // La operacion confirmada sale de la cola.
            assertEquals(0, db.outboxDao().contar())
        }

    // --- Una VENTA sin registrar no puede enviarse ---

    @Test
    fun `sin registrar VENTA la operacion queda PENDIENTE y no se envia`() = runBlocking {
        val clave = "clave-sin-registrar"
        crearVenta(clave)

        // El registro esta vacio: es el escenario real de hoy sin bootstrap.
        assertNull(RegistroEnvio.obtener(TiposOutbox.VENTA))

        assertFalse(procesarPendientes(db.outboxDao()))

        assertTrue("No debe haberse enviado nada", capturador.peticiones.isEmpty())

        val intacta = db.outboxDao().obtenerTodas().single()
        assertEquals(EstadoOutbox.PENDIENTE.valor, intacta.estado)
        assertEquals(0, intacta.intentos)

        // Y al registrar despues, la misma operacion si sale.
        registrarEnviadorVenta(
            ApplicationProvider.getApplicationContext(),
            db = db,
            api = api(capturador)
        )

        assertFalse(procesarPendientes(db.outboxDao()))
        assertEquals(1, capturador.peticiones.size)
        assertEquals(clave, capturador.peticiones.single().idempotencyKey)
    }

    // --- No hay dos enviadores para el mismo tipo ---

    @Test
    fun `registrar varias veces deja un unico enviador por tipo`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()

        registrarEnviadorVenta(contexto, db = db, api = api(capturador))
        registrarEnviadorVenta(contexto, db = db, api = api(capturador))
        registrarEnviadorVenta(contexto, db = db, api = api(capturador))

        val enviados = RegistroEnvio.enviados()

        assertEquals(1, enviados.size)
        assertEquals(1, enviados.keys.count { it == TiposOutbox.VENTA })
        assertTrue(RegistroEnvio.obtener(TiposOutbox.VENTA) is EnviadorVenta)
    }

    // --- GPS y demas tipos no se ven afectados ---

    @Test
    fun `registrar VENTA no crea ni altera enviadores de otros tipos`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()

        registrarEnviadorVenta(contexto, db = db, api = api(capturador))

        assertNull("GPS debe seguir sin registro", RegistroEnvio.obtener(TiposOutbox.GPS))
        assertNull(RegistroEnvio.obtener(TiposOutbox.TEST))
        assertEquals(setOf(TiposOutbox.VENTA), RegistroEnvio.enviados().keys)

        // Un tipo propio sigue registrandose igual que antes.
        val propio = EnviadorOutbox { ResultadoEnvio.Confirmado }
        RegistroEnvio.registrar(TiposOutbox.GPS, propio)

        assertEquals(propio, RegistroEnvio.obtener(TiposOutbox.GPS))
        assertTrue(RegistroEnvio.obtener(TiposOutbox.VENTA) is EnviadorVenta)
    }

    // --- Retry preservado a traves del mecanismo real ---

    @Test
    fun `un timeout deja la venta reintentable con la misma clave y sin duplicar`() = runBlocking {
        val clave = "clave-retry-integracion"
        crearVenta(clave)

        var sinRed = true
        val intermitente = Capturador(
            cuerpo = { EXITO },
            fallo = { if (sinRed) SocketTimeoutExceptionSimulado() else null }
        )

        registrarEnviadorVenta(
            ApplicationProvider.getApplicationContext(),
            db = db,
            api = api(intermitente)
        )

        // Primer intento: falla por red, la operacion queda reintentable.
        assertTrue(procesarPendientes(db.outboxDao()))

        val trasFallo = db.outboxDao().obtenerElegibles(
            EstadoOutbox.PENDIENTE.valor,
            System.currentTimeMillis()
        )
        assertEquals(1, trasFallo.size)

        // Segundo intento: ahora responde bien.
        sinRed = false
        assertFalse(procesarPendientes(db.outboxDao()))

        // Misma clave en ambos intentos y una sola venta local.
        assertEquals(2, intermitente.peticiones.size)
        assertTrue(intermitente.peticiones.all { it.idempotencyKey == clave })
        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(0, db.outboxDao().contar())
    }

    // --- Definitivo preservado a traves del mecanismo real ---

    @Test
    fun `un 400 deja la venta definitiva y no reintenta por el registro real`() = runBlocking {
        val clave = "clave-definitiva-integracion"
        crearVenta(clave)

        val rechazo = Capturador(
            cuerpo = { """{"ok":false,"mensaje":"producto inexistente"}""" },
            codigo = { 400 }
        )

        registrarEnviadorVenta(
            ApplicationProvider.getApplicationContext(),
            db = db,
            api = api(rechazo)
        )

        assertFalse(procesarPendientes(db.outboxDao()))
        assertFalse(procesarPendientes(db.outboxDao()))

        // Solo un intento: la definitiva no se reenvia.
        assertEquals(1, rechazo.peticiones.size)

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
        assertEquals(EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor, venta.estadoLocal)

        val op = db.outboxDao().obtenerTodas().single()
        assertEquals(EstadoOutbox.ERROR.valor, op.estado)
        assertTrue(
            db.outboxDao()
                .obtenerElegibles(EstadoOutbox.PENDIENTE.valor, System.currentTimeMillis())
                .isEmpty()
        )
    }

    private fun SocketTimeoutExceptionSimulado(): Throwable =
        java.net.SocketTimeoutException("timeout simulado")

    private companion object {
        const val EXITO = """{"ok":true,"mensaje":null,"venta":{"id":77,"folio":"VTA-000777","total":20.0}}"""
    }
}
