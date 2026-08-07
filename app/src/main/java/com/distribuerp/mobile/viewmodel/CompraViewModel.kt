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
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.models.CompraRequest
import com.distribuerp.mobile.models.ItemCompraRequest
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Proveedor
import com.distribuerp.mobile.repository.CompraRepository
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.ProveedorRepository
import com.distribuerp.mobile.repository.mensajeAmigable

data class ItemCompraTicket(
    val producto: Producto,
    val cantidadTexto: String = "1"
)

class CompraViewModel(
    private val compraRepository: CompraRepository,
    private val proveedorRepository: ProveedorRepository,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    var cargando by mutableStateOf(false)
        private set

    var compras by mutableStateOf<List<Compra>>(emptyList())
        private set

    var proveedores by mutableStateOf<List<Proveedor>>(emptyList())
        private set

    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    var proveedorFiltro by mutableStateOf<Proveedor?>(null)
        private set

    var proveedorSeleccionado by mutableStateOf<Proveedor?>(null)
        private set

    var observaciones by mutableStateOf("")
        private set

    var cargandoDetalle by mutableStateOf(false)
        private set

    var compraActual by mutableStateOf<Compra?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var compraExitosa by mutableStateOf<Compra?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    val items = mutableStateListOf<ItemCompraTicket>()

    fun cargarCompras() {
        cargando = true
        error = null

        compraRepository.obtenerCompras(
            proveedorId = proveedorFiltro?.id,
            onSuccess = { lista ->
                cargando = false
                compras = lista
            },
            onError = { t ->
                cargando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun filtrarPorProveedor(proveedor: Proveedor) {
        if (proveedor.id == proveedorFiltro?.id) {
            return
        }

        proveedorFiltro = proveedor
        cargarCompras()
    }

    fun quitarFiltroProveedor() {
        proveedorFiltro = null
        cargarCompras()
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

        proveedorRepository.obtenerProveedores(
            onSuccess = { lista ->
                proveedores = lista.filter { it.activo }

                if (proveedorSeleccionado == null) {
                    proveedorSeleccionado = proveedores.firstOrNull()
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

    fun seleccionarProveedor(proveedor: Proveedor) {
        proveedorSeleccionado = proveedor
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
            items.add(ItemCompraTicket(producto = producto))
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

    fun registrarCompra() {
        error = null

        if (items.isEmpty()) {
            mensaje = "Debe agregar al menos un producto"
            return
        }

        if (items.any { it.cantidad() <= 0 }) {
            mensaje = "Las cantidades deben ser mayores a cero"
            return
        }

        val proveedorId = proveedorSeleccionado?.id

        if (proveedorId == null) {
            mensaje = "Debe seleccionar un proveedor"
            return
        }

        val productos = items.map { item ->
            ItemCompraRequest(
                producto_id = item.producto.id,
                cantidad = item.cantidad()
            )
        }

        guardando = true

        compraRepository.crearCompra(
            request = CompraRequest(
                proveedor_id = proveedorId,
                observaciones = observaciones.trim().ifBlank { null },
                productos = productos
            ),
            onSuccess = { compra ->
                guardando = false
                compraExitosa = compra
                items.clear()
                observaciones = ""
            },
            onError = { t ->
                guardando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun cargarCompra(id: Int) {
        cargandoDetalle = true
        error = null

        compraRepository.obtenerCompra(
            id = id,
            onSuccess = { compra ->
                cargandoDetalle = false
                compraActual = compra
            },
            onError = { t ->
                cargandoDetalle = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun limpiarCompraExitosa() {
        compraExitosa = null
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
                CompraViewModel(
                    compraRepository = CompraRepository(),
                    proveedorRepository = ProveedorRepository(),
                    productoRepository = ProductoRepository()
                )
            }
        }
    }
}

private fun ItemCompraTicket.cantidad(): Double {
    return cantidadTexto.toDoubleOrNull() ?: 0.0
}

