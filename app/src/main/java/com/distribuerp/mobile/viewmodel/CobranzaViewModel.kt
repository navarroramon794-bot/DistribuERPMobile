package com.distribuerp.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.Pago
import com.distribuerp.mobile.models.PagoRequest
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.repository.ClienteRepository
import com.distribuerp.mobile.repository.CobranzaRepository

data class ResultadoPago(
    val pago: Pago,
    val saldoRestante: Double
)

class CobranzaViewModel(
    private val clienteRepository: ClienteRepository,
    private val cobranzaRepository: CobranzaRepository
) : ViewModel() {

    var cargandoClientes by mutableStateOf(false)
        private set

    var clientes by mutableStateOf<List<Cliente>>(emptyList())
        private set

    var clienteSeleccionado by mutableStateOf<Cliente?>(null)
        private set

    var cargandoEstadoCuenta by mutableStateOf(false)
        private set

    var estadoCuenta by mutableStateOf<EstadoCuentaResponse?>(null)
        private set

    var ventasPendientes by mutableStateOf<List<Venta>>(emptyList())
        private set

    var ventaSeleccionada by mutableStateOf<Venta?>(null)
        private set

    var montoTexto by mutableStateOf("")
        private set

    var guardando by mutableStateOf(false)
        private set

    var resultadoPago by mutableStateOf<ResultadoPago?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cargarClientes() {
        cargandoClientes = true
        error = null

        clienteRepository.obtenerClientes(
            onSuccess = { lista ->
                cargandoClientes = false
                clientes = lista

                if (clienteSeleccionado == null) {
                    clienteSeleccionado = lista.firstOrNull()

                    lista.firstOrNull()?.let {
                        cargarEstadoCuenta()
                    }
                }
            },
            onError = { t ->
                cargandoClientes = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun seleccionarCliente(cliente: Cliente) {
        if (cliente.id == clienteSeleccionado?.id) {
            return
        }

        clienteSeleccionado = cliente
        ventaSeleccionada = null
        montoTexto = ""
        cargarEstadoCuenta()
    }

    fun cargarEstadoCuenta() {
        val cliente = clienteSeleccionado ?: return

        cargandoEstadoCuenta = true
        error = null

        cobranzaRepository.obtenerEstadoCuenta(
            clienteId = cliente.id,
            onSuccess = { respuesta ->
                cargandoEstadoCuenta = false
                estadoCuenta = respuesta

                val pendientes = respuesta.ventas
                    .orEmpty()
                    .filter { it.saldo > 0 }

                ventasPendientes = pendientes

                ventaSeleccionada = pendientes.firstOrNull {
                    it.id == ventaSeleccionada?.id
                }
            },
            onError = { t ->
                cargandoEstadoCuenta = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun seleccionarVenta(venta: Venta) {
        ventaSeleccionada = venta
        montoTexto = ""
        error = null
        mensaje = null
    }

    fun cambiarMonto(texto: String) {
        montoTexto = texto.filter {
            it.isDigit() || it == '.'
        }
        error = null
        mensaje = null
    }

    fun registrarPago() {
        val venta = ventaSeleccionada ?: return

        if (montoTexto.isBlank()) {
            mensaje = "Debe capturar un monto"
            return
        }

        val monto = montoTexto.toDoubleOrNull() ?: 0.0

        if (monto <= 0) {
            mensaje = "El monto debe ser mayor a cero"
            return
        }

        guardando = true
        error = null
        mensaje = null

        cobranzaRepository.crearPago(
            request = PagoRequest(
                venta_id = venta.id,
                monto = monto
            ),
            onSuccess = { pago ->
                guardando = false
                resultadoPago = ResultadoPago(
                    pago = pago,
                    saldoRestante = (
                        venta.saldo - monto
                    ).coerceAtLeast(0.0)
                )
                montoTexto = ""
            },
            onError = { t ->
                guardando = false
                error = t.message ?: "Error de conexión"
            }
        )
    }

    fun aceptarExito() {
        resultadoPago = null
        ventaSeleccionada = null
        montoTexto = ""
        cargarEstadoCuenta()
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
                CobranzaViewModel(
                    clienteRepository = ClienteRepository(),
                    cobranzaRepository = CobranzaRepository()
                )
            }
        }
    }
}
