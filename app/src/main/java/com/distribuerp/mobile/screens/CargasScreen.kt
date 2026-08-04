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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.ui.components.BarraBusqueda
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.ui.components.formatearCantidad
import com.distribuerp.mobile.viewmodel.CargaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargasScreen(
    viewModel: CargaViewModel,
    esAdministrador: Boolean,
    onAbrirMenu: () -> Unit,
    onNuevaCarga: () -> Unit,
    onVerDetalle: (Int) -> Unit
) {
    val cargando = viewModel.cargando
    val cargas = viewModel.cargas
    val error = viewModel.error
    val vendedores = viewModel.vendedores
    val vendedorFiltro = viewModel.vendedorFiltro

    var busqueda by remember {
        mutableStateOf("")
    }

    val filtrados = remember(cargas, busqueda) {

        if (busqueda.isBlank()) {

            cargas

        } else {

            cargas.filter { carga ->

                carga.folio.contains(
                    busqueda,
                    ignoreCase = true
                ) || carga.vendedor?.contains(
                    busqueda,
                    ignoreCase = true
                ) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.cargarCargas()
        if (esAdministrador && vendedores.isEmpty()) {
            viewModel.cargarFormulario()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esAdministrador) {
                            "Cargas"
                        } else {
                            "Mis Cargas"
                        }
                    )
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

            if (esAdministrador) {

                ExtendedFloatingActionButton(
                    onClick = onNuevaCarga,
                    icon = {

                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                    },
                    text = {

                        Text("Nueva Carga")
                    }
                )
            }
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
                placeholder = "Buscar por folio o vendedor"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (esAdministrador && vendedores.isNotEmpty()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    SelectorDesplegable(
                        etiqueta = "Filtrar por vendedor",
                        seleccionado = vendedorFiltro,
                        opciones = vendedores,
                        textoDe = { it.nombre },
                        onSeleccionar = {
                            viewModel.filtrarPorVendedor(it)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (vendedorFiltro != null) {

                        IconButton(
                            onClick = {
                                viewModel.quitarFiltroVendedor()
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
                            viewModel.cargarCargas()
                        }
                    )
                }

                filtrados.isEmpty() -> {

                    EmptyContent(
                        mensaje = if (busqueda.isNotBlank()) {
                            "Sin resultados para la búsqueda"
                        } else {
                            "No hay cargas registradas"
                        },
                        icono = Icons.Filled.LocalShipping
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
                        ) { carga ->

                            TarjetaCarga(
                                carga = carga,
                                onClick = {
                                    onVerDetalle(carga.id)
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
private fun TarjetaCarga(
    carga: Carga,
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
                    text = carga.folio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = carga.fecha ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = carga.vendedor ?: "—",
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
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = formatearCantidad(
                            carga.total_cantidad
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
