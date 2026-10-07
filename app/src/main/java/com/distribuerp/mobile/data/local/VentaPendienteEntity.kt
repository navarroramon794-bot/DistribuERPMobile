package com.distribuerp.mobile.data.local

import androidx.room.Entity
import androidx.room.Index
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

/**
 * Los indices declarados aqui DEBEN coincidir con los `CREATE INDEX` de
 * `MigracionesRoom.MIGRACION_3_4`.
 *
 * Room valida el esquema tras migrar comparando columnas e indices. Si la
 * migracion crea un indice que la entidad no declara, Room aborta la apertura con
 * `IllegalStateException: Migration didn't properly handle:
 * ventas_pendientes(...)` y la app no arranca en ninguna instalacion que ya
 * tenga la base en version 3.
 *
 * `empresaId` indexa las consultas por tenant y `clientOperationId` las de
 * idempotencia; ambos son parte del contrato de OFFLINE-2B.
 *
 * Los `name` se escriben a proposito y NO usan el nombre por defecto de Room
 * (`index_ventas_pendientes_clientOperationId`), porque `MIGRACION_3_4` los crea
 * con otro nombre: `index_ventas_pendientes_clienteOperationId`. Declararlos
 * asi mantiene entidad y migracion exactamente iguales, y hace que una base
 * recien creada y una base migrada queden con el mismo esquema.
 */
@Entity(
    tableName = "ventas_pendientes",
    indices = [
        Index(value = ["empresaId"], name = "index_ventas_pendientes_empresaId"),
        Index(
            value = ["clientOperationId"],
            name = "index_ventas_pendientes_clienteOperationId"
        )
    ]
)
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
