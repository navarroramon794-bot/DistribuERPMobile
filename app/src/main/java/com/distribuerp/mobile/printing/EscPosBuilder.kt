package com.distribuerp.mobile.printing

import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.models.Pago
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.ui.components.formatearCantidad
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Alineacion {
    IZQUIERDA,
    CENTRO,
    DERECHA
}

object EscPosBuilder {

    private const val ANCHO = 32

    private fun comando(vararg bytes: Int): ByteArray =
        ByteArray(bytes.size) { bytes[it].toByte() }

    fun init(): ByteArray =
        comando(0x1B, 0x40)

    fun configurarCodepage(): ByteArray =
        comando(0x1B, 0x74, 0x02)

    fun lineaEnBlanco(cantidad: Int = 1): ByteArray =
        ByteArray(cantidad) { 0x0A }

    fun cortar(): ByteArray =
        comando(0x1D, 0x56, 0x00)

    fun alinear(alineacion: Alineacion): ByteArray =
        when (alineacion) {
            Alineacion.IZQUIERDA -> comando(0x1B, 0x61, 0x00)
            Alineacion.CENTRO -> comando(0x1B, 0x61, 0x01)
            Alineacion.DERECHA -> comando(0x1B, 0x61, 0x02)
        }

    fun negritas(activo: Boolean): ByteArray =
        comando(0x1B, 0x45, if (activo) 1 else 0)

    private fun modoTexto(dobleAncho: Boolean, dobleAlto: Boolean): ByteArray {
        var modo = 0
        if (dobleAlto) modo = modo or 0x10
        if (dobleAncho) modo = modo or 0x20
        return comando(0x1B, 0x21, modo)
    }

    fun texto(
        linea: String,
        alineacion: Alineacion = Alineacion.IZQUIERDA,
        negritas: Boolean = false,
        dobleAncho: Boolean = false,
        dobleAlto: Boolean = false
    ): ByteArray {
        val salida = ByteArrayOutputStream()
        salida.write(alinear(alineacion))
        salida.write(modoTexto(dobleAncho, dobleAlto))
        salida.write(negritas(negritas))
        salida.write(codificar(linea))
        salida.write(0x0A)
        salida.write(modoTexto(false, false))
        salida.write(negritas(false))
        return salida.toByteArray()
    }

    fun separador(): ByteArray =
        texto("-".repeat(ANCHO))

    fun columna(izquierda: String, derecha: String): String {
        val der = recortar(derecha, ANCHO)
        val izq = recortar(izquierda, ANCHO - der.length)
        val relleno = (ANCHO - izq.length - der.length).coerceAtLeast(0)
        return izq + " ".repeat(relleno) + der
    }

    fun item(nombre: String, cantidad: Double, precio: Double, subtotal: Double): ByteArray {
        val salida = ByteArrayOutputStream()
        val cant = formatearCantidad(cantidad)
        val sub = formatearDinero(subtotal)
        val prefijo = "$cant x "
        val espacioNombre = ANCHO - prefijo.length - sub.length
        val nombreCortado = recortar(nombre, espacioNombre)
        salida.write(texto(columna(prefijo + nombreCortado, sub)))
        salida.write(texto("    " + formatearDinero(precio) + " c/u"))
        return salida.toByteArray()
    }

    private fun recortar(texto: String, maximo: Int): String {
        if (maximo <= 0) {
            return ""
        }

        val limpio = texto.replace("\n", " ").trim()

        return if (limpio.length > maximo) {
            limpio.take(maximo - 1) + "."
        } else {
            limpio
        }
    }

    private fun codificar(texto: String): ByteArray {
        val charset = try {
            Charset.forName("IBM850")
        } catch (_: Exception) {
            Charset.forName("US-ASCII")
        }

        return texto.toByteArray(charset)
    }

