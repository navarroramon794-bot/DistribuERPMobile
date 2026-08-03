package com.distribuerp.mobile.printing

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.models.Pago
import com.distribuerp.mobile.models.Venta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStoreImpresora by preferencesDataStore(
    name = "impresora"
)

data class ConfiguracionImpresora(
    val nombre: String,
    val mac: String,
    val ultimaConexionMs: Long? = null
)

data class ResultadoImpresion(
    val ok: Boolean,
    val mensaje: String
)

class PrinterRepository(private val context: Context) {

    private object Keys {
        val NOMBRE = stringPreferencesKey("nombre")
        val MAC = stringPreferencesKey("mac")
        val ULTIMA_CONEXION = longPreferencesKey("ultima_conexion")
    }

    val configuracion: Flow<ConfiguracionImpresora?> =
        context.dataStoreImpresora.data.map { prefs ->
            val nombre = prefs[Keys.NOMBRE]
            val mac = prefs[Keys.MAC]

            if (nombre.isNullOrBlank() || mac.isNullOrBlank()) {
                null
            } else {
                ConfiguracionImpresora(
                    nombre = nombre,
                    mac = mac,
                    ultimaConexionMs = prefs[Keys.ULTIMA_CONEXION]
                )
            }
        }

    suspend fun conectarImpresora(nombre: String, mac: String): ResultadoImpresion {
        val adaptador = BluetoothAdapter.getDefaultAdapter()

        if (adaptador == null || !adaptador.isEnabled) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "El Bluetooth está apagado o no está disponible"
            )
        }

        val dispositivo = try {
            adaptador.getRemoteDevice(mac)
        } catch (_: Exception) {
            null
        }

        if (dispositivo == null) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "No se encontró el dispositivo $nombre"
            )
        }

        val conectado = BluetoothPrinterManager.conectar(dispositivo)

        if (!conectado) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "No se pudo conectar con $nombre. Verifica que esté encendida y cerca."
            )
        }

        guardarImpresora(nombre, mac)
        marcarConexionExitosa()

        return ResultadoImpresion(
            ok = true,
            mensaje = "Conectado a $nombre"
        )
    }

    suspend fun imprimirTicketVenta(venta: Venta): ResultadoImpresion {
        val preparado = prepararConexion()
        if (!preparado.ok) {
            return preparado
        }

        val enviado = BluetoothPrinterManager.escribir(
            EscPosBuilder.ticketVenta(venta)
        )

        return resultadoFinal(
            enviado = enviado,
            mensajeExito = "Ticket de venta enviado a la impresora"
        )
    }

    suspend fun imprimirReciboCobranza(
        pago: Pago,
        venta: Venta?,
        saldoRestante: Double
    ): ResultadoImpresion {
        val preparado = prepararConexion()
        if (!preparado.ok) {
            return preparado
        }

        val enviado = BluetoothPrinterManager.escribir(
            EscPosBuilder.ticketCobranza(
                pago = pago,
                venta = venta,
                saldoRestante = saldoRestante
            )
        )

        return resultadoFinal(
            enviado = enviado,
            mensajeExito = "Recibo enviado a la impresora"
        )
    }

    suspend fun imprimirComprobanteCarga(carga: Carga): ResultadoImpresion {
        val preparado = prepararConexion()
        if (!preparado.ok) {
            return preparado
        }

        val enviado = BluetoothPrinterManager.escribir(
            EscPosBuilder.ticketCarga(carga)
        )

        return resultadoFinal(
            enviado = enviado,
            mensajeExito = "Comprobante de carga enviado a la impresora"
        )
    }

    suspend fun imprimirComprobanteCompra(compra: Compra): ResultadoImpresion {
        val preparado = prepararConexion()
        if (!preparado.ok) {
            return preparado
        }

        val enviado = BluetoothPrinterManager.escribir(
            EscPosBuilder.ticketCompra(compra)
        )

        return resultadoFinal(
            enviado = enviado,
            mensajeExito = "Comprobante de compra enviado a la impresora"
        )
    }

    suspend fun imprimirPrueba(): ResultadoImpresion {
        val preparado = prepararConexion()
        if (!preparado.ok) {
            return preparado
        }

        val nombre = configuracion.first()?.nombre ?: "—"

        val enviado = BluetoothPrinterManager.escribir(
            EscPosBuilder.ticketPrueba(nombre)
        )

        return resultadoFinal(
            enviado = enviado,
            mensajeExito = "Prueba de impresión enviada"
        )
    }

    private suspend fun prepararConexion(): ResultadoImpresion {
        val configuracion = configuracion.first()

        if (configuracion == null) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "No hay impresora configurada. Entra a Configuración > Impresora."
            )
        }

        val actual = BluetoothPrinterManager.dispositivoConectado

        if (BluetoothPrinterManager.conectado && actual?.address == configuracion.mac) {
            return ResultadoImpresion(ok = true, mensaje = "")
        }

        val adaptador = BluetoothAdapter.getDefaultAdapter()

        if (adaptador == null || !adaptador.isEnabled) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "El Bluetooth está apagado o no está disponible"
            )
        }

        val dispositivo = try {
            adaptador.getRemoteDevice(configuracion.mac)
        } catch (_: Exception) {
            null
        }

        if (dispositivo == null) {
            return ResultadoImpresion(
                ok = false,
                mensaje = "No se encontró la impresora ${configuracion.nombre}"
            )
        }

        val conectado = BluetoothPrinterManager.conectar(dispositivo)

        return if (conectado) {
            ResultadoImpresion(ok = true, mensaje = "")
        } else {
            ResultadoImpresion(
                ok = false,
                mensaje = "No se pudo conectar con la impresora ${configuracion.nombre}. Verifica que esté encendida y cerca."
            )
        }
    }

    private suspend fun resultadoFinal(
        enviado: Boolean,
        mensajeExito: String
    ): ResultadoImpresion {
        if (enviado) {
            marcarConexionExitosa()
            return ResultadoImpresion(ok = true, mensaje = mensajeExito)
        }

        return ResultadoImpresion(
            ok = false,
            mensaje = "No se pudo enviar la impresión. Verifica que la impresora esté encendida y cerca."
        )
    }

    private suspend fun guardarImpresora(nombre: String, mac: String) {
        context.dataStoreImpresora.edit { prefs ->
            prefs[Keys.NOMBRE] = nombre
            prefs[Keys.MAC] = mac
        }
    }

    private suspend fun marcarConexionExitosa() {
        context.dataStoreImpresora.edit { prefs ->
            prefs[Keys.ULTIMA_CONEXION] = System.currentTimeMillis()
        }
    }
}
