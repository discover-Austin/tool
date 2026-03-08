package com.tradesketch.estimator.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProjectFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val engine by lazy {
        ProjectFileStorageEngine(
            rootDir = File(context.filesDir, "projects"),
            gson = gson
        )
    }

    suspend fun loadAllProjects(): List<Project> = engine.loadAllProjects()

    suspend fun saveProject(project: Project) = engine.saveProject(project)

    suspend fun deleteProject(projectId: String) = engine.deleteProject(projectId)

    internal fun storageEngine(): ProjectFileStorageEngine = engine
}

internal class ProjectFileStorageEngine(
    private val rootDir: File,
    private val gson: Gson = Gson()
) {
    suspend fun loadAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        val dir = ensureRootDir() ?: return@withContext emptyList()
        val files = dir.listFiles { file ->
            file.isFile && file.extension.equals("json", ignoreCase = true)
        }?.sortedBy { it.name } ?: return@withContext emptyList()
        val quarantineStamp = System.currentTimeMillis()
        files.mapNotNull { file ->
            val loaded = runCatching {
                val raw = file.readText()
                val parsed = runCatching {
                    gson.fromJson(raw, Project::class.java)
                }.getOrNull()
                sanitizeLoadedProject(parsed, file)
                    ?: parseProjectFallback(raw = raw, sourceFile = file)
            }.getOrNull()
            loaded ?: run {
                quarantineCorruptFile(
                    file = file,
                    dir = dir,
                    quarantineStamp = quarantineStamp
                )
                null
            }
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val dir = ensureRootDir() ?: return@withContext
        val target = File(dir, "${safeProjectId(project.id)}.json")
        val temp = File(dir, "${safeProjectId(project.id)}.json.tmp")
        val payload = gson.toJson(
            project.copy(
                blueprintDocument = project.blueprintDocument.copy(projectId = project.id)
            )
        )
        temp.writeText(payload)
        if (!temp.renameTo(target)) {
            runCatching {
                target.writeText(payload)
                temp.delete()
            }.getOrElse {
                temp.delete()
                throw IOException("Failed to atomically write project ${project.id}", it)
            }
        }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val dir = ensureRootDir() ?: return@withContext
        val target = File(dir, "${safeProjectId(projectId)}.json")
        if (target.exists()) {
            target.delete()
        }
    }

    private fun ensureRootDir(): File? {
        val dir = rootDir
        return if (dir.exists() || dir.mkdirs()) dir else null
    }

    private fun safeProjectId(projectId: String): String {
        return projectId
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "project" }
    }

    private fun sanitizeLoadedProject(
        project: Project?,
        sourceFile: File
    ): Project? {
        if (project == null) return null
        val fallbackId = sourceFile.nameWithoutExtension.trim()
        val projectId = runCatching { project.id.trim() }
            .getOrDefault("")
            .ifBlank { fallbackId }
            .ifBlank { return null }
        val projectName = runCatching { project.name.trim() }
            .getOrDefault("")
            .ifBlank { "Untitled project" }
        val createdAt = runCatching { project.createdAt }
            .getOrDefault(System.currentTimeMillis())
        val updatedAt = runCatching { project.updatedAt }
            .getOrDefault(createdAt)
        val takeoffSession = runCatching { project.takeoffSession }
            .getOrDefault(ProjectTakeoffSession())
        val blueprintDocument = runCatching { project.blueprintDocument }
            .getOrNull()
            ?.copy(projectId = projectId)
            ?: BlueprintDocument.empty(projectId = projectId)

        return Project(
            id = projectId,
            name = projectName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            takeoffSession = takeoffSession,
            blueprintDocument = blueprintDocument
        )
    }

    private fun parseProjectFallback(
        raw: String,
        sourceFile: File
    ): Project? {
        val payload = runCatching {
            gson.fromJson(raw, JsonObject::class.java)
        }.getOrNull() ?: return null

        val fallbackId = sourceFile.nameWithoutExtension.trim()
        val projectId = payload.readString("id")
            .ifBlank { fallbackId }
            .ifBlank { return null }
        val projectName = payload.readString("name")
            .ifBlank { "Untitled project" }
        val createdAt = payload.readLong("createdAt") ?: System.currentTimeMillis()
        val updatedAt = payload.readLong("updatedAt") ?: createdAt
        val takeoffSession = payload.readObject("takeoffSession")
            ?.let { takeoffJson ->
                runCatching {
                    gson.fromJson(takeoffJson, ProjectTakeoffSession::class.java)
                }.getOrNull()
            }
            ?: ProjectTakeoffSession()
        val blueprintDocument = payload.readObject("blueprintDocument")
            ?.let { blueprintJson ->
                runCatching {
                    gson.fromJson(blueprintJson, BlueprintDocument::class.java)
                }.getOrNull()
            }
            ?.copy(projectId = projectId)
            ?: BlueprintDocument.empty(projectId = projectId)

        return Project(
            id = projectId,
            name = projectName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            takeoffSession = takeoffSession,
            blueprintDocument = blueprintDocument
        )
    }

    private fun quarantineCorruptFile(
        file: File,
        dir: File,
        quarantineStamp: Long
    ) {
        val baseName = file.nameWithoutExtension.ifBlank { "project" }
        var candidate = File(dir, "$baseName.bad.$quarantineStamp.json")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(dir, "$baseName.bad.$quarantineStamp.$suffix.json")
            suffix += 1
        }
        if (!file.renameTo(candidate)) {
            runCatching {
                candidate.writeText(file.readText())
                file.delete()
            }
        }
    }
}

private fun JsonObject.readString(key: String): String {
    val element = this.get(key) ?: return ""
    if (element.isJsonNull) return ""
    return runCatching { element.asString.trim() }.getOrDefault("")
}

private fun JsonObject.readLong(key: String): Long? {
    val element = this.get(key) ?: return null
    if (element.isJsonNull) return null
    return runCatching { element.asLong }.getOrNull()
}

private fun JsonObject.readObject(key: String): JsonObject? {
    val element = this.get(key) ?: return null
    if (element.isJsonNull || !element.isJsonObject) return null
    return element.asJsonObject
}
