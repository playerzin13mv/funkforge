package com.funkforge.ui

import android.app.Activity
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.funkforge.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Forja secreta: escuro, olhos brilhando e a música; quando acaba, o app fecha. */
@Composable
fun SecretScreen() {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.6f) }
    val flick by rememberInfiniteTransition(label = "eyes").animateFloat(
        0.65f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "flick"
    )

    DisposableEffect(Unit) {
        val mp = MediaPlayer.create(ctx, R.raw.freddys_music_box)
        mp?.setOnCompletionListener { (ctx as? Activity)?.finishAndRemoveTask() }
        mp?.start()
        onDispose { mp?.release() }
    }
    LaunchedEffect(Unit) {
        delay(2500); show = true
        scale.animateTo(1.8f, tween(40000, easing = LinearEasing))
    }
    LaunchedEffect(show) {
        while (show) {
            delay(2500L + Random.nextLong(3000))
            visible = false; delay(160); visible = true
        }
    }

    Canvas(Modifier.fillMaxSize().background(Color.Black)) {
        if (show && visible) {
            val s = scale.value
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            val gap = size.width * 0.11f * s
            val r = size.width * 0.032f * s
            for (dx in listOf(-gap, gap)) {
                val c = Offset(cx + dx, cy)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f * flick), Color.Transparent), center = c, radius = r * 4.5f),
                    radius = r * 4.5f, center = c
                )
                drawCircle(Color.White.copy(alpha = flick), r, c)
                drawCircle(Color.Black, r * 0.3f, c)
            }
        }
    }
}
