package com.distribuerp.mobile.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConectividadAndroid(context: Context) : NetworkMonitor {

    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _estado = MutableStateFlow(consultarEstado())
    override val estado = _estado.asStateFlow()

    override fun estaOnline(): Boolean = _estado.value == EstadoConectividad.ONLINE

    fun iniciar() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { refrescar() }
            override fun onLost(network: Network) { refrescar() }
            override fun onUnavailable() { refrescar() }
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) { refrescar() }
        }
        cm.registerDefaultNetworkCallback(callback)
    }

    fun refrescar() {
        _estado.value = consultarEstado()
    }

    private fun consultarEstado(): EstadoConectividad {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return EstadoConectividad.OFFLINE
        val tieneInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validada = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (tieneInternet && validada) EstadoConectividad.ONLINE
        else EstadoConectividad.OFFLINE
    }
}
