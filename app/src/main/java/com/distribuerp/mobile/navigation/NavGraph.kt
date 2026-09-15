package com.distribuerp.mobile.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.distribuerp.mobile.screens.CargaDetalleScreen
import com.distribuerp.mobile.screens.CargasScreen
import com.distribuerp.mobile.screens.ClienteDetalleScreen
import com.distribuerp.mobile.screens.ClienteFormScreen
import com.distribuerp.mobile.screens.ClientesScreen
import com.distribuerp.mobile.screens.CompraDetalleScreen
import com.distribuerp.mobile.screens.ComprasScreen
import com.distribuerp.mobile.screens.AcercaDeScreen
import com.distribuerp.mobile.screens.ActualizacionesScreen
import com.distribuerp.mobile.screens.CobranzaScreen
import com.distribuerp.mobile.screens.ConfiguracionScreen
import com.distribuerp.mobile.screens.DashboardScreen
import com.distribuerp.mobile.screens.DiagnosticoScreen
import com.distribuerp.mobile.screens.ImpresoraScreen
import com.distribuerp.mobile.screens.InventarioScreen
import com.distribuerp.mobile.screens.LoginScreen
import com.distribuerp.mobile.screens.NuevaCargaScreen
import com.distribuerp.mobile.screens.NuevaCompraScreen
import com.distribuerp.mobile.screens.NuevaVentaScreen
import com.distribuerp.mobile.screens.ProductoDetalleScreen
import com.distribuerp.mobile.screens.ProductoFormScreen
import com.distribuerp.mobile.screens.ProductosScreen
import com.distribuerp.mobile.screens.ProveedorDetalleScreen
import com.distribuerp.mobile.screens.ProveedorFormScreen
import com.distribuerp.mobile.screens.ProveedoresScreen
import com.distribuerp.mobile.screens.ReporteFormScreen
import com.distribuerp.mobile.screens.ReportesScreen
import com.distribuerp.mobile.screens.ChangePasswordScreen
import com.distribuerp.mobile.screens.UbicacionScreen
import com.distribuerp.mobile.screens.VendedorDetalleScreen
import com.distribuerp.mobile.screens.VendedorFormScreen
import com.distribuerp.mobile.screens.VendedoresScreen
import com.distribuerp.mobile.screens.LicenciaScreen
import com.distribuerp.mobile.screens.MonitoreoUbicacionScreen
import com.distribuerp.mobile.ui.components.BannerDemo
import com.distribuerp.mobile.ui.components.PantallaCargandoLicencia
import com.distribuerp.mobile.data.Roles
import com.distribuerp.mobile.viewmodel.AuthViewModel
import com.distribuerp.mobile.viewmodel.CargaViewModel
import com.distribuerp.mobile.viewmodel.ClienteViewModel
import com.distribuerp.mobile.viewmodel.CobranzaViewModel
import com.distribuerp.mobile.viewmodel.CompraViewModel
import com.distribuerp.mobile.viewmodel.EstadoLicencia
import com.distribuerp.mobile.viewmodel.InventarioViewModel
import com.distribuerp.mobile.viewmodel.LicenciaViewModel
import com.distribuerp.mobile.viewmodel.MonitoreoUbicacionViewModel
import com.distribuerp.mobile.viewmodel.ProductoViewModel
import com.distribuerp.mobile.viewmodel.ProveedorViewModel
import com.distribuerp.mobile.viewmodel.UbicacionViewModel
import com.distribuerp.mobile.viewmodel.VendedorViewModel
import com.distribuerp.mobile.viewmodel.VentaViewModel
import kotlinx.coroutines.launch

