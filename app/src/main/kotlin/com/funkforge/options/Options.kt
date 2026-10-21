
package com.funkforge.options

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.ui.Fk
import java.util.Locale

data class GameOptions(
    val downscroll: Boolean = false,
    val middlescroll: Boolean = false,
    val ghostTapping: Boolean = true,
    val opponentNotes: Boolean = true,
    val noteSplashes: Boolean = true,
    val flashingLights: Boolean = true,
    val lowQuality: Boolean = false,
    val showFps: Boolean = false,
    val showAccuracy: Boolean = true,
    val showCombo: Boolean = true,
    val hitSounds: Boolean = false,
    val autoPause: Boolean = true,
    val noteSpeed: Float = 1f,
    val musicVolume: Float = 1f,
    val vocalsVolume: Float = 1f,
    val hitSoundVolume: Float = 0.5f,
    val noteOffset: Float = 0f,
    val backgroundDim: Float = 0.15f
)

object OptionsStore {
    private const val PREFS = "funkforge_options"

    fun load(context: Context): GameOptions {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        return GameOptions(
            downscroll = prefs.getBoolean("downscroll", false),
            middlescroll = prefs.getBoolean("middlescroll", false),
            ghostTapping = prefs.getBoolean("ghostTapping", true),
            opponentNotes = prefs.getBoolean("opponentNotes", true),
            noteSplashes = prefs.getBoolean("noteSplashes", true),
            flashingLights = prefs.getBoolean("flashingLights", true),
            lowQuality = prefs.getBoolean("lowQuality", false),
            showFps = prefs.getBoolean("showFps", false),
            showAccuracy = prefs.getBoolean("showAccuracy", true),
            showCombo = prefs.getBoolean("showCombo", true),
            hitSounds = prefs.getBoolean("hitSounds", false),
            autoPause = prefs.getBoolean("autoPause", true),
            noteSpeed = prefs.getFloat("noteSpeed", 1f),
            musicVolume = prefs.getFloat("musicVolume", 1f),
            vocalsVolume = prefs.getFloat("vocalsVolume", 1f),
            hitSoundVolume = prefs.getFloat("hitSoundVolume", 0.5f),
            noteOffset = prefs.getFloat("noteOffset", 0f),
            backgroundDim = prefs.getFloat("backgroundDim", 0.15f)
        )
    }

    fun save(context: Context, options: GameOptions) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean("downscroll", options.downscroll)
            .putBoolean("middlescroll", options.middlescroll)
            .putBoolean("ghostTapping", options.ghostTapping)
            .putBoolean("opponentNotes", options.opponentNotes)
            .putBoolean("noteSplashes", options.noteSplashes)
            .putBoolean("flashingLights", options.flashingLights)
            .putBoolean("lowQuality", options.lowQuality)
            .putBoolean("showFps", options.showFps)
            .putBoolean("showAccuracy", options.showAccuracy)
            .putBoolean("showCombo", options.showCombo)
            .putBoolean("hitSounds", options.hitSounds)
            .putBoolean("autoPause", options.autoPause)
            .putFloat("noteSpeed", options.noteSpeed)
            .putFloat("musicVolume", options.musicVolume)
            .putFloat("vocalsVolume", options.vocalsVolume)
            .putFloat("hitSoundVolume", options.hitSoundVolume)
            .putFloat("noteOffset", options.noteOffset)
            .putFloat("backgroundDim", options.backgroundDim)
            .apply()
    }

    fun reset(context: Context): GameOptions {
        val defaults = GameOptions()
        save(context, defaults)
        return defaults
    }
}

