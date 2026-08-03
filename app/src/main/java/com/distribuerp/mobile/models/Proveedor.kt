package com.distribuerp.mobile.models

data class Proveedor(
    val id: Int,
    val nombre: String,
    val rfc: String?,
    val telefono: String?,
    val direccion: String?,
    val activo: Boolean,
    val fecha_creacion: String?
)

data class ProveedoresResponse(
    val ok: Boolean,
    val proveedores: List<Proveedor>?
)

data class ProveedorResponse(
    val ok: Boolean,
    val mensaje: String?,
    val proveedor: Proveedor?
)

data class ProveedorRequest(
    val nombre: String,
    val rfc: String?,
    val telefono: String?,
    val direccion: String?
)
