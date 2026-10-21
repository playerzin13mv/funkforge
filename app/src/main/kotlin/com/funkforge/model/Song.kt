package com.funkforge.model

import java.io.File
import java.util.Locale
import kotlin.math.roundToLong

enum class SongDifficulty(
    val displayName: String,
    val fileSuffix: String
) {
    EASY("Easy", "-easy"),
    NORMAL("Normal", ""),
    HARD("Hard", "-hard"),
    ERECT("Erect", "-erect"),
    NIGHTMARE("Nightmare", "-nightmare"),
    CUSTOM("Custom", "");

    companion object {
        fun fromName(value: String): SongDifficulty {
            return entries.firstOrNull {
                it.name.equals(value, true) ||
                    it.displayName.equals(value, true)
            } ?: CUSTOM
        }
    }
}

enum class AudioFormat(
    val extension: String
) {
    OGG("ogg"),
    MP3("mp3"),
    WAV("wav"),
    M4A("m4a"),
    FLAC("flac"),
    UNKNOWN("");

    companion object {
        fun fromPath(path: String?): AudioFormat {
            val extension = path
                ?.substringAfterLast('.', "")
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            return entries.firstOrNull {
                it.extension.isNotEmpty() &&
                    it.extension == extension
            } ?: UNKNOWN
        }
    }
}

data class SongAudio(
    val instrumentalPath: String? = null,
    val voicesPath: String? = null,
    val instrumentalVolume: Float = 1f,
    val voicesVolume: Float = 1f,
    val offsetMs: Double = 0.0
) {
    val hasInstrumental: Boolean
        get() = !instrumentalPath.isNullOrBlank()

    val hasVoices: Boolean
        get() = !voicesPath.isNullOrBlank()

    val instrumentalFormat: AudioFormat
        get() = AudioFormat.fromPath(instrumentalPath)

    val voicesFormat: AudioFormat
        get() = AudioFormat.fromPath(voicesPath)

    fun instrumentalFile(): File? {
        return instrumentalPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
    }

    fun voicesFile(): File? {
        return voicesPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
    }

    fun hasValidInstrumental(): Boolean {
        return instrumentalFile()?.let {
            it.isFile && it.canRead() && it.length() > 0L
        } ?: false
    }

    fun hasValidVoices(): Boolean {
        return voicesFile()?.let {
            it.isFile && it.canRead() && it.length() > 0L
        } ?: false
    }

    fun normalized(): SongAudio {
        return copy(
            instrumentalPath = instrumentalPath
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            voicesPath = voicesPath
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            instrumentalVolume = instrumentalVolume
                .safeVolume(),
            voicesVolume = voicesVolume
                .safeVolume(),
            offsetMs = if (offsetMs.isFinite()) {
                offsetMs.coerceIn(-60000.0, 60000.0)
            } else {
                0.0
            }
        )
    }
}

data class SongMetadata(
    val title: String = "Untitled",
    val artist: String = "Unknown Artist",
    val composer: String = "",
    val charter: String = "",
    val album: String = "",
    val description: String = "",
    val difficulty: SongDifficulty = SongDifficulty.NORMAL,
    val customDifficulty: String = "",
    val tags: List<String> = emptyList()
) {
    val difficultyName: String
        get() = if (
            difficulty == SongDifficulty.CUSTOM
        ) {
            customDifficulty.ifBlank { "Custom" }
        } else {
            difficulty.displayName
        }

    fun normalized(): SongMetadata {
        return copy(
            title = title.trim().ifBlank { "Untitled" },
            artist = artist.trim().ifBlank {
                "Unknown Artist"
            },
            composer = composer.trim(),
            charter = charter.trim(),
            album = album.trim(),
            description = description.trim(),
            customDifficulty = customDifficulty.trim(),
            tags = tags.map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        )
    }
}

