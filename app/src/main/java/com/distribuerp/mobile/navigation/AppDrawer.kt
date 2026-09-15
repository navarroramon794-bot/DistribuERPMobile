package com.distribuerp.mobile.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.R
import com.distribuerp.mobile.data.Roles

data class ItemMenu(val etiqueta: String, val icono: ImageVector, val ruta: String)

val itemsMenuAdministrador = listOf(
    ItemMenu("Dashboard", Icons.Filled.Dashboard, Rutas.DASHBOARD),
    ItemMenu("Clientes", Icons.Filled.People, Rutas.CLIENTES),
    ItemMenu("Productos", Icons.Filled.Inventory2, Rutas.PRODUCTOS),
    ItemMenu("Vendedores", Icons.Filled.Badge, Rutas.VENDEDORES),
    ItemMenu("Ubicación de vendedores", Icons.Filled.LocationOn, Rutas.MONITOREO_UBICACION),
    ItemMenu("Inventario", Icons.Filled.Inventory, Rutas.INVENTARIO),
    ItemMenu("Cargas", Icons.Filled.LocalShipping, Rutas.CARGAS),
    ItemMenu("Proveedores", Icons.Filled.Storefront, Rutas.PROVEEDORES),
    ItemMenu("Compras", Icons.Filled.Receipt, Rutas.COMPRAS),
    ItemMenu("Nueva Venta", Icons.Filled.PointOfSale, Rutas.NUEVA_VENTA),
    ItemMenu("Cobranza", Icons.Filled.Payments, Rutas.COBRANZA),
    ItemMenu("Reportes", Icons.Filled.BarChart, Rutas.REPORTES),
    ItemMenu("Configuración", Icons.Filled.Settings, Rutas.CONFIGURACION)
)

val itemsMenuVendedor = listOf(
    ItemMenu("Dashboard", Icons.Filled.Dashboard, Rutas.DASHBOARD),
    ItemMenu("Clientes", Icons.Filled.People, Rutas.CLIENTES),
    ItemMenu("Mi Inventario", Icons.Filled.Inventory, Rutas.INVENTARIO),
    ItemMenu("Nueva Venta", Icons.Filled.PointOfSale, Rutas.NUEVA_VENTA),
    ItemMenu("Historial de Ventas", Icons.AutoMirrored.Filled.ReceiptLong, Rutas.reporteForm("ventas")),
    ItemMenu("Nueva Cobranza", Icons.Filled.Payments, Rutas.COBRANZA),
    ItemMenu("Historial de Cobranza", Icons.Filled.BarChart, Rutas.reporteForm("pagos")),
    ItemMenu("Mis Cargas", Icons.Filled.LocalShipping, Rutas.CARGAS),
    ItemMenu("Mi Ubicación", Icons.Filled.LocationOn, Rutas.UBICACION),
    ItemMenu("Configuración", Icons.Filled.Settings, Rutas.CONFIGURACION)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContenido(
    nombreUsuario: String,
    correoUsuario: String,
    rol: String?,
    rutaActual: String?,
    onSeleccionar: (ItemMenu) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val esAdministrador = rol == Roles.ADMINISTRADOR
    val items = if (esAdministrador) itemsMenuAdministrador else itemsMenuVendedor

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            DrawerHeader(nombreUsuario, correoUsuario, if (esAdministrador) "Administrador" else "Vendedor")
            Spacer(Modifier.height(12.dp))
            var seccionAnterior: String? = null
            items.forEach { item ->
                val seccion = seccionMenu(item, esAdministrador)
                if (seccion != seccionAnterior) {
                    DrawerSectionLabel(seccion)
                    seccionAnterior = seccion
                }
                NavigationDrawerItem(
                    label = {
                        Text(item.etiqueta, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (item.ruta == rutaActual) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    icon = { Icon(item.icono, contentDescription = item.etiqueta, modifier = Modifier.size(21.dp)) },
                    selected = item.ruta == rutaActual,
                    onClick = { onSeleccionar(item) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(6.dp))
            NavigationDrawerItem(
                label = { Text("Cerrar sesión", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", modifier = Modifier.size(21.dp)) },
                selected = false,
                onClick = onCerrarSesion,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(14.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedIconColor = MaterialTheme.colorScheme.error,
                    unselectedTextColor = MaterialTheme.colorScheme.error
                )
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerHeader(nombre: String, correo: String, rol: String) {
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                    Image(painter = painterResource(R.drawable.splash_isotipo), contentDescription = "DistribuERP", modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DistribuERP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Mobile", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.76f))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(correo, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.14f)) {
                Text(rol.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 10.dp, bottom = 4.dp))
}

private fun seccionMenu(item: ItemMenu, esAdministrador: Boolean): String = when (item.ruta) {
    Rutas.DASHBOARD -> "PRINCIPAL"
    Rutas.NUEVA_VENTA, Rutas.COBRANZA -> "GESTIÓN COMERCIAL"
    Rutas.REPORTES, Rutas.CONFIGURACION -> if (esAdministrador) "ADMINISTRACIÓN" else "CUENTA E HISTORIAL"
    else -> "OPERACIÓN"
}
