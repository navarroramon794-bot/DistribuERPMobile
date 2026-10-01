package com.distribuerp.mobile.viewmodel

/**
 * Códigos HTTP que representan un fallo temporal de comunicación o de
 * infraestructura y NO un estado inválido de la licencia (HTTP 431,
 * 5xx, límites de servidor, etc.).
 */
internal val CODIGOS_ERROR_TRANSPORTE: Set<Int> =
    setOf(408, 429, 431, 500, 502, 503, 504)

internal fun esErrorTransitorio(codigo: Int): Boolean =
    codigo in CODIGOS_ERROR_TRANSPORTE

/**
 * Decisión a tomar tras un fallo de verificación en línea (error
 * transitorio o sin conexión).
 *
 * Garantiza que la app NUNCA redirige a la pantalla de activación por
 * un fallo temporal: o bien usa la licencia guardada cuando está dentro
 * del período de gracia existente, o informa de falta de conexión. En
 * ningún caso borra ni sobrescribe la licencia local ni fuerza a
 * reintroducirla.
 */
internal enum class MedidaTrasFallo {
    UsarGracia,
    SinInternet
}

internal fun medidaTrasFalloDeVerificacion(
    dentroDePeriodoGracia: Boolean
): MedidaTrasFallo =
    if (dentroDePeriodoGracia) {
        MedidaTrasFallo.UsarGracia
    } else {
        MedidaTrasFallo.SinInternet
    }
