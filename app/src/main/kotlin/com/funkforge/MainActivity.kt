package com.funkforge

import android.os.Bundle
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
import com.funkforge.model.Chart
import com.funkforge.model.PsychFormat
import com.funkforge.ui.EditorScreen
import com.funkforge.ui.Fk
import com.funkforge.ui.HomeScreen

enum class Screen { Home, Editor }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Fk.pink, background = Fk.bg)) {
                FunkForgeApp()
            }
        }
    }
}

@Composable
fun FunkForgeApp() {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Home) }
    var chart by remember { mutableStateOf(Chart()) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                chart = PsychFormat.import(text)
                screen = Screen.Editor
            } catch (e: Exception) {
                Toast.makeText(ctx, "Não consegui importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(PsychFormat.export(chart)) }
                Toast.makeText(ctx, "Chart exportado!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler(enabled = screen == Screen.Editor) { screen = Screen.Home }

    when (screen) {
        Screen.Home -> HomeScreen(
            onNew = { chart = Chart(song = "Minha Música"); screen = Screen.Editor },
            onImport = { importer.launch(arrayOf("*/*")) },
            onSoon = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
        )
        Screen.Editor -> EditorScreen(
            chart = chart,
            onChart = { chart = it },
            onBack = { screen = Screen.Home },
            onImport = { importer.launch(arrayOf("*/*")) },
            onExport = { exporter.launch("${chart.song}.json") }
        )
    }
}
