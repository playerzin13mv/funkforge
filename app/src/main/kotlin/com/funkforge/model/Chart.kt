package com.funkforge.model

/**
 * Modelo interno do FunkForge. Tudo em milissegundos, independente do formato.
 * Lanes: 0..3 = oponente, 4..7 = jogador.
 * Cada formato (Psych, Codename, V-Slice, Funky Maker) converte de/para isso.
 */
data class BpmChange(val timeMs: Double, val bpm: Double)

data class Note(
    val timeMs: Double,
    val lane: Int,
    val sustainMs: Double = 0.0,
    val kind: String = ""
)

data class ChartEvent(
    val timeMs: Double,
    val name: String,
    val value1: String = "",
    val value2: String = ""
)

data class Chart(
    val song: String = "Untitled",
    val bpmChanges: List<BpmChange> = listOf(BpmChange(0.0, 120.0)),
    val speed: Double = 2.0,
    val notes: List<Note> = emptyList(),
    val events: List<ChartEvent> = emptyList(),
    val player1: String = "bf",
    val player2: String = "dad",
    val gfVersion: String = "gf",
    val stage: String = "stage"
) {
    /** Duração de um beat em ms no começo da música. */
    val beatMs: Double get() = 60000.0 / bpmChanges.first().bpm

    fun withNote(n: Note) = copy(notes = (notes + n).sortedBy { it.timeMs })
    fun withoutNote(n: Note) = copy(notes = notes - n)
}
