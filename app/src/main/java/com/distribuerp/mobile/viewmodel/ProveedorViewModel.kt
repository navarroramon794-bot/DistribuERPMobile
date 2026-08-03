package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Proveedor
import com.distribuerp.mobile.models.ProveedorRequest
import com.distribuerp.mobile.repository.ProveedorRepository
import com.distribuerp.mobile.repository.mensajeAmigable

class ProveedorViewModel(
    private val repository: ProveedorRepository
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var proveedores by mutableStateOf<List<Proveedor>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var proveedorActual by mutableStateOf<Proveedor?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var eliminando by mutableStateOf(false)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarProveedores() {
        loading = true
        error = null

        repository.obtenerProveedores(
            onSuccess = { lista ->
                loading = false
                proveedores = lista
            },
            onError = { t ->
                loading = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cargarProveedor(id: Int) {
        cargandoDetalle = true
        error = null

        repository.obtenerProveedor(
            id = id,
            onSuccess = { proveedor ->
                cargandoDetalle = false
                proveedorActual = proveedor
            },
            onError = { t ->
                cargandoDetalle = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun guardarProveedor(
        id: Int?,
        request: ProveedorRequest,
        onExito: () -> Unit
    ) {
        guardando = true
        mensaje = null

        val alExito: (Proveedor) -> Unit = { proveedor ->
            guardando = false
            proveedorActual = proveedor
            onExito()
        }

        val alError: (Throwable) -> Unit = { t ->
            guardando = false
            mensaje = mensajeAmigable(t)
        }

        if (id == null) {
            repository.crearProveedor(
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        } else {
            repository.actualizarProveedor(
                id = id,
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        }
    }

    fun eliminarProveedor(
        id: Int,
        onExito: () -> Unit
    ) {
        eliminando = true
        mensaje = null

        repository.eliminarProveedor(
            id = id,
            onSuccess = {
                eliminando = false
                proveedorActual = null
                onExito()
            },
            onError = { t ->
                eliminando = false
                mensaje = mensajeAmigable(t)
            }
        )
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProveedorViewModel(
                    repository = ProveedorRepository()
                )
            }
        }
    }
}
