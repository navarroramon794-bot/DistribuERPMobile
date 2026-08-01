package com.distribuerp.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BarraBusqueda(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {

            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null
            )
        },
        placeholder = {

            Text(placeholder)
        }
    )
}