data class Song(
    val metadata: SongMetadata = SongMetadata(),
    val audio: SongAudio = SongAudio(),
    val chart: Chart = Chart(),
    val needsVoices: Boolean = true,
    val previewStartMs: Double = 0.0,
    val previewEndMs: Double = 0.0
) {
    val title: String
        get() = metadata.title

    val difficulty: SongDifficulty
        get() = metadata.difficulty

    val bpm: Double
        get() = chart.bpm

    val speed: Double
        get() = chart.speed

    val noteCount: Int
        get() = chart.notes.size

    val eventCount: Int
        get() = chart.events.size

    val chartDurationMs: Double
        get() = chart.durationMs

    val durationSeconds: Double
        get() = chartDurationMs / 1000.0

    val hasAudio: Boolean
        get() = audio.hasValidInstrumental()

    val hasChart: Boolean
        get() = chart.notes.isNotEmpty()

    val songId: String
        get() = slug(metadata.title)

    val chartFileName: String
        get() {
            val suffix = if (
                metadata.difficulty == SongDifficulty.CUSTOM
            ) {
                "-${slug(metadata.customDifficulty)}"
            } else {
                metadata.difficulty.fileSuffix
            }

            return "$songId$suffix.json"
        }

    fun withChart(newChart: Chart): Song {
        return copy(
            chart = newChart,
            metadata = metadata.copy(
                title = newChart.song
            )
        )
    }

    fun withTitle(newTitle: String): Song {
        val title = newTitle.trim().ifBlank {
            "Untitled"
        }

        return copy(
            metadata = metadata.copy(title = title),
            chart = chart.copy(song = title)
        )
    }

    fun withInstrumental(path: String?): Song {
        return copy(
            audio = audio.copy(
                instrumentalPath = path
            )
        )
    }

    fun withVoices(path: String?): Song {
        return copy(
            audio = audio.copy(
                voicesPath = path
            )
        )
    }

    fun withDifficulty(
        difficulty: SongDifficulty,
        customName: String = ""
    ): Song {
        return copy(
            metadata = metadata.copy(
                difficulty = difficulty,
                customDifficulty = customName
            )
        )
    }

    fun normalized(): Song {
        val cleanMetadata = metadata.normalized()

        return copy(
            metadata = cleanMetadata,
            audio = audio.normalized(),
            chart = chart.normalized().copy(
                song = cleanMetadata.title
            ),
            previewStartMs = previewStartMs
                .safeTime(),
            previewEndMs = previewEndMs
                .safeTime()
                .coerceAtLeast(previewStartMs.safeTime())
        )
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        errors.addAll(chart.validate())

        if (metadata.title.isBlank()) {
            errors.add("Song title cannot be empty")
        }

        if (
            metadata.difficulty == SongDifficulty.CUSTOM &&
            metadata.customDifficulty.isBlank()
        ) {
            errors.add("Custom difficulty name is empty")
        }

        if (
            audio.instrumentalVolume.isNaN() ||
            audio.instrumentalVolume < 0f ||
            audio.instrumentalVolume > 1f
        ) {
            errors.add("Invalid instrumental volume")
        }

        if (
            audio.voicesVolume.isNaN() ||
            audio.voicesVolume < 0f ||
            audio.voicesVolume > 1f
        ) {
            errors.add("Invalid voices volume")
        }

        if (
            !previewStartMs.isFinite() ||
            !previewEndMs.isFinite() ||
            previewStartMs < 0.0 ||
            previewEndMs < 0.0
        ) {
            errors.add("Invalid preview time")
        }

        return errors
    }

    companion object {
        fun fromProject(
            project: Project,
            difficulty: SongDifficulty =
                SongDifficulty.NORMAL
        ): Song {
            return Song(
                metadata = SongMetadata(
                    title = project.chart.song,
                    difficulty = difficulty
                ),
                audio = SongAudio(
                    instrumentalPath = project.instPath,
                    voicesPath = project.voicesPath
                ),
                chart = project.chart,
                needsVoices = project.voicesPath != null
            )
        }

        fun create(
            title: String = "Untitled",
            bpm: Double = 120.0,
            difficulty: SongDifficulty =
                SongDifficulty.NORMAL
        ): Song {
            val safeBpm = if (
                bpm.isFinite() && bpm > 0.0
            ) {
                bpm.coerceIn(20.0, 999.0)
            } else {
                120.0
            }

            val safeTitle = title.trim().ifBlank {
                "Untitled"
            }

            return Song(
                metadata = SongMetadata(
                    title = safeTitle,
                    difficulty = difficulty
                ),
                chart = Chart(
                    song = safeTitle,
                    bpmChanges = listOf(
                        BpmChange(0.0, safeBpm)
                    )
                )
            )
        }

        fun slug(value: String): String {
            return value
                .trim()
                .lowercase(Locale.ROOT)
                .replace(
                    Regex("[^a-z0-9]+"),
                    "-"
                )
                .trim('-')
                .ifBlank { "untitled" }
        }
    }
}

private fun Float.safeVolume(): Float {
    return if (isFinite()) {
        coerceIn(0f, 1f)
    } else {
        1f
    }
}

private fun Double.safeTime(): Double {
    return if (isFinite()) {
        coerceAtLeast(0.0)
    } else {
        0.0
    }
}
