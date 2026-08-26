package com.distribuerp.mobile.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.Dispositivo
import com.distribuerp.mobile.data.LicenciaGuardada
import com.distribuerp.mobile.data.LicenciaManager
import com.distribuerp.mobile.models.ActivarRequest
import com.distribuerp.mobile.models.DemoRequest
import com.distribuerp.mobile.models.LicenciaInfo
import com.distribuerp.mobile.repository.extraerError
import com.distribuerp.mobile.repository.mensajeAmigable
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class EstadoLicencia {

    object Cargando : EstadoLicencia()

    object SinLicencia : EstadoLicencia()

    object Activa : EstadoLicencia()

    object Demo : EstadoLicencia()

    object Expirada : EstadoLicencia()

    object Suspendida : EstadoLicencia()

    object Cancelada : EstadoLicencia()

    object SinInternet : EstadoLicencia()

    object DemoExpirada : EstadoLicencia()

    object Error : EstadoLicencia()
}

class LicenciaViewModel(
    private val app: Application,
    private val manager: LicenciaManager
) : ViewModel() {

    var estado by mutableStateOf<EstadoLicencia>(EstadoLicencia.Cargando)
        private set

    var licencia by mutableStateOf<LicenciaInfo?>(null)
        private set

    var diasRestantes by mutableStateOf<Int?>(null)
        private set

    var cargando by mutableStateOf(false)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    private var androidIdCache: String? = null

    init {
        viewModelScope.launch {
            androidIdCache = Dispositivo.obtenerAndroidId(app)

            validar()
        }
    }

    fun validar() {
        if (cargando) return

        viewModelScope.launch {
            cargando = true
            mensaje = null
            estado = EstadoLicencia.Cargando

            val guardada = manager.obtener()

            if (guardada == null) {
                estado = EstadoLicencia.SinLicencia
                cargando = false
                return@launch
            }

            val validadaEnLinea = verificarEnServidor(guardada.codigo)

            if (validadaEnLinea) {
                cargando = false
                return@launch
            }

            if (dentroDePeriodoGracia(guardada)) {
                aplicarGuardada(guardada)
            } else {
                estado = EstadoLicencia.SinInternet
            }

            cargando = false
        }
    }

    fun activar(codigo: String) {
        if (cargando) return

        val codigoLimpio = codigo.trim().uppercase()

        if (codigoLimpio.isEmpty()) {
            mensaje = "Ingresa el código de licencia."
            return
        }

        viewModelScope.launch {
            cargando = true
            mensaje = null

            try {
                val respuesta = RetrofitClient.api.activarLicencia(
                    ActivarRequest(
                        codigo = codigoLimpio,
                        android_id = androidIdCache,
                        version_app = BuildConfig.APP_VERSION
                    )
                )

                val info = respuesta.licencia

                if (respuesta.ok && info != null) {
                    manager.guardar(info)
                    aplicarInfo(info)
                } else {
                    mensaje = respuesta.mensaje
                        ?: "No se pudo activar la licencia."
                }

            } catch (e: HttpException) {
                mensaje = mensajeHttp(e)
            } catch (e: Exception) {
                mensaje = mensajeAmigable(e)
            }

            cargando = false
        }
    }

    fun solicitarDemo() {
        if (cargando) return

        viewModelScope.launch {
            cargando = true
            mensaje = null

            try {
                val respuesta = RetrofitClient.api.solicitarDemo(
                    DemoRequest(
                        android_id = androidIdCache,
                        version_app = BuildConfig.APP_VERSION
                    )
                )

                val info = respuesta.licencia

                if (respuesta.ok && info != null) {
                    manager.guardar(info)
                    aplicarInfo(info)
                } else {
                    mensaje = respuesta.mensaje
                        ?: "No se pudo iniciar la demostración."
                }

            } catch (e: HttpException) {
                mensaje = mensajeHttp(e)
            } catch (e: Exception) {
                mensaje = mensajeAmigable(e)
            }

            cargando = false
        }
    }

    /** Muestra la pantalla de activación desde un estado bloqueado. */
    fun irAActivacion() {
        mensaje = null
        estado = EstadoLicencia.SinLicencia
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    private suspend fun verificarEnServidor(codigo: String): Boolean {
        return try {
            val respuesta = RetrofitClient.api.verificarLicencia(
                codigo = codigo,
                androidId = androidIdCache,
                versionApp = BuildConfig.APP_VERSION
            )

            val info = respuesta.licencia

            if (respuesta.ok && info != null) {
                manager.guardar(info)
                aplicarInfo(info)
                true
            } else {
                mensaje = respuesta.mensaje
                    ?: "No se pudo validar la licencia."

                estado = EstadoLicencia.Error

                true
            }

        } catch (e: HttpException) {
            mensaje = mensajeHttp(e)
            estado = EstadoLicencia.Error
            true

        } catch (e: IOException) {
            false

        } catch (e: Exception) {
            mensaje = mensajeAmigable(e)
            estado = EstadoLicencia.Error
            true
        }
    }

    private fun aplicarInfo(info: LicenciaInfo) {
        licencia = info
        diasRestantes = info.dias_restantes
        estado = clasificar(info)
    }

    private fun aplicarGuardada(guardada: LicenciaGuardada) {
        val info = guardada.toLicenciaInfo()

        licencia = info
        diasRestantes = guardada.diasRestantes
            ?.takeIf { it >= 0 }
            ?: LicenciaManager.diasRestantes(
                guardada.fechaVencimiento
            )

        estado = clasificar(info)
    }

    private fun mensajeHttp(e: HttpException): String {
        val respuesta = e.response()

        return if (respuesta != null) {
            extraerError(respuesta).message
                ?: "Error HTTP ${e.code()}"
        } else {
            "Error HTTP ${e.code()}"
        }
    }

    private fun clasificar(info: LicenciaInfo): EstadoLicencia {
        val esDemo = info.esDemo
        val estadoServer = info.estado ?: info.estado_guardado

        val restantes = info.dias_restantes

        if (esDemo && restantes != null && restantes <= 0) {
            return EstadoLicencia.DemoExpirada
        }

        return when {
            !info.valida && esDemo ->
                EstadoLicencia.DemoExpirada

            !info.valida && estadoServer == "suspendida" ->
                EstadoLicencia.Suspendida

            !info.valida && estadoServer == "cancelada" ->
                EstadoLicencia.Cancelada

            !info.valida ->
                EstadoLicencia.Expirada

            esDemo ->
                EstadoLicencia.Demo

            else ->
                EstadoLicencia.Activa
        }
    }

    private fun dentroDePeriodoGracia(guardada: LicenciaGuardada): Boolean {
        val ahora = System.currentTimeMillis()

        val horasDesdeCheck = (ahora - guardada.ultimoCheck) / 3600000.0

        if (horasDesdeCheck >
            LicenciaManager.DIAS_GRACIA_SIN_INTERNET * 24.0
        ) {
            return false
        }

        val vencimiento = LicenciaManager.parsearFecha(
            guardada.fechaVencimiento
        ) ?: return true

        val hoy = LocalDate.now()

        val limite = vencimiento.plusDays(
            guardada.diasGracia.toLong()
        )

        return !hoy.isAfter(limite)
    }

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application

                LicenciaViewModel(
                    app = app,
                    manager = LicenciaManager(app)
                )
            }
        }
    }
}
