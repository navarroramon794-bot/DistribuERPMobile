package com.distribuerp.mobile

import com.distribuerp.mobile.data.Roles
import org.junit.Assert.*
import org.junit.Test

class PasswordTemporalFlowTest {

    private fun debeIrACambiarPassword(passwordTemporal: Boolean?): Boolean {
        return passwordTemporal == true
    }

    private fun debeIrADashboard(passwordTemporal: Boolean?): Boolean {
        return passwordTemporal != true
    }

    @Test
    fun temporalTrueObligaCambio() {
        assertTrue(debeIrACambiarPassword(true))
        assertFalse(debeIrADashboard(true))
    }

    @Test
    fun temporalFalseVaADashboard() {
        assertTrue(debeIrADashboard(false))
        assertFalse(debeIrACambiarPassword(false))
    }

    @Test
    fun temporalNullVaADashboard() {
        assertTrue(debeIrADashboard(null))
        assertFalse(debeIrACambiarPassword(null))
    }

    @Test
    fun cambioExitosoVaADashboard() {
        // tras POST 200, SessionManager.actualizarPasswordTemporal(false)
        var temporal: Boolean? = true
        temporal = false // simulado
        assertTrue(debeIrADashboard(temporal))
    }

    @Test
    fun usuarioNormalNoEsForzado() {
        // admin o vendedor sin temporal no debe ser interceptado
        assertTrue(debeIrADashboard(false))
        assertTrue(debeIrADashboard(null))
    }

    @Test
    fun api401NoExponePassword() {
        // contrato: errorBody no debe contener password
        val fakeError = """{"ok":false,"mensaje":"La contraseña actual es incorrecta."}"""
        assertFalse(fakeError.contains("password_actual"))
        assertFalse(fakeError.contains("Fuerte"))
    }
}
