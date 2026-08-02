package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.models.VendedorRequest
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.mensajeAmigable

class VendedorViewModel(
    private val repository: VendedorRepository
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var vendedores by mutableStateOf<List<Vendedor>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var vendedorActual by mutableStateOf<Vendedor?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var eliminando by mutableStateOf(false)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarVendedores() {
        loading = true
        error = null

        repository.obtenerVendedores(
            onSuccess = { lista ->
                loading = false
                vendedores = lista
            },
            onError = { t ->
                loading = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cargarVendedor(id: Int) {
        cargandoDetalle = true
        error = null

        repository.obtenerVendedor(
            id = id,
            onSuccess = { vendedor ->
                cargandoDetalle = false
                vendedorActual = vendedor
            },
            onError = { t ->
                cargandoDetalle = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun guardarVendedor(
        id: Int?,
        request: VendedorRequest,
        onExito: () -> Unit
    ) {
        guardando = true
        mensaje = null

        val alExito: (Vendedor) -> Unit = { vendedor ->
            guardando = false
            vendedorActual = vendedor
            onExito()
        }

        val alError: (Throwable) -> Unit = { t ->
            guardando = false
            mensaje = mensajeAmigable(t)
        }

        if (id == null) {
            repository.crearVendedor(
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        } else {
            repository.actualizarVendedor(
                id = id,
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        }
    }

    fun eliminarVendedor(
        id: Int,
        onExito: () -> Unit
    ) {
        eliminando = true
        mensaje = null

        repository.eliminarVendedor(
            id = id,
            onSuccess = {
                eliminando = false
                vendedorActual = null
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
                VendedorViewModel(
                    repository = VendedorRepository()
                )
            }
        }
    }
}
