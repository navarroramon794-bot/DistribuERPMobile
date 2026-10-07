package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.app.Application
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.ItemVentaRequest
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.network.ConectividadGlobal
import com.distribuerp.mobile.repository.CobranzaRepository
import com.distribuerp.mobile.repository.ClienteRepository
import com.distribuerp.mobile.repository.ErrorApiException
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.ProductoRepository
import com.distribuerp.mobile.repository.VendedorRepository
import com.distribuerp.mobile.repository.VentaPendienteRepository
import com.distribuerp.mobile.repository.VentaRepository
import com.distribuerp.mobile.repository.mensajeAmigable
import com.distribuerp.mobile.sync.SyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

data class ItemTicket(
    val producto: Producto,
    val cantidadTexto: String = "1"
)

class VentaViewModel(
    private val vendedorRepository: VendedorRepository,
    private val clienteRepository: ClienteRepository,
    private val productoRepository: ProductoRepository,
    private val ventaRepository: VentaRepository,
    private val cobranzaRepository: CobranzaRepository,
    private val sessionManager: SessionManager,
    private val appContext: android.content.Context
) : ViewModel() {

    private val ventaPendienteRepository: VentaPendienteRepository by lazy {
        VentaPendienteRepository(AppDatabase.getInstance(appContext))
    }

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

    /**
     * Registra la venta.
     *
     * CONTADO es offline-first: si no hay red, o si el envio falla por un
     * problema de transporte, la venta se guarda en Room + Outbox con una
     * `clientOperationId` unica y se sincroniza mas tarde.
     *
     * CREDITO es ONLINE_ONLY: sin conexion no se intenta y no se escribe nada
     * en local, porque el limite de credito solo puede validarlo el backend.
     */
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

            if (!ConectividadGlobal.estaOnline()) {
                mensaje =
                    "La venta a crédito requiere conexión a internet. " +
                    "Conéctate e inténtalo de nuevo."
                return
            }
        }

        if (guardando) {
            return
        }

        // Snapshot de precios: se congela aqui y no se recalcula despues.
        val lineas = items.map { item ->
            LineaSnapshot(
                productoId = item.producto.id,
                cantidad = item.cantidad(),
                precioUnitario = item.producto.precio,
                subtotal = item.cantidad() * item.producto.precio
            )
        }

        val formaPago = formaPagoSeleccionada
        val totalVenta = total

        guardando = true

        viewModelScope.launch {
            try {
                // Una sola clave por operacion. Se reutiliza en el envio en
                // linea y en el fallback local para no duplicar la venta.
                val clientOperationId = UUID.randomUUID().toString()

                if (ConectividadGlobal.estaOnline()) {
                    registrarEnLinea(
                        clientOperationId = clientOperationId,
                        clienteId = clienteId,
                        vendedorId = vendedorId,
                        lineas = lineas,
                        formaPago = formaPago
                    )
                } else {
                    guardarEnLocal(
                        clientOperationId = clientOperationId,
                        clienteId = clienteId,
                        vendedorId = vendedorId,
                        lineas = lineas,
                        formaPago = formaPago
                    )
                }
            } catch (e: Exception) {
                guardando = false
                error = mensajeAmigable(e)
            }
        }
    }

    /** Intento por red. Solo un fallo de transporte cae a la cola local. */
    private suspend fun registrarEnLinea(
        clientOperationId: String,
        clienteId: Int,
        vendedorId: Int,
        lineas: List<LineaSnapshot>,
        formaPago: String
    ) {
        val request = VentaRequest(
            cliente_id = clienteId,
            vendedor_id = vendedorId,
            productos = lineas.map { linea ->
                ItemVentaRequest(
                    producto_id = linea.productoId,
                    cantidad = linea.cantidad
                )
            },
            forma_pago = formaPago
        )

        when (val resultado = llamarBackend(request, clientOperationId)) {
            is IntentoVenta.Confirmada -> {
                guardando = false
                ventaExitosa = resultado.venta
                items.clear()
            }

            is IntentoVenta.Fallida -> {
                // Un rechazo del backend no se convierte en cola offline.
                val falloDeTransporte = resultado.causa is IOException

                // El limite de credito solo puede validarlo el backend, asi que
                // una venta a CREDITO nunca cae a la cola local: quedaria
                // pendiente de una validacion que ya no es posible verificar.
                // Solo CONTADO es offline-first.
                val puedeCaerALaCola =
                    falloDeTransporte && formaPago == "CONTADO"

                if (puedeCaerALaCola) {
                    // La peticion pudo haber llegado al backend. Se encola con
                    // la MISMA clave: la idempotencia evita el duplicado.
                    guardarEnLocal(
                        clientOperationId = clientOperationId,
                        clienteId = clienteId,
                        vendedorId = vendedorId,
                        lineas = lineas,
                        formaPago = formaPago
                    )
                } else {
                    guardando = false

                    error = if (falloDeTransporte) {
                        // CREDITO + IOException: la venta no se registro en
                        // ninguna parte y no debe quedar a medias.
                        "No se pudo completar la venta a crédito por falta de " +
                            "conexión. Inténtalo de nuevo cuando tengas internet."
                    } else {
                        mensajeAmigable(resultado.causa)
                    }
                }
            }
        }
    }

    /** Resultado de un intento de envio por red. */
