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
 * La persistencia del contenido es externa: [exportar] produce la vista
 * serializada que un [CookieStore] guarda cifrada, y [importar] la restaura.
 * En memoria sigue siendo solo el jar sobre el que OkHttp aplica la semantica.
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

    /**
     * Serializa las cookies vivas con el formato canonico de OkHttp
     * ([Cookie.toString]), listas para persistirlas.
     *
     * Las cookies ya expiradas nunca salen del jar hacia el disco.
     */
    @Synchronized
    fun exportar(): List<String> {
        val ahora = System.currentTimeMillis()

        return almacenadas.values
            .filter { it.expiresAt() > ahora }
            .map { it.toString() }
    }

    /**
     * Restaura cookies previamente exportadas ([exportar]).
     *
     * Devuelve cuantas cookies vivas se restauraron. Las lineas invalidas,
     * corrompidas o ya expiradas se descartan en silencio, de modo que un disco
     * danado nunca rompe el arranque.
     */
    @Synchronized
    fun importar(
        serializadas: List<String>,
        url: HttpUrl
    ): Int {
        var restauradas = 0

        for (linea in serializadas) {
            val cookie = Cookie.parse(url, linea) ?: continue
            val clave = identidad(cookie)

            if (cookie.expiresAt() <= System.currentTimeMillis()) {
                almacenadas.remove(clave)
            } else {
                almacenadas[clave] = cookie
                restauradas++
            }
        }

        return restauradas
    }

    @Synchronized
    fun tieneCookies(): Boolean {
        val ahora = System.currentTimeMillis()

        return almacenadas.values.any { it.expiresAt() > ahora }
    }
}
