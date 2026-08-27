package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.viewmodel.MonitoreoUbicacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoreoUbicacionScreen(
    viewModel: MonitoreoUbicacionViewModel,
    onAbrirMenu: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.cargarUbicaciones()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación de vendedores") },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menú"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Button(
                onClick = { viewModel.cargarUbicaciones() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.cargando
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Actualizar ubicaciones")
            }

            viewModel.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when {
                viewModel.cargando -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.ubicaciones.isEmpty() -> {
                    Text(
                        text = "Actualmente no hay vendedores compartiendo ubicación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = viewModel.ubicaciones,
                            key = { it.vendedor_id ?: it.hashCode() }
                        ) { ubicacion ->
                            TarjetaVendedorUbicacion(ubicacion = ubicacion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaVendedorUbicacion(
    ubicacion: Ubicacion
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ubicacion.vendedor ?: "Vendedor ${ubicacion.vendedor_id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = if (ubicacion.activo) "Activo" else "Inactivo",
                style = MaterialTheme.typography.bodySmall,
                color = if (ubicacion.activo) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Ubicación: ${ubicacion.latitud}, ${ubicacion.longitud}",
                style = MaterialTheme.typography.bodyMedium
            )

            ubicacion.precision_m?.let {
                Text(
                    text = "Precisión: %.1f m".format(it),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ubicacion.velocidad?.let {
                Text(
                    text = "Velocidad: %.1f m/s".format(it),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ubicacion.fecha?.let {
                Text(
                    text = "Última actualización: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
