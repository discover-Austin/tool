package com.tradesketch.estimator.domain.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

data class PointMm(
    val x: Long,
    val y: Long
)

enum class BlueprintUnits {
    IMPERIAL,
    METRIC
}

enum class BlueprintPrecision {
    STANDARD,
    PRO
}

data class BlueprintParams(
    val wallHeightMm: Long = Millimeters.fromFeet(9.0).value,
    val bedDepthMm: Long = Millimeters.fromInches(4.0).value,
    val paintCoats: Int = 2,
    val drywallSheetSqFt: Double = 32.0,
    val wasteFactorPercent: Double = 10.0,
    val concreteThicknessMm: Long = Millimeters.fromFeet(0.33).value,
    val defaultWallThicknessMm: Long = Millimeters.fromInches(4.5).value
)

data class BlueprintUndoStackMeta(
    val undoDepth: Int = 0,
    val redoDepth: Int = 0,
    val revision: Long = 0L
)

data class BlueprintOpening(
    val id: String,
    val wallId: String,
    val t: Double,
    val widthMm: Long,
    val heightMm: Long,
    val sillMm: Long,
    val type: OpeningType = OpeningType.WINDOW,
    val tags: Set<String> = emptySet()
) {
    val openingAreaSqFt: Double
        get() = areaSqFt(Millimeters(widthMm), Millimeters(heightMm))

    fun normalized(): BlueprintOpening {
        return copy(
            t = t.coerceIn(0.0, 1.0),
            widthMm = widthMm.coerceAtLeast(1L),
            heightMm = heightMm.coerceAtLeast(1L),
            sillMm = sillMm.coerceAtLeast(0L)
        )
    }
}

enum class WallType {
    INTERIOR,
    EXTERIOR,
    FOUNDATION,
    PARTITION
}

data class WallSegment(
    val id: String,
    val start: PointMm,
    val end: PointMm,
    val thickness: Millimeters = Millimeters.fromInches(4.5),
    val height: Millimeters = Millimeters.fromFeet(9.0),
    val tags: Set<String> = emptySet(),
    val type: WallType = WallType.INTERIOR
) {
    val startMm: PointMm
        get() = start
    val endMm: PointMm
        get() = end
    val thicknessMm: Long
        get() = thickness.value
    val heightMm: Long
        get() = height.value

    fun lengthMillimeters(): Long {
        return hypot(
            (end.x - start.x).toDouble(),
            (end.y - start.y).toDouble()
        ).roundToLong()
    }

    fun lengthFeet(): Double = Millimeters(lengthMillimeters()).toFeet()

    fun angleDegrees(): Double {
        return Math.toDegrees(
            atan2(
                (end.y - start.y).toDouble(),
                (end.x - start.x).toDouble()
            )
        )
    }

    fun midpoint(): PointMm {
        return PointMm(
            x = ((start.x + end.x) / 2.0).roundToLong(),
            y = ((start.y + end.y) / 2.0).roundToLong()
        )
    }
}

data class RoomOverrides(
    val wallHeightMm: Long? = null,
    val paintCoats: Int? = null,
    val wasteFactorPercent: Double? = null
)

data class CeilingSpec(
    val enabled: Boolean = true,
    val height: Millimeters = Millimeters.fromFeet(9.0)
)

data class Room(
    val id: String,
    val name: String = "Room",
    val polygon: List<PointMm> = emptyList(),
    val wallSegmentIds: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
    val ceiling: CeilingSpec = CeilingSpec(),
    val overrides: RoomOverrides = RoomOverrides(),
    val wallLoopRef: List<String> = wallSegmentIds
) {
    fun areaSquareMillimeters(): Long {
        if (polygon.size < 3) return 0L
        var twiceArea = 0.0
        for (index in polygon.indices) {
            val a = polygon[index]
            val b = polygon[(index + 1) % polygon.size]
            twiceArea += (a.x.toDouble() * b.y.toDouble()) - (b.x.toDouble() * a.y.toDouble())
        }
        return (abs(twiceArea) / 2.0).roundToLong()
    }

    fun perimeterMillimeters(): Long {
        if (polygon.size < 2) return 0L
        var total = 0.0
        for (index in polygon.indices) {
            val a = polygon[index]
            val b = polygon[(index + 1) % polygon.size]
            total += hypot(
                (b.x - a.x).toDouble(),
                (b.y - a.y).toDouble()
            )
        }
        return total.roundToLong()
    }

    fun areaSqFt(): Double {
        return areaSquareMillimeters().toDouble() / 92_903.04
    }

    fun perimeterFeet(): Double {
        return Millimeters(perimeterMillimeters()).toFeet()
    }

    fun wallSurfaceAreaSqFt(openingAreaSqFt: Double = 0.0): Double {
        val gross = perimeterFeet() * ceiling.height.toFeet()
        return (gross - openingAreaSqFt).coerceAtLeast(0.0)
    }
}

