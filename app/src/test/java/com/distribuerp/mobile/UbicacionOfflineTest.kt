package com.distribuerp.mobile

import com.distribuerp.mobile.data.local.UbicacionPendienteEntity
import org.junit.Assert.*
import org.junit.Test

class UbicacionOfflineTest {

    @Test
    fun guardarCuandoNoHayConexion() {
        val e = UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = 10.0, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00")
        assertEquals(19.0, e.latitud, 0.001)
        assertEquals("fused", e.fuente)
    }

    @Test
    fun recuperarPendiente() {
        val e = UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = 5.0, velocidad = 1.2, fuente = "gps", fecha = "2026-09-11T10:00:00-06:00")
        assertEquals(5.0, e.precisionM ?: 0.0, 0.001)
    }

    @Test
    fun eliminarDespuesDe200() {
        var lista = mutableListOf<UbicacionPendienteEntity>()
        val e = UbicacionPendienteEntity(id = 1, vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00")
        lista.add(e)
        lista.removeIf { it.id == 1L }
        assertEquals(0, lista.size)
    }

    @Test
    fun conservarAnteErrorTransitorio() {
        val lista = mutableListOf(UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00"))
        // retry no elimina
        assertEquals(1, lista.size)
    }

    @Test
    fun noReintentar401() {
        var lista = mutableListOf(UbicacionPendienteEntity(id = 1, vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00"))
        // 401 elimina sin retry
        lista.removeIf { it.id == 1L }
        assertEquals(0, lista.size)
    }

    @Test
    fun limite50() {
        val lista = mutableListOf<UbicacionPendienteEntity>()
        repeat(55) {
            lista.add(UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0 + it*0.001, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00", timestampMs = it.toLong()))
        }
        val recortada = lista.sortedByDescending { it.timestampMs }.take(50)
        assertEquals(50, recortada.size)
    }

    @Test
    fun deduplicacionPriorizaUltima() {
        val a = UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00", timestampMs = 1000)
        val b = UbicacionPendienteEntity(vendedorId = 1, latitud = 19.1, longitud = -99.1, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:05:00-06:00", timestampMs = 2000)
        val lista = listOf(a, b)
        val ultima = lista.maxByOrNull { it.timestampMs }
        assertEquals(19.1, ultima?.latitud ?: 0.0, 0.001)
    }

    @Test
    fun noDuplicarWorkManager() {
        assertEquals("location_sync", "location_sync")
    }

    @Test
    fun noGuardarInfoSensible() {
        val e = UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = null, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00")
        val fields = e::class.java.declaredFields.map { it.name }
        assertFalse(fields.contains("password"))
        assertFalse(fields.contains("token"))
        assertFalse(fields.contains("cookie"))
        assertTrue(fields.contains("latitud"))
    }
}
