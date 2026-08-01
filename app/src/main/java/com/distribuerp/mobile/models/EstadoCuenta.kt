package com.distribuerp.mobile.models

data class EstadoCuentaResponse(
    val ok: Boolean,
    val mensaje: String?,
    val cliente: Cliente?,
    val total_vendido: Double?,
    val total_cobrado: Double?,
    val saldo: Double?,
    val ventas: List<Venta>?
)
