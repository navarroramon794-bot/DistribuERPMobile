package com.distribuerp.mobile.api

import com.distribuerp.mobile.models.CargaRequest
import com.distribuerp.mobile.models.CargaResponse
import com.distribuerp.mobile.models.CargasResponse
import com.distribuerp.mobile.models.ClienteRequest
import com.distribuerp.mobile.models.ClienteResponse
import com.distribuerp.mobile.models.ClientesResponse
import com.distribuerp.mobile.models.CompraRequest
import com.distribuerp.mobile.models.CompraResponse
import com.distribuerp.mobile.models.ComprasResponse
import com.distribuerp.mobile.models.DashboardResponse
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.InventarioResponse
import com.distribuerp.mobile.models.LoginRequest
import com.distribuerp.mobile.models.LoginResponse
import com.distribuerp.mobile.models.MensajeResponse
import com.distribuerp.mobile.models.PagoRequest
import com.distribuerp.mobile.models.PagoResponse
import com.distribuerp.mobile.models.PagosResponse
import com.distribuerp.mobile.models.PingResponse
import com.distribuerp.mobile.models.ProductoRequest
import com.distribuerp.mobile.models.ProductoResponse
import com.distribuerp.mobile.models.ProductosResponse
import com.distribuerp.mobile.models.ProveedorRequest
import com.distribuerp.mobile.models.ProveedorResponse
import com.distribuerp.mobile.models.ProveedoresResponse
import com.distribuerp.mobile.models.ReporteClientesSaldoResponse
import com.distribuerp.mobile.models.ReportePagosResponse
import com.distribuerp.mobile.models.ReporteProductosResponse
import com.distribuerp.mobile.models.ReporteVentasResponse
import com.distribuerp.mobile.models.VendedorRequest
import com.distribuerp.mobile.models.VendedorResponse
import com.distribuerp.mobile.models.VendedoresResponse
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.models.VentaResponse
import com.distribuerp.mobile.models.VersionResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/ping")
    fun ping(): Call<PingResponse>

    @GET("api/version")
    fun getVersion(): Call<VersionResponse>

    @POST("api/login")
    fun login(
        @Body datos: LoginRequest
    ): Call<LoginResponse>

    @GET("logout")
    fun logout(): Call<ResponseBody>

    @GET("api/dashboard")
    fun getDashboard(): Call<DashboardResponse>

    @GET("api/clientes")
    fun getClientes(): Call<ClientesResponse>

    @GET("api/clientes/{id}")
    fun getCliente(
        @Path("id") id: Int
    ): Call<ClienteResponse>

    @POST("api/clientes")
    fun crearCliente(
        @Body datos: ClienteRequest
    ): Call<ClienteResponse>

    @PUT("api/clientes/{id}")
    fun actualizarCliente(
        @Path("id") id: Int,
        @Body datos: ClienteRequest
    ): Call<ClienteResponse>

    @DELETE("api/clientes/{id}")
    fun eliminarCliente(
        @Path("id") id: Int
    ): Call<MensajeResponse>

    @GET("api/productos")
    fun getProductos(): Call<ProductosResponse>

    @GET("api/productos/{id}")
    fun getProducto(
        @Path("id") id: Int
    ): Call<ProductoResponse>

    @POST("api/productos")
    fun crearProducto(
        @Body datos: ProductoRequest
    ): Call<ProductoResponse>

    @PUT("api/productos/{id}")
    fun actualizarProducto(
        @Path("id") id: Int,
        @Body datos: ProductoRequest
    ): Call<ProductoResponse>

    @DELETE("api/productos/{id}")
    fun eliminarProducto(
        @Path("id") id: Int
    ): Call<MensajeResponse>

    @GET("api/vendedores")
    fun getVendedores(): Call<VendedoresResponse>

    @GET("api/vendedores/{id}")
    fun getVendedor(
        @Path("id") id: Int
    ): Call<VendedorResponse>

    @POST("api/vendedores")
    fun crearVendedor(
        @Body datos: VendedorRequest
    ): Call<VendedorResponse>

    @PUT("api/vendedores/{id}")
    fun actualizarVendedor(
        @Path("id") id: Int,
        @Body datos: VendedorRequest
    ): Call<VendedorResponse>

    @DELETE("api/vendedores/{id}")
    fun eliminarVendedor(
        @Path("id") id: Int
    ): Call<MensajeResponse>

    @GET("api/inventario/{vendedorId}")
    fun getInventario(
        @Path("vendedorId") vendedorId: Int
    ): Call<InventarioResponse>

    @POST("api/ventas")
    fun crearVenta(
        @Body datos: VentaRequest
    ): Call<VentaResponse>

    @GET("api/cargas")
    fun getCargas(
        @Query("vendedor_id") vendedorId: Int?
    ): Call<CargasResponse>

    @GET("api/cargas/{id}")
    fun getCarga(
        @Path("id") id: Int
    ): Call<CargaResponse>

    @POST("api/cargas")
    fun crearCarga(
        @Body datos: CargaRequest
    ): Call<CargaResponse>

    @GET("api/pagos")
    fun getPagos(): Call<PagosResponse>

    @GET("api/pagos/{id}")
    fun getPago(
        @Path("id") id: Int
    ): Call<PagoResponse>

    @POST("api/pagos")
    fun crearPago(
        @Body datos: PagoRequest
    ): Call<PagoResponse>

    @GET("api/clientes/{id}/estado_cuenta")
    fun getEstadoCuenta(
        @Path("id") id: Int
    ): Call<EstadoCuentaResponse>

    @GET("api/reportes/ventas")
    fun getReporteVentas(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?
    ): Call<ReporteVentasResponse>

    @GET("api/reportes/pagos")
    fun getReportePagos(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?
    ): Call<ReportePagosResponse>

    @GET("api/reportes/productos")
    fun getReporteProductos(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?
    ): Call<ReporteProductosResponse>

    @GET("api/reportes/clientes_saldos")
    fun getReporteClientesSaldo():
        Call<ReporteClientesSaldoResponse>

    @GET("api/reportes/exportar/{reporte}/{formato}")
    fun exportarReporte(
        @Path("reporte") reporte: String,
        @Path("formato") formato: String,
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?
    ): Call<ResponseBody>

    @GET("api/proveedores")
    fun getProveedores(): Call<ProveedoresResponse>

    @GET("api/proveedores/{id}")
    fun getProveedor(
        @Path("id") id: Int
    ): Call<ProveedorResponse>

    @POST("api/proveedores")
    fun crearProveedor(
        @Body datos: ProveedorRequest
    ): Call<ProveedorResponse>

    @PUT("api/proveedores/{id}")
    fun actualizarProveedor(
        @Path("id") id: Int,
        @Body datos: ProveedorRequest
    ): Call<ProveedorResponse>

    @DELETE("api/proveedores/{id}")
    fun eliminarProveedor(
        @Path("id") id: Int
    ): Call<MensajeResponse>

    @GET("api/compras")
    fun getCompras(
        @Query("proveedor_id") proveedorId: Int?
    ): Call<ComprasResponse>

    @GET("api/compras/{id}")
    fun getCompra(
        @Path("id") id: Int
    ): Call<CompraResponse>

    @POST("api/compras")
    fun crearCompra(
        @Body datos: CompraRequest
    ): Call<CompraResponse>
}
