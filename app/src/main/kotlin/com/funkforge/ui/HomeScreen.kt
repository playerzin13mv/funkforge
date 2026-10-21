package com.funkforge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class HomeParticle(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val lane: Int,
    val size: Int,
    val drift: Float
)

@Composable
fun HomeScreen(
    hasProject: Boolean,
    onContinue: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onSecret: () -> Unit,
    onSoon: (String) -> Unit
) {
    val sprites = rememberSprites()
    val density = LocalDensity.current.density

    val particles = remember {
        List(24) {
            HomeParticle(
                x = Random.nextFloat(),
                speed = 0.4f + Random.nextFloat() * 1.4f,
                phase = Random.nextFloat(),
                lane = Random.nextInt(4),
                size = Random.nextInt(24, 54),
                drift = Random.nextFloat() * 2f - 1f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "home")

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 16000,
                easing = LinearEasing
            )
        ),
        label = "particles"
    )

    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = LinearEasing
            )
        ),
        label = "beat"
    )

    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    var taps by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Fk.bg, Fk.bg2, Fk.bg)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Fk.pink.copy(alpha = glow * 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * 0.15f,
                        size.height * 0.25f
                    ),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(
                    size.width * 0.15f,
                    size.height * 0.25f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Fk.cyan.copy(alpha = glow * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(
                        size.width * 0.85f,
                        size.height * 0.75f
                    ),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(
                    size.width * 0.85f,
                    size.height * 0.75f
                )
            )

            particles.forEach { particle ->
                val pixelSize = (particle.size * density).toInt()

                val progress = (
                    time * particle.speed + particle.phase
                ) % 1f

                val wave = sin(
                    progress * PI.toFloat() * 2f
                ) * particle.drift * 20f * density

                val x = particle.x *
                    (size.width - pixelSize) + wave

                val y = size.height -
                    progress * (size.height + pixelSize)

                drawImage(
                    image = sprites.notes[particle.lane],
                    dstOffset = IntOffset(
                        x.toInt(),
                        y.toInt()
                    ),
                    dstSize = IntSize(pixelSize, pixelSize),
                    alpha = 0.12f + glow * 0.12f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 22.dp,
                    vertical = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Fk.cyan.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        Fk.cyan.copy(alpha = 0.35f),
                        RoundedCornerShape(50)
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    )
            ) {
                Text(
                    text = "FUNKFORGE  •  V0.1",
                    color = Fk.cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val active = beat.toInt().coerceIn(0, 3)

                repeat(4) { index ->
                    val targetScale =
                        if (index == active) 1.22f else 0.94f

                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "noteScale"
                    )

                    Image(
                        bitmap = sprites.notes[index],
                        contentDescription = null,
                        modifier = Modifier
                            .size(62.dp)
                            .scale(scale)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

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
                    taps++

                    if (taps >= 7) {
                        taps = 0
                        onSecret()
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Create. Edit. Remix.",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your Friday Night Funkin' modding studio, right in your pocket.",
                color = Fk.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(34.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Fk.yellow,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "YOUR WORKSPACE",
                    color = Fk.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            if (hasProject) {
                HomeMenuCard(
                    icon = Icons.Default.PlayArrow,
                    title = "Continue Project",
                    subtitle = "Pick up where you left off",
                    accent = Fk.yellow,
                    onClick = onContinue
                )

                Spacer(Modifier.height(12.dp))
            }

            HomeMenuCard(
                icon = Icons.Default.Add,
                title = "New Project",
                subtitle = "Start creating your next mod",
                accent = Fk.pink,
                onClick = onNew
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.FileOpen,
                title = "Import Chart",
                subtitle = "Open a Psych Engine JSON chart",
                accent = Fk.cyan,
                onClick = onImport
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.SwapHoriz,
                title = "Funky Maker Converter",
                subtitle = "Coming soon",
                accent = Fk.green,
                onClick = {
                    onSoon(
                        "Coming soon! A sample Funky Maker mod is needed."
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            HomeMenuCard(
                icon = Icons.Default.Star,
                title = "Secret Zone",
                subtitle = "Discover something mysterious",
                accent = Fk.yellow,
                onClick = onSecret
            )

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    text = "MADE FOR MOBILE  •  KEEP FUNKIN'",
                    color = Fk.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HomeMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) {
            accent.copy(alpha = 0.17f)
        } else {
            Fk.surface.copy(alpha = 0.85f)
        },
        animationSpec = tween(180),
        label = "cardBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = accent.copy(
                    alpha = if (pressed) 0.9f else 0.35f
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Fk.text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Fk.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = accent.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}
