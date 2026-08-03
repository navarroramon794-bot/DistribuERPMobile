package com.distribuerp.mobile.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distribuerp.mobile.models.TipoReporte
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.fechaCorta
import com.distribuerp.mobile.viewmodel.ReporteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteFormScreen(
    tipoClave: String,
    onVolver: () -> Unit
) {
    val viewModel: ReporteViewModel =
        viewModel(factory = ReporteViewModel.Factory)

    val contexto = LocalContext.current

    val tipo = TipoReporte.desdeClave(tipoClave)

    var avisoLocal by remember {
        mutableStateOf<String?>(null)
    }

    var pendienteFormato by remember {
        mutableStateOf<String?>(null)
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->

        val formato = pendienteFormato
        pendienteFormato = null

        if (concedido) {

            when (formato) {

                "pdf" -> viewModel.exportarPdf()

                else -> viewModel.exportarExcel()
            }

        } else {

            avisoLocal =
                "Permiso necesario para guardar " +
                    "el archivo en Descargas"
        }
    }

    fun iniciarExportacion(formato: String) {
        val requierePermiso =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED

        if (requierePermiso) {

            pendienteFormato = formato
            permisoLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        } else {

            when (formato) {

                "pdf" -> viewModel.exportarPdf()

                else -> viewModel.exportarExcel()
            }
        }
    }

    LaunchedEffect(tipoClave) {
        viewModel.configurar(tipo)
    }

    val cargandoResumen = viewModel.cargandoResumen
    val resumenTexto = viewModel.resumenTexto
    val exportando = viewModel.exportando
    val error = viewModel.error
    val mensaje = viewModel.mensaje
    val archivo = viewModel.archivoDescargado

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Text(tipo.titulo)
                },
                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            mensaje?.let { aviso ->

                item {

                    AvisoMensaje(
                        mensaje = aviso,
                        onCerrar = {

                            viewModel.limpiarMensaje()
                        }
                    )
                }
            }

            avisoLocal?.let { aviso ->

                item {

                    AvisoMensaje(
                        mensaje = aviso,
                        onCerrar = {

                            avisoLocal = null
                        }
                    )
                }
            }

            if (tipo.requiereFechas) {

                item {

                    Text(
                        text = "Rango de fechas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        SelectorFecha(
                            etiqueta = "Desde",
                            fechaMillis = viewModel.desdeMillis,
                            onCambio = viewModel::cambiarDesde,
                            modifier = Modifier.weight(1f)
                        )

                        SelectorFecha(
                            etiqueta = "Hasta",
                            fechaMillis = viewModel.hastaMillis,
                            onCambio = viewModel::cambiarHasta,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Text(
                            text = "Resumen",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        when {

                            cargandoResumen -> {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            error != null -> {

                                Text(
                                    text = error,
                                    style =
                                        MaterialTheme
                                            .typography.bodyMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme.error
                                )

                                TextButton(
                                    onClick = {

                                        viewModel.cargarResumen()
                                    }
                                ) {

                                    Text("Reintentar")
                                }
                            }

                            else -> {

                                Text(
                                    text = resumenTexto
                                        ?: "Sin datos disponibles",
                                    style =
                                        MaterialTheme
                                            .typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {

                            iniciarExportacion("pdf")
                        },
                        enabled = !exportando,
                        modifier = Modifier.weight(1f)
                    ) {

                        if (exportando) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme
                                    .colorScheme.onPrimary
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text("Exportar PDF")
                        }
                    }

                    OutlinedButton(
                        onClick = {

                            iniciarExportacion("excel")
                        },
                        enabled = !exportando,
                        modifier = Modifier.weight(1f)
                    ) {

                        if (exportando) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text("Exportar Excel")
                        }
                    }
                }
            }
        }
    }

    archivo?.let { descargado ->

        AlertDialog(
            onDismissRequest = {

                viewModel.aceptarArchivo()
            },
            title = {

                Text("Reporte descargado")
            },
            text = {

                Text(descargado.nombre)
            },
            confirmButton = {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    TextButton(
                        onClick = {

                            abrirArchivo(
                                contexto,
                                descargado.uri,
                                descargado.mime
                            )
                        }
                    ) {

                        Text("Abrir")
                    }

                    TextButton(
                        onClick = {

                            compartirArchivo(
                                contexto,
                                descargado.uri,
                                descargado.mime
                            )
                        }
                    ) {

                        Text("Compartir / Imprimir")
                    }

                    TextButton(
                        onClick = {

                            viewModel.aceptarArchivo()
                        }
                    ) {

                        Text("Cerrar")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(
    etiqueta: String,
    fechaMillis: Long?,
    onCambio: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrando by remember {
        mutableStateOf(false)
    }

    val estado = rememberDatePickerState(
        initialSelectedDateMillis = fechaMillis
    )

    Box(
        modifier = modifier
    ) {

        OutlinedTextField(
            value = fechaCorta(fechaMillis),
            onValueChange = {},
            readOnly = true,
            label = {
                Text(etiqueta)
            },
            trailingIcon = {

                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "Seleccionar fecha"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {

                    mostrando = true
                }
        )
    }

    if (mostrando) {

        DatePickerDialog(
            onDismissRequest = {

                mostrando = false
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        onCambio(estado.selectedDateMillis)
                        mostrando = false
                    }
                ) {

                    Text("Aceptar")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {

                        mostrando = false
                    }
                ) {

                    Text("Cancelar")
                }
            }
        ) {

            DatePicker(
                state = estado
            )
        }
    }
}

private fun abrirArchivo(
    contexto: android.content.Context,
    uri: Uri,
    mime: String
) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {

        contexto.startActivity(
            Intent.createChooser(intent, "Abrir con")
        )
    }
}

private fun compartirArchivo(
    contexto: android.content.Context,
    uri: Uri,
    mime: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {

        contexto.startActivity(
            Intent.createChooser(
                intent,
                "Compartir o imprimir"
            )
        )
    }
}
