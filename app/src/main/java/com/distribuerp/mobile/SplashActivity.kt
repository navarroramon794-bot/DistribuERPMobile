package com.distribuerp.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SplashActivity : ComponentActivity() {
    private val poppins = FontFamily(Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_medium, FontWeight.Medium), Font(R.font.poppins_semibold, FontWeight.SemiBold), Font(R.font.poppins_bold, FontWeight.Bold))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) { startActivity(Intent(this@SplashActivity, MainActivity::class.java)); finish() }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF062B67), Color(0xFF0B5FC1), Color(0xFF063D8B))))) {
                Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.splash_isotipo), "DistribuERP", Modifier.size(132.dp))
                    Spacer(Modifier.height(28.dp))
                    Row {
                        Text(text = "Distribu", fontFamily = poppins, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = Color.White)
                        Text(text = "ERP", fontFamily = poppins, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = Color(0xFFFF9800))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = "M O B I L E", fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 5.sp, color = Color.White.copy(alpha = 0.92f))
                    Spacer(Modifier.height(30.dp))
                    Text(text = "Gestión comercial para\nequipos de distribución", fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = Color.White.copy(alpha = 0.90f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(44.dp))
                    Text(text = "Ventas   ·   Cobranza   ·   Inventario   ·   GPS", fontFamily = poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
