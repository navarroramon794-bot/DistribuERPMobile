package com.distribuerp.mobile

import com.distribuerp.mobile.models.LoginResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class LoginResponseTest {

    private val gson = Gson()

    @Test
    fun parseConPasswordTemporalTrue() {
        val json = """{"ok":true,"mensaje":"ok","usuario":{"id":1,"nombre":"V","correo":"v@test","rol":"Vendedor","password_temporal":true}}"""
        val resp = gson.fromJson(json, LoginResponse::class.java)
        assertTrue(resp.ok)
        assertEquals(true, resp.usuario?.password_temporal)
    }

    @Test
    fun parseConPasswordTemporalFalse() {
        val json = """{"ok":true,"usuario":{"id":1,"nombre":"V","correo":"v@test","rol":"Vendedor","password_temporal":false}}"""
        val resp = gson.fromJson(json, LoginResponse::class.java)
        assertEquals(false, resp.usuario?.password_temporal)
    }

    @Test
    fun parseSinPasswordTemporalEsNull() {
        val json = """{"ok":true,"usuario":{"id":1,"nombre":"A","correo":"a@test","rol":"Administrador"}}"""
        val resp = gson.fromJson(json, LoginResponse::class.java)
        assertNull(resp.usuario?.password_temporal)
    }

    @Test
    fun parseUsuarioNormalVaADashboard() {
        val json = """{"ok":true,"usuario":{"id":2,"nombre":"A","correo":"a@test","rol":"Administrador","password_temporal":false}}"""
        val resp = gson.fromJson(json, LoginResponse::class.java)
        assertFalse(resp.usuario?.password_temporal == true)
    }

    @Test
    fun parseTemporalTrueRequiereCambio() {
        val json = """{"ok":true,"usuario":{"id":3,"nombre":"V","correo":"v@test","rol":"Vendedor","password_temporal":true}}"""
        val resp = gson.fromJson(json, LoginResponse::class.java)
        assertTrue(resp.usuario?.password_temporal == true)
    }
}
