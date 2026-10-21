package com.funkforge

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.funkforge.play.PlayTest
import com.funkforge.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class Screen {
    Home,
    Editor,
    Play,
    ModTest,
    Secret
}

object Session {
    var save: (() -> Unit)? = null
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Fk.pink,
                    secondary = Fk.cyan,
                    background = Fk.bg,
                    surface = Fk.surface,
                    onBackground = Fk.text,
                    onSurface = Fk.text
                )
            ) {
                FunkForgeApp()
            }
        }
    }

    override fun onPause() {
        Session.save?.invoke()
        super.onPause()
    }

    override fun onDestroy() {
        Session.save = null
        super.onDestroy()
    }
}

private fun displayName(
    context: Context,
    uri: Uri
): String {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (cursor.moveToFirst() && index >= 0) {
                cursor.getString(index)
            } else {
                null
            }
        } ?: uri.lastPathSegment ?: "unknown"
    } catch (_: Exception) {
        uri.lastPathSegment ?: "unknown"
    }
}

private fun safeFileName(value: String): String {
    return value
        .trim()
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .take(100)
        .ifBlank { "untitled" }
}

private fun copyAudio(
    context: Context,
    uri: Uri,
    target: String
): File {
    val name = displayName(context, uri)

    val extension = name
        .substringAfterLast('.', "")
        .lowercase()

    val allowed = setOf(
        "ogg",
        "mp3",
        "wav",
        "m4a",
        "flac"
    )

    if (extension !in allowed) {
        throw IOException(
            "Unsupported audio format: $extension"
        )
    }

    val directory = File(
        context.filesDir,
        "audio"
    ).apply {
        if (!exists() && !mkdirs()) {
            throw IOException(
                "Unable to create audio directory"
            )
        }
    }

    val output = File.createTempFile(
        "${target}_",
        ".$extension",
        directory
    )

    try {
        context.contentResolver.openInputStream(uri)
            ?.use { input ->
                output.outputStream().buffered().use { stream ->
                    input.copyTo(stream)
                }
            } ?: throw IOException(
                "Unable to read selected audio"
            )

        if (output.length() == 0L) {
            throw IOException("Audio file is empty")
        }

        return output
    } catch (e: Exception) {
        output.delete()
        throw e
    }
}

