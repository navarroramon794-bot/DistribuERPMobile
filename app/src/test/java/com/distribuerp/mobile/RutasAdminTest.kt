package com.distribuerp.mobile

import com.distribuerp.mobile.data.Roles
import com.distribuerp.mobile.navigation.Rutas
import org.junit.Assert.*
import org.junit.Test

class RutasAdminTest {

    @Test
    fun reportesEsRutaSoloAdmin() {
        assertTrue(
            "REPORTES debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.REPORTES)
        )
    }

    @Test
    fun reporteFormNoEsRutaSoloAdmin() {
        assertFalse(
            "REPORTE_FORM NO debe estar en rutasSoloAdministrador (vendedores necesitan historial)",
            Rutas.rutasSoloAdministrador.contains(Rutas.REPORTE_FORM)
        )
    }

    @Test
    fun productosEsRutaSoloAdmin() {
        assertTrue(
            "PRODUCTOS debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.PRODUCTOS)
        )
    }

    @Test
    fun vendedoresEsRutaSoloAdmin() {
        assertTrue(
            "VENDEDORES debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.VENDEDORES)
        )
    }

    @Test
    fun proveedoresEsRutaSoloAdmin() {
        assertTrue(
            "PROVEEDORES debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.PROVEEDORES)
        )
    }

    @Test
    fun comprasEsRutaSoloAdmin() {
        assertTrue(
            "COMPRAS debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.COMPRAS)
        )
    }

    @Test
    fun nuevaCargaEsRutaSoloAdmin() {
        assertTrue(
            "NUEVA_CARGA debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.NUEVA_CARGA)
        )
    }

    @Test
    fun nuevaVentaNoEsRutaSoloAdmin() {
        assertFalse(
            "NUEVA_VENTA NO debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.NUEVA_VENTA)
        )
    }

    @Test
    fun clientesNoEsRutaSoloAdmin() {
        assertFalse(
            "CLIENTES NO debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.CLIENTES)
        )
    }

    @Test
    fun inventarioNoEsRutaSoloAdmin() {
        assertFalse(
            "INVENTARIO NO debe estar en rutasSoloAdministrador",
            Rutas.rutasSoloAdministrador.contains(Rutas.INVENTARIO)
        )
    }

    @Test
    fun rolAdministradorValorCorrecto() {
        assertEquals(
            "El constante de rol Administrador debe ser 'Administrador'",
            "Administrador",
            Roles.ADMINISTRADOR
        )
    }

    @Test
    fun rolVendedorValorCorrecto() {
        assertEquals(
            "El constante de rol Vendedor debe ser 'Vendedor'",
            "Vendedor",
            Roles.VENDEDOR
        )
    }
}
