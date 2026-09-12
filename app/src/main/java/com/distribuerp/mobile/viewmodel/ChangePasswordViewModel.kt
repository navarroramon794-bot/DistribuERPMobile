package com.distribuerp.mobile.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.models.ChangePasswordRequest
import com.distribuerp.mobile.utils.PasswordValidator
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ChangePasswordUiState(
    val passwordActual: String = "",
    val passwordNueva: String = "",
    val confirmacion: String = "",
    val errorActual: String? = null,
    val errorNueva: String? = null,
    val errorConfirmacion: String? = null,
    val mensaje: String = "",
    val enviando: Boolean = false,
    val exito: Boolean = false
)

class ChangePasswordViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    var uiState by mutableStateOf(ChangePasswordUiState())
        private set

    fun onActualChange(v: String) {
        uiState = uiState.copy(passwordActual = v, errorActual = null, mensaje = "")
    }

    fun onNuevaChange(v: String) {
        uiState = uiState.copy(passwordNueva = v, errorNueva = null, mensaje = "")
    }

    fun onConfirmacionChange(v: String) {
        uiState = uiState.copy(confirmacion = v, errorConfirmacion = null, mensaje = "")
    }

    private fun validarLocal(): Boolean {
        var ok = true
        if (uiState.passwordActual.isBlank()) {
            uiState = uiState.copy(errorActual = "La contraseña actual es obligatoria.")
            ok = false
        }
        val errNueva = PasswordValidator.validar(uiState.passwordNueva)
        if (errNueva != null) {
            uiState = uiState.copy(errorNueva = errNueva)
            ok = false
        }
        val errConf = PasswordValidator.validarConfirmacion(uiState.passwordNueva, uiState.confirmacion)
        if (errConf != null) {
            uiState = uiState.copy(errorConfirmacion = errConf)
            ok = false
        }
        if (uiState.passwordActual == uiState.passwordNueva && uiState.passwordNueva.isNotEmpty()) {
            uiState = uiState.copy(errorNueva = "La nueva contraseña debe ser diferente a la actual.")
            ok = false
        }
        return ok
    }

    fun cambiarPassword(onSuccess: () -> Unit) {
        if (!validarLocal()) return
        if (uiState.enviando) return

        uiState = uiState.copy(enviando = true, mensaje = "")

        val body = ChangePasswordRequest(
            password_actual = uiState.passwordActual,
            password_nueva = uiState.passwordNueva,
            password_nueva_confirmacion = uiState.confirmacion
        )

        RetrofitClient.api.cambiarPassword(body).enqueue(object : Callback<com.distribuerp.mobile.models.ChangePasswordResponse> {
            override fun onResponse(
                call: Call<com.distribuerp.mobile.models.ChangePasswordResponse>,
                response: Response<com.distribuerp.mobile.models.ChangePasswordResponse>
            ) {
                when (response.code()) {
                    200 -> {
                        viewModelScope.launch {
                            sessionManager.actualizarPasswordTemporal(false)
                        }
                        uiState = uiState.copy(enviando = false, exito = true, mensaje = response.body()?.mensaje ?: "Contraseña actualizada")
                        onSuccess()
                    }
                    400, 401, 403 -> {
                        val msg = try {
                            response.errorBody()?.string()?.let { raw ->
                                // intentar extraer mensaje del JSON {"mensaje": "..."}
                                Regex("\"mensaje\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1) ?: raw.take(200)
                            } ?: "Error ${response.code()}"
                        } catch (_: Exception) {
                            "Error ${response.code()}"
                        }
                        uiState = uiState.copy(enviando = false, mensaje = msg)
                    }
                    else -> {
                        uiState = uiState.copy(enviando = false, mensaje = "Error HTTP ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<com.distribuerp.mobile.models.ChangePasswordResponse>, t: Throwable) {
                uiState = uiState.copy(enviando = false, mensaje = "Error de red: ${t.message ?: "desconocido"}")
            }
        })
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                ChangePasswordViewModel(SessionManager(app))
            }
        }
    }
}
