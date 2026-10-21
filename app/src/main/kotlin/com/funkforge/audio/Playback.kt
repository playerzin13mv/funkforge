package com.funkforge.audio

import android.media.MediaPlayer
import java.io.File

/** Toca Inst + Voices juntos. */
class Playback {
    private var inst: MediaPlayer? = null
    private var voices: MediaPlayer? = null

    private fun create(path: String?): MediaPlayer? =
        if (path != null && File(path).exists()) try {
            MediaPlayer().apply { setDataSource(path); prepare() }
        } catch (e: Exception) { null } else null

    fun load(instPath: String?, voicesPath: String?) {
        release(); inst = create(instPath); voices = create(voicesPath)
    }

    val hasAudio: Boolean get() = inst != null
    val isPlaying: Boolean get() = try { inst?.isPlaying == true } catch (e: Exception) { false }
    val positionMs: Int get() = try { inst?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    fun play(ms: Int) {
        for (m in listOfNotNull(inst, voices)) { try { m.seekTo(ms); m.start() } catch (_: Exception) {} }
    }

    fun pause() {
        for (m in listOfNotNull(inst, voices)) { try { if (m.isPlaying) m.pause() } catch (_: Exception) {} }
    }

    fun release() {
        try { inst?.release(); voices?.release() } catch (_: Exception) {}
        inst = null; voices = null
    }
}
