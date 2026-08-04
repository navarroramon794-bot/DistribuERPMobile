package com.distribuerp.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.data.Contactos
import com.distribuerp.mobile.ui.theme.AzulClaro
import com.distribuerp.mobile.ui.theme.AzulOscuro
import com.distribuerp.mobile.ui.theme.Naranja

/** Banner "VERSIÓN DEMO" visible en todas las pantallas mientras la
 *  licencia de demostración está vigente. */
@Composable
fun BannerDemo(
    diasRestantes: Int?,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Naranja, AzulClaro, AzulOscuro)
                )
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface
                            .copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(modifier = Modifier.width(8.dp))

            Text(
                text = "VERSIÓN DEMO · " +
                    (diasRestantes?.let { "Quedan $it días" } ?: "Quedan 0 días"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        TextButton(
            onClick = {
                Contactos.intentWhatsApp()
                    ?.let { contexto.startActivity(it) }
            }
        ) {
            Text(
                text = "Comprar Licencia",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
