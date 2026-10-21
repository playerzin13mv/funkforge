package com.funkforge.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KotlinUi {
    val cornerSmall = 10.dp
    val cornerMedium = 16.dp
    val cornerLarge = 22.dp

    val gradient = Brush.horizontalGradient(
        listOf(Fk.cyan, Fk.pink)
    )

    val backgroundGradient = Brush.verticalGradient(
        listOf(Fk.bg, Fk.bg2, Fk.bg)
    )
}

@Composable
fun FkScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KotlinUi.backgroundGradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun FkTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = TextStyle(
            brush = KotlinUi.gradient,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black
        )
    )
}

@Composable
fun FkSubtitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = Fk.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}

@Composable
fun FkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Fk.cyan,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Fk.buttonDisabled
            pressed -> accent.copy(alpha = 0.75f)
            else -> accent
        },
        label = "buttonColor"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(KotlinUi.cornerMedium))
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = 20.dp,
                vertical = 15.dp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Fk.bg,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = text,
            color = if (enabled) Fk.bg else Fk.textMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun FkOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Fk.pink,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(KotlinUi.cornerMedium)

    Box(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.1f))
            .border(1.5.dp, accent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = 20.dp,
                vertical = 14.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) accent else Fk.textDisabled,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FkCard(
    modifier: Modifier = Modifier,
    accent: Color = Fk.purple,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(KotlinUi.cornerLarge)

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(Fk.surface)
            .border(
                1.dp,
                accent.copy(alpha = 0.4f),
                shape
            )
            .then(clickableModifier)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun FkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val shape = RoundedCornerShape(KotlinUi.cornerMedium)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Fk.surfaceVariant)
            .border(
                1.dp,
                Fk.border,
                shape
            )
            .padding(16.dp),
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(
            color = Fk.text,
            fontSize = 15.sp
        ),
        cursorBrush = SolidColor(Fk.cyan),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Fk.textMuted,
                        fontSize = 15.sp
                    )
                }

                innerTextField()
            }
        }
    )
}

@Composable
fun FkSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = Fk.yellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = Fk.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun FkDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Fk.divider)
    )
}

@Composable
fun FkLoading(
    text: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            color = Fk.cyan,
            trackColor = Fk.surfaceVariant,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )

        Text(
            text = text,
            color = Fk.textSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun FkInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Fk.cyan
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = title,
            color = Fk.textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = Fk.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
