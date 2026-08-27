package com.distribuerp.mobile.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.data.UbicacionProvider
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.models.UbicacionRequest
import com.distribuerp.mobile.repository.UbicacionRepository
import com.distribuerp.mobile.repository.mensajeAmigable
import kotlinx.coroutines.launch

class UbicacionViewModel(
    private val provider: UbicacionProvider?,
    private val repository: UbicacionRepository
) : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var ubicacionActual by mutableStateOf<Ubicacion?>(null)
        private set

    var ultimaUbicacion by mutableStateOf<Ubicacion?>(null)
        private set

    var activo by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun tienePermisoUbicacion(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun obtenerUbicacionActual() {
        val p = provider
        if (p == null) {
            error = "Proveedor de ubicación no disponible."
            return
        }

        cargando = true
        error = null
        mensaje = null

        viewModelScope.launch {
            try {
                val ubicacionDispositivo = p.obtenerUbicacionActual()
                ubicacionActual = Ubicacion(
                    vendedor_id = null,
                    vendedor = null,
                    latitud = ubicacionDispositivo.latitud,
                    longitud = ubicacionDispositivo.longitud,
                    precision_m = ubicacionDispositivo.precision_m,
                    velocidad = ubicacionDispositivo.velocidad,
                    fuente = ubicacionDispositivo.fuente,
                    fecha = UbicacionProvider.fechaIsoActual(),
                    activo = activo
                )
                cargando = false
            } catch (t: Throwable) {
                cargando = false
                error = mensajeAmigable(t)
            }
        }
    }

    fun enviarUbicacionActual() {
        val ubicacion = ubicacionActual
        if (ubicacion == null) {
            error = "Primero obtén la ubicación actual."
            return
        }

        cargando = true
        error = null
        mensaje = null

        val request = UbicacionRequest(
            latitud = ubicacion.latitud,
            longitud = ubicacion.longitud,
            precision_m = ubicacion.precision_m,
            velocidad = ubicacion.velocidad,
            fuente = ubicacion.fuente ?: "gps",
            fecha = ubicacion.fecha
        )

        repository.enviarUbicacion(
            request = request,
            onSuccess = { guardada ->
                cargando = false
                ultimaUbicacion = guardada
                mensaje = "Ubicación enviada correctamente."
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cambiarEstadoUbicacion(nuevoEstado: Boolean) {
        cargando = true
        error = null
        mensaje = null

        repository.cambiarEstadoUbicacion(
            activo = nuevoEstado,
            onSuccess = { estado ->
                cargando = false
                activo = estado
                mensaje = if (estado) {
                    "Compartir ubicación activado."
                } else {
                    "Compartir ubicación desactivado."
                }
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cargarUltimaUbicacion(vendedorId: Int) {
        cargando = true
        error = null
        mensaje = null

        repository.obtenerUltimaUbicacion(
            vendedorId = vendedorId,
            onSuccess = { ubicacion ->
                cargando = false
                ultimaUbicacion = ubicacion
                if (ubicacion != null) {
                    activo = ubicacion.activo
                }
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun limpiarMensajes() {
        error = null
        mensaje = null
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                UbicacionViewModel(
                    provider = UbicacionProvider(context),
                    repository = UbicacionRepository()
                )
            }
        }
    }
}