object Rutas {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CLIENTES = "clientes"
    const val CLIENTE_DETALLE = "clientes/{clienteId}"
    const val CLIENTE_FORM = "clientes/formulario?clienteId={clienteId}"
    const val PRODUCTOS = "productos"
    const val PRODUCTO_DETALLE = "productos/{productoId}"
    const val PRODUCTO_FORM = "productos/formulario?productoId={productoId}"
    const val VENDEDORES = "vendedores"
    const val VENDEDOR_DETALLE = "vendedores/{vendedorId}"
    const val VENDEDOR_FORM = "vendedores/formulario?vendedorId={vendedorId}"
    const val INVENTARIO = "inventario"
    const val CARGAS = "cargas"
    const val CARGA_DETALLE = "cargas/{cargaId}"
    const val NUEVA_CARGA = "nueva_carga"
    const val PROVEEDORES = "proveedores"
    const val PROVEEDOR_DETALLE = "proveedores/{proveedorId}"
    const val PROVEEDOR_FORM = "proveedores/formulario?proveedorId={proveedorId}"
    const val COMPRAS = "compras"
    const val COMPRA_DETALLE = "compras/{compraId}"
    const val NUEVA_COMPRA = "nueva_compra"
    const val NUEVA_VENTA = "nueva_venta"
    const val COBRANZA = "cobranza"
    const val REPORTES = "reportes"
    const val REPORTE_FORM = "reportes/formulario?tipo={tipo}"
    const val CONFIGURACION = "configuracion"
    const val LICENCIA = "licencia"
    const val IMPRESORA = "impresora"
    const val DIAGNOSTICO = "diagnostico"
    const val ACERCA_DE = "acerca_de"
    const val ACTUALIZACIONES = "actualizaciones"
    const val UBICACION = "ubicacion"
    const val MONITOREO_UBICACION = "ubicacion_vendedores"
    const val CAMBIAR_PASSWORD = "cambiar_password?obligatorio={obligatorio}"

    val rutasSoloAdministrador = setOf(
        PRODUCTOS,
        PRODUCTO_DETALLE,
        PRODUCTO_FORM,
        VENDEDORES,
        VENDEDOR_DETALLE,
        VENDEDOR_FORM,
        PROVEEDORES,
        PROVEEDOR_DETALLE,
        PROVEEDOR_FORM,
        COMPRAS,
        COMPRA_DETALLE,
        NUEVA_COMPRA,
        NUEVA_CARGA,
        REPORTES,
        MONITOREO_UBICACION
    )

    fun clienteDetalle(clienteId: Int): String =
        "clientes/$clienteId"

    fun clienteForm(clienteId: Int? = null): String =
        if (clienteId != null) {
            "clientes/formulario?clienteId=$clienteId"
        } else {
            "clientes/formulario"
        }

    fun productoDetalle(productoId: Int): String =
        "productos/$productoId"

    fun productoForm(productoId: Int? = null): String =
        if (productoId != null) {
            "productos/formulario?productoId=$productoId"
        } else {
            "productos/formulario"
        }

    fun vendedorDetalle(vendedorId: Int): String =
        "vendedores/$vendedorId"

    fun vendedorForm(vendedorId: Int? = null): String =
        if (vendedorId != null) {
            "vendedores/formulario?vendedorId=$vendedorId"
        } else {
            "vendedores/formulario"
        }

    fun reporteForm(tipo: String): String =
        "reportes/formulario?tipo=$tipo"

    fun cargaDetalle(cargaId: Int): String =
        "cargas/$cargaId"

    fun proveedorDetalle(proveedorId: Int): String =
        "proveedores/$proveedorId"

    fun proveedorForm(proveedorId: Int? = null): String =
        if (proveedorId != null) {
            "proveedores/formulario?proveedorId=$proveedorId"
        } else {
            "proveedores/formulario"
        }