@Composable
fun FunkForgeApp() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    val savedProject = remember {
        runCatching {
            ProjectStore.load(context)
        }.getOrNull()
    }

    var project by remember {
        mutableStateOf(savedProject ?: Project())
    }

    var hasProject by remember {
        mutableStateOf(savedProject != null)
    }

    var screen by remember {
        mutableStateOf(Screen.Home)
    }

    var showNew by remember {
        mutableStateOf(false)
    }

    var audioTarget by remember {
        mutableStateOf("inst")
    }

    var selectedModFolder by remember {
        mutableStateOf<Uri?>(null)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var saveJob by remember {
        mutableStateOf<Job?>(null)
    }

    fun toast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun saveProject() {
        if (!hasProject) return

        val snapshot = project

        saveJob?.cancel()

        saveJob = scope.launch(Dispatchers.IO) {
            runCatching {
                ProjectStore.save(context, snapshot)
            }
        }
    }

    fun openScreen(destination: Screen) {
        screen = destination
    }

    fun navigateBack() {
        when (screen) {
            Screen.Home -> Unit
            Screen.Editor -> {
                saveProject()
                screen = Screen.Home
            }
            Screen.Play -> screen = Screen.Editor
            Screen.ModTest -> screen = Screen.Home
            Screen.Secret -> screen = Screen.Home
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
        }
    }

    SideEffect {
        Session.save = {
            if (hasProject) {
                ProjectStore.save(context, project)
            }
        }
    }

    LaunchedEffect(project, hasProject) {
        if (hasProject) {
            delay(1200L)

            val snapshot = project

            withContext(Dispatchers.IO) {
                runCatching {
                    ProjectStore.save(context, snapshot)
                }
            }
        }
    }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !busy) {
            scope.launch {
                busy = true

                try {
                    val chart = withContext(Dispatchers.IO) {
                        val json = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: throw IOException(
                                "Unable to read JSON file"
                            )

                        PsychFormat.import(json)
                    }

                    project = project.copy(chart = chart)
                    hasProject = true
                    screen = Screen.Editor

                    toast("Chart imported successfully")
                } catch (e: Exception) {
                    toast(
                        "Import failed: ${e.message}"
                    )
                } finally {
                    busy = false
                }
            }
        }
    }

    val jsonExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/json"
        )
    ) { uri ->
        if (uri != null && !busy) {
            val snapshot = project

            scope.launch {
                busy = true

                try {
                    withContext(Dispatchers.IO) {
                        val json = PsychFormat.export(
                            snapshot.chart
                        )

                        context.contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter()
                            ?.use { it.write(json) }
                            ?: throw IOException(
                                "Unable to write JSON"
                            )
                    }

                    toast("Chart exported successfully")
                } catch (e: Exception) {
                    toast(
                        "Export failed: ${e.message}"
                    )
                } finally {
                    busy = false
                }
            }
        }
    }

    val modExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/zip"
        )
    ) { uri ->
        if (uri != null && !busy) {
            val snapshot = project

            scope.launch {
                busy = true

                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                            .openOutputStream(uri, "w")
                            ?.use { output ->
                                ModExporter.write(
                                    snapshot,
                                    output
                                )
                            } ?: throw IOException(
                                "Unable to create ZIP"
                            )
                    }

                    val unsupportedAudio = listOfNotNull(
                        snapshot.instPath,
                        snapshot.voicesPath
                    ).any {
                        !it.endsWith(
                            ".ogg",
                            ignoreCase = true
                        )
                    }

                    toast(
                        if (unsupportedAudio) {
                            "Mod exported. Convert audio to OGG for Psych Engine."
                        } else {
                            "Mod exported successfully"
                        }
                    )
                } catch (e: Exception) {
                    toast(
                        "Mod export failed: ${e.message}"
                    )
                } finally {
                    busy = false
                }
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !busy) {
            val target = audioTarget

            scope.launch {
                busy = true

                try {
                    val file = withContext(Dispatchers.IO) {
                        copyAudio(
                            context,
                            uri,
                            target
                        )
                    }

                    val oldPath = if (target == "inst") {
                        project.instPath
                    } else {
                        project.voicesPath
                    }

                    project = if (target == "inst") {
                        project.copy(
                            instPath = file.absolutePath
                        )
                    } else {
                        project.copy(
                            voicesPath = file.absolutePath
                        )
                    }

                    if (
                        oldPath != null &&
                        oldPath != file.absolutePath
                    ) {
                        val oldFile = File(oldPath)
                        val audioDirectory = File(
                            context.filesDir,
                            "audio"
                        ).canonicalFile

                        val parent = runCatching {
                            oldFile.canonicalFile.parentFile
                        }.getOrNull()

                        if (parent == audioDirectory) {
                            withContext(Dispatchers.IO) {
                                oldFile.delete()
                            }
                        }
                    }

                    toast(
                        if (target == "inst") {
                            "Instrumental loaded"
                        } else {
                            "Vocals loaded"
                        }
                    )
                } catch (e: Exception) {
                    toast(
                        "Audio import failed: ${e.message}"
                    )
                } finally {
                    busy = false
                }
            }
        }
    }

    val modFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            selectedModFolder = uri
            screen = Screen.ModTest
        }
    }

    BackHandler(
        enabled = screen != Screen.Home || showNew
    ) {
        if (showNew) {
            showNew = false
        } else {
            navigateBack()
        }
    }

    if (showNew) {
        SettingsDialog(
            Project(),
            true,
            {
                showNew = false
            }
        ) { newProject ->
            project = newProject
            hasProject = true
            showNew = false
            screen = Screen.Editor
        }
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            hasProject = hasProject,
            onContinue = {
                openScreen(Screen.Editor)
            },
            onNew = {
                showNew = true
            },
            onImport = {
                importer.launch(
                    arrayOf("application/json", "text/plain")
                )
            },
            onSecret = {
                openScreen(Screen.Secret)
            },
            onSoon = { message ->
                if (message == "Open Mod Folder") {
                    modFolderPicker.launch(null)
                } else {
                    toast(message)
                }
            }
        )

        Screen.Editor -> EditorScreen(
            project = project,
            onProject = {
                project = it
            },
            onBack = {
                navigateBack()
            },
            onImport = {
                importer.launch(
                    arrayOf("application/json", "text/plain")
                )
            },
            onExportJson = {
                jsonExporter.launch(
                    "${safeFileName(project.chart.song)}.json"
                )
            },
            onExportMod = {
                modExporter.launch(
                    "${safeFileName(project.modName)}.zip"
                )
            },
            onPickInst = {
                audioTarget = "inst"
                audioPicker.launch(
                    arrayOf("audio/*")
                )
            },
            onPickVoices = {
                audioTarget = "voices"
                audioPicker.launch(
                    arrayOf("audio/*")
                )
            },
            onPlaytest = {
                openScreen(Screen.Play)
            }
        )

        Screen.Play -> PlayScreen(
            project = project,
            onBack = {
                navigateBack()
            }
        )

        Screen.ModTest -> {
            val folder = selectedModFolder

            if (folder != null) {
                PlayTest(
                    modFolderUri = folder,
                    onBack = {
                        navigateBack()
                    }
                )
            }
        }

        Screen.Secret -> SecretScreen()
    }
}
