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
import com.distribuerp.mobile.models.LoginRequest
import com.distribuerp.mobile.models.LoginResponse
import com.distribuerp.mobile.models.PingResponse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class LoginUiState(
    val enviando: Boolean = false,
    val mensaje: String = ""
)

class AuthViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    var pingEstado by mutableStateOf("Servidor: Conectando...")
        private set

    val sesion =
        sessionManager.sesion.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        checkServidor()
    }

    fun checkServidor() {
        RetrofitClient.api
            .ping()
            .enqueue(
                object : Callback<PingResponse> {

                    override fun onResponse(
                        call: Call<PingResponse>,
                        response: Response<PingResponse>
                    ) {
                        Log.d("API", "Código: ${response.code()}")
                        Log.d("API", "Body: ${response.body()}")

                        pingEstado =
                            if (response.isSuccessful) {
                                "Servidor conectado ✅"
                            } else {
                                "HTTP ${response.code()} ❌"
                            }
                    }

                    override fun onFailure(
                        call: Call<PingResponse>,
                        t: Throwable
                    ) {
                        Log.e("API", "ERROR", t)

                        pingEstado =
                            t.javaClass.simpleName +
                                    "\n" +
                                    (t.message ?: "")
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

        RetrofitClient.api
            .login(datos)
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
                                                rol = usuario.rol
                                            )
                                        )
                                        loginUiState = loginUiState.copy(
                                            enviando = false,
                                            mensaje = "Bienvenido ${usuario.nombre}"
                                        )
                                    }
                                } else {
                                    loginUiState = loginUiState.copy(
                                        enviando = false,
                                        mensaje = "Respuesta inválida del servidor"
                                    )
                                }
                            }

                            401 -> {
                                loginUiState = loginUiState.copy(
                                    enviando = false,
                                    mensaje = "Credenciales inválidas ❌"
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
                            mensaje = t.message ?: "Error de conexión"
                        )
                    }
                }
            )
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            sessionManager.cerrarSesion()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                AuthViewModel(
                    sessionManager = SessionManager(app)
                )
            }
        }
    }
}
