package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.DashboardDatos
import com.distribuerp.mobile.repository.DashboardRepository

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    var loading by mutableStateOf(true)
        private set

    var dashboard by mutableStateOf<DashboardDatos?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        loading = true
        error = null

        repository.obtenerResumen(
            onSuccess = { response ->
                loading = false
                dashboard = response.datos
            },
            onError = { t ->
                loading = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    repository = DashboardRepository()
                )
            }
        }
    }
}
