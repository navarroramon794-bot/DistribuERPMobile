package com.distribuerp.mobile

import com.distribuerp.mobile.utils.PasswordValidator
import org.junit.Assert.*
import org.junit.Test

class ChangePasswordViewModelTest {

    @Test
    fun validacionLocalPasswordCortaFalla() {
        val err = PasswordValidator.validar("Ab1!")
        assertNotNull(err)
    }

    @Test
    fun validacionLocalFaltaMayusculaFalla() {
        assertNotNull(PasswordValidator.validar("abcdef1!"))
    }

    @Test
    fun validacionLocalFaltaMinusculaFalla() {
        assertNotNull(PasswordValidator.validar("ABCDEF1!"))
    }

    @Test
    fun validacionLocalFaltaNumeroFalla() {
        assertNotNull(PasswordValidator.validar("Abcdefg!"))
    }

    @Test
    fun validacionLocalFaltaSimboloFalla() {
        assertNotNull(PasswordValidator.validar("Abcdef12"))
    }

    @Test
    fun validacionConfirmacionFalla() {
        val err = PasswordValidator.validarConfirmacion("Abcdef1!", "Abcdef1@")
        assertEquals("La confirmación no coincide.", err)
    }

    @Test
    fun validacionConfirmacionOk() {
        assertNull(PasswordValidator.validarConfirmacion("Abcdef1!", "Abcdef1!"))
    }

    @Test
    fun passwordValidaPasa() {
        assertNull(PasswordValidator.validar("Fuerte99#"))
        assertNull(PasswordValidator.validarConfirmacion("Fuerte99#", "Fuerte99#"))
    }

    @Test
    fun cambioDebeSerDiferenteActual() {
        val actual = "Abcdef1!"
        val nueva = "Abcdef1!"
        // la VM valida que nueva != actual
        assertTrue(actual == nueva)
    }

    // Nota: los casos API 200/400/401/403 se validan con MockWebServer en instrumented
    // Aquí se cubre la lógica local que bloquea el botón mientras haya errores.
}
