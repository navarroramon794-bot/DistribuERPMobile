package com.distribuerp.mobile.viewmodel

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.UsuarioGuardado
import com.distribuerp.mobile.models.LoginResponse
import com.distribuerp.mobile.models.Usuario
import com.distribuerp.mobile.sync.SyncManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Response

/**
 * Login exitoso => sesion guardada + sincronizacion reencolada (fix UAT-05).
 *
 * El worker observa `RetrofitClient.tieneCookies()`: sin cookie no envia. Tras
 * un login correcto OkHttp ya aplico la cookie al jar y aqui debe reencolarse
 * `sync_general` (politica KEEP) para retomar las ventas pendientes sin que
 * queden bloqueadas esperando un reinicio.
 *
 * Se usa el seam `AuthViewModel.loginApi` (mismo patron que
 * `SyncManager.encolar`) para no depender de red.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelLoginTest {

    private val context: Application =
        ApplicationProvider.getApplicationContext<Application>()

    private var encolarOriginal:
        ((Context, String, ExistingWorkPolicy, OneTimeWorkRequest) -> Unit)? = null

    @Before
    fun setUp() {
        encolarOriginal = SyncManager.encolar
        runBlocking { SessionManager(context).cerrarSesion() }
    }

    @After
    fun tearDown() {
        SyncManager.encolar = encolarOriginal!!
    }

    private class FakeCall(
        private val bloque: () -> Response<LoginResponse>
    ) : Call<LoginResponse> {

        override fun execute(): Response<LoginResponse> = bloque()

        override fun enqueue(callback: retrofit2.Callback<LoginResponse>) {
            try {
                callback.onResponse(this, bloque())
            } catch (e: Throwable) {
                callback.onFailure(this, e)
            }
        }

        override fun clone(): Call<LoginResponse> = this
        override fun isExecuted(): Boolean = false
        override fun cancel() = Unit
        override fun isCanceled(): Boolean = false

        override fun request(): okhttp3.Request =
            okhttp3.Request.Builder()
                .url("http://localhost/api/login")
                .build()

        override fun timeout(): okio.Timeout = okio.Timeout()
    }

    /** Espera (con idle del main looper) hasta que la condicion se cumpla. */
    private fun esperarCondicion(predicado: () -> Boolean): Boolean {
        val limite = System.currentTimeMillis() + 5000L

        while (System.currentTimeMillis() < limite) {
            shadowOf(Looper.getMainLooper()).idle()
            if (predicado()) return true
            Thread.sleep(20)
        }
        return false
    }

    /** Espera (con idle del main looper) a que la sesion quede guardada. */
    private fun esperarSesionGuardada(): UsuarioGuardado? {
        val limite = System.currentTimeMillis() + 5000L
        var leida: UsuarioGuardado? = null

        while (System.currentTimeMillis() < limite) {
            shadowOf(Looper.getMainLooper()).idle()
            leida = runBlocking { SessionManager(context).sesion.firstOrNull() }
            if (leida != null) return leida
            Thread.sleep(20)
        }
        return leida
    }

    @Test
    fun `un login exitoso guarda la sesion y reencola la sincronizacion`() = runBlocking {
        val encolados = mutableListOf<Pair<String, ExistingWorkPolicy>>()
        SyncManager.encolar = { _, nombre, politica, _ ->
            encolados += nombre to politica
        }

        val viewModel = AuthViewModel(context, SessionManager(context))
        viewModel.loginApi = { _ ->
            FakeCall {
                Response.success(
                    LoginResponse(
                        ok = true,
                        mensaje = "bienvenido",
                        usuario = Usuario(
                            id = 42,
                            nombre = "Vendedor",
                            correo = "vendedor@prueba.com",
                            rol = "VENDEDOR",
                            vendedor_id = 5,
                            empresa_id = 1
                        )
                    )
                )
            }
        }

        viewModel.login("vendedor@prueba.com", "password")

        val guardada = esperarSesionGuardada()
        assertEquals(
            "El login exitoso debe guardar la identidad del usuario",
            "42",
            guardada?.id
        )

        val reencolo = esperarCondicion {
            encolados.any {
                it.first == SyncManager.TRABAJO_GENERAL &&
                    it.second == ExistingWorkPolicy.KEEP
            }
        }
        assertTrue(
            "Tras el login debe encolarse la sincronizacion con politica KEEP",
            reencolo
        )
    }
}