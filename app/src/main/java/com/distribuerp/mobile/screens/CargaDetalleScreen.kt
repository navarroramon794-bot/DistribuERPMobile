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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
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
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.models.EmpresaDatos
import com.distribuerp.mobile.models.ItemCarga
import com.distribuerp.mobile.printing.PrinterRepository
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.FilaInformacion
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.formatearCantidad
import com.distribuerp.mobile.viewmodel.CargaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargaDetalleScreen(
    cargaId: Int,
    viewModel: CargaViewModel,
    empresa: EmpresaDatos? = null,
    onVolver: () -> Unit
) {
    val cargando = viewModel.cargandoDetalle
    val carga = viewModel.cargaActual
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

    LaunchedEffect(cargaId) {
        viewModel.cargarCarga(cargaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle de la Carga")
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

                cargando && carga == null -> {

                    LoadingContent()
                }

                error != null && carga == null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {

                            viewModel.cargarCarga(cargaId)
                        }
                    )
                }

                carga != null -> {

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

                                TarjetaResumenCarga(
                                    carga = carga
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
                                    text = "Productos de la carga",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        top = 8.dp
                                    )
                                )
                            }

                            if (carga.items.isEmpty()) {

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
                                    items = carga.items,
                                    key = { it.producto_id }
                                ) { detalle ->

                                    TarjetaItemCargaDetalle(
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
                                            .imprimirComprobanteCarga(
                                                carga,
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
                            text = "No se encontró la carga.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumenCarga(
    carga: Carga
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
                        text = carga.folio,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                SurfaceCantidad(
                    total = carga.total_cantidad
                )
            }

            FilaInformacion(
                icono = Icons.Filled.DateRange,
                etiqueta = "Fecha",
                valor = carga.fecha ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.Badge,
                etiqueta = "Vendedor",
                valor = carga.vendedor ?: "—"
            )

            FilaInformacion(
                icono = Icons.AutoMirrored.Filled.Notes,
                etiqueta = "Observaciones",
                valor = carga.observaciones?.ifBlank {
                    null
                } ?: "—"
            )

            FilaInformacion(
                icono = Icons.Filled.LocalShipping,
                etiqueta = "Productos",
                valor = carga.total_productos.toString()
            )
        }
    }
}

@Composable
private fun SurfaceCantidad(
    total: Double
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {

        Text(
            text = formatearCantidad(total),
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
private fun TarjetaItemCargaDetalle(
    detalle: ItemCarga
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
                }

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
            }

            val unidad = if (detalle.unidad.isBlank()) "kg" else detalle.unidad

            Spacer(
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "Vendido",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatearCantidad(detalle.vendido)} $unidad",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatearCantidad(detalle.disponible)} $unidad",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
