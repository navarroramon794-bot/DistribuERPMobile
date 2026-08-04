package com.distribuerp.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.distribuerp.mobile.screens.ActivacionScreen
import com.distribuerp.mobile.screens.ActivarLicenciaScreen
import com.distribuerp.mobile.screens.DemoTerminadaScreen
import com.distribuerp.mobile.screens.LicenciaBloqueadaScreen
import com.distribuerp.mobile.viewmodel.EstadoLicencia
import com.distribuerp.mobile.viewmodel.LicenciaViewModel

private const val MODO_PRINCIPAL = "principal"
private const val MODO_CODIGO = "codigo"

/** Puerta de licencia: activación, demo terminada y bloqueos. */
@Composable
fun PuertaLicencia(
    viewModel: LicenciaViewModel,
    estado: EstadoLicencia,
    mensaje: String?
) {
    var modoActivacion by rememberSaveable {
        mutableStateOf(MODO_PRINCIPAL)
    }

    when (estado) {

        EstadoLicencia.DemoExpirada -> {
            DemoTerminadaScreen(
                onActivar = {
                    modoActivacion = MODO_CODIGO
                    viewModel.irAActivacion()
                }
            )
        }

        EstadoLicencia.SinLicencia -> {
            when (modoActivacion) {

                MODO_CODIGO -> {
                    ActivarLicenciaScreen(
                        cargando = viewModel.cargando,
                        mensaje = mensaje,
                        onVolver = {
                            modoActivacion = MODO_PRINCIPAL
                            viewModel.limpiarMensaje()
                        },
                        onActivar = { codigo ->
                            viewModel.activar(codigo)
                        }
                    )
                }

                else -> {
                    ActivacionScreen(
                        cargando = viewModel.cargando,
                        mensaje = mensaje,
                        onActivar = {
                            modoActivacion = MODO_CODIGO
                            viewModel.limpiarMensaje()
                        },
                        onProbarDemo = {
                            viewModel.solicitarDemo()
                        }
                    )
                }
            }
        }

        else -> {
            LicenciaBloqueadaScreen(
                estado = estado,
                mensaje = mensaje,
                onReintentar = {
                    viewModel.validar()
                },
                onActivar = {
                    modoActivacion = MODO_CODIGO
                    viewModel.irAActivacion()
                }
            )
        }
    }
}
