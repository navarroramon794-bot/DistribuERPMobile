package com.distribuerp.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.distribuerp.mobile.navigation.NavGraph
import com.distribuerp.mobile.ui.theme.DistribuERPMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            DistribuERPMobileTheme {
                NavGraph()
            }
        }
    }
}
