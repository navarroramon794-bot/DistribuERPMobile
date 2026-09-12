package com.distribuerp.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "session"
)

class SessionManager(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("id")
        val NOMBRE = stringPreferencesKey("nombre")
        val CORREO = stringPreferencesKey("correo")
        val ROL = stringPreferencesKey("rol")
        val VENDEDOR_ID = stringPreferencesKey("vendedor_id")
        val VENDEDOR = stringPreferencesKey("vendedor")
        val PASSWORD_TEMPORAL = booleanPreferencesKey("password_temporal")
    }

    val sesion: Flow<UsuarioGuardado?> =
        context.dataStore.data.map { prefs ->
            val id = prefs[Keys.ID]
            val nombre = prefs[Keys.NOMBRE]
            val correo = prefs[Keys.CORREO]
            if (id == null || nombre == null || correo == null) {
                null
            } else {
                UsuarioGuardado(
                    id = id,
                    nombre = nombre,
                    correo = correo,
                    rol = prefs[Keys.ROL],
                    vendedor_id = prefs[Keys.VENDEDOR_ID],
                    vendedor = prefs[Keys.VENDEDOR],
                    password_temporal = prefs[Keys.PASSWORD_TEMPORAL] ?: false
                )
            }
        }

    suspend fun guardarSesion(usuario: UsuarioGuardado) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ID] = usuario.id
            prefs[Keys.NOMBRE] = usuario.nombre
            prefs[Keys.CORREO] = usuario.correo
            prefs[Keys.ROL] = usuario.rol ?: ""
            prefs[Keys.VENDEDOR_ID] = usuario.vendedor_id ?: ""
            prefs[Keys.VENDEDOR] = usuario.vendedor ?: ""
            prefs[Keys.PASSWORD_TEMPORAL] = usuario.password_temporal ?: false
        }
    }

    suspend fun actualizarPasswordTemporal(valor: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PASSWORD_TEMPORAL] = valor
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

data class UsuarioGuardado(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: String?,
    val vendedor_id: String? = null,
    val vendedor: String? = null,
    val password_temporal: Boolean? = null
)
