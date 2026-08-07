package com.distribuerp.mobile.models

data class ItemVentaRequest(
    val producto_id: Int,
    val cantidad: Double
)

data class VentaRequest(
    val cliente_id: Int,
    val vendedor_id: Int,
    val productos: List<ItemVentaRequest>
)

data class ItemVenta(
    val producto_id: Int,
    val producto: String,
    val cantidad: Double,
    val unidad: String = "",
    val precio: Double,
    val subtotal: Double
)

data class Venta(
    val id: Int,
    val folio: String,
    val fecha: String?,
    val cliente_id: Int,
    val cliente: String,
    val vendedor_id: Int,
    val vendedor: String,
    val total: Double,
    val items: List<ItemVenta>,
    val pagado: Double = 0.0,
    val saldo: Double = 0.0
)

data class VentaResponse(
    val ok: Boolean,
    val mensaje: String?,
    val venta: Venta?
)

data class VentasResponse(
    val ok: Boolean,
    val ventas: List<Venta>?
)
