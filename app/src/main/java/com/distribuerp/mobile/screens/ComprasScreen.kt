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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.CompraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    viewModel: CompraViewModel,
    onAbrirMenu: () -> Unit,
    onNuevaCompra: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val cargando = viewModel.cargando
    val compras = viewModel.compras
    val error = viewModel.error
    val proveedores = viewModel.proveedores
    val proveedorFiltro = viewModel.proveedorFiltro

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtrados = remember(compras, busqueda) {

        if (busqueda.isBlank()) {

            compras

        } else {

            compras.filter { compra ->

                compra.folio.contains(
                    busqueda,
                    ignoreCase = true
                ) || compra.proveedor?.contains(
                    busqueda,
                    ignoreCase = true
                ) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarCompras()
        if (proveedores.isEmpty()) {
            viewModel.cargarFormulario()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Compras")
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
                }
            )
        },
        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onNuevaCompra,
                icon = {

                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = {

                    Text("Nueva Compra")
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
                placeholder = "Buscar por folio o proveedor"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (proveedores.isNotEmpty()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    SelectorDesplegable(
                        etiqueta = "Filtrar por proveedor",
                        seleccionado = proveedorFiltro,
                        opciones = proveedores,
                        textoDe = { it.nombre },
                        onSeleccionar = {
                            viewModel.filtrarPorProveedor(it)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (proveedorFiltro != null) {

                        IconButton(
                            onClick = {
                                viewModel.quitarFiltroProveedor()
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Quitar filtro",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            when {

                cargando -> {

                    LoadingContent()
                }

                error != null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {
                            viewModel.cargarCompras()
                        }
                    )
                }

                filtrados.isEmpty() -> {

                    EmptyContent(
                        mensaje = if (busqueda.isNotBlank()) {
                            "Sin resultados para la búsqueda"
                        } else {
                            "No hay compras registradas"
                        },
                        icono = Icons.Filled.Receipt
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
                        ) { compra ->

                            TarjetaCompra(
                                compra = compra,
                                onClick = {
                                    onVerDetalle(compra.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCompra(
    compra: Compra,
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
                    text = compra.folio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = compra.fecha ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = compra.proveedor ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = formatearDinero(compra.total),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
