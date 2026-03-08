package com.tradesketch.estimator.data.local

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Project
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectFileStorageEngineTest {
    @Test
    fun loadAllProjects_quarantinesInvalidJson_and_keepsValidProjects() = runBlocking {
        val tempRoot = createTempDir(prefix = "project-file-store-test-")
        try {
            val projectsDir = File(tempRoot, "projects")
            val storage = ProjectFileStorageEngine(rootDir = projectsDir)
            storage.saveProject(
                Project(
                    id = "valid-1",
                    name = "Valid",
                    createdAt = 1L,
                    updatedAt = 2L,
                    blueprintDocument = BlueprintDocument.empty("valid-1")
                )
            )
            File(projectsDir, "broken.json").writeText("{ this is not valid json")

            val loaded = storage.loadAllProjects()

            assertEquals(1, loaded.size)
            assertEquals("valid-1", loaded.first().id)
            val quarantined = projectsDir
                .listFiles()
                ?.firstOrNull { it.name.startsWith("broken.bad.") && it.name.endsWith(".json") }
            assertNotNull(quarantined)
            assertTrue(quarantined.exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun loadAllProjects_quarantinesStructurallyInvalidProject_payload() = runBlocking {
        val tempRoot = createTempDir(prefix = "project-file-store-missing-blueprint-test-")
        try {
            val projectsDir = File(tempRoot, "projects").apply { mkdirs() }
            val storage = ProjectFileStorageEngine(rootDir = projectsDir)
            File(projectsDir, "legacy.json").writeText(
                """
                {
                  "id": "legacy",
                  "name": "Legacy Project",
                  "createdAt": 1,
                  "updatedAt": 2,
                  "takeoffSession": null,
                  "blueprintDocument": null
                }
                """.trimIndent()
            )

            val loaded = storage.loadAllProjects()

            assertEquals(0, loaded.size)
            val quarantined = projectsDir
                .listFiles()
                ?.firstOrNull { it.name.startsWith("legacy.bad.") && it.name.endsWith(".json") }
            assertNotNull(quarantined)
            assertTrue(quarantined.exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
