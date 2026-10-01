package com.distribuerp.mobile.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * CookieJar en memoria con semántica HTTP correcta.
 *
 * Corrige el problema de acumulación de cookies del APK (HTTP 431):
 * - una cookie se identifica por (nombre, dominio, path);
 * - una nueva cookie con la misma identidad reemplaza a la anterior;
 * - las cookies expiradas se eliminan y nunca se devuelven en
 *   [loadForRequest];
 * - el almacén permanece acotado: nunca existen dos cookies con la
 *   misma identidad, por lo que el header Cookie no crece de forma
 *   indefinida tras repetidos login/logout.
 *
 * No introduce persistencia: el contenido es solo en memoria, igual que
 * el comportamiento anterior del APK.
 */
class SesionCookieJar : CookieJar {

    private val almacenadas = mutableMapOf<String, Cookie>()

    private fun identidad(cookie: Cookie): String =
        "${cookie.name()}|${cookie.domain()}|${cookie.path()}"

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        for (cookie in cookies) {
            val clave = identidad(cookie)

            if (cookie.expiresAt() <= System.currentTimeMillis()) {
                almacenadas.remove(clave)
            } else {
                almacenadas[clave] = cookie
            }
        }
    }

    @Synchronized
    override fun loadForRequest(
        url: HttpUrl
    ): List<Cookie> {
        val ahora = System.currentTimeMillis()

        return almacenadas.values.filter { cookie ->
            cookie.expiresAt() > ahora && cookie.matches(url)
        }
    }

    @Synchronized
    fun limpiar() {
        almacenadas.clear()
    }

    @Synchronized
    fun tieneCookies(): Boolean {
        val ahora = System.currentTimeMillis()

        return almacenadas.values.any { it.expiresAt() > ahora }
    }
}
