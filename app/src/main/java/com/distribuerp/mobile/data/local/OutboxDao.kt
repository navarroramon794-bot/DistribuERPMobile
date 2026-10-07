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

    /**
     * Operaciones elegibles para envio: las que estan PENDIENTE y ya pueden
     * reintentarse (`enviar_despues` vencido o cero).
     *
     * Una operacion con error recuperable vuelve a PENDIENTE, de modo que
     * reaparece aqui en el siguiente sync.
     */
    @Query("SELECT * FROM outbox WHERE estado = :estado AND enviar_despues <= :ahora ORDER BY creado_en ASC")
    suspend fun obtenerElegibles(estado: String, ahora: Long): List<OutboxOperation>

    /**
     * Devuelve la operacion a PENDIENTE para un proximo intento, conservando
     * el motivo del fallo y el contador de intentos.
     */
    @Query("UPDATE outbox SET estado = :estado, ultimo_error = :ultimoError, enviar_despues = :enviarDespues WHERE uuid = :uuid")
    suspend fun registrarRetry(
        uuid: String,
        estado: String = EstadoOutbox.PENDIENTE.valor,
        ultimoError: String? = null,
        enviarDespues: Long = 0L
    )

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun contar(): Int

    @Query("SELECT COUNT(*) FROM outbox WHERE estado = :estado")
    suspend fun contarPorEstado(estado: String): Int

    @Query("UPDATE outbox SET estado = :estado, ultimo_error = :ultimoError WHERE uuid = :uuid")
    suspend fun actualizarEstado(uuid: String, estado: String, ultimoError: String? = null)

    /**
     * Registra un nuevo intento de envio: incrementa `intentos`, limpia el error
     * previo y sella el lease del intento en `enviar_despues`.
     *
     * El lease es la garantia de que una operacion ENVIANDOSE no se recupere
     * mientras su ejecucion sigue viva. `SyncGeneralWorker` pasa
     * `ahora + LEASE_INTENTO_OUTBOX_MS`.
     */
    @Query("UPDATE outbox SET estado = :estado, intentos = intentos + 1, ultimo_error = NULL, enviar_despues = :enviarDespues WHERE uuid = :uuid")
    suspend fun registrarIntento(
        uuid: String,
        estado: String = EstadoOutbox.ENVIANDO.valor,
        enviarDespues: Long = 0L
    )

    /**
 * Recupera operaciones abandonadas en [EstadoOutbox.ENVIANDO].
 *
 * Solo toca filas cuyo lease ya vencio (`enviar_despues <= ahora`), es decir,
 * Whose intento empezo en una ejecucion que ya no existe porque el proceso murio
 * o fue interrumpido. Devuelve el numero de filas liberadas.
 *
 * No se intenta distinguir "proceso muerto" de "envio en vuelo" con un reloj de
 * pared: el lease es la unica prueba disponible y por eso `registrarIntento` lo
 * sella siempre. Una fila con `enviar_despues = 0` (ENVIANDO escrito antes de
 * que existiera el lease) se recupera de inmediato, que es justo el caso
 * atascado que se quiere desatascar.
 *
 * `enviar_despues` se pone a 0 para que la operacion sea elegible de inmediato
 * en el mismo ciclo. `intentos` y `ultimo_error` se conservan como traza.
 */
@Query("UPDATE outbox SET estado = :estadoRecuperada, enviar_despues = 0 WHERE estado = :estadoEnviando AND enviar_despues <= :ahora")
    suspend fun recuperarEnviandoAntiguos(
        ahora: Long,
        estadoEnviando: String = EstadoOutbox.ENVIANDO.valor,
        estadoRecuperada: String = EstadoOutbox.PENDIENTE.valor
    ): Int

    /**
     * Guarda el resultado del intento en curso sin alterar el contador de
     * intentos, y libera el lease.
     *
     * El lease solo debe existir mientras la operacion esta ENVIANDOSE. Al
     * terminar el intento la fila vuelve a PENDIENTE (o ERROR) y debe quedar
     * elegible de inmediato: el ritmo entre reintentos lo gobierna el backoff de
     * WorkManager, no `enviar_despues`. Si aqui se dejara el lease, una operacion
     * con error recuperable quedaria PENDIENTE pero no seria elegible hasta que
     * venciera el lease, y se perderia el reintento.
     */
    @Query("UPDATE outbox SET estado = :estado, ultimo_error = :ultimoError, enviar_despues = 0 WHERE uuid = :uuid")
    suspend fun registrarResultado(uuid: String, estado: String, ultimoError: String? = null)

    @Query("DELETE FROM outbox WHERE uuid = :uuid")
    suspend fun eliminarPorUuid(uuid: String)
}