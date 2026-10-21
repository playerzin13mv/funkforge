package com.funkforge.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.audio.Playback
import com.funkforge.model.BpmChange
import com.funkforge.model.Chart
import com.funkforge.model.Note
import com.funkforge.model.Project
import com.funkforge.model.lowerBound
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.round

private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
fun EditorScreen(
    project: Project,
    onProject: (Project) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onExportJson: () -> Unit,
    onExportMod: () -> Unit,
    onPickInst: () -> Unit,
    onPickVoices: () -> Unit,
    onPlaytest: () -> Unit
) {
    val ctx = LocalContext.current
    val chart = project.chart
    val onChart: (Chart) -> Unit = { onProject(project.copy(chart = it)) }
    val density = LocalDensity.current.density
    val sprites = rememberSprites()
    val tm = rememberTextMeasurer()
    var scroll by remember { mutableFloatStateOf(0f) }
    var beatPx by remember { mutableFloatStateOf(90f * density) }
    var bpmText by remember(chart.song) { mutableStateOf(fmt(chart.bpmChanges.first().bpm)) }
    var showSettings by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val beatMs = chart.beatMs
    val stepMs = beatMs / 4.0
    val headerPx = 64f * density
    val maxSus = remember(chart.notes) { chart.notes.maxOfOrNull { it.sustainMs } ?: 0.0 }

    val pb = remember { Playback() }
    DisposableEffect(project.instPath, project.voicesPath) {
        pb.load(project.instPath, project.voicesPath)
        onDispose { pb.release() }
    }
    LaunchedEffect(playing) {
        if (playing) {
            pb.play(((scroll / beatPx) * beatMs).toInt().coerceAtLeast(0))
            while (pb.isPlaying) {
                withFrameNanos { }
                scroll = (pb.positionMs / beatMs).toFloat() * beatPx
            }
            playing = false
        } else pb.pause()
    }

    if (showSettings) SettingsDialog(project, false, { showSettings = false }) { onProject(it); showSettings = false }

    Column(Modifier.fillMaxSize().background(Fk.bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(chart.song, color = Fk.cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = onImport) { Icon(Icons.Default.FileOpen, null, tint = Color.White) }
            IconButton(onClick = onExportJson) { Icon(Icons.Default.Save, null, tint = Color.White) }
            IconButton(onClick = onExportMod) { Icon(Icons.Default.Share, null, tint = Fk.pink) }
        }

        Canvas(
            Modifier.weight(1f).fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy -> if (!playing) scroll = (scroll - dy).coerceAtLeast(0f) }
                }
                .pointerInput(chart, scroll, beatPx) {
                    detectTapGestures { p ->
                        if (p.y > headerPx) {
                            val lane = (p.x / (size.width / 8f)).toInt().coerceIn(0, 7)
                            val t = (p.y - headerPx + scroll) / beatPx * beatMs
                            val snapped = (round(t / stepMs) * stepMs).coerceAtLeast(0.0)
                            val existing = chart.notes.firstOrNull { it.lane == lane && abs(it.timeMs - snapped) < stepMs / 2 }
                            onChart(if (existing != null) chart.withoutNote(existing) else chart.withNote(Note(snapped, lane)))
                        }
                    }
                }
        ) {
            val laneW = size.width / 8f
            val ns = min(laneW * 0.92f, 56f * density)
            fun yOf(ms: Double) = headerPx + (ms / beatMs).toFloat() * beatPx - scroll

            clipRect(top = headerPx) {
                for (i in 0 until 8) {
                    drawRect(
                        if (i < 4) Color(0xFF1E0D38) else Color(0xFF241046),
                        Offset(i * laneW, headerPx), Size(laneW, size.height - headerPx)
                    )
                    if (i % 4 != 0) drawLine(Color.White.copy(alpha = 0.05f), Offset(i * laneW, headerPx), Offset(i * laneW, size.height))
                }
                val first = floor(scroll / beatPx).toInt().coerceAtLeast(0)
                val last = ceil((scroll + size.height) / beatPx).toInt()
                for (b in first..last) {
                    val y = headerPx + b * beatPx - scroll
                    val measure = b % 4 == 0
                    drawLine(Color.White.copy(alpha = if (measure) 0.35f else 0.15f), Offset(0f, y), Offset(size.width, y), if (measure) 3f else 1.5f)
                    for (s in 1..3) {
                        val ys = y + s * beatPx / 4f
                        drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, ys), Offset(size.width, ys))
                    }
                    if (measure) drawText(tm, "${b / 4}", Offset(6f, y + 4f), TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp))
                }
                drawLine(Fk.pink, Offset(size.width / 2f, headerPx), Offset(size.width / 2f, size.height), 4f)

                // só desenha as notas visíveis (busca binária) — não trava com milhares de notas
                val msLo = (scroll - ns) / beatPx * beatMs - maxSus
                val msHi = (scroll + size.height - headerPx + ns) / beatPx * beatMs
                val notes = chart.notes
                var i = notes.lowerBound(msLo)
                while (i < notes.size && notes[i].timeMs <= msHi) {
                    val n = notes[i]
                    i++
                    val y = yOf(n.timeMs)
                    val cx = n.lane * laneW + laneW / 2f
                    if (n.sustainMs > 0) {
                        drawRect(Fk.lane[n.lane % 4].copy(alpha = 0.6f), Offset(cx - ns * 0.12f, y), Size(ns * 0.24f, yOf(n.timeMs + n.sustainMs) - y))
                    }
                    drawImage(
                        sprites.notes[n.lane % 4],
                        dstOffset = IntOffset((cx - ns / 2f).toInt(), (y - ns / 2f).toInt()),
                        dstSize = IntSize(ns.toInt(), ns.toInt())
                    )
                }
            }

            drawRect(Fk.bg, Offset(0f, 0f), Size(size.width, headerPx))
            for (i in 0 until 8) {
                val cx = i * laneW + laneW / 2f
                drawImage(
                    sprites.statics[i % 4],
                    dstOffset = IntOffset((cx - ns / 2f).toInt(), ((headerPx - ns) / 2f).toInt()),
                    dstSize = IntSize(ns.toInt(), ns.toInt()),
                    alpha = if (i < 4) 0.7f else 1f
                )
            }
            drawLine(Fk.cyan, Offset(0f, headerPx), Offset(size.width, headerPx), 3f)
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = {
                if (!pb.hasAudio) Toast.makeText(ctx, "Importe a música (Inst) primeiro 🎵", Toast.LENGTH_SHORT).show()
                else playing = !playing
            }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Fk.yellow) }
            IconButton(onClick = onPickInst) { Icon(Icons.Default.MusicNote, null, tint = if (project.instPath != null) Fk.cyan else Color.White) }
            IconButton(onClick = onPickVoices) { Icon(Icons.Default.RecordVoiceOver, null, tint = if (project.voicesPath != null) Fk.cyan else Color.White) }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
            Button(onClick = { playing = false; onPlaytest() }) {
                Icon(Icons.Default.SportsEsports, null); Spacer(Modifier.width(6.dp)); Text("Testar")
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("${chart.notes.size} notas", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = bpmText,
                onValueChange = { s ->
                    bpmText = s
                    val v = s.toDoubleOrNull()
                    if (v != null && v in 20.0..999.0) {
                        onChart(chart.copy(bpmChanges = listOf(BpmChange(0.0, v)) + chart.bpmChanges.drop(1)))
                    }
                },
                label = { Text("BPM (20–999)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(140.dp)
            )
            IconButton(onClick = { beatPx = (beatPx * 0.8f).coerceAtLeast(30f * density) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
            IconButton(onClick = { beatPx = (beatPx * 1.25f).coerceAtMost(300f * density) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    }
}
