package com.tradesketch.estimator.tools

import com.google.gson.Gson
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import org.junit.Test
import java.io.File

class EmitPlayStoreSeedProjectJsonTest {

    @Test
    fun emitSeedProjectJson() {
        val projectId = "fba2816f-7463-46d7-87d9-3cd666260f79"
        val wallHeight = Millimeters.fromFeet(9.0)
        val width = Millimeters.fromFeet(18.0).value
        val depth = Millimeters.fromFeet(12.0).value
        val partitionX = Millimeters.fromFeet(11.0).value

        val walls = listOf(
            WallSegment(
                id = "wall-1",
                start = PointMm(0, 0),
                end = PointMm(width, 0),
                height = wallHeight
            ),
            WallSegment(
                id = "wall-2",
                start = PointMm(width, 0),
                end = PointMm(width, depth),
                height = wallHeight
            ),
            WallSegment(
                id = "wall-3",
                start = PointMm(width, depth),
                end = PointMm(0, depth),
                height = wallHeight
            ),
            WallSegment(
                id = "wall-4",
                start = PointMm(0, depth),
                end = PointMm(0, 0),
                height = wallHeight
            ),
            WallSegment(
                id = "wall-5",
                start = PointMm(partitionX, 0),
                end = PointMm(partitionX, depth),
                height = wallHeight
            )
        )

        val rooms = listOf(
            Room(
                id = "room-1",
                name = "Front Office",
                polygon = listOf(
                    PointMm(0, 0),
                    PointMm(partitionX, 0),
                    PointMm(partitionX, depth),
                    PointMm(0, depth)
                ),
                wallSegmentIds = listOf("wall-1", "wall-4", "wall-3", "wall-5")
            ),
            Room(
                id = "room-2",
                name = "Storage",
                polygon = listOf(
                    PointMm(partitionX, 0),
                    PointMm(width, 0),
                    PointMm(width, depth),
                    PointMm(partitionX, depth)
                ),
                wallSegmentIds = listOf("wall-1", "wall-2", "wall-3", "wall-5")
            )
        )

        val openings = listOf(
            BlueprintOpening(
                id = "door-1",
                wallId = "wall-5",
                t = 0.56,
                widthMm = Millimeters.fromFeet(3.0).value,
                heightMm = Millimeters.fromFeet(7.0).value,
                sillMm = 0L,
                type = OpeningType.DOOR
            ),
            BlueprintOpening(
                id = "window-1",
                wallId = "wall-2",
                t = 0.28,
                widthMm = Millimeters.fromFeet(4.0).value,
                heightMm = Millimeters.fromFeet(4.0).value,
                sillMm = Millimeters.fromFeet(3.0).value,
                type = OpeningType.WINDOW
            ),
            BlueprintOpening(
                id = "window-2",
                wallId = "wall-2",
                t = 0.72,
                widthMm = Millimeters.fromFeet(4.0).value,
                heightMm = Millimeters.fromFeet(4.0).value,
                sillMm = Millimeters.fromFeet(3.0).value,
                type = OpeningType.WINDOW
            )
        )

        val blueprint = BlueprintDocument(
            projectId = projectId,
            walls = walls,
            rooms = rooms,
            openings = openings
        )

        val project = Project(
            id = projectId,
            name = "Front Office",
            createdAt = 1775990400000,
            updatedAt = 1775990400000,
            takeoffSession = ProjectTakeoffSession(
                selectedScope = TakeoffScope.DRYWALL,
                inputMode = TakeoffInputMode.BLUEPRINT
            ),
            blueprintDocument = blueprint
        )

        val output = File("tmp/play_store_seed_project.json")
        output.parentFile.mkdirs()
        output.writeText(Gson().toJson(project))
        println(output.absolutePath)
    }
}
