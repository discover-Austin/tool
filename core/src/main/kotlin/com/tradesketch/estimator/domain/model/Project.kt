package com.tradesketch.estimator.domain.model

/**
 * Represents a construction project using blueprint-first architecture.
 * All geometric data is stored in blueprintDocument.
 */
data class Project(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val takeoffSession: ProjectTakeoffSession = ProjectTakeoffSession(),
    val blueprintDocument: BlueprintDocument = BlueprintDocument.empty(projectId = id)
)

/**
 * Pre-built project templates for common scenarios.
 */
enum class ProjectTemplate {
    BEDROOM,
    GARAGE,
    DRIVEWAY,
    YARD_BED,
    BLANK;

    fun createProject(name: String = this.displayName()): Project {
        val id = java.util.UUID.randomUUID().toString()
        val blueprint = when (this) {
            BEDROOM -> {
                // Create a simple bedroom with 4 walls and a ceiling room
                val wallHeight = Millimeters.fromFeet(8.0)
                val walls = listOf(
                    WallSegment(
                        id = "wall-1",
                        start = PointMm(0, 0),
                        end = PointMm(Millimeters.fromFeet(12.0).value, 0),
                        height = wallHeight
                    ),
                    WallSegment(
                        id = "wall-2",
                        start = PointMm(Millimeters.fromFeet(12.0).value, 0),
                        end = PointMm(Millimeters.fromFeet(12.0).value, Millimeters.fromFeet(10.0).value),
                        height = wallHeight
                    ),
                    WallSegment(
                        id = "wall-3",
                        start = PointMm(Millimeters.fromFeet(12.0).value, Millimeters.fromFeet(10.0).value),
                        end = PointMm(0, Millimeters.fromFeet(10.0).value),
                        height = wallHeight
                    ),
                    WallSegment(
                        id = "wall-4",
                        start = PointMm(0, Millimeters.fromFeet(10.0).value),
                        end = PointMm(0, 0),
                        height = wallHeight
                    )
                )
                val room = Room(
                    id = "room-1",
                    name = "Bedroom",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(Millimeters.fromFeet(12.0).value, 0),
                        PointMm(Millimeters.fromFeet(12.0).value, Millimeters.fromFeet(10.0).value),
                        PointMm(0, Millimeters.fromFeet(10.0).value)
                    ),
                    wallSegmentIds = walls.map { it.id },
                    ceiling = CeilingSpec(enabled = true, height = wallHeight)
                )
                val openings = listOf(
                    BlueprintOpening(
                        id = "door-1",
                        wallId = "wall-3",
                        t = 0.5,
                        widthMm = Millimeters.fromFeet(3.0).value,
                        heightMm = Millimeters.fromFeet(7.0).value,
                        sillMm = 0L,
                        type = OpeningType.DOOR
                    ),
                    BlueprintOpening(
                        id = "window-1",
                        wallId = "wall-2",
                        t = 0.3,
                        widthMm = Millimeters.fromFeet(4.0).value,
                        heightMm = Millimeters.fromFeet(5.0).value,
                        sillMm = Millimeters.fromFeet(3.0).value,
                        type = OpeningType.WINDOW
                    ),
                    BlueprintOpening(
                        id = "window-2",
                        wallId = "wall-2",
                        t = 0.7,
                        widthMm = Millimeters.fromFeet(4.0).value,
                        heightMm = Millimeters.fromFeet(5.0).value,
                        sillMm = Millimeters.fromFeet(3.0).value,
                        type = OpeningType.WINDOW
                    )
                )
                BlueprintDocument(
                    projectId = id,
                    walls = walls,
                    rooms = listOf(room),
                    openings = openings
                )
            }
            GARAGE -> {
                // Create a 20'×20' garage slab as a room
                val room = Room(
                    id = "room-1",
                    name = "Garage slab",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(Millimeters.fromFeet(20.0).value, 0),
                        PointMm(Millimeters.fromFeet(20.0).value, Millimeters.fromFeet(20.0).value),
                        PointMm(0, Millimeters.fromFeet(20.0).value)
                    ),
                    tags = setOf("slab", "concrete"),
                    ceiling = CeilingSpec(enabled = false)
                )
                BlueprintDocument(
                    projectId = id,
                    rooms = listOf(room)
                )
            }
            DRIVEWAY -> {
                // Create a 40'×12' driveway slab as a room
                val room = Room(
                    id = "room-1",
                    name = "Driveway",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(Millimeters.fromFeet(40.0).value, 0),
                        PointMm(Millimeters.fromFeet(40.0).value, Millimeters.fromFeet(12.0).value),
                        PointMm(0, Millimeters.fromFeet(12.0).value)
                    ),
                    tags = setOf("slab", "concrete"),
                    ceiling = CeilingSpec(enabled = false)
                )
                BlueprintDocument(
                    projectId = id,
                    rooms = listOf(room)
                )
            }
            YARD_BED -> {
                // Create a 15'×8' yard bed as a room
                val room = Room(
                    id = "room-1",
                    name = "Yard bed",
                    polygon = listOf(
                        PointMm(0, 0),
                        PointMm(Millimeters.fromFeet(15.0).value, 0),
                        PointMm(Millimeters.fromFeet(15.0).value, Millimeters.fromFeet(8.0).value),
                        PointMm(0, Millimeters.fromFeet(8.0).value)
                    ),
                    tags = setOf("bed", "gravel"),
                    ceiling = CeilingSpec(enabled = false)
                )
                BlueprintDocument(
                    projectId = id,
                    rooms = listOf(room)
                )
            }
            BLANK -> BlueprintDocument.empty(projectId = id)
        }

        return Project(
            id = id,
            name = name,
            blueprintDocument = blueprint
        )
    }

    fun displayName(): String = when (this) {
        BEDROOM -> "Bedroom"
        GARAGE -> "Garage"
        DRIVEWAY -> "Driveway slab"
        YARD_BED -> "Yard bed"
        BLANK -> "Blank project"
    }

    fun description(): String = when (this) {
        BEDROOM -> "4 walls + ceiling, with door and windows"
        GARAGE -> "20'×20' concrete slab, 4\" thick"
        DRIVEWAY -> "40'×12' concrete slab, 4\" thick"
        YARD_BED -> "15'×8' rectangular bed"
        BLANK -> "Start from scratch"
    }
}
