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
import com.distribuerp.mobile.models.ClienteRequest
import com.distribuerp.mobile.ui.components.AvisoMensaje
import com.distribuerp.mobile.ui.components.LoadingContent
import com.distribuerp.mobile.viewmodel.ClienteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormScreen(
    clienteId: Int?,
    viewModel: ClienteViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val esEdicion = clienteId != null
    val clienteActual = viewModel.clienteActual
    val cargando = viewModel.cargandoDetalle
    val guardando = viewModel.guardando
    val mensaje = viewModel.mensaje

    var yaPrefill by remember {
        mutableStateOf(false)
    }

    var nombre by remember {
        mutableStateOf("")
    }

    var direccion by remember {
        mutableStateOf("")
    }

    var telefono by remember {
        mutableStateOf("")
    }

    var limiteCredito by remember {
        mutableStateOf("")
    }

    LaunchedEffect(clienteId) {

        if (
            clienteId != null
            && viewModel.clienteActual?.id != clienteId
        ) {

            viewModel.cargarCliente(clienteId)
        }
    }

    LaunchedEffect(clienteActual) {

        val cliente = clienteActual

        if (
            !yaPrefill
            && cliente != null
            && cliente.id == clienteId
        ) {

            nombre = cliente.nombre
            direccion = cliente.direccion ?: ""
            telefono = cliente.telefono ?: ""
            limiteCredito = if (cliente.limite_credito > 0) {
                java.lang.String.format(
                    java.util.Locale.US,
                    "%.2f",
                    cliente.limite_credito
                )
            } else {
                ""
            }
            yaPrefill = true
        }
    }

    val puedeGuardar = nombre.isNotBlank() && !guardando

    fun guardar() {

        val limite = limiteCredito.toDoubleOrNull()
            ?: 0.0

        val request = ClienteRequest(
            nombre = nombre.trim(),
            direccion = direccion.trim().ifEmpty {
                null
            },
            telefono = telefono.trim().ifEmpty {
                null
            },
            limite_credito = limite
        )

        viewModel.guardarCliente(
            id = clienteId,
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
                            "Editar Cliente"
                        } else {
                            "Nuevo Cliente"
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

            if (cargando && clienteActual == null) {

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
                        value = direccion,
                        onValueChange = {
                            direccion = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Dirección")
                        }
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
                        value = limiteCredito,
                        onValueChange = {
                            limiteCredito = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Límite de crédito")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
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
                                    "Crear Cliente"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
