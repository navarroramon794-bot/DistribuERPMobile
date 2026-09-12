package com.distribuerp.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UbicacionPendienteDao {
    @Insert
    suspend fun insertar(entity: UbicacionPendienteEntity): Long

    @Query("SELECT * FROM ubicaciones_pendientes WHERE vendedorId = :vendedorId ORDER BY timestampMs DESC")
    suspend fun obtenerPorVendedor(vendedorId: Int): List<UbicacionPendienteEntity>

    @Query("SELECT * FROM ubicaciones_pendientes ORDER BY timestampMs ASC")
    suspend fun obtenerTodas(): List<UbicacionPendienteEntity>

    @Query("SELECT COUNT(*) FROM ubicaciones_pendientes WHERE vendedorId = :vendedorId")
    suspend fun contarPorVendedor(vendedorId: Int): Int

    @Query("DELETE FROM ubicaciones_pendientes WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("DELETE FROM ubicaciones_pendientes WHERE vendedorId = :vendedorId AND id NOT IN (SELECT id FROM ubicaciones_pendientes WHERE vendedorId = :vendedorId ORDER BY timestampMs DESC LIMIT 50)")
    suspend fun recortarA50(vendedorId: Int)

    @Query("DELETE FROM ubicaciones_pendientes WHERE vendedorId = :vendedorId")
    suspend fun eliminarPorVendedor(vendedorId: Int)
}
