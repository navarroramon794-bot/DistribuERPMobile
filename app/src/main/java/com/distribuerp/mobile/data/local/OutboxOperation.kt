package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EstadoOutbox(val valor: String) {
    PENDIENTE("PENDIENTE"),
    ENVIANDO("ENVIANDO"),
    OK("OK"),
    ERROR("ERROR");

    companion object {
        fun desde(valor: String): EstadoOutbox =
            entries.firstOrNull { it.valor == valor } ?: PENDIENTE
    }
}

@Entity(tableName = "outbox")
data class OutboxOperation(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val tipo: String,
    val payload: String,
    val estado: String = EstadoOutbox.PENDIENTE.valor,
    val intentos: Int = 0,
    val ultimo_error: String? = null,
    val creado_en: Long = System.currentTimeMillis(),
    val enviar_despues: Long = 0L
) {
    val estadoActual: EstadoOutbox get() = EstadoOutbox.desde(estado)
}