@Composable
fun OptionsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var options by remember {
        mutableStateOf(OptionsStore.load(context))
    }

    var showResetDialog by remember {
        mutableStateOf(false)
    }

    fun update(value: GameOptions) {
        options = value
        OptionsStore.save(context, value)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fk.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Fk.bg2)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Fk.text
                )
            }

            Text(
                text = "OPTIONS",
                color = Fk.cyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    showResetDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset options",
                    tint = Fk.yellow
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OptionSection("GAMEPLAY")

            OptionToggle(
                "Downscroll",
                "Notes move from top to bottom",
                options.downscroll
            ) {
                update(options.copy(downscroll = it))
            }

            OptionToggle(
                "Middlescroll",
                "Center the player note receptors",
                options.middlescroll
            ) {
                update(options.copy(middlescroll = it))
            }

            OptionToggle(
                "Ghost Tapping",
                "Allow pressing without hitting notes",
                options.ghostTapping
            ) {
                update(options.copy(ghostTapping = it))
            }

            OptionToggle(
                "Opponent Notes",
                "Display opponent notes during gameplay",
                options.opponentNotes
            ) {
                update(options.copy(opponentNotes = it))
            }

            OptionSlider(
                "Note Speed",
                "Adjust the note scrolling speed",
                options.noteSpeed,
                0.5f..4f,
                "%.2fx".format(Locale.US, options.noteSpeed)
            ) {
                update(options.copy(noteSpeed = it))
            }

            OptionSlider(
                "Note Offset",
                "Adjust hit timing calibration",
                options.noteOffset,
                -250f..250f,
                "${options.noteOffset.toInt()} ms"
            ) {
                update(options.copy(noteOffset = it))
            }

            OptionSection("VISUALS")

            OptionToggle(
                "Note Splashes",
                "Display effects on successful hits",
                options.noteSplashes
            ) {
                update(options.copy(noteSplashes = it))
            }

            OptionToggle(
                "Flashing Lights",
                "Enable flashing visual effects",
                options.flashingLights
            ) {
                update(options.copy(flashingLights = it))
            }

            OptionToggle(
                "Low Quality",
                "Reduce visual effects for performance",
                options.lowQuality
            ) {
                update(options.copy(lowQuality = it))
            }

            OptionToggle(
                "Show FPS",
                "Display the frame rate counter",
                options.showFps
            ) {
                update(options.copy(showFps = it))
            }

            OptionToggle(
                "Show Accuracy",
                "Display accuracy during gameplay",
                options.showAccuracy
            ) {
                update(options.copy(showAccuracy = it))
            }

            OptionToggle(
                "Show Combo",
                "Display the current combo counter",
                options.showCombo
            ) {
                update(options.copy(showCombo = it))
            }

            OptionSlider(
                "Background Dim",
                "Adjust background darkness",
                options.backgroundDim,
                0f..0.85f,
                "${(options.backgroundDim * 100).toInt()}%"
            ) {
                update(options.copy(backgroundDim = it))
            }

            OptionSection("AUDIO")

            OptionSlider(
                "Music Volume",
                "Instrumental audio volume",
                options.musicVolume,
                0f..1f,
                "${(options.musicVolume * 100).toInt()}%"
            ) {
                update(options.copy(musicVolume = it))
            }

            OptionSlider(
                "Vocals Volume",
                "Character vocals volume",
                options.vocalsVolume,
                0f..1f,
                "${(options.vocalsVolume * 100).toInt()}%"
            ) {
                update(options.copy(vocalsVolume = it))
            }

            OptionToggle(
                "Hit Sounds",
                "Play a sound when hitting notes",
                options.hitSounds
            ) {
                update(options.copy(hitSounds = it))
            }

            if (options.hitSounds) {
                OptionSlider(
                    "Hit Sound Volume",
                    "Adjust hit sound loudness",
                    options.hitSoundVolume,
                    0f..1f,
                    "${(options.hitSoundVolume * 100).toInt()}%"
                ) {
                    update(options.copy(hitSoundVolume = it))
                }
            }

            OptionSection("SYSTEM")

            OptionToggle(
                "Auto Pause",
                "Pause gameplay when the app loses focus",
                options.autoPause
            ) {
                update(options.copy(autoPause = it))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "FUNKFORGE SETTINGS",
                color = Fk.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
            },
            title = {
                Text("Reset Settings?")
            },
            text = {
                Text(
                    "All settings will be restored to their default values."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        options = OptionsStore.reset(context)
                        showResetDialog = false
                    }
                ) {
                    Text(
                        "Reset",
                        color = Fk.pink
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Fk.surface,
            titleContentColor = Fk.text,
            textContentColor = Fk.textSecondary
        )
    }
}

@Composable
private fun OptionSection(title: String) {
    Spacer(Modifier.height(8.dp))

    Text(
        text = title,
        color = Fk.yellow,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun OptionToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Fk.surface)
            .clickable {
                onCheckedChange(!checked)
            }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                color = Fk.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                subtitle,
                color = Fk.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun OptionSlider(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Fk.surface)
            .padding(15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = Fk.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                valueText,
                color = Fk.cyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            color = Fk.textSecondary,
            fontSize = 12.sp
        )

        Slider(
            value = value.coerceIn(
                range.start,
                range.endInclusive
            ),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
