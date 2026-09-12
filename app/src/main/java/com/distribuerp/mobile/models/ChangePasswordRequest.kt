package com.distribuerp.mobile.models

data class ChangePasswordRequest(
    val password_actual: String,
    val password_nueva: String,
    val password_nueva_confirmacion: String
)
