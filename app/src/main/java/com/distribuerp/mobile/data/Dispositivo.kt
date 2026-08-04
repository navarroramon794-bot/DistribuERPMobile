package com.distribuerp.mobile.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.dispositivoDataStore by preferencesDataStore(
    name = "dispositivo"
)

object Dispositivo {

    private const val CLAVE_UUID = "uuid_fallback"

    suspend fun obtenerAndroidId(context: Context): String? {
        val delSistema = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            null
        }

        if (!delSistema.isNullOrBlank()) {
            return delSistema
        }

        val store = context.dispositivoDataStore

        val prefs = store.data.first()

        val previo = prefs[stringPreferencesKey(CLAVE_UUID)]

        if (!previo.isNullOrBlank()) {
            return previo
        }

        val nuevo = UUID.randomUUID().toString()

        store.edit { it[stringPreferencesKey(CLAVE_UUID)] = nuevo }

        return nuevo
    }
}
