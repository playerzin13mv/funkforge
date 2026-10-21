package com.funkforge.model

/** Primeiro índice com timeMs >= ms (lista ordenada). Busca binária: rápido mesmo com 50 mil notas. */
fun List<Note>.lowerBound(ms: Double): Int {
    var lo = 0
    var hi = size
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (this[mid].timeMs < ms) lo = mid + 1 else hi = mid
    }
    return lo
}
