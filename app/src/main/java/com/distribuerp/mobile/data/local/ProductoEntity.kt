package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "productos",
    primaryKeys = ["empresa_id", "id"],
    indices = [
        Index(value = ["empresa_id", "codigo"], unique = true),
        Index(value = ["empresa_id", "codigo_barras"]),
        Index(value = ["empresa_id", "nombre"])
    ]
)
data class ProductoEntity(
    val empresa_id: Int,
    val id: Int,
    val codigo: String,
    val nombre: String,
    val descripcion: String? = null,
    val costo: Double = 0.0,
    val precio: Double = 0.0,
    val existencia: Double = 0.0,
    val codigo_barras: String? = null,
    val tipo_codigo: String? = null,
    val unidad_venta: String = "kg",
    val activo: Boolean = true,
    val fecha_creacion: String? = null
)