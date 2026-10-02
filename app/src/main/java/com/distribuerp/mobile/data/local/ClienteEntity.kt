package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clientes",
    primaryKeys = ["empresa_id", "id"],
    indices = [
        Index(value = ["empresa_id", "nombre"]),
        Index(value = ["empresa_id", "id"])
    ]
)
data class ClienteEntity(
    val empresa_id: Int,
    val id: Int,
    val nombre: String,
    val direccion: String? = null,
    val telefono: String? = null,
    val limite_credito: Double = 0.0,
    val activo: Boolean = true,
    val fecha_creacion: String? = null,
    val credito_autorizado: Boolean = false,
    val dias_credito: Int = 0,
    val bloqueado: Boolean = false
)