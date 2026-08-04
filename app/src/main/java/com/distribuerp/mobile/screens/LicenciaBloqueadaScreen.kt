package com.distribuerp.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.ui.components.BarraContactoComercial
import com.distribuerp.mobile.ui.theme.AzulClaro
import com.distribuerp.mobile.ui.theme.AzulOscuro
import com.distribuerp.mobile.ui.theme.Naranja
import com.distribuerp.mobile.viewmodel.EstadoLicencia

data class InfoBloqueo(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector,
    val mostrarReintentar: Boolean = false
)

private fun infoBloqueo(
    estado: EstadoLicencia,
    mensaje: String?
): InfoBloqueo {
    return when (estado) {
        EstadoLicencia.Expirada -> InfoBloqueo(
            titulo = "Licencia vencida",
            descripcion = "Tu licencia ha vencido. Renueva tu licencia "
                + "para continuar usando DistribuERP.",
            icono = Icons.Filled.Warning,
            mostrarReintentar = true
        )

        EstadoLicencia.Suspendida -> InfoBloqueo(
            titulo = "Licencia suspendida",
            descripcion = "Tu licencia fue suspendida. Comunícate con tu "
                + "proveedor para resolver la situación.",
            icono = Icons.Filled.Block,
            mostrarReintentar = true
        )

        EstadoLicencia.Cancelada -> InfoBloqueo(
            titulo = "Licencia cancelada",
            descripcion = "Tu licencia fue cancelada. Si consideras que es "
                + "un error, contáctanos.",
            icono = Icons.Filled.Block,
            mostrarReintentar = true
        )

        EstadoLicencia.SinInternet -> InfoBloqueo(
            titulo = "Sin conexión",
            descripcion = "No se pudo validar tu licencia porque superaste "
                + "el periodo de tolerancia sin internet. Conéctate a la red "
                + "e inténtalo de nuevo.",
            icono = Icons.Filled.WifiOff,
            mostrarReintentar = true
        )

        else -> InfoBloqueo(
            titulo = "No se pudo validar",
            descripcion = mensaje?.takeIf { it.isNotBlank() }
                ?: "Ocurrió un problema al validar tu licencia. "
                    + "Inténtalo de nuevo.",
            icono = Icons.Filled.Warning,
            mostrarReintentar = true
        )
    }
}

@Composable
fun LicenciaBloqueadaScreen(
    estado: EstadoLicencia,
    mensaje: String?,
    onReintentar: () -> Unit,
    onActivar: () -> Unit
) {
    val info = infoBloqueo(estado, mensaje)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AzulOscuro, AzulClaro, Naranja)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = info.icono,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = info.titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = info.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onActivar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        Text("Activar Licencia")
                    }

                    if (info.mostrarReintentar) {
                        OutlinedButton(
                            onClick = onReintentar,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            Text("Reintentar")
                        }
                    }

                    Text(
                        text = "¿Necesitas ayuda? Contáctanos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    BarraContactoComercial()
                }
            }
        }
    }
}
