package com.distribuerp.mobile.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.VersionResponse
import com.distribuerp.mobile.repository.mensajeAmigable
import com.distribuerp.mobile.ui.components.FilaInformacion
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private fun compararVersiones(
    instalada: String,
    disponible: String
): Int {
    val a = instalada.split("-").first()
        .split(".")
        .mapNotNull { parte -> parte.toIntOrNull() }
    val b = disponible.split("-").first()
        .split(".")
        .mapNotNull { parte -> parte.toIntOrNull() }

    val largo = maxOf(a.size, b.size)

    for (i in 0 until largo) {
        val va = a.getOrElse(i) { 0 }
        val vb = b.getOrElse(i) { 0 }

        if (va != vb) {
            return va.compareTo(vb)
        }
    }

    return 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizacionesScreen(
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current

    var consultando by remember {
        mutableStateOf(true)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var version by remember {
        mutableStateOf<VersionResponse?>(null)
    }

    fun consultar() {
        consultando = true
        error = null

        RetrofitClient.api
            .getVersion()
            .enqueue(
                object : Callback<VersionResponse> {

                    override fun onResponse(
                        call: Call<VersionResponse>,
                        response: Response<VersionResponse>
                    ) {
                        consultando = false

                        if (response.isSuccessful) {
                            version = response.body()
                        } else {
                            error = "Error HTTP ${response.code()}"
                        }
                    }

                    override fun onFailure(
                        call: Call<VersionResponse>,
                        t: Throwable
                    ) {
                        consultando = false
                        error = mensajeAmigable(t)
                        Log.e(
                            "ACTUALIZACIONES",
                            "Consulta de versión fallida",
                            t
                        )
                    }
                }
            )
    }

    LaunchedEffect(Unit) {
        consultar()
    }

    val instalada = BuildConfig.APP_VERSION
    val disponible = version?.version.orEmpty()
    val hayNueva = version != null &&
        disponible.isNotBlank() &&
        compararVersiones(instalada, disponible) < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Actualizaciones")
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
                        text = "Versión instalada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    FilaInformacion(
                        icono = Icons.Filled.SystemUpdate,
                        etiqueta = "Versión",
                        valor = "v$instalada"
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Info,
                        etiqueta = "Build",
                        valor = BuildConfig.VERSION_CODE.toString()
                    )

                    FilaInformacion(
                        icono = Icons.Filled.Cloud,
                        etiqueta = "Servidor",
                        valor = "https://distribu-erp.onrender.com/"
                    )
                }
            }

            if (consultando) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text("Consultando el servidor...")
                    }
                }
            }

            error?.let { mensaje ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(
                            onClick = { consultar() },
                            modifier = Modifier.fillMaxWidth()
                        ) {

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

            version?.let { datos ->

                if (hayNueva) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                Text(
                                    text = "Hay una versión nueva disponible",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Se recomienda actualizar para "
                                    + "obtener las últimas mejoras y "
                                    + "correcciones.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                } else {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "Tienes la versión más reciente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
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
                            text = "Versión en el portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        FilaInformacion(
                            icono = Icons.Filled.SystemUpdate,
                            etiqueta = "Disponible",
                            valor = "v${datos.version.orEmpty()}"
                        )

                        datos.build?.let { build ->

                            FilaInformacion(
                                icono = Icons.Filled.Info,
                                etiqueta = "Build",
                                valor = build
                            )
                        }

                        datos.fecha_publicacion
                            ?.takeIf { it.isNotBlank() }
                            ?.let { fecha ->

                                FilaInformacion(
                                    icono = Icons.Filled.Info,
                                    etiqueta = "Publicación",
                                    valor = fecha
                                )
                            }

                        datos.android_min
                            ?.takeIf { it.isNotBlank() }
                            ?.let { minimo ->

                                FilaInformacion(
                                    icono = Icons.Filled.Info,
                                    etiqueta = "Compatibilidad",
                                    valor = minimo
                                )
                            }

                        datos.apk?.tamano_mb?.let { tamano ->

                            FilaInformacion(
                                icono = Icons.Filled.Download,
                                etiqueta = "Tamaño",
                                valor = "$tamano MB"
                            )
                        }

                        datos.apk?.sha256?.let { sha ->

                            FilaInformacion(
                                icono = Icons.Filled.Info,
                                etiqueta = "SHA-256",
                                valor = sha.take(16) + "…"
                            )
                        }
                    }
                }

                if (datos.novedades.isNotEmpty()) {

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
                                text = "Novedades",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            datos.novedades.forEach { novedad ->

                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {

                                    Text(
                                        text = "• ",
                                        style =
                                            MaterialTheme.typography.bodyMedium,
                                        color =
                                            MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = novedad,
                                        style =
                                            MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Text(
                            text = "Sin instalación automática",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Para actualizar, abre la página de "
                                + "descarga en tu navegador, descarga el "
                                + "APK y ábrelo para instalarlo "
                                + "manualmente. Tus datos se conservan "
                                + "al instalar sobre la versión actual.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (datos.apk_disponible &&
                            !datos.descarga_url.isNullOrBlank()
                        ) {

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                datos.descarga_url
                                            )
                                        )
                                        contexto.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.e(
                                            "ACTUALIZACIONES",
                                            "Sin navegador disponible",
                                            e
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.size(8.dp)
                                )

                                Text("Abrir página de descarga")
                            }
                        }
                    }
                }
            }
        }
    }
}