    fun ticketVenta(venta: Venta): ByteArray {
        val s = ByteArrayOutputStream()

        s.write(init())
        s.write(configurarCodepage())
        s.write(lineaEnBlanco())
        s.write(separador())
        s.write(
            texto(
                linea = "DistribuERP",
                alineacion = Alineacion.CENTRO,
                negritas = true,
                dobleAncho = true,
                dobleAlto = true
            )
        )
        s.write(texto("www.distribuerp.com", Alineacion.CENTRO))
        s.write(lineaEnBlanco())
        s.write(texto("VENTA", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco())
        s.write(texto(columna("Folio", venta.folio)))
        s.write(texto(columna("Fecha", venta.fecha ?: "—")))
        s.write(texto(columna("Cliente", venta.cliente)))
        s.write(texto(columna("Vendedor", venta.vendedor)))
        s.write(separador())
        s.write(texto(columna("CANT PRODUCTO", "SUBTOTAL"), negritas = true))
        venta.items.forEach { itemVenta ->
            s.write(
                item(
                    nombre = itemVenta.producto,
                    cantidad = itemVenta.cantidad,
                    precio = itemVenta.precio,
                    subtotal = itemVenta.subtotal
                )
            )
        }
        s.write(separador())
        s.write(texto(columna("TOTAL", formatearDinero(venta.total)), negritas = true))
        if (venta.pagado > 0) {
            s.write(texto(columna("Pagado", formatearDinero(venta.pagado))))
            s.write(texto(columna("Saldo", formatearDinero(venta.saldo))))
        }
        s.write(separador())
        s.write(lineaEnBlanco())
        s.write(texto("Gracias por su compra", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco(2))
        s.write(cortar())

        return s.toByteArray()
    }

    fun ticketCobranza(pago: Pago, venta: Venta?, saldoRestante: Double): ByteArray {

        val s = ByteArrayOutputStream()

        s.write(init())
        s.write(configurarCodepage())
        s.write(lineaEnBlanco())
        s.write(separador())
        s.write(
            texto(
                linea = "DistribuERP",
                alineacion = Alineacion.CENTRO,
                negritas = true,
                dobleAncho = true,
                dobleAlto = true
            )
        )
        s.write(texto("www.distribuerp.com", Alineacion.CENTRO))
        s.write(lineaEnBlanco())
        s.write(texto("RECIBO DE PAGO", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco())
        s.write(texto(columna("Recibo", pago.id.toString())))
        venta?.let {
            s.write(texto(columna("Venta", it.folio)))
            s.write(texto(columna("Cliente", it.cliente)))
        }
        s.write(texto(columna("Fecha", pago.fecha ?: "—")))
        s.write(separador())
        s.write(texto(columna("Monto pagado", formatearDinero(pago.monto)), negritas = true))
        s.write(texto(columna("Saldo restante", formatearDinero(saldoRestante))))
        s.write(separador())
        s.write(lineaEnBlanco())
        s.write(texto("Gracias por su pago", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco(2))
        s.write(cortar())

        return s.toByteArray()
    }

    fun ticketCarga(carga: Carga): ByteArray {
        val s = ByteArrayOutputStream()

        s.write(init())
        s.write(configurarCodepage())
        s.write(lineaEnBlanco())
        s.write(separador())
        s.write(
            texto(
                linea = "DistribuERP",
                alineacion = Alineacion.CENTRO,
                negritas = true,
                dobleAncho = true,
                dobleAlto = true
            )
        )
        s.write(texto("www.distribuerp.com", Alineacion.CENTRO))
        s.write(lineaEnBlanco())
        s.write(texto("COMPROBANTE DE CARGA", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco())
        s.write(texto(columna("Folio", carga.folio)))
        s.write(texto(columna("Fecha", carga.fecha ?: "—")))
        s.write(texto(columna("Vendedor", carga.vendedor ?: "—")))
        s.write(separador())
        s.write(texto(columna("CANT PRODUCTO", "CANTIDAD"), negritas = true))
        carga.items.forEach { itemCarga ->
            val cant = formatearCantidad(itemCarga.cantidad)
            val prefijo = "$cant x "
            val espacioNombre = ANCHO - prefijo.length - cant.length
            val nombreCortado = recortar(itemCarga.producto, espacioNombre)
            s.write(texto(columna(prefijo + nombreCortado, cant)))
        }
        s.write(separador())
        s.write(texto(columna("TOTAL", formatearCantidad(carga.total_cantidad)), negritas = true))
        if (!carga.observaciones.isNullOrBlank()) {
            s.write(texto(columna("Observaciones", recortar(carga.observaciones, ANCHO))))
        }
        s.write(separador())
        s.write(lineaEnBlanco())
        s.write(texto("Carga registrada", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco(2))
        s.write(cortar())

        return s.toByteArray()
    }

    fun ticketCompra(compra: Compra): ByteArray {
        val s = ByteArrayOutputStream()

        s.write(init())
        s.write(configurarCodepage())
        s.write(lineaEnBlanco())
        s.write(separador())
        s.write(
            texto(
                linea = "DistribuERP",
                alineacion = Alineacion.CENTRO,
                negritas = true,
                dobleAncho = true,
                dobleAlto = true
            )
        )
        s.write(texto("www.distribuerp.com", Alineacion.CENTRO))
        s.write(lineaEnBlanco())
        s.write(texto("COMPROBANTE DE COMPRA", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco())
        s.write(texto(columna("Folio", compra.folio)))
        s.write(texto(columna("Fecha", compra.fecha ?: "—")))
        s.write(texto(columna("Proveedor", compra.proveedor ?: "—")))
        s.write(separador())
        s.write(texto(columna("CANT PRODUCTO", "SUBTOTAL"), negritas = true))
        compra.items.forEach { itemCompra ->
            s.write(
                item(
                    nombre = itemCompra.producto,
                    cantidad = itemCompra.cantidad,
                    precio = itemCompra.precio,
                    subtotal = itemCompra.subtotal
                )
            )
        }
        s.write(separador())
        s.write(texto(columna("TOTAL", formatearDinero(compra.total)), negritas = true))
        if (!compra.observaciones.isNullOrBlank()) {
            s.write(texto(columna("Observaciones", recortar(compra.observaciones, ANCHO))))
        }
        s.write(separador())
        s.write(lineaEnBlanco())
        s.write(texto("Compra registrada", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco(2))
        s.write(cortar())

        return s.toByteArray()
    }

    fun ticketPrueba(nombreImpresora: String): ByteArray {

        val s = ByteArrayOutputStream()

        s.write(init())
        s.write(configurarCodepage())
        s.write(separador())
        s.write(
            texto(
                linea = "DistribuERP",
                alineacion = Alineacion.CENTRO,
                negritas = true,
                dobleAncho = true,
                dobleAlto = true
            )
        )
        s.write(texto("www.distribuerp.com", Alineacion.CENTRO))
        s.write(lineaEnBlanco())
        s.write(texto("DIAGNÓSTICO", Alineacion.CENTRO, negritas = true))
        s.write(lineaEnBlanco())
        s.write(texto(columna("Versión", BuildConfig.APP_VERSION)))
        s.write(texto(columna("Fecha y hora", fechaActual())))
        s.write(texto(columna("Impresora", recortar(nombreImpresora, ANCHO))))
        s.write(texto(columna("Estado", "Conexión correcta")))
        s.write(separador())
        s.write(lineaEnBlanco(2))
        s.write(cortar())

        return s.toByteArray()
    }

    private fun fechaActual(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
}
