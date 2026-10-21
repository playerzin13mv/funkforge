
package com.funkforge.ui

import androidx.compose.ui.graphics.Color

object Fk {
    val bg = Color(0xFF140826)
    val bg2 = Color(0xFF2E1259)
    val bg3 = Color(0xFF3C1970)

    val surface = Color(0xFF21103D)
    val surfaceVariant = Color(0xFF351A60)
    val surfaceElevated = Color(0xFF46257A)

    val cyan = Color(0xFF00E5FF)
    val pink = Color(0xFFFF2D95)
    val yellow = Color(0xFFFFE14D)
    val purple = Color(0xFFAA69FF)
    val green = Color(0xFF12FA05)
    val red = Color(0xFFF9393F)

    val primary = cyan
    val secondary = pink
    val accent = yellow

    val text = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFC9B9E5)
    val textMuted = Color(0xFF9380B5)
    val textDisabled = Color(0xFF65547E)

    val border = Color(0xFF62418F)
    val borderFocused = cyan
    val divider = Color(0xFF422660)

    val success = Color(0xFF43F58A)
    val warning = yellow
    val error = Color(0xFFFF526C)
    val info = cyan

    val button = Color(0xFF48257C)
    val buttonHover = Color(0xFF6036A0)
    val buttonPressed = Color(0xFF301654)
    val buttonDisabled = Color(0xFF332448)

    val overlay = Color(0x99000000)
    val scrim = Color(0xCC0A0415)

    val laneLeft = Color(0xFFC24B99)
    val laneDown = Color(0xFF00FFFF)
    val laneUp = Color(0xFF12FA05)
    val laneRight = Color(0xFFF9393F)

    val lane = listOf(
        laneLeft,
        laneDown,
        laneUp,
        laneRight
    )

    val laneGlow = lane.map { it.copy(alpha = 0.45f) }

    val laneDimmed = lane.map { it.copy(alpha = 0.35f) }

    fun laneColor(direction: Int): Color {
        return lane.getOrElse(direction) { text }
    }

    fun withAlpha(color: Color, alpha: Float): Color {
        return color.copy(alpha = alpha.coerceIn(0f, 1f))
    }
}
