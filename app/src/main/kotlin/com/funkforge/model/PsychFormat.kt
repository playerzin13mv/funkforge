package com.funkforge.model

import kotlinx.serialization.json.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object PsychFormat : ChartFormat {

    override val name = "Psych Engine"

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private const val DEFAULT_BPM = 120.0
    private const val DEFAULT_SPEED = 2.0
    private const val STEPS_PER_SECTION = 16.0
    private const val MAX_SECTIONS = 20000
    private const val EPSILON = 0.001

    override fun import(text: String): Chart {
        val root = json.parseToJsonElement(
            text.removePrefix("\uFEFF")
        ).asObject() ?: throw IllegalArgumentException(
            "Invalid chart JSON"
        )

        val song = root["song"].asObject() ?: root

        val initialBpm = song["bpm"]
            .number(DEFAULT_BPM)
            .safeBpm()

        val changes = mutableListOf(
            BpmChange(0.0, initialBpm)
        )

        val notes = mutableListOf<Note>()
        val events = mutableListOf<ChartEvent>()

        var currentBpm = initialBpm
        var sectionStart = 0.0

        val sections = song["notes"].asArray()

        sections.forEach { element ->
            val section = element.asObject()
                ?: return@forEach

            val sectionBpm = section["bpm"]
                .number(currentBpm)
                .safeBpm()

            val changeBpm = section["changeBPM"]
                .boolean(false)

            if (
                changeBpm &&
                abs(sectionBpm - currentBpm) > EPSILON
            ) {
                currentBpm = sectionBpm

                if (sectionStart <= EPSILON) {
                    changes[0] = BpmChange(
                        0.0,
                        currentBpm
                    )
                } else {
                    changes.add(
                        BpmChange(
                            sectionStart,
                            currentBpm
                        )
                    )
                }
            }

            val mustHit = section["mustHitSection"]
                .boolean(true)

            section["sectionNotes"]
                .asArray()
                .forEach { noteElement ->
                    val values = noteElement.asArray()

                    if (values.size < 3) {
                        return@forEach
                    }

                    val time = values[0]
                        .number(Double.NaN)

                    val rawLane = values[1]
                        .number(Double.NaN)

                    val sustain = values[2]
                        .number(0.0)

                    if (
                        !time.isFinite() ||
                        time < 0.0 ||
                        !rawLane.isFinite() ||
                        rawLane < 0.0 ||
                        rawLane > Int.MAX_VALUE
                    ) {
                        return@forEach
                    }

                    val raw = rawLane.toInt()

                    val isPlayer = (
                        raw % 8 < 4
                    ) == mustHit

                    val lane = (
                        if (isPlayer) 4 else 0
                    ) + raw % 4

                    val kind = values
                        .getOrNull(3)
                        .string()

                    notes.add(
                        Note(
                            timeMs = time,
                            lane = lane,
                            sustainMs = if (
                                sustain.isFinite()
                            ) {
                                sustain.coerceAtLeast(0.0)
                            } else {
                                0.0
                            },
                            kind = kind
                        )
                    )
                }

            val steps = section["lengthInSteps"]
                .number(STEPS_PER_SECTION)
                .let {
                    if (it.isFinite() && it > 0.0) {
                        it
                    } else {
                        STEPS_PER_SECTION
                    }
                }

            sectionStart += (
                steps / 4.0
            ) * 60000.0 / currentBpm
        }

        fun readEvents(element: JsonElement?) {
            element.asArray().forEach { group ->
                val values = group.asArray()

                if (values.size < 2) {
                    return@forEach
                }

                val time = values[0]
                    .number(Double.NaN)

                if (
                    !time.isFinite() ||
                    time < 0.0
                ) {
                    return@forEach
                }

                values[1].asArray().forEach { entry ->
                    val data = entry.asArray()

                    if (data.isEmpty()) {
                        return@forEach
                    }

                    val name = data[0].string()

                    if (name.isBlank()) {
                        return@forEach
                    }

                    events.add(
                        ChartEvent(
                            timeMs = time,
                            name = name,
                            value1 = data
                                .getOrNull(1)
                                .string(),
                            value2 = data
                                .getOrNull(2)
                                .string()
                        )
                    )
                }
            }
        }

        readEvents(song["events"])

        if (root !== song) {
            readEvents(root["events"])
        }

        return Chart(
            song = song["song"]
                .string()
                .ifBlank { "Untitled" },
            bpmChanges = changes,
            speed = song["speed"]
                .number(DEFAULT_SPEED)
                .safeSpeed(),
            notes = notes.sortedWith(
                compareBy<Note> { it.timeMs }
                    .thenBy { it.lane }
            ),
            events = events
                .distinct()
                .sortedBy { it.timeMs },
            player1 = song["player1"]
                .string()
                .ifBlank { "bf" },
            player2 = song["player2"]
                .string()
                .ifBlank { "dad" },
            gfVersion = song["gfVersion"]
                .string()
                .ifBlank {
                    song["player3"]
                        .string()
                        .ifBlank { "gf" }
                },
            stage = song["stage"]
                .string()
                .ifBlank { "stage" }
        )
    }

    override fun export(chart: Chart): String {
        val clean = chart.normalized()

        val bpmChanges = clean.bpmChanges
            .sortedBy { it.timeMs }

        val initialBpm = bpmChanges
            .firstOrNull()
            ?.bpm
            ?.safeBpm()
            ?: DEFAULT_BPM

        val sortedNotes = clean.notes
            .sortedBy { it.timeMs }

        val lastNoteTime = sortedNotes
            .maxOfOrNull {
                it.timeMs + it.sustainMs
            } ?: 0.0

        val lastEventTime = clean.events
            .maxOfOrNull {
                it.timeMs
            } ?: 0.0

        val lastBpmTime = bpmChanges
            .maxOfOrNull {
                it.timeMs
            } ?: 0.0

        val lastTime = max(
            lastNoteTime,
            max(lastEventTime, lastBpmTime)
        )

        val sections = mutableListOf<JsonElement>()

        var currentBpm = initialBpm
        var sectionStart = 0.0
        var noteIndex = 0
        var sectionIndex = 0

        while (
            sectionIndex < MAX_SECTIONS
        ) {
            val nextBpm = bpmChanges
                .lastOrNull {
                    it.timeMs <= sectionStart + EPSILON
                }
                ?.bpm
                ?.safeBpm()
                ?: currentBpm

            val bpmChanged = (
                sectionIndex > 0 &&
                abs(nextBpm - currentBpm) > EPSILON
            )

            currentBpm = nextBpm

            val sectionDuration = (
                STEPS_PER_SECTION / 4.0
            ) * 60000.0 / currentBpm

            val sectionEnd = (
                sectionStart + sectionDuration
            )

            val sectionNotes = mutableListOf<Note>()

            while (
                noteIndex < sortedNotes.size &&
                sortedNotes[noteIndex].timeMs <
                    sectionEnd - EPSILON
            ) {
                sectionNotes.add(
                    sortedNotes[noteIndex]
                )

                noteIndex++
            }

            val playerCount = sectionNotes.count {
                it.lane >= 4
            }

            val opponentCount = sectionNotes.size -
                playerCount

            val mustHit = playerCount >= opponentCount

            val encodedNotes = sectionNotes.map { note ->
                val isPlayer = note.lane >= 4

                val rawLane = (
                    note.lane % 4
                ) + if (
                    isPlayer == mustHit
                ) {
                    0
                } else {
                    4
                }

                buildJsonArray {
                    add(note.timeMs)
                    add(rawLane)
                    add(note.sustainMs)

                    if (note.kind.isNotBlank()) {
                        add(note.kind)
                    }
                }
            }

            sections.add(
                buildJsonObject {
                    put(
                        "sectionNotes",
                        JsonArray(encodedNotes)
                    )
                    put("sectionBeats", 4)
                    put("lengthInSteps", 16)
                    put("mustHitSection", mustHit)
                    put("gfSection", false)
                    put("altAnim", false)
                    put("bpm", currentBpm)
                    put(
                        "changeBPM",
                        bpmChanged
                    )
                    put("typeOfSection", 0)
                }
            )

            sectionIndex++
            sectionStart = sectionEnd

            if (
                sectionStart > lastTime &&
                noteIndex >= sortedNotes.size
            ) {
                break
            }
        }

        if (noteIndex < sortedNotes.size) {
            throw IllegalStateException(
                "Chart exceeds maximum section count"
            )
        }

        val encodedEvents = clean.events
            .groupBy { it.timeMs }
            .toSortedMap()
            .map { (time, group) ->
                buildJsonArray {
                    add(time)

                    add(
                        buildJsonArray {
                            group.forEach { event ->
                                add(
                                    buildJsonArray {
                                        add(event.name)
                                        add(event.value1)
                                        add(event.value2)
                                    }
                                )
                            }
                        }
                    )
                }
            }

        val song = buildJsonObject {
            put("song", clean.song)
            put("notes", JsonArray(sections))
            put(
                "events",
                JsonArray(encodedEvents)
            )
            put("bpm", initialBpm)
            put("needsVoices", true)
            put("speed", clean.speed)
            put("player1", clean.player1)
            put("player2", clean.player2)
            put("gfVersion", clean.gfVersion)
            put("stage", clean.stage)
            put("validScore", true)
        }

        val root = buildJsonObject {
            put("song", song)
        }

        return json.encodeToString(
            JsonObject.serializer(),
            root
        )
    }

    private fun JsonElement?.asObject(): JsonObject? {
        return this as? JsonObject
    }

    private fun JsonElement?.asArray(): JsonArray {
        return this as? JsonArray
            ?: JsonArray(emptyList())
    }

    private fun JsonElement?.number(
        default: Double
    ): Double {
        return (this as? JsonPrimitive)
            ?.doubleOrNull
            ?: default
    }

    private fun JsonElement?.boolean(
        default: Boolean
    ): Boolean {
        return (this as? JsonPrimitive)
            ?.booleanOrNull
            ?: default
    }

    private fun JsonElement?.string(): String {
        return (this as? JsonPrimitive)
            ?.contentOrNull
            .orEmpty()
    }

    private fun Double.safeBpm(): Double {
        return if (isFinite()) {
            coerceIn(20.0, 999.0)
        } else {
            DEFAULT_BPM
        }
    }

    private fun Double.safeSpeed(): Double {
        return if (isFinite()) {
            coerceIn(0.1, 10.0)
        } else {
            DEFAULT_SPEED
        }
    }
}
