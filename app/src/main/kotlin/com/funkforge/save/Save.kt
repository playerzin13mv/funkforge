
package com.funkforge.save

import android.content.Context
import com.funkforge.model.Project
import com.funkforge.model.ProjectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class SaveStatus {
    Idle,
    Pending,
    Saving,
    Saved,
    Failed
}

data class SaveState(
    val status: SaveStatus = SaveStatus.Idle,
    val lastSavedAt: Long = 0L,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)

class SaveManager(
    context: Context,
    private val debounceMillis: Long = 1200L
) {
    private val appContext = context.applicationContext

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val mutex = Mutex()

    private val mutableState = MutableStateFlow(SaveState())

    val state: StateFlow<SaveState> = mutableState.asStateFlow()

    private var pendingJob: Job? = null
    private var latestProject: Project? = null
    private var revision = 0L
    private var savedRevision = 0L
    private var closed = false

    fun load(): Project? {
        return runCatching {
            ProjectStore.load(appContext)
        }.onFailure { error ->
            mutableState.value = SaveState(
                status = SaveStatus.Failed,
                error = error.message ?: "Unable to load project"
            )
        }.getOrNull()
    }

    fun requestSave(project: Project) {
        if (closed) return

        latestProject = project
        revision++

        mutableState.value = mutableState.value.copy(
            status = SaveStatus.Pending,
            error = null,
            hasUnsavedChanges = true
        )

        pendingJob?.cancel()

        pendingJob = scope.launch {
            delay(debounceMillis)
            flush()
        }
    }

    suspend fun flush(): Boolean {
        val snapshot = latestProject ?: return true
        val snapshotRevision = revision

        return mutex.withLock {
            if (snapshotRevision <= savedRevision) {
                return@withLock true
            }

            mutableState.value = mutableState.value.copy(
                status = SaveStatus.Saving,
                error = null
            )

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    ProjectStore.save(appContext, snapshot)
                }
            }

            result.fold(
                onSuccess = {
                    savedRevision = snapshotRevision

                    val hasChanges = revision > savedRevision

                    mutableState.value = SaveState(
                        status = if (hasChanges) {
                            SaveStatus.Pending
                        } else {
                            SaveStatus.Saved
                        },
                        lastSavedAt = System.currentTimeMillis(),
                        hasUnsavedChanges = hasChanges
                    )

                    true
                },
                onFailure = { error ->
                    mutableState.value = mutableState.value.copy(
                        status = SaveStatus.Failed,
                        error = error.message ?: "Unable to save project",
                        hasUnsavedChanges = true
                    )

                    false
                }
            )
        }
    }

    fun saveNow(project: Project) {
        requestSave(project)
        pendingJob?.cancel()

        pendingJob = scope.launch {
            flush()
        }
    }

    fun retry() {
        if (closed || latestProject == null) return

        pendingJob?.cancel()

        pendingJob = scope.launch {
            flush()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        pendingJob?.cancel()
        scope.cancel()
    }
}
