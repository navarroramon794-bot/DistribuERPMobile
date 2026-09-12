package com.distribuerp.mobile.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
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
import com.distribuerp.mobile.service.LocationForegroundService
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

    fun tienePermisoNotificacion(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun tienePermisoBackground(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 29) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun debeIniciarTracking(context: Context, vendedorId: Int?, esVendedor: Boolean, vendedorActivo: Boolean = true): Boolean {
        if (!esVendedor) return false
        if (vendedorId == null) return false
        if (!vendedorActivo) return false
        if (!activo) return false
        if (!tienePermisoUbicacion(context)) return false
        if (!isGpsEnabled(context)) return false
        return true
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
        cambiarEstadoUbicacion(null, nuevoEstado)
    }

    fun cambiarEstadoUbicacion(context: Context?, nuevoEstado: Boolean) {
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
                // Integrar ForegroundService
                if (context != null) {
                    if (estado) {
                        if (tienePermisoUbicacion(context) && isGpsEnabled(context)) {
                            // POST_NOTIFICATIONS es recomendado pero no bloqueante para el flujo; el servicio maneja su ausencia
                            LocationForegroundService.start(context)
                        } else {
                            error = if (!tienePermisoUbicacion(context)) "Falta permiso de ubicación" else "GPS desactivado"
                        }
                    } else {
                        LocationForegroundService.stop(context)
                    }
                }
                // Punto de integración Etapa 3: si falla por red, se encolará en Room/WorkManager
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun sincronizarTracking(context: Context, vendedorId: Int?, esVendedor: Boolean) {
        if (debeIniciarTracking(context, vendedorId, esVendedor)) {
            if (tienePermisoNotificacion(context) || Build.VERSION.SDK_INT < 33) {
                LocationForegroundService.start(context)
            }
        } else {
            // Si no debe trackear, asegurar servicio detenido si estaba activo por estado previo
            if (!activo) {
                LocationForegroundService.stop(context)
            }
        }
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
