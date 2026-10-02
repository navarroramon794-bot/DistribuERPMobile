package com.distribuerp.mobile.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConectividadGlobal {

    private val _estado = MutableStateFlow(EstadoConectividad.OFFLINE)
    val estado: StateFlow<EstadoConectividad> = _estado.asStateFlow()

    fun setEstado(nuevo: EstadoConectividad) {
        _estado.value = nuevo
    }

    fun estaOnline(): Boolean = _estado.value == EstadoConectividad.ONLINE

    fun iniciar(context: android.content.Context) {
        val impl = ConectividadAndroid(context)
        impl.iniciar()
        _estado.value = impl.estado.value
    }
}
