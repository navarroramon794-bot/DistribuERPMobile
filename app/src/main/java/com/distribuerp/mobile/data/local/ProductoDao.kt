package com.distribuerp.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND activo = 1 ORDER BY nombre ASC")
    suspend fun obtenerTodosActivos(empresaId: Int): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND activo = 1 ORDER BY nombre ASC")
    fun obtenerTodosActivosFlow(empresaId: Int): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND id = :id")
    suspend fun obtenerPorId(empresaId: Int, id: Int): ProductoEntity?

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND codigo = :codigo")
    suspend fun obtenerPorCodigo(empresaId: Int, codigo: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND codigo_barras = :codigoBarras")
    suspend fun obtenerPorCodigoBarras(empresaId: Int, codigoBarras: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE empresa_id = :empresaId AND activo = 1 AND (codigo LIKE '%' || :texto || '%' OR nombre LIKE '%' || :texto || '%' OR codigo_barras LIKE '%' || :texto || '%') ORDER BY nombre ASC")
    suspend fun buscarActivos(empresaId: Int, texto: String): List<ProductoEntity>

    @Query("SELECT COUNT(*) FROM productos WHERE empresa_id = :empresaId")
    suspend fun contar(empresaId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(productos: List<ProductoEntity>)

    @Query("DELETE FROM productos WHERE empresa_id = :empresaId")
    suspend fun eliminarTodoTenant(empresaId: Int)
}
