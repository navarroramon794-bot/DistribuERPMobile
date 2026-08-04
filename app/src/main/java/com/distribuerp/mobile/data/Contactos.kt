package com.distribuerp.mobile.data

import android.content.Intent
import android.net.Uri

/**
 * Datos de contacto de C&R Technology Solutions.
 *
 * Para actualizarlos no hace falta tocar la lógica de la aplicación:
 * basta con cambiar las constantes centralizadas de abajo.
 */
object Contactos {

    /** Número de WhatsApp con lada internacional, sin el signo '+'. */
    const val CONTACTO_WHATSAPP = "523122703836"

    const val CONTACTO_EMAIL = "soluciones.cr.en.tecnologia@gmail.com"

    const val CONTACTO_WEB = "https://distribu-erp.onrender.com"

    const val MENSAJE_DEMOSTRACION =
        "Hola, me interesa una demostración de DistribuERP."

    const val MENSAJE_COMPRA =
        "Hola, quiero adquirir una licencia de DistribuERP."

    fun intentWhatsApp(mensaje: String = MENSAJE_COMPRA): Intent? {
        return try {
            val url = "https://wa.me/$CONTACTO_WHATSAPP" +
                "?text=${Uri.encode(mensaje)}"

            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } catch (e: Exception) {
            null
        }
    }

    fun intentCorreo(asunto: String, cuerpo: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CONTACTO_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, cuerpo)
        }
    }

    fun intentWeb(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(CONTACTO_WEB))
    }
}
