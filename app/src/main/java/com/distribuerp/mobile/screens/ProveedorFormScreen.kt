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
import com.distribuerp.mobile.models.ProveedorRequest
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.viewmodel.ProveedorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedorFormScreen(
    proveedorId: Int?,
    viewModel: ProveedorViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val esEdicion = proveedorId != null
    val proveedorActual = viewModel.proveedorActual
    val cargando = viewModel.cargandoDetalle
    val guardando = viewModel.guardando
    val mensaje = viewModel.mensaje

    var yaPrefill by remember {
        mutableStateOf(false)
    }

    var nombre by remember {
        mutableStateOf("")
    }

    var rfc by remember {
        mutableStateOf("")
    }

    var telefono by remember {
        mutableStateOf("")
    }

    var direccion by remember {
        mutableStateOf("")
    }

    LaunchedEffect(proveedorId) {

        if (
            proveedorId != null
            && viewModel.proveedorActual?.id != proveedorId
        ) {

            viewModel.cargarProveedor(proveedorId)
        }
    }

    LaunchedEffect(proveedorActual) {

        val proveedor = proveedorActual

        if (
            !yaPrefill
            && proveedor != null
            && proveedor.id == proveedorId
        ) {

            nombre = proveedor.nombre
            rfc = proveedor.rfc ?: ""
            telefono = proveedor.telefono ?: ""
            direccion = proveedor.direccion ?: ""
            yaPrefill = true
        }
    }

    val puedeGuardar = nombre.isNotBlank() && !guardando

    fun guardar() {

        val request = ProveedorRequest(
            nombre = nombre.trim(),
            rfc = rfc.trim().ifEmpty {
                null
            },
            telefono = telefono.trim().ifEmpty {
                null
            },
            direccion = direccion.trim().ifEmpty {
                null
            }
        )

        viewModel.guardarProveedor(
            id = proveedorId,
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
                            "Editar Proveedor"
                        } else {
                            "Nuevo Proveedor"
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

            if (cargando && proveedorActual == null) {

                LoadingContent()

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

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
                        value = rfc,
                        onValueChange = {
                            rfc = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("RFC")
                        },
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            telefono = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Teléfono")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = direccion,
                        onValueChange = {
                            direccion = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Dirección")
                        },
                        minLines = 2
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
                                    "Crear Proveedor"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
