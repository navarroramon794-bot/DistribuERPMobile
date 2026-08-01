package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.ClienteRequest
import com.distribuerp.mobile.repository.ClienteRepository

class ClienteViewModel(
    private val repository: ClienteRepository
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var clientes by mutableStateOf<List<Cliente>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var clienteActual by mutableStateOf<Cliente?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var eliminando by mutableStateOf(false)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarClientes() {
        loading = true
        error = null

        repository.obtenerClientes(
            onSuccess = { lista ->
                loading = false
                clientes = lista
            },
            onError = { t ->
                loading = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun cargarCliente(id: Int) {
        cargandoDetalle = true
        error = null

        repository.obtenerCliente(
            id = id,
            onSuccess = { cliente ->
                cargandoDetalle = false
                clienteActual = cliente
            },
            onError = { t ->
                cargandoDetalle = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun guardarCliente(
        id: Int?,
        request: ClienteRequest,
        onExito: () -> Unit
    ) {
        guardando = true
        mensaje = null

        val alExito: (Cliente) -> Unit = { cliente ->
            guardando = false
            clienteActual = cliente
            onExito()
        }

        val alError: (Throwable) -> Unit = { t ->
            guardando = false
            mensaje = t.message ?: "Error de conexión"
        }

        if (id == null) {
            repository.crearCliente(
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        } else {
            repository.actualizarCliente(
                id = id,
                request = request,
                onSuccess = alExito,
                onError = alError
            )
        }
    }

    fun eliminarCliente(
        id: Int,
        onExito: () -> Unit
    ) {
        eliminando = true
        mensaje = null

        repository.eliminarCliente(
            id = id,
            onSuccess = {
                eliminando = false
                clienteActual = null
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
                ClienteViewModel(
                    repository = ClienteRepository()
                )
            }
        }
    }
}
