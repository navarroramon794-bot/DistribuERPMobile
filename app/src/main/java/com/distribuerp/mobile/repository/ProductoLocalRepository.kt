package com.distribuerp.mobile.repository

import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.ProductoEntity
import com.distribuerp.mobile.models.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductoLocalRepository(
    private val db: AppDatabase
) {
    suspend fun sincronizar(empresaId: Int, productos: List<Producto>) = withContext(Dispatchers.IO) {
        if (productos.isEmpty()) return@withContext
        val entidades = productos.map { it.toEntity(empresaId) }
        db.productoDao().eliminarTodoTenant(empresaId)
        db.productoDao().insertarTodos(entidades)
    }

    suspend fun obtenerTodosActivos(empresaId: Int): List<Producto> = withContext(Dispatchers.IO) {
        db.productoDao().obtenerTodosActivos(empresaId).map { it.toModel() }
    }

    suspend fun buscarActivos(empresaId: Int, texto: String): List<Producto> = withContext(Dispatchers.IO) {
        db.productoDao().buscarActivos(empresaId, texto).map { it.toModel() }
    }

    private fun Producto.toEntity(empresaId: Int) = ProductoEntity(
        empresa_id = empresaId,
        id = id,
        codigo = codigo,
        nombre = nombre,
        descripcion = descripcion,
        costo = costo,
        precio = precio,
        existencia = existencia,
        codigo_barras = codigo_barras,
        tipo_codigo = tipo_codigo,
        unidad_venta = unidad_venta,
        activo = activo,
        fecha_creacion = fecha_creacion
    )

    private fun ProductoEntity.toModel() = Producto(
        id = id,
        codigo = codigo,
        nombre = nombre,
        descripcion = descripcion,
        costo = costo,
        precio = precio,
        existencia = existencia,
        codigo_barras = codigo_barras,
        tipo_codigo = tipo_codigo,
        unidad_venta = unidad_venta,
        activo = activo,
        fecha_creacion = fecha_creacion
    )
}
