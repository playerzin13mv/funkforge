package com.funkforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFF2D95))) { Home() }
        }
    }
}

@Composable
fun Home() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF1A0B2E)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FunkForge", style = MaterialTheme.typography.displaySmall, color = Color(0xFF00E5FF))
        Spacer(Modifier.height(8.dp))
        Text("Faça mods de Psych Engine pelo celular", color = Color.White)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {}) { Text("Novo mod") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {}) { Text("Importar chart") }
    }
}
