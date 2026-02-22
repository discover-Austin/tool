package com.tradesketch.estimator.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoritativeBlueprintTest {

    @Test
    fun returnsBlueprintAsIsWhenProjectIdAlreadyMatches() {
        val blueprint = BlueprintDocument(
            projectId = "project-1",
            walls = listOf(
                WallSegment(
                    id = "w1",
                    start = PointMm(0, 0),
                    end = PointMm(1000, 0)
                )
            )
        )
        val project = Project(
            id = "project-1",
            name = "Test",
            blueprintDocument = blueprint
        )

        val resolved = project.authoritativeBlueprint()

        assertEquals("project-1", resolved.projectId)
        assertEquals(1, resolved.walls.size)
        assertEquals("w1", resolved.walls.first().id)
    }

    @Test
    fun rewritesProjectIdWhenBlueprintHasContent() {
        val blueprint = BlueprintDocument(
            projectId = "legacy-id",
            rooms = listOf(
                Room(
                    id = "room-1",
                    name = "Room 1",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(2000, 0),
                        PointMm(2000, 1500),
                        PointMm(0, 1500)
                    )
                )
            )
        )
        val project = Project(
            id = "project-2",
            name = "Test",
            blueprintDocument = blueprint
        )

        val resolved = project.authoritativeBlueprint()

        assertEquals("project-2", resolved.projectId)
        assertEquals(1, resolved.rooms.size)
        assertEquals("room-1", resolved.rooms.first().id)
    }

    @Test
    fun returnsEmptyBlueprintForTrulyEmptyMismatchedDocument() {
        val project = Project(
            id = "project-3",
            name = "Test",
            blueprintDocument = BlueprintDocument.empty(projectId = "stale-id")
        )

        val resolved = project.authoritativeBlueprint()

        assertEquals("project-3", resolved.projectId)
        assertTrue(resolved.walls.isEmpty())
        assertTrue(resolved.rooms.isEmpty())
        assertTrue(resolved.openings.isEmpty())
    }
}
