package com.distribuerp.mobile.printing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object BluetoothPrinterManager {

    private val UUID_SPP: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var dispositivoConectado: BluetoothDevice? by mutableStateOf(null)
        private set

    var conectado: Boolean by mutableStateOf(false)
        private set

    var ultimaConexionMs: Long? by mutableStateOf(null)
        private set

    @Volatile
    private var socket: BluetoothSocket? = null

    fun tieneBluetooth(): Boolean =
        BluetoothAdapter.getDefaultAdapter() != null

    fun bluetoothActivado(): Boolean =
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    fun dispositivosEmparejados(): List<BluetoothDevice> {
        val adaptador = BluetoothAdapter.getDefaultAdapter()
            ?: return emptyList()

        if (!adaptador.isEnabled) {
            return emptyList()
        }

        return adaptador.bondedDevices
            ?.filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    suspend fun conectar(dispositivo: BluetoothDevice): Boolean =
        withContext(Dispatchers.IO) {
            desconectar()

            var exito = false

            crearSockets(dispositivo).forEach { candidato ->
                if (exito) {
                    return@forEach
                }

                try {
                    candidato.connect()
                    socket = candidato
                    dispositivoConectado = dispositivo
                    conectado = true
                    ultimaConexionMs = System.currentTimeMillis()
                    exito = true
                } catch (_: Exception) {
                    try {
                        candidato.close()
                    } catch (_: Exception) {
                    }
                }
            }

            if (!exito) {
                dispositivoConectado = null
                conectado = false
            }

            exito
        }

    fun desconectar() {
        val actual = socket
        socket = null
        try {
            actual?.close()
        } catch (_: Exception) {
        }
        conectado = false
        dispositivoConectado = null
    }

    suspend fun escribir(bytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            val actual = socket

            if (actual == null || !actual.isConnected) {
                return@withContext false
            }

            try {
                actual.outputStream.write(bytes)
                actual.outputStream.flush()
                true
            } catch (_: Exception) {
                desconectar()
                false
            }
        }

    private fun crearSockets(dispositivo: BluetoothDevice): List<BluetoothSocket> {
        val candidatos = mutableListOf<BluetoothSocket>()

        try {
            candidatos.add(
                dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
            )
        } catch (_: Exception) {
        }

        try {
            candidatos.add(
                dispositivo.createInsecureRfcommSocketToServiceRecord(UUID_SPP)
            )
        } catch (_: Exception) {
        }

        try {
            candidatos.add(crearSocketPorReflexion(dispositivo))
        } catch (_: Exception) {
        }

        return candidatos
    }

    @Suppress("DEPRECATION")
    private fun crearSocketPorReflexion(dispositivo: BluetoothDevice): BluetoothSocket {
        val metodo = dispositivo.javaClass.getMethod(
            "createRfcommSocket",
            Int::class.javaPrimitiveType
        )
        return metodo.invoke(dispositivo, 1) as BluetoothSocket
    }
}
