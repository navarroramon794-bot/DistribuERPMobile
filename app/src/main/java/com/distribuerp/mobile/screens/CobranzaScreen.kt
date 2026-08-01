package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.FilaInformacion
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.CobranzaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobranzaScreen(
    viewModel: CobranzaViewModel,
    onAbrirMenu: () -> Unit
) {
    val cargandoClientes = viewModel.cargandoClientes
    val clientes = viewModel.clientes
    val clienteSeleccionado = viewModel.clienteSeleccionado
    val cargandoEstadoCuenta = viewModel.cargandoEstadoCuenta
    val estadoCuenta = viewModel.estadoCuenta
    val ventasPendientes = viewModel.ventasPendientes
    val ventaSeleccionada = viewModel.ventaSeleccionada
    val montoTexto = viewModel.montoTexto
    val guardando = viewModel.guardando
    val resultadoPago = viewModel.resultadoPago
    val error = viewModel.error
    val mensaje = viewModel.mensaje

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtradas = remember(ventasPendientes, busqueda) {

        if (busqueda.isBlank()) {

            ventasPendientes

        } else {

            ventasPendientes.filter { venta ->

                venta.folio.contains(
                    busqueda,
                    ignoreCase = true
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarClientes()
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Text("Cobranza")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onAbrirMenu
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menú"
                        )
                    }
                },
                actions = {

                    IconButton(
                        onClick = {

                            viewModel.cargarEstadoCuenta()
                        },
                        enabled =
                            clienteSeleccionado != null
                            && !cargandoEstadoCuenta
                            && !guardando
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when {

            cargandoClientes && clientes.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    LoadingContent()
                }
            }

            error != null && clientes.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarClientes()
                        }
                    )
                }
            }

            clientes.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    EmptyContent(
                        mensaje = "No hay clientes registrados",
                        icono = Icons.Filled.People
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    mensaje?.let { aviso ->

                        item {

                            AvisoMensaje(
                                mensaje = aviso,
                                onCerrar = {

                                    viewModel.limpiarMensaje()
                                }
                            )
                        }
                    }

                    error?.let { aviso ->

                        item {

                            AvisoMensaje(
                                mensaje = aviso,
                                onCerrar = {

                                    viewModel.limpiarError()
                                }
                            )
                        }
                    }

                    item {

                        SelectorDesplegable(
                            etiqueta = "Cliente",
                            seleccionado = clienteSeleccionado,
                            opciones = clientes,
                            textoDe = { it.nombre },
                            onSeleccionar = {

                                viewModel.seleccionarCliente(it)
                            }
                        )
                    }

                    if (clienteSeleccionado != null) {

                        if (!cargandoEstadoCuenta && estadoCuenta != null) {

                            item {

                                TarjetaResumenEstado(
                                    estadoCuenta = estadoCuenta
                                )
                            }
                        }

                        item {

                            Text(
                                text =
                                    "Ventas con saldo pendiente",
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    top = 8.dp
                                )
                            )
                        }

                        when {

                            cargandoEstadoCuenta -> {

                                item {

                                    LoadingContent()
                                }
                            }

                            ventasPendientes.isEmpty() -> {

                                item {

                                    EmptyContent(
                                        mensaje =
                                            "El cliente no tiene " +
                                                "saldos pendientes",
                                        icono =
                                            Icons.Filled.CheckCircle,
                                        expandido = false
                                    )
                                }
                            }

                            else -> {

                                item {

                                    BarraBusqueda(
                                        valor = busqueda,
                                        onCambio = {

                                            busqueda = it
                                        },
                                        placeholder = "Buscar por folio"
                                    )
                                }

                                if (filtradas.isEmpty()) {

                                    item {

                                        EmptyContent(
                                            mensaje =
                                                "Sin resultados " +
                                                    "para la búsqueda",
                                            icono =
                                                Icons.Filled.Search,
                                            expandido = false
                                        )
                                    }

                                } else {

                                    items(
                                        items = filtradas,
                                        key = { it.id }
                                    ) { venta ->

                                        TarjetaVentaPendiente(
                                            venta = venta,
                                            seleccionada =
                                                venta.id ==
                                                    ventaSeleccionada?.id,
                                            onClick = {

                                                viewModel
                                                    .seleccionarVenta(venta)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ventaSeleccionada?.let { venta ->

                        item {

                            Text(
                                text = "Detalle de la venta",
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    top = 8.dp
                                )
                            )
                        }

                        item {

                            TarjetaDetalleVenta(
                                venta = venta
                            )
                        }

                        item {

                            OutlinedTextField(
                                value = montoTexto,
                                onValueChange = {

                                    viewModel.cambiarMonto(it)
                                },
                                label = {

                                    Text("Monto del pago")
                                },
                                prefix = {

                                    Text("$")
                                },
                                singleLine = true,
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType =
                                            KeyboardType.Number
                                    ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {

                            Button(
                                onClick = {

                                    viewModel.registrarPago()
                                },
                                enabled = !guardando,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                if (guardando) {

                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color =
                                            MaterialTheme
                                                .colorScheme.onPrimary
                                    )

                                } else {

                                    Text("Registrar Pago")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    resultadoPago?.let { resultado ->

        AlertDialog(
            onDismissRequest = {

                viewModel.aceptarExito()
            },
            title = {

                Text("Pago registrado correctamente")
            },
            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Monto",
                            style =
                                MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = formatearDinero(
                                resultado.pago.monto
                            ),
                            style =
                                MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Saldo restante",
                            style =
                                MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = formatearDinero(
                                resultado.saldoRestante
                            ),
                            style =
                                MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.aceptarExito()
                    }
                ) {

                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaResumenEstado(
    estadoCuenta: EstadoCuentaResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Estado de cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FilaInformacion(
                icono = Icons.Filled.AttachMoney,
                etiqueta = "Total vendido",
                valor = formatearDinero(
                    estadoCuenta.total_vendido ?: 0.0
                )
            )

            FilaInformacion(
                icono = Icons.Filled.Payments,
                etiqueta = "Total cobrado",
                valor = formatearDinero(
                    estadoCuenta.total_cobrado ?: 0.0
                )
            )

            FilaInformacion(
                icono = Icons.Filled.AccountBalance,
                etiqueta = "Saldo",
                valor = formatearDinero(
                    estadoCuenta.saldo ?: 0.0
                )
            )
        }
    }
}

@Composable
private fun TarjetaVentaPendiente(
    venta: Venta,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = venta.folio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = venta.fecha ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = "Saldo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatearDinero(venta.saldo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TarjetaDetalleVenta(
    venta: Venta
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            FilaInformacion(
                icono = Icons.Filled.Receipt,
                etiqueta = "Folio",
                valor = venta.folio
            )

            FilaInformacion(
                icono = Icons.Filled.DateRange,
                etiqueta = "Fecha",
                valor = venta.fecha ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.AttachMoney,
                etiqueta = "Total",
                valor = formatearDinero(venta.total)
            )

            FilaInformacion(
                icono = Icons.Filled.Payments,
                etiqueta = "Pagado",
                valor = formatearDinero(venta.pagado)
            )

            FilaInformacion(
                icono = Icons.Filled.AccountBalance,
                etiqueta = "Saldo pendiente",
                valor = formatearDinero(venta.saldo)
            )
        }
    }
}
