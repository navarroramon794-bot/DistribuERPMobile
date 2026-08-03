package com.distribuerp.mobile.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ItemMenu(
    val etiqueta: String,
    val icono: ImageVector,
    val ruta: String
)

val itemsMenu = listOf(
    ItemMenu(
        etiqueta = "Dashboard",
        icono = Icons.Filled.Dashboard,
        ruta = Rutas.DASHBOARD
    ),
    ItemMenu(
        etiqueta = "Clientes",
        icono = Icons.Filled.People,
        ruta = Rutas.CLIENTES
    ),
    ItemMenu(
        etiqueta = "Productos",
        icono = Icons.Filled.Inventory2,
        ruta = Rutas.PRODUCTOS
    ),
    ItemMenu(
        etiqueta = "Vendedores",
        icono = Icons.Filled.Badge,
        ruta = Rutas.VENDEDORES
    ),
    ItemMenu(
        etiqueta = "Inventario",
        icono = Icons.Filled.Inventory,
        ruta = Rutas.INVENTARIO
    ),
    ItemMenu(
        etiqueta = "Cargas",
        icono = Icons.Filled.LocalShipping,
        ruta = Rutas.CARGAS
    ),
    ItemMenu(
        etiqueta = "Proveedores",
        icono = Icons.Filled.Storefront,
        ruta = Rutas.PROVEEDORES
    ),
    ItemMenu(
        etiqueta = "Compras",
        icono = Icons.Filled.Receipt,
        ruta = Rutas.COMPRAS
    ),
    ItemMenu(
        etiqueta = "Nueva Venta",
        icono = Icons.Filled.PointOfSale,
        ruta = Rutas.NUEVA_VENTA
    ),
    ItemMenu(
        etiqueta = "Cobranza",
        icono = Icons.Filled.Payments,
        ruta = Rutas.COBRANZA
    ),
    ItemMenu(
        etiqueta = "Reportes",
        icono = Icons.Filled.BarChart,
        ruta = Rutas.REPORTES
    ),
    ItemMenu(
        etiqueta = "Configuración",
        icono = Icons.Filled.Settings,
        ruta = Rutas.CONFIGURACION
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContenido(
    nombreUsuario: String,
    correoUsuario: String,
    rutaActual: String?,
    onSeleccionar: (ItemMenu) -> Unit,
    onCerrarSesion: () -> Unit
) {

    ModalDrawerSheet {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "DistribuERP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = nombreUsuario,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = correoUsuario,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            itemsMenu.forEach { item ->

                NavigationDrawerItem(
                    label = {
                        Text(item.etiqueta)
                    },
                    icon = {

                        Icon(
                            imageVector = item.icono,
                            contentDescription = item.etiqueta
                        )
                    },
                    selected = item.ruta == rutaActual,
                    onClick = {

                        onSeleccionar(item)
                    },
                    modifier = Modifier.padding(
                        horizontal = 8.dp
                    )
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider()

            NavigationDrawerItem(
                label = {

                    Text("Cerrar sesión")
                },
                icon = {

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Cerrar sesión"
                    )
                },
                selected = false,
                onClick = onCerrarSesion,
                modifier = Modifier.padding(
                    horizontal = 8.dp
                )
            )
        }
    }
}
