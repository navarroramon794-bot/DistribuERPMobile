package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ubicaciones_pendientes")
data class UbicacionPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vendedorId: Int,
    val latitud: Double,
    val longitud: Double,
    val precisionM: Double?,
    val velocidad: Double?,
    val fuente: String,
    val fecha: String,
    val timestampMs: Long = System.currentTimeMillis()
)
