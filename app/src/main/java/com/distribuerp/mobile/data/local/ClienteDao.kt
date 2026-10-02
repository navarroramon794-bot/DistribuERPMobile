package com.distribuerp.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes WHERE empresa_id = :empresaId ORDER BY nombre ASC")
    suspend fun obtenerTodos(empresaId: Int): List<ClienteEntity>

    @Query("SELECT * FROM clientes WHERE empresa_id = :empresaId ORDER BY nombre ASC")
    fun obtenerTodosFlow(empresaId: Int): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE empresa_id = :empresaId AND id = :id")
    suspend fun obtenerPorId(empresaId: Int, id: Int): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE empresa_id = :empresaId AND (nombre LIKE '%' || :texto || '%' OR telefono LIKE '%' || :texto || '%' OR direccion LIKE '%' || :texto || '%') ORDER BY nombre ASC")
    suspend fun buscar(empresaId: Int, texto: String): List<ClienteEntity>

    @Query("SELECT COUNT(*) FROM clientes WHERE empresa_id = :empresaId")
    suspend fun contar(empresaId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cliente: ClienteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(clientes: List<ClienteEntity>)

    @Query("DELETE FROM clientes WHERE empresa_id = :empresaId")
    suspend fun eliminarTodoTenant(empresaId: Int)
}
