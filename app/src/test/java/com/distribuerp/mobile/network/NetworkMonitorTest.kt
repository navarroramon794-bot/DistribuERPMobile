package com.distribuerp.mobile.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun `estado inicial offline`() = runTest {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.OFFLINE)

        assertFalse(monitor.estaOnline())
        assertEquals(EstadoConectividad.OFFLINE, monitor.estado.first())
    }

    @Test
    fun `estado inicial online`() = runTest {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.ONLINE)

        assertTrue(monitor.estaOnline())
    }

    @Test
    fun `transicion offline a online`() = runTest {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.OFFLINE)

        monitor.setEstado(EstadoConectividad.ONLINE)

        assertTrue(monitor.estaOnline())
        assertEquals(EstadoConectividad.ONLINE, monitor.estado.first())
    }

    @Test
    fun `transicion online a offline`() = runTest {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.ONLINE)

        monitor.setEstado(EstadoConectividad.OFFLINE)

        assertFalse(monitor.estaOnline())
    }

    @Test
    fun `setEstado repetido con el mismo valor es estable`() = runTest {
        val monitor = NetworkMonitorInmutable(EstadoConectividad.ONLINE)

        monitor.setEstado(EstadoConectividad.ONLINE)
        monitor.setEstado(EstadoConectividad.ONLINE)

        assertTrue(monitor.estaOnline())
        assertEquals(EstadoConectividad.ONLINE, monitor.estado.first())
    }

    @Test
    fun `el estado global refleja el valor asignado`() = runTest {
        ConectividadGlobal.setEstado(EstadoConectividad.ONLINE)
        assertTrue(ConectividadGlobal.estaOnline())
        assertEquals(EstadoConectividad.ONLINE, ConectividadGlobal.estado.first())

        ConectividadGlobal.setEstado(EstadoConectividad.OFFLINE)
        assertFalse(ConectividadGlobal.estaOnline())
        assertEquals(EstadoConectividad.OFFLINE, ConectividadGlobal.estado.first())
    }

    @Test
    fun `el monitor es independiente del estado global`() = runTest {
        ConectividadGlobal.setEstado(EstadoConectividad.ONLINE)
        val monitor = NetworkMonitorInmutable(EstadoConectividad.OFFLINE)

        assertTrue(ConectividadGlobal.estaOnline())
        assertFalse(monitor.estaOnline())
    }
}