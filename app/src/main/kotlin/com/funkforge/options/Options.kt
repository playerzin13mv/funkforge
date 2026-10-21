package com.funkforge.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.funkforge.backend.AppLanguage
import com.funkforge.backend.Language
import com.funkforge.backend.TextKey
import com.funkforge.data.FPSCounter

private const val OPTIONS_PREFS = "funkforge_options"

@Composable
fun FunkForgeOptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            OPTIONS_PREFS,
            Context.MODE_PRIVATE
        )
    }

    val fpsEnabled by FPSCounter.rememberEnabled()

    var reducedMotion by remember {
        mutableStateOf(
            preferences.getBoolean("reduced_motion", false)
        )
    }

    var languageDialog by remember {
        mutableStateOf(false)
    }

    DisposableEffect(preferences) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener {
                prefs, key ->
                if (key == "reduced_motion") {
                    reducedMotion = prefs.getBoolean(
                        "reduced_motion",
                        false
                    )
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(
            listener
        )

        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(
                listener
            )
        }
    }

    val currentLanguage = Language.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(
                    "‹",
                    color = Fk.cyan,
                    fontSize = 30.sp
                )
            }

            Text(
                Language.get(TextKey.OPTIONS),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        OptionsSection(Language.get(TextKey.DISPLAY))

        OptionsSwitch(
            title = Language.get(TextKey.SHOW_FPS),
            description = Language.get(
                TextKey.SHOW_FPS_DESCRIPTION
            ),
            checked = fpsEnabled,
            onChange = {
                FPSCounter.setEnabled(context, it)
            }
        )

        OptionsSwitch(
            title = Language.get(TextKey.REDUCE_MOTION),
            description = Language.get(
                TextKey.REDUCE_MOTION_DESCRIPTION
            ),
            checked = reducedMotion,
            onChange = {
                reducedMotion = it
                preferences.edit()
                    .putBoolean("reduced_motion", it)
                    .apply()
            }
        )

        OptionsSection(Language.get(TextKey.LANGUAGE))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = { languageDialog = true },
            shape = RoundedCornerShape(16.dp),
            color = Fk.surface,
            border = BorderStroke(1.dp, Fk.border)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        Language.get(TextKey.LANGUAGE),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        currentLanguage.displayName,
                        color = Fk.textSecondary,
                        fontSize = 13.sp
                    )
                }

                Text(
                    "›",
                    color = Fk.cyan,
                    fontSize = 26.sp
                )
            }
        }

        OptionsSection(Language.get(TextKey.ABOUT))

        Surface(
            color = Fk.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Fk.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "FunkForge",
                    color = Fk.cyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "${Language.get(TextKey.VERSION)} 0.1.0",
                    color = Color.White
                )

                Text(
                    Language.get(TextKey.APP_SUBTITLE),
                    color = Fk.textSecondary
                )

                Text(
                    Language.get(TextKey.MADE_FOR_MOBILE),
                    color = Fk.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = {
                languageDialog = false
            },
            title = {
                Text(Language.get(TextKey.LANGUAGE))
            },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Language.setLanguage(
                                        context,
                                        language
                                    )
                                    languageDialog = false
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == language,
                                onClick = {
                                    Language.setLanguage(
                                        context,
                                        language
                                    )
                                    languageDialog = false
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(language.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { languageDialog = false }
                ) {
                    Text(Language.get(TextKey.CLOSE))
                }
            }
        )
    }
}

@Composable
private fun OptionsSection(title: String) {
    Text(
        text = title.uppercase(),
        color = Fk.cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 12.dp)
    )

    HorizontalDivider(color = Fk.border)
}

@Composable
private fun OptionsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Fk.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Fk.border)
    ) {
        Row(
            modifier = Modifier
                .clickable { onChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    description,
                    color = Fk.textSecondary,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onChange
            )
        }
    }
}
