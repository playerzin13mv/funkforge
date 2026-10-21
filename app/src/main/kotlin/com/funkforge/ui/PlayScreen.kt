package com.funkforge.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.audio.Playback
import com.funkforge.model.Project
import com.funkforge.model.lowerBound
import kotlin.math.abs
import kotlin.math.min

/** Playtest em horizontal: oponente toca sozinho, você toca nas 4 setas da direita. */
@Composable
fun PlayScreen(project: Project, onExit: () -> Unit) {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val act = ctx as? Activity
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    BackHandler { onExit() }

    val chart = project.chart
    val notes = remember(chart) { chart.notes.sortedBy { it.timeMs } }
    val sprites = rememberSprites()
    val pb = remember { Playback() }
    DisposableEffect(Unit) { pb.load(project.instPath, project.voicesPath); onDispose { pb.release() } }

    var runId by remember { mutableIntStateOf(0) }
    val hit = remember(notes, runId) { BooleanArray(notes.size) }
    val missed = remember(notes, runId) { BooleanArray(notes.size) }
    val counts = remember(runId) { IntArray(5) } // sick, good, bad, shit, miss
    val glow = remember(runId) { DoubleArray(8) }
    val held = remember(runId) { BooleanArray(4) }
    val drawPtr = remember(notes, runId) { intArrayOf(0) }
    var songMs by remember { mutableDoubleStateOf(-2500.0) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var health by remember { mutableFloatStateOf(1f) }
    var finished by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(runId) {
        val t0 = withFrameNanos { it }
        var audio = false
        var oppIdx = 0
        var missIdx = 0
        val endMs = (notes.maxOfOrNull { it.timeMs + it.sustainMs } ?: 0.0) + 1500.0
        while (!finished) {
            val ms = (withFrameNanos { it } - t0) / 1_000_000.0 - 2500.0
            songMs = ms
            if (!audio && ms >= 0) { audio = true; pb.play(0) }
            while (oppIdx < notes.size && notes[oppIdx].timeMs <= ms) {
                val n = notes[oppIdx]
                if (n.lane < 4 && !hit[oppIdx]) { hit[oppIdx] = true; glow[n.lane] = ms + 110 }
                oppIdx++
            }
            while (missIdx < notes.size && notes[missIdx].timeMs < ms - 160) {
                val n = notes[missIdx]
                if (n.lane >= 4 && !hit[missIdx]) {
                    hit[missIdx] = true; missed[missIdx] = true
                    counts[4]++; combo = 0; health = (health - 0.06f).coerceAtLeast(0f)
                }
                missIdx++
            }
            if (health <= 0f) { failed = true; finished = true } else if (ms > endMs) finished = true
        }
        pb.pause()
    }

    fun press(lane: Int) {
        if (finished) return
        held[lane] = true
        val t = songMs
        var i = notes.lowerBound(t - 160)
        while (i < notes.size && notes[i].timeMs <= t + 160) {
            if (notes[i].lane == 4 + lane && !hit[i]) {
                val d = abs(notes[i].timeMs - t)
                hit[i] = true
                val k = if (d <= 45) 0 else if (d <= 90) 1 else if (d <= 135) 2 else 3
                counts[k]++
                score += intArrayOf(350, 200, 100, 50)[k]
                combo++
                health = (health + 0.03f).coerceAtMost(2f)
                glow[4 + lane] = t + 110
                break
            }
            i++
        }
    }

    fun restart() {
        pb.pause(); score = 0; combo = 0; health = 1f; failed = false; finished = false; songMs = -2500.0; runId++
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0E0520))) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(runId) {
                awaitPointerEventScope {
                    val down = HashMap<PointerId, Int>()
                    while (true) {
                        val ev = awaitPointerEvent()
                        val laneW = min(size.height * 0.2f, size.width / 9.5f)
                        val xp = (size.width - (8 * laneW + laneW * 0.7f)) / 2f + 4 * laneW + laneW * 0.7f
                        for (c in ev.changes) {
                            if (c.pressed && !c.previousPressed) {
                                if (c.position.x >= xp - laneW * 0.5f) {
                                    val lane = ((c.position.x - xp) / laneW).toInt().coerceIn(0, 3)
                                    press(lane); down[c.id] = lane
                                }
                                c.consume()
                            } else if (!c.pressed && c.previousPressed) {
                                down.remove(c.id)?.let { held[it] = false }
                            }
                        }
                    }
                }
            }
        ) {
            val w = size.width
            val h = size.height
            val laneW = min(h * 0.2f, w / 9.5f)
            val gap = laneW * 0.7f
            val x0 = (w - (8 * laneW + gap)) / 2f
            fun laneX(l: Int) = if (l < 4) x0 + l * laneW else x0 + 4 * laneW + gap + (l - 4) * laneW
            val strumY = h * 0.14f
            val pxMs = 0.45f * chart.speed.toFloat() * (h / 720f)
            val ns = laneW * 0.92f
            val now = songMs
            fun dst(l: Int, cy: Float) = IntOffset((laneX(l) + (laneW - ns) / 2f).toInt(), (cy - ns / 2f).toInt())
            val sz = IntSize(ns.toInt(), ns.toInt())

            // setas paradas / apertando / acertando
            for (l in 0 until 8) {
                val img = when {
                    glow[l] > now -> sprites.confirms[l % 4]
                    l >= 4 && held[l - 4] -> sprites.presses[l % 4]
                    else -> sprites.statics[l % 4]
                }
                drawImage(img, dstOffset = dst(l, strumY), dstSize = sz, alpha = if (l < 4) 0.8f else 1f)
            }

            // notas
            while (drawPtr[0] < notes.size && notes[drawPtr[0]].let { it.timeMs + it.sustainMs } < now - 400) drawPtr[0]++
            var i = drawPtr[0]
            while (i < notes.size) {
                val n = notes[i]
                val y = strumY + ((n.timeMs - now) * pxMs).toFloat()
                if (y > h + ns) break
                val a = if (missed[i]) 0.3f else 1f
                if (n.sustainMs > 0) {
                    val yEnd = strumY + ((n.timeMs + n.sustainMs - now) * pxMs).toFloat()
                    val top = if (hit[i] && !missed[i]) maxOf(y, strumY) else y
                    if (yEnd > top) {
                        drawRect(
                            Fk.lane[n.lane % 4].copy(alpha = 0.55f * a),
                            Offset(laneX(n.lane) + laneW / 2f - ns * 0.12f, top), Size(ns * 0.24f, yEnd - top)
                        )
                    }
                }
                if ((!hit[i] || missed[i]) && y > -ns) {
                    drawImage(sprites.notes[n.lane % 4], dstOffset = dst(n.lane, y), dstSize = sz, alpha = a)
                }
                i++
            }

            // barra de vida
            val bw = w * 0.5f
            val by = h - 22f
            drawRect(Color(0xFFFF3B3B), Offset((w - bw) / 2f, by), Size(bw, 12f))
            drawRect(Color(0xFF3BFF6B), Offset((w - bw) / 2f, by), Size(bw * (health / 2f).coerceIn(0f, 1f), 12f))
        }

        Text(
            "Score $score   Combo $combo   Miss ${counts[4]}",
            color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp)
        )
        if (songMs < 0) {
            Text("${(-songMs / 1000).toInt() + 1}", color = Fk.cyan, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Center))
        }
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Close, null, tint = Color.White) }

        if (finished) {
            Column(
                Modifier.align(Alignment.Center).background(Color(0xE6140826)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (failed) "Você morreu 💀" else "Fim!", color = if (failed) Fk.pink else Fk.cyan, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Score $score", color = Color.White)
                Text("Sick ${counts[0]} • Good ${counts[1]} • Bad ${counts[2]} • Shit ${counts[3]} • Miss ${counts[4]}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { restart() }) { Text("Jogar de novo") }
                    OutlinedButton(onClick = onExit) { Text("Voltar") }
                }
            }
        }
    }
}