    fun compraDetalle(compraId: Int): String =
        "compras/$compraId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val clienteViewModel: ClienteViewModel =
        viewModel(factory = ClienteViewModel.Factory)
    val productoViewModel: ProductoViewModel =
        viewModel(factory = ProductoViewModel.Factory)
    val vendedorViewModel: VendedorViewModel =
        viewModel(factory = VendedorViewModel.Factory)
    val inventarioViewModel: InventarioViewModel =
        viewModel(factory = InventarioViewModel.Factory)
    val ventaViewModel: VentaViewModel =
        viewModel(factory = VentaViewModel.Factory)
    val cobranzaViewModel: CobranzaViewModel =
        viewModel(factory = CobranzaViewModel.Factory)
    val cargaViewModel: CargaViewModel =
        viewModel(factory = CargaViewModel.Factory)
    val proveedorViewModel: ProveedorViewModel =
        viewModel(factory = ProveedorViewModel.Factory)
    val compraViewModel: CompraViewModel =
        viewModel(factory = CompraViewModel.Factory)
    val licenciaViewModel: LicenciaViewModel =
        viewModel(factory = LicenciaViewModel.Factory)
    val contexto = LocalContext.current
    val ubicacionViewModel: UbicacionViewModel =
        viewModel(factory = UbicacionViewModel.factory(contexto))
    val monitoreoUbicacionViewModel: MonitoreoUbicacionViewModel =
        viewModel(factory = MonitoreoUbicacionViewModel.Factory)
    val sesion by viewModel.sesion.collectAsState()
    val empresa = viewModel.empresa

    val estadoLicencia = licenciaViewModel.estado
    val licenciaInfo = licenciaViewModel.licencia
    val diasRestantesLicencia = licenciaViewModel.diasRestantes
    val mensajeLicencia = licenciaViewModel.mensaje

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val accesoPermitido =
        estadoLicencia == EstadoLicencia.Activa ||
            estadoLicencia == EstadoLicencia.Demo

