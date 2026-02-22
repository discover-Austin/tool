package com.tradesketch.estimator.desktop

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PaintSessionParams
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.PricingSessionParams
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.DrywallSessionParams
import com.tradesketch.estimator.domain.model.ConcreteSessionParams
import com.tradesketch.estimator.domain.model.GravelSessionParams
import com.tradesketch.estimator.domain.model.WallSegment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

class DesktopStorage(
    private val baseDir: Path = Paths.get(
        System.getProperty("user.home"),
        ".tradesketch-estimator"
    )
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val projectsFile: Path = baseDir.resolve("projects.json")
    private val settingsFile: Path = baseDir.resolve("settings.json")

    @Synchronized
    fun loadProjects(): List<Project> {
        return runCatching {
            if (!Files.exists(projectsFile)) {
                return emptyList()
            }
            val content = Files.readString(projectsFile)
            if (content.isBlank()) {
                return emptyList()
            }

            val type = object : TypeToken<List<ProjectJson>>() {}.type
            val parsed = gson.fromJson<List<ProjectJson>>(content, type) ?: emptyList()
            parsed.map { it.toProject() }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveProjects(projects: List<Project>) {
        ensureBaseDir()
        val json = gson.toJson(projects.map { ProjectJson.fromProject(it) })
        Files.writeString(projectsFile, json, CREATE, TRUNCATE_EXISTING, WRITE)
    }

    @Synchronized
    fun loadSettings(): Settings {
        return runCatching {
            if (!Files.exists(settingsFile)) {
                return Settings.DEFAULT
            }
            val content = Files.readString(settingsFile)
            if (content.isBlank()) {
                return Settings.DEFAULT
            }
            gson.fromJson(content, Settings::class.java) ?: Settings.DEFAULT
        }.getOrElse { Settings.DEFAULT }
    }

    @Synchronized
    fun saveSettings(settings: Settings) {
        ensureBaseDir()
        val json = gson.toJson(settings)
        Files.writeString(settingsFile, json, CREATE, TRUNCATE_EXISTING, WRITE)
    }

    private fun ensureBaseDir() {
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir)
        }
    }
}

