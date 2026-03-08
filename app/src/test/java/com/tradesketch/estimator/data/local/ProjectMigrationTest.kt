package com.tradesketch.estimator.data.local

import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectMigrationTest {
    @Test
    fun migration_writesFiles_and_marksMigratedTrue() = runBlocking {
        val tempRoot = createTempDir(prefix = "project-migration-test-")
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
}
