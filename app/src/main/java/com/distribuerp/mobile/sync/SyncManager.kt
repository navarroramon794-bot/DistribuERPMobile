package com.distribuerp.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import java.util.concurrent.TimeUnit

/**
 * Agenda el envio de la Outbox usando el `SyncGeneralWorker` existente.
 *
 * Es el unico punto de encolado de ventas: no hay worker ni manager paralelos.
 */
object SyncManager {

    const val TRABAJO_GENERAL = "sync_general"

    private const val BACKOFF_BASE_SEG = 30L

    /**
     * Encolado real contra WorkManager.
     *
     * Se sustituye en tests para observar la llamada sin depender de
     * WorkManager, que en una prueba unitaria exigiria `work-testing`.
     */
    internal var encolar: (
        contexto: Context,
        nombre: String,
        politica: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) -> Unit = { contexto, nombre, politica, request ->
        WorkManager.getInstance(contexto).enqueueUniqueWork(nombre, politica, request)
    }

    /**
     * Encola una sincronizacion general.
     *
     * La restriccion `CONNECTED` hace que WorkManager espere a que haya red y
     * ejecute el worker en cuanto vuelva, sin depender de ningun observador de
     * conectividad. Como es *unique work* con politica `KEEP`, llamarla varias
     * veces no deja workers simultaneos: mientras haya uno pendiente o en
     * ejecucion, las llamadas siguientes se ignoran.
     */
    fun encolarSincronizacion(context: Context) {
        encolar(
            context.applicationContext,
            TRABAJO_GENERAL,
            ExistingWorkPolicy.KEEP,
            construirRequest()
        )
    }

    /**
     * Encola la sincronizacion solo si hay operaciones pendientes de reintento
     * o de envio.
     *
     * Se usa al arrancar la app para retomar lo que quedo en la Outbox de una
     * ejecucion anterior. Las operaciones en error definitivo no cuentan: ya no
     * deben reintentarse solas.
     *
     * Devuelve `true` si se encolo algo.
     */
    suspend fun programarSiHayPendientes(
        context: Context,
        db: AppDatabase = AppDatabase.getInstance(context)
    ): Boolean {
        val pendientes =
            db.outboxDao().contarPorEstado(EstadoOutbox.PENDIENTE.valor)

        if (pendientes == 0) return false

        encolarSincronizacion(context)
        return true
    }

    private fun construirRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<SyncGeneralWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_BASE_SEG,
                TimeUnit.SECONDS
            )
            .build()
}
