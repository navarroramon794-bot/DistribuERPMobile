package com.distribuerp.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.distribuerp.mobile.R
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.viewmodel.MonitoreoUbicacionViewModel
import com.distribuerp.mobile.utils.GpsConfig
import kotlin.runWithCoroutines

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbicacionScreen(
    viewModel: MonitoreoUbicacionViewModel,
    vendedorId: Int?,
    onAbrirMenu: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var permisoConcedido by remember {
        mutableStateOf(viewModel::class.java.getDeclaredMethod("tienePermisoUbicacion", context::class.java).invoke(null, context) as Boolean)
    }

    var mostrarRationale by remember { mutableStateOf(false) }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        if (concedido) {
            // Solicitar ubicaciones después de conceder permiso
            viewModel.cargarUbicaciones()
        } else {
            mostrarRationale = true
        }
    }

    // Lifecycle: iniciar polling al entrar, detener al salir
    DisposableEffect(Unit) {
        onDispose {
            viewModel.detenerPolling()
        }
    }

    // Iniciar polling cuando la pantalla es visible y hay permiso
    LaunchedEffect(Unit) {
        if (permisoConcedido) {
            viewModel.iniciarPolling()
        }
    }

    // Actualizar filtro sin crear polling duplicado
    LaunchedEffect(filtroVendedorId) {
        // Solo actualiza el filtro, el polling ya está corriendo
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Monitoreo GPS",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menú"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        } { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Estado del polling ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador de estado de polling
                    val (estadoTexto, estadoColor) = when {
                        viewModel.cargando -> ("Actualizando...", MaterialTheme.colorScheme.primary)
                        viewModel.ubicaciones.isEmpty() && !viewModel.cargando -> ("Sin datos", MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> ("Actualizado", MaterialTheme.colorScheme.secondary)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = estadoColor,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (viewModel.cargando) Icons.Filled.Refresh else Icons.Fixed.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = estadoTexto,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Actualización cada ${GpsConfig.ADMIN_POLL_INTERVAL_MS / 1000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Filtro por vendedor ───────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtrar por vendedor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.setFiltro(null) },
                        modifier = Modifier.size(80.dp),
                        enabled = !viewModel.cargando,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.filtroVendedorId == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Todos")
                    }

                    if (viewModel.ubicaciones.isNotEmpty()) {
                        // Simulación de filtro - en implementación completa usaría un spinner
                        Text(
                            text = "ID: ${viewModel.filtroVendedorId ?: "todos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Lista de ubicaciones con indicadores de estado ────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (viewModel.ubicaciones.isEmpty()) {
                        Text(
                            text = "Sin ubicaciones registradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        viewModel.ubicaciones.forEach { ubicacion ->
                            // Calcular estado de la ubicación
                            val (edad, tipoEstado) = viewModel.estadoAgrupado(ubicacion.fecha)
                            val esVendedorSeleccionado = viewModel.filtroVendedorId == null || ubicacion.vendedor_id == viewModel.filtroVendedorId

                            if (esVendedorSeleccionado) {
                                LocationCard(
                                    ubicacion = ubicacion,
                                    estadoTipo = tipoEstado,
                                    edadTexto = edad
                                )
                            }
                        }
                    }
                }
            }

            // ── Loading ────────────────────────────────
            if (viewModel.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // ── Mensajes ───────────────────────────────
            viewModel.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun LocationCard(
    ubicacion: Ubicacion,
    estadoTipo: String,
    edadTexto: String
) {
    val colorEstado = when (estadoTipo) {
        "reciente" -> MaterialTheme.colorScheme.primary
        "antigua" -> MaterialTheme.colorScheme.warning
        "desactualizada" -> MaterialTheme.colorScheme.error
        "sin ubicación" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            padding = 16.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colorEstado,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Vendedor ${ubicacion.vendedor_id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilaDato("Latitud", "%.6f".format(ubicacion.latitud))
                FilaDato("Longitud", "%.6f".format(ubicacion.longitud))
                ubicacion.precision_m?.let {
                    FilaDato("Precisión", "%.1f m".format(it))
                }
                ubicacion.velocidad?.let {
                    FilaDato("Velocidad", "%.1f m/s".format(it))
                }
                ubicacion.fuente?.let {
                    FilaDato("Fuente", it)
                }
                ubicacion.fecha?.let {
                    FilaDato("Última actualización", it)
                }
                // Indicador de estado
                HLine()
                Text(
                    text = "Estado: ${estadoTipo.capitalize()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colorEstado
                )
                Text(
                    text = "(${edadTexto})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorEstado.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HLine() {
    Canvas(modifier = Modifier.height(1.dp).fillMaxWidth()) {
        drawRect(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            size = Size(CanvasWidth, 1.dp)
        )
    }
}

@Composable
private fun FilaDato(
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}