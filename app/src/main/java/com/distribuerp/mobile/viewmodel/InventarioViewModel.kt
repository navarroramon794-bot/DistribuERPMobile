package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.ItemInventario
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.repository.InventarioRepository
import com.distribuerp.mobile.repository.VendedorRepository

class InventarioViewModel(
    private val inventarioRepository: InventarioRepository,
    private val vendedorRepository: VendedorRepository
) : ViewModel() {

    var cargandoVendedores by mutableStateOf(false)
        private set

    var vendedores by mutableStateOf<List<Vendedor>>(emptyList())
        private set

    var cargandoInventario by mutableStateOf(false)
        private set

    var inventario by mutableStateOf<List<ItemInventario>>(emptyList())
        private set

    var vendedorSeleccionado by mutableStateOf<Vendedor?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarVendedores() {
        cargandoVendedores = true
        error = null

        vendedorRepository.obtenerVendedores(
            onSuccess = { lista ->
                cargandoVendedores = false
                vendedores = lista

                if (vendedorSeleccionado == null) {
                    vendedorSeleccionado = lista.firstOrNull()

                    lista.firstOrNull()?.let { vendedor ->
                        cargarInventario(vendedor.id)
                    }
                }
            },
            onError = { t ->
                cargandoVendedores = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun seleccionarVendedor(vendedor: Vendedor) {
        if (vendedor.id == vendedorSeleccionado?.id) {
            return
        }

        vendedorSeleccionado = vendedor
        cargarInventario(vendedor.id)
    }

    fun cargarInventario(vendedorId: Int) {
        cargandoInventario = true
        error = null

        inventarioRepository.obtenerInventario(
            vendedorId = vendedorId,
            onSuccess = { respuesta ->
                cargandoInventario = false
                inventario = respuesta.inventario.orEmpty()

                respuesta.vendedor?.let { vendedor ->
                    vendedorSeleccionado = vendedor
                }

                respuesta.mensaje?.let { mensaje ->
                    this.mensaje = mensaje
                }
            },
            onError = { t ->
                cargandoInventario = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun recargar() {
        vendedorSeleccionado?.let { vendedor ->
            cargarInventario(vendedor.id)
        }
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InventarioViewModel(
                    inventarioRepository = InventarioRepository(),
                    vendedorRepository = VendedorRepository()
                )
            }
        }
    }
}
