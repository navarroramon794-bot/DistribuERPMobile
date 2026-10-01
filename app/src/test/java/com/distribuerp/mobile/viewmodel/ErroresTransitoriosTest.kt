package com.distribuerp.mobile.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErroresTransitoriosTest {

    @Test
    fun erroresDeTransporteSeClasificanComoTransitorios() {
        for (codigo in listOf(408, 429, 431, 500, 502, 503, 504)) {
            assertTrue(
                "Código $codigo debería ser transitorio",
                esErrorTransitorio(codigo)
            )
        }
    }

    @Test
    fun codigosValidosORespuestasDefinitivasNoSonTransitorios() {
        for (codigo in listOf(200, 201, 400, 401, 403, 404, 409, 422, 451)) {
            assertFalse(
                "Código $codigo no debería ser transitorio",
                esErrorTransitorio(codigo)
            )
        }
    }

    @Test
    fun laLicenciaQuedaDentroDeLaListaDeCodigos() {
        assertTrue(431 in CODIGOS_ERROR_TRANSPORTE)
    }

    @Test
    fun errorTransitorioDentroDeGraciaNuncaFuerzaReactivacion() {
        val falloTransitorio = esErrorTransitorio(431)

        assertTrue(falloTransitorio)

        assertEquals(
            MedidaTrasFallo.UsarGracia,
            medidaTrasFalloDeVerificacion(dentroDePeriodoGracia = true)
        )
    }

    @Test
    fun errorTransitorioFueraDeGraciaNoBorraNiObligaAReactivar() {
        assertTrue(esErrorTransitorio(500))
        assertTrue(esErrorTransitorio(503))
        assertTrue(esErrorTransitorio(504))

        assertEquals(
            MedidaTrasFallo.SinInternet,
            medidaTrasFalloDeVerificacion(dentroDePeriodoGracia = false)
        )
    }

    @Test
    fun codigosNoTransitoriosMantienenElCaminoDeError() {
        assertFalse(esErrorTransitorio(400))
        assertFalse(esErrorTransitorio(422))
    }
}
