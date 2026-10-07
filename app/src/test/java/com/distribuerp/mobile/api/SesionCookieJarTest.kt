package com.distribuerp.mobile.api

import okhttp3.Cookie
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionCookieJarTest {

    private val JAR = SesionCookieJar()

    private val HOST = "distribu-erp.onrender.com"

    private fun url(
        ruta: String = "/api/licencias/verificar"
    ): HttpUrl =
        HttpUrl.Builder()
            .scheme("https")
            .host(HOST)
            .encodedPath(ruta)
            .build()

    private fun cookie(
        name: String = "session",
        value: String = "abc123",
        domain: String = HOST,
        hostOnly: Boolean = true,
        path: String = "/",
        expiresAt: Long = ahora() + 3600_000L
    ): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)

        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }

        return builder.build()
    }

    private fun expirada(
        name: String = "session",
        value: String = "expired"
    ): Cookie =
        cookie(
            name = name,
            value = value,
            expiresAt = ahora() - 1000L
        )

    private fun ahora(): Long = System.currentTimeMillis()

    private fun headerDeAplicacion(jar: SesionCookieJar = JAR): String =
        jar.loadForRequest(url())
            .joinToString("; ") { "${it.name()}=${it.value()}" }

    @Test
    fun nuevaCookieReemplazaAnteriorPorIdentidad() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(cookie(value = "primerlogin")))
        jar.saveFromResponse(url(), listOf(cookie(value = "segundologin")))

        val devueltas = jar.loadForRequest(url())

        assertEquals(1, devueltas.size)
        assertEquals("segundologin", devueltas[0].value())
    }

    @Test
    fun noAcumulaCookiesIguales() {
        val jar = SesionCookieJar()

        for (i in 0 until 30) {
            jar.saveFromResponse(url(), listOf(cookie(value = "v$i")))
        }

        val devueltas = jar.loadForRequest(url())

        assertEquals(1, devueltas.size)
        assertEquals("v29", devueltas[0].value())
    }

    @Test
    fun cookieExpiradaNoSeDevuelve() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(expirada()))

        assertTrue(jar.loadForRequest(url()).isEmpty())
        assertFalse(jar.tieneCookies())
    }

    @Test
    fun cookieValidaSeDevuelve() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(cookie()))

        val devueltas = jar.loadForRequest(url())

        assertEquals(1, devueltas.size)
        assertEquals("session", devueltas[0].name())
    }

    @Test
    fun cookieExpiradaReemplazaYNoReenviaLaAnterior() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(cookie(value = "session-activa")))
        jar.saveFromResponse(url(), listOf(expirada(value = "tombstone")))

        assertTrue(jar.loadForRequest(url()).isEmpty())
        assertFalse(jar.tieneCookies())
    }

    @Test
    fun limpiarVaciaElAlmacenComoLogout() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(cookie()))
        jar.limpiar()

        assertTrue(jar.loadForRequest(url()).isEmpty())
        assertFalse(jar.tieneCookies())
    }

    @Test
    fun loginLogoutLoginDevuelveUnaSolaSession() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(url(), listOf(cookie(value = "primer-login")))
        jar.saveFromResponse(url(), listOf(expirada()))

        assertEquals(0, jar.loadForRequest(url()).size)

        jar.saveFromResponse(url(), listOf(cookie(value = "segundo-login")))

        val devueltas = jar.loadForRequest(url())

        assertEquals(1, devueltas.size)
        assertEquals("segundo-login", devueltas[0].value())
    }

    @Test
    fun treintaCiclosLoginLogoutNoCrecenNiDesbordan() {
        val jar = SesionCookieJar()

        for (i in 0 until 30) {
            jar.saveFromResponse(url(), listOf(cookie(value = "login-$i")))
            jar.saveFromResponse(url(), listOf(expirada(value = "logout-$i")))
        }

        assertTrue(jar.loadForRequest(url()).isEmpty())
        assertFalse(jar.tieneCookies())
    }

    @Test
    fun muchasRespuestasNoDesbordanElHeader() {
        val jar = SesionCookieJar()

        for (i in 0 until 500) {
            jar.saveFromResponse(url(), listOf(cookie(value = "valor-unico")))
        }

        val header = headerDeAplicacion(jar)

        assertEquals("session=valor-unico", header)
        assertTrue(header.length < 200)
    }

    @Test
    fun igualNombreDistintoPathSeTratanPorSeparado() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(
            url("/api/login"),
            listOf(cookie(name = "session", value = "raiz", path = "/"))
        )
        jar.saveFromResponse(
            url("/admin"),
            listOf(cookie(name = "session", value = "admin", path = "/admin"))
        )

        val devueltas = jar.loadForRequest(url("/api/licencias/verificar"))

        assertEquals(1, devueltas.size)
        assertEquals("raiz", devueltas[0].value())
    }

    @Test
    fun igualNombreDistintoDominioSeTratanPorSeparado() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(
            url(),
            listOf(cookie(name = "session", value = "propia"))
        )
        jar.saveFromResponse(
            url(),
            listOf(cookie(name = "session", value = "ajena", domain = "otro.host.com"))
        )

        val devueltas = jar.loadForRequest(url())

        assertEquals(1, devueltas.size)
        assertEquals("propia", devueltas[0].value())
        assertTrue(headerDeAplicacion(jar).startsWith("session=propia"))
    }

    @Test
    fun noRompeOtrasCookiesLegitimas() {
        val jar = SesionCookieJar()

        jar.saveFromResponse(
            url("/api/login"),
            listOf(
                cookie(name = "session", value = "abc"),
                cookie(name = "csrf", value = "xyz", expiresAt = ahora() + 60_000L)
            )
        )
        jar.saveFromResponse(url(), listOf(cookie(name = "session", value = "def")))
        jar.saveFromResponse(url(), listOf(expirada()))

        val devueltas = jar.loadForRequest(url("/api/login")).associateBy { it.name() }

        assertEquals(null, devueltas["session"])
        assertEquals("xyz", devueltas["csrf"]?.value())
    }

    // --- Exportar/importar: persistencia de la session HTTP (fix UAT-05) ---

    @Test
    fun exportarEImportarConservanLaSession() {
        val jarOrigen = SesionCookieJar()
        jarOrigen.saveFromResponse(url(), listOf(cookie(value = "exportada")))

        val serializadas = jarOrigen.exportar()
        assertFalse(
            "La exportacion no puede llegar vacia con una cookie viva",
            serializadas.isEmpty()
        )

        val jarDestino = SesionCookieJar()
        val restauradas = jarDestino.importar(serializadas, url())

        assertEquals(1, restauradas)
        assertEquals("session=exportada", headerDeAplicacion(jarDestino))
        assertTrue(jarDestino.tieneCookies())
    }

    @Test
    fun exportarExcluyeLasCookiesYaExpiradas() {
        val jarOrigen = SesionCookieJar()
        jarOrigen.saveFromResponse(url(), listOf(expirada(value = "vieja")))

        assertTrue(jarOrigen.exportar().isEmpty())
    }

    @Test
    fun importarDescartaCookiesExpiradas() {
        val jar = SesionCookieJar()

        val restauradas = jar.importar(listOf(expirada().toString()), url())

        assertEquals(0, restauradas)
        assertTrue(jar.loadForRequest(url()).isEmpty())
        assertFalse(jar.tieneCookies())
    }

    @Test
    fun importarIgnoraLineasInvalidas() {
        val jar = SesionCookieJar()
        val serializadas = listOf(
            "esto-no-es-una-cookie",
            "tambien.deberia.ignorarse",
            cookie(name = "session", value = "valida").toString()
        )

        val restauradas = jar.importar(serializadas, url())

        assertEquals(1, restauradas)
        assertEquals("session=valida", headerDeAplicacion(jar))
    }

    @Test
    fun importarMantieneLaUltimaPorIdentidad() {
        val jarOrigen = SesionCookieJar()
        jarOrigen.saveFromResponse(url(), listOf(cookie(value = "primera")))
        jarOrigen.saveFromResponse(url(), listOf(cookie(value = "segunda")))

        val jarDestino = SesionCookieJar()
        jarDestino.importar(jarOrigen.exportar(), url())

        assertEquals("session=segunda", headerDeAplicacion(jarDestino))
    }
}
