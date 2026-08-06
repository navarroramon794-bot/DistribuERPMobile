package com.distribuerp.mobile.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.PingResponse
import com.distribuerp.mobile.printing.BluetoothPrinterManager
import com.distribuerp.mobile.printing.DispositivoDiagnostico
import com.distribuerp.mobile.printing.PrinterRepository
import com.distribuerp.mobile.repository.mensajeAmigable
import com.distribuerp.mobile.ui.components.FilaInformacion
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoScreen(
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val repositorio = remember {
        PrinterRepository(contexto.applicationContext)
    }
    val configuracion by repositorio.configuracion.collectAsState(
        initial = null
    )

    var servidorEstado by remember {
        mutableStateOf("Conectando...")
    }
    var servidorOk by remember {
        mutableStateOf<Boolean?>(null)
    }
    var comprobando by remember {
        mutableStateOf(false)
    }

    var dispositivos by remember {
        mutableStateOf<List<DispositivoDiagnostico>>(emptyList())
    }
    var bluetoothError by remember {
        mutableStateOf<String?>(null)
    }

    val launcherPermisoBluetooth =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->
            if (concedido) {
                bluetoothError = null
                dispositivos =
                    BluetoothPrinterManager.diagnosticarDispositivos()
            } else {
                bluetoothError =
                    "Permiso Bluetooth denegado"
            }
        }

    fun cargarDispositivosBluetooth() {
        val necesitaPermiso =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED

        if (necesitaPermiso) {
            launcherPermisoBluetooth.launch(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            bluetoothError = null
            dispositivos =
                BluetoothPrinterManager.diagnosticarDispositivos()
        }
    }

    fun comprobarServidor() {
        if (comprobando) return

        comprobando = true
        servidorEstado = "Comprobando..."

        RetrofitClient.api
            .ping()
            .enqueue(
                object : Callback<PingResponse> {

                    override fun onResponse(
                        call: Call<PingResponse>,
                        response: Response<PingResponse>
                    ) {
                        comprobando = false
                        servidorOk = response.isSuccessful
                        servidorEstado = if (response.isSuccessful) {
                            "Conectado"
                        } else {
                            "Error HTTP ${response.code()}"
                        }
                    }

                    override fun onFailure(
                        call: Call<PingResponse>,
                        t: Throwable
                    ) {
                        comprobando = false
                        servidorOk = false
                        servidorEstado = mensajeAmigable(t)
                        Log.e("DIAGNOSTICO", "Ping fallido", t)
                    }
                }
            )
    }

    LaunchedEffect(Unit) {
        comprobarServidor()
        cargarDispositivosBluetooth()
    }

    val impresoraNombre = configuracion?.nombre
    val impresoraConectada = BluetoothPrinterManager.conectado

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Diagnóstico")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

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
                        text = "Servidor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Cloud,
                        etiqueta = "Estado",
                        valor = servidorEstado
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "URL",
                        valor = "https://distribu-erp.onrender.com/"
                    )

                    if (servidorOk == false) {

                        Button(
                            onClick = {
                                comprobarServidor()
                            },
                            enabled = !comprobando,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            if (comprobando) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color =
                                        MaterialTheme.colorScheme.onPrimary
                                )

                            } else {

                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.size(8.dp)
                                )

                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

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
                        text = "Impresora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = if (impresoraConectada) {
                            Icons.Filled.Bluetooth
                        } else {
                            Icons.AutoMirrored.Filled.BluetoothSearching
                        },
                        etiqueta = "Estado",
                        valor = if (impresoraConectada) {
                            "Conectada"
                        } else {
                            "Desconectada"
                        }
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Print,
                        etiqueta = "Impresora",
                        valor = impresoraNombre ?: "Sin configurar"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Bluetooth,
                        etiqueta = "Dirección",
                        valor = configuracion?.mac ?: "—"
                    )
                }
            }

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
                        text = "Bluetooth emparejado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = if (BluetoothPrinterManager.bluetoothActivado()) {
                            Icons.Filled.Bluetooth
                        } else {
                            Icons.AutoMirrored.Filled.BluetoothSearching
                        },
                        etiqueta = "Bluetooth",
                        valor = if (BluetoothPrinterManager.bluetoothActivado()) {
                            "Activado"
                        } else {
                            "Apagado"
                        }
                    )

                    if (bluetoothError != null) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = bluetoothError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {
                                cargarDispositivosBluetooth()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.size(8.dp)
                            )

                            Text("Conceder permiso")
                        }

                    } else if (dispositivos.isEmpty()) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "No hay dispositivos emparejados",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    } else {

                        dispositivos.forEach { dispositivo ->

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = dispositivo.nombre,
                                style =
                                    MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            FilaInformacion(
                                icono = Icons.Filled.Bluetooth,
                                etiqueta = "Dirección",
                                valor = dispositivo.mac
                            )

                            FilaInformacion(
                                icono = Icons.Filled.Info,
                                etiqueta = "Tipo",
                                valor = dispositivo.tipo
                            )

                            FilaInformacion(
                                icono = Icons.Filled.CheckCircle,
                                etiqueta = "Vinculo",
                                valor = dispositivo.vinculo
                            )
                        }
                    }
                }
            }

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
                        text = "Aplicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Versión",
                        valor = BuildConfig.APP_VERSION
                    )

                    FilaInformacion(
                        icono = Icons.Filled.CheckCircle,
                        etiqueta = "Ambiente",
                        valor = "Producción (Render)"
                    )
                }
            }
        }
    }
}
