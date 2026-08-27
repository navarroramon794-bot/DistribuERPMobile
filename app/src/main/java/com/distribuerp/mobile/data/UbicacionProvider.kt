package com.distribuerp.mobile.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UbicacionDispositivo(
    val latitud: Double,
    val longitud: Double,
    val precision_m: Double?,
    val velocidad: Double?,
    val fuente: String
)

class UbicacionProvider(context: Context) {

    private val cliente: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): UbicacionDispositivo {
        return suspendCancellableCoroutine { continuation ->

            val solicitud = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                10000L
            ).apply {
                setWaitForAccurateLocation(true)
                setMinUpdateIntervalMillis(5000L)
            }.build()

            val callback = object : LocationCallback() {

                override fun onLocationResult(result: LocationResult) {
                    val ubicacion = result.lastLocation
                    if (ubicacion != null) {
                        cliente.removeLocationUpdates(this)
                        continuation.resume(mapearUbicacion(ubicacion))
                    }
                }
            }

            cliente.requestLocationUpdates(
                solicitud,
                callback,
                Looper.getMainLooper()
            )

            continuation.invokeOnCancellation {
                cliente.removeLocationUpdates(callback)
            }
        }
    }

    private fun mapearUbicacion(location: Location): UbicacionDispositivo {
        return UbicacionDispositivo(
            latitud = location.latitude,
            longitud = location.longitude,
            precision_m = if (location.hasAccuracy()) {
                location.accuracy.toDouble()
            } else {
                null
            },
            velocidad = if (location.hasSpeed()) {
                location.speed.toDouble()
            } else {
                null
            },
            fuente = "fused"
        )
    }

    companion object {

        fun fechaIsoActual(): String {
            return DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                .withZone(ZoneOffset.systemDefault())
                .format(Instant.now())
        }
    }
}
