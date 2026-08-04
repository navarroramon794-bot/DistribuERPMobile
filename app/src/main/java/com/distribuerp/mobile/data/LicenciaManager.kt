package com.distribuerp.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.distribuerp.mobile.models.LicenciaInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Context.licenciaDataStore by preferencesDataStore(
    name = "licencia"
)

data class LicenciaGuardada(
    val codigo: String,
    val empresa: String?,
    val contacto: String?,
    val telefono: String?,
    val correo: String?,
    val tipo: String?,
    val permanente: Boolean,
    val fechaInicio: String?,
    val fechaVencimiento: String?,
    val diasGracia: Int,
    val estado: String?,
    val diasRestantes: Int?,
    val valida: Boolean,
    val ultimaValidacion: String?,
    val fechaActivacion: String?,
    val versionApp: String?,
    val ultimoCheck: Long
) {

    val esDemo: Boolean
        get() = tipo == "demo"

    fun toLicenciaInfo(): LicenciaInfo {
        return LicenciaInfo(
            codigo = codigo,
            empresa = empresa,
            contacto = contacto,
            telefono = telefono,
            correo = correo,
            tipo = tipo,
            permanente = permanente,
            fecha_inicio = fechaInicio,
            fecha_vencimiento = fechaVencimiento,
            dias_gracia = diasGracia,
            estado = estado,
            fecha_activacion = fechaActivacion,
            ultima_validacion = ultimaValidacion,
            version_app = versionApp,
            dias_restantes = diasRestantes,
            valida = valida
        )
    }
}

class LicenciaManager(private val context: Context) {

    private object Keys {
        val CODIGO = stringPreferencesKey("codigo")
        val EMPRESA = stringPreferencesKey("empresa")
        val CONTACTO = stringPreferencesKey("contacto")
        val TELEFONO = stringPreferencesKey("telefono")
        val CORREO = stringPreferencesKey("correo")
        val TIPO = stringPreferencesKey("tipo")
        val PERMANENTE = booleanPreferencesKey("permanente")
        val FECHA_INICIO = stringPreferencesKey("fecha_inicio")
        val FECHA_VENCIMIENTO = stringPreferencesKey("fecha_vencimiento")
        val DIAS_GRACIA = intPreferencesKey("dias_gracia")
        val ESTADO = stringPreferencesKey("estado")
        val DIAS_RESTANTES = intPreferencesKey("dias_restantes")
        val VALIDA = booleanPreferencesKey("valida")
        val ULTIMA_VALIDACION = stringPreferencesKey("ultima_validacion")
        val FECHA_ACTIVACION = stringPreferencesKey("fecha_activacion")
        val VERSION_APP = stringPreferencesKey("version_app")
        val ULTIMO_CHECK = longPreferencesKey("ultimo_check")
    }

    val licencia: Flow<LicenciaGuardada?> =
        context.licenciaDataStore.data.map { prefs ->
            val codigo = prefs[Keys.CODIGO]

            if (codigo == null) {
                null
            } else {
                LicenciaGuardada(
                    codigo = codigo,
                    empresa = prefs[Keys.EMPRESA],
                    contacto = prefs[Keys.CONTACTO],
                    telefono = prefs[Keys.TELEFONO],
                    correo = prefs[Keys.CORREO],
                    tipo = prefs[Keys.TIPO],
                    permanente = prefs[Keys.PERMANENTE] ?: false,
                    fechaInicio = prefs[Keys.FECHA_INICIO],
                    fechaVencimiento = prefs[Keys.FECHA_VENCIMIENTO],
                    diasGracia = prefs[Keys.DIAS_GRACIA] ?: 5,
                    estado = prefs[Keys.ESTADO],
                    diasRestantes = prefs[Keys.DIAS_RESTANTES],
                    valida = prefs[Keys.VALIDA] ?: false,
                    ultimaValidacion = prefs[Keys.ULTIMA_VALIDACION],
                    fechaActivacion = prefs[Keys.FECHA_ACTIVACION],
                    versionApp = prefs[Keys.VERSION_APP],
                    ultimoCheck = prefs[Keys.ULTIMO_CHECK] ?: 0L
                )
            }
        }

    suspend fun obtener(): LicenciaGuardada? =
        licencia.first()

    suspend fun guardar(info: LicenciaInfo) {
        context.licenciaDataStore.edit { prefs ->
            prefs[Keys.CODIGO] = info.codigo ?: return@edit
            prefs.cadena(Keys.EMPRESA, info.empresa)
            prefs.cadena(Keys.CONTACTO, info.contacto)
            prefs.cadena(Keys.TELEFONO, info.telefono)
            prefs.cadena(Keys.CORREO, info.correo)
            prefs.cadena(Keys.TIPO, info.tipo)
            prefs[Keys.PERMANENTE] = info.permanente
            prefs.cadena(Keys.FECHA_INICIO, info.fecha_inicio)
            prefs.cadena(Keys.FECHA_VENCIMIENTO, info.fecha_vencimiento)
            prefs[Keys.DIAS_GRACIA] = info.dias_gracia
            prefs.cadena(Keys.ESTADO, info.estado)
            prefs[Keys.DIAS_RESTANTES] = info.dias_restantes ?: -1
            prefs[Keys.VALIDA] = info.valida
            prefs.cadena(Keys.ULTIMA_VALIDACION, info.ultima_validacion)
            prefs.cadena(Keys.FECHA_ACTIVACION, info.fecha_activacion)
            prefs.cadena(Keys.VERSION_APP, info.version_app)
            prefs[Keys.ULTIMO_CHECK] = System.currentTimeMillis()
        }
    }

    suspend fun borrar() {
        context.licenciaDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun MutablePreferences.cadena(
        clave: Preferences.Key<String>,
        valor: String?
    ) {
        if (valor != null) {
            set(clave, valor)
        } else {
            remove(clave)
        }
    }

    companion object {

        /** Días de tolerancia sin conexión al servidor. */
        const val DIAS_GRACIA_SIN_INTERNET = 5L

        private val FORMATO = DateTimeFormatter.ISO_LOCAL_DATE

        fun parsearFecha(valor: String?): LocalDate? {
            if (valor.isNullOrBlank()) return null

            return try {
                LocalDate.parse(valor, FORMATO)
            } catch (e: Exception) {
                null
            }
        }

        fun diasRestantes(fechaVencimiento: String?): Int? {
            val vencimiento = parsearFecha(fechaVencimiento) ?: return null

            val dias = ChronoUnit.DAYS.between(
                LocalDate.now(),
                vencimiento
            )

            return dias.coerceAtLeast(0).toInt()
        }
    }
}
