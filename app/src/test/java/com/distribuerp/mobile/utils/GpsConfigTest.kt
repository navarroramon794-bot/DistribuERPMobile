package com.distribuerp.mobile.utils

import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsConfigTest {

    @Test
    fun min_displacement_es_cero() {
        assertEquals(0f, GpsConfig.MIN_DISPLACEMENT_M, 0.001f)
    }

    @Test
    fun prioridad_es_high_accuracy() {
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, GpsConfig.PRIORITY)
    }

    @Test
    fun intervalo_foreground_sigue_en_60s() {
        assertEquals(60_000L, GpsConfig.FOREGROUND_INTERVAL_MS)
    }

    @Test
    fun fastest_foreground_sigue_en_30s() {
        assertEquals(30_000L, GpsConfig.FASTEST_INTERVAL_FG_MS)
    }

    @Test
    fun intervalo_background_sigue_en_5min() {
        assertEquals(5 * 60_000L, GpsConfig.BACKGROUND_INTERVAL_MS)
    }

    @Test
    fun fastest_background_sigue_en_2min() {
        assertEquals(2 * 60_000L, GpsConfig.FASTEST_INTERVAL_BG_MS)
    }

    @Test
    fun intervalo_estacionario_es_15min() {
        assertEquals(15 * 60_000L, GpsConfig.STATIONARY_INTERVAL_MS)
    }

    @Test
    fun el_request_del_servicio_no_usa_filtro_de_50m() {
        assertEquals(0f, GpsConfig.MIN_DISPLACEMENT_M, 0.001f)
        assertEquals(false, GpsConfig.MIN_DISPLACEMENT_M >= 50f)
    }
}