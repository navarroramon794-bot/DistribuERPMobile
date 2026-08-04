package com.distribuerp.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.data.Contactos

/** Botones de contacto (WhatsApp, correo y sitio web) usados en las
 *  pantallas comerciales de licenciamiento. */
@Composable
fun BarraContactoComercial(
    mensajeWhatsApp: String = Contactos.MENSAJE_COMPRA,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                Contactos.intentWhatsApp(mensajeWhatsApp)
                    ?.let { contexto.startActivity(it) }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "WhatsApp",
                tint = Color(0xFF25D366),
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(
            onClick = {
                contexto.startActivity(
                    Contactos.intentCorreo(
                        asunto = "Información sobre DistribuERP",
                        cuerpo = "Hola, me gustaría recibir información sobre DistribuERP."
                    )
                )
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "Correo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(
            onClick = {
                contexto.startActivity(Contactos.intentWeb())
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Sitio web",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
