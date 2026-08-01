package com.distribuerp.mobile.models

data class Vendedor(
    val id: Int,
    val nombre: String,
    val telefono: String?,
    val activo: Boolean,
    val fecha_creacion: String?
)

data class VendedoresResponse(
    val ok: Boolean,
    val vendedores: List<Vendedor>?
)

data class VendedorResponse(
    val ok: Boolean,
    val mensaje: String?,
    val vendedor: Vendedor?
)

data class VendedorRequest(
    val nombre: String,
    val telefono: String?
)
