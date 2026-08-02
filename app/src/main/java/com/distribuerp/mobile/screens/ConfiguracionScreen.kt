package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    onAbrirMenu: () -> Unit,
    onAbrirImpresora: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
    onAbrirAcercaDe: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Configuración")
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
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Configuración de la aplicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OpcionConfiguracion(
                titulo = "Impresora Bluetooth",
                descripcion = "Selecciona y configura la impresora de tickets",
                icono = Icons.Filled.Print,
                habilitada = true,
                onClick = onAbrirImpresora
            )

            OpcionConfiguracion(
                titulo = "Diagnóstico",
                descripcion = "Estado del servidor, impresora y versión",
                icono = Icons.Filled.Build,
                habilitada = true,
                onClick = onAbrirDiagnostico
            )

            OpcionConfiguracion(
                titulo = "Acerca de",
                descripcion = "Información de la aplicación y el desarrollador",
                icono = Icons.Filled.Info,
                habilitada = true,
                onClick = onAbrirAcercaDe
            )

            Text(
                text = "Próximamente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            OpcionConfiguracion(
                titulo = "Empresa",
                descripcion = "Datos de la empresa",
                icono = Icons.Filled.Settings,
                habilitada = false,
                onClick = null
            )

            OpcionConfiguracion(
                titulo = "Licencia",
                descripcion = "Información de la licencia",
                icono = Icons.Filled.Favorite,
                habilitada = false,
                onClick = null
            )

            OpcionConfiguracion(
                titulo = "Actualizaciones",
                descripcion = "Buscar nuevas versiones de la aplicación",
                icono = Icons.Filled.SystemUpdate,
                habilitada = false,
                onClick = null
            )
        }
    }
}

@Composable
private fun OpcionConfiguracion(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    habilitada: Boolean,
    onClick: (() -> Unit)?
) {
    if (habilitada && onClick != null) {

        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {

            ContenidoOpcion(
                titulo = titulo,
                descripcion = descripcion,
                icono = icono,
                habilitada = true
            )
        }

    } else {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {

            ContenidoOpcion(
                titulo = titulo,
                descripcion = descripcion,
                icono = icono,
                habilitada = false
            )
        }
    }
}

@Composable
private fun ContenidoOpcion(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    habilitada: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (habilitada) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(28.dp)
        )

        Spacer(
            modifier = Modifier.width(16.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (habilitada) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(
                modifier = Modifier.size(2.dp)
            )

            Text(
                text = if (habilitada) descripcion else "Próximamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (habilitada) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
