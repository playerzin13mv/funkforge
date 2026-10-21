package com.funkforge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class MenuParticle(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val size: Float,
    val color: Int
)

@Composable
fun HomeScreen(
    hasProject: Boolean,
    onContinue: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onSecret: () -> Unit,
    onSoon: (String) -> Unit,
    onOptions: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            "funkforge_options",
            0
        )
    }

    var reducedMotion by remember {
        mutableStateOf(
            preferences.getBoolean("reduced_motion", false)
        )
    }

    val particles = remember {
        List(24) {
            MenuParticle(
                x = Random.nextFloat(),
                phase = Random.nextFloat(),
                speed = 0.4f + Random.nextFloat(),
                size = 2f + Random.nextFloat() * 4f,
                color = Random.nextInt(3)
            )
        }
    }

    val transition = rememberInfiniteTransition(
        label = "home"
    )

    val animation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                16000,
                easing = LinearEasing
            )
        ),
        label = "background"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    var secretTaps by remember {
        mutableIntStateOf(0)
    }

    val colors = listOf(
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
                        Color(0xFF251043)
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Fk.pink.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * 0.1f,
                        size.height * 0.25f
                    ),
                    radius = size.width
                ),
                center = Offset(
                    size.width * 0.1f,
                    size.height * 0.25f
                ),
                radius = size.width
            )

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Fk.cyan.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * 0.9f,
                        size.height * 0.8f
                    ),
                    radius = size.width
                ),
                center = Offset(
                    size.width * 0.9f,
                    size.height * 0.8f
                ),
                radius = size.width
            )

            particles.forEach { particle ->
                val progress = if (reducedMotion) {
                    particle.phase
                } else {
                    (
                        animation * particle.speed +
                        particle.phase
                    ) % 1f
                }

                val wave = sin(
                    progress * PI * 2.0
                ).toFloat() * 15f

                drawCircle(
                    color = colors[particle.color].copy(
                        alpha = 0.25f
                    ),
                    radius = particle.size,
                    center = Offset(
                        particle.x * size.width + wave,
                        size.height * (1f - progress)
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
                    shape = RoundedCornerShape(50),
                    color = Fk.cyan.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        Fk.cyan.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "FUNKFORGE  •  V0.1",
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 8.dp
                        ),
                        color = Fk.cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onOptions,
                    modifier = Modifier
                        .background(
                            Fk.surfaceVariant,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Options",
                        tint = Fk.cyan
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sprites = rememberSprites()

                repeat(4) { index ->
                    Image(
                        bitmap = sprites.notes[index],
                        contentDescription = null,
                        modifier = Modifier
                            .size(53.dp)
                            .scale(
                                if (reducedMotion) 1f
                                else if (
                                    (animation * 16).toInt() % 4 == index
                                ) pulse
                                else 0.95f
                            )
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = "FunkForge",
                color = Color.White,
                fontSize = 43.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                modifier = Modifier.clickable {
                    secretTaps++

                    if (secretTaps >= 7) {
                        secretTaps = 0
                        onSecret()
                    }
                }
            )

            Text(
                text = "CREATE • EDIT • REMIX",
                color = Fk.cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(9.dp))

            Text(
                text = "Your Friday Night Funkin' creation studio.",
                color = Fk.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(25.dp))

            HomeHeading(
                "WORKSPACE",
                Icons.Default.Dashboard
            )

            Spacer(Modifier.height(10.dp))

            HomeTile(
                icon = Icons.Default.Edit,
                title = "Chart Editor",
                description = if (hasProject) {
                    "Open your current chart and continue editing"
                } else {
                    "Create your first chart and start editing"
                },
                color = Fk.pink,
                featured = true,
                onClick = {
                    if (hasProject) onContinue()
                    else onNew()
                }
            )

            Spacer(Modifier.height(10.dp))

            if (hasProject) {
                HomeTile(
                    icon = Icons.Default.FolderOpen,
                    title = "Continue Project",
                    description = "Resume your saved FunkForge project",
                    color = Fk.yellow,
                    onClick = onContinue
                )

                Spacer(Modifier.height(10.dp))
            }

            HomeTile(
                icon = Icons.Default.AddCircle,
                title = "New Project",
                description = "Create a song and configure your mod",
                color = Fk.purple,
                onClick = onNew
            )

            Spacer(Modifier.height(18.dp))

            HomeHeading(
                "TOOLS",
                Icons.Default.Build
            )

            Spacer(Modifier.height(10.dp))

            HomeTile(
                icon = Icons.Default.FileOpen,
                title = "Import Chart",
                description = "Import a Psych Engine JSON chart",
                color = Fk.cyan,
                onClick = onImport
            )

            Spacer(Modifier.height(10.dp))

            HomeTile(
                icon = Icons.Default.Folder,
                title = "Mod Playtest",
                description = "Open a mod folder and test its songs",
                color = Fk.green,
                onClick = {
                    onSoon("Open Mod Folder")
                }
            )

            Spacer(Modifier.height(10.dp))

            HomeTile(
                icon = Icons.Default.SwapHoriz,
                title = "Funky Maker Converter",
                description = "Conversion tools • Coming soon",
                color = Fk.yellow,
                onClick = {
                    onSoon(
                        "Funky Maker Converter is coming soon"
                    )
                }
            )

            Spacer(Modifier.height(18.dp))

            HomeHeading(
                "MORE",
                Icons.Default.AutoAwesome
            )

            Spacer(Modifier.height(10.dp))

            HomeTile(
                icon = Icons.Default.Star,
                title = "Secret Zone",
                description = "Discover hidden FunkForge content",
                color = Fk.purple,
                onClick = onSecret
            )

            Spacer(Modifier.height(25.dp))

            Text(
                text = "MADE FOR MOBILE • KEEP FUNKIN'",
                color = Fk.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HomeHeading(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Fk.yellow,
            modifier = Modifier.size(17.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            color = Fk.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp
        )
    }
}

@Composable
private fun HomeTile(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    featured: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (featured) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF49215D),
                            Color(0xFF291544)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Fk.surface,
                            Fk.surfaceVariant.copy(alpha = 0.65f)
                        )
                    )
                }
            )
            .border(
                1.dp,
                color.copy(
                    alpha = if (featured) 0.75f else 0.3f
                ),
                shape
            )
            .clickable(onClick = onClick)
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
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(27.dp)
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

        Spacer(Modifier.width(6.dp))

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
fun FunkForgeOptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            "funkforge_options",
            0
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fk.bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "OPTIONS",
                color = Fk.cyan,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "DISPLAY",
            color = Fk.yellow,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        OptionSwitch(
            icon = Icons.Default.Speed,
            title = "Show FPS",
            description = "Enable the FPS counter",
            checked = showFps,
            onChange = {
                showFps = it
                preferences.edit()
                    .putBoolean("show_fps", it)
                    .apply()
            }
        )

        Spacer(Modifier.height(12.dp))

        OptionSwitch(
            icon = Icons.Default.Animation,
            title = "Reduce Motion",
            description = "Reduce animated effects in the menu",
            checked = reducedMotion,
            onChange = {
                reducedMotion = it
                preferences.edit()
                    .putBoolean("reduced_motion", it)
                    .apply()
            }
        )

        Spacer(Modifier.height(26.dp))

        Text(
            "ABOUT",
            color = Fk.yellow,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            color = Fk.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                Fk.border.copy(alpha = 0.5f)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    "Version 0.1.0",
                    color = Color.White
                )

                Text(
                    "A mobile chart editor for Friday Night Funkin' modding.",
                    color = Fk.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OptionSwitch(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Fk.surface)
            .clickable {
                onChange(!checked)
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Fk.cyan,
            modifier = Modifier.size(25.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

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
