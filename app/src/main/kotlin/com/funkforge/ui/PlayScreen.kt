package com.funkforge.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.audio.Playback
import com.funkforge.model.Project
import com.funkforge.model.lowerBound
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val COUNTDOWN_MS = 2500.0
private const val HIT_WINDOW_MS = 160.0
private const val SUSTAIN_TICK_MS = 100.0

private enum class PlayResult {
    Sick,
    Good,
    Bad,
    Shit,
    Miss
}

private fun judgeHit(diff: Double): PlayResult {
    return when {
        diff <= 45.0 -> PlayResult.Sick
        diff <= 90.0 -> PlayResult.Good
        diff <= 135.0 -> PlayResult.Bad
        else -> PlayResult.Shit
    }
}

private fun resultScore(result: PlayResult): Int {
    return when (result) {
        PlayResult.Sick -> 350
        PlayResult.Good -> 200
        PlayResult.Bad -> 100
        PlayResult.Shit -> 50
        PlayResult.Miss -> 0
    }
}

private fun resultWeight(result: PlayResult): Double {
    return when (result) {
        PlayResult.Sick -> 1.0
        PlayResult.Good -> 0.75
        PlayResult.Bad -> 0.5
        PlayResult.Shit -> 0.25
        PlayResult.Miss -> 0.0
    }
}

private class PlaySession(
    val noteCount: Int
) {
    val hit = BooleanArray(noteCount)
    val missed = BooleanArray(noteCount)
    val sustainBroken = BooleanArray(noteCount)
    val sustainProgress = DoubleArray(noteCount)
    val sustainLastTick = DoubleArray(noteCount) { Double.NaN }
    val counts = IntArray(5)
    val glow = DoubleArray(8)
    val held = BooleanArray(4)
    val pointerCount = IntArray(4)

    var score by mutableIntStateOf(0)
    var combo by mutableIntStateOf(0)
    var maxCombo by mutableIntStateOf(0)
    var health by mutableFloatStateOf(1f)
    var accuracyTotal by mutableDoubleStateOf(0.0)
    var accuracyCount by mutableIntStateOf(0)
    var lastRating by mutableStateOf("")
    var lastRatingUntil by mutableDoubleStateOf(0.0)
    var songMs by mutableDoubleStateOf(-COUNTDOWN_MS)
    var paused by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var failed by mutableStateOf(false)

    val accuracy: Double
        get() = if (accuracyCount == 0) {
            100.0
        } else {
            accuracyTotal / accuracyCount * 100.0
        }

    val rank: String
        get() = when {
            accuracy >= 99.5 -> "S+"
            accuracy >= 95.0 -> "S"
            accuracy >= 90.0 -> "A"
            accuracy >= 80.0 -> "B"
            accuracy >= 70.0 -> "C"
            else -> "D"
        }

    fun register(result: PlayResult, time: Double) {
        val index = result.ordinal
        counts[index]++
        accuracyTotal += resultWeight(result)
        accuracyCount++

        if (result == PlayResult.Miss) {
            combo = 0
            health = (health - 0.06f).coerceAtLeast(0f)
        } else {
            score += resultScore(result)
            combo++
            maxCombo = maxOf(maxCombo, combo)
            health = (health + 0.025f).coerceAtMost(2f)
        }

        lastRating = result.name.uppercase()
        lastRatingUntil = time + 450.0
    }

    fun breakSustain(time: Double) {
        combo = 0
        health = (health - 0.025f).coerceAtLeast(0f)
        lastRating = "MISS"
        lastRatingUntil = time + 450.0
    }

    fun pressLane(lane: Int) {
        if (lane !in 0..3) return
        pointerCount[lane]++
        held[lane] = true
    }

    fun releaseLane(lane: Int) {
        if (lane !in 0..3) return
        pointerCount[lane] = (pointerCount[lane] - 1).coerceAtLeast(0)
        held[lane] = pointerCount[lane] > 0
    }
}

