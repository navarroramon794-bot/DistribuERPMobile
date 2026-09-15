package com.distribuerp.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distribuerp.mobile.data.Roles
import com.distribuerp.mobile.ui.components.ErrorContent
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.formatearCantidad
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.viewmodel.AuthViewModel
import com.distribuerp.mobile.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
    dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    onAbrirMenu: () -> Unit = {},
    onNavegarClientes: () -> Unit = {}
) {
    val loading = dashboardViewModel.loading
    val dashboard = dashboardViewModel.dashboard
    val error = dashboardViewModel.error
    val sesion by authViewModel.sesion.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) dashboardViewModel.cargar()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nombre = when (sesion?.rol) {
        Roles.ADMINISTRADOR -> "Administrador"
        Roles.VENDEDOR -> sesion?.vendedor ?: sesion?.nombre ?: "Vendedor"
        else -> "Usuario"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DistribuERP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Panel comercial", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.76f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) { Icon(Icons.Filled.Menu, contentDescription = "Abrir menú") }
                },
                actions = {
                    IconButton(onClick = { dashboardViewModel.cargar() }) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar resumen") }
                    IconButton(onClick = { authViewModel.cerrarSesion() }) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavegarClientes,
                icon = { Icon(Icons.Filled.People, contentDescription = null) },
                text = { Text("Clientes", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
        ) {
            when {
                loading -> LoadingContent()
                error != null -> ErrorContent(error, onReintentar = { dashboardViewModel.cargar() })
                dashboard != null -> {
                    val paleta = paletaKpis()
                    val tarjetas = listOf(
                        TarjetaDatos("Ventas hoy", formatearDinero(dashboard.ventas_hoy), "Operación del día", Icons.Filled.AttachMoney, paleta.azul),
                        TarjetaDatos("Cobrado hoy", formatearDinero(dashboard.cobrado_hoy), "Cobranza registrada", Icons.Filled.Payments, paleta.verde),
                        TarjetaDatos("Pendiente", formatearDinero(dashboard.saldo_pendiente), "Saldo por cobrar", Icons.Filled.Warning, paleta.rojo),
                        TarjetaDatos("Clientes", dashboard.total_clientes.toString(), "Cartera activa", Icons.Filled.People, paleta.morado),
                        TarjetaDatos("Inventario", formatearCantidad(dashboard.inventario_total), "Productos en stock", Icons.Filled.Inventory, paleta.naranja),
                        TarjetaDatos("Vendedores", dashboard.total_vendedores.toString(), "Equipo comercial", Icons.Filled.Person, paleta.neutro)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 92.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) { DashboardEncabezado(nombre) }
                        items(tarjetas, key = { it.titulo }) { tarjeta -> TarjetaKpi(tarjeta) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardEncabezado(nombre: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("HOY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text("Hola, $nombre", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Aquí está el pulso de tu operación.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f))
        }
    }
}

@Composable
private fun TarjetaKpi(tarjeta: TarjetaDatos) = Card(
    modifier = Modifier.fillMaxWidth().height(146.dp), shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = tarjeta.estilo.fondo), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = tarjeta.estilo.iconoFondo) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(tarjeta.icono, tarjeta.titulo, tint = tarjeta.estilo.contenido, modifier = Modifier.size(22.dp))
            }
        }
        Column {
            Text(tarjeta.titulo, style = MaterialTheme.typography.labelMedium, color = tarjeta.estilo.contenido.copy(alpha = 0.86f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tarjeta.valor, style = MaterialTheme.typography.titleLarge, color = tarjeta.estilo.contenido, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tarjeta.detalle, style = MaterialTheme.typography.labelSmall, color = tarjeta.estilo.contenido.copy(alpha = 0.76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun paletaKpis() = if (isSystemInDarkTheme()) PaletaKpi(
    EstiloKpi(Color(0xFF0753B5), Color.White, Color.White.copy(alpha = 0.18f)), EstiloKpi(Color(0xFF176B38), Color.White, Color.White.copy(alpha = 0.18f)),
    EstiloKpi(Color(0xFFB72B26), Color.White, Color.White.copy(alpha = 0.18f)), EstiloKpi(Color(0xFF5635A8), Color.White, Color.White.copy(alpha = 0.18f)),
    EstiloKpi(Color(0xFFC46600), Color.White, Color.White.copy(alpha = 0.18f)), EstiloKpi(Color(0xFF334155), Color.White, Color.White.copy(alpha = 0.18f))
) else PaletaKpi(
    EstiloKpi(Color(0xFF1769C2), Color.White, Color.White.copy(alpha = 0.20f)), EstiloKpi(Color(0xFF2E8B46), Color.White, Color.White.copy(alpha = 0.20f)),
    EstiloKpi(Color(0xFFD93B32), Color.White, Color.White.copy(alpha = 0.20f)), EstiloKpi(Color(0xFF6542B7), Color.White, Color.White.copy(alpha = 0.20f)),
    EstiloKpi(Color(0xFFF28A00), Color.White, Color.White.copy(alpha = 0.20f)), EstiloKpi(Color(0xFF475569), Color.White, Color.White.copy(alpha = 0.20f))
)

private data class PaletaKpi(val azul: EstiloKpi, val verde: EstiloKpi, val rojo: EstiloKpi, val morado: EstiloKpi, val naranja: EstiloKpi, val neutro: EstiloKpi)
private data class EstiloKpi(val fondo: Color, val contenido: Color, val iconoFondo: Color)
private data class TarjetaDatos(val titulo: String, val valor: String, val detalle: String, val icono: ImageVector, val estilo: EstiloKpi)
