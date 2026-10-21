package com.funkforge.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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

    var onComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val state: FunkinSoundState
        get() = currentState

    val isPlaying: Boolean
        get() = currentState == FunkinSoundState.Playing &&
            runCatching {
                (instrumental ?: vocals)?.isPlaying == true
            }.getOrDefault(false)

    val duration: Long
        get() = maxOf(
            safeDuration(instrumental),
            safeDuration(vocals)
        )

    val position: Long
        get() {
            val player = instrumental ?: vocals

            return if (player != null) {
                runCatching {
                    player.currentPosition.toLong()
                }.getOrDefault(fallbackPosition)
            } else {
                fallbackPosition
            }
        }

    private fun safeDuration(player: MediaPlayer?): Long {
        return runCatching {
            player?.duration?.toLong() ?: 0L
        }.getOrDefault(0L)
    }

    private fun safePosition(player: MediaPlayer?): Long {
        return runCatching {
            player?.currentPosition?.toLong() ?: 0L
        }.getOrDefault(0L)
    }

    private fun createPlayer(path: String): MediaPlayer {
        val file = File(path)

        require(file.exists() && file.isFile) {
            "Audio file not found: $path"
        }

        return MediaPlayer().apply {
            try {
                setAudioAttributes(createAttributes())
                setDataSource(file.absolutePath)
                prepare()
            } catch (e: Exception) {
                release()
                throw e
            }
        }
    }

    private fun createPlayer(uri: Uri): MediaPlayer {
        return MediaPlayer().apply {
            try {
                setAudioAttributes(createAttributes())

                appContext.contentResolver
                    .openAssetFileDescriptor(uri, "r")
                    ?.use { descriptor ->
                        setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.length
                        )
                    } ?: error("Unable to open audio URI")

                prepare()
            } catch (e: Exception) {
                release()
                throw e
            }
        }
    }

    private fun createAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    fun load(
        instPath: String?,
        voicesPath: String? = null
    ): Boolean {
        if (released) return false

        clearPlayers()

        return runCatching {
            instrumental = instPath
                ?.takeIf { it.isNotBlank() }
                ?.let { createPlayer(it) }

            vocals = voicesPath
                ?.takeIf { it.isNotBlank() }
                ?.let { createPlayer(it) }

            require(
                instrumental != null || vocals != null
            ) {
                "No audio files provided"
            }

            configurePlayers()

            fallbackPosition = 0L
            lastSyncTime = 0L
            currentState = FunkinSoundState.Ready

            true
        }.getOrElse {
            handleError(it)
            false
        }
    }

    fun load(
        instUri: Uri?,
        voicesUri: Uri? = null
    ): Boolean {
        if (released) return false

        clearPlayers()

        return runCatching {
            instrumental = instUri?.let {
                createPlayer(it)
            }

            vocals = voicesUri?.let {
                createPlayer(it)
            }

            require(
                instrumental != null || vocals != null
            ) {
                "No audio sources provided"
            }

            configurePlayers()

            fallbackPosition = 0L
            lastSyncTime = 0L
            currentState = FunkinSoundState.Ready

            true
        }.getOrElse {
            handleError(it)
            false
        }
    }

    private fun configurePlayers() {
        instrumental?.apply {
            isLooping = false
            setOnCompletionListener {
                handleCompletion()
            }
            setOnErrorListener { _, what, extra ->
                handleError(
                    IllegalStateException(
                        "Instrumental error: $what / $extra"
                    )
                )
                true
            }
        }

        vocals?.apply {
            isLooping = false

            if (instrumental == null) {
                setOnCompletionListener {
                    handleCompletion()
                }
            }

            setOnErrorListener { _, what, extra ->
                handleError(
                    IllegalStateException(
                        "Vocals error: $what / $extra"
                    )
                )
                true
            }
        }

        updateVolumes()
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

            currentState = FunkinSoundState.Playing
            lastSyncTime = 0L

            updatePlaybackSpeed()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun pause() {
        if (released || !isPlaying) return

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
            .coerceIn(0L, duration)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        runCatching {
            instrumental?.seekTo(target)
            vocals?.seekTo(target)
        }.onFailure {
            handleError(it)
        }

        fallbackPosition = target.toLong()
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
        instrumentalVolume = volume.coerceIn(0f, 1f)
        vocalsVolume = instrumentalVolume
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

        if (isPlaying) {
            updatePlaybackSpeed()
        }
    }

    private fun updateVolumes() {
        val inst = if (muted) 0f else instrumentalVolume
        val voice = if (muted) 0f else vocalsVolume

        runCatching {
            instrumental?.setVolume(inst, inst)
            vocals?.setVolume(voice, voice)
        }
    }

    private fun updatePlaybackSpeed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        listOfNotNull(instrumental, vocals).forEach { player ->
            runCatching {
                val params = player.playbackParams
                params.speed = playbackSpeed
                player.playbackParams = params
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

        if (now - lastSyncTime < intervalMs) {
            return
        }

        lastSyncTime = now

        val instPosition = safePosition(inst)
        val voicePosition = safePosition(voice)

        if (
            abs(instPosition - voicePosition) >
            toleranceMs
        ) {
            runCatching {
                voice.seekTo(
                    instPosition.coerceAtMost(
                        Int.MAX_VALUE.toLong()
                    ).toInt()
                )
            }
        }
    }

    private fun handleCompletion() {
        if (released) return

        if (looping) {
            restart()
            return
        }

        fallbackPosition = duration
        currentState = FunkinSoundState.Completed

        runCatching {
            vocals?.pause()
        }

        onComplete?.invoke()
    }

    private fun handleError(error: Throwable) {
        clearPlayers()
        currentState = FunkinSoundState.Error

        onError?.invoke(
            error.message ?: "Unknown audio error"
        )
    }

    fun getInfo(): FunkinSoundInfo {
        return FunkinSoundInfo(
            position = position,
            duration = duration,
            playing = isPlaying,
            state = currentState
        )
    }

    private fun clearPlayers() {
        val oldInst = instrumental
        val oldVoices = vocals

        instrumental = null
        vocals = null

        listOfNotNull(oldInst, oldVoices).forEach {
            runCatching {
                it.setOnCompletionListener(null)
                it.setOnErrorListener(null)
                it.release()
            }
        }
    }

    fun release() {
        if (released) return

        clearPlayers()

        released = true
        currentState = FunkinSoundState.Released

        onComplete = null
        onError = null
    }
}
