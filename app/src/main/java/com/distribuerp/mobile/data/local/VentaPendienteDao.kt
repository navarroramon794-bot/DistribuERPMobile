package com.distribuerp.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class VentaPendienteCompleta(
    val venta: VentaPendienteEntity,
    val detalles: List<DetalleVentaPendienteEntity>
)

@Dao
interface VentaPendienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVenta(venta: VentaPendienteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDetalles(detalles: List<DetalleVentaPendienteEntity>)

    @Transaction
    suspend fun insertarVentaCompleta(venta: VentaPendienteEntity, detalles: List<DetalleVentaPendienteEntity>) {
        insertarVenta(venta)
        insertarDetalles(detalles)
    }

    @Query("SELECT * FROM ventas_pendientes WHERE id = :id")
    suspend fun obtenerVentaPorId(id: String): VentaPendienteEntity?

    @Query("SELECT * FROM detalles_ventas_pendientes WHERE ventaId = :ventaId")
    suspend fun obtenerDetallesPorVentaId(ventaId: String): List<DetalleVentaPendienteEntity>

    @Transaction
    suspend fun obtenerVentaCompleta(id: String): VentaPendienteCompleta? {
        val venta = obtenerVentaPorId(id) ?: return null
        val detalles = obtenerDetallesPorVentaId(id)
        return VentaPendienteCompleta(venta, detalles)
    }

    @Query("SELECT * FROM ventas_pendientes WHERE empresaId = :empresaId AND clienteOperationId = :clientOpId LIMIT 1")
    suspend fun obtenerPorClientOperationId(empresaId: Int, clientOpId: String): VentaPendienteEntity?

    @Query("SELECT * FROM ventas_pendientes WHERE empresaId = :empresaId ORDER BY creadoEn DESC")
    suspend fun obtenerPorEmpresa(empresaId: Int): List<VentaPendienteEntity>

    @Query("UPDATE ventas_pendientes SET estadoLocal = :estado, intentos = intentos + 1, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarEnviando(id: String, estado: String = EstadoVentaSincronizacion.ENVIANDO.valor, ahora: Long = System.currentTimeMillis())

    @Query("UPDATE ventas_pendientes SET estadoLocal = :estado, error = :error, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarError(id: String, estado: String, error: String?, ahora: Long = System.currentTimeMillis())

    @Query("UPDATE ventas_pendientes SET estadoLocal = :estado, folioBackend = :folio, ventaIdBackend = :ventaId, error = NULL, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarSincronizada(id: String, folio: String?, ventaId: Int?, estado: String = EstadoVentaSincronizacion.SINCRONIZADA.valor, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM ventas_pendientes WHERE id = :id")
    suspend fun eliminar(id: String)
}
