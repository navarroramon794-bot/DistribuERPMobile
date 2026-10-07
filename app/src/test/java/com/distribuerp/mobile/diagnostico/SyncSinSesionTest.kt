package com.distribuerp.mobile.diagnostico

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
import com.distribuerp.mobile.sync.EnviadorVenta
import com.distribuerp.mobile.sync.EnviadorVentaApi
import com.distribuerp.mobile.sync.ResultadoEnvio
import com.distribuerp.mobile.sync.procesarPendientes
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

/**
 * UAT-05 TEST B/C: ?puede sincronizar el worker sin sesion?
 *
 * Al arrancar la app despues de un reinicio no existen cookies de sesion: el
 * `CookieJar` es solo memoria y ya no se cierra la sesion por ese motivo
 * (cambio de UAT-05). El worker sincroniza igual (la empresa y el vendedor
 * viajan en el payload de la outbox).
 *
 * Este escenario reproduce el caso en que, aun asi, el backend responde 302
 * (redirect a login) o 401 porque la cookie es invalida o expiro. Con el fix,
 * ninguno de los dos es definitivo: la operacion se conserva en `PENDIENTE`
 * con `ultimo_error=auth_<codigo>`, la venta queda en `ERROR_RECUPERABLE` y se
 * reintenta al recuperar la sesion. La venta nunca se elimina ni se marca
 * sincronizada hasta que vuelva la autenticacion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncSinSesionTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: VentaPendienteRepository

    private data class Peticion(
        val idempotencyKey: String?,
        val cookieHeader: String?
    )

    private inner class ApiFija(
        codigo: Int,
        mensaje: String
    ) {
        val peticiones = mutableListOf<Peticion>()

        val capturador = Interceptor { chain ->
            val request = chain.request()
            peticiones += Peticion(
                idempotencyKey = request.header("X-Idempotency-Key"),
                cookieHeader = request.header("Cookie")
            )

            okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(codigo)
                .message(if (codigo == 302) "Found" else "Error")
                .header("Content-Type", "application/json")
                .body(
                    ResponseBody.create(
                        MediaType.parse("application/json"),
                        """{"ok":false,"mensaje":"$mensaje"}"""
                    )
                )
                .build()
        }

        val api: ApiService = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(OkHttpClient.Builder().addInterceptor(capturador).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val enviador: EnviadorVenta =
            EnviadorVenta(
                api = EnviadorVentaApi { request, headers ->
                    api.crearVenta(request, headers)
                },
                db = db
            )
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

    private suspend fun crearVenta(clave: String = "clave-sin-sesion") =
        repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = clave
        )

    @Test
    fun `B el worker identifica empresa y vendedor desde el payload aunque no haya sesion`() =
        runBlocking {
            val creada = crearVenta("clave-identidad")
            val op = db.outboxDao().obtenerTodas().single()

            val venta = db.ventaPendienteDao().obtenerVentaPorId(creada.ventaIdLocal)!!
            assertEquals(
                "La venta pendiente conserva empresaId sin depender de la sesion",
                1,
                venta.empresaId
            )
            assertEquals(
                "La venta pendiente conserva vendedorId sin depender de la sesion",
                5,
                venta.vendedorId
            )
            assertTrue(op.payload.contains("\"empresaId\":1"))
            assertTrue(op.payload.contains("\"vendedor_id\":5"))
        }

    @Test
    fun `C sin sesion la peticion viaja sin cookie pero con la clave de idempotencia`() =
        runBlocking {
            crearVenta("clave-cookie")
            val apiFija = ApiFija(302, "redirect login")
            val op = db.outboxDao().obtenerTodas().single()

            val resultado = apiFija.enviador.enviar(op)

            assertTrue(resultado is ResultadoEnvio.AuthRequerida)
            assertEquals(302, (resultado as ResultadoEnvio.AuthRequerida).codigo)
            val peticion = apiFija.peticiones.single()
            assertEquals("clave-cookie", peticion.idempotencyKey)
            assertNull(
                "Tras reiniciar no hay cookie de sesion: la peticion va sin autenticar",
                peticion.cookieHeader
            )
        }

    @Test
    fun `C un 302 por sesion inexistente conserva la venta recuperable con auth_302`() =
        runBlocking {
            crearVenta("clave-302")
            val apiFija = ApiFija(302, "redirect login")
            val registro = mapOf(TiposOutbox.VENTA to apiFija.enviador)

            assertFalse(
                "El 302 no debe pedir reintento por WorkManager",
                procesarPendientes(db.outboxDao(), registro)
            )

            val op = db.outboxDao().obtenerTodas().single()
            assertEquals(
                "Tras el 302 la operacion debe seguir PENDIENTE para reintentar",
                EstadoOutbox.PENDIENTE.valor,
                op.estado
            )
            assertEquals("auth_302", op.ultimo_error)
            assertEquals(1, op.intentos)

            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
            assertEquals(
                "El 302 es recuperable: la venta queda en ERROR_RECUPERABLE, no sincronizada",
                EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
                venta.estadoLocal
            )
            assertNull(venta.folioBackend)
        }

    @Test
    fun `C un 401 por sesion inexistente conserva la venta recuperable con auth_401`() =
        runBlocking {
            crearVenta("clave-401")
            val apiFija = ApiFija(401, "no autorizado")
            val registro = mapOf(TiposOutbox.VENTA to apiFija.enviador)

            assertFalse(
                "El 401 no debe pedir reintento por WorkManager",
                procesarPendientes(db.outboxDao(), registro)
            )
            assertFalse(
                "El 401 tampoco debe eliminar la operacion ni marcarla definitiva",
                procesarPendientes(db.outboxDao(), registro)
            )

            val op = db.outboxDao().obtenerTodas().single()
            assertEquals(EstadoOutbox.PENDIENTE.valor, op.estado)
            assertEquals("auth_401", op.ultimo_error)

            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
            assertEquals(
                EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
                venta.estadoLocal
            )
            assertNotNull(venta.error)
            assertTrue(venta.error!!.contains("401"))
            assertNull(venta.folioBackend)
        }

    @Test
    fun `C tras N reintentos por 302 la venta se conserva y nunca se marca sincronizada`() =
        runBlocking {
            crearVenta("clave-302-n")
            val apiFija = ApiFija(302, "redirect login")
            val registro = mapOf(TiposOutbox.VENTA to apiFija.enviador)

            repeat(3) {
                assertFalse(procesarPendientes(db.outboxDao(), registro))
            }

            val op = db.outboxDao().obtenerTodas().single()
            assertEquals(
                "La operacion se conserva en PENDIENTE: nunca se elimina tras un 302",
                EstadoOutbox.PENDIENTE.valor,
                op.estado
            )
            assertEquals("auth_302", op.ultimo_error)
            assertEquals(3, op.intentos)

            val venta = db.ventaPendienteDao().obtenerPorEmpresa(1).single()
            assertEquals(
                "Tras N reintentos por 302 la venta sigue en ERROR_RECUPERABLE",
                EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
                venta.estadoLocal
            )
            assertNull(venta.folioBackend)
        }
}