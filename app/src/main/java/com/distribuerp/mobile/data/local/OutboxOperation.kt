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

/**
 * Duracion del lease de un intento en curso.
 *
 * Al marcar una operacion como [EstadoOutbox.ENVIANDO] se sella su
 * `enviar_despues` con `ahora + LEASE_INTENTO_OUTBOX_MS`. Ese instante es
 * "no recuperar ni reintentar este intento antes de aqui".
 *
 * Si el proceso muere con la operacion en ENVIANDO, la fila queda con el lease
 * ya sellado: cuando el lease vence, el reaper la devuelve a PENDIENTE. Un
 * ENVIANDO con el lease en el futuro pertenece a una ejecucion viva y no se
 * toca, de modo que dos ejecuciones nunca procesan la misma operacion.
 *
 * 5 minutos es un margen de sobra sobre la llamada HTTP mas lenta posible: los
 * timeouts de `RetrofitClient` son 15 s para connect, read y write, asi que el
 * peor caso de un envio son ~15 s. El margen evita recuperar una operacion que
 * sigue en vuelo, y a la vez no deja una venta atascada mas de 5 minutos.
 *
 * Es el unico lugar donde vive este numero: `SyncGeneralWorker` lo usa como
 * valor por defecto y los tests lo importan.
 */
const val LEASE_INTENTO_OUTBOX_MS: Long = 5 * 60 * 1000L

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
