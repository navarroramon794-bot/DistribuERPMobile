package com.distribuerp.mobile.models

data class Ubicacion(
    val vendedor_id: Int?,
    val vendedor: String?,
    val latitud: Double,
    val longitud: Double,
    val precision_m: Double?,
    val velocidad: Double?,
    val fuente: String?,
    val fecha: String?,
    val activo: Boolean = true
)

data class UbicacionRequest(
    val latitud: Double,
    val longitud: Double,
    val precision_m: Double? = null,
    val velocidad: Double? = null,
    val fuente: String = "gps",
    val fecha: String? = null
)

data class UbicacionResponse(
    val ok: Boolean,
    val mensaje: String?,
    val ubicacion: Ubicacion?
)

data class UbicacionesResponse(
    val ok: Boolean,
    val mensaje: String?,
    val ubicaciones: List<Ubicacion>?
)
