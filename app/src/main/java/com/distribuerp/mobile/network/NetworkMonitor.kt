package com.distribuerp.mobile.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EstadoConectividad { ONLINE, OFFLINE }

interface NetworkMonitor {
    val estado: StateFlow<EstadoConectividad>
    fun estaOnline(): Boolean
}

class NetworkMonitorInmutable(estadoInicial: EstadoConectividad) : NetworkMonitor {
    private val _estado = MutableStateFlow(estadoInicial)
    override val estado: StateFlow<EstadoConectividad> = _estado.asStateFlow()
    override fun estaOnline(): Boolean = _estado.value == EstadoConectividad.ONLINE
    fun setEstado(nuevo: EstadoConectividad) { _estado.value = nuevo }
}
