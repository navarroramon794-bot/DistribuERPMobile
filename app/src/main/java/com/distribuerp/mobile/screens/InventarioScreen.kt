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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.ItemInventario
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.viewmodel.InventarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    onAbrirMenu: () -> Unit
) {
    val cargandoVendedores = viewModel.cargandoVendedores
    val vendedores = viewModel.vendedores
    val cargandoInventario = viewModel.cargandoInventario
    val inventario = viewModel.inventario
    val vendedorSeleccionado = viewModel.vendedorSeleccionado
    val error = viewModel.error
    val mensaje = viewModel.mensaje

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtrados = remember(inventario, busqueda) {

        if (busqueda.isBlank()) {

            inventario

        } else {

            inventario.filter { item ->

                item.nombre.contains(
                    busqueda,
                    ignoreCase = true
                ) || item.codigo.contains(
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
                    Text("Inventario")
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
                            viewModel.recargar()
                        },
                        enabled =
                            vendedorSeleccionado != null
                            && !cargandoInventario
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
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

            SelectorDesplegable(
                etiqueta = "Vendedor",
                seleccionado = vendedorSeleccionado,
                opciones = vendedores,
                textoDe = { it.nombre },
                onSeleccionar = {

                    viewModel.seleccionarVendedor(it)
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            BarraBusqueda(
                valor = busqueda,
                onCambio = {
                    busqueda = it
                },
                placeholder = "Buscar por nombre o código"
            )

            mensaje?.let { texto ->

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                AvisoMensaje(
                    mensaje = texto,
                    onCerrar = {

                        viewModel.limpiarMensaje()
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            when {

                error != null && vendedorSeleccionado == null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarVendedores()
                        }
                    )
                }

                cargandoVendedores && vendedores.isEmpty() -> {

                    LoadingContent()
                }

                vendedores.isEmpty() -> {

                    EmptyContent(
                        mensaje = "No hay vendedores registrados",
                        icono = Icons.Filled.Badge
                    )
                }

                cargandoInventario -> {

                    LoadingContent()
                }

                error != null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.recargar()
                        }
                    )
                }

                filtrados.isEmpty() -> {

                    EmptyContent(
                        mensaje = if (busqueda.isNotBlank()) {
                            "Sin resultados para la búsqueda"
                        } else {
                            "El vendedor no tiene productos asignados"
                        },
                        icono = Icons.Filled.Search
                    )
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {

                        items(
                            items = filtrados,
                            key = { it.producto_id }
                        ) { item ->

                            TarjetaItemInventario(
                                item = item
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaItemInventario(
    item: ItemInventario
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = item.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = item.codigo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${formatearNumero(item.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                DatoCantidad(
                    etiqueta = "Disponible",
                    valor = formatearNumero(item.disponible),
                    color =
                        if (item.disponible > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                )

                DatoCantidad(
                    etiqueta = "Cargado",
                    valor = formatearNumero(item.cargado)
                )

                DatoCantidad(
                    etiqueta = "Vendido",
                    valor = formatearNumero(item.vendido)
                )
            }
        }
    }
}

@Composable
private fun DatoCantidad(
    etiqueta: String,
    valor: String,
    color: Color =
        MaterialTheme.colorScheme.onSurface
) {
    Column {

        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatearNumero(valor: Double): String {

    val entero = valor.toInt()

    return if (valor == entero.toDouble()) {
        entero.toString()
    } else {
        valor.toString()
    }
}
