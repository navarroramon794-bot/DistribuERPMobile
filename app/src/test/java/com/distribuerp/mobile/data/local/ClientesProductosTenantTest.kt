package com.distribuerp.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.Producto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClientesProductosTenantTest {

    private lateinit var db: AppDatabase
    private val clienteDao get() = db.clienteDao()
    private val productoDao get() = db.productoDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `clientes se insertan y consultan por tenant`() = runBlocking {
        val c1 = ClienteEntity(empresa_id = 1, id = 10, nombre = "Ana", direccion = "D1", telefono = "123", limite_credito = 100.0, activo = true, fecha_creacion = "2026-01-01", credito_autorizado = false, dias_credito = 0, bloqueado = false)
        val c2 = ClienteEntity(empresa_id = 2, id = 10, nombre = "Luis", direccion = "D2", telefono = "456", limite_credito = 200.0, activo = true, fecha_creacion = "2026-01-01", credito_autorizado = true, dias_credito = 15, bloqueado = false)
        clienteDao.insertar(c1)
        clienteDao.insertar(c2)

        assertEquals(1, clienteDao.contar(1))
        assertEquals(1, clienteDao.contar(2))
        val t1 = clienteDao.obtenerTodos(1)
        val t2 = clienteDao.obtenerTodos(2)
        assertEquals("Ana", t1[0].nombre)
        assertEquals("Luis", t2[0].nombre)
        assertEquals(1, t1[0].empresa_id)
        assertEquals(2, t2[0].empresa_id)
    }

    @Test
    fun `productos con PK compuesta y aislamiento`() = runBlocking {
        val p1 = ProductoEntity(empresa_id = 1, id = 20, codigo = "A01", nombre = "ProdA", descripcion = "d", costo = 1.0, precio = 2.0, existencia = 5.0, codigo_barras = "123", tipo_codigo = "EAN-13", unidad_venta = "kg", activo = true, fecha_creacion = "2026-01-01")
        val p2 = ProductoEntity(empresa_id = 2, id = 20, codigo = "B01", nombre = "ProdB", descripcion = "d", costo = 3.0, precio = 4.0, existencia = 10.0, codigo_barras = "456", tipo_codigo = "EAN-13", unidad_venta = "kg", activo = true, fecha_creacion = "2026-01-01")
        productoDao.insertar(p1)
        productoDao.insertar(p2)

        assertEquals(1, productoDao.contar(1))
        assertEquals(1, productoDao.contar(2))
        assertEquals("ProdA", productoDao.obtenerPorId(1,20)?.nombre)
        assertEquals("ProdB", productoDao.obtenerPorId(2,20)?.nombre)
        assertEquals("A01", productoDao.obtenerPorCodigo(1,"A01")?.codigo)
        assertEquals("B01", productoDao.obtenerPorCodigo(2,"B01")?.codigo)
    }

    @Test
    fun `buscar clientes por texto dentro de tenant`() = runBlocking {
        clienteDao.insertar(ClienteEntity(empresa_id=1,id=1,nombre="Ana María",telefono="555",limite_credito=0.0,activo=true))
        clienteDao.insertar(ClienteEntity(empresa_id=1,id=2,nombre="Pedro",telefono="444",limite_credito=0.0,activo=true))
        clienteDao.insertar(ClienteEntity(empresa_id=2,id=1,nombre="Ana",telefono="333",limite_credito=0.0,activo=true))

        val res1 = clienteDao.buscar(1,"Ana")
        val res2 = clienteDao.buscar(2,"Ana")
        assertEquals(1, res1.size)
        assertEquals("Ana María", res1[0].nombre)
        assertEquals(1, res2.size)
    }

    @Test
    fun `buscar productos activos por texto`() = runBlocking {
        productoDao.insertar(ProductoEntity(empresa_id=1,id=1,codigo="AB01",nombre="Arroz",precio=1.0,existencia=1.0,activo=true,unidad_venta="kg"))
        productoDao.insertar(ProductoEntity(empresa_id=1,id=2,codigo="CD02",nombre="Frijol",precio=1.0,existencia=1.0,activo=false,unidad_venta="kg"))
        productoDao.insertar(ProductoEntity(empresa_id=1,id=3,codigo="EF03",nombre="Arroz Integral",precio=1.0,existencia=1.0,activo=true,unidad_venta="kg"))

        val res = productoDao.buscarActivos(1,"Arroz")
        assertEquals(2, res.size)
        assertTrue(res.all { it.activo })
    }

    @Test
    fun `eliminarTodoTenant solo borra ese tenant`() = runBlocking {
        clienteDao.insertar(ClienteEntity(empresa_id=1,id=1,nombre="A",limite_credito=0.0,activo=true))
        clienteDao.insertar(ClienteEntity(empresa_id=2,id=1,nombre="B",limite_credito=0.0,activo=true))
        productoDao.insertar(ProductoEntity(empresa_id=1,id=1,codigo="X",nombre="X",precio=0.0,existencia=0.0,activo=true,unidad_venta="kg"))
        productoDao.insertar(ProductoEntity(empresa_id=2,id=1,codigo="Y",nombre="Y",precio=0.0,existencia=0.0,activo=true,unidad_venta="kg"))

        clienteDao.eliminarTodoTenant(1)
        productoDao.eliminarTodoTenant(1)

        assertEquals(0, clienteDao.contar(1))
        assertEquals(1, clienteDao.contar(2))
        assertEquals(0, productoDao.contar(1))
        assertEquals(1, productoDao.contar(2))
    }
}