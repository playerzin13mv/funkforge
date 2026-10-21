
package com.funkforge.play

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.max

private val laneColors = listOf(
    Color(0xFFC24B99),
    Color(0xFF00FFFF),
    Color(0xFF12FA05),
    Color(0xFFF9393F)
)

private data class TestNote(
    val id: Int,
    val time: Long,
    val lane: Int,
    val sustain: Long,
    val playable: Boolean
)

private data class TestSong(
    val name: String,
    val chart: DocumentFile,
    val audio: DocumentFile?,
    val notes: List<TestNote>,
    val speed: Float
)

private enum class TestResult {
    Perfect,
    Good,
    Bad,
    Miss
}

private class TestEngine(
    val song: TestSong
) {
    val judged = mutableStateMapOf<Int, TestResult>()
    var score by mutableIntStateOf(0)
    var combo by mutableIntStateOf(0)
    var misses by mutableIntStateOf(0)
    var health by mutableFloatStateOf(1f)
    var accuracy by mutableFloatStateOf(100f)
    var lastJudgement by mutableStateOf("")
    var playing by mutableStateOf(false)
    var position by mutableLongStateOf(0L)

    private var startTime = 0L
    private var pausedAt = 0L
    private var totalJudgements = 0
    private var accuracyPoints = 0f

    fun start() {
        reset()
        startTime = SystemClock.elapsedRealtime()
        playing = true
    }

    fun pause() {
        if (!playing) return
        pausedAt = position
        playing = false
    }

    fun resume() {
        if (playing) return
        startTime = SystemClock.elapsedRealtime() - pausedAt
        playing = true
    }

    fun reset() {
        judged.clear()
        score = 0
        combo = 0
        misses = 0
        health = 1f
        accuracy = 100f
        position = 0L
        pausedAt = 0L
        totalJudgements = 0
        accuracyPoints = 0f
        lastJudgement = ""
        playing = false
    }

    fun update(audioPosition: Long? = null) {
        if (!playing) return

        position = audioPosition
            ?: (SystemClock.elapsedRealtime() - startTime)

        song.notes.forEach { note ->
            if (
                note.playable &&
                !judged.containsKey(note.id) &&
                position - note.time > 170L
            ) {
                register(note, TestResult.Miss)
            }
        }
    }

    fun hit(lane: Int) {
        if (!playing) return

        val note = song.notes
            .asSequence()
            .filter {
                it.playable &&
                it.lane == lane &&
                !judged.containsKey(it.id)
            }
            .minByOrNull { abs(it.time - position) }
            ?: return

        val difference = abs(note.time - position)

        val result = when {
            difference <= 45L -> TestResult.Perfect
            difference <= 95L -> TestResult.Good
            difference <= 170L -> TestResult.Bad
            else -> return
        }

        register(note, result)
    }

    private fun register(note: TestNote, result: TestResult) {
        if (judged.containsKey(note.id)) return

        judged[note.id] = result
        totalJudgements++

        when (result) {
            TestResult.Perfect -> {
                score += 350
                combo++
                health = (health + 0.025f).coerceAtMost(2f)
                accuracyPoints += 1f
                lastJudgement = "SICK!"
            }

            TestResult.Good -> {
                score += 200
                combo++
                health = (health + 0.012f).coerceAtMost(2f)
                accuracyPoints += 0.7f
                lastJudgement = "GOOD"
            }

            TestResult.Bad -> {
                score += 50
                combo = 0
                health = (health - 0.035f).coerceAtLeast(0f)
                accuracyPoints += 0.25f
                lastJudgement = "BAD"
            }

            TestResult.Miss -> {
                combo = 0
                misses++
                health = (health - 0.08f).coerceAtLeast(0f)
                lastJudgement = "MISS"
            }
        }

        accuracy = if (totalJudgements > 0) {
            accuracyPoints / totalJudgements * 100f
        } else {
            100f
        }

        if (health <= 0f) {
            pause()
            lastJudgement = "GAME OVER"
        }
    }
}

private fun DocumentFile.child(name: String): DocumentFile? {
    return listFiles().firstOrNull {
        it.name.equals(name, ignoreCase = true)
    }
}

private fun DocumentFile.path(vararg names: String): DocumentFile? {
    var current: DocumentFile = this

    for (name in names) {
        current = current.child(name) ?: return null
    }

    return current
}

