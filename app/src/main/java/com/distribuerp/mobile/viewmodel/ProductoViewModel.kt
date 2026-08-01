package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.ProductoRequest
import com.distribuerp.mobile.repository.ProductoRepository

class ProductoViewModel(
    private val repository: ProductoRepository
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var productoActual by mutableStateOf<Producto?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var eliminando by mutableStateOf(false)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarProductos() {
        loading = true
        error = null

        repository.obtenerProductos(
            onSuccess = { lista ->
                loading = false
                productos = lista
            },
            onError = { t ->
                loading = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun cargarProducto(id: Int) {
        cargandoDetalle = true
        error = null

        repository.obtenerProducto(
            id = id,
            onSuccess = { producto ->
                cargandoDetalle = false
                productoActual = producto
            },
            onError = { t ->
                cargandoDetalle = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun guardarProducto(
        id: Int?,
        request: ProductoRequest,
        onExito: () -> Unit
    ) {
        guardando = true
        mensaje = null

        val alExito: (Producto) -> Unit = { producto ->
            guardando = false
            productoActual = producto
            onExito()
        }

        val alError: (Throwable) -> Unit = { t ->
            guardando = false
            mensaje = t.message ?: "Error de conexión"
        }

        if (id == null) {
            repository.crearProducto(
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        } else {
            repository.actualizarProducto(
                id = id,
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        }
    }

    fun eliminarProducto(
        id: Int,
        onExito: () -> Unit
    ) {
        eliminando = true
        mensaje = null

        repository.eliminarProducto(
            id = id,
            onSuccess = {
                eliminando = false
                productoActual = null
                onExito()
            },
            onError = { t ->
                eliminando = false
                mensaje = t.message ?: "Error de conexión"
            }
        )
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProductoViewModel(
                    repository = ProductoRepository()
                )
            }
        }
    }
}
