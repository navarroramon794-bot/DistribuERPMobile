package com.distribuerp.mobile.models

data class ItemCargaRequest(
    val producto_id: Int,
    val cantidad: Double
)

data class CargaRequest(
    val vendedor_id: Int,
    val observaciones: String? = null,
    val productos: List<ItemCargaRequest>
)

data class ItemCarga(
    val producto_id: Int,
    val codigo: String,
    val producto: String,
    val cantidad: Double,
    val unidad: String = ""
)

data class Carga(
    val id: Int,
    val folio: String,
    val fecha: String?,
    val vendedor_id: Int,
    val vendedor: String?,
    val observaciones: String?,
    val total_productos: Int,
    val total_cantidad: Double,
    val items: List<ItemCarga>
)

data class CargaResponse(
    val ok: Boolean,
    val mensaje: String?,
    val carga: Carga?
)

data class CargasResponse(
    val ok: Boolean,
    val cargas: List<Carga>?
)
