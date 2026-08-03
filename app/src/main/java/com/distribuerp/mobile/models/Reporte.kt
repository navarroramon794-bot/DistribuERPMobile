package com.distribuerp.mobile.models

enum class TipoReporte(
    val clave: String,
    val titulo: String,
    val nombreBase: String,
    val requiereFechas: Boolean
) {
    VENTAS(
        clave = "ventas",
        titulo = "Historial de Ventas",
        nombreBase = "Reporte_Ventas",
        requiereFechas = true
    ),
    PAGOS(
        clave = "pagos",
        titulo = "Historial de Cobranza",
        nombreBase = "Reporte_Cobranza",
        requiereFechas = true
    ),
    PRODUCTOS(
        clave = "productos",
        titulo = "Productos más vendidos",
        nombreBase = "Reporte_Productos",
        requiereFechas = true
    ),
    CLIENTES(
        clave = "clientes",
        titulo = "Clientes con saldo pendiente",
        nombreBase = "Reporte_Clientes_Saldos",
        requiereFechas = false
    );

    companion object {
        fun desdeClave(clave: String): TipoReporte =
            entries.firstOrNull {
                it.clave == clave
            } ?: VENTAS
    }
}

data class ReporteVentasResponse(
    val ok: Boolean,
    val mensaje: String?,
    val ventas: List<Venta>?,
    val total_ventas: Int?,
    val total_monto: Double?
)

data class ReportePagosResponse(
    val ok: Boolean,
    val mensaje: String?,
    val pagos: List<Pago>?,
    val total_monto: Double?
)

data class ProductoMasVendido(
    val producto_id: Int,
    val producto: String,
    val cantidad: Double,
    val monto: Double
)

data class ReporteProductosResponse(
    val ok: Boolean,
    val mensaje: String?,
    val productos: List<ProductoMasVendido>?,
    val total_monto: Double?
)

data class ClienteSaldo(
    val cliente_id: Int,
    val cliente: String,
    val saldo: Double
)

data class ReporteClientesSaldoResponse(
    val ok: Boolean,
    val mensaje: String?,
    val clientes: List<ClienteSaldo>?,
    val total_saldo: Double?,
    val total_clientes: Int?
)
