package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.TipoReporte

private data class OpcionReporte(
    val tipo: TipoReporte,
    val descripcion: String,
    val icono: ImageVector
)

private val opcionesReporte = listOf(
    OpcionReporte(
        tipo = TipoReporte.VENTAS,
        descripcion = "Historial completo de ventas por rango de fechas",
        icono = Icons.AutoMirrored.Filled.ReceiptLong
    ),
    OpcionReporte(
        tipo = TipoReporte.PAGOS,
        descripcion = "Pagos y cobranzas registradas por rango de fechas",
        icono = Icons.Filled.Payments
    ),
    OpcionReporte(
        tipo = TipoReporte.PRODUCTOS,
        descripcion = "Productos con mayor cantidad e importe vendido",
        icono = Icons.Filled.ShoppingCart
    ),
    OpcionReporte(
        tipo = TipoReporte.CLIENTES,
        descripcion = "Clientes con saldo pendiente de pago",
        icono = Icons.Filled.AccountBalance
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    onAbrirMenu: () -> Unit,
    onAbrirReporte: (String) -> Unit
) {
    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text("Reportes")
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Text(
                    text = "Selecciona un reporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                count = opcionesReporte.size,
                key = { opcionesReporte[it].tipo.clave }
            ) { indice ->

                val opcion = opcionesReporte[indice]

                TarjetaReporte(
                    opcion = opcion,
                    onClick = {

                        onAbrirReporte(opcion.tipo.clave)
                    }
                )
            }
        }
    }
}

@Composable
private fun TarjetaReporte(
    opcion: OpcionReporte,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = opcion.icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = opcion.tipo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = opcion.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