    LaunchedEffect(sesion, rutaActual, accesoPermitido) {
        if (!accesoPermitido) {
            return@LaunchedEffect
        }

        val esTemporal = sesion?.password_temporal == true

        // Forzar cambio si es temporal y no estamos ya en esa pantalla
        if (esTemporal && rutaActual != null && !rutaActual.startsWith("cambiar_password")) {
            navController.navigate("cambiar_password?obligatorio=true") {
                popUpTo(Rutas.LOGIN) { inclusive = false }
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        if (!esTemporal && rutaActual?.startsWith("cambiar_password") == true) {
            navController.navigate(Rutas.DASHBOARD) {
                popUpTo("cambiar_password?obligatorio={obligatorio}") { inclusive = true }
            }
            return@LaunchedEffect
        }

        Log.d(
            "NAV",
            "sesion=${sesion != null} ruta=$rutaActual"
        )

        when {
            sesion != null && rutaActual == Rutas.LOGIN -> {
                if (esTemporal) {
                    navController.navigate("cambiar_password?obligatorio=true") {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                } else {
                    navController.navigate(Rutas.DASHBOARD) {
                        popUpTo(Rutas.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            }

            sesion == null && rutaActual != Rutas.LOGIN -> {
                navController.navigate(Rutas.LOGIN) {
                    popUpTo(0) {
                        inclusive = true
                    }
                }
            }

            sesion?.rol != Roles.ADMINISTRADOR &&
                rutaActual in Rutas.rutasSoloAdministrador -> {

                navController.navigate(Rutas.DASHBOARD) {
                    popUpTo(Rutas.DASHBOARD) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    val esPantallaConDrawer =
        sesion != null &&
                rutaActual != null &&
                rutaActual != Rutas.LOGIN

    val contenidoNav: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            NavHost(
                navController = navController,
                startDestination =
                    if (sesion != null) Rutas.DASHBOARD else Rutas.LOGIN
            ) {

                composable(Rutas.LOGIN) {
                    LoginScreen(viewModel = viewModel)
                }

                composable(
                    route = Rutas.CAMBIAR_PASSWORD,
                    arguments = listOf(
                        navArgument("obligatorio") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entrada ->
                    val obligatorio = entrada.arguments?.getBoolean("obligatorio") ?: false
                    ChangePasswordScreen(
                        obligatorio = obligatorio || (sesion?.password_temporal == true),
                        onSuccess = {
                            navController.navigate(Rutas.DASHBOARD) {
                                popUpTo(Rutas.CAMBIAR_PASSWORD) { inclusive = true }
                                popUpTo(Rutas.LOGIN) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Rutas.DASHBOARD) {
                    DashboardScreen(
                        authViewModel = viewModel,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNavegarClientes = {
                            navController.navigate(Rutas.CLIENTES)
                        }
                    )
                }

                composable(Rutas.CLIENTES) {
                    ClientesScreen(
                        viewModel = clienteViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevoCliente = {
                            navController.navigate(Rutas.clienteForm())
                        },
                        onVerDetalle = { clienteId ->
                            navController.navigate(
                                Rutas.clienteDetalle(clienteId)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.CLIENTE_DETALLE,
                    arguments = listOf(
                        navArgument("clienteId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val clienteId =
                        entrada.arguments?.getInt("clienteId") ?: 0

                    ClienteDetalleScreen(
                        clienteId = clienteId,
                        viewModel = clienteViewModel,
                        esAdministrador =
                            sesion?.rol == Roles.ADMINISTRADOR,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onEditar = { id ->
                            navController.navigate(
                                Rutas.clienteForm(id)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.CLIENTE_FORM,
                    arguments = listOf(
                        navArgument("clienteId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entrada ->

                    val clienteId =
                        entrada.arguments?.getInt("clienteId") ?: -1

                    ClienteFormScreen(
                        clienteId = if (clienteId > 0) {
                            clienteId
                        } else {
                            null
                        },
                        viewModel = clienteViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onGuardado = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.PRODUCTOS) {
                    ProductosScreen(
                        viewModel = productoViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevoProducto = {
                            navController.navigate(Rutas.productoForm())
                        },
                        onVerDetalle = { productoId ->
                            navController.navigate(
                                Rutas.productoDetalle(productoId)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.PRODUCTO_DETALLE,
                    arguments = listOf(
                        navArgument("productoId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val productoId =
                        entrada.arguments?.getInt("productoId") ?: 0

                    ProductoDetalleScreen(
                        productoId = productoId,
                        viewModel = productoViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onEditar = { id ->
                            navController.navigate(
                                Rutas.productoForm(id)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.PRODUCTO_FORM,
                    arguments = listOf(
                        navArgument("productoId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entrada ->

                    val productoId =
                        entrada.arguments?.getInt("productoId") ?: -1

                    ProductoFormScreen(
                        productoId = if (productoId > 0) {
                            productoId
                        } else {
                            null
                        },
                        viewModel = productoViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onGuardado = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.VENDEDORES) {
                    VendedoresScreen(
                        viewModel = vendedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevoVendedor = {
                            navController.navigate(Rutas.vendedorForm())
                        },
                        onVerDetalle = { vendedorId ->
                            navController.navigate(
                                Rutas.vendedorDetalle(vendedorId)
                            )
                        }
                    )
                }

                composable(Rutas.INVENTARIO) {
                    InventarioScreen(
                        viewModel = inventarioViewModel,
                        vendedorIdPropio =
                            if (sesion?.rol == Roles.VENDEDOR) {
                                sesion?.vendedor_id?.toIntOrNull()
                            } else {
                                null
                            },
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }

                composable(Rutas.CARGAS) {
                    CargasScreen(
                        viewModel = cargaViewModel,
                        esAdministrador =
                            sesion?.rol == Roles.ADMINISTRADOR,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevaCarga = {
                            navController.navigate(Rutas.NUEVA_CARGA)
                        },
                        onVerDetalle = { cargaId ->
                            navController.navigate(
                                Rutas.cargaDetalle(cargaId)
                            )
                        }
                    )
                }

                composable(Rutas.NUEVA_CARGA) {
                    NuevaCargaScreen(
                        viewModel = cargaViewModel,
                        empresa = empresa,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Rutas.CARGA_DETALLE,
                    arguments = listOf(
                        navArgument("cargaId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val cargaId =
                        entrada.arguments?.getInt("cargaId") ?: 0

                    CargaDetalleScreen(
                        cargaId = cargaId,
                        viewModel = cargaViewModel,
                        empresa = empresa,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.PROVEEDORES) {
                    ProveedoresScreen(
                        viewModel = proveedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevoProveedor = {
                            navController.navigate(Rutas.proveedorForm())
                        },
                        onVerDetalle = { proveedorId ->
                            navController.navigate(
                                Rutas.proveedorDetalle(proveedorId)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.PROVEEDOR_DETALLE,
                    arguments = listOf(
                        navArgument("proveedorId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val proveedorId =
                        entrada.arguments?.getInt("proveedorId") ?: 0

                    ProveedorDetalleScreen(
                        proveedorId = proveedorId,
                        viewModel = proveedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onEditar = { id ->
                            navController.navigate(
                                Rutas.proveedorForm(id)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.PROVEEDOR_FORM,
                    arguments = listOf(
                        navArgument("proveedorId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entrada ->

                    val proveedorId =
                        entrada.arguments?.getInt("proveedorId") ?: -1

                    ProveedorFormScreen(
                        proveedorId = if (proveedorId > 0) {
                            proveedorId
                        } else {
                            null
                        },
                        viewModel = proveedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onGuardado = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.COMPRAS) {
                    ComprasScreen(
                        viewModel = compraViewModel,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNuevaCompra = {
                            navController.navigate(Rutas.NUEVA_COMPRA)
                        },
                        onVerDetalle = { compraId ->
                            navController.navigate(
                                Rutas.compraDetalle(compraId)
                            )
                        }
                    )
                }

                composable(Rutas.NUEVA_COMPRA) {
                    NuevaCompraScreen(
                        viewModel = compraViewModel,
                        empresa = empresa,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Rutas.COMPRA_DETALLE,
                    arguments = listOf(
                        navArgument("compraId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val compraId =
                        entrada.arguments?.getInt("compraId") ?: 0

                    CompraDetalleScreen(
                        compraId = compraId,
                        viewModel = compraViewModel,
                        empresa = empresa,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.NUEVA_VENTA) {
                    NuevaVentaScreen(
                        viewModel = ventaViewModel,
                        empresa = empresa,
                        esAdministrador =
                            sesion?.rol == Roles.ADMINISTRADOR,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onFinalizar = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.COBRANZA) {
                    CobranzaScreen(
                        viewModel = cobranzaViewModel,
                        empresa = empresa,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }

                composable(Rutas.REPORTES) {
                    ReportesScreen(
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onAbrirReporte = { tipo ->
                            navController.navigate(
                                Rutas.reporteForm(tipo)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.REPORTE_FORM,
                    arguments = listOf(
                        navArgument("tipo") {
                            type = NavType.StringType
                        }
                    )
                ) { entrada ->

                    val tipo =
                        entrada.arguments?.getString("tipo")
                            ?: "ventas"

                    ReporteFormScreen(
                        tipoClave = tipo,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.CONFIGURACION) {
                    ConfiguracionScreen(
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onAbrirImpresora = {
                            navController.navigate(Rutas.IMPRESORA)
                        },
                        onAbrirDiagnostico = {
                            navController.navigate(Rutas.DIAGNOSTICO)
                        },
                        onAbrirAcercaDe = {
                            navController.navigate(Rutas.ACERCA_DE)
                        },
                        onAbrirActualizaciones = {
                            navController.navigate(
                                Rutas.ACTUALIZACIONES
                            )
                        },
                        onAbrirLicencia = {
                            navController.navigate(Rutas.LICENCIA)
                        }
                    )
                }

                composable(Rutas.LICENCIA) {
                    LicenciaScreen(
                        licencia = licenciaInfo,
                        diasRestantes = diasRestantesLicencia,
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.IMPRESORA) {
                    ImpresoraScreen(
                        empresa = empresa,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }

                composable(Rutas.DIAGNOSTICO) {
                    DiagnosticoScreen(
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.ACERCA_DE) {
                    AcercaDeScreen(
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.ACTUALIZACIONES) {
                    ActualizacionesScreen(
                        onVolver = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Rutas.VENDEDOR_DETALLE,
                    arguments = listOf(
                        navArgument("vendedorId") {
                            type = NavType.IntType
                        }
                    )
                ) { entrada ->

                    val vendedorId =
                        entrada.arguments?.getInt("vendedorId") ?: 0

                    VendedorDetalleScreen(
                        vendedorId = vendedorId,
                        viewModel = vendedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onEditar = { id ->
                            navController.navigate(
                                Rutas.vendedorForm(id)
                            )
                        }
                    )
                }

                composable(
                    route = Rutas.VENDEDOR_FORM,
                    arguments = listOf(
                        navArgument("vendedorId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entrada ->

                    val vendedorId =
                        entrada.arguments?.getInt("vendedorId") ?: -1

                    VendedorFormScreen(
                        vendedorId = if (vendedorId > 0) {
                            vendedorId
                        } else {
                            null
                        },
                        viewModel = vendedorViewModel,
                        onVolver = {
                            navController.popBackStack()
                        },
                        onGuardado = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Rutas.UBICACION) {
                    UbicacionScreen(
                        viewModel = ubicacionViewModel,
                        vendedorId = if (
                            sesion?.rol == Roles.VENDEDOR
                        ) {
                            sesion?.vendedor_id?.toIntOrNull()
                        } else {
                            null
                        },
                        esVendedor = sesion?.rol == Roles.VENDEDOR,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }

                composable(Rutas.MONITOREO_UBICACION) {
                    MonitoreoUbicacionScreen(
                        viewModel = monitoreoUbicacionViewModel,
                        onAbrirMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }

    if (!accesoPermitido) {

        when (estadoLicencia) {

            EstadoLicencia.Cargando ->
                PantallaCargandoLicencia()

            else ->
                PuertaLicencia(
                    viewModel = licenciaViewModel,
                    estado = estadoLicencia,
                    mensaje = mensajeLicencia
                )
        }

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            if (estadoLicencia == EstadoLicencia.Demo) {

                BannerDemo(
                    diasRestantes = diasRestantesLicencia
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (estadoLicencia == EstadoLicencia.Demo) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.statusBars
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {

    if (esPantallaConDrawer) {

        // En Monitoreo GPS el gesto horizontal debe ser capturado por el mapa,
        // no por el Drawer. Se deshabilita SOLO la apertura del Drawer por gesto
        // en esa pantalla; el botón hamburguesa (☰) sigue abriendo el menú.
        val esMonitoreoGPS =
            rutaActual == Rutas.MONITOREO_UBICACION

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !esMonitoreoGPS,
            drawerContent = {

                DrawerContenido(
                    nombreUsuario = sesion?.nombre ?: "",
                    correoUsuario = sesion?.correo ?: "",
                    rol = sesion?.rol,
                    rutaActual = rutaActual,
                    onSeleccionar = { item ->

                        scope.launch {
                            drawerState.close()
                        }

                        val ruta = item.ruta

                        if (ruta != rutaActual) {

                            navController.navigate(ruta) {
                                popUpTo(Rutas.DASHBOARD)
                                launchSingleTop = true
                            }
                        }
                    },
                    onCerrarSesion = {

                        scope.launch {
                            drawerState.close()
                        }

                        viewModel.cerrarSesion()
                    }
                )
            }
        ) {

            contenidoNav()
        }

    } else {

            contenidoNav()
                }
            }
        }
    }
}
