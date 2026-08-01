package com.distribuerp.mobile.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDeleteDialog(
    titulo: String,
    mensaje: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {

            Text(titulo)
        },
        text = {

            Text(mensaje)
        },
        confirmButton = {

            TextButton(
                onClick = onConfirmar
            ) {

                Text("Eliminar")
            }
        },
        dismissButton = {

            TextButton(
                onClick = onCancelar
            ) {

                Text("Cancelar")
            }
        }
    )
}
