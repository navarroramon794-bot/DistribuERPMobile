package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.ConfirmDeleteDialog
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.FilaInformacion
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.ClienteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetalleScreen(
    clienteId: Int,
    viewModel: ClienteViewModel,
    onVolver: () -> Unit,
    onEditar: (Int) -> Unit
) {
    val cargando = viewModel.cargandoDetalle
    val cliente = viewModel.clienteActual
    val error = viewModel.error
    val eliminando = viewModel.eliminando
    val mensaje = viewModel.mensaje

    var mostrarConfirmacion by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(clienteId) {
        viewModel.cargarCliente(clienteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle del Cliente")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            when {

                cargando && cliente == null -> {

                    LoadingContent()
                }

                error != null && cliente == null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarCliente(clienteId)
                        }
                    )
                }

                cliente != null -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        TarjetaEncabezadoCliente(
                            cliente = cliente
                        )

                        TarjetaInformacionCliente(
                            cliente = cliente
                        )

                        mensaje?.let { texto ->

                            AvisoMensaje(
                                mensaje = texto,
                                onCerrar = {

                                    viewModel.limpiarMensaje()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            OutlinedButton(
                                onClick = {

                                    onEditar(cliente.id)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !eliminando
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(6.dp)
                                )

                                Text("Editar")
                            }

                            Button(
                                onClick = {

                                    mostrarConfirmacion = true
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !eliminando,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {

                                if (eliminando) {

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onError,
                                        strokeWidth = 2.dp
                                    )

                                } else {

                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.width(6.dp)
                                    )

                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }

                else -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "No se encontró el cliente.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    if (mostrarConfirmacion) {

        ConfirmDeleteDialog(
            titulo = "Eliminar cliente",
            mensaje =
                "¿Seguro que deseas desactivar a " +
                "${cliente?.nombre ?: "este cliente"}?",
            onConfirmar = {

                mostrarConfirmacion = false

                viewModel.eliminarCliente(
                    id = clienteId,
                    onExito = onVolver
                )
            },
            onCancelar = {

                mostrarConfirmacion = false
            }
        )
    }
}

@Composable
private fun TarjetaEncabezadoCliente(
    cliente: Cliente
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = cliente.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (cliente.activo) {

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = "Activo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                } else {

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = "Inactivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaInformacionCliente(
    cliente: Cliente
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
                icono = Icons.Filled.Person,
                etiqueta = "Nombre",
                valor = cliente.nombre
            )

            FilaInformacion(
                icono = Icons.Filled.Phone,
                etiqueta = "Teléfono",
                valor = cliente.telefono?.ifBlank {
                    null
                } ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.LocationOn,
                etiqueta = "Dirección",
                valor = cliente.direccion?.ifBlank {
                    null
                } ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.AttachMoney,
                etiqueta = "Límite de crédito",
                valor = formatearDinero(
                    cliente.limite_credito
                )
            )

            FilaInformacion(
                icono = Icons.Filled.DateRange,
                etiqueta = "Fecha de creación",
                valor = cliente.fecha_creacion ?: "—"
            )
        }
    }
}
