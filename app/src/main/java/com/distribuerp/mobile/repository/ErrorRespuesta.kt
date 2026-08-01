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
