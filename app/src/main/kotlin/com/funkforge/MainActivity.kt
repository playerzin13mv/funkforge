package com.funkforge

import android.os.Bundle
import android.provider.OpenableColumns
import android.net.Uri
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.funkforge.model.ModExporter
import com.funkforge.model.Project
import com.funkforge.model.ProjectStore
import com.funkforge.model.PsychFormat
import com.funkforge.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { Home, Editor, Play, Secret }

/** Usado pelo onPause para salvar o projeto. */
object Session { var save: (() -> Unit)? = null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Fk.pink, background = Fk.bg)) { FunkForgeApp() }
        }
    }

    override fun onPause() {
        super.onPause()
        Session.save?.invoke()
    }
}

private fun displayName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst() && i >= 0) c.getString(i) else null
    } ?: "audio.mp3"

@Composable
fun FunkForgeApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { ProjectStore.load(ctx) }
    var project by remember { mutableStateOf(saved ?: Project()) }
    var hasProject by remember { mutableStateOf(saved != null) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var showNew by remember { mutableStateOf(false) }
    var audioTarget by remember { mutableStateOf("inst") }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    SideEffect { Session.save = { if (hasProject) ProjectStore.save(ctx, project) } }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val chart = withContext(Dispatchers.Default) {
                    PsychFormat.import(ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
                }
                project = project.copy(chart = chart); hasProject = true; screen = Screen.Editor
            } catch (e: Exception) { toast("Não consegui importar: ${e.message}") }
        }
    }
    val jsonExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(PsychFormat.export(project.chart)) }
                }
                toast("Chart exportado!")
            } catch (e: Exception) { toast("Erro ao exportar: ${e.message}") }
        }
    }
    val modExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            try {
                val p = project
                withContext(Dispatchers.Default) { ctx.contentResolver.openOutputStream(uri)!!.use { ModExporter.write(p, it) } }
                val notOgg = listOfNotNull(p.instPath, p.voicesPath).any { !it.endsWith(".ogg", true) }
                toast(if (notOgg) "Mod exportado! ⚠️ O Psych só lê áudio .ogg — converta Inst/Voices pra .ogg." else "Mod exportado! Extraia na pasta mods/ do Psych.")
            } catch (e: Exception) { toast("Erro ao exportar o mod: ${e.message}") }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val ext = displayName(ctx, uri).substringAfterLast('.', "mp3").lowercase()
            val dir = File(ctx.filesDir, "audio").apply { mkdirs() }
            val f = File(dir, "${audioTarget}_${System.currentTimeMillis()}.$ext")
            ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            val old = if (audioTarget == "inst") project.instPath else project.voicesPath
            old?.let { File(it).delete() }
            project = if (audioTarget == "inst") project.copy(instPath = f.path) else project.copy(voicesPath = f.path)
            toast(if (audioTarget == "inst") "Inst carregada 🎵" else "Voices carregada 🎤")
        } catch (e: Exception) { toast("Erro ao carregar áudio: ${e.message}") }
    }

    BackHandler(enabled = screen == Screen.Editor) { screen = Screen.Home }

    if (showNew) SettingsDialog(Project(), true, { showNew = false }) { project = it; hasProject = true; showNew = false; screen = Screen.Editor }

    when (screen) {
        Screen.Home -> HomeScreen(
            hasProject = hasProject,
            onContinue = { screen = Screen.Editor },
            onNew = { showNew = true },
            onImport = { importer.launch(arrayOf("*/*")) },
            onSecret = { screen = Screen.Secret },
            onSoon = { toast(it) }
        )
        Screen.Editor -> EditorScreen(
            project = project,
            onProject = { project = it },
            onBack = { ProjectStore.save(ctx, project); screen = Screen.Home },
            onImport = { importer.launch(arrayOf("*/*")) },
            onExportJson = { jsonExporter.launch("${project.chart.song}.json") },
            onExportMod = { modExporter.launch("${project.modName}.zip") },
            onPickInst = { audioTarget = "inst"; audioPicker.launch(arrayOf("audio/*")) },
            onPickVoices = { audioTarget = "voices"; audioPicker.launch(arrayOf("audio/*")) },
            onPlaytest = { screen = Screen.Play }
        )
        Screen.Play -> PlayScreen(project) { screen = Screen.Editor }
        Screen.Secret -> SecretScreen()
    }
}
