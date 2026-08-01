package com.distribuerp.mobile.models

data class Producto(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val descripcion: String?,
    val costo: Double,
    val precio: Double,
    val existencia: Double,
    val activo: Boolean,
    val fecha_creacion: String?
)

data class ProductosResponse(
    val ok: Boolean,
    val productos: List<Producto>?
)

data class ProductoResponse(
    val ok: Boolean,
    val mensaje: String?,
    val producto: Producto?
)

data class ProductoRequest(
    val codigo: String,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val existencia: Double
)