private fun readChart(
    context: Context,
    file: DocumentFile
): Pair<List<TestNote>, Float> {
    val json = context.contentResolver.openInputStream(file.uri)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Unable to read chart")

    val root = JSONObject(json)
    val song = root.optJSONObject("song") ?: root
    val sections = song.optJSONArray("notes")
        ?: error("Chart contains no sections")

    val notes = mutableListOf<TestNote>()
    var nextId = 0

    for (i in 0 until sections.length()) {
        val section = sections.optJSONObject(i) ?: continue
        val mustHit = section.optBoolean("mustHitSection", false)
        val sectionNotes = section.optJSONArray("sectionNotes")
            ?: continue

        for (j in 0 until sectionNotes.length()) {
            val entry = sectionNotes.optJSONArray(j) ?: continue
            if (entry.length() < 2) continue

            val rawLane = entry.optInt(1, -1)
            if (rawLane < 0 || rawLane >= 8) continue

            val noteTime = entry.optDouble(0, 0.0)
                .toLong()
                .coerceAtLeast(0L)

            val sustain = entry.optDouble(2, 0.0)
                .toLong()
                .coerceAtLeast(0L)

            val playerNote = if (rawLane < 4) {
                mustHit
            } else {
                !mustHit
            }

            notes.add(
                TestNote(
                    id = nextId++,
                    time = noteTime,
                    lane = rawLane % 4,
                    sustain = sustain,
                    playable = playerNote
                )
            )
        }
    }

    return notes.sortedBy { it.time } to
        song.optDouble("speed", 1.0).toFloat()
            .coerceIn(0.5f, 5f)
}

private fun discoverSongs(
    context: Context,
    root: DocumentFile
): List<TestSong> {
    val data = root.child("data") ?: root
    val songsRoot = root.child("songs")

    return data.listFiles()
        .filter { it.isDirectory }
        .mapNotNull { folder ->
            val chart = folder.listFiles().firstOrNull {
                it.isFile &&
                it.name?.endsWith(".json", true) == true &&
                !it.name.orEmpty().contains("events", true)
            } ?: return@mapNotNull null

            try {
                val parsed = readChart(context, chart)

                val audioFolder = songsRoot?.child(
                    folder.name.orEmpty()
                )

                val audio = audioFolder?.child("Inst.ogg")
                    ?: audioFolder?.child("Inst.mp3")
                    ?: folder.child("Inst.ogg")
                    ?: folder.child("Inst.mp3")

                TestSong(
                    name = folder.name.orEmpty(),
                    chart = chart,
                    audio = audio,
                    notes = parsed.first,
                    speed = parsed.second
                )
            } catch (_: Exception) {
                null
            }
        }
        .sortedBy { it.name }
}

