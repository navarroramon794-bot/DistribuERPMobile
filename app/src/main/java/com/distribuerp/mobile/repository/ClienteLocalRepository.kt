package com.distribuerp.mobile.repository

import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.ClienteEntity
import com.distribuerp.mobile.models.Cliente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteLocalRepository(
    private val db: AppDatabase
) {
    suspend fun sincronizar(empresaId: Int, clientes: List<Cliente>) = withContext(Dispatchers.IO) {
        if (clientes.isEmpty()) return@withContext
        val entidades = clientes.map { it.toEntity(empresaId) }
        db.clienteDao().eliminarTodoTenant(empresaId)
        db.clienteDao().insertarTodos(entidades)
    }

    suspend fun obtenerTodos(empresaId: Int): List<Cliente> = withContext(Dispatchers.IO) {
        db.clienteDao().obtenerTodos(empresaId).map { it.toModel() }
    }

    suspend fun buscar(empresaId: Int, texto: String): List<Cliente> = withContext(Dispatchers.IO) {
        db.clienteDao().buscar(empresaId, texto).map { it.toModel() }
    }

    private fun Cliente.toEntity(empresaId: Int) = ClienteEntity(
        empresa_id = empresaId,
        id = id,
        nombre = nombre,
        direccion = direccion,
        telefono = telefono,
        limite_credito = limite_credito,
        activo = activo,
        fecha_creacion = fecha_creacion,
        credito_autorizado = credito_autorizado,
        dias_credito = dias_credito,
        bloqueado = bloqueado
    )

    private fun ClienteEntity.toModel() = Cliente(
        id = id,
        nombre = nombre,
        direccion = direccion,
        telefono = telefono,
        limite_credito = limite_credito,
        activo = activo,
        fecha_creacion = fecha_creacion,
        credito_autorizado = credito_autorizado,
        dias_credito = dias_credito,
        bloqueado = bloqueado
    )
}
