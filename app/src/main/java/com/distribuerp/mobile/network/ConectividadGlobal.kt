package com.distribuerp.mobile.network

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de conectividad compartido por la app.
 *
 * Se inicializa una vez desde `MainActivity.onCreate` con [iniciar], que engancha
 * el [NetworkMonitor] real y refleja aqui sus cambios.
 */
object ConectividadGlobal {

    private val _estado = MutableStateFlow(EstadoConectividad.OFFLINE)
    val estado: StateFlow<EstadoConectividad> = _estado.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var monitorActivo: NetworkMonitor? = null
    private var coleccion: Job? = null

    fun setEstado(nuevo: EstadoConectividad) {
        _estado.value = nuevo
    }

    fun estaOnline(): Boolean = _estado.value == EstadoConectividad.ONLINE

    /**
     * Arranca el monitoreo real de conectividad y refleja su estado en [estado].
     *
     * Es idempotente: `MainActivity` se recrea en cada cambio de configuracion y
     * `ConectividadAndroid` registra un `NetworkCallback` permanente, asi que
     * arrancar mas de una vez filtraria callbacks huerfanos.
     */
    fun iniciar(context: Context) {
        iniciar(ConectividadAndroid(context.applicationContext))
    }

    /**
     * Igual que [iniciar], pero con el monitor ya construido, para poder
     * comprobar la propagacion de estado sin depender de la red real.
     */
    internal fun iniciar(monitor: NetworkMonitor) {
        synchronized(this) {
            if (monitorActivo != null) return

            monitorActivo = monitor

            // Solo el monitor real necesita enganchar el callback del sistema.
            (monitor as? ConectividadAndroid)?.iniciar()

            // Estado inicial de inmediato, sin esperar al primer callback.
            _estado.value = monitor.estado.value

            // Sin esta escucha el estado global se quedaria congelado en el valor
            // de arranque: `ConectividadAndroid` actualiza su propio `StateFlow`
            // y nadie lo transfiria aqui, con lo que OFFLINE -> ONLINE no se
            // reflejaria y CREDITO quedaria bloqueado sin razon.
            coleccion = scope.launch {
                monitor.estado.collect { nuevo -> _estado.value = nuevo }
            }
        }
    }

    /** Estado ya inicializado con un monitor real. */
    internal val inicializado: Boolean get() = monitorActivo != null

    /**
     * Detiene el monitoreo y vuelve al estado inicial.
     *
     * Solo para tests: los singletons sobreviven entre casos de la misma JVM.
     */
    internal fun detener() {
        synchronized(this) {
            coleccion?.cancel()
            coleccion = null
            monitorActivo = null
            _estado.value = EstadoConectividad.OFFLINE
        }
    }
}
