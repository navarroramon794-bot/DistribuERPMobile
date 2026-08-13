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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.models.EmpresaDatos
import com.distribuerp.mobile.models.ItemCompra
import com.distribuerp.mobile.printing.PrinterRepository
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.FilaInformacion
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.formatearCantidad
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.CompraViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompraDetalleScreen(
    compraId: Int,
    viewModel: CompraViewModel,
    empresa: EmpresaDatos? = null,
    onVolver: () -> Unit
) {
    val cargando = viewModel.cargandoDetalle
    val compra = viewModel.compraActual
    val error = viewModel.error

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

    LaunchedEffect(compraId) {
        viewModel.cargarCompra(compraId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle de la Compra")
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
        ) {

            when {

                cargando && compra == null -> {

                    LoadingContent()
                }

                error != null && compra == null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarCompra(compraId)
                        }
                    )
                }

                compra != null -> {

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            item {

                                TarjetaResumenCompra(
                                    compra = compra
                                )
                            }

                            errorImpresion?.let { mensajeError ->

                                item {

                                    Text(
                                        text = mensajeError,
                                        style =
                                            MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            item {

                                Text(
                                    text = "Productos de la compra",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        top = 8.dp
                                    )
                                )
                            }

                            if (compra.items.isEmpty()) {

                                item {

                                    Text(
                                        text = "Sin productos",
                                        style =
                                            MaterialTheme.typography.bodyMedium,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                            } else {

                                items(
                                    items = compra.items,
                                    key = { it.producto_id }
                                ) { detalle ->

                                    TarjetaItemCompraDetalle(
                                        detalle = detalle
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {

                                imprimiendo = true
                                errorImpresion = null

                                scopeImpresion.launch {

                                    val resultado =
                                        impresoraRepositorio
                                            .imprimirComprobanteCompra(
                                                compra,
                                                empresa
                                            )
                                    imprimiendo = false

                                    if (!resultado.ok) {

                                        errorImpresion = resultado.mensaje
                                    }
                                }
                            },
                            enabled = !imprimiendo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            if (imprimiendo) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )

                            } else {

                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                Text("Imprimir Comprobante")
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
                            text = "No se encontró la compra.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumenCompra(
    compra: Compra
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
                        text = compra.folio,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                SurfaceTotal(
                    total = compra.total
                )
            }

            FilaInformacion(
                icono = Icons.Filled.DateRange,
                etiqueta = "Fecha",
                valor = compra.fecha ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.Storefront,
                etiqueta = "Proveedor",
                valor = compra.proveedor ?: "—"
            )

            FilaInformacion(
                icono = Icons.AutoMirrored.Filled.Notes,
                etiqueta = "Observaciones",
                valor = compra.observaciones?.ifBlank {
                    null
                } ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.ShoppingCart,
                etiqueta = "Productos",
                valor = compra.total_productos.toString()
            )
        }
    }
}

@Composable
private fun SurfaceTotal(
    total: Double
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {

        Text(
            text = formatearDinero(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            )
        )
    }
}

@Composable
private fun TarjetaItemCompraDetalle(
    detalle: ItemCompra
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = detalle.producto,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Código ${detalle.codigo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatearDinero(detalle.precio) + " c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = if (detalle.unidad.isBlank()) {
                        formatearCantidad(detalle.cantidad)
                    } else {
                        formatearCantidad(detalle.cantidad) + " " + detalle.unidad
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = formatearDinero(detalle.subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
