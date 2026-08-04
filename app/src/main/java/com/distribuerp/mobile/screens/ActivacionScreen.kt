package com.distribuerp.mobile.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.data.Contactos
import com.distribuerp.mobile.ui.components.BarraContactoComercial
import com.distribuerp.mobile.ui.theme.AzulClaro
import com.distribuerp.mobile.ui.theme.AzulOscuro
import com.distribuerp.mobile.ui.theme.Naranja

@Composable
fun ActivacionScreen(
    cargando: Boolean,
    mensaje: String?,
    onActivar: () -> Unit,
    onProbarDemo: () -> Unit
) {
    val contexto = LocalContext.current

    val gradiente = Brush.verticalGradient(
        listOf(AzulOscuro, AzulClaro, Naranja)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradiente)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            LogoAnimado()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DistribuERP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "El sistema para controlar tu distribución",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Comienza a usar DistribuERP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Elige cómo deseas continuar:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onActivar,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulOscuro
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        Text("Activar Licencia")
                    }

                    OutlinedButton(
                        onClick = {
                            Contactos.intentWhatsApp(
                                Contactos.MENSAJE_DEMOSTRACION
                            )?.let { contexto.startActivity(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF25D366)
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        Text("Solicitar una Demostración")
                    }

                    FilledTonalButton(
                        onClick = onProbarDemo,
                        enabled = !cargando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        Text("Probar Demo (15 días)")
                    }

                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.CenterHorizontally),
                            strokeWidth = 2.dp
                        )
                    }

                    mensaje?.let { texto ->
                        Text(
                            text = texto,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¿Necesitas ayuda? Contáctanos",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            BarraContactoComercial(
                mensajeWhatsApp = Contactos.MENSAJE_DEMOSTRACION
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "v${BuildConfig.APP_VERSION} · C&R Technology Solutions",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LogoAnimado() {
    val transicion = rememberInfiniteTransition(label = "logo")

    val escala by transicion.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala_logo"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .scale(escala)
            .background(
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = "DistribuERP",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}
