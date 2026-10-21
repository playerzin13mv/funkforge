package com.funkforge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.funkforge.R

/** Ordem: 0 = esquerda, 1 = baixo, 2 = cima, 3 = direita. */
class Sprites(val notes: List<ImageBitmap>, val statics: List<ImageBitmap>)

@Composable
fun rememberSprites(): Sprites = Sprites(
    notes = listOf(
        ImageBitmap.imageResource(R.drawable.arrow_note_left),
        ImageBitmap.imageResource(R.drawable.arrow_note_down),
        ImageBitmap.imageResource(R.drawable.arrow_note_up),
        ImageBitmap.imageResource(R.drawable.arrow_note_right)
    ),
    statics = listOf(
        ImageBitmap.imageResource(R.drawable.arrow_static_left),
        ImageBitmap.imageResource(R.drawable.arrow_static_down),
        ImageBitmap.imageResource(R.drawable.arrow_static_up),
        ImageBitmap.imageResource(R.drawable.arrow_static_right)
    )
)
