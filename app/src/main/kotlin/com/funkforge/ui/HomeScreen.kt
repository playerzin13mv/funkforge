package com.funkforge.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funkforge.backend.AppLanguage
import com.funkforge.backend.Language
import com.funkforge.backend.TextKey
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class HomeParticle(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val radius: Float,
    val colorIndex: Int
)

private fun homeText(key: TextKey): String {
    return Language.get(key)
}

@Composable
fun HomeScreen(
    hasProject: Boolean,
    onContinue: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onSecret: () -> Unit,
    onSoon: (String) -> Unit,
    onOptions: () -> Unit = {},
    onEditor: () -> Unit = {
        if (hasProject) onContinue() else onNew()
    }
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            "funkforge_options",
            Context.MODE_PRIVATE
        )
    }

    var reducedMotion by remember {
        mutableStateOf(
            preferences.getBoolean("reduced_motion", false)
        )
    }

    val language = Language.current

    val sprites = rememberSprites()

    val particles = remember {
        List(30) {
            HomeParticle(
                x = Random.nextFloat(),
                phase = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 1.1f,
                radius = 1.5f + Random.nextFloat() * 3.5f,
                colorIndex = Random.nextInt(3)
            )
        }
    }

    val transition = rememberInfiniteTransition(
        label = "homeTransition"
    )

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = LinearEasing
            )
        ),
        label = "homeTime"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homePulse"
    )

    var secretTaps by remember {
        mutableIntStateOf(0)
    }

    val particleColors = listOf(
        Fk.pink,
        Fk.cyan,
        Fk.purple
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF10071E),
                        Fk.bg,
                        Fk.bg2,
                        Color(0xFF110820)
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val firstCenter = Offset(
                size.width * 0.1f,
                size.height * 0.2f
            )

            val secondCenter = Offset(
                size.width * 0.9f,
                size.height * 0.75f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Fk.pink.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = firstCenter,
                    radius = size.width
                ),
                center = firstCenter,
                radius = size.width
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Fk.cyan.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = secondCenter,
                    radius = size.width
                ),
                center = secondCenter,
                radius = size.width
            )

            particles.forEach { particle ->
                val progress = if (reducedMotion) {
                    particle.phase
                } else {
                    (
                        time * particle.speed +
                        particle.phase
                    ) % 1f
                }

                val drift = sin(
                    progress.toDouble() * PI * 2.0
                ).toFloat() * 14f

                drawCircle(
                    color = particleColors[
                        particle.colorIndex
                    ].copy(alpha = 0.22f),
                    radius = particle.radius,
                    center = Offset(
                        particle.x * size.width + drift,
                        (1f - progress) * size.height
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Fk.cyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        Fk.cyan.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "FUNKFORGE • V0.1",
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 8.dp
                        ),
                        color = Fk.cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Surface(
                    onClick = onOptions,
                    color = Fk.surfaceVariant,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(
                        1.dp,
                        Fk.border.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier.size(45.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = homeText(
                                TextKey.OPTIONS
                            ),
                            tint = Fk.cyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val active = (
                        (time * 32f).toInt() % 4
                    ) == index

                    Image(
                        bitmap = sprites.notes[index],
                        contentDescription = null,
                        modifier = Modifier
                            .size(53.dp)
                            .scale(
                                if (reducedMotion) 1f
                                else if (active) pulse
                                else 0.92f
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "FunkForge",
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = Brush.horizontalGradient(
                        listOf(Fk.cyan, Fk.pink)
                    ),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                ),
                modifier = Modifier.clickable {
                    secretTaps++

                    if (secretTaps >= 7) {
                        secretTaps = 0
                        onSecret()
                    }
                }
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = homeText(TextKey.APP_SLOGAN),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = homeText(TextKey.APP_SUBTITLE),
                color = Fk.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(25.dp))

            HomeSection(
                title = homeText(TextKey.WORKSPACE),
                icon = Icons.Default.Dashboard,
                color = Fk.yellow
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Edit,
                title = homeText(TextKey.CHART_EDITOR),
                description = homeText(
                    TextKey.CHART_EDITOR_DESCRIPTION
                ),
                color = Fk.pink,
                featured = true,
                onClick = onEditor
            )

            Spacer(Modifier.height(10.dp))

            if (hasProject) {
                HomeMenuCard(
                    icon = Icons.Default.PlayArrow,
                    title = homeText(
                        TextKey.CONTINUE_PROJECT
                    ),
                    description = homeText(
                        TextKey.CONTINUE_PROJECT_DESCRIPTION
                    ),
                    color = Fk.yellow,
                    onClick = onContinue
                )

                Spacer(Modifier.height(10.dp))
            }

            HomeMenuCard(
                icon = Icons.Default.AddCircle,
                title = homeText(TextKey.NEW_PROJECT),
                description = homeText(
                    TextKey.NEW_PROJECT_DESCRIPTION
                ),
                color = Fk.purple,
                onClick = onNew
            )

            Spacer(Modifier.height(20.dp))

            HomeSection(
                title = homeText(TextKey.TOOLS),
                icon = Icons.Default.Build,
                color = Fk.cyan
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.FileOpen,
                title = homeText(TextKey.IMPORT_CHART),
                description = homeText(
                    TextKey.IMPORT_CHART_DESCRIPTION
                ),
                color = Fk.cyan,
                onClick = onImport
            )

            Spacer(Modifier.height(10.dp))

            HomeMenuCard(
                icon = Icons.Default.FolderOpen,
                title = homeText(TextKey.MOD_PLAYTEST),
                description = homeText(
                    TextKey.MOD_PLAYTEST_DESCRIPTION
                ),
                color = Fk.green,
                onClick = {
                    onSoon("Open Mod Folder")
                }
            )

            Spacer(Modifier.height(10.dp))

            HomeMenuCard(
                icon = Icons.Default.SwapHoriz,
                title = homeText(
                    TextKey.FUNKY_MAKER_CONVERTER
                ),
                description = homeText(
                    TextKey.COMING_SOON
                ),
                color = Fk.yellow,
                onClick = {
                    onSoon(
                        homeText(
                            TextKey.COMING_SOON
                        )
                    )
                }
            )

            Spacer(Modifier.height(20.dp))

            HomeSection(
                title = homeText(TextKey.MORE),
                icon = Icons.Default.AutoAwesome,
                color = Fk.purple
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Star,
                title = homeText(TextKey.SECRET_ZONE),
                description = homeText(
                    TextKey.SECRET_ZONE_DESCRIPTION
                ),
                color = Fk.purple,
                onClick = onSecret
            )

            Spacer(Modifier.height(26.dp))

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = homeText(
                        TextKey.MADE_FOR_MOBILE
                    ),
                    modifier = Modifier.padding(
                        horizontal = 17.dp,
                        vertical = 10.dp
                    ),
                    color = Fk.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(9.dp))

        Text(
            text = title.uppercase(),
            color = Fk.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.3.sp
        )

        Spacer(Modifier.width(12.dp))

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Fk.border.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun HomeMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    featured: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    val interaction = remember {
        MutableInteractionSource()
    }

    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menuCardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = color.copy(
            alpha = if (pressed) 0.95f
            else if (featured) 0.7f
            else 0.3f
        ),
        label = "menuCardBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (featured) {
                        listOf(
                            Color(0xFF482055),
                            Color(0xFF291440)
                        )
                    } else {
                        listOf(
                            Fk.surface,
                            Fk.surfaceVariant.copy(
                                alpha = 0.65f
                            )
                        )
                    }
                )
            )
            .border(
                1.dp,
                borderColor,
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    color.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = description,
                color = Fk.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FunkForgeOptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val preferences = remember(context) {
        context.getSharedPreferences(
            "funkforge_options",
            Context.MODE_PRIVATE
        )
    }

    var showFps by remember {
        mutableStateOf(
            preferences.getBoolean("show_fps", false)
        )
    }

    var reducedMotion by remember {
        mutableStateOf(
            preferences.getBoolean("reduced_motion", false)
        )
    }

    var languageDialog by remember {
        mutableStateOf(false)
    }

    val currentLanguage = Language.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fk.bg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = homeText(TextKey.BACK),
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = homeText(TextKey.OPTIONS).uppercase(),
                color = Fk.cyan,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(25.dp))

        HomeSection(
            title = homeText(TextKey.DISPLAY),
            icon = Icons.Default.Tune,
            color = Fk.yellow
        )

        Spacer(Modifier.height(12.dp))

        HomeOptionSwitch(
            icon = Icons.Default.Speed,
            title = homeText(TextKey.SHOW_FPS),
            description = homeText(
                TextKey.SHOW_FPS_DESCRIPTION
            ),
            checked = showFps,
            onChange = {
                showFps = it

                preferences.edit()
                    .putBoolean("show_fps", it)
                    .apply()
            }
        )

        Spacer(Modifier.height(10.dp))

        HomeOptionSwitch(
            icon = Icons.Default.Animation,
            title = homeText(TextKey.REDUCE_MOTION),
            description = homeText(
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

        Spacer(Modifier.height(24.dp))

        HomeSection(
            title = homeText(TextKey.LANGUAGE),
            icon = Icons.Default.Language,
            color = Fk.cyan
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            onClick = {
                languageDialog = true
            },
            color = Fk.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                Fk.border.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = Fk.cyan
                )

                Spacer(Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        homeText(TextKey.LANGUAGE),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        currentLanguage.displayName,
                        color = Fk.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Fk.cyan
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        HomeSection(
            title = homeText(TextKey.ABOUT),
            icon = Icons.Default.Info,
            color = Fk.purple
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            color = Fk.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                Fk.border.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "FunkForge",
                    color = Fk.cyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "${homeText(TextKey.VERSION)} 0.1.0",
                    color = Color.White
                )

                Text(
                    homeText(TextKey.APP_SUBTITLE),
                    color = Fk.textSecondary,
                    fontSize = 13.sp
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
                Text(homeText(TextKey.LANGUAGE))
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
                                .padding(vertical = 8.dp),
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
                    onClick = {
                        languageDialog = false
                    }
                ) {
                    Text(homeText(TextKey.CLOSE))
                }
            }
        )
    }
}

@Composable
private fun HomeOptionSwitch(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Surface(
        color = Fk.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            Fk.border.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onChange(!checked)
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Fk.cyan,
                modifier = Modifier.size(25.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
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
