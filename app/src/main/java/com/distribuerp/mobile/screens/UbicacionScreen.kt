package com.distribuerp.mobile.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.distribuerp.mobile.viewmodel.UbicacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbicacionScreen(
    viewModel: UbicacionViewModel,
    vendedorId: Int?,
    onAbrirMenu: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var permisoConcedido by remember {
        mutableStateOf(viewModel.tienePermisoUbicacion(context))
    }

    var mostrarRationale by remember { mutableStateOf(false) }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        if (concedido) {
            viewModel.obtenerUbicacionActual()
        } else {
            mostrarRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (permisoConcedido) {
            viewModel.obtenerUbicacionActual()
        }
        vendedorId?.let { viewModel.cargarUltimaUbicacion(it) }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Ubicación") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Compartir ubicación GPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Activa el toggle para indicar que deseas compartir tu ubicación con el administrador. Presiona el botón para enviar tu ubicación actual al servidor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!permisoConcedido) {
                CardPermiso(
                    mostrarRationale = mostrarRationale,
                    onSolicitarPermiso = {
                        lanzadorPermiso.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    },
                    onAbrirConfiguracion = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compartir ubicación",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!permisoConcedido) {
                            "Sin permiso de ubicación"
                        } else if (viewModel.activo) {
                            "Activo"
                        } else {
                            "Inactivo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = viewModel.activo,
                    onCheckedChange = { activo ->
                        viewModel.cambiarEstadoUbicacion(activo)
                    },
                    enabled = !viewModel.cargando && permisoConcedido
                )
            }

            if (viewModel.ubicacionActual != null) {
                TarjetaUbicacion(
                    titulo = "Ubicación actual",
                    ubicacion = viewModel.ubicacionActual
                )
            }

            if (viewModel.ultimaUbicacion != null &&
                viewModel.ultimaUbicacion != viewModel.ubicacionActual
            ) {
                TarjetaUbicacion(
                    titulo = "Última ubicación enviada",
                    ubicacion = viewModel.ultimaUbicacion
                )
            }

            if (viewModel.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            viewModel.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            viewModel.mensaje?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    if (permisoConcedido) {
                        viewModel.obtenerUbicacionActual()
                    } else {
                        val activityLocal = activity
                        if (activityLocal != null &&
                            shouldShowRequestPermissionRationale(
                                activityLocal,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            mostrarRationale = true
                        } else {
                            lanzadorPermiso.launch(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.cargando
            ) {
                Text("Obtener ubicación")
            }

            Button(
                onClick = { viewModel.enviarUbicacionActual() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.cargando &&
                    permisoConcedido &&
                    viewModel.ubicacionActual != null
            ) {
                Text("Enviar ubicación ahora")
            }

            OutlinedButton(
                onClick = {
                    vendedorId?.let { viewModel.cargarUltimaUbicacion(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.cargando && vendedorId != null
            ) {
                Text("Consultar última ubicación")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CardPermiso(
    mostrarRationale: Boolean,
    onSolicitarPermiso: () -> Unit,
    onAbrirConfiguracion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permiso de ubicación requerido",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = if (mostrarRationale) {
                    "Es necesario acceder a la ubicación para que el administrador pueda conocer tu posición. Puedes habilitarlo desde la configuración de la aplicación."
                } else {
                    "La aplicación necesita permiso de ubicación para compartir tu posición con el administrador."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (mostrarRationale) {
                TextButton(
                    onClick = onAbrirConfiguracion,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Abrir configuración")
                }
            } else {
                Button(
                    onClick = onSolicitarPermiso,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Permitir ubicación")
                }
            }
        }
    }
}

@Composable
private fun TarjetaUbicacion(
    titulo: String,
    ubicacion: com.distribuerp.mobile.models.Ubicacion?
) {
    if (ubicacion == null) return

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
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Latitud: ${ubicacion.latitud}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Longitud: ${ubicacion.longitud}",
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
            ubicacion.fuente?.let {
                Text(
                    text = "Fuente: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ubicacion.fecha?.let {
                Text(
                    text = "Fecha: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
