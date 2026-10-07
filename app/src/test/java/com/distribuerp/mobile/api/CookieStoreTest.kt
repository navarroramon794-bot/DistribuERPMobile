package com.distribuerp.mobile.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Persistencia cifrada de la session HTTP (fix UAT-05).
 *
 * `CookieStore` se prueba inyectando un `CifradorCookie` doble para cubrir los
 * tres estados posibles: cifrado correcto, cifrado imposible (Keystore sin
 * llave) y descifrado imposible (datos corrompidos). El almacen real es
 * Preferences DataStore sobre un archivo temporal, sin Robolectric.
 */
class CookieStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class CifradorDoble(
        var fallarCifrado: Boolean = false,
        var fallarDescifrado: Boolean = false
    ) : CifradorCookie {

        override fun cifrar(plaintext: String): ByteArray? {
            if (fallarCifrado) return null
            return plaintext.toByteArray(Charsets.UTF_8)
                .map { (it + 1).toByte() }
                .toByteArray()
        }

        override fun descifrar(cifrado: ByteArray): String? {
            if (fallarDescifrado) return null
            return String(
                cifrado.map { (it - 1).toByte() }.toByteArray(),
                Charsets.UTF_8
            )
        }
    }

    private fun nuevoPar(
        cifrador: CifradorCookie
    ): Pair<DataStore<Preferences>, CookieStore> {
        val archivo = { File(folder.root, "cookie.preferences_pb") }
        val ds = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO),
            produceFile = archivo
        )
        return ds to CookieStore(ds, cifrador)
    }

    private fun contenidoDisco(): String {
        val archivo = File(folder.root, "cookie.preferences_pb")
        return if (archivo.exists()) {
            String(archivo.readBytes(), Charsets.ISO_8859_1)
        } else {
            ""
        }
    }

    @Test
    fun `persistir y restaurar conserva las cookies sin texto plano`() = runBlocking {
        val (_, store) = nuevoPar(CifradorDoble())
        val cookies = listOf(
            "session=abc; domain=host.test; path=/; httponly",
            "csrf=xyz; path=/; httponly"
        )
        store.guardar(cookies)

        val restauradas = store.restaurar()

        assertEquals("CookieStore.restaurar()", cookies, restauradas)
        assertFalse(
            "Solo debe existir texto plano en claro",
            contenidoDisco().contains("session=abc")
        )
    }

    @Test
    fun `si el cifrado falla no se persiste nada en claro`() = runBlocking {
        val (_, store) = nuevoPar(CifradorDoble(fallarCifrado = true))

        store.guardar(listOf("session=secreta; path=/"))

        val restauradas = store.restaurar()
        assertTrue("Sin cifrado no debe quedar copia persistida", restauradas.isEmpty())
        assertFalse(
            "Nunca puede quedar la cookie en claro",
            contenidoDisco().contains("secreta")
        )
    }

    @Test
    fun `datos que no se pueden descifrar degradan la restauracion a vacio`() = runBlocking {
        val cifrador = CifradorDoble()
        val (_, store) = nuevoPar(cifrador)
        store.guardar(listOf("session=abc; path=/"))

        cifrador.fallarDescifrado = true
        assertTrue(store.restaurar().isEmpty())
    }

    @Test
    fun `contenido corrupto en disco se degrada a sin cookies`() = runBlocking {
        val (ds, store) = nuevoPar(CifradorDoble())

        // La llave "cookies" es el contrato con CookieStore.Keys.CIFRADO.
        ds.edit { it[stringPreferencesKey("cookies")] = "no-es-base64!!!" }

        assertTrue(store.restaurar().isEmpty())
    }

    @Test
    fun `limpiar borra la copia persistida`() = runBlocking {
        val (_, store) = nuevoPar(CifradorDoble())
        store.guardar(listOf("session=abc; path=/"))

        store.limpiar()

        assertTrue(store.restaurar().isEmpty())
    }

    @Test
    fun `guardar vacio equivale a limpiar`() = runBlocking {
        val (_, store) = nuevoPar(CifradorDoble())
        store.guardar(listOf("session=abc; path=/"))
        store.guardar(emptyList())

        assertTrue(store.restaurar().isEmpty())
    }

    @Test
    fun `restaurar reconstruye la sesion que el jar habia exportado`() = runBlocking {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("host.test")
            .build()
        val jarOrigen = SesionCookieJar()
        jarOrigen.saveFromResponse(
            url,
            listOf(
                Cookie.Builder()
                    .name("session").value("abc")
                    .domain("host.test").path("/")
                    .build(),
                Cookie.Builder()
                    .name("csrf").value("z")
                    .domain("host.test").path("/")
                    .build()
            )
        )

        val (_, store) = nuevoPar(CifradorDoble())
        store.guardar(jarOrigen.exportar())

        val jarDestino = SesionCookieJar()
        val restauradas = jarDestino.importar(store.restaurar(), url)

        assertEquals(2, restauradas)
        val devueltas = jarDestino.loadForRequest(url).associateBy { it.name() }
        assertEquals("abc", devueltas["session"]?.value())
        assertEquals("z", devueltas["csrf"]?.value())
    }
}