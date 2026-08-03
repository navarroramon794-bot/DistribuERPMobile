package com.distribuerp.mobile.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

fun millisDeFecha(fecha: LocalDate): Long =
    fecha.atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

fun fechaDeMillis(millis: Long?): String? {
    if (millis == null) {
        return null
    }

    return Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
}

fun fechaCorta(millis: Long?): String {
    if (millis == null) {
        return ""
    }

    val patron = DateTimeFormatterBuilder()
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral("/")
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral("/")
        .appendValue(
            ChronoField.YEAR,
            4,
            4,
            SignStyle.NOT_NEGATIVE
        )
        .toFormatter()

    return Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(patron)
}
