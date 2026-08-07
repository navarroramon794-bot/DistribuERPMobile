package com.distribuerp.mobile.models

data class ItemInventario(
    val producto_id: Int,
    val codigo: String,
    val nombre: String,
    val precio: Double,
    val unidad_venta: String = "kg",
    val cargado: Double,
    val vendido: Double,
    val disponible: Double
)

data class InventarioResponse(
    val ok: Boolean,
    val vendedor: Vendedor?,
    val inventario: List<ItemInventario>?,
    val mensaje: String?
)
