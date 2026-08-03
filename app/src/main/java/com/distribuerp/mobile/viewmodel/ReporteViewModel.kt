package com.distribuerp.mobile.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.distribuerp.mobile.models.TipoReporte
import com.distribuerp.mobile.repository.DescargadorArchivos
import com.distribuerp.mobile.repository.ReporteRepository
import com.distribuerp.mobile.repository.mensajeAmigable
import com.distribuerp.mobile.ui.components.fechaDeMillis
import com.distribuerp.mobile.ui.components.formatearDinero
import com.distribuerp.mobile.ui.components.millisDeFecha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import okhttp3.ResponseBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ArchivoDescargado(
    val uri: Uri,
    val nombre: String,
    val mime: String
)

class ReporteViewModel(
    private val contexto: Context,
    private val repository: ReporteRepository
) : ViewModel() {

    var reporteTipo by mutableStateOf<TipoReporte?>(null)
        private set

    var desdeMillis by mutableStateOf<Long?>(null)
        private set

    var hastaMillis by mutableStateOf<Long?>(null)
        private set

    var cargandoResumen by mutableStateOf(false)
        private set

    var resumenTexto by mutableStateOf<String?>(null)
        private set

    var exportando by mutableStateOf(false)
        private set

    var archivoDescargado by mutableStateOf<ArchivoDescargado?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf<String?>(null)
        private set

    fun configurar(tipo: TipoReporte) {
        if (reporteTipo == tipo) {
            return
        }

        reporteTipo = tipo
        limpiarEstado()

        if (tipo.requiereFechas) {
            val hoy = LocalDate.now()

            desdeMillis = millisDeFecha(
                hoy.minusDays(29)
            )

            hastaMillis = millisDeFecha(hoy)
        }

        cargarResumen()
    }

    fun cambiarDesde(millis: Long?) {
        desdeMillis = millis
        error = null
        mensaje = null

        if (
            reporteTipo?.requiereFechas == true
            && desdeMillis != null
            && hastaMillis != null
        ) {
            cargarResumen()
        }
    }

    fun cambiarHasta(millis: Long?) {
        hastaMillis = millis
        error = null
        mensaje = null

        if (
            reporteTipo?.requiereFechas == true
            && desdeMillis != null
            && hastaMillis != null
        ) {
            cargarResumen()
        }
    }

    fun cargarResumen() {
        val tipo = reporteTipo ?: return

        cargandoResumen = true
        error = null

        val (desde, hasta) = parametrosFecha(tipo)

        when (tipo) {
            TipoReporte.VENTAS -> {
                repository.obtenerVentas(
                    desde = desde,
                    hasta = hasta,
                    onSuccess = { respuesta ->
                        cargandoResumen = false
                        resumenTexto = (
                            "Ventas: ${respuesta.total_ventas ?: 0}  " +
                                "·  Monto: " +
                                formatearDinero(
                                    respuesta.total_monto ?: 0.0
                                )
                            )
                    },
                    onError = { t ->
                        cargandoResumen = false
                        error = mensajeAmigable(t)
                    }
                )
            }

            TipoReporte.PAGOS -> {
                repository.obtenerPagos(
                    desde = desde,
                    hasta = hasta,
                    onSuccess = { respuesta ->
                        cargandoResumen = false
                        resumenTexto = (
                            "Pagos: ${respuesta.pagos?.size ?: 0}  " +
                                "·  Monto cobrado: " +
                                formatearDinero(
                                    respuesta.total_monto ?: 0.0
                                )
                            )
                    },
                    onError = { t ->
                        cargandoResumen = false
                        error = mensajeAmigable(t)
                    }
                )
            }

            TipoReporte.PRODUCTOS -> {
                repository.obtenerProductos(
                    desde = desde,
                    hasta = hasta,
                    onSuccess = { respuesta ->
                        cargandoResumen = false
                        resumenTexto = (
                            "Productos: ${respuesta.productos?.size ?: 0}  " +
                                "·  Importe total: " +
                                formatearDinero(
                                    respuesta.total_monto ?: 0.0
                                )
                            )
                    },
                    onError = { t ->
                        cargandoResumen = false
                        error = mensajeAmigable(t)
                    }
                )
            }

            TipoReporte.CLIENTES -> {
                repository.obtenerClientesSaldo(
                    onSuccess = { respuesta ->
                        cargandoResumen = false
                        resumenTexto = (
                            "Clientes: ${respuesta.total_clientes ?: 0}  " +
                                "·  Saldo total: " +
                                formatearDinero(
                                    respuesta.total_saldo ?: 0.0
                                )
                            )
                    },
                    onError = { t ->
                        cargandoResumen = false
                        error = mensajeAmigable(t)
                    }
                )
            }
        }
    }

    fun exportarPdf() {
        exportar("pdf")
    }

    fun exportarExcel() {
        exportar("excel")
    }

    private fun exportar(formato: String) {
        val tipo = reporteTipo ?: return

        val (desde, hasta) = parametrosFecha(tipo)

        if (tipo.requiereFechas && desde == null && hasta == null) {
            mensaje = "Selecciona el rango de fechas"
            return
        }

        exportando = true
        error = null
        mensaje = null

        repository.exportar(
            reporte = tipo.clave,
            formato = formato,
            desde = desde,
            hasta = hasta,
            onSuccess = { respuesta ->
                descargarArchivo(
                    tipo = tipo,
                    formato = formato,
                    respuesta = respuesta
                )
            },
            onError = { t ->
                exportando = false
                mensaje = mensajeAmigable(t)
            }
        )
    }

    private fun descargarArchivo(
        tipo: TipoReporte,
        formato: String,
        respuesta: Response<ResponseBody>
    ) {
        val cuerpo = respuesta.body() ?: run {
            exportando = false
            mensaje = "El servidor no devolvió el archivo"
            return
        }

        val nombre = nombreArchivo(
            respuesta = respuesta,
            tipo = tipo,
            formato = formato
        )

        val mime = cuerpo.contentType()?.toString()
            ?: mimeDeFormato(formato)

        viewModelScope.launch {
            val resultado = runCatching {
                withContext(Dispatchers.IO) {
                    DescargadorArchivos.guardarEnDescargas(
                        contexto = contexto,
                        cuerpo = cuerpo,
                        nombreArchivo = nombre
                    )
                }
            }

            exportando = false

            resultado.onSuccess { uri ->
                archivoDescargado = ArchivoDescargado(
                    uri = uri,
                    nombre = nombre,
                    mime = mime
                )
            }.onFailure { t ->
                mensaje = mensajeAmigable(t)
            }
        }
    }

    private fun nombreArchivo(
        respuesta: Response<ResponseBody>,
        tipo: TipoReporte,
        formato: String
    ): String {
        val disposicion = respuesta.headers()["Content-Disposition"]

        val servidor = disposicion?.let { valor ->
            Regex(
                "filename=\"?([^\";]+)\"?"
            ).find(valor)?.groupValues?.get(1)
        }

        if (!servidor.isNullOrBlank()) {
            return servidor
        }

        val ext = if (formato == "pdf") "pdf" else "xlsx"

        val fecha = LocalDate.now().format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )

        return "${tipo.nombreBase}_$fecha.$ext"
    }

    private fun mimeDeFormato(formato: String): String {
        return if (formato == "pdf") {
            "application/pdf"
        } else {
            "application/vnd.openxmlformats-" +
                "officedocument.spreadsheetml.sheet"
        }
    }

    fun aceptarArchivo() {
        archivoDescargado = null
    }

    fun limpiarMensaje() {
        mensaje = null
    }

    fun limpiarError() {
        error = null
    }

    private fun parametrosFecha(
        tipo: TipoReporte
    ): Pair<String?, String?> {
        if (!tipo.requiereFechas) {
            return null to null
        }

        val desde = fechaDeMillis(desdeMillis)
        val hasta = fechaDeMillis(hastaMillis)

        return desde to hasta
    }

    private fun limpiarEstado() {
        desdeMillis = null
        hastaMillis = null
        cargandoResumen = false
        resumenTexto = null
        exportando = false
        archivoDescargado = null
        error = null
        mensaje = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[
                    ViewModelProvider
                        .AndroidViewModelFactory
                        .APPLICATION_KEY
                ] as Application

                ReporteViewModel(
                    contexto = app.applicationContext,
                    repository = ReporteRepository()
                )
            }
        }
    }
}
