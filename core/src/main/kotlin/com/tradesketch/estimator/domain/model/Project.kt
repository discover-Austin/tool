package com.tradesketch.estimator.domain.model

/**
 * Represents a construction project with multiple spaces.
 */
data class Project(
    val id: String,
    val name: String,
    val spaces: List<Space> = emptyList(),
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
        val spaces = when (this) {
            BEDROOM -> listOf(
                Space(
                    id = "wall-1",
                    name = "Wall 1",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(12.0),
                        height = Millimeters.fromFeet(8.0)
                    )
                ),
                Space(
                    id = "wall-2",
                    name = "Wall 2",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(10.0),
                        height = Millimeters.fromFeet(8.0)
                    )
                ),
                Space(
                    id = "wall-3",
                    name = "Wall 3",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(12.0),
                        height = Millimeters.fromFeet(8.0)
                    ),
                    openings = listOf(
                        Opening(
                            width = Millimeters.fromFeet(3.0),
                            height = Millimeters.fromFeet(7.0),
                            count = 1
                        )
                    )
                ),
                Space(
                    id = "wall-4",
                    name = "Wall 4",
                    geometry = Geometry.Wall(
                        length = Millimeters.fromFeet(10.0),
                        height = Millimeters.fromFeet(8.0)
                    ),
                    openings = listOf(
                        Opening(
                            width = Millimeters.fromFeet(4.0),
                            height = Millimeters.fromFeet(5.0),
                            count = 2
                        )
                    )
                ),
                Space(
                    id = "ceiling-1",
                    name = "Ceiling",
                    geometry = Geometry.Rect(
                        length = Millimeters.fromFeet(12.0),
                        width = Millimeters.fromFeet(10.0)
                    )
                )
            )
            GARAGE -> listOf(
                Space(
                    id = "slab-1",
                    name = "Garage slab",
                    geometry = Geometry.Slab(
                        length = Millimeters.fromFeet(20.0),
                        width = Millimeters.fromFeet(20.0),
                        thickness = Millimeters.fromFeet(0.33)
                    )
                )
            )
            DRIVEWAY -> listOf(
                Space(
                    id = "slab-1",
                    name = "Driveway",
                    geometry = Geometry.Slab(
                        length = Millimeters.fromFeet(40.0),
                        width = Millimeters.fromFeet(12.0),
                        thickness = Millimeters.fromFeet(0.33)
                    )
                )
            )
            YARD_BED -> listOf(
                Space(
                    id = "yard-1",
                    name = "Yard bed",
                    geometry = Geometry.Rect(
                        length = Millimeters.fromFeet(15.0),
                        width = Millimeters.fromFeet(8.0)
                    )
                )
            )
            BLANK -> emptyList()
        }

        val id = java.util.UUID.randomUUID().toString()
        return Project(
            id = id,
            name = name,
            spaces = spaces,
            blueprintDocument = BlueprintDocument.fromLegacySpaces(projectId = id, spaces = spaces)
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
