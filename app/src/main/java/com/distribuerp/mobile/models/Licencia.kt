package com.distribuerp.mobile.models

data class LicenciaResponse(
    val ok: Boolean = false,
    val mensaje: String? = null,
    val licencia: LicenciaInfo? = null
)

data class VerificarResponse(
    val ok: Boolean = false,
    val valida: Boolean = false,
    val mensaje: String? = null,
    val licencia: LicenciaInfo? = null
)

data class ActivarRequest(
    val codigo: String,
    val android_id: String?,
    val version_app: String?
)

data class DemoRequest(
    val android_id: String?,
    val version_app: String?
)

data class LicenciaInfo(
    val id: Int = 0,
    val codigo: String? = null,
    val empresa: String? = null,
    val contacto: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val tipo: String? = null,
    val permanente: Boolean = false,
    val fecha_inicio: String? = null,
    val fecha_vencimiento: String? = null,
    val dias_gracia: Int = 5,
    val estado: String? = null,
    val estado_guardado: String? = null,
    val observaciones: String? = null,
    val vendedor: String? = null,
    val fecha_activacion: String? = null,
    val ultima_validacion: String? = null,
    val android_id_hash: String? = null,
    val version_app: String? = null,
    val dias_restantes: Int? = null,
    val valida: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    val esDemo: Boolean
        get() = tipo == "demo"

    val esPermanente: Boolean
        get() = permanente || tipo == "permanente"

    fun codigoEnmascarado(): String {
        val codigo = codigo ?: return "********"
        val visibles = codigo.takeLast(6)

        return "*".repeat(8) + visibles
    }

    fun planLegible(): String {
        return when (tipo) {
            "demo" -> "Demostración (15 días)"
            "mensual" -> "Mensual"
            "trimestral" -> "Trimestral"
            "semestral" -> "Semestral"
            "anual" -> "Anual"
            "permanente" -> "Permanente"
            else -> tipo ?: "—"
        }
    }

    fun estadoLegible(): String {
        return when (estado) {
            "activa" -> "Activa"
            "demo" -> "Demo"
            "expirada" -> "Expirada"
            "suspendida" -> "Suspendida"
            "cancelada" -> "Cancelada"
            else -> estado ?: "—"
        }
    }
}
