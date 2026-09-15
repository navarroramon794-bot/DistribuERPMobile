package com.distribuerp.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distribuerp.mobile.BuildConfig
import com.distribuerp.mobile.ui.theme.LoginBackground
import com.distribuerp.mobile.ui.theme.LoginBorder
import com.distribuerp.mobile.ui.theme.LoginBorderFocus
import com.distribuerp.mobile.ui.theme.LoginCardBorder
import com.distribuerp.mobile.ui.theme.LoginCardGlow
import com.distribuerp.mobile.ui.theme.LoginError
import com.distribuerp.mobile.ui.theme.LoginPrimary
import com.distribuerp.mobile.ui.theme.LoginSurface
import com.distribuerp.mobile.ui.theme.LoginSurfaceVariant
import com.distribuerp.mobile.ui.theme.LoginTextPrimary
import com.distribuerp.mobile.ui.theme.LoginTextSecondary
import com.distribuerp.mobile.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val loginUiState = viewModel.loginUiState

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? androidx.activity.ComponentActivity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            window.statusBarColor = android.graphics.Color.parseColor("#0B1120")
        }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = LoginPrimary,
            onPrimary = LoginTextPrimary,
            surface = LoginSurface,
            onSurface = LoginTextPrimary,
            background = LoginBackground,
            onBackground = LoginTextPrimary,
            error = LoginError
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LoginBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(LoginCardGlow.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(LoginSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = LoginPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Distribu")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                color = LoginPrimary,
                                letterSpacing = 1.sp
                            )
                        ) {
                            append("ERP")
                        }
                        append(" Mobile")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = LoginTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Versión ${BuildConfig.APP_VERSION}",
                    style = MaterialTheme.typography.labelMedium,
                    color = LoginTextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = LoginSurface,
                    border = BorderStroke(1.dp, LoginCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Iniciar Sesión",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = LoginTextPrimary
                        )

                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Correo") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = LoginTextSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LoginTextPrimary,
                                unfocusedTextColor = LoginTextPrimary,
                                focusedBorderColor = LoginBorderFocus,
                                unfocusedBorderColor = LoginBorder,
                                cursorColor = LoginPrimary,
                                focusedLabelColor = LoginPrimary,
                                unfocusedLabelColor = LoginTextSecondary,
                                focusedLeadingIconColor = LoginPrimary,
                                unfocusedLeadingIconColor = LoginTextSecondary,
                                focusedContainerColor = LoginSurfaceVariant,
                                unfocusedContainerColor = LoginSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = LoginTextSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                        tint = LoginTextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LoginTextPrimary,
                                unfocusedTextColor = LoginTextPrimary,
                                focusedBorderColor = LoginBorderFocus,
                                unfocusedBorderColor = LoginBorder,
                                cursorColor = LoginPrimary,
                                focusedLabelColor = LoginPrimary,
                                unfocusedLabelColor = LoginTextSecondary,
                                focusedLeadingIconColor = LoginPrimary,
                                unfocusedLeadingIconColor = LoginTextSecondary,
                                focusedTrailingIconColor = LoginTextSecondary,
                                unfocusedTrailingIconColor = LoginTextSecondary,
                                focusedContainerColor = LoginSurfaceVariant,
                                unfocusedContainerColor = LoginSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.login(
                                    correo = correo,
                                    password = password
                                )
                            },
                            enabled = !loginUiState.enviando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginPrimary,
                                contentColor = LoginTextPrimary,
                                disabledContainerColor = LoginPrimary.copy(alpha = 0.5f),
                                disabledContentColor = LoginTextPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (loginUiState.enviando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = LoginTextPrimary
                                )
                            } else {
                                Text(
                                    text = "Iniciar Sesión",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (loginUiState.mensaje.isNotBlank()) {
                            val isError = loginUiState.mensaje.contains("❌") ||
                                loginUiState.mensaje.contains("Error") ||
                                loginUiState.mensaje.contains("inválid")

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isError) LoginError.copy(alpha = 0.08f) else LoginSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isError) LoginError.copy(alpha = 0.25f) else LoginBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (isError) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Error,
                                                contentDescription = null,
                                                tint = LoginError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Credenciales inválidas",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoginError
                                            )
                                        }
                                    }
                                    Text(
                                        text = loginUiState.mensaje,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoginTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = viewModel.pingEstado,
                    style = MaterialTheme.typography.bodySmall,
                    color = LoginTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "© 2026 DistribuERP · Todos los derechos reservados",
                    style = MaterialTheme.typography.labelSmall,
                    color = LoginTextSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}
