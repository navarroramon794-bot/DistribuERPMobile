package com.distribuerp.mobile.models

data class EmpresaResponse(
    val ok: Boolean,
    val datos: EmpresaDatos
)

data class EmpresaDatos(
    val nombre: String,
    val razon_social: String?,
    val rfc: String?,
    val eslogan: String?,
    val direccion: String?,
    val colonia: String?,
    val ciudad: String?,
    val estado: String?,
    val codigo_postal: String?,
    val telefono: String?,
    val whatsapp: String?,
    val correo: String?,
    val sitio_web: String?
)