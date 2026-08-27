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
import com.distribuerp.mobile.repository.mensajeAmigable
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

    fun cargarUbicaciones() {
        cargando = true
        error = null

        viewModelScope.launch {
            repository.obtenerUbicacionesActivas(
                onSuccess = { lista ->
                    cargando = false
                    ubicaciones = lista ?: emptyList()
                    error = null
                },
                onError = { t ->
                    cargando = false
                    ubicaciones = emptyList()
                    error = mensajeAmigable(t)
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
