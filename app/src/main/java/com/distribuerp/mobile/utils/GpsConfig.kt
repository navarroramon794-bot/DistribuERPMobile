package com.distribuerp.mobile.utils

import com.google.android.gms.location.Priority

object GpsConfig {
    const val FOREGROUND_INTERVAL_MS = 60_000L
    const val BACKGROUND_INTERVAL_MS = 5 * 60_000L
    const val FASTEST_INTERVAL_FG_MS = 30_000L
    const val FASTEST_INTERVAL_BG_MS = 2 * 60_000L
    const val MIN_DISPLACEMENT_M = 50f
    const val STATIONARY_INTERVAL_MS = 15 * 60_000L
    const val ADMIN_POLL_INTERVAL_MS = 30_000L
    const val PRIORITY = Priority.PRIORITY_BALANCED_POWER_ACCURACY
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "gps_tracking"
    val FUENTES_PERMITIDAS = setOf("gps", "network", "fused", "passive")
    const val ACCURACY_MAX_M = 100f
}
