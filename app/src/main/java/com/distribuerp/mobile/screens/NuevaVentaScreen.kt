package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.printing.PrinterRepository
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.ItemTicket
import com.distribuerp.mobile.viewmodel.VentaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    viewModel: VentaViewModel,
    onAbrirMenu: () -> Unit
) {
    val cargandoInicial = viewModel.cargandoInicial
    val vendedores = viewModel.vendedores
    val clientes = viewModel.clientes
    val productos = viewModel.productos
    val vendedorSeleccionado = viewModel.vendedorSeleccionado
    val clienteSeleccionado = viewModel.clienteSeleccionado
    val items = viewModel.items
    val total = viewModel.total
    val guardando = viewModel.guardando
    val ventaExitosa = viewModel.ventaExitosa
    val error = viewModel.error
    val mensaje = viewModel.mensaje

    val contexto = LocalContext.current
    val impresoraRepositorio = remember {
        PrinterRepository(contexto.applicationContext)
    }
    val scopeImpresion = rememberCoroutineScope()
    var imprimiendo by remember {
        mutableStateOf(false)
    }
    var errorImpresion by remember {
        mutableStateOf<String?>(null)
    }

    var busqueda by remember {
        mutableStateOf("")
    }

    LaunchedEffect(ventaExitosa) {
        imprimiendo = false
        errorImpresion = null
    }

    val filtrados = remember(productos, busqueda) {

        if (busqueda.isBlank()) {

            productos

        } else {

            productos.filter { producto ->

                producto.nombre.contains(
                    busqueda,
                    ignoreCase = true
                ) || producto.codigo.contains(
                    busqueda,
                    ignoreCase = true
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text("Nueva Venta")
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
        }
    ) { padding ->

        when {

            cargandoInicial -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    LoadingContent()
                }
            }

            error != null && vendedores.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarDatos()
                        }
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

                    if (vendedores.isEmpty()) {

                        item {

                            EmptyContent(
                                mensaje = "No hay vendedores registrados",
                                icono = Icons.Filled.Badge,
                                expandido = false
                            )
                        }

                    } else {

                        item {

                            SelectorDesplegable(
                                etiqueta = "Vendedor",
                                seleccionado = vendedorSeleccionado,
                                opciones = vendedores,
                                textoDe = { it.nombre },
                                onSeleccionar = {

                                    viewModel.seleccionarVendedor(it)
                                }
                            )
                        }
                    }

                    if (clientes.isEmpty()) {

                        item {

                            EmptyContent(
                                mensaje = "No hay clientes registrados",
                                icono = Icons.Filled.People,
                                expandido = false
                            )
                        }

                    } else {

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
                    }

                    item {

                        BarraBusqueda(
                            valor = busqueda,
                            onCambio = {

                                busqueda = it
                            },
                            placeholder =
                                "Buscar producto por nombre o código"
                        )
                    }

                    if (filtrados.isEmpty()) {

                        item {

                            EmptyContent(
                                mensaje = "No hay productos para agregar",
                                icono = Icons.Filled.Search,
                                expandido = false
                            )
                        }

                    } else {

                        items(
                            items = filtrados,
                            key = { "buscar_${it.id}" }
                        ) { producto ->

                            TarjetaProductoBusqueda(
                                producto = producto,
                                onAgregar = {

                                    viewModel.agregarProducto(producto)
                                }
                            )
                        }
                    }

                    item {

                        Text(
                            text = "Productos de la venta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 8.dp
                            )
                        )
                    }

                    if (items.isEmpty()) {

                        item {

                            EmptyContent(
                                mensaje = "Aún no hay productos agregados",
                                icono = Icons.Filled.ShoppingCart,
                                expandido = false
                            )
                        }

                    } else {

                        itemsIndexed(
                            items = items,
                            key = { _, item ->
                                "item_${item.producto.id}"
                            }
                        ) { indice, item ->

                            TarjetaItemVenta(
                                item = item,
                                onCambiarCantidad = { texto ->

                                    viewModel.cambiarCantidad(indice, texto)
                                },
                                onIncrementar = {

                                    viewModel.incrementarCantidad(indice)
                                },
                                onDecrementar = {

                                    viewModel.decrementarCantidad(indice)
                                },
                                onEliminar = {

                                    viewModel.eliminarItem(indice)
                                }
                            )
                        }

                        item {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = "Total",
                                        style =
                                            MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = formatearDinero(total),
                                        style =
                                            MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        item {

                            Button(
                                onClick = {

                                    viewModel.registrarVenta()
                                },
                                enabled =
                                    !guardando
                                    && vendedorSeleccionado != null
                                    && clienteSeleccionado != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                if (guardando) {

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onPrimary
                                    )

                                } else {

                                    Text("Registrar Venta")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ventaExitosa?.let { venta ->

        AlertDialog(
            onDismissRequest = {

                viewModel.limpiarVentaExitosa()
            },
            title = {

                Text("Venta registrada correctamente")
            },
            text = {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Folio",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = venta.folio,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = formatearDinero(venta.total),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    errorImpresion?.let { mensajeError ->

                        Text(
                            text = mensajeError,
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    TextButton(
                        onClick = {

                            imprimiendo = true
                            errorImpresion = null

                            scopeImpresion.launch {

                                val resultado =
                                    impresoraRepositorio
                                        .imprimirTicketVenta(venta)
                                imprimiendo = false

                                if (resultado.ok) {

                                    viewModel.limpiarVentaExitosa()

                                } else {

                                    errorImpresion =
                                        resultado.mensaje
                                }
                            }
                        },
                        enabled = !imprimiendo
                    ) {

                        if (imprimiendo) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )

                        } else {

                            Text("Imprimir Ticket")
                        }
                    }

                    TextButton(
                        onClick = {

                            viewModel.limpiarVentaExitosa()
                        },
                        enabled = !imprimiendo
                    ) {

                        Text("Cerrar")
                    }
                }
            }
        )
    }
}

@Composable
private fun TarjetaProductoBusqueda(
    producto: Producto,
    onAgregar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Código ${producto.codigo} · " +
                            formatearDinero(producto.precio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            IconButton(
                onClick = onAgregar
            ) {

                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Agregar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TarjetaItemVenta(
    item: ItemTicket,
    onCambiarCantidad: (String) -> Unit,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit,
    onEliminar: () -> Unit
) {
    val cantidad = item.cantidadTexto.toDoubleOrNull() ?: 0.0
    val subtotal = cantidad * item.producto.precio

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = item.producto.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatearDinero(item.producto.precio),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEliminar
                ) {

                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Quitar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onDecrementar
                ) {

                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Disminuir"
                    )
                }

                OutlinedTextField(
                    value = item.cantidadTexto,
                    onValueChange = onCambiarCantidad,
                    modifier = Modifier.width(88.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                IconButton(
                    onClick = onIncrementar
                ) {

                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Aumentar"
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatearDinero(subtotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
