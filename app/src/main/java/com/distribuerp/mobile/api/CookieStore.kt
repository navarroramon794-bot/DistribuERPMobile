package com.distribuerp.mobile.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private val Context.cookieDataStore by preferencesDataStore(
    name = "cookie"
)

/**
 * Cifra y descifra el bloque de cookies persistidas.
 *
 * Se separa como seam para probar `CookieStore` en Robolectric con un doble
 * que falle igual que fallaria AndroidKeyStore (llave borrada, datos
 * corrompidos), sin depender del Keystore real del instrumento.
 */
interface CifradorCookie {
    fun cifrar(plaintext: String): ByteArray?
    fun descifrar(cifrado: ByteArray): String?
}

/**
 * Cifrado AES/GCM real apoyado en AndroidKeyStore.
 *
 * La llave se genera una sola vez (`distribuerp_cookie_v1`) y nunca sale del
 * Keystore. Formato en disco: `Base64(iv[12] + cifrado)`.
 *
 * Cualquier fallo (llave inexistente tras un backup, datos corrompidos o
 * manipulados) devuelve `null`: aqui no existe degradacion a texto plano.
 */
class AndroidKeyStoreCifrador(
    context: Context
) : CifradorCookie {

    private companion object {
        const val ALIAS = "distribuerp_cookie_v1"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        val AAD = "distribuerp.cookie.sesion".toByteArray(Charsets.UTF_8)
    }

    private fun llave(): SecretKey? {
        return try {
            val almacen = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            val existente =
                (almacen.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            existente ?: generar()
        } catch (_: Exception) {
            null
        }
    }

    private fun generar(): SecretKey? {
        return try {
            val generador = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            generador.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generador.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    override fun cifrar(plaintext: String): ByteArray? {
        return try {
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.ENCRYPT_MODE, llave() ?: return null)
            cifrador.updateAAD(AAD)
            val cuerpo = cifrador.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            cifrador.iv + cuerpo
        } catch (_: Exception) {
            null
        }
    }

    override fun descifrar(cifrado: ByteArray): String? {
        return try {
            if (cifrado.size < IV_BYTES + (GCM_TAG_BITS / 8)) return null
            val iv = cifrado.copyOfRange(0, IV_BYTES)
            val cuerpo = cifrado.copyOfRange(IV_BYTES, cifrado.size)
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(
                Cipher.DECRYPT_MODE,
                llave() ?: return null,
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            cifrador.updateAAD(AAD)
            String(cifrador.doFinal(cuerpo), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Persistencia de cookies de sesion cifradas.
 *
 * Guarda una sola cadena (las cookies serializadas separadas por salto de
 * linea) bajo la llave `cookies`, cifrada con [CifradorCookie]. Como el
 * cifrado es autenticado (AES/GCM), cualquier manipulacion en disco se detecta
 * y la restauracion se degrada a "sin cookies".
 *
 * Nunca almacena, loguea ni devuelve el texto plano de una cookie. Si el
 * cifrado no es posible, [guardar] simplemente borra lo que hubiera.
 */
class CookieStore(
    private val dataStore: DataStore<Preferences>,
    private val cifrador: CifradorCookie
) {

    private object Keys {
        val CIFRADO = stringPreferencesKey("cookies")
    }

    suspend fun guardar(serializadas: List<String>) {
        if (serializadas.isEmpty()) {
            limpiar()
            return
        }

        val cifrado = cifrador.cifrar(serializadas.joinToString("\n"))
        if (cifrado == null) {
            // Sin Keystore no hay cifrado, y sin cifrado no hay persistencia.
            limpiar()
            return
        }

        dataStore.edit { prefs ->
            prefs[Keys.CIFRADO] = Base64.getEncoder().encodeToString(cifrado)
        }
    }

    suspend fun restaurar(): List<String> {
        val guardado = dataStore.data.first()[Keys.CIFRADO] ?: return emptyList()

        val cifrado = try {
            Base64.getDecoder().decode(guardado)
        } catch (_: Exception) {
            return emptyList()
        }

        val texto = cifrador.descifrar(cifrado) ?: return emptyList()

        return texto.split("\n").filter { it.isNotBlank() }
    }

    suspend fun limpiar() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.CIFRADO)
        }
    }

    companion object {
        fun crear(context: Context): CookieStore {
            val app = context.applicationContext
            return CookieStore(
                dataStore = app.cookieDataStore,
                cifrador = AndroidKeyStoreCifrador(app)
            )
        }
    }
}