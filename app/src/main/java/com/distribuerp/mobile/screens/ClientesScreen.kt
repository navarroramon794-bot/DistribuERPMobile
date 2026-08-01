package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.ClienteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    viewModel: ClienteViewModel,
    onVolver: () -> Unit,
    onAbrirMenu: (() -> Unit)? = null,
    onNuevoCliente: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val loading = viewModel.loading
    val clientes = viewModel.clientes
    val error = viewModel.error

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtrados = remember(clientes, busqueda) {

        if (busqueda.isBlank()) {

            clientes

        } else {

            clientes.filter { cliente ->

                cliente.nombre.contains(
                    busqueda,
                    ignoreCase = true
                ) || cliente.telefono?.contains(
                    busqueda,
                    ignoreCase = true
                ) == true
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
                    Text("Clientes")
                },
                navigationIcon = {

                    if (onAbrirMenu != null) {

                        IconButton(
                            onClick = onAbrirMenu
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Abrir menú"
                            )
                        }

                    } else {

                        IconButton(
                            onClick = onVolver
                        ) {

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onNuevoCliente,
                icon = {

                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {

                    Text("Nuevo Cliente")
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            BarraBusqueda(
                valor = busqueda,
                onCambio = {
                    busqueda = it
                },
                placeholder = "Buscar por nombre o teléfono"
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            when {

                loading -> {

                    LoadingContent()
                }

                error != null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarClientes()
                        }
                    )
                }

                filtrados.isEmpty() -> {

                    EmptyContent(
                        mensaje = if (busqueda.isNotBlank()) {
                            "Sin resultados para la búsqueda"
                        } else {
                            "No hay clientes registrados"
                        },
                        icono = Icons.Filled.Search
                    )
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {

                        items(
                            items = filtrados,
                            key = { it.id }
                        ) { cliente ->

                            TarjetaCliente(
                                cliente = cliente,
                                onClick = {

                                    onVerDetalle(cliente.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaCliente(
    cliente: Cliente,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    cliente.telefono?.let { telefono ->

                        if (telefono.isNotBlank()) {

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = telefono,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!cliente.activo) {

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {

                        Text(
                            text = "Inactivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = formatearDinero(
                        cliente.limite_credito
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
