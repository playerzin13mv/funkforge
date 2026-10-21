package com.funkforge.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.material3.Button
import androidx.compose.material.material3.Icon
import androidx.compose.material.material3.IconButton
import androidx.compose.material.material3.OutlinedButton
import androidx.compose.material.material3.Text
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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

@Composable
fun PlayScreen(
    project: Project,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    BackHandler(onBack = onExit)

    val chart = project.chart

    val notes = remember(chart) {
        chart.notes.sortedBy { it.timeMs }
    }

    val sprites = rememberSprites()

    val playback = remember(context) {
        Playback(context)
    }

    DisposableEffect(
        playback,
        project.instPath,
        project.voicesPath
    ) {
        playback.load(
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

    val hit = remember(notes, runId) {
        BooleanArray(notes.size)
    }

    val missed = remember(notes, runId) {
        BooleanArray(notes.size)
    }

    val counts = remember(runId) {
        IntArray(5)
    }

    val glow = remember(runId) {
        DoubleArray(8)
    }

    val held = remember(runId) {
        BooleanArray(4)
    }

    val drawPtr = remember(notes, runId) {
        intArrayOf(0)
    }

    var songMs by remember(runId) {
        mutableDoubleStateOf(-2500.0)
    }

    var score by remember(runId) {
        mutableIntStateOf(0)
    }

    var combo by remember(runId) {
        mutableIntStateOf(0)
    }

    var health by remember(runId) {
        mutableFloatStateOf(1f)
    }

    var finished by remember(runId) {
        mutableStateOf(false)
    }

    var failed by remember(runId) {
        mutableStateOf(false)
    }

    val endMs = remember(notes) {
        (notes.maxOfOrNull {
            it.timeMs + it.sustainMs
        } ?: 0.0) + 1500.0
    }

    LaunchedEffect(runId, playback) {
        playback.stop()

        val countdownStart = SystemClock.elapsedRealtime()
        var audioStarted = false
        var opponentIndex = 0
        var missIndex = 0

        while (!finished) {
            withFrameNanos { }

            val elapsed = (
                SystemClock.elapsedRealtime() - countdownStart
            ).toDouble()

            val time = if (!audioStarted) {
                elapsed - 2500.0
            } else {
                if (playback.hasAudio) {
                    playback.positionMs.toDouble()
                } else {
                    elapsed - 2500.0
                }
            }

            songMs = time

            if (!audioStarted && time >= 0.0) {
                audioStarted = true
                playback.play(0)
                songMs = 0.0
            }

            if (audioStarted && playback.isPlaying) {
                playback.synchronize()
            }

            while (
                opponentIndex < notes.size &&
                notes[opponentIndex].timeMs <= songMs
            ) {
                val note = notes[opponentIndex]

                if (
                    note.lane in 0..3 &&
                    !hit[opponentIndex]
                ) {
                    hit[opponentIndex] = true
                    glow[note.lane] = songMs + 110.0
                }

                opponentIndex++
            }

            while (
                missIndex < notes.size &&
                notes[missIndex].timeMs < songMs - 160.0
            ) {
                val note = notes[missIndex]

                if (
                    note.lane in 4..7 &&
                    !hit[missIndex]
                ) {
                    hit[missIndex] = true
                    missed[missIndex] = true
                    counts[4]++

                    combo = 0
                    health = (
                        health - 0.06f
                    ).coerceAtLeast(0f)
                }

                missIndex++
            }

            if (health <= 0f) {
                failed = true
                finished = true
            } else if (songMs > endMs) {
                finished = true
            }
        }

        playback.pause()
    }

    fun press(lane: Int) {
        if (
            finished ||
            songMs < 0.0 ||
            lane !in 0..3
        ) return

        held[lane] = true

        val currentTime = songMs

        var index = notes.lowerBound(
            currentTime - 160.0
        )

        while (
            index < notes.size &&
            notes[index].timeMs <= currentTime + 160.0
        ) {
            val note = notes[index]

            if (
                note.lane == lane + 4 &&
                !hit[index]
            ) {
                val difference = abs(
                    note.timeMs - currentTime
                )

                hit[index] = true

                val rating = when {
                    difference <= 45.0 -> 0
                    difference <= 90.0 -> 1
                    difference <= 135.0 -> 2
                    else -> 3
                }

                counts[rating]++

                score += intArrayOf(
                    350,
                    200,
                    100,
                    50
                )[rating]

                combo++

                health = (
                    health + 0.03f
                ).coerceAtMost(2f)

                glow[lane + 4] = currentTime + 110.0

                break
            }

            index++
        }
    }

    fun restart() {
        playback.stop()
        runId++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0520))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(runId) {
                    awaitPointerEventScope {
                        val activePointers =
                            HashMap<PointerId, Int>()

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

                                        press(lane)

                                        activePointers[change.id] = lane
                                    }

                                    change.consume()
                                } else if (
                                    !change.pressed &&
                                    change.previousPressed
                                ) {
                                    activePointers
                                        .remove(change.id)
                                        ?.let { lane ->
                                            held[lane] = false
                                        }
                                }
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

            val pixelsPerMs =
                0.45f *
                chart.speed.toFloat() *
                (height / 720f)

            val noteSize = laneWidth * 0.92f

            val now = songMs

            fun destination(
                lane: Int,
                centerY: Float
            ): IntOffset {
                return IntOffset(
                    (
                        laneX(lane) +
                        (laneWidth - noteSize) / 2f
                    ).toInt(),
                    (
                        centerY - noteSize / 2f
                    ).toInt()
                )
            }

            val imageSize = IntSize(
                noteSize.toInt(),
                noteSize.toInt()
            )

            for (lane in 0 until 8) {
                val image = when {
                    glow[lane] > now ->
                        sprites.confirms[lane % 4]

                    lane >= 4 && held[lane - 4] ->
                        sprites.presses[lane % 4]

                    else ->
                        sprites.statics[lane % 4]
                }

                drawImage(
                    image = image,
                    dstOffset = destination(
                        lane,
                        strumY
                    ),
                    dstSize = imageSize,
                    alpha = if (lane < 4) 0.8f else 1f
                )
            }

            while (
                drawPtr[0] < notes.size &&
                notes[drawPtr[0]].let {
                    it.timeMs + it.sustainMs
                } < now - 400.0
            ) {
                drawPtr[0]++
            }

            var index = drawPtr[0]

            while (index < notes.size) {
                val note = notes[index]

                val y = strumY + (
                    (note.timeMs - now) * pixelsPerMs
                ).toFloat()

                if (y > height + noteSize) break

                val alpha = if (missed[index]) {
                    0.3f
                } else {
                    1f
                }

                if (note.sustainMs > 0) {
                    val sustainEnd = strumY + (
                        (
                            note.timeMs +
                            note.sustainMs -
                            now
                        ) * pixelsPerMs
                    ).toFloat()

                    val top = if (
                        hit[index] &&
                        !missed[index]
                    ) {
                        maxOf(y, strumY)
                    } else {
                        y
                    }

                    if (sustainEnd > top) {
                        drawRect(
                            color = Fk.lane[
                                note.lane % 4
                            ].copy(
                                alpha = 0.55f * alpha
                            ),
                            topLeft = Offset(
                                laneX(note.lane) +
                                    laneWidth / 2f -
                                    noteSize * 0.12f,
                                top
                            ),
                            size = Size(
                                noteSize * 0.24f,
                                sustainEnd - top
                            )
                        )
                    }
                }

                if (
                    (!hit[index] || missed[index]) &&
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
                size = Size(
                    barWidth,
                    12f
                )
            )

            drawRect(
                color = Color(0xFF3BFF6B),
                topLeft = Offset(
                    (width - barWidth) / 2f,
                    barY
                ),
                size = Size(
                    barWidth *
                        (health / 2f).coerceIn(0f, 1f),
                    12f
                )
            )
        }

        Text(
            text = "Score $score   Combo $combo   Miss ${counts[4]}",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )

        if (songMs < 0.0 && !finished) {
            Text(
                text = "${(-songMs / 1000.0).toInt() + 1}",
                color = Fk.cyan,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }

        IconButton(
            onClick = onExit,
            modifier = Modifier.align(
                Alignment.TopEnd
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit",
                tint = Color.White
            )
        }

        if (finished) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xE6140826))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (failed) {
                        "GAME OVER"
                    } else {
                        "SONG COMPLETE"
                    },
                    color = if (failed) {
                        Fk.pink
                    } else {
                        Fk.cyan
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Score $score",
                    color = Color.White
                )

                Text(
                    text = "Sick ${counts[0]} • " +
                        "Good ${counts[1]} • " +
                        "Bad ${counts[2]} • " +
                        "Shit ${counts[3]} • " +
                        "Miss ${counts[4]}",
                    color = Color.White.copy(
                        alpha = 0.8f
                    ),
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            restart()
                        }
                    ) {
                        Text("RESTART")
                    }

                    OutlinedButton(
                        onClick = onExit
                    ) {
                        Text("EXIT")
                    }
                }
            }
        }
    }
}
