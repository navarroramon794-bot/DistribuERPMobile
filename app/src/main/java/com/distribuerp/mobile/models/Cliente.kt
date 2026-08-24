package com.distribuerp.mobile.models

data class Cliente(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val limite_credito: Double,
    val activo: Boolean,
    val fecha_creacion: String?,
    val credito_autorizado: Boolean = false,
    val dias_credito: Int = 0,
    val bloqueado: Boolean = false
)

data class ClientesResponse(
    val ok: Boolean,
    val clientes: List<Cliente>?
)

data class ClienteResponse(
    val ok: Boolean,
    val mensaje: String?,
    val cliente: Cliente?
)

data class ClienteRequest(
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val limite_credito: Double
)

data class MensajeResponse(
    val ok: Boolean,
    val mensaje: String?
)
