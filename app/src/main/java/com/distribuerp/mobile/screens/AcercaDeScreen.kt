package com.distribuerp.mobile.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.R
import com.distribuerp.mobile.ui.components.FilaInformacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcercaDeScreen(
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Acerca de")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.splash_isotipo
                        ),
                        contentDescription = "Logotipo de DistribuERP",
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "DistribuERP Mobile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Gestión de ventas y cobranzas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "v${BuildConfig.APP_VERSION}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Aplicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Versión",
                        valor = BuildConfig.APP_VERSION
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Build",
                        valor = BuildConfig.VERSION_CODE.toString()
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Compatibilidad",
                        valor = "Android 8.0 (API 26) o superior"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.CheckCircle,
                        etiqueta = "Ambiente",
                        valor = "Producción (Render)"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Storage,
                        etiqueta = "Servidor",
                        valor = "https://distribu-erp.onrender.com/"
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Desarrollador",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Code,
                        etiqueta = "Empresa",
                        valor = "C&R Technology Solutions"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Web,
                        etiqueta = "Plataforma",
                        valor = "Android (Kotlin)"
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Contacto y soporte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Web,
                        etiqueta = "Sitio web",
                        valor = "https://distribu-erp.onrender.com/"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Email,
                        etiqueta = "Correo",
                        valor = "soporte@distribuerp.com"
                    )

                    FilaInformacion(
                        icono = Icons.AutoMirrored.Filled.Chat,
                        etiqueta = "WhatsApp",
                        valor = "+52 33 0000 0000"
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Legal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Lock,
                        etiqueta = "Licencia",
                        valor = "Uso autorizado para clientes de DistribuERP"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Copyright,
                        etiqueta = "Copyright",
                        valor = "© 2026 C&R Technology Solutions"
                    )
                }
            }

            Text(
                text = "DistribuERP © 2026 · C&R Technology Solutions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
