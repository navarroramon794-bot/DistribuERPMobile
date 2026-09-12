package com.distribuerp.mobile.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distribuerp.mobile.viewmodel.ChangePasswordViewModel

@Composable
fun ChangePasswordScreen(
    obligatorio: Boolean = false,
    viewModel: ChangePasswordViewModel = viewModel(factory = ChangePasswordViewModel.Factory),
    onSuccess: () -> Unit = {}
) {
    val state = viewModel.uiState
    var actualVisible by remember { mutableStateOf(false) }
    var nuevaVisible by remember { mutableStateOf(false) }
    var confVisible by remember { mutableStateOf(false) }

    if (obligatorio) {
        BackHandler(enabled = true) { /* bloquea back */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (obligatorio) "Cambia tu contraseña" else "Cambiar contraseña",
            style = MaterialTheme.typography.headlineSmall
        )
        if (obligatorio) {
            Text(
                text = "Tu contraseña es temporal. Debes cambiarla para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = state.passwordActual,
            onValueChange = viewModel::onActualChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña actual") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { actualVisible = !actualVisible }) {
                    Icon(if (actualVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            singleLine = true,
            visualTransformation = if (actualVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.errorActual != null,
            supportingText = { state.errorActual?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        OutlinedTextField(
            value = state.passwordNueva,
            onValueChange = viewModel::onNuevaChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nueva contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { nuevaVisible = !nuevaVisible }) {
                    Icon(if (nuevaVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            singleLine = true,
            visualTransformation = if (nuevaVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.errorNueva != null,
            supportingText = { state.errorNueva?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        OutlinedTextField(
            value = state.confirmacion,
            onValueChange = viewModel::onConfirmacionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirmar nueva contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { confVisible = !confVisible }) {
                    Icon(if (confVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            singleLine = true,
            visualTransformation = if (confVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.errorConfirmacion != null,
            supportingText = { state.errorConfirmacion?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        if (state.mensaje.isNotBlank() && !state.exito) {
            Text(text = state.mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { viewModel.cambiarPassword(onSuccess) },
            enabled = !state.enviando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.enviando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Cambiar contraseña")
            }
        }

        if (!obligatorio) {
            OutlinedButton(
                onClick = onSuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
