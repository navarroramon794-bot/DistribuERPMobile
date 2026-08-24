package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.ItemVentaRequest
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.repository.CobranzaRepository
import com.distribuerp.mobile.repository.ClienteRepository
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.VentaRepository
import com.distribuerp.mobile.repository.mensajeAmigable

data class ItemTicket(
    val producto: Producto,
    val cantidadTexto: String = "1"
)

class VentaViewModel(
    private val vendedorRepository: VendedorRepository,
    private val clienteRepository: ClienteRepository,
    private val productoRepository: ProductoRepository,
    private val ventaRepository: VentaRepository,
    private val cobranzaRepository: CobranzaRepository
) : ViewModel() {

    var cargandoInicial by mutableStateOf(false)
        private set

    var vendedores by mutableStateOf<List<Vendedor>>(emptyList())
        private set

    var clientes by mutableStateOf<List<Cliente>>(emptyList())
        private set

    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    var vendedorSeleccionado by mutableStateOf<Vendedor?>(null)
        private set

    var clienteSeleccionado by mutableStateOf<Cliente?>(null)
        private set

    var formaPagoSeleccionada by mutableStateOf("CONTADO")
        private set

    var cargandoCredito by mutableStateOf(false)
        private set

    var estadoCredito by mutableStateOf<EstadoCuentaResponse?>(null)
        private set

    val creditoDisponible: Double
        get() {
            val cliente = clienteSeleccionado ?: return 0.0
            val ec = estadoCredito
            val utilizado = ec?.total_vendido
                ?.let { ec.total_cobrado?.let { c -> it - c } }
                ?: 0.0
            return (cliente.limite_credito - utilizado)
                .coerceAtLeast(0.0)
        }

    val items = mutableStateListOf<ItemTicket>()

    var guardando by mutableStateOf(false)
        private set

    var ventaExitosa by mutableStateOf<Venta?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    val total: Double
        get() = items.sumOf {

            it.cantidad() * it.producto.precio
        }

    fun cargarDatos() {
        cargandoInicial = true
        error = null

        var pendientes = 3

        fun terminar() {
            pendientes -= 1

            if (pendientes == 0) {
                cargandoInicial = false
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

        clienteRepository.obtenerClientes(
            onSuccess = { lista ->
                clientes = lista

                if (clienteSeleccionado == null) {
                    clienteSeleccionado = lista.firstOrNull()
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
                productos = lista
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

    fun seleccionarCliente(cliente: Cliente) {
        clienteSeleccionado = cliente
        estadoCredito = null

        if (formaPagoSeleccionada == "CREDITO") {
            cargarInfoCredito()
        }
    }

    fun seleccionarFormaPago(forma: String) {
        if (forma == formaPagoSeleccionada) return

        formaPagoSeleccionada = forma
        estadoCredito = null

        if (forma == "CREDITO" && clienteSeleccionado != null) {
            cargarInfoCredito()
        }
    }

    private fun cargarInfoCredito() {
        val cliente = clienteSeleccionado ?: return

        cargandoCredito = true

        cobranzaRepository.obtenerEstadoCuenta(
            clienteId = cliente.id,
            onSuccess = { respuesta ->
                cargandoCredito = false
                estadoCredito = respuesta
            },
            onError = {
                cargandoCredito = false
                estadoCredito = null
            }
        )
    }

    fun agregarProducto(producto: Producto) {
        error = null

        val existente = items.firstOrNull { it.producto.id == producto.id }

        if (existente != null) {
            val nueva = java.lang.String.format(
                java.util.Locale.US,
                "%.2f",
                existente.cantidad() + 1
            )

            val indice = items.indexOf(existente)
            items[indice] = existente.copy(cantidadTexto = nueva)
        } else {
            items.add(
                ItemTicket(producto = producto)
            )
        }
    }

    fun eliminarItem(indice: Int) {
        items.removeAt(indice)
    }

    fun cambiarCantidad(indice: Int, texto: String) {
        if (indice in items.indices) {
            items[indice] = items[indice].copy(
                cantidadTexto = texto.filter { it.isDigit() || it == '.' }
            )
        }
    }

    fun incrementarCantidad(indice: Int) {
        if (indice !in items.indices) {
            return
        }

        val item = items[indice]

        val nueva = item.cantidad() + 1
        items[indice] = item.copy(
            cantidadTexto = nueva.toInt().toString()
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

    fun registrarVenta() {
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
        val clienteId = clienteSeleccionado?.id

        if (vendedorId == null || clienteId == null) {
            mensaje = "Debe seleccionar vendedor y cliente"
            return
        }

        val cliente = clienteSeleccionado!!

        if (formaPagoSeleccionada == "CREDITO") {
            if (!cliente.credito_autorizado) {
                mensaje =
                    "El cliente no tiene crédito autorizado."
                return
            }

            if (cliente.bloqueado) {
                mensaje =
                    "El cliente está bloqueado para " +
                    "compras a crédito."
                return
            }

            if (total > creditoDisponible) {
                mensaje =
                    "Crédito insuficiente. " +
                    "Disponible: $creditoDisponible. " +
                    "Venta: $total."
                return
            }
        }

        val productos = items.map { item ->
            ItemVentaRequest(
                producto_id = item.producto.id,
                cantidad = item.cantidad()
            )
        }

        guardando = true

        ventaRepository.crearVenta(
            request = VentaRequest(
                cliente_id = clienteId,
                vendedor_id = vendedorId,
                productos = productos,
                forma_pago = formaPagoSeleccionada
            ),
            onSuccess = { venta ->
                guardando = false
                ventaExitosa = venta
                items.clear()
            },
            onError = { t ->
                guardando = false
                error = mensajeAmigable(t)
            }
        )
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    fun mostrarMensaje(texto: String) {
        error = null
        mensaje = texto
    }

    fun limpiarError() {
        error = null
    }

    fun limpiarVentaExitosa() {
        ventaExitosa = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VentaViewModel(
                    vendedorRepository = VendedorRepository(),
                    clienteRepository = ClienteRepository(),
                    productoRepository = ProductoRepository(),
                    ventaRepository = VentaRepository(),
                    cobranzaRepository = CobranzaRepository()
                )
            }
        }
    }
}

private fun ItemTicket.cantidad(): Double {
    return cantidadTexto.toDoubleOrNull() ?: 0.0
}
