package com.distribuerp.mobile

import com.distribuerp.mobile.utils.PasswordValidator
import org.junit.Assert.*
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun passwordValida() {
        assertNull(PasswordValidator.validar("Abcdef1!"))
        assertNull(PasswordValidator.validar("Fuerte99#X"))
    }

    @Test
    fun passwordDemasiadoCorta() {
        assertNotNull(PasswordValidator.validar("Ab1!"))
        assertEquals("La contraseña debe tener al menos 8 caracteres.", PasswordValidator.validar("Ab1!"))
    }

    @Test
    fun faltaMayuscula() {
        assertNotNull(PasswordValidator.validar("abcdef1!"))
    }

    @Test
    fun faltaMinuscula() {
        assertNotNull(PasswordValidator.validar("ABCDEF1!"))
    }

    @Test
    fun faltaNumero() {
        assertNotNull(PasswordValidator.validar("Abcdefg!"))
    }

    @Test
    fun faltaSimbolo() {
        assertNotNull(PasswordValidator.validar("Abcdef12"))
    }

    @Test
    fun confirmacionDiferente() {
        assertNotNull(PasswordValidator.validarConfirmacion("Abcdef1!", "Abcdef1@"))
        assertNull(PasswordValidator.validarConfirmacion("Abcdef1!", "Abcdef1!"))
    }

    @Test
    fun simboloPermitido() {
        for (s in listOf("!", "@", "#", "$", "%", "&", "*")) {
            assertNull(PasswordValidator.validar("Abcdef1$s"))
        }
    }
}
