package com.distribuerp.mobile.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.UsuarioGuardado
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.models.EmpresaDatos
import com.distribuerp.mobile.models.EmpresaResponse
import com.distribuerp.mobile.models.LoginRequest
import com.distribuerp.mobile.models.LoginResponse
import com.distribuerp.mobile.models.PingResponse
import com.distribuerp.mobile.repository.mensajeAmigable
import com.distribuerp.mobile.sync.SyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class LoginUiState(
    val enviando: Boolean = false,
    val mensaje: String = ""
)

class AuthViewModel(
    private val application: Application,
    private val sessionManager: SessionManager
) : ViewModel() {

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    var pingEstado by mutableStateOf("Servidor: Conectando...")
        private set

    var empresa by mutableStateOf<EmpresaDatos?>(null)
        private set

    /**
     * `true` cuando el backend respondio 401/302 pese a haber sesion local: la
     * cookie de sesion HTTP falta o expiro. No se borra la identidad ni las
     * ventas; solo se avisa de que hay que volver a entrar para reanudar la
     * sincronizacion.
     */
    var authPerdida by mutableStateOf(false)
        private set

    /**
     * Cantidad de operaciones Outbox pendientes de enviar. Solo se usa como
     * aviso en la pantalla de login: el conteo no es critico.
     */
    var ventasPendientes by mutableStateOf(0)
        private set

    val sesion =
        sessionManager.sesion.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        RetrofitClient.onSessionExpirada = {
            // Un 401/302 no borra la identidad: conserva la sesion, las ventas
            // y la outbox. La sincronizacion se retoma al volver a entrar.
            viewModelScope.launch {
                val haySesion =
                    sessionManager.sesion.first() != null
                if (haySesion) {
                    authPerdida = true
                }
            }
        }

        actualizarPendientes()
        checkServidor()
    }

    private fun actualizarPendientes() {
        viewModelScope.launch {
            ventasPendientes = try {
                AppDatabase.getInstance(application)
                    .outboxDao()
                    .contarPorEstado(EstadoOutbox.PENDIENTE.valor)
            } catch (_: Exception) {
                0
            }
        }
    }

    /**
     * Seam de pruebas: sustituye la llamada HTTP real del login por un `Call`
     * falso, mismo patron que `SyncManager.encolar`. Permite verificar que un
     * login exitoso guarda la sesion y reencola la sincronizacion sin red.
     */
    internal var loginApi: (LoginRequest) -> Call<LoginResponse> =
        { datos -> RetrofitClient.api.login(datos) }

    fun checkServidor() {
        RetrofitClient.api
            .ping()
            .enqueue(
                object : Callback<PingResponse> {

                    override fun onResponse(
                        call: Call<PingResponse>,
                        response: Response<PingResponse>
                    ) {
                        Log.d("API", "CA3digo: ${response.code()}")
                        Log.d("API", "Body: ${response.body()}")

                        pingEstado =
                            if (response.isSuccessful) {
                                "Servidor conectado âo."
                            } else {
                                "HTTP ${response.code()} â?O"
                            }
                    }

                    override fun onFailure(
                        call: Call<PingResponse>,
                        t: Throwable
                    ) {
                        Log.e("API", "ERROR", t)

                        pingEstado = mensajeAmigable(t)
                    }
                }
            )
    }

    private fun cargarEmpresa() {
        RetrofitClient.api
            .getEmpresa()
            .enqueue(
                object : Callback<EmpresaResponse> {

                    override fun onResponse(
                        call: Call<EmpresaResponse>,
                        response: Response<EmpresaResponse>
                    ) {
                        val body = response.body()

                        if (response.isSuccessful &&
                            body != null &&
                            body.ok
                        ) {
                            empresa = body.datos
                        } else {
                            empresa = null
                            Log.w(
                                "EMPRESA",
                                "Respuesta invA¡lida: HTTP ${response.code()}"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<EmpresaResponse>,
                        t: Throwable
                    ) {
                        empresa = null
                        Log.e("EMPRESA", "Error al cargar empresa", t)
                    }
                }
            )
    }

    fun login(correo: String, password: String) {
        if (loginUiState.enviando) return

        loginUiState = loginUiState.copy(
            enviando = true,
            mensaje = "Validando..."
        )

        val datos = LoginRequest(
            correo = correo,
            password = password
        )

        loginApi(datos)
            .enqueue(
                object : Callback<LoginResponse> {

                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        Log.d("LOGIN", "HTTP: ${response.code()}")
                        Log.d("LOGIN", "BODY: ${response.body()}")
                        Log.d("LOGIN", "ERROR: ${response.errorBody()?.string()}")

                        when (response.code()) {

                            200 -> {
                                val usuario = response.body()?.usuario
                                if (usuario != null) {
                                    viewModelScope.launch {
                                        sessionManager.guardarSesion(
                                            UsuarioGuardado(
                                                id = usuario.id.toString(),
                                                nombre = usuario.nombre,
                                                correo = usuario.correo,
                                                rol = usuario.rol,
                                                vendedor_id =
                                                    usuario.vendedor_id
                                                        ?.toString(),
                                                vendedor = usuario.vendedor,
                                                empresa_id = usuario.empresa_id?.toString(),
                                                password_temporal = usuario.password_temporal
                                            )
                                        )
                                        // La cookie ya esta en el jar (OkHttp la
                                        // aplico al recibir la respuesta): se
                                        // persiste cifrada para que sobreviva al
                                        // reinicio del proceso.
                                        RetrofitClient.persistirCookies()
                                        authPerdida = false
                                        actualizarPendientes()
                                        cargarEmpresa()

                                        // De inmediato se retoma lo que quedo
                                        // pendiente de la sesion anterior.
                                        SyncManager.encolarSincronizacion(application)

                                        loginUiState = loginUiState.copy(
                                            enviando = false,
                                            mensaje = "Bienvenido ${usuario.nombre}"
                                        )
                                    }
                                } else {
                                    loginUiState = loginUiState.copy(
                                        enviando = false,
                                        mensaje = "Respuesta invA¡lida del servidor"
                                    )
                                }
                            }

                            401 -> {
                                loginUiState = loginUiState.copy(
                                    enviando = false,
                                    mensaje = "Credenciales invA¡lidas â?O"
                                )
                            }

                            500 -> {
                                loginUiState = loginUiState.copy(
                                    enviando = false,
                                    mensaje = "Error de servidor (500). Revisa Render"
                                )
                            }

                            else -> {
                                loginUiState = loginUiState.copy(
                                    enviando = false,
                                    mensaje = "Error HTTP ${response.code()}"
                                )
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<LoginResponse>,
                        t: Throwable
                    ) {
                        loginUiState = loginUiState.copy(
                            enviando = false,
                            mensaje = mensajeAmigable(t)
                        )
                    }
                }
            )
    }

    fun cerrarSesion() {
        RetrofitClient.api
            .logout()
            .enqueue(
                object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                    }

                    override fun onFailure(
                        call: Call<ResponseBody>,
                        t: Throwable
                    ) {
                    }
                }
            )

        cerrarSesionLocal()
    }

    private fun cerrarSesionLocal() {
        viewModelScope.launch {
            RetrofitClient.limpiarCookies()
            RetrofitClient.limpiarCookiesPersistidas()
            sessionManager.cerrarSesion()
            authPerdida = false
            empresa = null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                AuthViewModel(
                    application = app,
                    sessionManager = SessionManager(app)
                )
            }
        }
    }
}

