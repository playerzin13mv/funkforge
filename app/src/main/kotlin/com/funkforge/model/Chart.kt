package com.funkforge.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToLong

data class BpmChange(
    val timeMs: Double,
    val bpm: Double
)

data class Note(
    val timeMs: Double,
    val lane: Int,
    val sustainMs: Double = 0.0,
    val kind: String = ""
) {
    val endTimeMs: Double
        get() = timeMs + sustainMs.coerceAtLeast(0.0)

    val isSustain: Boolean
        get() = sustainMs > 0.0

    val isPlayer: Boolean
        get() = lane in 4..7

    val isOpponent: Boolean
        get() = lane in 0..3

    val direction: Int
        get() = lane.coerceIn(0, 7) % 4

    fun normalized(): Note {
        return copy(
            timeMs = timeMs.safePositive(),
            lane = lane.coerceIn(0, 7),
            sustainMs = sustainMs.safePositive(),
            kind = kind.trim()
        )
    }
}

data class ChartEvent(
    val timeMs: Double,
    val name: String,
    val value1: String = "",
    val value2: String = ""
) {
    fun normalized(): ChartEvent {
        return copy(
            timeMs = timeMs.safePositive(),
            name = name.trim()
        )
    }
}

data class Chart(
    val song: String = "Untitled",
    val bpmChanges: List<BpmChange> = listOf(
        BpmChange(0.0, 120.0)
    ),
    val speed: Double = 2.0,
    val notes: List<Note> = emptyList(),
    val events: List<ChartEvent> = emptyList(),
    val player1: String = "bf",
    val player2: String = "dad",
    val gfVersion: String = "gf",
    val stage: String = "stage"
) {
    companion object {
        const val DEFAULT_BPM = 120.0
        const val DEFAULT_SPEED = 2.0
        const val MIN_BPM = 20.0
        const val MAX_BPM = 999.0
        const val MIN_SPEED = 0.1
        const val MAX_SPEED = 10.0
        const val LANE_COUNT = 8
        const val STEPS_PER_BEAT = 4
        const val BEATS_PER_SECTION = 4
        const val NOTE_TOLERANCE_MS = 0.5
    }

    private val timeline: List<BpmChange>
        get() {
            val changes = bpmChanges
                .filter {
                    it.timeMs.isFinite() &&
                    it.bpm.isFinite() &&
                    it.bpm > 0.0
                }
                .map {
                    BpmChange(
                        it.timeMs.coerceAtLeast(0.0),
                        it.bpm.coerceIn(MIN_BPM, MAX_BPM)
                    )
                }
                .sortedBy { it.timeMs }
                .toMutableList()

            if (changes.isEmpty()) {
                return listOf(
                    BpmChange(0.0, DEFAULT_BPM)
                )
            }

            val result = mutableListOf<BpmChange>()

            if (changes.first().timeMs > 0.0) {
                result.add(
                    BpmChange(0.0, changes.first().bpm)
                )
            }

            changes.forEach { change ->
                if (
                    result.isNotEmpty() &&
                    result.last().timeMs == change.timeMs
                ) {
                    result[result.lastIndex] = change
                } else {
                    result.add(change)
                }
            }

            return result
        }

    val bpm: Double
        get() = timeline.first().bpm

    val beatMs: Double
        get() = 60000.0 / bpm

    val stepMs: Double
        get() = beatMs / STEPS_PER_BEAT

    val sectionMs: Double
        get() = beatMs * BEATS_PER_SECTION

    val noteCount: Int
        get() = notes.size

    val eventCount: Int
        get() = events.size

    val playerNotes: List<Note>
        get() = notes.filter { it.isPlayer }

    val opponentNotes: List<Note>
        get() = notes.filter { it.isOpponent }

    val sustainNotes: List<Note>
        get() = notes.filter { it.isSustain }

    val durationMs: Double
        get() {
            val lastNote = notes
                .filter { it.timeMs.isFinite() }
                .maxOfOrNull {
                    it.timeMs.safePositive() +
                        it.sustainMs.safePositive()
                } ?: 0.0

            val lastEvent = events
                .filter { it.timeMs.isFinite() }
                .maxOfOrNull {
                    it.timeMs.safePositive()
                } ?: 0.0

            return maxOf(lastNote, lastEvent, 0.0)
        }

    fun bpmAt(timeMs: Double): Double {
        val time = timeMs.safePositive()

        return timeline.lastOrNull {
            it.timeMs <= time
        }?.bpm ?: bpm
    }

    fun beatDurationAt(timeMs: Double): Double {
        return 60000.0 / bpmAt(timeMs)
    }

    fun stepDurationAt(timeMs: Double): Double {
        return beatDurationAt(timeMs) / STEPS_PER_BEAT
    }

    fun timeToBeat(timeMs: Double): Double {
        val time = timeMs.safePositive()
        val changes = timeline
        var beats = 0.0

        for (index in changes.indices) {
            val current = changes[index]

            if (time <= current.timeMs) {
                break
            }

            val nextTime = changes
                .getOrNull(index + 1)
                ?.timeMs ?: time

            val end = minOf(time, nextTime)

            if (end > current.timeMs) {
                beats += (
                    end - current.timeMs
                ) * current.bpm / 60000.0
            }

            if (time <= nextTime) {
                break
            }
        }

        return beats
    }

    fun beatToTime(beat: Double): Double {
        var remaining = beat.safePositive()
        val changes = timeline

        for (index in changes.indices) {
            val current = changes[index]

            val next = changes.getOrNull(index + 1)

            val beatDuration = 60000.0 / current.bpm

            if (next == null) {
                return current.timeMs +
                    remaining * beatDuration
            }

            val segmentBeats = (
                next.timeMs - current.timeMs
            ) / beatDuration

            if (remaining < segmentBeats) {
                return current.timeMs +
                    remaining * beatDuration
            }

            remaining -= segmentBeats
        }

        return 0.0
    }

    fun timeToStep(timeMs: Double): Double {
        return timeToBeat(timeMs) * STEPS_PER_BEAT
    }

    fun stepToTime(step: Double): Double {
        return beatToTime(
            step.safePositive() / STEPS_PER_BEAT
        )
    }

    fun timeToSection(timeMs: Double): Int {
        val sections = floor(
            timeToBeat(timeMs) / BEATS_PER_SECTION
        )

        return sections
            .coerceIn(0.0, Int.MAX_VALUE.toDouble())
            .toInt()
    }

    fun snapTime(
        timeMs: Double,
        division: Int = 16
    ): Double {
        val safeDivision = division.coerceIn(1, 192)

        val beatInterval = 4.0 / safeDivision

        val beatPosition = timeToBeat(timeMs)

        val snappedBeat = round(
            beatPosition / beatInterval
        ) * beatInterval

        return beatToTime(snappedBeat)
    }

    fun snapNote(
        note: Note,
        division: Int = 16
    ): Note {
        return note.copy(
            timeMs = snapTime(
                note.timeMs,
                division
            )
        )
    }

    fun notesInRange(
        startMs: Double,
        endMs: Double,
        includeSustains: Boolean = true
    ): List<Note> {
        val start = startMs.safePositive()
        val end = endMs.safePositive()

        if (end < start) return emptyList()

        return notes.filter { note ->
            if (includeSustains) {
                note.timeMs <= end &&
                    note.endTimeMs >= start
            } else {
                note.timeMs in start..end
            }
        }.sortedWith(
            compareBy<Note> { it.timeMs }
                .thenBy { it.lane }
        )
    }

    fun notesAtLane(lane: Int): List<Note> {
        if (lane !in 0 until LANE_COUNT) {
            return emptyList()
        }

        return notes
            .filter { it.lane == lane }
            .sortedBy { it.timeMs }
    }

    fun findNote(
        timeMs: Double,
        lane: Int,
        toleranceMs: Double = NOTE_TOLERANCE_MS
    ): Note? {
        if (lane !in 0 until LANE_COUNT) {
            return null
        }

        val tolerance = toleranceMs.safePositive()

        return notes.firstOrNull {
            it.lane == lane &&
                abs(it.timeMs - timeMs) <= tolerance
        }
    }

    fun hasNote(
        timeMs: Double,
        lane: Int,
        toleranceMs: Double = NOTE_TOLERANCE_MS
    ): Boolean {
        return findNote(
            timeMs,
            lane,
            toleranceMs
        ) != null
    }

    fun withNote(n: Note): Chart {
        val note = n.normalized()

        val updated = notes
            .filterNot {
                it.lane == note.lane &&
                    abs(
                        it.timeMs - note.timeMs
                    ) <= NOTE_TOLERANCE_MS
            } + note

        return copy(
            notes = updated.sortedWith(
                compareBy<Note> { it.timeMs }
                    .thenBy { it.lane }
            )
        )
    }

    fun withoutNote(n: Note): Chart {
        return copy(
            notes = notes - n
        )
    }

    fun removeNoteAt(
        timeMs: Double,
        lane: Int,
        toleranceMs: Double = NOTE_TOLERANCE_MS
    ): Chart {
        val note = findNote(
            timeMs,
            lane,
            toleranceMs
        ) ?: return this

        return withoutNote(note)
    }

    fun replaceNote(
        oldNote: Note,
        newNote: Note
    ): Chart {
        return withoutNote(oldNote)
            .withNote(newNote)
    }

    fun moveNote(
        note: Note,
        newTimeMs: Double,
        newLane: Int = note.lane
    ): Chart {
        return replaceNote(
            note,
            note.copy(
                timeMs = newTimeMs,
                lane = newLane
            )
        )
    }

    fun changeSustain(
        note: Note,
        sustainMs: Double
    ): Chart {
        return replaceNote(
            note,
            note.copy(
                sustainMs = sustainMs
            )
        )
    }

    fun clearNotes(): Chart {
        return copy(
            notes = emptyList()
        )
    }

    fun clearLane(lane: Int): Chart {
        return copy(
            notes = notes.filterNot {
                it.lane == lane
            }
        )
    }

    fun withEvent(event: ChartEvent): Chart {
        val normalized = event.normalized()

        return copy(
            events = (events + normalized)
                .sortedBy { it.timeMs }
        )
    }

    fun withoutEvent(event: ChartEvent): Chart {
        return copy(
            events = events - event
        )
    }

    fun eventsInRange(
        startMs: Double,
        endMs: Double
    ): List<ChartEvent> {
        val start = startMs.safePositive()
        val end = endMs.safePositive()

        if (end < start) return emptyList()

        return events.filter {
            it.timeMs in start..end
        }.sortedBy { it.timeMs }
    }

    fun withBpmChange(
        timeMs: Double,
        newBpm: Double
    ): Chart {
        val time = timeMs.safePositive()

        val change = BpmChange(
            timeMs = time,
            bpm = newBpm.safeBpm()
        )

        val updated = timeline
            .filterNot {
                abs(it.timeMs - time) <= 0.001
            } + change

        return copy(
            bpmChanges = updated.sortedBy {
                it.timeMs
            }
        )
    }

    fun withoutBpmChange(
        timeMs: Double
    ): Chart {
        if (timeMs <= 0.0) return this

        return copy(
            bpmChanges = timeline.filterNot {
                abs(it.timeMs - timeMs) <= 0.001 &&
                    it.timeMs > 0.0
            }
        )
    }

    fun normalized(): Chart {
        val cleanNotes = notes
            .map { it.normalized() }
            .sortedWith(
                compareBy<Note> { it.timeMs }
                    .thenBy { it.lane }
            )
            .distinctBy {
                it.lane to it.timeMs
            }

        val cleanEvents = events
            .map { it.normalized() }
            .sortedBy { it.timeMs }

        return copy(
            song = song.trim().ifBlank {
                "Untitled"
            },
            bpmChanges = timeline,
            speed = speed.safeSpeed(),
            notes = cleanNotes,
            events = cleanEvents,
            player1 = player1.trim().ifBlank {
                "bf"
            },
            player2 = player2.trim().ifBlank {
                "dad"
            },
            gfVersion = gfVersion.trim().ifBlank {
                "gf"
            },
            stage = stage.trim().ifBlank {
                "stage"
            }
        )
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (song.isBlank()) {
            errors += "Song name cannot be empty"
        }

        if (
            !speed.isFinite() ||
            speed !in MIN_SPEED..MAX_SPEED
        ) {
            errors += "Invalid scroll speed"
        }

        if (bpmChanges.isEmpty()) {
            errors += "No BPM changes found"
        }

        bpmChanges.forEachIndexed { index, change ->
            if (
                !change.timeMs.isFinite() ||
                change.timeMs < 0.0
            ) {
                errors += "Invalid BPM time at index $index"
            }

            if (
                !change.bpm.isFinite() ||
                change.bpm !in MIN_BPM..MAX_BPM
            ) {
                errors += "Invalid BPM at index $index"
            }
        }

        notes.forEachIndexed { index, note ->
            if (
                !note.timeMs.isFinite() ||
                note.timeMs < 0.0
            ) {
                errors += "Invalid note time at index $index"
            }

            if (note.lane !in 0 until LANE_COUNT) {
                errors += "Invalid note lane at index $index"
            }

            if (
                !note.sustainMs.isFinite() ||
                note.sustainMs < 0.0
            ) {
                errors += "Invalid sustain at index $index"
            }
        }

        events.forEachIndexed { index, event ->
            if (
                !event.timeMs.isFinite() ||
                event.timeMs < 0.0
            ) {
                errors += "Invalid event time at index $index"
            }

            if (event.name.isBlank()) {
                errors += "Empty event name at index $index"
            }
        }

        return errors
    }
}

private fun Double.safePositive(): Double {
    return if (isFinite()) {
        coerceAtLeast(0.0)
    } else {
        0.0
    }
}

private fun Double.safeBpm(): Double {
    return if (isFinite()) {
        coerceIn(
            Chart.MIN_BPM,
            Chart.MAX_BPM
        )
    } else {
        Chart.DEFAULT_BPM
    }
}

private fun Double.safeSpeed(): Double {
    return if (isFinite()) {
        coerceIn(
            Chart.MIN_SPEED,
            Chart.MAX_SPEED
        )
    } else {
        Chart.DEFAULT_SPEED
    }
}
