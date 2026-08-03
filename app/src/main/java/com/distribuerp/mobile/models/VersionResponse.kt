package com.distribuerp.mobile.models

data class VersionResponse(
    val app: String? = null,
    val app_mobile: String? = null,
    val version: String? = null,
    val build: String? = null,
    val fecha_compilacion: String? = null,
    val fecha_publicacion: String? = null,
    val android_min: String? = null,
    val ambiente: String? = null,
    val cambios: List<String> = emptyList(),
    val novedades: List<String> = emptyList(),
    val apk: ApkInfo? = null,
    val apk_disponible: Boolean = false,
    val descarga_url: String? = null,
    val documentos: List<DocumentoInfo> = emptyList()
)

data class ApkInfo(
    val nombre: String? = null,
    val tamano: Long = 0,
    val tamano_mb: Double = 0.0,
    val sha256: String? = null
)

data class DocumentoInfo(
    val archivo: String? = null,
    val titulo: String? = null,
    val tamano_mb: Double = 0.0
)
