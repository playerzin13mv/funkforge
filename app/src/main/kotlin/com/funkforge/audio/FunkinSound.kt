package com.funkforge.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import java.io.File
import kotlin.math.abs

enum class FunkinSoundState {
    Idle,
    Ready,
    Playing,
    Paused,
    Completed,
    Error,
    Released
}

data class FunkinSoundInfo(
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
    val state: FunkinSoundState = FunkinSoundState.Idle
)

class FunkinSound(
    context: Context
) {
    private val appContext = context.applicationContext

    private var instrumental: MediaPlayer? = null
    private var vocals: MediaPlayer? = null

    private var currentState = FunkinSoundState.Idle
    private var released = false

    private var instrumentalVolume = 1f
    private var vocalsVolume = 1f
    private var playbackSpeed = 1f
    private var muted = false
    private var looping = false

    private var lastSyncTime = 0L
    private var fallbackPosition = 0L
    private var fallbackStartTime = 0L

    var onComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val state: FunkinSoundState
        get() = currentState

    val isPlaying: Boolean
        get() = currentState == FunkinSoundState.Playing

    val duration: Long
        get() = maxOf(
            instrumental?.duration?.toLong() ?: 0L,
            vocals?.duration?.toLong() ?: 0L
        )

    val position: Long
        get() {
            val player = instrumental ?: vocals

            if (player != null) {
                return runCatching {
                    player.currentPosition.toLong()
                }.getOrDefault(fallbackPosition)
            }

            if (isPlaying) {
                return fallbackPosition + (
                    (SystemClock.elapsedRealtime() - fallbackStartTime) *
                        playbackSpeed
                ).toLong()
            }

            return fallbackPosition
        }

    fun load(
        instPath: String?,
        voicesPath: String? = null
    ): Boolean {
        if (released) return false

        releasePlayers()

        return try {
            instrumental = instPath
                ?.takeIf { it.isNotBlank() }
                ?.let { createPlayer(it) }

            vocals = voicesPath
                ?.takeIf { it.isNotBlank() }
                ?.let { createPlayer(it) }

            if (instrumental == null && vocals == null) {
                throw IllegalArgumentException(
                    "No audio files provided"
                )
            }

            configurePlayers()

            fallbackPosition = 0L
            fallbackStartTime = 0L
            lastSyncTime = 0L
            currentState = FunkinSoundState.Ready

            true
        } catch (e: Exception) {
            releasePlayers()
            currentState = FunkinSoundState.Error
            onError?.invoke(
                e.message ?: "Unable to load audio"
            )
            false
        }
    }

    fun load(
        instUri: Uri?,
        voicesUri: Uri? = null
    ): Boolean {
        if (released) return false

        releasePlayers()

        return try {
            instrumental = instUri?.let {
                createPlayer(it)
            }

            vocals = voicesUri?.let {
                createPlayer(it)
            }

            if (instrumental == null && vocals == null) {
                throw IllegalArgumentException(
                    "No audio sources provided"
                )
            }

            configurePlayers()

            fallbackPosition = 0L
            fallbackStartTime = 0L
            lastSyncTime = 0L
            currentState = FunkinSoundState.Ready

            true
        } catch (e: Exception) {
            releasePlayers()
            currentState = FunkinSoundState.Error
            onError?.invoke(
                e.message ?: "Unable to load audio"
            )
            false
        }
    }

    private fun createPlayer(path: String): MediaPlayer {
        val file = File(path)

        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException(
                "Audio file not found: $path"
            )
        }

        return MediaPlayer().apply {
            setAudioAttributes(audioAttributes())
            setDataSource(file.absolutePath)
            prepare()
        }
    }

    private fun createPlayer(uri: Uri): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(audioAttributes())

            appContext.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { descriptor ->
                    setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length
                    )
                } ?: throw IllegalArgumentException(
                    "Unable to open audio URI"
                )

            prepare()
        }
    }

    private fun audioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    private fun configurePlayers() {
        instrumental?.apply {
            isLooping = false
            setOnCompletionListener {
                handleCompletion()
            }
        }

        vocals?.apply {
            isLooping = false
            if (instrumental == null) {
                setOnCompletionListener {
                    handleCompletion()
                }
            }
        }

        updateVolumes()
        updatePlaybackSpeed()
    }

    fun play() {
        if (released) return

        if (
            currentState == FunkinSoundState.Idle ||
            currentState == FunkinSoundState.Error
        ) return

        if (currentState == FunkinSoundState.Completed) {
            seekTo(0L)
        }

        try {
            instrumental?.start()
            vocals?.start()

            fallbackStartTime = SystemClock.elapsedRealtime()
            currentState = FunkinSoundState.Playing
            lastSyncTime = 0L
        } catch (e: Exception) {
            currentState = FunkinSoundState.Error
            onError?.invoke(
                e.message ?: "Unable to start audio"
            )
        }
    }

    fun pause() {
        if (!isPlaying || released) return

        fallbackPosition = position

        runCatching {
            instrumental?.pause()
            vocals?.pause()
        }

        currentState = FunkinSoundState.Paused
    }

    fun resume() {
        if (
            currentState == FunkinSoundState.Paused ||
            currentState == FunkinSoundState.Ready
        ) {
            play()
        }
    }

    fun togglePause() {
        if (isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun stop() {
        if (released) return

        runCatching {
            instrumental?.pause()
            vocals?.pause()
        }

        seekTo(0L)
        fallbackPosition = 0L
        currentState = FunkinSoundState.Ready
    }

    fun restart() {
        if (released) return

        stop()
        play()
    }

    fun seekTo(milliseconds: Long) {
        if (released) return

        val target = milliseconds
            .coerceAtLeast(0L)
            .coerceAtMost(duration)

        runCatching {
            instrumental?.seekTo(target.toInt())
            vocals?.seekTo(target.toInt())
        }

        fallbackPosition = target
        fallbackStartTime = SystemClock.elapsedRealtime()
        lastSyncTime = 0L
    }

    fun setInstrumentalVolume(volume: Float) {
        instrumentalVolume = volume.coerceIn(0f, 1f)
        updateVolumes()
    }

    fun setVocalsVolume(volume: Float) {
        vocalsVolume = volume.coerceIn(0f, 1f)
        updateVolumes()
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        instrumentalVolume = safeVolume
        vocalsVolume = safeVolume
        updateVolumes()
    }

    fun setMuted(value: Boolean) {
        muted = value
        updateVolumes()
    }

    fun setLooping(value: Boolean) {
        looping = value
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2f)
        updatePlaybackSpeed()
    }

    private fun updateVolumes() {
        val inst = if (muted) 0f else instrumentalVolume
        val voice = if (muted) 0f else vocalsVolume

        instrumental?.setVolume(inst, inst)
        vocals?.setVolume(voice, voice)
    }

    private fun updatePlaybackSpeed() {
        listOfNotNull(instrumental, vocals).forEach { player ->
            runCatching {
                player.playbackParams = player.playbackParams.apply {
                    speed = playbackSpeed
                }
            }
        }
    }

    fun synchronize(
        toleranceMs: Long = 35L,
        intervalMs: Long = 250L
    ) {
        if (!isPlaying || released) return

        val inst = instrumental ?: return
        val voice = vocals ?: return

        val now = SystemClock.elapsedRealtime()

        if (now - lastSyncTime < intervalMs) return

        lastSyncTime = now

        val instPosition = inst.currentPosition
        val voicePosition = voice.currentPosition

        if (
            abs(instPosition - voicePosition) >
            toleranceMs
        ) {
            voice.seekTo(instPosition)
        }
    }

    private fun handleCompletion() {
        if (released) return

        if (looping) {
            seekTo(0L)
            play()
            return
        }

        fallbackPosition = duration
        currentState = FunkinSoundState.Completed
        onComplete?.invoke()
    }

    fun getInfo(): FunkinSoundInfo {
        return FunkinSoundInfo(
            position = position,
            duration = duration,
            playing = isPlaying,
            state = currentState
        )
    }

    private fun releasePlayers() {
        instrumental?.runCatching {
            reset()
            release()
        }

        vocals?.runCatching {
            reset()
            release()
        }

        instrumental = null
        vocals = null
    }

    fun release() {
        if (released) return

        releasePlayers()

        released = true
        currentState = FunkinSoundState.Released

        onComplete = null
        onError = null
    }
}
