package com.funkforge

import android.content.Context
import android.content.Intent
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

private object ProjectSaveLock {
    val lock = Any()
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
    return runCatching {
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
    }.getOrDefault(
        uri.lastPathSegment ?: "unknown"
    )
}

private fun safeFileName(value: String): String {
    return value
        .trim()
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .take(100)
        .ifBlank { "untitled" }
}

private fun saveProjectSnapshot(
    context: Context,
    project: Project
) {
    synchronized(ProjectSaveLock.lock) {
        ProjectStore.save(context, project)
    }
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

    val supported = setOf(
        "ogg",
        "mp3",
        "wav",
        "m4a",
        "flac"
    )

    if (extension !in supported) {
        throw IOException(
            "Unsupported audio format: $extension"
        )
    }

    val directory = File(
        context.filesDir,
        "audio"
    )

    if (!directory.exists() && !directory.mkdirs()) {
        throw IOException(
            "Unable to create audio directory"
        )
    }

    val output = File.createTempFile(
        "${target}_",
        ".$extension",
        directory
    )

    try {
        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                output.outputStream()
                    .buffered()
                    .use { stream ->
                        input.copyTo(stream)
                    }
            } ?: throw IOException(
                "Unable to read audio"
            )

        if (output.length() <= 0L) {
            throw IOException(
                "Selected audio is empty"
            )
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

    val savedProject = remember(context) {
        runCatching {
            synchronized(ProjectSaveLock.lock) {
                ProjectStore.load(context)
            }
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

    var status by remember {
        mutableStateOf("")
    }

    val latestProject by rememberUpdatedState(project)
    val latestHasProject by rememberUpdatedState(hasProject)

    fun toast(message: String) {
        status = message

        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun saveNow() {
        if (!hasProject) return

        val snapshot = project

        runCatching {
            saveProjectSnapshot(
                context,
                snapshot
            )
        }
    }

    fun openScreen(destination: Screen) {
        if (busy) return

        screen = destination
    }

    fun navigateBack() {
        if (busy) return

        when (screen) {
            Screen.Home -> Unit

            Screen.Editor -> {
                saveNow()
                screen = Screen.Home
            }

            Screen.Play -> {
                screen = Screen.Editor
            }

            Screen.ModTest -> {
                selectedModFolder = null
                screen = Screen.Home
            }

            Screen.Secret -> {
                screen = Screen.Home
            }
        }
    }

    DisposableEffect(context) {
        Session.save = {
            if (latestHasProject) {
                runCatching {
                    saveProjectSnapshot(
                        context,
                        latestProject
                    )
                }
            }
        }

        onDispose {
            Session.save = null
        }
    }

    LaunchedEffect(project, hasProject) {
        if (!hasProject) return@LaunchedEffect

        delay(1200L)

        val snapshot = project

        withContext(Dispatchers.IO) {
            runCatching {
                saveProjectSnapshot(
                    context,
                    snapshot
                )
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
                    val importedChart = withContext(
                        Dispatchers.IO
                    ) {
                        val json = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            } ?: throw IOException(
                                "Unable to read chart"
                            )

                        PsychFormat.import(json)
                    }

                    project = project.copy(
                        chart = importedChart
                    )

                    hasProject = true
                    showNew = false
                    screen = Screen.Editor

                    toast(
                        "Chart imported successfully"
                    )
                } catch (e: Exception) {
                    toast(
                        "Chart import failed: ${e.message}"
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
                            ?.use {
                                it.write(json)
                            } ?: throw IOException(
                                "Unable to write chart"
                            )
                    }

                    toast(
                        "Chart exported successfully"
                    )
                } catch (e: Exception) {
                    toast(
                        "Chart export failed: ${e.message}"
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
                                "Unable to create mod ZIP"
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

                    if (unsupportedAudio) {
                        toast(
                            "Mod exported. Convert audio to OGG for Psych Engine."
                        )
                    } else {
                        toast(
                            "Mod exported successfully"
                        )
                    }
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
                    val file = withContext(
                        Dispatchers.IO
                    ) {
                        copyAudio(
                            context,
                            uri,
                            target
                        )
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

                    toast(
                        if (target == "inst") {
                            "Instrumental loaded successfully"
                        } else {
                            "Vocals loaded successfully"
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
        if (uri != null && !busy) {
            runCatching {
                context.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            }

            selectedModFolder = uri
            screen = Screen.ModTest
        }
    }

    BackHandler(
        enabled = showNew || (
            screen != Screen.Home &&
            screen != Screen.Play &&
            screen != Screen.ModTest
        )
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
        Screen.Home -> {
            HomeScreen(
                hasProject = hasProject,
                onContinue = {
                    if (hasProject) {
                        openScreen(Screen.Editor)
                    } else {
                        showNew = true
                    }
                },
                onNew = {
                    if (!busy) {
                        showNew = true
                    }
                },
                onImport = {
                    if (!busy) {
                        importer.launch(
                            arrayOf(
                                "application/json",
                                "text/plain"
                            )
                        )
                    }
                },
                onSecret = {
                    openScreen(Screen.Secret)
                },
                onSoon = { message ->
                    if (message == "Open Mod Folder") {
                        if (!busy) {
                            modFolderPicker.launch(null)
                        }
                    } else {
                        toast(message)
                    }
                }
            )
        }

        Screen.Editor -> {
            EditorScreen(
                project = project,
                onProject = { updated ->
                    project = updated
                    hasProject = true
                },
                onBack = {
                    navigateBack()
                },
                onImport = {
                    if (!busy) {
                        importer.launch(
                            arrayOf(
                                "application/json",
                                "text/plain"
                            )
                        )
                    }
                },
                onExportJson = {
                    if (!busy) {
                        jsonExporter.launch(
                            "${safeFileName(project.chart.song)}.json"
                        )
                    }
                },
                onExportMod = {
                    if (!busy) {
                        modExporter.launch(
                            "${safeFileName(project.modName)}.zip"
                        )
                    }
                },
                onPickInst = {
                    if (!busy) {
                        audioTarget = "inst"

                        audioPicker.launch(
                            arrayOf("audio/*")
                        )
                    }
                },
                onPickVoices = {
                    if (!busy) {
                        audioTarget = "voices"

                        audioPicker.launch(
                            arrayOf("audio/*")
                        )
                    }
                },
                onPlaytest = {
                    saveNow()
                    openScreen(Screen.Play)
                }
            )
        }

        Screen.Play -> {
            PlayScreen(
                project = project,
                onExit = {
                    navigateBack()
                }
            )
        }

        Screen.ModTest -> {
            val folder = selectedModFolder

            if (folder != null) {
                PlayTest(
                    modFolderUri = folder,
                    onBack = {
                        navigateBack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    screen = Screen.Home
                }
            }
        }

        Screen.Secret -> {
            SecretScreen()
        }
    }
}
