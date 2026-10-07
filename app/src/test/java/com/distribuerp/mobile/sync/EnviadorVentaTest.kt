package com.distribuerp.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.EstadoVentaSincronizacion
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.models.VentaResponse
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.VentaPendienteRepository
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
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
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnviadorVentaTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: VentaPendienteRepository

    /**
     * `Call` falsa de Retrofit.
     *
     * `timeout()` devuelve `okio.Timeout` segun la firma real de Retrofit 2.11
     * (verificado con javap), no `okhttp3.Call.Timeout`.
     */
    private class FakeCall : Call<VentaResponse> {

        private val bloque: () -> Response<VentaResponse>

        constructor(bloque: () -> Response<VentaResponse>) {
            this.bloque = bloque
        }

        override fun execute(): Response<VentaResponse> = bloque()

        override fun enqueue(callback: retrofit2.Callback<VentaResponse>) {
            try {
                callback.onResponse(this, bloque())
            } catch (e: Throwable) {
                callback.onFailure(this, e)
            }
        }

        override fun clone(): Call<VentaResponse> = this
        override fun isExecuted(): Boolean = false
        override fun cancel() = Unit
        override fun isCanceled(): Boolean = false

        override fun request(): okhttp3.Request = okhttp3.Request.Builder()
            .url("http://localhost/api/ventas")
            .build()

        override fun timeout(): okio.Timeout = okio.Timeout()
    }

    /** Api falsa que registra cabeceras y permite fijar respuesta o fallo. */
    private class ApiFake : EnviadorVentaApi {

        val cabeceras = mutableListOf<Map<String, String>>()
        val formasPago = mutableListOf<String>()

        var respuesta: (() -> Response<VentaResponse>)? = null
        var fallo: Throwable? = null

        override fun crearVenta(
            request: VentaRequest,
            headers: Map<String, String>
        ): Call<VentaResponse> {
            cabeceras += headers
            formasPago += request.forma_pago

            return FakeCall {
                fallo?.let { throw it }
                respuesta!!.invoke()
            }
        }
    }

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

    private fun linea() = LineaSnapshot(
        productoId = 1,
        cantidad = 2.0,
        precioUnitario = 10.0,
        subtotal = 20.0
    )

    private fun okResponse(folio: String = "VTA-000001", id: Int = 42) =
        Response.success(
            VentaResponse(
                ok = true,
                mensaje = null,
                venta = Venta(
                    id = id,
                    folio = folio,
                    fecha = "2026-01-01",
                    cliente_id = 9,
                    cliente = "Cliente",
                    vendedor_id = 5,
                    vendedor = "Vendedor",
                    total = 20.0,
                    items = emptyList()
                )
            )
        )

    private fun errorResponse(codigo: Int, mensaje: String): Response<VentaResponse> {
        val cuerpo = ResponseBody.create(
            MediaType.parse("application/json"),
            """{"ok":false,"mensaje":"$mensaje"}"""
        )
        // Retrofit solo admite HTTP >= 400 en Response.error(int, body); para un
        // 302 se construye la respuesta OkHttp cruda y se usa
        // Response.error(ResponseBody, rawResponse), que acepta cualquier
        // codigo fuera de 2xx.
        return if (codigo < 400) {
            val request = okhttp3.Request.Builder()
                .url("http://localhost/api/ventas")
                .build()
            val raw = okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(codigo)
                .message("Redirect")
                .body(cuerpo)
                .build()
            Response.error(cuerpo, raw)
        } else {
            Response.error(codigo, cuerpo)
        }
    }

    private suspend fun crearVenta(
        empresaId: Int = 1,
        clave: String? = null
    ) = repo.crearVentaOffline(
        empresaId = empresaId,
        vendedorId = 5,
        clienteId = 9,
        formaPago = "CONTADO",
        lineas = listOf(linea()),
        clientOperationId = clave ?: "clave-${empresaId}-${contador++}"
    )

    private var contador = 0

    private suspend fun opDeEmpresa(
        empresaId: Int,
        clientOperationId: String
    ) = db.outboxDao().obtenerTodas().first {
        it.payload.contains(clientOperationId)
    }

    @Test
    fun `envia la clientOperationId como X-Idempotency-Key`() = runBlocking {
        val creada = crearVenta(clave = "clave-header")
        val api = ApiFake().apply { respuesta = { okResponse() } }
        val op = db.outboxDao().obtenerTodas()[0]

        EnviadorVenta(api, db).enviar(op)

        assertEquals(creada.clientOperationId, api.cabeceras[0]["X-Idempotency-Key"])
    }

    @Test
    fun `reintentos reutilizan la misma clave de idempotencia`() = runBlocking {
        crearVenta(clave = "clave-reintento")
        val api = ApiFake()
        val op = db.outboxDao().obtenerTodas()[0]
        val enviador = EnviadorVenta(api, db)

        api.fallo = SocketTimeoutException("timeout")
        assertTrue(enviador.enviar(op) is ResultadoEnvio.Retry)

        api.fallo = null
        api.respuesta = { okResponse() }
        assertTrue(enviador.enviar(op) is ResultadoEnvio.Confirmado)

        assertEquals(2, api.cabeceras.size)
        assertEquals("clave-reintento", api.cabeceras[0]["X-Idempotency-Key"])
        assertEquals(
            api.cabeceras[0]["X-Idempotency-Key"],
            api.cabeceras[1]["X-Idempotency-Key"]
        )
    }

    @Test
    fun `un 200 marca la venta sincronizada con folio y ventaId del backend`() = runBlocking {
        crearVenta(clave = "clave-ok")
        val api = ApiFake().apply { respuesta = { okResponse("VTA-000321", 99) } }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.Confirmado)

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals("SINCRONIZADA", venta.estadoLocal)
        assertEquals("VTA-000321", venta.folioBackend)
        assertEquals(99, venta.ventaIdBackend)
        assertNull(venta.error)
    }

    @Test
    fun `un rechazo definitivo conserva la venta en ERROR_DEFINITIVO`() = runBlocking {
        crearVenta(clave = "clave-400")
        val api = ApiFake().apply { respuesta = { errorResponse(400, "sin existencia") } }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.Descartar)

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals(EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor, venta.estadoLocal)
        assertNotNull(venta.error)
        assertTrue(venta.error!!.contains("sin existencia"))
        assertNull(venta.folioBackend)
    }

    @Test
    fun `un 401 devuelve AuthRequerida y conserva la venta recuperable`() = runBlocking {
        crearVenta(clave = "clave-401")
        val api = ApiFake().apply { respuesta = { errorResponse(401, "no autorizado") } }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.AuthRequerida)
        assertEquals(401, (resultado as ResultadoEnvio.AuthRequerida).codigo)
        assertEquals(
            EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `un 302 devuelve AuthRequerida y conserva la venta recuperable`() = runBlocking {
        crearVenta(clave = "clave-302")
        val api = ApiFake().apply { respuesta = { errorResponse(302, "redirige") } }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.AuthRequerida)
        assertEquals(302, (resultado as ResultadoEnvio.AuthRequerida).codigo)
        assertEquals(
            EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `un 409 por conflicto de idempotencia es definitivo`() = runBlocking {
        crearVenta(clave = "clave-409")
        val api = ApiFake().apply {
            respuesta = { errorResponse(409, "clave usada con otro contenido") }
        }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.Descartar)
        assertEquals(
            EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor,
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `un 500 es recuperable y conserva la venta`() = runBlocking {
        crearVenta(clave = "clave-500")
        val api = ApiFake().apply { respuesta = { errorResponse(500, "error interno") } }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.Retry)
        assertEquals(
            EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `un 408 y un 429 son recuperables`() = runBlocking {
        crearVenta(clave = "clave-transiente")
        val op = db.outboxDao().obtenerTodas()[0]

        for (codigo in listOf(408, 429)) {
            val api = ApiFake().apply { respuesta = { errorResponse(codigo, "reintenta") } }
            val resultado = EnviadorVenta(api, db).enviar(op)
            assertTrue("HTTP $codigo deberia ser recuperable", resultado is ResultadoEnvio.Retry)
        }
    }

    @Test
    fun `un IOException conserva la venta y pide reintento`() = runBlocking {
        crearVenta(clave = "clave-io")
        val api = ApiFake().apply { fallo = IOException("sin red") }
        val op = db.outboxDao().obtenerTodas()[0]

        val resultado = EnviadorVenta(api, db).enviar(op)

        assertTrue(resultado is ResultadoEnvio.Retry)
        assertEquals(
            EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `un payload ilegible no borra la venta ni la outbox`() = runBlocking {
        crearVenta(clave = "clave-payload")
        val op = db.outboxDao().obtenerTodas()[0]
        val api = ApiFake().apply { respuesta = { okResponse() } }

        val resultado = EnviadorVenta(api, db).enviar(op.copy(payload = "no-es-json"))

        assertTrue(resultado is ResultadoEnvio.Descartar)
        assertEquals(1, db.outboxDao().contar())
        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
    }

    @Test
    fun `el enviador no toca la venta de otra empresa`() = runBlocking {
        crearVenta(empresaId = 1, clave = "clave-e1")
        crearVenta(empresaId = 2, clave = "clave-e2")

        val opEmpresa1 = opDeEmpresa(1, "clave-e1")
        val api = ApiFake().apply { respuesta = { errorResponse(400, "rechazada") } }

        EnviadorVenta(api, db).enviar(opEmpresa1)

        assertEquals(
            "ERROR_DEFINITIVO",
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
        assertEquals(
            "PENDIENTE_SYNC",
            db.ventaPendienteDao().obtenerPorEmpresa(2)[0].estadoLocal
        )
    }

    @Test
    fun `procesarPendientes elimina la outbox confirmada`() = runBlocking {
        crearVenta(clave = "clave-sync-ok")
        val api = ApiFake().apply { respuesta = { okResponse() } }

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))
        )

        assertEquals(false, huboRetry)
        assertEquals(0, db.outboxDao().contar())
        assertEquals(
            "SINCRONIZADA",
            db.ventaPendienteDao().obtenerPorEmpresa(1)[0].estadoLocal
        )
    }

    @Test
    fun `procesarPendientes deja la outbox en ERROR con el motivo`() = runBlocking {
        crearVenta(clave = "clave-sync-400")
        val api = ApiFake().apply { respuesta = { errorResponse(400, "sin existencia") } }

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))
        )

        assertEquals(false, huboRetry)
        val op = db.outboxDao().obtenerTodas()[0]
        assertEquals(EstadoOutbox.ERROR.valor, op.estado)
        assertEquals(1, op.intentos)
        assertNotNull(op.ultimo_error)
    }

    @Test
    fun `el payload enviado conserva la forma de pago CONTADO`() = runBlocking {
        crearVenta(clave = "clave-forma")
        val api = ApiFake().apply { respuesta = { okResponse() } }
        val op = db.outboxDao().obtenerTodas()[0]

        EnviadorVenta(api, db).enviar(op)

        assertEquals(listOf("CONTADO"), api.formasPago)
    }

    // --- Retry de ventas: recuperable reintenta, definitivo no ---

    @Test
    fun `A una venta con timeout queda elegible para retry`() = runBlocking {
        crearVenta(clave = "clave-retry-a")
        val api = ApiFake().apply { fallo = SocketTimeoutException("timeout") }
        val op = db.outboxDao().obtenerTodas()[0]

        val huboRetry = procesarPendientes(
            db.outboxDao(),
            mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))
        )

        assertTrue(huboRetry)
        val tras = db.outboxDao().obtenerPorUuid(op.uuid)
        assertNotNull(tras)
        assertEquals(EstadoOutbox.PENDIENTE.valor, tras!!.estado)
        assertEquals(1, tras.intentos)
    }

    @Test
    fun `B el siguiente sync vuelve a intentar la venta y la confirma`() = runBlocking {
        crearVenta(clave = "clave-retry-b")
        val api = ApiFake()
        val op = db.outboxDao().obtenerTodas()[0]
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        api.fallo = IOException("sin red")
        assertTrue(procesarPendientes(db.outboxDao(), registro))
        assertEquals(1, api.cabeceras.size)
        assertNotNull(db.outboxDao().obtenerPorUuid(op.uuid))

        api.fallo = null
        api.respuesta = { okResponse("VTA-000777", 55) }
        assertFalse(procesarPendientes(db.outboxDao(), registro))

        assertEquals(2, api.cabeceras.size)
        assertEquals(0, db.outboxDao().contar())

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals("SINCRONIZADA", venta.estadoLocal)
        assertEquals("VTA-000777", venta.folioBackend)
    }

    @Test
    fun `D el clientOperationId se mantiene entre retries`() = runBlocking {
        repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), "clave-estable")
        val api = ApiFake()
        val op = db.outboxDao().obtenerTodas()[0]
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        api.fallo = SocketTimeoutException("timeout")
        procesarPendientes(db.outboxDao(), registro)

        api.fallo = SocketTimeoutException("timeout")
        procesarPendientes(db.outboxDao(), registro)

        api.fallo = null
        api.respuesta = { okResponse() }
        procesarPendientes(db.outboxDao(), registro)

        assertEquals(3, api.cabeceras.size)
        assertTrue(
            api.cabeceras.all { it["X-Idempotency-Key"] == "clave-estable" }
        )
        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
    }

    @Test
    fun `C una venta con 400 no entra en retry automatico`() = runBlocking {
        crearVenta(clave = "clave-definitiva")
        val api = ApiFake().apply { respuesta = { errorResponse(400, "sin existencia") } }
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        assertFalse(procesarPendientes(db.outboxDao(), registro))
        assertFalse(procesarPendientes(db.outboxDao(), registro))

        assertEquals(1, api.cabeceras.size)

        val op = db.outboxDao().obtenerTodas()[0]
        assertEquals(EstadoOutbox.ERROR.valor, op.estado)

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals(EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor, venta.estadoLocal)
    }

    @Test
    fun `C una venta con 401 queda PENDIENTE con auth_401 y reintenta tras el login`() =
        runBlocking {
            verificarAuth(401)
        }

    @Test
    fun `C una venta con 302 queda PENDIENTE con auth_302`() =
        runBlocking {
            verificarAuth(302)
        }

    @Test
    fun `C una venta con 403 queda en ERROR_DEFINITIVO y no es elegible para retry`() =
        runBlocking {
            val resultado = verificarDefinitivo(403)

            assertEquals("http_403", resultado.motivo)
        }

    @Test
    fun `C una venta con 404 queda en ERROR_DEFINITIVO y no es elegible para retry`() =
        runBlocking {
            val resultado = verificarDefinitivo(404)

            assertEquals("http_404", resultado.motivo)
        }

    /**
     * Verifica el comportamiento comun de un rechazo definitivo por HTTP:
     * la venta local queda en `ERROR_DEFINITIVO`, la operacion Outbox se
     * conserva en `ERROR` y no vuelve a aparecer entre las elegibles.
     *
     * Cada test invoca este helper sobre su propia base creada en `setUp()`,
     * sin compartir filas con iteraciones anteriores.
     */
    private suspend fun verificarDefinitivo(codigo: Int): ResultadoEnvio.Descartar {
        crearVenta(clave = "clave-$codigo")

        val api = ApiFake().apply { respuesta = { errorResponse(codigo, "rechazada") } }
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        assertFalse(
            "HTTP $codigo no debe pedir reintento",
            procesarPendientes(db.outboxDao(), registro)
        )

        // Segundo sync: la definitiva no debe volver a enviarse.
        assertFalse(
            "HTTP $codigo no debe reintentarse",
            procesarPendientes(db.outboxDao(), registro)
        )

        assertEquals("HTTP $codigo debe intentar enviarse una sola vez", 1, api.cabeceras.size)

        // La operacion se conserva para traza, en estado ERROR.
        assertEquals(1, db.outboxDao().contar())
        val op = db.outboxDao().obtenerElegibles(EstadoOutbox.PENDIENTE.valor, System.currentTimeMillis())
        assertTrue("La definitiva no debe seguir siendo elegible", op.isEmpty())

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals(
            EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor,
            venta.estadoLocal
        )

        val entidad = db.outboxDao().obtenerTodas().single()
        assertEquals(EstadoOutbox.ERROR.valor, entidad.estado)
        assertEquals("http_$codigo", entidad.ultimo_error)

        return ResultadoEnvio.Descartar(entidad.ultimo_error!!)
    }

    /**
     * Verifica el comportamiento comun de una sesion perdida por HTTP:
     * la venta local queda en `ERROR_RECUPERABLE`, la operacion Outbox se
     * conserva en `PENDIENTE` con `ultimo_error=auth_<codigo>` y sigue siendo
     * elegible para el siguiente sync (ya no es definitiva, cambio de UAT-05).
     */
    private suspend fun verificarAuth(codigo: Int) {
        crearVenta(clave = "clave-auth-$codigo")

        val api = ApiFake().apply { respuesta = { errorResponse(codigo, "sin sesion") } }
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        assertFalse(
            "HTTP $codigo no debe pedir reintento por WorkManager",
            procesarPendientes(db.outboxDao(), registro)
        )

        val op = db.outboxDao().obtenerTodas()[0]
        assertEquals(
            "Debe conservarse en PENDIENTE",
            EstadoOutbox.PENDIENTE.valor,
            op.estado
        )
        assertEquals("auth_$codigo", op.ultimo_error)
        assertEquals(1, op.intentos)

        // El siguiente sync (disparado al recuperar la sesion) debe reintentarla.
        val elegibles = db.outboxDao()
            .obtenerElegibles(EstadoOutbox.PENDIENTE.valor, System.currentTimeMillis())
        assertTrue(
            "HTTP $codigo debe seguir siendo elegible",
            elegibles.any { it.uuid == op.uuid }
        )

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals(
            EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
            venta.estadoLocal
        )
        assertNull(venta.folioBackend)

        assertEquals("HTTP $codigo debe intentar enviarse una sola vez", 1, api.cabeceras.size)
    }

    @Test
    fun `despues de un 401 la venta se sincroniza cuando vuelve la sesion`() = runBlocking {
        crearVenta(clave = "clave-relogin")
        val api = ApiFake().apply { respuesta = { errorResponse(401, "no autorizado") } }
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        assertFalse(procesarPendientes(db.outboxDao(), registro))
        assertEquals(1, api.cabeceras.size)

        // Vuelve la sesion: el siguiente sync envia y confirma.
        api.respuesta = { okResponse("VTA-000500", 60) }
        assertFalse(procesarPendientes(db.outboxDao(), registro))

        assertEquals(2, api.cabeceras.size)
        assertEquals(0, db.outboxDao().contar())

        val venta = db.ventaPendienteDao().obtenerPorEmpresa(1)[0]
        assertEquals("SINCRONIZADA", venta.estadoLocal)
        assertEquals("VTA-000500", venta.folioBackend)
    }

    @Test
    fun `un timeout no genera venta duplicada tras varios intentos`() = runBlocking {
        repo.crearVentaOffline(1, 5, 9, "CONTADO", listOf(linea()), "clave-sin-duplicado")
        val api = ApiFake()
        val registro = mapOf(TiposOutbox.VENTA to EnviadorVenta(api, db))

        repeat(3) {
            api.fallo = SocketTimeoutException("timeout")
            procesarPendientes(db.outboxDao(), registro)
        }

        api.fallo = null
        api.respuesta = { okResponse("VTA-000999", 88) }
        procesarPendientes(db.outboxDao(), registro)

        assertEquals(1, db.ventaPendienteDao().obtenerPorEmpresa(1).size)
        assertEquals(0, db.outboxDao().contar())
        assertEquals("VTA-000999", db.ventaPendienteDao().obtenerPorEmpresa(1)[0].folioBackend)
    }
}