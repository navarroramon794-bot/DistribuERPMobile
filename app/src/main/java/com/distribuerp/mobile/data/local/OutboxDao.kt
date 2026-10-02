package com.distribuerp.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(op: OutboxOperation): Long

    @Query("SELECT * FROM outbox WHERE uuid = :uuid")
    suspend fun obtenerPorUuid(uuid: String): OutboxOperation?

    @Query("SELECT * FROM outbox ORDER BY creado_en ASC")
    suspend fun obtenerTodas(): List<OutboxOperation>

    @Query("SELECT * FROM outbox WHERE estado = :estado ORDER BY creado_en ASC")
    suspend fun obtenerPorEstado(estado: String): List<OutboxOperation>

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun contar(): Int

    @Query("SELECT COUNT(*) FROM outbox WHERE estado = :estado")
    suspend fun contarPorEstado(estado: String): Int

    @Query("UPDATE outbox SET estado = :estado, ultimo_error = :ultimoError WHERE uuid = :uuid")
    suspend fun actualizarEstado(uuid: String, estado: String, ultimoError: String? = null)

    /** Registra un nuevo intento de envio: incrementa `intentos` y limpia el error previo. */
    @Query("UPDATE outbox SET estado = :estado, intentos = intentos + 1, ultimo_error = NULL WHERE uuid = :uuid")
    suspend fun registrarIntento(uuid: String, estado: String = EstadoOutbox.ENVIANDO.valor)

    /** Guarda el resultado del intento en curso sin alterar el contador de intentos. */
    @Query("UPDATE outbox SET estado = :estado, ultimo_error = :ultimoError WHERE uuid = :uuid")
    suspend fun registrarResultado(uuid: String, estado: String, ultimoError: String? = null)

    @Query("DELETE FROM outbox WHERE uuid = :uuid")
    suspend fun eliminarPorUuid(uuid: String)
}