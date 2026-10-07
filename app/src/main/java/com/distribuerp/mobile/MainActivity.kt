package com.distribuerp.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.navigation.NavGraph
import com.distribuerp.mobile.network.ConectividadGlobal
import com.distribuerp.mobile.sync.SyncManager
import com.distribuerp.mobile.ui.theme.DistribuERPMobileTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Primero la conectividad: a partir de aqui `ConectividadGlobal.estaOnline()`
        // refleja la red real, que es lo que decide el enrutamiento de la venta.
        ConectividadGlobal.iniciar(this)

        // La cookie de una sesion anterior se restaura cifrada desde disco ANTES
        // de encolar sync ni montar la UI: sin ella el arranque offline no tiene
        // razon para cerrar la sesion ni para enviar nada sin autenticar.
        RetrofitClient.iniciar(this)

        // Retoma lo que quedo pendiente en la Outbox de una ejecucion anterior.
        // Es idempotente: si no hay nada pendiente no encola nada.
        lifecycleScope.launch {
            RetrofitClient.restaurarCookies()
            SyncManager.programarSiHayPendientes(this@MainActivity)
        }

        setContent {
            DistribuERPMobileTheme {
                NavGraph()
            }
        }
    }
}
