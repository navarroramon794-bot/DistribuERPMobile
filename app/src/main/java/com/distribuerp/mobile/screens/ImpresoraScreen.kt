package com.distribuerp.mobile.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.distribuerp.mobile.printing.BluetoothPrinterManager
import com.distribuerp.mobile.printing.ConfiguracionImpresora
import com.distribuerp.mobile.printing.PrinterRepository
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.EmptyContent
import com.distribuerp.mobile.ui.components.FilaInformacion
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpresoraScreen(
    onAbrirMenu: () -> Unit
) {
    val contexto = LocalContext.current
    val repositorio = remember {
        PrinterRepository(contexto.applicationContext)
    }
    val configuracion by repositorio.configuracion.collectAsState(
        initial = null
    )
    val scope = rememberCoroutineScope()

    var emparejadas by remember {
        mutableStateOf<List<BluetoothDevice>>(emptyList())
    }
    var conectandoMac by remember {
        mutableStateOf<String?>(null)
    }
    var imprimiendoPrueba by remember {
        mutableStateOf(false)
    }
    var mensaje by remember {
        mutableStateOf<String?>(null)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    val launcherPermiso =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->

            if (concedido) {
                error = null
                emparejadas =
                    BluetoothPrinterManager.dispositivosEmparejados()
            } else {
                error =
                    "Se necesita el permiso Bluetooth para " +
                        "usar la impresora"
            }
        }

    fun buscarImpresoras() {
        val necesitaPermiso =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED

        if (necesitaPermiso) {
            launcherPermiso.launch(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            error = null
            emparejadas =
                BluetoothPrinterManager.dispositivosEmparejados()
        }
    }

    LaunchedEffect(Unit) {
        buscarImpresoras()
    }

    fun mostrarResultado(resultado: com.distribuerp.mobile.printing.ResultadoImpresion) {
        if (resultado.ok) {
            error = null
            mensaje = resultado.mensaje
        } else {
            mensaje = null
            error = resultado.mensaje
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Impresora")
                },
                navigationIcon = {

                    IconButton(
                        onClick = onAbrirMenu
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menú"
                        )
                    }
                },
                actions = {

                    IconButton(
                        onClick = {

                            buscarImpresoras()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Buscar impresoras"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            mensaje?.let { aviso ->

                item {

                    AvisoMensaje(
                        mensaje = aviso,
                        onCerrar = {

                            mensaje = null
                        }
                    )
                }
            }

            error?.let { aviso ->

                item {

                    AvisoMensaje(
                        mensaje = aviso,
                        onCerrar = {

                            error = null
                        }
                    )
                }
            }

            item {

                TarjetaImpresoraConfigurada(
                    configuracion = configuracion,
                    conectada =
                        BluetoothPrinterManager.conectado &&
                            BluetoothPrinterManager
                                .dispositivoConectado?.address ==
                            configuracion?.mac
                )
            }

            item {

                Button(
                    onClick = {

                        imprimiendoPrueba = true
                        scope.launch {

                            val resultado =
                                repositorio.imprimirPrueba()
                            imprimiendoPrueba = false
                            mostrarResultado(resultado)
                        }
                    },
                    enabled =
                        configuracion != null && !imprimiendoPrueba,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    if (imprimiendoPrueba) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color =
                                MaterialTheme.colorScheme.onPrimary
                        )

                    } else {

                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.size(8.dp)
                        )

                        Text("Probar impresión")
                    }
                }
            }

            item {

                Text(
                    text = "Dispositivos emparejados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            when {

                emparejadas.isEmpty() -> {

                    item {

                        EmptyContent(
                            mensaje =
                                "No hay impresoras Bluetooth " +
                                    "emparejadas",
                            icono = Icons.AutoMirrored.Filled.BluetoothSearching
                        )
                    }
                }

                else -> {

                    items(
                        items = emparejadas,
                        key = { it.address }
                    ) { dispositivo ->

                        TarjetaDispositivo(
                            dispositivo = dispositivo,
                            seleccionada =
                                configuracion?.mac ==
                                    dispositivo.address,
                            conectando =
                                conectandoMac ==
                                    dispositivo.address,
                            onClick = {

                                if (conectandoMac != null) {
                                    return@TarjetaDispositivo
                                }

                                conectandoMac =
                                    dispositivo.address
                                scope.launch {

                                    val resultado =
                                        repositorio
                                            .conectarImpresora(
                                                nombre =
                                                    dispositivo.name,
                                                mac =
                                                    dispositivo.address
                                            )
                                    conectandoMac = null
                                    mostrarResultado(resultado)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaImpresoraConfigurada(
    configuracion: ConfiguracionImpresora?,
    conectada: Boolean
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
                text = "Impresora configurada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (configuracion == null) {

                Text(
                    text = "Sin configurar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Selecciona un dispositivo de la lista " +
                        "para configurarla",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            } else {

                FilaInformacion(
                    icono = Icons.Filled.Print,
                    etiqueta = "Nombre",
                    valor = configuracion.nombre
                )

                FilaInformacion(
                    icono = Icons.Filled.Bluetooth,
                    etiqueta = "Dirección",
                    valor = configuracion.mac
                )

                FilaInformacion(
                    icono = if (conectada) {
                        Icons.Filled.Bluetooth
                    } else {
                        Icons.AutoMirrored.Filled.BluetoothSearching
                    },
                    etiqueta = "Estado",
                    valor = if (conectada) {
                        "Conectada"
                    } else {
                        "Desconectada"
                    }
                )

                FilaInformacion(
                    icono = Icons.Filled.Refresh,
                    etiqueta = "Última conexión",
                    valor = formatearFecha(
                        configuracion.ultimaConexionMs
                    )
                )
            }
        }
    }
}

@Composable
private fun TarjetaDispositivo(
    dispositivo: BluetoothDevice,
    seleccionada: Boolean,
    conectando: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = dispositivo.name.ifBlank { "Impresora" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = dispositivo.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (conectando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

private fun formatearFecha(ms: Long?): String {
    if (ms == null) {
        return "—"
    }

    return SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(ms))
}
