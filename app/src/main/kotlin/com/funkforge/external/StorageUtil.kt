package com.funkforge.external

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.funkforge.model.Chart
import com.funkforge.model.ModExporter
import com.funkforge.model.Project
import com.funkforge.model.PsychFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object StorageUtil {

    private val audioExtensions = setOf(
        "ogg", "mp3", "wav", "m4a", "flac"
    )

    private const val MAX_JSON_BYTES = 32L * 1024L * 1024L
    private const val MAX_AUDIO_BYTES = 512L * 1024L * 1024L

    fun safeName(value: String): String {
        return value
            .trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('.', '_')
            .take(100)
            .ifBlank { "untitled" }
    }

    fun createProject(
        modName: String = "My Mod",
        songName: String = "Untitled"
    ): Project {
        return Project(
            modName = modName.trim().ifBlank { "My Mod" },
            chart = Chart(
                song = songName.trim().ifBlank { "Untitled" }
            )
        )
    }

    fun displayName(
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

                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)
                } else {
                    null
                }
            } ?: uri.lastPathSegment ?: "unknown"
        }.getOrDefault("unknown")
    }

    private fun readLimited(
        context: Context,
        uri: Uri,
        maxBytes: Long
    ): ByteArray {
        val input = context.contentResolver
            .openInputStream(uri)
            ?: throw IOException("Unable to open file")

        return input.buffered().use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L

            while (true) {
                val count = stream.read(buffer)

                if (count == -1) break

                total += count

                if (total > maxBytes) {
                    throw IOException("File is too large")
                }

                output.write(buffer, 0, count)
            }

            output.toByteArray()
        }
    }

    suspend fun importChart(
        context: Context,
        uri: Uri
    ): Chart = withContext(Dispatchers.IO) {
        val bytes = readLimited(
            context,
            uri,
            MAX_JSON_BYTES
        )

        if (bytes.isEmpty()) {
            throw IOException("Chart file is empty")
        }

        val json = bytes.toString(Charsets.UTF_8)
            .removePrefix("\uFEFF")

        PsychFormat.import(json)
    }

    suspend fun exportChart(
        context: Context,
        uri: Uri,
        chart: Chart
    ) = withContext(Dispatchers.IO) {
        val json = PsychFormat.export(chart)

        val output = context.contentResolver
            .openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open export destination")

        output.bufferedWriter(Charsets.UTF_8).use {
            it.write(json)
            it.flush()
        }
    }

    suspend fun exportMod(
        context: Context,
        uri: Uri,
        project: Project
    ) = withContext(Dispatchers.IO) {
        val output = context.contentResolver
            .openOutputStream(uri, "w")
            ?: throw IOException("Unable to create ZIP")

        output.use {
            ModExporter.write(project, it)
        }
    }

    suspend fun importAudio(
        context: Context,
        uri: Uri,
        target: String
    ): File = withContext(Dispatchers.IO) {
        val name = displayName(context, uri)
        val extension = name
            .substringAfterLast('.', "")
            .lowercase()

        if (extension !in audioExtensions) {
            throw IOException(
                "Unsupported audio format: $extension"
            )
        }

        val directory = File(
            context.filesDir,
            "audio"
        )

        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create audio directory")
        }

        val prefix = safeName(target)
            .take(20)
            .ifBlank { "audio" }

        val file = File(
            directory,
            "${prefix}_${UUID.randomUUID()}.$extension"
        )

        try {
            val input = context.contentResolver
                .openInputStream(uri)
                ?: throw IOException("Unable to read audio")

            input.buffered().use { source ->
                file.outputStream().buffered().use { destination ->
                    val buffer = ByteArray(8192)
                    var total = 0L

                    while (true) {
                        val count = source.read(buffer)

                        if (count == -1) break

                        total += count

                        if (total > MAX_AUDIO_BYTES) {
                            throw IOException("Audio file is too large")
                        }

                        destination.write(buffer, 0, count)
                    }
                }
            }

            if (file.length() == 0L) {
                throw IOException("Audio file is empty")
            }

            file
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun isManagedAudio(
        context: Context,
        path: String
    ): Boolean {
        return runCatching {
            val directory = File(
                context.filesDir,
                "audio"
            ).canonicalFile

            val file = File(path).canonicalFile

            file.parentFile == directory
        }.getOrDefault(false)
    }

    suspend fun deleteUnusedAudio(
        context: Context,
        path: String?,
        project: Project
    ): Boolean = withContext(Dispatchers.IO) {
        if (path == null) return@withContext false

        if (
            path == project.instPath ||
            path == project.voicesPath
        ) {
            return@withContext false
        }

        if (!isManagedAudio(context, path)) {
            return@withContext false
        }

        File(path).delete()
    }
}
