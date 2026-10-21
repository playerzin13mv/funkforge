package com.funkforge.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Choreographer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class FPSData(
    val fps: Int = 0,
    val averageFps: Int = 0,
    val frameTimeMs: Double = 0.0,
    val frameCount: Long = 0L
)

object FPSCounter {
    private const val PREFS_NAME = "funkforge_options"
    private const val PREF_SHOW_FPS = "show_fps"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(PREF_SHOW_FPS, false)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(PREF_SHOW_FPS, enabled)
            .apply()
    }

    @Composable
    fun rememberEnabled(): State<Boolean> {
        val context = LocalContext.current

        val enabled = remember {
            mutableStateOf(isEnabled(context))
        }

        DisposableEffect(context) {
            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener {
                    _, key ->
                    if (key == PREF_SHOW_FPS) {
                        enabled.value = prefs.getBoolean(
                            PREF_SHOW_FPS,
                            false
                        )
                    }
                }

            prefs.registerOnSharedPreferenceChangeListener(
                listener
            )

            enabled.value = prefs.getBoolean(
                PREF_SHOW_FPS,
                false
            )

            onDispose {
                prefs.unregisterOnSharedPreferenceChangeListener(
                    listener
                )
            }
        }

        return enabled
    }

    @Composable
    fun rememberFPSData(
        enabled: Boolean = true
    ): State<FPSData> {
        val data = remember {
            mutableStateOf(FPSData())
        }

        DisposableEffect(enabled) {
            if (!enabled) {
                data.value = FPSData()

                onDispose {}
            } else {
                val choreographer = Choreographer.getInstance()

                var running = true
                var lastFrameNs = 0L
                var windowStartNs = 0L
                var windowFrames = 0
                var totalFrames = 0L
                var smoothedFps = 0.0
                var smoothedFrameMs = 0.0

                val callback = object :
                    Choreographer.FrameCallback {

                    override fun doFrame(frameTimeNanos: Long) {
                        if (!running) return

                        if (windowStartNs == 0L) {
                            windowStartNs = frameTimeNanos
                        }

                        if (lastFrameNs != 0L) {
                            val deltaNs = (
                                frameTimeNanos - lastFrameNs
                            ).coerceAtLeast(1L)

                            val frameMs = deltaNs / 1_000_000.0

                            smoothedFrameMs = if (
                                smoothedFrameMs == 0.0
                            ) {
                                frameMs
                            } else {
                                smoothedFrameMs * 0.9 +
                                    frameMs * 0.1
                            }
                        }

                        lastFrameNs = frameTimeNanos
                        windowFrames++
                        totalFrames++

                        val elapsedNs = (
                            frameTimeNanos - windowStartNs
                        )

                        if (elapsedNs >= 500_000_000L) {
                            val currentFps = (
                                windowFrames * 1_000_000_000.0 /
                                    elapsedNs
                            )

                            smoothedFps = if (
                                smoothedFps == 0.0
                            ) {
                                currentFps
                            } else {
                                smoothedFps * 0.75 +
                                    currentFps * 0.25
                            }

                            data.value = FPSData(
                                fps = currentFps.roundToInt(),
                                averageFps = smoothedFps.roundToInt(),
                                frameTimeMs = smoothedFrameMs,
                                frameCount = totalFrames
                            )

                            windowFrames = 0
                            windowStartNs = frameTimeNanos
                        }

                        choreographer.postFrameCallback(this)
                    }
                }

                choreographer.postFrameCallback(callback)

                onDispose {
                    running = false
                    choreographer.removeFrameCallback(callback)
                }
            }
        }

        return data
    }
}

@Composable
fun FPSOverlay(
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val enabled by FPSCounter.rememberEnabled()

    if (!enabled) return

    val fpsData by FPSCounter.rememberFPSData(
        enabled = enabled
    )

    val fpsColor = when {
        fpsData.fps >= 55 -> Color(0xFF36F58B)
        fpsData.fps >= 40 -> Color(0xFFFFD54F)
        fpsData.fps > 0 -> Color(0xFFFF5D73)
        else -> Color.White
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xE8121020),
        border = BorderStroke(
            1.dp,
            fpsColor.copy(alpha = 0.45f)
        ),
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 7.dp
            ),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            fpsColor,
                            RoundedCornerShape(50)
                        )
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    text = "${fpsData.fps} FPS",
                    color = fpsColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (showDetails) {
                Spacer(Modifier.height(3.dp))

                Text(
                    text = "AVG ${fpsData.averageFps}",
                    color = Color(0xFFD4CCE4),
                    fontSize = 10.sp
                )

                Text(
                    text = String.format(
                        java.util.Locale.US,
                        "%.1f ms",
                        fpsData.frameTimeMs
                    ),
                    color = Color(0xFFB5A8CE),
                    fontSize = 10.sp
                )
            }
        }
    }
}
