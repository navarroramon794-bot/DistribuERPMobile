package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.data.Contactos
import com.distribuerp.mobile.models.LicenciaInfo
import com.distribuerp.mobile.ui.components.FilaInformacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenciaScreen(
    licencia: LicenciaInfo?,
    diasRestantes: Int?,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current

    val info = licencia ?: LicenciaInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Licencia")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (info.esDemo) {
                            "Versión Demo"
                        } else {
                            info.estadoLegible()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    diasRestantes?.let { dias ->

                        Text(
                            text = if (info.esPermanente) {
                                "Licencia permanente"
                            } else {
                                "Quedan $dias días"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (info.esDemo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Estás usando la versión de demostración.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Adquiere tu licencia para no interrumpir "
                                + "tu operación cuando termine el periodo "
                                + "de prueba.",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                Contactos.intentWhatsApp()
                                    ?.let { contexto.startActivity(it) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            Text("Comprar Licencia")
                        }
                    }
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
                        text = "Detalle de la licencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Business,
                        etiqueta = "Empresa",
                        valor = info.empresa ?: "—"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.VerifiedUser,
                        etiqueta = "Estado",
                        valor = info.estadoLegible()
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Storefront,
                        etiqueta = "Plan",
                        valor = info.planLegible()
                    )

                    FilaInformacion(
                        icono = Icons.Filled.SystemUpdate,
                        etiqueta = "Versión de la app",
                        valor = "v${BuildConfig.APP_VERSION}"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Schedule,
                        etiqueta = "Fecha de inicio",
                        valor = info.fecha_inicio ?: "—"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Schedule,
                        etiqueta = "Fecha de vencimiento",
                        valor = if (info.esPermanente) {
                            "Permanente"
                        } else {
                            info.fecha_vencimiento ?: "—"
                        }
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Schedule,
                        etiqueta = "Días restantes",
                        valor = if (info.esPermanente) {
                            "—"
                        } else {
                            (diasRestantes ?: info.dias_restantes ?: 0)
                                .toString()
                        }
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Última validación",
                        valor = info.ultima_validacion ?: "—"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Key,
                        etiqueta = "Código",
                        valor = info.codigoEnmascarado()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.size(6.dp))

                Text(
                    text = "Esta información es de solo lectura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
