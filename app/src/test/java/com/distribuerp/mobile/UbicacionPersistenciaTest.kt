package com.distribuerp.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.UbicacionPendienteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class UbicacionPersistenciaTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = ctx.getDatabasePath("test_persist_${System.currentTimeMillis()}.db")
        dbFile.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        dbFile.delete()
        ctx.getDatabasePath("test_persist.db").delete()
    }

@Test
    fun persistenciaDespuesDeRecrearDB() {
        runBlocking {
            // Crear DB Room sobre archivo temporal
            val db1 = Room.databaseBuilder(ctx, AppDatabase::class.java, "test_persist.db").allowMainThreadQueries().build()
            val dao1 = db1.ubicacionPendienteDao()
            dao1.insertar(UbicacionPendienteEntity(vendedorId = 99, latitud = 19.1, longitud = -99.1, precisionM = 5.0, velocidad = null, fuente = "fused", fecha = "2026-09-11T10:00:00-06:00"))
            db1.close()

            // Recrear instancia apuntando al mismo archivo
            val db2 = Room.databaseBuilder(ctx, AppDatabase::class.java, "test_persist.db").allowMainThreadQueries().build()
            val dao2 = db2.ubicacionPendienteDao()
            val lista = dao2.obtenerPorVendedor(99)
            assertEquals(1, lista.size)
            assertEquals(19.1, lista[0].latitud, 0.001)
            // Limpieza
            dao2.eliminarPorVendedor(99)
            db2.close()
            ctx.deleteDatabase("test_persist.db")
        }
    }

    @Test
    fun insertarYConsultar() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.ubicacionPendienteDao()
        dao.insertar(UbicacionPendienteEntity(vendedorId = 1, latitud = 19.0, longitud = -99.0, precisionM = 5.0, velocidad = 1.2, fuente = "gps", fecha = "2026-09-11T10:00:00-06:00"))
        assertEquals(1, dao.contarPorVendedor(1))
        db.close()
    }
}