data class BlueprintDocument(
    val projectId: String,
    val units: BlueprintUnits = BlueprintUnits.IMPERIAL,
    val precision: BlueprintPrecision = BlueprintPrecision.PRO,
    val params: BlueprintParams = BlueprintParams(),
    val walls: List<WallSegment> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val openings: List<BlueprintOpening> = emptyList(),
    val undoStackMeta: BlueprintUndoStackMeta = BlueprintUndoStackMeta()
) {
    companion object {
        fun empty(projectId: String): BlueprintDocument {
            return BlueprintDocument(projectId = projectId)
        }

        fun fromLegacySpaces(
            projectId: String,
            spaces: List<Space>,
            params: BlueprintParams = BlueprintParams()
        ): BlueprintDocument {
            val walls = spaces.mapNotNull { it.toWallSegmentOrNull() }
            val roomSpaces = spaces.filter { it.geometry is Geometry.Rect || it.geometry is Geometry.LShape }
            val rooms = roomSpaces.mapIndexed { index, space ->
                Room(
                    id = "room-${index + 1}",
                    name = space.name,
                    polygon = spacePolygon(space),
                    tags = space.tags
                )
            }
            val openings = spaces.flatMap { space ->
                space.openings.mapIndexed { index, opening ->
                    BlueprintOpening(
                        id = opening.id.ifBlank { "${space.id}-opening-${index + 1}" },
                        wallId = space.id,
                        t = opening.wallPositionT,
                        widthMm = opening.width.value,
                        heightMm = opening.height.value,
                        sillMm = opening.sillHeight.value,
                        type = opening.type
                    ).normalized()
                }
            }
            return BlueprintDocument(
                projectId = projectId,
                params = params,
                walls = walls,
                rooms = rooms,
                openings = openings
            )
        }

        private fun spacePolygon(space: Space): List<PointMm> {
            val geometry = space.geometry as? Geometry.Rect ?: return emptyList()
            val centerX = Millimeters.fromFeet(space.transform.xFeet).value
            val centerY = Millimeters.fromFeet(space.transform.zFeet).value
            val halfLength = geometry.length.value / 2
            val halfWidth = geometry.width.value / 2
            return listOf(
                PointMm(centerX - halfLength, centerY - halfWidth),
                PointMm(centerX + halfLength, centerY - halfWidth),
                PointMm(centerX + halfLength, centerY + halfWidth),
                PointMm(centerX - halfLength, centerY + halfWidth)
            )
        }
    }

    fun wallById(id: String): WallSegment? = walls.firstOrNull { it.id == id }

    fun openingAreaByWallId(): Map<String, Double> {
        return openings.groupBy { it.wallId }
            .mapValues { (_, entries) -> entries.sumOf { it.openingAreaSqFt } }
    }

    fun withRoomsDetected(detectedRooms: List<Room>): BlueprintDocument {
        return copy(rooms = detectedRooms)
    }

    fun addWall(wall: WallSegment): BlueprintDocument {
        return copy(
            walls = walls + wall,
            undoStackMeta = undoStackMeta.copy(revision = undoStackMeta.revision + 1)
        )
    }
}

data class MaterialParams(
    val paintCoats: Int = 2,
    val drywallSheetAreaSqFt: Double = 32.0,
    val wasteFactorPercent: Double = 10.0,
    val concreteThicknessFeet: Double = 0.33
)

enum class BlueprintTool {
    SELECT,
    DRAW_WALL,
    PLACE_DOOR,
    PLACE_WINDOW,
    PAN,
    MEASURE
}

data class BlueprintSnapSettings(
    val gridEnabled: Boolean = true,
    val endpointEnabled: Boolean = true,
    val midpointEnabled: Boolean = true,
    val angleEnabled: Boolean = true,
    val closureEnabled: Boolean = true,
    val gridStepFeet: Double = 1.0,
    val angleIncrementDegrees: Int = 15,
    val thresholdFeet: Double = 0.75
)

fun Space.toWallSegmentOrNull(): WallSegment? {
    val wall = geometry as? Geometry.Wall ?: return null
    val halfLength = wall.length.toFeet() / 2.0
    val yawRadians = Math.toRadians(transform.yawDegrees)
    val dx = cos(yawRadians) * halfLength
    val dy = sin(yawRadians) * halfLength
    val centerX = Millimeters.fromFeet(transform.xFeet).value
    val centerY = Millimeters.fromFeet(transform.zFeet).value
    val start = PointMm(
        x = (centerX - Millimeters.fromFeet(dx).value),
        y = (centerY - Millimeters.fromFeet(dy).value)
    )
    val end = PointMm(
        x = (centerX + Millimeters.fromFeet(dx).value),
        y = (centerY + Millimeters.fromFeet(dy).value)
    )
    return WallSegment(
        id = id,
        start = start,
        end = end,
        height = wall.height,
        tags = tags
    )
}

