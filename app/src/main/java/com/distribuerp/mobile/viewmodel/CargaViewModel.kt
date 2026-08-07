package com.distribuerp.mobile.viewmodel

import java.util.Locale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.models.CargaRequest
import com.distribuerp.mobile.models.ItemCargaRequest
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.repository.CargaRepository
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.mensajeAmigable

data class ItemCargaTicket(
    val producto: Producto,
    val cantidadTexto: String = "1"
)

class CargaViewModel(
    private val cargaRepository: CargaRepository,
    private val vendedorRepository: VendedorRepository,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var cargas by mutableStateOf<List<Carga>>(emptyList())
        private set

    var vendedores by mutableStateOf<List<Vendedor>>(emptyList())
        private set

    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    var vendedorFiltro by mutableStateOf<Vendedor?>(null)
        private set

    var vendedorSeleccionado by mutableStateOf<Vendedor?>(null)
        private set

    var observaciones by mutableStateOf("")
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var cargaActual by mutableStateOf<Carga?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var cargaExitosa by mutableStateOf<Carga?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    val items = mutableStateListOf<ItemCargaTicket>()

    fun cargarCargas() {
        cargando = true
        error = null

        cargaRepository.obtenerCargas(
            vendedorId = vendedorFiltro?.id,
            onSuccess = { lista ->
                cargando = false
                cargas = lista
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun filtrarPorVendedor(vendedor: Vendedor) {
        if (vendedor.id == vendedorFiltro?.id) {
            return
        }

        vendedorFiltro = vendedor
        cargarCargas()
    }

    fun quitarFiltroVendedor() {
        vendedorFiltro = null
        cargarCargas()
    }

    fun cargarFormulario() {
        cargando = true
        error = null

        var pendientes = 2

        fun terminar() {
            pendientes -= 1

            if (pendientes == 0) {
                cargando = false
            }
        }

        vendedorRepository.obtenerVendedores(
            onSuccess = { lista ->
                vendedores = lista

                if (vendedorSeleccionado == null) {
                    vendedorSeleccionado = lista.firstOrNull()
                }

                terminar()
            },
            onError = { t ->
                error = mensajeAmigable(t)
                terminar()
            }
        )

        productoRepository.obtenerProductos(
            onSuccess = { lista ->
                productos = lista.filter { it.activo }
                terminar()
            },
            onError = { t ->
                error = mensajeAmigable(t)
                terminar()
            }
        )
    }

    fun seleccionarVendedor(vendedor: Vendedor) {
        vendedorSeleccionado = vendedor
    }

    fun cambiarObservaciones(texto: String) {
        observaciones = texto
    }

    fun agregarProducto(producto: Producto) {
        error = null

        val existente = items.firstOrNull {
            it.producto.id == producto.id
        }

        if (existente != null) {
            val nueva = String.format(
                Locale.US,
                "%.2f",
                existente.cantidad() + 1
            )

            val indice = items.indexOf(existente)
            items[indice] = existente.copy(
                cantidadTexto = nueva
            )
        } else {
            items.add(ItemCargaTicket(producto = producto))
        }
    }

    fun eliminarItem(indice: Int) {
        items.removeAt(indice)
    }

    fun cambiarCantidad(indice: Int, texto: String) {
        if (indice in items.indices) {
            items[indice] = items[indice].copy(
                cantidadTexto = texto.filter {
                    it.isDigit() || it == '.'
                }
            )
        }
    }

    fun incrementarCantidad(indice: Int) {
        if (indice !in items.indices) {
            return
        }

        val item = items[indice]
        items[indice] = item.copy(
            cantidadTexto = (item.cantidad() + 1).toInt().toString()
        )
    }

    fun decrementarCantidad(indice: Int) {
        if (indice !in items.indices) {
            return
        }

        val item = items[indice]
        val nueva = item.cantidad() - 1

        if (nueva < 1) {
            return
        }

        items[indice] = item.copy(
            cantidadTexto = nueva.toInt().toString()
        )
    }

    fun registrarCarga() {
        error = null

        if (items.isEmpty()) {
            mensaje = "Debe agregar al menos un producto"
            return
        }

        if (items.any { it.cantidad() <= 0 }) {
            mensaje = "Las cantidades deben ser mayores a cero"
            return
        }

        val vendedorId = vendedorSeleccionado?.id

        if (vendedorId == null) {
            mensaje = "Debe seleccionar un vendedor"
            return
        }

        val productos = items.map { item ->
            ItemCargaRequest(
                producto_id = item.producto.id,
                cantidad = item.cantidad()
            )
        }

        guardando = true

        cargaRepository.crearCarga(
            request = CargaRequest(
                vendedor_id = vendedorId,
                observaciones = observaciones.trim().ifBlank { null },
                productos = productos
            ),
            onSuccess = { carga ->
                guardando = false
                cargaExitosa = carga
                items.clear()
                observaciones = ""
            },
            onError = { t ->
                guardando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cargarCarga(id: Int) {
        cargandoDetalle = true
        error = null

        cargaRepository.obtenerCarga(
            id = id,
            onSuccess = { carga ->
                cargandoDetalle = false
                cargaActual = carga
            },
            onError = { t ->
                cargandoDetalle = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun limpiarCargaExitosa() {
        cargaExitosa = null
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    fun limpiarError() {
        error = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CargaViewModel(
                    cargaRepository = CargaRepository(),
                    vendedorRepository = VendedorRepository(),
                    productoRepository = ProductoRepository()
                )
            }
        }
    }
}

private fun ItemCargaTicket.cantidad(): Double {
    return cantidadTexto.toDoubleOrNull() ?: 0.0
}

