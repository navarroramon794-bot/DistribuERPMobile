package com.distribuerp.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.distribuerp.mobile.data.Contactos
import com.distribuerp.mobile.ui.theme.AzulOscuro

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
            .background(AzulOscuro)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "VERSIÓN DEMO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.8.sp
            )

            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f)
            )

            Text(
                text = diasRestantes?.let { "Quedan $it días" } ?: "Quedan 0 días",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF90CAF9)
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF90CAF9)
            )
        }
    }
}
