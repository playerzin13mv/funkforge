package com.funkforge.model

/** Interface que cada formato (Psych, Codename, V-Slice, Funky Maker) vai implementar. */
interface ChartFormat {
    val name: String
    fun import(text: String): Chart
    fun export(chart: Chart): String
}
