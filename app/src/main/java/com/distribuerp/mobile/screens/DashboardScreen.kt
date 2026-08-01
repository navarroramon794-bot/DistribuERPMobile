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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {

                        Text(
                            text = "DistribuERP Mobile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Administrador",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                alpha = 0.8f
                            )
                        )
                    }
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
                },
                actions = {

                    IconButton(
                        onClick = {
                            dashboardViewModel.cargar()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }

                    IconButton(
                        onClick = {
                            authViewModel.cerrarSesion()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onNavegarClientes,
                icon = {

                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null
                    )
                },
                text = {

                    Text("Clientes")
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            when {

                loading -> {

                    LoadingContent()
                }

                error != null -> {

                    ErrorContent(
                        error = error,
                        onReintentar = {
                            dashboardViewModel.cargar()
                        }
                    )
                }

                dashboard != null -> {

                    val colores = coloresKpis()

                    val tarjetas = listOf(
                        TarjetaDatos(
                            titulo = "Ventas Hoy",
                            valor = formatearDinero(dashboard.ventas_hoy),
                            icono = Icons.Filled.AttachMoney,
                            contenedor = colores.verde.contenedor,
                            contenido = colores.verde.contenido
                        ),
                        TarjetaDatos(
                            titulo = "Cobrado Hoy",
                            valor = formatearDinero(dashboard.cobrado_hoy),
                            icono = Icons.Filled.Payments,
                            contenedor = colores.azul.contenedor,
                            contenido = colores.azul.contenido
                        ),
                        TarjetaDatos(
                            titulo = "Pendiente",
                            valor = formatearDinero(dashboard.saldo_pendiente),
                            icono = Icons.Filled.Warning,
                            contenedor = colores.rojo.contenedor,
                            contenido = colores.rojo.contenido
                        ),
                        TarjetaDatos(
                            titulo = "Clientes",
                            valor = dashboard.total_clientes.toString(),
                            icono = Icons.Filled.People,
                            contenedor = colores.morado.contenedor,
                            contenido = colores.morado.contenido
                        ),
                        TarjetaDatos(
                            titulo = "Inventario",
                            valor = formatearCantidad(dashboard.inventario_total),
                            icono = Icons.Filled.Inventory,
                            contenedor = colores.naranja.contenedor,
                            contenido = colores.naranja.contenido
                        ),
                        TarjetaDatos(
                            titulo = "Vendedores",
                            valor = dashboard.total_vendedores.toString(),
                            icono = Icons.Filled.Person,
                            contenedor = colores.gris.contenedor,
                            contenido = colores.gris.contenido
                        )
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {

                        items(
                            items = tarjetas,
                            key = { it.titulo }
                        ) { tarjeta ->
                            TarjetaKpi(tarjeta)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaKpi(
    tarjeta: TarjetaDatos
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = tarjeta.contenedor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = tarjeta.contenido.copy(
                    alpha = 0.14f
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = tarjeta.icono,
                        contentDescription = tarjeta.titulo,
                        tint = tarjeta.contenido,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = tarjeta.titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = tarjeta.contenido.copy(
                    alpha = 0.85f
                )
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = tarjeta.valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tarjeta.contenido
            )
        }
    }
}

@Composable
private fun coloresKpis(): PaletaKpi =
    if (isSystemInDarkTheme()) {
        PaletaKpi(
            verde = PaletaColor(Color(0xFF1E3A24), Color(0xFF81C784)),
            azul = PaletaColor(Color(0xFF16324F), Color(0xFF64B5F6)),
            rojo = PaletaColor(Color(0xFF3F1D1D), Color(0xFFE57373)),
            morado = PaletaColor(Color(0xFF33234A), Color(0xFFCE93D8)),
            naranja = PaletaColor(Color(0xFF3A2A12), Color(0xFFFFB74D)),
            gris = PaletaColor(Color(0xFF2E2E2E), Color(0xFFBDBDBD))
        )
    } else {
        PaletaKpi(
            verde = PaletaColor(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
            azul = PaletaColor(Color(0xFFE3F2FD), Color(0xFF1565C0)),
            rojo = PaletaColor(Color(0xFFFFEBEE), Color(0xFFC62828)),
            morado = PaletaColor(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
            naranja = PaletaColor(Color(0xFFFFF3E0), Color(0xFFEF6C00)),
            gris = PaletaColor(Color(0xFFF5F5F5), Color(0xFF424242))
        )
    }

private data class PaletaKpi(
    val verde: PaletaColor,
    val azul: PaletaColor,
    val rojo: PaletaColor,
    val morado: PaletaColor,
    val naranja: PaletaColor,
    val gris: PaletaColor
)

private data class PaletaColor(
    val contenedor: Color,
    val contenido: Color
)

private data class TarjetaDatos(
    val titulo: String,
    val valor: String,
    val icono: ImageVector,
    val contenedor: Color,
    val contenido: Color
)
