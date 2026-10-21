package com.funkforge.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class Project(
    val modName: String = "MeuMod",
    val chart: Chart = Chart(),
    val instPath: String? = null,
    val voicesPath: String? = null
)

object ProjectStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "project").apply { mkdirs() }

    fun save(ctx: Context, p: Project) {
        try {
            val d = dir(ctx)
            File(d, "chart.json").writeText(PsychFormat.export(p.chart))
            val meta = JSONObject().put("modName", p.modName)
            p.instPath?.let { meta.put("inst", it) }
            p.voicesPath?.let { meta.put("voices", it) }
            File(d, "meta.json").writeText(meta.toString())
        } catch (_: Exception) {
        }
    }

    fun load(ctx: Context): Project? = try {
        val d = dir(ctx)
        val c = File(d, "chart.json")
        val m = File(d, "meta.json")
        if (!c.exists() || !m.exists()) null else {
            val meta = JSONObject(m.readText())
            Project(
                modName = meta.optString("modName", "MeuMod"),
                chart = PsychFormat.import(c.readText()),
                instPath = if (meta.has("inst")) meta.getString("inst") else null,
                voicesPath = if (meta.has("voices")) meta.getString("voices") else null
            )
        }
    } catch (_: Exception) {
        null
    }
}

/** Gera um .zip pronto pra extrair em mods/ do Psych Engine. */
object ModExporter {
    fun slug(s: String) = s.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "song" }

    fun write(p: Project, out: OutputStream) {
        val root = p.modName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "MeuMod" }
        val song = slug(p.chart.song)
        ZipOutputStream(out).use { z ->
            fun text(path: String, s: String) {
                z.putNextEntry(ZipEntry("$root/$path")); z.write(s.toByteArray()); z.closeEntry()
            }
            fun file(src: String, path: String) {
                val f = File(src)
                if (!f.exists()) return
                z.putNextEntry(ZipEntry("$root/$path")); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
            text("pack.json", JSONObject()
                .put("name", p.modName).put("description", "Feito com FunkForge")
                .put("restart", false).put("runsGlobally", false)
                .put("color", JSONArray(listOf(170, 0, 255))).toString(2))
            text("data/$song/$song.json", PsychFormat.export(p.chart))
            text("weeks/${slug(root)}.json", JSONObject()
                .put("songs", JSONArray().put(JSONArray().put(p.chart.song).put(p.chart.player2).put(JSONArray(listOf(165, 0, 77)))))
                .put("weekCharacters", JSONArray(listOf(p.chart.player2, p.chart.player1, p.chart.gfVersion)))
                .put("weekBackground", p.chart.stage).put("weekBefore", "tutorial")
                .put("storyName", p.modName).put("weekName", p.modName)
                .put("startUnlocked", true).put("hideStoryMode", false).put("hideFreeplay", false)
                .put("difficulties", "").toString(2))
            p.instPath?.let { file(it, "songs/$song/Inst.${it.substringAfterLast('.')}") }
            p.voicesPath?.let { file(it, "songs/$song/Voices.${it.substringAfterLast('.')}") }
        }
    }
}
