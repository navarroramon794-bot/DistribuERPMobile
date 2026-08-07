package com.distribuerp.mobile.ui.components

import java.util.Locale

fun formatearDinero(valor: Double): String =
    String.format(Locale.US, "$%,.2f", valor)

fun formatearCantidad(valor: Double): String =
    String.format(Locale.US, "%,.2f", valor)

fun formatearNumero(valor: Double): String =
    formatearCantidad(valor)
