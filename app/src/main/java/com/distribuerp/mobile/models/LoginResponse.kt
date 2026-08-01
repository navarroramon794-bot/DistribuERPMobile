package com.distribuerp.mobile.models

data class LoginResponse(
    val ok: Boolean,
    val mensaje: String?,
    val usuario: Usuario?
)

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String?
)