package com.distribuerp.mobile.worker

import android.content.Context
import androidx.work.*
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.models.UbicacionRequest
import com.distribuerp.mobile.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class LocationSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.ubicacionPendienteDao()
        val pendientes = dao.obtenerTodas()
        if (pendientes.isEmpty()) return@withContext Result.success()
        val porVendedor = pendientes.groupBy { it.vendedorId }
        for ((vendedorId, lista) in porVendedor) {
            if (lista.size > 50) dao.recortarA50(vendedorId)
            val ultima = lista.maxByOrNull { it.timestampMs } ?: continue
            val req = UbicacionRequest(latitud = ultima.latitud, longitud = ultima.longitud, precision_m = ultima.precisionM, velocidad = ultima.velocidad, fuente = ultima.fuente, fecha = ultima.fecha)
            try {
                val resp = RetrofitClient.api.enviarUbicacion(req).execute()
                when {
                    resp.isSuccessful -> {
                        dao.eliminarPorId(ultima.id)
                        val restantes = dao.obtenerPorVendedor(vendedorId)
                        if (restantes.size > 1) {
                            for (r in restantes) if (r.id != ultima.id) dao.eliminarPorId(r.id)
                        }
                    }
                    resp.code() in listOf(400, 401, 403) -> dao.eliminarPorId(ultima.id)
                    else -> return@withContext Result.retry()
                }
            } catch (e: IOException) {
                return@withContext Result.retry()
            } catch (_: Exception) {
                return@withContext Result.failure()
            }
        }
        Result.success()
    }
    companion object {
        private const val WORK_NAME = "location_sync"
        fun encolar(context: Context) {
            val req = OneTimeWorkRequestBuilder<LocationSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, req)
        }
    }
}
