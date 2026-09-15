package com.distribuerp.mobile.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.viewmodel.UbicacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbicacionScreen(
    viewModel: UbicacionViewModel,
    vendedorId: Int?,
    esVendedor: Boolean,
    onAbrirMenu: () -> Unit
) {
    val contexto = LocalContext.current

    var permisosRefresco by remember { mutableIntStateOf(0) }
    var paso by remember { mutableIntStateOf(0) }       // 0 idle, 1 ubicación, 2 notificaciones, 3 segundo plano, 4 iniciar
    var intencion by remember { mutableIntStateOf(0) }  // 0 ninguna, 1 activar tracking, 2 obtener ubicación
    var aviso by remember { mutableStateOf<String?>(null) }

    val permisoUbicacion = remember(permisosRefresco) {
        viewModel.tienePermisoUbicacion(contexto)
    }
    val gpsEncendido = viewModel.isGpsEnabled(contexto)
    val permisoNotificacion = remember(permisosRefresco) {
        viewModel.tienePermisoNotificacion(contexto)
    }
    val permisoBackground = remember(permisosRefresco) {
        viewModel.tienePermisoBackground(contexto)
    }

    val lanzadorUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permisosRefresco++
        when {
            intencion == 1 && !viewModel.tienePermisoUbicacion(contexto) ->
                aviso = "Se requiere el permiso de ubicación para compartirla."
            intencion == 1 -> paso = 2
            intencion == 2 && viewModel.tienePermisoUbicacion(contexto) -> {
                intencion = 0
                viewModel.obtenerUbicacionActual()
            }
        }
    }

    val lanzadorNotificacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        permisosRefresco++
        if (intencion == 1) paso = 3
    }

    val lanzadorBackground = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        permisosRefresco++
        if (intencion == 1) paso = 4
    }

    LaunchedEffect(paso) {
        when (paso) {
            1 -> {
                if (viewModel.tienePermisoUbicacion(contexto)) {
                    paso = 2
                } else {
                    lanzadorUbicacion.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
            2 -> {
                if (Build.VERSION.SDK_INT >= 33 &&
                    !viewModel.tienePermisoNotificacion(contexto)
                ) {
                    lanzadorNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    paso = 3
                }
            }
            3 -> {
                if (Build.VERSION.SDK_INT >= 29 &&
                    !viewModel.tienePermisoBackground(contexto)
                ) {
                    lanzadorBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    paso = 4
                }
            }
            4 -> {
                intencion = 0
                viewModel.cambiarEstadoUbicacion(contexto, true)
                paso = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        if (esVendedor && vendedorId != null) {
            viewModel.cargarUltimaUbicacion(vendedorId)
        }
    }

    LaunchedEffect(esVendedor, vendedorId, viewModel.activo) {
        if (esVendedor && vendedorId != null) {
            viewModel.sincronizarTracking(contexto, vendedorId, esVendedor)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Ubicación",
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
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (!esVendedor || vendedorId == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "El tracking de ubicación está disponible solo para vendedores con un vendedor asignado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {

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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewModel.activo)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (viewModel.activo)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compartir ubicación",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (viewModel.activo)
                                    "Compartiendo con el administrador"
                                else
                                    "El administrador podrá verte en el monitoreo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = viewModel.activo,
                            onCheckedChange = { nuevo ->
                                aviso = null
                                if (nuevo) {
                                    intencion = 1
                                    paso = 1
                                } else {
                                    intencion = 0
                                    viewModel.cambiarEstadoUbicacion(contexto, false)
                                }
                            }
                        )
                    }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Estado del dispositivo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilaEstado(
                            etiqueta = "Permiso de ubicación",
                            ok = permisoUbicacion
                        )
                        FilaEstado(
                            etiqueta = "GPS del dispositivo",
                            ok = gpsEncendido
                        )
                        if (Build.VERSION.SDK_INT >= 33) {
                            FilaEstado(
                                etiqueta = "Notificaciones",
                                ok = permisoNotificacion
                            )
                        }
                        if (Build.VERSION.SDK_INT >= 29) {
                            FilaEstado(
                                etiqueta = "Ubicación en segundo plano",
                                ok = permisoBackground
                            )
                        }
                    }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Última ubicación enviada",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val ultima = viewModel.ultimaUbicacion
                        if (ultima == null) {
                            Text(
                                text = "Aún no se ha enviado ninguna ubicación.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            DatosUbicacion(ubicacion = ultima)
                        }
                    }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Mi ubicación actual",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val actual = viewModel.ubicacionActual

                        OutlinedButton(
                            onClick = {
                                aviso = null
                                if (viewModel.tienePermisoUbicacion(contexto)) {
                                    viewModel.obtenerUbicacionActual()
                                } else {
                                    intencion = 2
                                    lanzadorUbicacion.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !viewModel.cargando,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Obtener ubicación actual")
                        }

                        if (viewModel.cargando) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        if (actual != null) {
                            DatosUbicacion(ubicacion = actual)

                            Button(
                                onClick = { viewModel.enviarUbicacionActual() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar ubicación")
                            }
                        }

                        Text(
                            text = "El envío automático ocurre mientras compartes tu ubicación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            aviso?.let { texto ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = texto,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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

            viewModel.mensaje?.let { mensaje ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = mensaje,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DatosUbicacion(ubicacion: Ubicacion) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
            FilaDato("Fecha", it)
        }
    }
}

@Composable
private fun FilaEstado(
    etiqueta: String,
    ok: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (ok)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = if (ok) "OK" else "Faltante",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (ok)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
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