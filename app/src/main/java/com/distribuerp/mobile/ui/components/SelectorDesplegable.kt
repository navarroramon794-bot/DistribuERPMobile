package com.distribuerp.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    seleccionado: T?,
    opciones: List<T>,
    textoDe: (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = {

            expandido = it
        }
    ) {

        OutlinedTextField(
            value = seleccionado?.let(textoDe) ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ),
            label = {
                Text(etiqueta)
            },
            trailingIcon = {

                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expandido
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = {

                expandido = false
            }
        ) {

            opciones.forEach { opcion ->

                DropdownMenuItem(
                    text = {
                        Text(textoDe(opcion))
                    },
                    onClick = {

                        expandido = false
                        onSeleccionar(opcion)
                    }
                )
            }
        }
    }
}
