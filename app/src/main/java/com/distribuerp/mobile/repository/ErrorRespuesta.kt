package com.distribuerp.mobile.repository

import com.google.gson.JsonParser
import retrofit2.Response

/**
 * Error de negocio/devolucion del backend que conserva el codigo HTTP.
 *
 * Permite distinguir un rechazo definitivo (400/401/403/404) de un fallo
 * recuperable (408/429/5xx) sin depender del texto del mensaje.
 */
class ErrorApiException(
    val codigo: Int,
    mensaje: String? = null
) : Throwable(mensaje ?: "Error HTTP $codigo") {

    /** El backend rechazo la operacion de forma permanente: no se reintenta. */
    val esDefinitivo: Boolean
        get() = codigo in 400..499 && codigo != 408 && codigo != 429

    /** Fallo transitorio de infraestructura: se reintenta con el mismo id. */
    val esRecuperable: Boolean
        get() = codigo == 408 || codigo == 429 || codigo in 500..599
}

fun extraerError(
    response: Response<*>
): Throwable {

    val mensaje = response.errorBody()?.string()?.let {
        cuerpo ->

        try {
            JsonParser.parseString(cuerpo)
                .asJsonObject
                .get("mensaje")
                ?.asString
        } catch (e: Exception) {
            null
        }
    }

    return ErrorApiException(
        codigo = response.code(),
        mensaje = mensaje ?: "Error HTTP ${response.code()}"
    )
}

fun mensajeAmigable(t: Throwable): String {
    return when (t) {
        is java.net.SocketTimeoutException,
        is java.io.InterruptedIOException ->
            "El servidor tardó demasiado en responder. " +
                    "Verifica tu conexión e inténtalo de nuevo."

        is java.net.UnknownHostException ->
            "Sin conexión a internet. " +
                    "Verifica tu red e inténtalo de nuevo."

        is javax.net.ssl.SSLException ->
            "No se pudo establecer una conexión segura con el servidor. " +
                    "Inténtalo más tarde."

        is java.net.ConnectException,
        is java.net.NoRouteToHostException ->
            "No se pudo conectar con el servidor. " +
                    "Verifica tu conexión o inténtalo más tarde."

        is java.io.IOException ->
            "Error de conexión. " +
                    "Verifica tu red e inténtalo de nuevo."

        else ->
            t.message?.takeIf { it.isNotBlank() }
                ?: "Ocurrió un error. Inténtalo de nuevo."
    }
}
