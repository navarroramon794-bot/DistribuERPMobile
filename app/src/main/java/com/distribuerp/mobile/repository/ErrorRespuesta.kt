package com.distribuerp.mobile.repository

import com.google.gson.JsonParser
import retrofit2.Response

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

    return Throwable(
        mensaje ?: "Error HTTP ${response.code()}"
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
