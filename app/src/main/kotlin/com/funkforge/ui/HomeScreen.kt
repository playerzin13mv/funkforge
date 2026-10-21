package com.funkforge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

private data class Particle(val x: Float, val speed: Int, val phase: Float, val lane: Int, val size: Int)

@Composable
fun HomeScreen(onNew: () -> Unit, onImport: () -> Unit, onSoon: (String) -> Unit) {
    val sprites = rememberSprites()
    val density = LocalDensity.current.density
    val parts = remember { List(18) { Particle(Random.nextFloat(), 1 + Random.nextInt(3), Random.nextFloat(), Random.nextInt(4), 34 + Random.nextInt(32)) } }
    val inf = rememberInfiniteTransition(label = "home")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "fall")
    val beat by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "beat")
    var taps by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Fk.bg, Fk.bg2, Fk.bg)))) {
        Canvas(Modifier.fillMaxSize()) {
            for (p in parts) {
                val s = (p.size * density).toInt()
                val prog = (t * p.speed + p.phase) % 1f
                val y = size.height - prog * (size.height + s)
                val x = p.x * (size.width - s)
                drawImage(
                    sprites.notes[p.lane],
                    dstOffset = IntOffset(x.toInt(), y.toInt()),
                    dstSize = IntSize(s, s),
                    alpha = 0.22f
                )
            }
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val active = (beat * 4f).toInt().coerceIn(0, 3)
                for (i in 0..3) {
                    Image(
                        bitmap = sprites.notes[i],
                        contentDescription = null,
                        modifier = Modifier.size(58.dp).scale(if (i == active) 1.25f else 1f)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "FunkForge",
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = Brush.horizontalGradient(listOf(Fk.cyan, Fk.pink)),
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.clickable {
                    taps++
                    if (taps == 7) { taps = 0; onSoon("🔥 Você acendeu a forja secreta... (em breve)") }
                }
            )
            Text("Faça mods de Psych Engine pelo celular", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Spacer(Modifier.height(32.dp))

            MenuCard(Icons.Default.Add, "Novo mod", "Comece um chart do zero", Fk.pink, onNew)
            Spacer(Modifier.height(12.dp))
            MenuCard(Icons.Default.FileOpen, "Importar chart", "Psych Engine (.json)", Fk.cyan, onImport)
            Spacer(Modifier.height(12.dp))
            MenuCard(Icons.Default.SwapHoriz, "Conversor Funky Maker", "Em breve", Fk.lane[2]) { onSoon("Em breve! Preciso de um mod de exemplo do Funky Maker 😉") }
            Spacer(Modifier.height(12.dp))
            MenuCard(Icons.Default.Star, "???", "Algo secreto vem aí", Fk.yellow) { onSoon("🤫 Segredo...") }

            Spacer(Modifier.height(28.dp))
            Text("v0.1 • feito no celular 📱", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.5.dp, accent.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = accent) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}
