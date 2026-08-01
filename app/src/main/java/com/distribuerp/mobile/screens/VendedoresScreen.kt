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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.viewmodel.VendedorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendedoresScreen(
    viewModel: VendedorViewModel,
    onVolver: () -> Unit,
    onAbrirMenu: (() -> Unit)? = null,
    onNuevoVendedor: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val loading = viewModel.loading
    val vendedores = viewModel.vendedores
    val error = viewModel.error

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtrados = remember(vendedores, busqueda) {

        if (busqueda.isBlank()) {

            vendedores

        } else {

            vendedores.filter { vendedor ->

                vendedor.nombre.contains(
                    busqueda,
                    ignoreCase = true
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarVendedores()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Vendedores")
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
                onClick = onNuevoVendedor,
                icon = {

                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {

                    Text("Nuevo Vendedor")
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
                placeholder = "Buscar por nombre"
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

                            viewModel.cargarVendedores()
                        }
                    )
                }

                filtrados.isEmpty() -> {

                    EmptyContent(
                        mensaje = if (busqueda.isNotBlank()) {
                            "Sin resultados para la búsqueda"
                        } else {
                            "No hay vendedores registrados"
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
                        ) { vendedor ->

                            TarjetaVendedor(
                                vendedor = vendedor,
                                onClick = {

                                    onVerDetalle(vendedor.id)
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
private fun TarjetaVendedor(
    vendedor: Vendedor,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = vendedor.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                vendedor.telefono?.let { telefono ->

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

            if (!vendedor.activo) {

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
    }
}