@Composable
fun PlayTest(
    modFolderUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var songs by remember(modFolderUri) {
        mutableStateOf<List<TestSong>>(emptyList())
    }

    var selected by remember(modFolderUri) {
        mutableStateOf<TestSong?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(modFolderUri) {
        try {
            val root = DocumentFile.fromTreeUri(
                context,
                modFolderUri
            ) ?: error("Invalid mod folder")

            songs = discoverSongs(context, root)

            if (songs.isEmpty()) {
                error = "No compatible Psych Engine charts found"
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to open mod folder"
        }
    }

    if (selected == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF140826))
                .padding(20.dp)
        ) {
            Text(
                "PLAY TEST",
                color = Color(0xFF00E5FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Select a chart to test",
                color = Color.LightGray
            )

            Spacer(Modifier.height(20.dp))

            if (error != null) {
                Text(
                    error.orEmpty(),
                    color = Color(0xFFFF526C)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(songs) { song ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF2E1259),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selected = song }
                            .padding(18.dp)
                    ) {
                        Text(
                            song.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${song.notes.count { it.playable }} playable notes",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Text(
                            if (song.audio != null) {
                                "Audio available"
                            } else {
                                "Chart only"
                            },
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Button(onClick = onBack) {
                Text("Back")
            }
        }
    } else {
        val song = selected!!

        key(song.chart.uri) {
            PlayTestGame(
                song = song,
                onBack = { selected = null }
            )
        }
    }
}

@Composable
private fun PlayTestGame(
    song: TestSong,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember(song) { TestEngine(song) }

    var player by remember(song) {
        mutableStateOf<MediaPlayer?>(null)
    }

    var audioError by remember(song) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(song) {
        val media = song.audio?.let { file ->
            try {
                MediaPlayer().apply {
                    context.contentResolver.openFileDescriptor(
                        file.uri,
                        "r"
                    )?.use { descriptor ->
                        setDataSource(descriptor.fileDescriptor)
                    } ?: error("Unable to open audio")

                    prepare()
                }
            } catch (e: Exception) {
                audioError = e.message
                null
            }
        }

        player = media

        onDispose {
            media?.release()
            player = null
        }
    }

    fun startGame() {
        player?.seekTo(0)
        engine.start()
        player?.start()
    }

    fun togglePause() {
        if (engine.playing) {
            engine.pause()
            player?.pause()
        } else {
            engine.resume()
            player?.start()
        }
    }

    LaunchedEffect(engine, player) {
        while (true) {
            if (engine.playing) {
                val audioTime = player?.let {
                    if (it.isPlaying) it.currentPosition.toLong()
                    else null
                }

                engine.update(audioTime)

                val lastNote = song.notes.maxOfOrNull {
                    it.time + it.sustain
                } ?: 0L

                if (engine.position > lastNote + 2000L) {
                    engine.pause()
                    player?.pause()
                    engine.lastJudgement = "SONG COMPLETE"
                }
            }

            delay(16L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140826))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            song.name.uppercase(),
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Black,
            fontSize = 22.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Score: ${engine.score}  |  Misses: ${engine.misses}",
            color = Color.White,
            fontSize = 13.sp
        )

        Text(
            "Accuracy: ${"%.2f".format(engine.accuracy)}%  |  Combo: ${engine.combo}",
            color = Color.White,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(engine.health / 2f)
                    .fillMaxHeight()
                    .background(Color(0xFF12FA05))
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF21103D))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val laneWidth = size.width / 4f
                val receptorY = size.height * 0.78f
                val pixelsPerMs = 0.42f * song.speed

                for (lane in 0..3) {
                    drawRect(
                        color = laneColors[lane].copy(alpha = 0.08f),
                        topLeft = Offset(laneWidth * lane, 0f),
                        size = Size(laneWidth, size.height)
                    )

                    drawRect(
                        color = laneColors[lane],
                        topLeft = Offset(
                            laneWidth * lane + 4f,
                            receptorY
                        ),
                        size = Size(
                            laneWidth - 8f,
                            12f
                        )
                    )
                }

                song.notes.forEach { note ->
                    if (
                        !note.playable ||
                        engine.judged.containsKey(note.id)
                    ) return@forEach

                    val distance = note.time - engine.position
                    val y = receptorY - distance * pixelsPerMs

                    if (y < -size.height || y > size.height + 100f) {
                        return@forEach
                    }

                    val x = note.lane * laneWidth + 6f
                    val width = laneWidth - 12f

                    if (note.sustain > 0L) {
                        val sustainHeight = note.sustain * pixelsPerMs

                        drawRect(
                            color = laneColors[note.lane]
                                .copy(alpha = 0.5f),
                            topLeft = Offset(
                                x + width * 0.35f,
                                y - sustainHeight
                            ),
                            size = Size(
                                width * 0.3f,
                                sustainHeight
                            )
                        )
                    }

                    drawRect(
                        color = laneColors[note.lane],
                        topLeft = Offset(x, y),
                        size = Size(width, 20f)
                    )
                }
            }

            Text(
                engine.lastJudgement,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                color = Color(0xFFFFE14D),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val symbols = listOf("←", "↓", "↑", "→")

            repeat(4) { lane ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(
                            2.dp,
                            laneColors[lane],
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            laneColors[lane].copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .pointerInput(engine, lane) {
                            detectTapGestures {
                                engine.hit(lane)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        symbols[lane],
                        color = laneColors[lane],
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (audioError != null) {
            Text(
                "Audio error: $audioError",
                color = Color.Red,
                fontSize = 11.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { startGame() }) {
                Text("Restart")
            }

            Button(
                onClick = { togglePause() },
                enabled = engine.position > 0L
            ) {
                Text(if (engine.playing) "Pause" else "Resume")
            }

            Button(
                onClick = {
                    player?.pause()
                    engine.pause()
                    onBack()
                }
            ) {
                Text("Exit")
            }
        }

        if (!engine.playing && engine.position == 0L) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Press Restart to begin",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
