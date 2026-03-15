package com.tradesketch.estimator.data.local

import com.google.gson.Gson
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectMigrationTest {
    @Test
    fun migration_writesFiles_and_marksMigratedTrue() = runBlocking {
        val tempRoot = createTempDirectory(prefix = "project-migration-test-").toFile()
        try {
            val storage = ProjectFileStorageEngine(rootDir = File(tempRoot, "projects"))
            val legacyJson = """
                [
                  {"id":"p1","name":"Kitchen","createdAt":1,"updatedAt":2},
                  {"id":"p2","name":"Garage","createdAt":3,"updatedAt":4}
                ]
            """.trimIndent()

            val result = migrateLegacyProjectsJsonToFileStore(
                projectsJson = legacyJson,
                gson = Gson(),
                storageEngine = storage
            )

            val loaded = storage.loadAllProjects()
            assertTrue(result.migratedToFiles)
            assertEquals(2, result.writtenCount)
            assertEquals(2, loaded.size)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun migration_keepsLegacyData_whenJsonIsMalformed() = runBlocking {
        val tempRoot = createTempDirectory(prefix = "project-migration-failure-test-").toFile()
        try {
            val storage = ProjectFileStorageEngine(rootDir = File(tempRoot, "projects"))

            val result = migrateLegacyProjectsJsonToFileStore(
                projectsJson = "{ definitely not valid json",
                gson = Gson(),
                storageEngine = storage
            )

            assertFalse(result.migratedToFiles)
            assertEquals(0, result.writtenCount)
            assertTrue(result.failureMessage?.isNotBlank() == true)
            assertTrue(storage.loadAllProjects().isEmpty())
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
