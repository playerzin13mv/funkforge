package com.funkforge.options

import android.content.Context
import android.view.Choreographer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.ui.Fk

data class GameOptions(
    val showFps: Boolean = false
)

object OptionsStore {
    private const val PREFS = "funkforge_options"
    private const val FPS_KEY = "showFps"

    fun load(context: Context): GameOptions {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        return GameOptions(
            showFps = prefs.getBoolean(FPS_KEY, false)
        )
    }

    fun save(context: Context, options: GameOptions) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(FPS_KEY, options.showFps)
            .apply()
    }
}

@Composable
fun OptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var options by remember {
        mutableStateOf(OptionsStore.load(context))
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fk.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Fk.bg2)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "OPTIONS",
                color = Fk.cyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Fk.surface)
                .clickable {
                    val updated = options.copy(
                        showFps = !options.showFps
                    )
                    options = updated
                    OptionsStore.save(context, updated)
                }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "FPS",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Show the frame rate counter",
                    color = Fk.textSecondary,
                    fontSize = 13.sp
                )
            }

            Switch(
                checked = options.showFps,
                onCheckedChange = { enabled ->
                    val updated = options.copy(showFps = enabled)
                    options = updated
                    OptionsStore.save(context, updated)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Fk.bg,
                    checkedTrackColor = Fk.cyan
                )
            )
        }
    }
}

@Composable
fun FpsCounter(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fps by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        var frames = 0
        var lastTime = 0L

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastTime == 0L) {
                    lastTime = frameTimeNanos
                }

                frames++

                val elapsed = frameTimeNanos - lastTime

                if (elapsed >= 1_000_000_000L) {
                    fps = (
                        frames * 1_000_000_000L / elapsed
                    ).toInt()

                    frames = 0
                    lastTime = frameTimeNanos
                }

                choreographer.postFrameCallback(this)
            }
        }

        choreographer.postFrameCallback(callback)

        onDispose {
            choreographer.removeFrameCallback(callback)
        }
    }

    Text(
        text = "$fps FPS",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = Fk.cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}