private sealed class IntentoVenta {
        data class Confirmada(val venta: Venta) : IntentoVenta()
        data class Fallida(val causa: Throwable) : IntentoVenta()
    }

    private suspend fun llamarBackend(
        request: VentaRequest,
        clientOperationId: String
    ): IntentoVenta = suspendCancellableCoroutine { continuacion ->
        ventaRepository.crearVenta(
            request = request,
            headers = mapOf("X-Idempotency-Key" to clientOperationId),
            onSuccess = { venta ->
                if (continuacion.isActive) {
                    continuacion.resume(IntentoVenta.Confirmada(venta))
                }
            },
            onError = { causa ->
                if (continuacion.isActive) {
                    continuacion.resume(IntentoVenta.Fallida(causa))
                }
            }
        )
    }

/**
     * Guarda la venta en Room + Outbox de forma atomica.
     *
     * `vendedorId` llega como argumento y es SIEMPRE el vendedor seleccionado en
     * el ticket, el mismo que viaja en el envio en linea. No se toma de la
     * sesion: si se usara `usuario.vendedor_id`, una venta offline creada
     * eligiendo otro vendedor se sincronizaria atribuida al equivocado.
     *
     * `empresaId` si viene de la sesion, porque es el tenant y no el autor de
     * la operacion.
     *
     * No se genera folio comercial: `folioBackend` queda en null hasta que el
     * backend responda en la sincronizacion.
     */
    private suspend fun guardarEnLocal(
        clientOperationId: String,
        clienteId: Int,
        vendedorId: Int,
        lineas: List<LineaSnapshot>,
        formaPago: String
    ) {
        val usuario = sessionManager.sesion.first()

        val empresaId = usuario?.empresa_id?.toIntOrNull()

        if (empresaId == null) {
            guardando = false
            mensaje =
                "Tu sesión no tiene empresa o vendedor asignados. " +
                    "Cierra sesión e inicia de nuevo."
            return
        }

        ventaPendienteRepository.crearVentaOffline(
            empresaId = empresaId,
            vendedorId = vendedorId,
            clienteId = clienteId,
            formaPago = formaPago,
            lineas = lineas,
            clientOperationId = clientOperationId
        )

        // La venta ya esta en la Outbox: se pide el envio. Si ahora no hay red,
        // WorkManager espera a que vuelva y ejecuta el worker por su cuenta, con
        // la misma clientOperationId para no duplicar la venta.
        SyncManager.encolarSincronizacion(appContext)

        guardando = false
        mensaje =
            "Venta guardada. Se enviará al servidor cuando tengas conexión."
        items.clear()
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
                val app = this[APPLICATION_KEY] as Application
                VentaViewModel(
                    vendedorRepository = VendedorRepository(),
                    clienteRepository = ClienteRepository(),
                    productoRepository = ProductoRepository(),
                    ventaRepository = VentaRepository(),
                    cobranzaRepository = CobranzaRepository(),
                    sessionManager = SessionManager(app),
                    appContext = app
                )
            }
        }
    }
}

private fun ItemTicket.cantidad(): Double {
    return cantidadTexto.toDoubleOrNull() ?: 0.0
}
