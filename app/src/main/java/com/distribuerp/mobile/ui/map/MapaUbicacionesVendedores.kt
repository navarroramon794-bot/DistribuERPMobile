package com.distribuerp.mobile.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.distribuerp.mobile.models.Ubicacion
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

const val LATITUD_MIN = -90.0
const val LATITUD_MAX = 90.0
const val LONGITUD_MIN = -180.0
const val LONGITUD_MAX = 180.0
const val ZOOM_PUNTO_UNICO = 15.0

fun esCoordenadaValida(latitud: Double, longitud: Double): Boolean {
    val latOk = latitud in LATITUD_MIN..LATITUD_MAX
    val lonOk = longitud in LONGITUD_MIN..LONGITUD_MAX
    val origen = latitud == 0.0 && longitud == 0.0
    return latOk && lonOk && !origen
}

@Composable
fun MapaUbicacionesVendedores(
    ubicaciones: List<Ubicacion>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ubicacionesActuales = rememberUpdatedState(ubicaciones)

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            controller.setZoom(4.0)
        }
    }

    // Markers por identificador de vendedor para reutilizarlos y no recrearlos
    // en cada actualización (evita el parpadeo).
    val markersPorVendedor = remember { mutableStateMapOf<Int, Marker>() }
    // La cámara solo debe centrarse una vez, al primer conjunto de datos.
    // A partir de ahí se respeta la posición manual del administrador.
    var centradoInicial by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            markersPorVendedor.clear()
            mapView.onDetach()
        }
    }

    LaunchedEffect(ubicaciones) {
        val lista = ubicacionesActuales.value

        val puntosValidos = lista.filter {
            esCoordenadaValida(it.latitud, it.longitud)
        }

        val idsActuales = puntosValidos
            .mapNotNull { it.vendedor_id }
            .toSet()

        // Quitar markers de vendedores que ya no están en la lista.
        markersPorVendedor.keys.toList().forEach { id ->
            if (id !in idsActuales) {
                mapView.overlays.remove(markersPorVendedor[id])
                markersPorVendedor.remove(id)
            }
        }

        // Añadir o actualizar markers sin recrear los existentes.
        val puntos = mutableListOf<GeoPoint>()
        puntosValidos.forEach { ubicacion ->
            val id = ubicacion.vendedor_id ?: return@forEach
            val geo = GeoPoint(ubicacion.latitud, ubicacion.longitud)
            puntos.add(geo)

            val marker = markersPorVendedor[id]
            if (marker == null) {
                val nuevo = Marker(mapView).apply {
                    position = geo
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = ubicacion.vendedor
                        ?: "Vendedor ${ubicacion.vendedor_id}"
                    snippet = buildString {
                        append(
                            "Lat: %.6f, Lon: %.6f".format(
                                ubicacion.latitud,
                                ubicacion.longitud
                            )
                        )
                        ubicacion.precision_m?.let {
                            append("\nPrecisión: %.1f m".format(it))
                        }
                        ubicacion.fecha?.let {
                            append("\nÚltima actualización: $it")
                        }
                    }
                }
                markersPorVendedor[id] = nuevo
                mapView.overlays.add(nuevo)
            } else {
                marker.position = geo
            }
        }

        // Centrar la cámara únicamente la primera vez que hay vendedores.
        if (!centradoInicial && puntos.isNotEmpty()) {
            if (puntos.size == 1) {
                mapView.controller.setZoom(ZOOM_PUNTO_UNICO)
                mapView.controller.animateTo(puntos.first())
            } else {
                val bounds = BoundingBox.fromGeoPoints(puntos)
                mapView.zoomToBoundingBox(bounds, true, 120)
            }
            centradoInicial = true
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
