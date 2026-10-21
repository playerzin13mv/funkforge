package com.funkforge.model

import kotlinx.serialization.json.*

/**
 * Chart JSON do Psych Engine (formato clássico: song.notes[].sectionNotes).
 * Interno: lanes 0..3 = oponente, 4..7 = jogador.
 */
object PsychFormat : ChartFormat {
    override val name = "Psych Engine"

    private val json = Json { prettyPrint = true }

    override fun import(text: String): Chart {
        val root = Json.parseToJsonElement(text).jsonObject
        val song = (root["song"] as? JsonObject) ?: root

        val notes = mutableListOf<Note>()
        val bpmChanges = mutableListOf<BpmChange>()
        var bpm = song["bpm"].dbl(120.0)
        bpmChanges += BpmChange(0.0, bpm)

        var sectionStart = 0.0
        for (secEl in song["notes"]?.jsonArray ?: JsonArray(emptyList())) {
            val sec = secEl.jsonObject
            if (sec["changeBPM"]?.jsonPrimitive?.booleanOrNull == true) {
                val nb = sec["bpm"].dbl(bpm)
                if (nb != bpm) { bpm = nb; if (sectionStart > 0) bpmChanges += BpmChange(sectionStart, bpm) }
            }
            val mustHit = sec["mustHitSection"]?.jsonPrimitive?.booleanOrNull ?: true
            for (nEl in sec["sectionNotes"]?.jsonArray ?: JsonArray(emptyList())) {
                val n = nEl.jsonArray
                if (n.size < 3) continue
                val raw = n[1].dbl(0.0).toInt()
                val isPlayer = (raw < 4) == mustHit
                val kind = if (n.size > 3) (n[3] as? JsonPrimitive)?.contentOrNull.orEmpty() else ""
                notes += Note(n[0].dbl(0.0), (if (isPlayer) 4 else 0) + (raw % 4), n[2].dbl(0.0), kind)
            }
            val steps = sec["lengthInSteps"].dbl(16.0)
            sectionStart += steps / 4.0 * 60000.0 / bpm
        }

        val events = mutableListOf<ChartEvent>()
        for (evEl in song["events"]?.jsonArray ?: JsonArray(emptyList())) {
            val ev = evEl.jsonArray
            val t = ev[0].dbl(0.0)
            for (e in ev[1].jsonArray) {
                val a = e.jsonArray
                events += ChartEvent(t, a[0].str(), a.getOrNull(1).str(), a.getOrNull(2).str())
            }
        }

        return Chart(
            song = song["song"].str().ifEmpty { "Untitled" },
            bpmChanges = bpmChanges,
            speed = song["speed"].dbl(2.0),
            notes = notes.sortedBy { it.timeMs },
            events = events.sortedBy { it.timeMs },
            player1 = song["player1"].str().ifEmpty { "bf" },
            player2 = song["player2"].str().ifEmpty { "dad" },
            gfVersion = song["gfVersion"].str().ifEmpty { song["player3"].str().ifEmpty { "gf" } },
            stage = song["stage"].str().ifEmpty { "stage" }
        )
    }

    override fun export(chart: Chart): String {
        val lastMs = (chart.notes.maxOfOrNull { it.timeMs + it.sustainMs } ?: 0.0)
        val sections = mutableListOf<JsonElement>()
        var start = 0.0
        var bpm = chart.bpmChanges.first().bpm
        var first = true
        do {
            val newBpm = chart.bpmChanges.lastOrNull { it.timeMs <= start + 1 }?.bpm ?: bpm
            val changed = !first && newBpm != bpm
            bpm = newBpm
            val end = start + 4 * 60000.0 / bpm
            // seção do jogador se tiver mais notas de jogador do que de oponente; senão oponente
            val inSec = chart.notes.filter { it.timeMs >= start && it.timeMs < end }
            val mustHit = inSec.count { it.lane >= 4 } >= inSec.count { it.lane < 4 }
            val sn = inSec.map { n ->
                val isPlayer = n.lane >= 4
                val raw = (n.lane % 4) + (if (isPlayer == mustHit) 0 else 4)
                buildJsonArray {
                    add(n.timeMs); add(raw); add(n.sustainMs)
                    if (n.kind.isNotEmpty()) add(n.kind)
                }
            }
            sections += buildJsonObject {
                put("sectionNotes", JsonArray(sn))
                put("sectionBeats", 4)
                put("lengthInSteps", 16)
                put("mustHitSection", mustHit)
                put("gfSection", false)
                put("altAnim", false)
                put("bpm", bpm)
                put("changeBPM", changed)
                put("typeOfSection", 0)
            }
            first = false
            start = end
        } while (start <= lastMs)

        val evs = chart.events.groupBy { it.timeMs }.toSortedMap().map { (t, list) ->
            buildJsonArray {
                add(t)
                add(buildJsonArray { list.forEach { e -> add(buildJsonArray { add(e.name); add(e.value1); add(e.value2) }) } })
            }
        }

        val song = buildJsonObject {
            put("song", chart.song)
            put("notes", JsonArray(sections))
            put("events", JsonArray(evs))
            put("bpm", chart.bpmChanges.first().bpm)
            put("needsVoices", true)
            put("speed", chart.speed)
            put("player1", chart.player1)
            put("player2", chart.player2)
            put("gfVersion", chart.gfVersion)
            put("stage", chart.stage)
            put("validScore", true)
        }
        return json.encodeToString(JsonObject.serializer(), buildJsonObject { put("song", song) })
    }

    private fun JsonElement?.dbl(def: Double) = (this as? JsonPrimitive)?.doubleOrNull ?: def
    private fun JsonElement?.str() = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
}
