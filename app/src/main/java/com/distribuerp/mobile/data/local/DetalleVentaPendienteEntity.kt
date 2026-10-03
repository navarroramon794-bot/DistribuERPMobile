package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "detalles_ventas_pendientes",
    primaryKeys = ["ventaId", "productoId"],
    foreignKeys = [
        ForeignKey(
            entity = VentaPendienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ventaId"), Index("productoId")]
)
data class DetalleVentaPendienteEntity(
    val ventaId: String,
    val productoId: Int,
    val cantidad: Double,
    val precioUnitario: Double,
    val subtotal: Double
)
