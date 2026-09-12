package com.distribuerp.mobile.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.distribuerp.mobile.R
import com.distribuerp.mobile.data.UbicacionProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.UbicacionPendienteEntity
import com.distribuerp.mobile.repository.UbicacionRepository
import com.distribuerp.mobile.utils.GpsConfig
import com.distribuerp.mobile.worker.LocationSyncWorker
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationCallback: LocationCallback? = null
    private var lastSendTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 13+: verificar POST_NOTIFICATIONS antes de foreground
        if (Build.VERSION.SDK_INT >= 33) {
            val hasNotif = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                // No iniciar tracking, detener servicio y notificar al usuario al volver
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (!tienePermisoFine()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isLocationEnabled()) {
            // GPS apagado: mostrar notificación de error y no iniciar tracking
            val n = buildNotificationError("GPS desactivado — activa la ubicación")
            startForeground(GpsConfig.NOTIFICATION_ID, n)
            return START_STICKY
        }

        val notification = buildNotification()
        startForeground(GpsConfig.NOTIFICATION_ID, notification)
        // Determinar estado real fg/bg
        val isFg = isAppInForeground()
        startTracking(isForeground = isFg)
        return START_STICKY
    }

    override fun onDestroy() {
        removeUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            GpsConfig.NOTIFICATION_CHANNEL_ID,
            "Ubicación en tiempo real",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, GpsConfig.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("DistribuERP — Compartiendo ubicación")
            .setContentText("Tu ubicación se comparte con el administrador")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun buildNotificationError(texto: String): Notification {
        return NotificationCompat.Builder(this, GpsConfig.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("DistribuERP — Ubicación no disponible")
            .setContentText(texto)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(false)
            .build()
    }

    private fun tienePermisoFine(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun tienePermisoBackground(): Boolean {
        return if (Build.VERSION.SDK_INT >= 29) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pkg = packageName
        val procs = am.runningAppProcesses ?: return true
        for (p in procs) {
            if (p.processName == pkg) {
                return p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
            }
        }
        return false
    }

    private fun removeUpdates() {
        locationCallback?.let {
            try {
                LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it)
            } catch (_: Exception) {}
        }
        locationCallback = null
    }

    private fun startTracking(isForeground: Boolean) {
        // Evitar múltiples callbacks
        removeUpdates()

        // Si no hay permiso fine, no iniciar
        if (!tienePermisoFine()) {
            stopSelf()
            return
        }
        // En background sin permiso background, no iniciar (Android 10+)
        if (!isForeground && !tienePermisoBackground()) {
            // Mantener notificación pero sin tracking hasta que se conceda
            return
        }
        if (!isLocationEnabled()) {
            return
        }

        val interval = if (isForeground) GpsConfig.FOREGROUND_INTERVAL_MS else GpsConfig.BACKGROUND_INTERVAL_MS
        val fastest = if (isForeground) GpsConfig.FASTEST_INTERVAL_FG_MS else GpsConfig.FASTEST_INTERVAL_BG_MS

        val request = LocationRequest.Builder(GpsConfig.PRIORITY, interval).apply {
            setMinUpdateIntervalMillis(fastest)
            setMinUpdateDistanceMeters(GpsConfig.MIN_DISPLACEMENT_M)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val now = System.currentTimeMillis()
                // Criterio único: Fused ya filtra por 50m; para vendedor inmóvil enviamos al menos cada STATIONARY_INTERVAL
                val tiempoDesdeUltimo = now - lastSendTimeMs
                val debeEnviarPorTiempo = lastSendTimeMs == 0L || tiempoDesdeUltimo >= GpsConfig.STATIONARY_INTERVAL_MS
                // Si no ha pasado el tiempo estacionario y Fused no entregó por distancia, no hay callback; si entregó, es porque se movió >=50m
                // Por tanto, enviamos siempre que llega callback (movimiento) o por tiempo estacionario
                if (!debeEnviarPorTiempo && lastSendTimeMs != 0L) {
                    // Este callback llegó por movimiento (>=50m), enviar
                } else if (lastSendTimeMs != 0L && tiempoDesdeUltimo < GpsConfig.STATIONARY_INTERVAL_MS) {
                    // Si llegó por intervalo temporal sin movimiento suficiente, Fused no debería haber entregado; aun así enviamos si es por tiempo
                }

                lastSendTimeMs = now

                val precision = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null
                val velocidad = if (loc.hasSpeed()) loc.speed.toDouble() else null
                val fecha = UbicacionProvider.fechaIsoActual()

                scope.launch {
                    try {
                        UbicacionRepository().enviarUbicacion(
                            com.distribuerp.mobile.models.UbicacionRequest(
                                latitud = loc.latitude,
                                longitud = loc.longitude,
                                precision_m = precision,
                                velocidad = velocidad,
                                fuente = "fused",
                                fecha = fecha
                            ),
                            onSuccess = {},
                            onError = { t ->
                                scope.launch {
                                    val msg = t.message ?: ""
                                    val esAuth = msg.contains("401") || msg.contains("403") || msg.contains("400")
                                    if (!esAuth && (t is java.io.IOException || msg.contains("timeout") || msg.contains("Unable to resolve host"))) {
                                        try {
                                            val db = AppDatabase.getInstance(applicationContext)
                                            val dao = db.ubicacionPendienteDao()
                                            dao.insertar(
                                                UbicacionPendienteEntity(
                                                    vendedorId = 0,
                                                    latitud = loc.latitude,
                                                    longitud = loc.longitude,
                                                    precisionM = precision,
                                                    velocidad = velocidad,
                                                    fuente = "fused",
                                                    fecha = fecha
                                                )
                                            )
                                            dao.recortarA50(0)
                                            LocationSyncWorker.encolar(applicationContext)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }

        try {
            LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Permiso revocado durante tracking
            removeUpdates()
            // Actualizar notificación y detener
            val n = buildNotificationError("Permiso de ubicación revocado")
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(GpsConfig.NOTIFICATION_ID, n)
            stopSelf()
        }
    }

    /**
     * Llamado externamente cuando la app cambia de foreground/background para reconfigurar intervalo sin recrear servicio.
     */
    fun actualizarIntervaloSegunEstado() {
        val fg = isAppInForeground()
        startTracking(isForeground = fg)
    }

    companion object {
        fun start(context: Context) {
            // Android 14+: no iniciar desde background sin estar en foreground, el caller debe verificar
            val intent = Intent(context, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
}
