package com.funkforge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.funkforge.model.Project

@Composable
fun SettingsDialog(project: Project, isNew: Boolean, onDismiss: () -> Unit, onSave: (Project) -> Unit) {
    var mod by remember { mutableStateOf(project.modName) }
    var song by remember { mutableStateOf(project.chart.song) }
    var p1 by remember { mutableStateOf(project.chart.player1) }
    var p2 by remember { mutableStateOf(project.chart.player2) }
    var gf by remember { mutableStateOf(project.chart.gfVersion) }
    var stage by remember { mutableStateOf(project.chart.stage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Novo mod" else "Configurações do mod") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Nome do mod", mod) { mod = it }
                Field("Nome da música", song) { song = it }
                Field("Jogador (player1)", p1) { p1 = it }
                Field("Oponente (player2)", p2) { p2 = it }
                Field("Namorada (gf)", gf) { gf = it }
                Field("Palco (stage)", stage) { stage = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    project.copy(
                        modName = mod.trim().ifEmpty { "MeuMod" },
                        chart = project.chart.copy(
                            song = song.trim().ifEmpty { "Untitled" },
                            player1 = p1.trim().ifEmpty { "bf" },
                            player2 = p2.trim().ifEmpty { "dad" },
                            gfVersion = gf.trim().ifEmpty { "gf" },
                            stage = stage.trim().ifEmpty { "stage" }
                        )
                    )
                )
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
