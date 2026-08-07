package com.distribuerp.mobile.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.distribuerp.mobile.models.ProductoRequest
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.ui.components.SelectorDesplegable
import com.distribuerp.mobile.viewmodel.ProductoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(
    productoId: Int?,
    viewModel: ProductoViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val esEdicion = productoId != null
    val productoActual = viewModel.productoActual
    val cargando = viewModel.cargandoDetalle
    val guardando = viewModel.guardando
    val mensaje = viewModel.mensaje

    var yaPrefill by remember {
        mutableStateOf(false)
    }

    var codigo by remember {
        mutableStateOf("")
    }

    var nombre by remember {
        mutableStateOf("")
    }

    var descripcion by remember {
        mutableStateOf("")
    }

    var precio by remember {
        mutableStateOf("")
    }

    var existencia by remember {
        mutableStateOf("")
    }

    var codigoBarras by remember {
        mutableStateOf("")
    }

    var tipoCodigo by remember {
        mutableStateOf("EAN-13")
    }

    val unidadesVenta = listOf(
        "kg", "g", "pza", "caja", "paquete", "bolsa",
        "costal", "litro", "ml", "botella", "cubeta",
        "charola", "otro"
    )

    var unidadVenta by remember {
        mutableStateOf("kg")
    }

    LaunchedEffect(productoId) {

        if (
            productoId != null
            && viewModel.productoActual?.id != productoId
        ) {

            viewModel.cargarProducto(productoId)
        }
    }

    LaunchedEffect(productoActual) {

        val producto = productoActual

        if (
            !yaPrefill
            && producto != null
            && producto.id == productoId
        ) {

            codigo = producto.codigo
            nombre = producto.nombre
            descripcion = producto.descripcion ?: ""
            precio = if (producto.precio > 0) {
                java.lang.String.format(
                    java.util.Locale.US,
                    "%.2f",
                    producto.precio
                )
            } else {
                ""
            }
            existencia = if (producto.existencia > 0) {
                java.lang.String.format(
                    java.util.Locale.US,
                    "%.2f",
                    producto.existencia
                )
            } else {
                ""
            }
            codigoBarras = producto.codigo_barras ?: ""
            tipoCodigo = producto.tipo_codigo ?: "EAN-13"
            unidadVenta = producto.unidad_venta.ifBlank { "kg" }
            yaPrefill = true
        }
    }

    val puedeGuardar =
        codigo.isNotBlank() &&
                nombre.isNotBlank() &&
                !guardando

    fun guardar() {

        val request = ProductoRequest(
            codigo = codigo.trim(),
            nombre = nombre.trim(),
            descripcion = descripcion.trim().ifEmpty {
                null
            },
            precio = precio.toDoubleOrNull() ?: 0.0,
            existencia = existencia.toDoubleOrNull() ?: 0.0,
            codigo_barras = codigoBarras.trim().ifEmpty {
                null
            },
            tipo_codigo = tipoCodigo,
            unidad_venta = unidadVenta
        )

        viewModel.guardarProducto(
            id = productoId,
            request = request,
            onExito = onGuardado
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {

                    Text(
                        if (esEdicion) {
                            "Editar Producto"
                        } else {
                            "Nuevo Producto"
                        }
                    )
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            if (cargando && productoActual == null) {

                LoadingContent()

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = {
                            codigo = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Código *")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Nombre *")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = {
                            descripcion = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Descripción")
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = precio,
                        onValueChange = {
                            precio = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Precio")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = existencia,
                        onValueChange = {
                            existencia = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Existencia")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = codigoBarras,
                        onValueChange = {
                            codigoBarras = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Código de Barras (opcional)")
                        },
                        placeholder = {
                            Text("EAN-13")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    SelectorDesplegable(
                        etiqueta = "Tipo de Código",
                        seleccionado = tipoCodigo,
                        opciones = listOf("EAN-13"),
                        textoDe = { it },
                        onSeleccionar = {
                            tipoCodigo = it
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    SelectorDesplegable(
                        etiqueta = "Unidad de Venta",
                        seleccionado = unidadVenta,
                        opciones = unidadesVenta,
                        textoDe = { it },
                        onSeleccionar = {
                            unidadVenta = it
                        }
                    )

                    mensaje?.let { texto ->

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        AvisoMensaje(
                            mensaje = texto,
                            onCerrar = {

                                viewModel.limpiarMensaje()
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = {
                            guardar()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = puedeGuardar
                    ) {

                        if (guardando) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                        } else {

                            Text(
                                if (esEdicion) {
                                    "Guardar Cambios"
                                } else {
                                    "Crear Producto"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
