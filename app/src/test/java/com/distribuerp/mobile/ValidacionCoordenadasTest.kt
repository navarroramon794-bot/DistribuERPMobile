package com.distribuerp.mobile

import com.distribuerp.mobile.ui.map.esCoordenadaValida
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionCoordenadasTest {

    @Test
    fun coordenadaValidaEsAceptada() {
        assertTrue(esCoordenadaValida(-34.603722, -58.381592))
        assertTrue(esCoordenadaValida(-90.0, -180.0))
        assertTrue(esCoordenadaValida(90.0, 180.0))
    }

    @Test
    fun latitudFueraDeRangoEsInvalida() {
        assertFalse(esCoordenadaValida(91.0, 0.0))
        assertFalse(esCoordenadaValida(-91.0, 0.0))
    }

    @Test
    fun longitudFueraDeRangoEsInvalida() {
        assertFalse(esCoordenadaValida(0.0, 181.0))
        assertFalse(esCoordenadaValida(0.0, -181.0))
    }

    @Test
    fun origenCeroCeroEsInvalido() {
        assertFalse(
            "El origen (0,0) del parseador GSON no debe dibujarse como marcador",
            esCoordenadaValida(0.0, 0.0)
        )
    }
}
