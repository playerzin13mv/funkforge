package com.funkforge.audio

import android.content.Context
import android.net.Uri

class Playback(
    context: Context
) {
    private val sound = FunkinSound(context)

    val hasAudio: Boolean
        get() = sound.state != FunkinSoundState.Idle &&
            sound.state != FunkinSoundState.Error &&
            sound.state != FunkinSoundState.Released

    val isPlaying: Boolean
        get() = sound.isPlaying

    val positionMs: Int
        get() = sound.position
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()

    val durationMs: Int
        get() = sound.duration
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()

    val state: FunkinSoundState
        get() = sound.state

    var onComplete: (() -> Unit)?
        get() = sound.onComplete
        set(value) {
            sound.onComplete = value
        }

    var onError: ((String) -> Unit)?
        get() = sound.onError
        set(value) {
            sound.onError = value
        }

    fun load(
        instPath: String?,
        voicesPath: String?
    ): Boolean {
        return sound.load(
            instPath = instPath,
            voicesPath = voicesPath
        )
    }

    fun load(
        instUri: Uri?,
        voicesUri: Uri?
    ): Boolean {
        return sound.load(
            instUri = instUri,
            voicesUri = voicesUri
        )
    }

    fun play(ms: Int = 0) {
        sound.seekTo(ms.toLong())
        sound.play()
    }

    fun resume() {
        sound.resume()
    }

    fun pause() {
        sound.pause()
    }

    fun stop() {
        sound.stop()
    }

    fun restart() {
        sound.restart()
    }

    fun seekTo(ms: Int) {
        sound.seekTo(ms.toLong())
    }

    fun setVolume(volume: Float) {
        sound.setVolume(volume)
    }

    fun setInstVolume(volume: Float) {
        sound.setInstrumentalVolume(volume)
    }

    fun setVoicesVolume(volume: Float) {
        sound.setVocalsVolume(volume)
    }

    fun setPlaybackSpeed(speed: Float) {
        sound.setPlaybackSpeed(speed)
    }

    fun setMuted(muted: Boolean) {
        sound.setMuted(muted)
    }

    fun setLooping(looping: Boolean) {
        sound.setLooping(looping)
    }

    fun synchronize() {
        sound.synchronize()
    }

    fun getInfo(): FunkinSoundInfo {
        return sound.getInfo()
    }

    fun release() {
        sound.release()
    }
}
