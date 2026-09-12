package com.distribuerp.mobile.utils

object PasswordValidator {

    private const val SIMBOLOS = "!@#$%&*"

    fun validar(password: String): String? {
        if (password.length < 8) {
            return "La contraseña debe tener al menos 8 caracteres."
        }
        if (!password.any { it.isUpperCase() }) {
            return "La contraseña debe incluir una mayúscula."
        }
        if (!password.any { it.isLowerCase() }) {
            return "La contraseña debe incluir una minúscula."
        }
        if (!password.any { it.isDigit() }) {
            return "La contraseña debe incluir un número."
        }
        if (!password.any { it in SIMBOLOS }) {
            return "La contraseña debe incluir un símbolo (!@#\$%&*)."
        }
        return null
    }

    fun validarConfirmacion(nueva: String, confirmacion: String): String? {
        if (nueva != confirmacion) {
            return "La confirmación no coincide."
        }
        return null
    }
}