fun Project.authoritativeBlueprint(): BlueprintDocument {
    val current = blueprintDocument
    if (current.projectId == id) {
        return current
    }
    if (current.walls.isNotEmpty() || current.rooms.isNotEmpty() || current.openings.isNotEmpty()) {
        return current.copy(projectId = id)
    }
    return BlueprintDocument.empty(projectId = id)
}

fun BlueprintDocument.toLegacySpaces(): List<Space> {
    val roomSpaces = rooms.map { room ->
        val polygon = room.polygon
        val minX = polygon.minOfOrNull { it.x } ?: 0L
        val maxX = polygon.maxOfOrNull { it.x } ?: 0L
        val minY = polygon.minOfOrNull { it.y } ?: 0L
        val maxY = polygon.maxOfOrNull { it.y } ?: 0L
        val lengthMm = (maxX - minX).coerceAtLeast(1L)
        val widthMm = (maxY - minY).coerceAtLeast(1L)
        Space(
            id = room.id,
            name = room.name,
            geometry = Geometry.Rect(
                length = Millimeters(lengthMm),
                width = Millimeters(widthMm)
            ),
            tags = room.tags,
            transform = SpaceTransform(
                xFeet = Millimeters((minX + maxX) / 2).toFeet(),
                zFeet = Millimeters((minY + maxY) / 2).toFeet()
            )
        )
    }

    val openingsByWall = openings.groupBy { it.wallId }
    val wallSpaces = walls.map { wall ->
        val openingModels = openingsByWall[wall.id].orEmpty().map { opening ->
            Opening(
                width = Millimeters(opening.widthMm),
                height = Millimeters(opening.heightMm),
                count = 1,
                type = opening.type,
                wallPositionT = opening.t,
                sillHeight = Millimeters(opening.sillMm),
                id = opening.id
            )
        }
        val centerX = ((wall.start.x + wall.end.x) / 2.0).roundToLong()
        val centerY = ((wall.start.y + wall.end.y) / 2.0).roundToLong()
        val yaw = Math.toDegrees(
            atan2(
                (wall.end.y - wall.start.y).toDouble(),
                (wall.end.x - wall.start.x).toDouble()
            )
        )
        Space(
            id = wall.id,
            name = "Wall",
            geometry = Geometry.Wall(
                length = Millimeters(wall.lengthMillimeters().coerceAtLeast(1L)),
                height = wall.height
            ),
            tags = wall.tags,
            openings = openingModels,
            transform = SpaceTransform(
                xFeet = Millimeters(centerX).toFeet(),
                zFeet = Millimeters(centerY).toFeet(),
                yawDegrees = yaw
            )
        )
    }

    return wallSpaces + roomSpaces
}

fun List<PointMm>.boundsOrNull(): Pair<PointMm, PointMm>? {
    if (isEmpty()) return null
    var minX = Long.MAX_VALUE
    var minY = Long.MAX_VALUE
    var maxX = Long.MIN_VALUE
    var maxY = Long.MIN_VALUE
    forEach { point ->
        minX = min(minX, point.x)
        minY = min(minY, point.y)
        maxX = max(maxX, point.x)
        maxY = max(maxY, point.y)
    }
    return PointMm(minX, minY) to PointMm(maxX, maxY)
}

/**
 * Helper functions for migrating from Space-based to BlueprintDocument-based code.
 */
fun BlueprintDocument.allElementIds(): Set<String> {
    return (walls.map { it.id } + rooms.map { it.id } + openings.map { it.id }).toSet()
}

fun BlueprintDocument.getElementById(id: String): Any? {
    return walls.find { it.id == id }
        ?: rooms.find { it.id == id }
        ?: openings.find { it.id == id }
}

fun BlueprintDocument.totalWallAreaSqFt(): Double {
    val openingAreas = openingAreaByWallId()
    return walls.sumOf { wall ->
        val wallArea = wall.lengthFeet() * Millimeters(wall.heightMm).toFeet()
        val openingArea = openingAreas[wall.id] ?: 0.0
        (wallArea - openingArea).coerceAtLeast(0.0)
    }
}

fun BlueprintDocument.totalRoomAreaSqFt(): Double {
    return rooms.sumOf { it.areaSqFt() }
}

fun BlueprintDocument.totalAreaSqFt(): Double {
    return totalWallAreaSqFt() + totalRoomAreaSqFt()
}

fun BlueprintDocument.totalOpeningCount(): Int {
    return openings.size
}

fun BlueprintDocument.elementCount(): Int {
    return walls.size + rooms.size
}
