package com.distribuerp.mobile.models

data class ItemCompraRequest(
    val producto_id: Int,
    val cantidad: Double
)

data class CompraRequest(
    val proveedor_id: Int,
    val observaciones: String? = null,
    val productos: List<ItemCompraRequest>
)

data class ItemCompra(
    val producto_id: Int,
    val codigo: String,
    val producto: String,
    val cantidad: Double,
    val precio: Double,
    val subtotal: Double
)

data class Compra(
    val id: Int,
    val folio: String,
    val fecha: String?,
    val proveedor_id: Int,
    val proveedor: String?,
    val observaciones: String?,
    val total_productos: Int,
    val total_cantidad: Double,
    val total: Double,
    val items: List<ItemCompra>
)

data class CompraResponse(
    val ok: Boolean,
    val mensaje: String?,
    val compra: Compra?
)

data class ComprasResponse(
    val ok: Boolean,
    val compras: List<Compra>?
)