@Composable
fun PlayScreen(
    project: Project,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            if (previous != null) {
                activity.requestedOrientation = previous
            }
        }
    }

    val chart = project.chart
    val notes = remember(chart) {
        chart.notes.sortedBy { it.timeMs }
    }
    val sprites = rememberSprites()

    val playback = remember(
        context,
        project.instPath,
        project.voicesPath
    ) {
        Playback(context)
    }

    var audioAvailable by remember(playback) {
        mutableStateOf(false)
    }

    DisposableEffect(playback) {
        audioAvailable = playback.load(
            project.instPath,
            project.voicesPath
        )

        onDispose {
            playback.release()
        }
    }

    var runId by remember {
        mutableIntStateOf(0)
    }

    val session = remember(notes, runId) {
        PlaySession(notes.size)
    }

    var countdownStart by remember(runId) {
        mutableLongStateOf(0L)
    }

    var pauseStarted by remember(runId) {
        mutableLongStateOf(0L)
    }

    var accumulatedPause by remember(runId) {
        mutableLongStateOf(0L)
    }

    var audioStarted by remember(runId) {
        mutableStateOf(false)
    }

    val endMs = remember(notes, playback) {
        maxOf(
            notes.maxOfOrNull {
                it.timeMs + it.sustainMs
            } ?: 0.0,
            playback.durationMs.toDouble()
        ) + 1200.0
    }

    fun restart() {
        playback.stop()
        runId++
    }

    fun pauseGame() {
        if (session.finished || session.paused) return

        session.paused = true
        pauseStarted = SystemClock.elapsedRealtime()
        playback.pause()
    }

    fun resumeGame() {
        if (session.finished || !session.paused) return

        val now = SystemClock.elapsedRealtime()
        accumulatedPause += now - pauseStarted
        session.paused = false

        if (audioStarted && audioAvailable) {
            playback.resume()
        }
    }

    fun exitGame() {
        playback.pause()
        onExit()
    }

    BackHandler {
        if (session.finished) {
            exitGame()
        } else if (session.paused) {
            resumeGame()
        } else {
            pauseGame()
        }
    }

    fun press(lane: Int) {
        if (
            lane !in 0..3 ||
            session.paused ||
            session.finished ||
            session.songMs < 0.0
        ) return

        session.pressLane(lane)

        val time = session.songMs
        var index = notes.lowerBound(time - HIT_WINDOW_MS)

        while (
            index < notes.size &&
            notes[index].timeMs <= time + HIT_WINDOW_MS
        ) {
            val note = notes[index]

            if (
                note.lane == lane + 4 &&
                !session.hit[index]
            ) {
                val difference = abs(note.timeMs - time)
                val result = judgeHit(difference)

                session.hit[index] = true
                session.register(result, time)
                session.glow[lane + 4] = time + 110.0

                if (note.sustainMs > 0.0) {
                    session.sustainProgress[index] = 0.0
                    session.sustainLastTick[index] = note.timeMs
                }

                return
            }

            index++
        }
    }

    LaunchedEffect(runId, playback) {
        playback.stop()
        countdownStart = SystemClock.elapsedRealtime()
        accumulatedPause = 0L
        audioStarted = false

        var opponentIndex = 0
        var missIndex = 0

        while (!session.finished) {
            withFrameNanos { }

            if (session.paused) continue

            val now = SystemClock.elapsedRealtime()
            val elapsed = (
                now - countdownStart - accumulatedPause
            ).toDouble()

            val fallbackTime = elapsed - COUNTDOWN_MS

            if (!audioStarted && fallbackTime >= 0.0) {
                audioStarted = true

                if (audioAvailable) {
                    playback.play(0)
                }
            }

            val time = if (
                audioStarted &&
                audioAvailable &&
                playback.isPlaying
            ) {
                playback.positionMs.toDouble()
            } else {
                fallbackTime
            }

            session.songMs = time

            if (
                audioStarted &&
                audioAvailable &&
                playback.isPlaying
            ) {
                playback.synchronize()
            }

            while (
                opponentIndex < notes.size &&
                notes[opponentIndex].timeMs <= time
            ) {
                val note = notes[opponentIndex]

                if (
                    note.lane in 0..3 &&
                    !session.hit[opponentIndex]
                ) {
                    session.hit[opponentIndex] = true
                    session.glow[note.lane] = time + 110.0
                }

                opponentIndex++
            }

            while (
                missIndex < notes.size &&
                notes[missIndex].timeMs < time - HIT_WINDOW_MS
            ) {
                val note = notes[missIndex]

                if (
                    note.lane in 4..7 &&
                    !session.hit[missIndex]
                ) {
                    session.hit[missIndex] = true
                    session.missed[missIndex] = true
                    session.register(PlayResult.Miss, time)
                }

                missIndex++
            }

            for (index in notes.indices) {
                val note = notes[index]

                if (
                    note.lane !in 4..7 ||
                    note.sustainMs <= 0.0 ||
                    !session.hit[index] ||
                    session.missed[index] ||
                    session.sustainBroken[index]
                ) continue

                val lane = note.lane - 4
                val sustainEnd = note.timeMs + note.sustainMs

                if (time < note.timeMs) continue

                if (time >= sustainEnd) {
                    session.sustainProgress[index] = note.sustainMs
                    continue
                }

                if (!session.held[lane]) {
                    if (time > note.timeMs + 90.0) {
                        session.sustainBroken[index] = true
                        session.breakSustain(time)
                    }
                    continue
                }

                val progress = (
                    time - note.timeMs
                ).coerceIn(0.0, note.sustainMs)

                session.sustainProgress[index] = progress

                val lastTick = session.sustainLastTick[index]

                if (
                    lastTick.isNaN() ||
                    time - lastTick >= SUSTAIN_TICK_MS
                ) {
                    session.sustainLastTick[index] = time
                    session.score += 10
                    session.health = (
                        session.health + 0.002f
                    ).coerceAtMost(2f)
                }
            }

            if (session.health <= 0f) {
                session.failed = true
                session.finished = true
            } else if (time > endMs) {
                session.finished = true
            }
        }

        playback.pause()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0520))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(runId, session) {
                    awaitPointerEventScope {
                        val pointers = HashMap<PointerId, Int>()

                        try {
                            while (true) {
                                val event = awaitPointerEvent()

                                val laneWidth = min(
                                    size.height * 0.2f,
                                    size.width / 9.5f
                                )

                                val gap = laneWidth * 0.7f
                                val startX = (
                                    size.width -
                                    (8 * laneWidth + gap)
                                ) / 2f

                                val playerX =
                                    startX + 4 * laneWidth + gap

                                for (change in event.changes) {
                                    if (
                                        change.pressed &&
                                        !change.previousPressed
                                    ) {
                                        val x = change.position.x

                                        if (
                                            x >= playerX &&
                                            x < playerX + 4 * laneWidth
                                        ) {
                                            val lane = (
                                                (x - playerX) / laneWidth
                                            ).toInt().coerceIn(0, 3)

                                            if (
                                                !session.paused &&
                                                !session.finished
                                            ) {
                                                press(lane)
                                                pointers[change.id] = lane
                                            }
                                        }

                                        change.consume()
                                    } else if (
                                        !change.pressed &&
                                        change.previousPressed
                                    ) {
                                        pointers.remove(change.id)?.let {
                                            session.releaseLane(it)
                                        }
                                    }
                                }
                            }
                        } finally {
                            pointers.values.forEach {
                                session.releaseLane(it)
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val laneWidth = min(
                height * 0.2f,
                width / 9.5f
            )

            val gap = laneWidth * 0.7f
            val startX = (
                width - (8 * laneWidth + gap)
            ) / 2f

            fun laneX(lane: Int): Float {
                return if (lane < 4) {
                    startX + lane * laneWidth
                } else {
                    startX +
                        4 * laneWidth +
                        gap +
                        (lane - 4) * laneWidth
                }
            }

            val strumY = height * 0.14f
            val pixelsPerMs = (
                0.45f *
                chart.speed.toFloat() *
                (height / 720f)
            ).coerceAtLeast(0.01f)

            val noteSize = laneWidth * 0.92f
            val now = session.songMs

            fun destination(
                lane: Int,
                centerY: Float
            ): IntOffset {
                return IntOffset(
                    (
                        laneX(lane) +
                        (laneWidth - noteSize) / 2f
                    ).roundToInt(),
                    (
                        centerY - noteSize / 2f
                    ).roundToInt()
                )
            }

            val imageSize = IntSize(
                noteSize.roundToInt().coerceAtLeast(1),
                noteSize.roundToInt().coerceAtLeast(1)
            )

            for (lane in 0 until 8) {
                val image = when {
                    session.glow[lane] > now ->
                        sprites.confirms[lane % 4]

                    lane >= 4 && session.held[lane - 4] ->
                        sprites.presses[lane % 4]

                    else ->
                        sprites.statics[lane % 4]
                }

                drawImage(
                    image = image,
                    dstOffset = destination(lane, strumY),
                    dstSize = imageSize,
                    alpha = if (lane < 4) 0.8f else 1f
                )
            }

            val earliestVisible = now -
                (strumY + noteSize) / pixelsPerMs

            var index = notes.lowerBound(
                earliestVisible - 500.0
            )

            while (index < notes.size) {
                val note = notes[index]

                if (note.lane !in 0..7) {
                    index++
                    continue
                }

                val y = strumY + (
                    (note.timeMs - now) * pixelsPerMs
                ).toFloat()

                if (y > height + noteSize) break

                val alpha = if (
                    session.missed[index]
                ) 0.3f else 1f

                if (note.sustainMs > 0.0) {
                    val sustainEnd = strumY + (
                        (
                            note.timeMs +
                            note.sustainMs -
                            now
                        ) * pixelsPerMs
                    ).toFloat()

                    val sustainStart = if (
                        session.hit[index] &&
                        !session.missed[index]
                    ) {
                        maxOf(y, strumY)
                    } else {
                        y
                    }

                    if (sustainEnd > sustainStart) {
                        drawRect(
                            color = Fk.lane[
                                note.lane % 4
                            ].copy(alpha = 0.55f * alpha),
                            topLeft = Offset(
                                laneX(note.lane) +
                                    laneWidth / 2f -
                                    noteSize * 0.12f,
                                sustainStart
                            ),
                            size = Size(
                                noteSize * 0.24f,
                                sustainEnd - sustainStart
                            )
                        )
                    }
                }

                if (
                    (!session.hit[index] ||
                        session.missed[index]) &&
                    y > -noteSize
                ) {
                    drawImage(
                        image = sprites.notes[
                            note.lane % 4
                        ],
                        dstOffset = destination(
                            note.lane,
                            y
                        ),
                        dstSize = imageSize,
                        alpha = alpha
                    )
                }

                index++
            }

            val barWidth = width * 0.5f
            val barY = height - 22f

            drawRect(
                color = Color(0xFFFF3B3B),
                topLeft = Offset(
                    (width - barWidth) / 2f,
                    barY
                ),
                size = Size(barWidth, 12f)
            )

            drawRect(
                color = Color(0xFF3BFF6B),
                topLeft = Offset(
                    (width - barWidth) / 2f,
                    barY
                ),
                size = Size(
                    barWidth *
                        (session.health / 2f).coerceIn(0f, 1f),
                    12f
                )
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = chart.song.uppercase(),
                color = Fk.cyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${(session.songMs / 1000.0).coerceAtLeast(0.0).toInt()}s",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            IconButton(onClick = { pauseGame() }) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White
                )
            }

            IconButton(onClick = { exitGame() }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Exit",
                    tint = Color.White
                )
            }
        }

        Text(
            text = "Score ${session.score}   " +
                "Combo ${session.combo}   " +
                "Accuracy ${"%.2f".format(session.accuracy)}%   " +
                "Miss ${session.counts[4]}",
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )

        if (
            session.lastRating.isNotEmpty() &&
            session.lastRatingUntil > session.songMs &&
            !session.finished
        ) {
            Text(
                text = session.lastRating,
                color = when (session.lastRating) {
                    "SICK" -> Fk.cyan
                    "GOOD" -> Color.Green
                    "BAD" -> Color.Yellow
                    else -> Fk.pink
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (session.songMs < 0.0 && !session.finished) {
            Text(
                text = "${(-session.songMs / 1000.0).toInt() + 1}",
                color = Fk.cyan,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (session.paused && !session.finished) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color(0xEF140826),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PAUSED",
                    color = Fk.cyan,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Button(onClick = { resumeGame() }) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("RESUME")
                }

                OutlinedButton(onClick = { restart() }) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("RESTART")
                }

                OutlinedButton(onClick = { exitGame() }) {
                    Text("EXIT")
                }
            }
        }

        if (session.finished) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        Color(0xEF140826),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (session.failed) {
                        "GAME OVER"
                    } else {
                        "SONG COMPLETE"
                    },
                    color = if (session.failed) Fk.pink else Fk.cyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Rank ${session.rank}",
                    color = Fk.yellow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Score ${session.score}",
                    color = Color.White
                )

                Text(
                    text = "Max Combo ${session.maxCombo}",
                    color = Color.White
                )

                Text(
                    text = "Accuracy ${"%.2f".format(session.accuracy)}%",
                    color = Color.White
                )

                Text(
                    text = "Sick ${session.counts[0]} • " +
                        "Good ${session.counts[1]} • " +
                        "Bad ${session.counts[2]} • " +
                        "Shit ${session.counts[3]} • " +
                        "Miss ${session.counts[4]}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { restart() }) {
                        Text("RESTART")
                    }

                    OutlinedButton(onClick = { exitGame() }) {
                        Text("EXIT")
                    }
                }
            }
        }
    }
}
