package com.funkforge.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

private enum class EditorTool {
    Draw,
    Select,
    Erase
}

private fun editorNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private fun editorTime(ms: Double): String {
    val total = (ms.coerceAtLeast(0.0) / 1000.0).toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
private fun EditorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .background(
                if (selected) Fk.pink.copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(10.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = when {
                !enabled -> Color.Gray
                selected -> Fk.pink
                else -> Color.White
            }
        )
    }
}

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
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val sprites = rememberSprites()
    val textMeasurer = rememberTextMeasurer()

    val chart = project.chart
    val beatMs = chart.beatMs.coerceAtLeast(1.0)

    var scroll by remember { mutableFloatStateOf(0f) }
    var beatPx by remember(density) {
        mutableFloatStateOf(90f * density)
    }

    var snapDivision by remember {
        mutableIntStateOf(4)
    }

    var tool by remember {
        mutableStateOf(EditorTool.Draw)
    }

    var selectedNote by remember {
        mutableStateOf<Note?>(null)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showProperties by remember {
        mutableStateOf(false)
    }

    var playing by remember {
        mutableStateOf(false)
    }

    var bpmText by remember(chart.song) {
        mutableStateOf(
            editorNumber(chart.bpmChanges.first().bpm)
        )
    }

    var speedText by remember(chart.song) {
        mutableStateOf(editorNumber(chart.speed))
    }

    val undoStack = remember {
        mutableStateListOf<Chart>()
    }

    val redoStack = remember {
        mutableStateListOf<Chart>()
    }

    val stepMs = beatMs / snapDivision
    val headerPx = 68f * density

    val maxSustain = remember(chart.notes) {
        chart.notes.maxOfOrNull { it.sustainMs } ?: 0.0
    }

    val playback = remember(
        context,
        project.instPath,
        project.voicesPath
    ) {
        Playback(context)
    }

    var audioLoaded by remember(playback) {
        mutableStateOf(false)
    }

    DisposableEffect(playback) {
        audioLoaded = playback.load(
            project.instPath,
            project.voicesPath
        )

        onDispose {
            playback.release()
        }
    }

    fun notify(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun updateChart(next: Chart) {
        if (next == chart) return

        undoStack.add(chart)

        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }

        redoStack.clear()
        onProject(project.copy(chart = next))
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val previous = undoStack.removeAt(
            undoStack.lastIndex
        )

        redoStack.add(chart)
        selectedNote = null
        onProject(project.copy(chart = previous))
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val next = redoStack.removeAt(
            redoStack.lastIndex
        )

        undoStack.add(chart)
        selectedNote = null
        onProject(project.copy(chart = next))
    }

    fun replaceNote(old: Note, new: Note) {
        val index = chart.notes.indexOf(old)
        if (index < 0) {
            selectedNote = null
            return
        }

        val updated = chart.notes.toMutableList()
        updated[index] = new

        updateChart(
            chart.copy(
                notes = updated.sortedBy { it.timeMs }
            )
        )

        selectedNote = new
    }

    fun removeSelected() {
        val note = selectedNote ?: return

        updateChart(chart.withoutNote(note))
        selectedNote = null
    }

    fun moveSelected(deltaMs: Double) {
        val note = selectedNote ?: return

        replaceNote(
            note,
            note.copy(
                timeMs = (
                    note.timeMs + deltaMs
                ).coerceAtLeast(0.0)
            )
        )
    }

    fun changeSustain(deltaMs: Double) {
        val note = selectedNote ?: return

        replaceNote(
            note,
            note.copy(
                sustainMs = (
                    note.sustainMs + deltaMs
                ).coerceAtLeast(0.0)
            )
        )
    }

    fun pausePreview() {
        playing = false
        playback.pause()
    }

    LaunchedEffect(playing, playback) {
        if (playing) {
            if (!audioLoaded) {
                playing = false
                return@LaunchedEffect
            }

            playback.play(
                (
                    scroll / beatPx * beatMs
                ).toInt().coerceAtLeast(0)
            )

            while (playing && playback.isPlaying) {
                withFrameNanos { }

                playback.synchronize()

                scroll = (
                    playback.positionMs / beatMs
                ).toFloat() * beatPx
            }

            playing = false
        } else {
            playback.pause()
        }
    }

    if (showSettings) {
        SettingsDialog(
            project,
            false,
            { showSettings = false }
        ) {
            onProject(it)
            showSettings = false
        }
    }

    if (showProperties) {
        AlertDialog(
            onDismissRequest = {
                showProperties = false
            },
            title = {
                Text("Chart Properties")
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = bpmText,
                        onValueChange = {
                            bpmText = it
                        },
                        label = {
                            Text("BPM")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    OutlinedTextField(
                        value = speedText,
                        onValueChange = {
                            speedText = it
                        },
                        label = {
                            Text("Scroll Speed")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Text(
                        "Song: ${chart.song}",
                        color = Fk.cyan
                    )

                    Text(
                        "Notes: ${chart.notes.size}",
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bpm = bpmText.toDoubleOrNull()
                        val speed = speedText.toDoubleOrNull()

                        if (
                            bpm == null ||
                            bpm !in 20.0..999.0 ||
                            speed == null ||
                            speed !in 0.1..10.0
                        ) {
                            notify("Invalid BPM or speed")
                        } else {
                            updateChart(
                                chart.copy(
                                    bpmChanges = listOf(
                                        BpmChange(0.0, bpm)
                                    ) + chart.bpmChanges.drop(1),
                                    speed = speed
                                )
                            )

                            showProperties = false
                        }
                    }
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProperties = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fk.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF190C2D))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorAction(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back"
            ) {
                pausePreview()
                onBack()
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chart.song.uppercase(),
                    color = Fk.cyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1
                )

                Text(
                    text = "FUNKFORGE • CHART EDITOR",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }

            EditorAction(
                Icons.Default.Undo,
                "Undo",
                enabled = undoStack.isNotEmpty()
            ) {
                undo()
            }

            Spacer(Modifier.width(4.dp))

            EditorAction(
                Icons.Default.Redo,
                "Redo",
                enabled = redoStack.isNotEmpty()
            ) {
                redo()
            }

            Spacer(Modifier.width(4.dp))

            EditorAction(
                Icons.Default.Settings,
                "Project Settings"
            ) {
                showSettings = true
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(6.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorAction(
                Icons.Default.Edit,
                "Draw",
                selected = tool == EditorTool.Draw
            ) {
                tool = EditorTool.Draw
            }

            EditorAction(
                Icons.Default.TouchApp,
                "Select",
                selected = tool == EditorTool.Select
            ) {
                tool = EditorTool.Select
            }

            EditorAction(
                Icons.Default.Delete,
                "Erase",
                selected = tool == EditorTool.Erase
            ) {
                tool = EditorTool.Erase
            }

            VerticalDivider(
                modifier = Modifier.height(28.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            Text(
                "SNAP",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )

            listOf(4, 8, 16, 32).forEach { division ->
                Box(
                    modifier = Modifier
                        .background(
                            if (snapDivision == division) {
                                Fk.pink
                            } else {
                                Color(0xFF2C1945)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            snapDivision = division
                        }
                        .padding(
                            horizontal = 10.dp,
                            vertical = 9.dp
                        )
                ) {
                    Text(
                        text = "1/$division",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(28.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            EditorAction(
                Icons.Default.Remove,
                "Zoom Out"
            ) {
                beatPx = (
                    beatPx * 0.8f
                ).coerceAtLeast(28f * density)
            }

            Text(
                "${(beatPx / density).toInt()}px",
                color = Fk.cyan,
                fontSize = 11.sp
            )

            EditorAction(
                Icons.Default.Add,
                "Zoom In"
            ) {
                beatPx = (
                    beatPx * 1.25f
                ).coerceAtMost(300f * density)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF130923))
                .padding(
                    horizontal = 12.dp,
                    vertical = 5.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                "BEAT ${editorNumber(scroll / beatPx.toDouble())}",
                color = Fk.cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                editorTime(
                    scroll / beatPx.toDouble() * beatMs
                ),
                color = Color.White,
                fontSize = 12.sp
            )

            Text(
                "BPM ${editorNumber(chart.bpmChanges.first().bpm)}",
                color = Fk.yellow,
                fontSize = 11.sp
            )

            Text(
                "${chart.notes.size} NOTES",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(playing) {
                    detectVerticalDragGestures { _, delta ->
                        if (!playing) {
                            scroll = (
                                scroll - delta
                            ).coerceAtLeast(0f)
                        }
                    }
                }
                .pointerInput(
                    chart,
                    scroll,
                    beatPx,
                    snapDivision,
                    tool
                ) {
                    detectTapGestures { position ->
                        if (
                            playing ||
                            position.y <= headerPx
                        ) return@detectTapGestures

                        val laneWidth = size.width / 8f

                        val lane = (
                            position.x / laneWidth
                        ).toInt().coerceIn(0, 7)

                        val time = (
                            position.y -
                            headerPx +
                            scroll
                        ) / beatPx * beatMs

                        val snapped = (
                            round(time / stepMs) * stepMs
                        ).coerceAtLeast(0.0)

                        val nearby = chart.notes
                            .filter {
                                it.lane == lane &&
                                    abs(it.timeMs - snapped) <=
                                    stepMs * 0.5
                            }
                            .minByOrNull {
                                abs(it.timeMs - snapped)
                            }

                        when (tool) {
                            EditorTool.Draw -> {
                                if (nearby != null) {
                                    selectedNote = nearby
                                    tool = EditorTool.Select
                                } else {
                                    val note = Note(
                                        timeMs = snapped,
                                        lane = lane
                                    )

                                    updateChart(
                                        chart.withNote(note)
                                    )

                                    selectedNote = note
                                }
                            }

                            EditorTool.Select -> {
                                selectedNote = nearby
                            }

                            EditorTool.Erase -> {
                                if (nearby != null) {
                                    updateChart(
                                        chart.withoutNote(nearby)
                                    )

                                    if (selectedNote == nearby) {
                                        selectedNote = null
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val laneWidth = size.width / 8f
            val noteSize = min(
                laneWidth * 0.92f,
                56f * density
            )

            fun yOf(ms: Double): Float {
                return headerPx +
                    (ms / beatMs).toFloat() * beatPx -
                    scroll
            }

            clipRect(top = headerPx) {
                for (lane in 0 until 8) {
                    drawRect(
                        color = if (lane < 4) {
                            Color(0xFF1C1032)
                        } else {
                            Color(0xFF271341)
                        },
                        topLeft = Offset(
                            lane * laneWidth,
                            headerPx
                        ),
                        size = Size(
                            laneWidth,
                            (size.height - headerPx)
                                .coerceAtLeast(0f)
                        )
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(
                            lane * laneWidth,
                            headerPx
                        ),
                        end = Offset(
                            lane * laneWidth,
                            size.height
                        )
                    )
                }

                val firstBeat = floor(
                    scroll / beatPx
                ).toInt().coerceAtLeast(0)

                val lastBeat = ceil(
                    (scroll + size.height) / beatPx
                ).toInt()

                for (beat in firstBeat..lastBeat) {
                    val y = headerPx +
                        beat * beatPx -
                        scroll

                    val measure = beat % 4 == 0

                    drawLine(
                        color = Color.White.copy(
                            alpha = if (measure) 0.4f else 0.16f
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (measure) 2.5f else 1f
                    )

                    for (step in 1 until snapDivision) {
                        val stepY = y +
                            step * beatPx / snapDivision

                        drawLine(
                            color = Color.White.copy(alpha = 0.045f),
                            start = Offset(0f, stepY),
                            end = Offset(size.width, stepY)
                        )
                    }

                    if (measure) {
                        drawText(
                            textMeasurer,
                            "${beat / 4}",
                            Offset(5f, y + 3f),
                            TextStyle(
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                drawLine(
                    color = Fk.pink,
                    start = Offset(
                        size.width / 2f,
                        headerPx
                    ),
                    end = Offset(
                        size.width / 2f,
                        size.height
                    ),
                    strokeWidth = 3f
                )

                val lowerMs = (
                    scroll - noteSize
                ) / beatPx * beatMs - maxSustain

                val upperMs = (
                    scroll +
                    size.height -
                    headerPx +
                    noteSize
                ) / beatPx * beatMs

                var index = chart.notes.lowerBound(lowerMs)

                while (
                    index < chart.notes.size &&
                    chart.notes[index].timeMs <= upperMs
                ) {
                    val note = chart.notes[index]
                    index++

                    if (note.lane !in 0..7) continue

                    val y = yOf(note.timeMs)
                    val centerX = note.lane * laneWidth +
                        laneWidth / 2f

                    val selected = selectedNote == note

                    if (note.sustainMs > 0.0) {
                        val endY = yOf(
                            note.timeMs + note.sustainMs
                        )

                        drawRect(
                            color = Fk.lane[
                                note.lane % 4
                            ].copy(
                                alpha = if (selected) 1f else 0.65f
                            ),
                            topLeft = Offset(
                                centerX - noteSize * 0.12f,
                                y
                            ),
                            size = Size(
                                noteSize * 0.24f,
                                (endY - y).coerceAtLeast(0f)
                            )
                        )
                    }

                    if (selected) {
                        drawRect(
                            color = Fk.yellow,
                            topLeft = Offset(
                                centerX - noteSize * 0.57f,
                                y - noteSize * 0.57f
                            ),
                            size = Size(
                                noteSize * 1.14f,
                                noteSize * 1.14f
                            )
                        )
                    }

                    drawImage(
                        image = sprites.notes[note.lane % 4],
                        dstOffset = IntOffset(
                            (centerX - noteSize / 2f).toInt(),
                            (y - noteSize / 2f).toInt()
                        ),
                        dstSize = IntSize(
                            noteSize.toInt().coerceAtLeast(1),
                            noteSize.toInt().coerceAtLeast(1)
                        )
                    )
                }
            }

            drawRect(
                color = Fk.bg,
                topLeft = Offset.Zero,
                size = Size(size.width, headerPx)
            )

            for (lane in 0 until 8) {
                val centerX = lane * laneWidth +
                    laneWidth / 2f

                drawImage(
                    image = sprites.statics[lane % 4],
                    dstOffset = IntOffset(
                        (centerX - noteSize / 2f).toInt(),
                        ((headerPx - noteSize) / 2f).toInt()
                    ),
                    dstSize = IntSize(
                        noteSize.toInt().coerceAtLeast(1),
                        noteSize.toInt().coerceAtLeast(1)
                    ),
                    alpha = if (lane < 4) 0.7f else 1f
                )
            }

            drawLine(
                color = Fk.cyan,
                start = Offset(0f, headerPx),
                end = Offset(size.width, headerPx),
                strokeWidth = 3f
            )
        }

        if (selectedNote != null) {
            val note = selectedNote!!

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF25143C))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SELECTED NOTE",
                        color = Fk.yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "Lane ${note.lane + 1} • ${editorNumber(note.timeMs)}ms",
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = {
                            selectedNote = null
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Deselect",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "TIME",
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    EditorAction(
                        Icons.Default.Remove,
                        "Move Earlier"
                    ) {
                        moveSelected(-stepMs)
                    }

                    EditorAction(
                        Icons.Default.Add,
                        "Move Later"
                    ) {
                        moveSelected(stepMs)
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "SUSTAIN ${editorNumber(note.sustainMs)}ms",
                        color = Fk.cyan,
                        fontSize = 11.sp
                    )

                    EditorAction(
                        Icons.Default.Remove,
                        "Shorten Sustain"
                    ) {
                        changeSustain(-stepMs)
                    }

                    EditorAction(
                        Icons.Default.Add,
                        "Extend Sustain"
                    ) {
                        changeSustain(stepMs)
                    }

                    EditorAction(
                        Icons.Default.Delete,
                        "Delete Note"
                    ) {
                        removeSelected()
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF190C2D))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            EditorAction(
                Icons.Default.SkipPrevious,
                "Previous Beat"
            ) {
                pausePreview()
                scroll = (
                    scroll - beatPx
                ).coerceAtLeast(0f)
            }

            EditorAction(
                if (playing) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                if (playing) "Pause" else "Play",
                selected = playing
            ) {
                if (!audioLoaded) {
                    notify("Import Inst or Voices first")
                } else {
                    playing = !playing
                }
            }

            EditorAction(
                Icons.Default.Stop,
                "Stop"
            ) {
                pausePreview()
                playback.stop()
            }

            EditorAction(
                Icons.Default.SkipNext,
                "Next Beat"
            ) {
                pausePreview()
                scroll += beatPx
            }

            Spacer(Modifier.weight(1f))

            EditorAction(
                Icons.Default.MusicNote,
                "Import Inst",
                selected = project.instPath != null
            ) {
                pausePreview()
                onPickInst()
            }

            EditorAction(
                Icons.Default.RecordVoiceOver,
                "Import Voices",
                selected = project.voicesPath != null
            ) {
                pausePreview()
                onPickVoices()
            }

            EditorAction(
                Icons.Default.Tune,
                "Chart Properties"
            ) {
                showProperties = true
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onImport
            ) {
                Icon(
                    Icons.Default.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("IMPORT")
            }

            OutlinedButton(
                onClick = onExportJson
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("JSON")
            }

            OutlinedButton(
                onClick = onExportMod
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("EXPORT MOD")
            }

            Button(
                onClick = {
                    pausePreview()
                    onPlaytest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fk.pink
                )
            ) {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text("PLAYTEST")
            }
        }
    }
}
