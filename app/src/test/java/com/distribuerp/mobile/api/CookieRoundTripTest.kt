package com.distribuerp.mobile.api

import okhttp3.Cookie
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * R13 (requisito del analisis): el formato de persistencia de cookies debe
 * sobrevivir un round-trip real con el OkHttp que usa la app.
 *
 * [Cookie.toString] es la serializacion canonica de OkHttp y [Cookie.parse] la
 * relectura. Si el par no preserva la identidad (nombre|dominio|path) ni los
 * atributos (secure/httpOnly/expira), la persistencia NO debe apoyarse en ese
 * formato y habra que usar un formato propio (Gson).
 *
 * En produccion las cookies entran por `saveFromResponse` (OkHttp ya las
 * parseo) y salen a disco con `toString()`: esto prueba exactamente ese ciclo.
 */
class CookieRoundTripTest {

    private val URL: HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("distribu-erp.onrender.com")
        .encodedPath("/api/ventas")
        .build()

    private fun roundTrip(
        setCookie: String,
        url: HttpUrl = URL
    ): Pair<Cookie, Cookie> {
        val original = Cookie.parse(url, setCookie)
        assertNotNull("Set-Cookie '$setCookie' debe parsear con OkHttp", original)

        val serializado = original!!.toString()
        assertNotNull(
            "toString()='$serializado' debe volver a parsear con OkHttp",
            Cookie.parse(url, serializado)
        )
        val releida = Cookie.parse(url, serializado)!!

        return original to releida
    }

    private fun assertEqualsCookie(
        etiqueta: String,
        original: Cookie,
        releida: Cookie,
        toleranciaExpiraMs: Long = 2_000L
    ) {
        assertEquals("$etiqueta: name", original.name(), releida.name())
        assertEquals("$etiqueta: value", original.value(), releida.value())
        assertEquals("$etiqueta: domain", original.domain(), releida.domain())
        assertEquals("$etiqueta: hostOnly", original.hostOnly(), releida.hostOnly())
        assertEquals("$etiqueta: path", original.path(), releida.path())
        assertEquals("$etiqueta: secure", original.secure(), releida.secure())
        assertEquals("$etiqueta: httpOnly", original.httpOnly(), releida.httpOnly())
        assertEquals("$etiqueta: persistent", original.persistent(), releida.persistent())

        // La expiracion se formatea a segundo al serializar: admitimos el
        // desfase de redondeo, pero nunca un salto de 0 a MAX_VALUE.
        val desfase = abs(original.expiresAt() - releida.expiresAt())
        assertTrue(
            "$etiqueta: expiresAt difiere demasiado: $desfase",
            desfase <= toleranciaExpiraMs
        )
    }

    @Test
    fun `cookie de sesion host-only con HttpOnly conserva identidad`() {
        val (original, releida) = roundTrip(
            "session=abc123; Path=/; HttpOnly"
        )

        assertEqualsCookie("host-only+httponly", original, releida)
        // OkHttp 4.12 marca las de sesion persistent=false y mantiene la misma
        // expiracion literal al serializar (omite el atributo expires).
        assertFalse("Una cookie de sesion no es persistente", releida.persistent())
        assertEquals(
            "La cookie de sesion no debe hurtar expiracion en el round-trip",
            original.expiresAt(),
            releida.expiresAt()
        )
    }

    @Test
    fun `cookie persistente con Secure y HttpOnly conserva todos los atributos`() {
        val (original, releida) = roundTrip(
            "session=abc123; Path=/; HttpOnly; Secure; Max-Age=86400"
        )

        assertEqualsCookie("persistente+secure", original, releida)
        assertTrue(releida.secure())
        assertTrue(releida.httpOnly())
        assertTrue(releida.hostOnly())
        assertTrue(releida.persistent())
    }

    @Test
    fun `cookie con dominio explicito no host-only conserva el dominio`() {
        val (original, releida) = roundTrip(
            "session=abc123; Domain=onrender.com; Path=/; Max-Age=86400"
        )

        assertEqualsCookie("dominio explicito", original, releida)
        assertEquals("onrender.com", releida.domain())
    }

    @Test
    fun `cookie con path distinto de raiz conserva el path`() {
        val (original, releida) = roundTrip(
            "sid=xyz; Path=/api; Max-Age=300"
        )

        assertEqualsCookie("path especifico", original, releida)
        assertEquals("/api", releida.path())
    }

    @Test
    fun `cookie de sesion sin Max-Age sigue siendo de sesion tras el round-trip`() {
        val (original, releida) = roundTrip(
            "connect.sid=deadbeef; Path=/; HttpOnly"
        )

        assertEqualsCookie("session sin max-age", original, releida)
        assertFalse(
            "Sin Max-Age ni Expires la cookie sigue siendo de sesion (persistent=false)",
            releida.persistent()
        )
        assertEquals(
            "La cookie de sesion conserva su expiracion literal",
            original.expiresAt(),
            releida.expiresAt()
        )
    }

    @Test
    fun `una cookie distinta del host (dominio valido) conserva su dominio`() {
        val urlHelper = HttpUrl.Builder()
            .scheme("https")
            .host("mi.otro.host.com")
            .encodedPath("/")
            .build()

        val (original, releida) = roundTrip(
            "pref=1; Domain=otro.host.com; Path=/; Max-Age=600",
            url = urlHelper
        )

        assertEqualsCookie("dominio no exacto", original, releida)
        assertEquals("otro.host.com", releida.domain())
    }

    @Test
    fun `varias cookies distintas se serializan y releen sin perder identidad`() {
        val originales = listOf(
            Cookie.parse(URL, "session=abc; Path=/; HttpOnly; Max-Age=86400")!!,
            Cookie.parse(URL, "csrf=xyz; Path=/; Max-Age=600; Secure")!!,
            Cookie.parse(URL, "pref=dark; Path=/prefs; Max-Age=3600")!!
        )

        val releidas = originales.map { original ->
            val serializado = original.toString()
            Cookie.parse(URL, serializado)!!
        }

        originales.zip(releidas).forEachIndexed { i, (original, releida) ->
            assertEqualsCookie("multi-cookie[$i]", original, releida)
        }
    }
}