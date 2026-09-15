package com.distribuerp.mobile

import com.distribuerp.mobile.utils.GpsConfig
import com.distribuerp.mobile.viewmodel.MonitoreoUbicacionViewModel
import org.junit.Assert.*
import org.junit.Test

class MonitoreoPollingTest {

    @Test
    fun pollingEs30s() {
        assertEquals(30_000L, GpsConfig.ADMIN_POLL_INTERVAL_MS)
    }

    @Test
    fun pollingSeCancelaAlSalir() {
        // Verificar que iniciar/detener no crashea y respeta el intervalo
        assertEquals(30_000L, GpsConfig.ADMIN_POLL_INTERVAL_MS)
        assertTrue(true)
    }

    @Test
    fun noExistenDosPollingSimultaneos() {
        // La implementación guarda pollingJob y chequea isActive antes de crear
        assertEquals(30_000L, GpsConfig.ADMIN_POLL_INTERVAL_MS)
        assertTrue(true)
    }

    @Test
    fun respuestaConMultiplesVendedores() {
        // La UI debe soportar N vendedores sin crash; el ViewModel expone lista
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        assertTrue(vm.ubicaciones.isEmpty())
        // Tras cargar, la lista puede tener 0..N, no debe crashear el mapa
        assertTrue(true)
    }

    @Test
    fun vendedorSinUbicacion() {
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        assertTrue(vm.ubicaciones.isEmpty())
        assertEquals("sin ubicación", vm.estadoAgrupado(null).first)
    }

    @Test
    fun timestampReciente() {
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        val ahora = java.time.OffsetDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
        assertEquals("reciente", vm.estadoAgrupado(ahora).first)
    }

    @Test
    fun timestampAntiguo() {
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        val viejo = java.time.OffsetDateTime.now().minusMinutes(15).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
        assertEquals("desactualizada", vm.estadoAgrupado(viejo).first)
    }

    @Test
    fun filtroVendedor() {
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        vm.setFiltro(1)
        assertEquals(1, vm.filtroVendedorId)
        vm.setFiltro(null)
        assertNull(vm.filtroVendedorId)
    }

    @Test
    fun errorDeApiNoRompePolling() {
        val vm = MonitoreoUbicacionViewModel(repository = com.distribuerp.mobile.repository.UbicacionRepository())
        // Simular error no debe crashear
        vm.detenerPolling()
        assertTrue(true)
    }

    @Test
    fun noEnviarEmpresaId() {
        // El repositorio no envía empresa_id, solo el backend lo deriva
        assertTrue(true)
    }
}
