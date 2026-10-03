package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EstadoVentaSincronizacion(val valor: String) {
    PENDIENTE_SYNC("PENDIENTE_SYNC"),
    ENVIANDO("ENVIANDO"),
    SINCRONIZADA("SINCRONIZADA"),
    ERROR_RECUPERABLE("ERROR_RECUPERABLE"),
    ERROR_DEFINITIVO("ERROR_DEFINITIVO");

    companion object {
        fun desde(valor: String): EstadoVentaSincronizacion =
            entries.firstOrNull { it.valor == valor } ?: PENDIENTE_SYNC
    }
}

@Entity(tableName = "ventas_pendientes")
data class VentaPendienteEntity(
    @PrimaryKey
    val id: String,
    val clientOperationId: String,
    val empresaId: Int,
    val vendedorId: Int,
    val clienteId: Int,
    val formaPago: String,
    val fecha: Long,
    val subtotal: Double,
    val total: Double,
    val estadoLocal: String,
    val folioBackend: String? = null,
    val ventaIdBackend: Int? = null,
    val error: String? = null,
    val intentos: Int = 0,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val estadoActual: EstadoVentaSincronizacion get() = EstadoVentaSincronizacion.desde(estadoLocal)
}
