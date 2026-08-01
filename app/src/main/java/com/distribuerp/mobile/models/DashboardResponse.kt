package com.distribuerp.mobile.models

data class DashboardResponse(
    val ok: Boolean,
    val mensaje: String?,
    val datos: DashboardDatos?
)

data class DashboardDatos(
    val ventas_hoy: Double,
    val cobrado_hoy: Double,
    val saldo_pendiente: Double,
    val total_clientes: Int,
    val inventario_total: Double,
    val total_vendedores: Int
)
