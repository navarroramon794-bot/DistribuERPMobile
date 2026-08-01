package com.distribuerp.mobile.models

data class PagoRequest(
    val venta_id: Int,
    val monto: Double
)

data class Pago(
    val id: Int,
    val venta_id: Int,
    val fecha: String?,
    val monto: Double
)

data class PagoResponse(
    val ok: Boolean,
    val mensaje: String?,
    val pago: Pago?
)

data class PagosResponse(
    val ok: Boolean,
    val pagos: List<Pago>?
)
