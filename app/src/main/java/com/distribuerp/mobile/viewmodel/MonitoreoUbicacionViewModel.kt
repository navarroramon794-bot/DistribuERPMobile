package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.repository.UbicacionRepository
import com.distribuerp.mobile.utils.GpsConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MonitoreoUbicacionViewModel(
    private val repository: UbicacionRepository
) : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var ubicaciones by mutableStateOf<List<Ubicacion>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var filtroVendedorId by mutableStateOf<Int?>(null)
        private set

    private var pollingJob: Job? = null

    val ubicacionesFiltradas: List<Ubicacion>
        get() = if (filtroVendedorId == null) ubicaciones else ubicaciones.filter { it.vendedor_id == filtroVendedorId }

    fun setFiltro(vendedorId: Int?) {
        filtroVendedorId = vendedorId
    }

    fun estadoAgrupado(fechaIso: String?): (String, String) {
        if (fechaIso == null) return ("sin ubicación", "NO_LOCATION")
        return try {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            val fecha = java.time.OffsetDateTime.parse(fechaIso, fmt)
            val ahora = java.time.OffsetDateTime.now()
            val diffMin = java.time.Duration.between(fecha, ahora).toMinutes()
            when {
                diffMin < 2 -> ("reciente", "RECENT")
                diffMin <= 10 -> ("antigua", "OLD")
                else -> ("desactualizada", "STALE")
            }
        } catch (_: Exception) {
            ("sin ubicación", "NO_LOCATION")
        }
    }

    fun iniciarPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                cargarUbicaciones()
                delay(GpsConfig.ADMIN_POLL_INTERVAL_MS)
            }
        }
    }

    fun detenerPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun cargarUbicaciones() {
        cargando = true
        error = null

        viewModelScope.launch {
            repository.obtenerUbicacionesActivas(
                onSuccess = { lista ->
                    cargando = false
                    if (lista != null) {
                        ubicaciones = lista
                    }
                    error = null
                },
                onError = { t ->
                    cargando = false
                    ubicaciones = emptyList()
                    error = t.message
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MonitoreoUbicacionViewModel(
                    repository = UbicacionRepository()
                )
            }
        }
    }
}