private data class ProjectJson(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val spaces: List<SpaceJson>? = null,
    val takeoffSession: ProjectTakeoffSessionJson? = null,
    val blueprintDocument: BlueprintDocument? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toProject(): Project {
        val blueprint = blueprintDocument?.copy(projectId = id)
            ?: legacySpacesToBlueprint(projectId = id, spaces = spaces.orEmpty())
        return Project(
            id = id,
            name = name,
            takeoffSession = takeoffSession?.toTakeoffSession() ?: ProjectTakeoffSession(),
            blueprintDocument = blueprint,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromProject(project: Project): ProjectJson = ProjectJson(
            schemaVersion = 2,
            id = project.id,
            name = project.name,
            spaces = null,
            takeoffSession = ProjectTakeoffSessionJson.fromTakeoffSession(project.takeoffSession),
            blueprintDocument = project.blueprintDocument,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt
        )
    }
}

private data class SpaceJson(
    val id: String,
    val name: String,
    val geometry: GeometryJson,
    val tags: List<String> = emptyList(),
    val openings: List<OpeningJson>,
    val transform: SpaceTransformJson? = null
)

private data class GeometryJson(
    val type: String,
    val length: Long = 0,
    val width: Long? = null,
    val height: Long? = null,
    val thickness: Long? = null,
    val radius: Long? = null,
    val rectA: RectJson? = null,
    val rectB: RectJson? = null
)

private data class RectJson(
    val length: Long,
    val width: Long
)

private data class SpaceTransformJson(
    val xFeet: Double = 0.0,
    val yFeet: Double = 0.0,
    val zFeet: Double = 0.0,
    val yawDegrees: Double = 0.0,
    val colorHex: Long = 0xFF4E79A7
)

private data class ProjectTakeoffSessionJson(
    val selectedScope: String = TakeoffScope.DRYWALL.name,
    val selectedPlaybook: String = "BALANCED",
    val snapSettings: BlueprintSnapSettingsJson? = null,
    val drywall: DrywallSessionParamsJson? = null,
    val concrete: ConcreteSessionParamsJson? = null,
    val gravel: GravelSessionParamsJson? = null,
    val paint: PaintSessionParamsJson? = null,
    val pricing: PricingSessionParamsJson? = null
) {
    fun toTakeoffSession(): ProjectTakeoffSession {
        return ProjectTakeoffSession(
            selectedScope = runCatching { TakeoffScope.valueOf(selectedScope) }
                .getOrElse { TakeoffScope.DRYWALL },
            selectedPlaybook = selectedPlaybook,
            snapSettings = snapSettings?.toDomain() ?: BlueprintSnapSettings(),
            drywall = drywall?.toDomain() ?: DrywallSessionParams(),
            concrete = concrete?.toDomain() ?: ConcreteSessionParams(),
            gravel = gravel?.toDomain() ?: GravelSessionParams(),
            paint = paint?.toDomain() ?: PaintSessionParams(),
            pricing = pricing?.toDomain() ?: PricingSessionParams()
        )
    }

    companion object {
        fun fromTakeoffSession(session: ProjectTakeoffSession): ProjectTakeoffSessionJson =
            ProjectTakeoffSessionJson(
                selectedScope = session.selectedScope.name,
                selectedPlaybook = session.selectedPlaybook,
                snapSettings = BlueprintSnapSettingsJson.fromDomain(session.snapSettings),
                drywall = DrywallSessionParamsJson.fromDomain(session.drywall),
                concrete = ConcreteSessionParamsJson.fromDomain(session.concrete),
                gravel = GravelSessionParamsJson.fromDomain(session.gravel),
                paint = PaintSessionParamsJson.fromDomain(session.paint),
                pricing = PricingSessionParamsJson.fromDomain(session.pricing)
            )
    }
}

private data class BlueprintSnapSettingsJson(
    val gridEnabled: Boolean = true,
    val endpointEnabled: Boolean = true,
    val midpointEnabled: Boolean = true,
    val angleEnabled: Boolean = true,
    val closureEnabled: Boolean = true,
    val gridStepFeet: Double = 1.0,
    val angleIncrementDegrees: Int = 15,
    val thresholdFeet: Double = 0.75
) {
    fun toDomain(): BlueprintSnapSettings = BlueprintSnapSettings(
        gridEnabled = gridEnabled,
        endpointEnabled = endpointEnabled,
        midpointEnabled = midpointEnabled,
        angleEnabled = angleEnabled,
        closureEnabled = closureEnabled,
        gridStepFeet = gridStepFeet,
        angleIncrementDegrees = angleIncrementDegrees,
        thresholdFeet = thresholdFeet
    )

    companion object {
        fun fromDomain(domain: BlueprintSnapSettings): BlueprintSnapSettingsJson =
            BlueprintSnapSettingsJson(
                gridEnabled = domain.gridEnabled,
                endpointEnabled = domain.endpointEnabled,
                midpointEnabled = domain.midpointEnabled,
                angleEnabled = domain.angleEnabled,
                closureEnabled = domain.closureEnabled,
                gridStepFeet = domain.gridStepFeet,
                angleIncrementDegrees = domain.angleIncrementDegrees,
                thresholdFeet = domain.thresholdFeet
            )
    }
}

private data class DrywallSessionParamsJson(
    val sheetAreaSqFt: Double = 32.0,
    val wastePercent: Double = 10.0,
    val screwsPerSheet: Int = 32,
    val mudGallonsPer100SqFt: Double = 0.5,
    val includeCeilings: Boolean = true
) {
    fun toDomain(): DrywallSessionParams = DrywallSessionParams(
        sheetAreaSqFt = sheetAreaSqFt,
        wastePercent = wastePercent,
        screwsPerSheet = screwsPerSheet,
        mudGallonsPer100SqFt = mudGallonsPer100SqFt,
        includeCeilings = includeCeilings
    )

    companion object {
        fun fromDomain(domain: DrywallSessionParams): DrywallSessionParamsJson =
            DrywallSessionParamsJson(
                sheetAreaSqFt = domain.sheetAreaSqFt,
                wastePercent = domain.wastePercent,
                screwsPerSheet = domain.screwsPerSheet,
                mudGallonsPer100SqFt = domain.mudGallonsPer100SqFt,
                includeCeilings = domain.includeCeilings
            )
    }
}

private data class ConcreteSessionParamsJson(
    val thicknessFeet: Double = 0.33,
    val wastePercent: Double = 5.0
) {
    fun toDomain(): ConcreteSessionParams = ConcreteSessionParams(
        thicknessFeet = thicknessFeet,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(domain: ConcreteSessionParams): ConcreteSessionParamsJson =
            ConcreteSessionParamsJson(
                thicknessFeet = domain.thicknessFeet,
                wastePercent = domain.wastePercent
            )
    }
}

private data class GravelSessionParamsJson(
    val depthFeet: Double = 0.25,
    val densityTonsPerYard: Double = 1.4,
    val wastePercent: Double = 10.0
) {
    fun toDomain(): GravelSessionParams = GravelSessionParams(
        depthFeet = depthFeet,
        densityTonsPerYard = densityTonsPerYard,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(domain: GravelSessionParams): GravelSessionParamsJson =
            GravelSessionParamsJson(
                depthFeet = domain.depthFeet,
                densityTonsPerYard = domain.densityTonsPerYard,
                wastePercent = domain.wastePercent
            )
    }
}

private data class PaintSessionParamsJson(
    val coverageSqFtPerGallon: Double = 350.0,
    val coats: Int = 2,
    val wastePercent: Double = 5.0
) {
    fun toDomain(): PaintSessionParams = PaintSessionParams(
        coverageSqFtPerGallon = coverageSqFtPerGallon,
        coats = coats,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(domain: PaintSessionParams): PaintSessionParamsJson =
            PaintSessionParamsJson(
                coverageSqFtPerGallon = domain.coverageSqFtPerGallon,
                coats = domain.coats,
                wastePercent = domain.wastePercent
            )
    }
}

private data class PricingSessionParamsJson(
    val drywallSheetCost: Double = 17.5,
    val drywallScrewCost: Double = 0.01,
    val drywallMudCost: Double = 9.5,
    val concreteYardCost: Double = 165.0,
    val gravelYardCost: Double = 52.0,
    val gravelTonCost: Double = 36.0,
    val paintGallonCost: Double = 38.0,
    val laborPercent: Double = 20.0,
    val markupPercent: Double = 15.0,
    val taxPercent: Double = 8.0
) {
    fun toDomain(): PricingSessionParams = PricingSessionParams(
        drywallSheetCost = drywallSheetCost,
        drywallScrewCost = drywallScrewCost,
        drywallMudCost = drywallMudCost,
        concreteYardCost = concreteYardCost,
        gravelYardCost = gravelYardCost,
        gravelTonCost = gravelTonCost,
        paintGallonCost = paintGallonCost,
        laborPercent = laborPercent,
        markupPercent = markupPercent,
        taxPercent = taxPercent
    )

    companion object {
        fun fromDomain(domain: PricingSessionParams): PricingSessionParamsJson =
            PricingSessionParamsJson(
                drywallSheetCost = domain.drywallSheetCost,
                drywallScrewCost = domain.drywallScrewCost,
                drywallMudCost = domain.drywallMudCost,
                concreteYardCost = domain.concreteYardCost,
                gravelYardCost = domain.gravelYardCost,
                gravelTonCost = domain.gravelTonCost,
                paintGallonCost = domain.paintGallonCost,
                laborPercent = domain.laborPercent,
                markupPercent = domain.markupPercent,
                taxPercent = domain.taxPercent
            )
    }
}

private data class OpeningJson(
    val width: Long,
    val height: Long,
    val count: Int,
    val type: String? = null,
    val wallPositionT: Double? = null,
    val sillHeight: Long? = null,
    val id: String? = null
)

private fun legacySpacesToBlueprint(
    projectId: String,
    spaces: List<SpaceJson>
): BlueprintDocument {
    if (spaces.isEmpty()) return BlueprintDocument.empty(projectId)
    val defaultParams = BlueprintParams()
    val walls = mutableListOf<WallSegment>()
    val openings = mutableListOf<BlueprintOpening>()
    val rooms = mutableListOf<Room>()

    spaces.forEach { space ->
        val wall = space.toLegacyWallSegmentOrNull(defaultWallHeightMm = defaultParams.wallHeightMm)
            ?: return@forEach
        walls += wall
        space.openings.forEachIndexed { openingIndex, opening ->
            val count = opening.count.coerceAtLeast(1)
            repeat(count) { copyIndex ->
                val inferredType = opening.type
                    ?.let { raw -> runCatching { OpeningType.valueOf(raw.uppercase()) }.getOrNull() }
                    ?: if (Millimeters(opening.height.coerceAtLeast(1L)).toFeet() >= 6.0) {
                        OpeningType.DOOR
                    } else {
                        OpeningType.WINDOW
                    }
                openings += BlueprintOpening(
                    id = opening.id
                        ?.takeIf { it.isNotBlank() }
                        ?: "${wall.id}-opening-${openingIndex + 1}-${copyIndex + 1}",
                    wallId = wall.id,
                    t = opening.wallPositionT ?: 0.5,
                    widthMm = opening.width.coerceAtLeast(1L),
                    heightMm = opening.height.coerceAtLeast(1L),
                    sillMm = (opening.sillHeight ?: 0L).coerceAtLeast(0L),
                    type = inferredType
                ).normalized()
            }
        }
    }

    spaces.forEachIndexed { index, space ->
        val polygon = space.toLegacyRoomPolygonOrNull() ?: return@forEachIndexed
        if (polygon.size >= 3) {
            rooms += Room(
                id = space.id.ifBlank { "room-${index + 1}" },
                name = space.name.ifBlank { "Room ${rooms.size + 1}" },
                polygon = polygon,
                tags = space.normalizedTags()
            )
        }
    }

    val resolvedRooms = if (rooms.isNotEmpty()) rooms else RoomLoopDetector.detectRooms(walls)
    return BlueprintDocument(
        projectId = projectId,
        params = defaultParams,
        walls = walls,
        rooms = resolvedRooms,
        openings = openings
    )
}

private fun SpaceJson.normalizedTags(): Set<String> {
    return tags.map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()
}

private fun SpaceJson.toLegacyWallSegmentOrNull(defaultWallHeightMm: Long): WallSegment? {
    if (geometry.type != "Wall") return null
    val localTransform = transform ?: SpaceTransformJson()
    val lengthMm = geometry.length.coerceAtLeast(1L)
    val centerXmm = Millimeters.fromFeet(localTransform.xFeet).value
    val centerYmm = Millimeters.fromFeet(localTransform.zFeet).value
    val halfLengthFeet = Millimeters(lengthMm).toFeet() / 2.0
    val yawRadians = Math.toRadians(localTransform.yawDegrees)
    val dxMm = Millimeters.fromFeet(cos(yawRadians) * halfLengthFeet).value
    val dyMm = Millimeters.fromFeet(sin(yawRadians) * halfLengthFeet).value
    return WallSegment(
        id = id.ifBlank { UUID.randomUUID().toString() },
        start = PointMm(centerXmm - dxMm, centerYmm - dyMm),
        end = PointMm(centerXmm + dxMm, centerYmm + dyMm),
        height = Millimeters((geometry.height ?: defaultWallHeightMm).coerceAtLeast(1L)),
        tags = normalizedTags()
    )
}

private fun SpaceJson.toLegacyRoomPolygonOrNull(): List<PointMm>? {
    val localTransform = transform ?: SpaceTransformJson()
    val centerXmm = Millimeters.fromFeet(localTransform.xFeet).value
    val centerYmm = Millimeters.fromFeet(localTransform.zFeet).value
    return when (geometry.type) {
        "Rect", "Slab" -> rectanglePolygon(
            centerX = centerXmm,
            centerY = centerYmm,
            lengthMm = geometry.length.coerceAtLeast(1L),
            widthMm = (geometry.width ?: geometry.length).coerceAtLeast(1L),
            yawDegrees = localTransform.yawDegrees
        )
        "LShape" -> {
            val rectA = geometry.rectA ?: return null
            val rectB = geometry.rectB ?: return null
            rectanglePolygon(
                centerX = centerXmm,
                centerY = centerYmm,
                lengthMm = maxOf(rectA.length, rectB.length).coerceAtLeast(1L),
                widthMm = maxOf(rectA.width, rectB.width).coerceAtLeast(1L),
                yawDegrees = localTransform.yawDegrees
            )
        }
        "Circle" -> {
            val radius = geometry.radius?.coerceAtLeast(1L) ?: return null
            List(16) { index ->
                val radians = (2.0 * PI * index.toDouble()) / 16.0
                PointMm(
                    x = centerXmm + (cos(radians) * radius.toDouble()).roundToLong(),
                    y = centerYmm + (sin(radians) * radius.toDouble()).roundToLong()
                )
            }
        }
        else -> null
    }
}

private fun rectanglePolygon(
    centerX: Long,
    centerY: Long,
    lengthMm: Long,
    widthMm: Long,
    yawDegrees: Double
): List<PointMm> {
    val halfLength = lengthMm / 2.0
    val halfWidth = widthMm / 2.0
    val radians = Math.toRadians(yawDegrees)
    val localCorners = listOf(
        Pair(-halfLength, -halfWidth),
        Pair(halfLength, -halfWidth),
        Pair(halfLength, halfWidth),
        Pair(-halfLength, halfWidth)
    )
    return localCorners.map { (lx, ly) ->
        val rotatedX = (lx * cos(radians)) - (ly * sin(radians))
        val rotatedY = (lx * sin(radians)) + (ly * cos(radians))
        PointMm(
            x = centerX + rotatedX.roundToLong(),
            y = centerY + rotatedY.roundToLong()
        )
    }
}
