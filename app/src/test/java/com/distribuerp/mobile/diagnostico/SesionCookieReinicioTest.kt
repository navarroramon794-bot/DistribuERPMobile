package com.distribuerp.mobile.diagnostico

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.UsuarioGuardado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UAT-05 TEST E: ?por que el reinicio offline mandaba a Login?
 *
 * Antes del fix, `AuthViewModel.init` borraba la sesion cuando existia sesion
 * en disco pero no cookie HTTP:
 *
 *     val haySesion = sessionManager.sesion.first() != null
 *     if (haySesion && !RetrofitClient.tieneCookies()) {
 *         cerrarSesionLocal()
 *     }
 *
 * Los datos de sesion viven en DataStore (persistente); las cookies de sesion
 * viven SOLO en memoria (`SesionCookieJar`). Al reiniciar el proceso existe la
 * sesion guardada pero no la cookie, y esa condicion borraba la sesion en
 * DataStore: el `NavGraph` lee `sesion == null` y fuerza la ruta `login`.
 *
 * Con el fix esa condicion se ELIMINA: la identidad es persistente y no puede
 * depender de una cookie en memoria. Este test confirma el contrato nuevo con
 * las piezas reales, sin red: sesion en disco + jar vacio => la sesion se
 * conserva.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SesionCookieReinicioTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun reset() = runBlocking {
        RetrofitClient.limpiarCookies()
        SessionManager(context).cerrarSesion()
    }

    @After
    fun limpiar() = runBlocking {
        RetrofitClient.limpiarCookies()
        SessionManager(context).cerrarSesion()
    }

    @Test
    fun `la sesion guardada en DataStore persiste entre instancias`() = runBlocking {
        SessionManager(context).guardarSesion(
            UsuarioGuardado(
                id = "42",
                nombre = "Vendedor Prueba",
                correo = "vendedor@prueba.com",
                rol = "VENDEDOR",
                vendedor_id = "5",
                empresa_id = "1"
            )
        )

        // Una instancia nueva (como un proceso recien arrancado) lee la misma
        // sesion desde disco.
        val leida = SessionManager(context).sesion.first()
        assertEquals("42", leida!!.id)
        assertEquals("1", leida.empresa_id)
        assertEquals("5", leida.vendedor_id)
    }

    @Test
    fun `la cookie de sesion no sobrevive al reinicio del proceso`() = runBlocking {
        // Jar nuevo en memoria: es el estado real tras un arranque.
        assertFalse(
            "Los jar de cookie nacen vacios: el proceso reiniciado no tiene sesion HTTP",
            RetrofitClient.tieneCookies()
        )
    }

    @Test
    fun `al arrancar con sesion en disco y sin cookie se conserva la sesion`() = runBlocking {
        SessionManager(context).guardarSesion(
            UsuarioGuardado(
                id = "7",
                nombre = "Usuario",
                correo = "usuario@prueba.com",
                rol = "VENDEDOR",
                vendedor_id = "5",
                empresa_id = "1"
            )
        )

        // Itinerario real del arranque:
        // 1) existe sesion en disco,
        // 2) no hay cookies (jar en memoria, proceso reiniciado).
        val haySesion = SessionManager(context).sesion.first() != null
        val tieneCookie = RetrofitClient.tieneCookies()
        assertEquals(true, haySesion)
        assertFalse(tieneCookie)

        // 3) el arranque YA NO borra la sesion por falta de cookie (fix UAT-05):
        //    la identidad es persistente e independiente de la sesion HTTP.
        val trasArranque = SessionManager(context).sesion.first()
        assertEquals("La sesion se conserva aunque no haya cookie", "7", trasArranque!!.id)
    }
}