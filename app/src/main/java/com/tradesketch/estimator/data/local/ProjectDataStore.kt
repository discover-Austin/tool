package com.tradesketch.estimator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

private val Context.projectsDataStore: DataStore<Preferences> by preferencesDataStore(name = "projects")

@Singleton
class ProjectDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectFileStore: ProjectFileStore
) {
    private val gson = Gson()
    private val migrationMutex = Mutex()
    private val refreshSignal = MutableStateFlow(0L)
    
    companion object {
        private val PROJECTS_KEY = stringPreferencesKey("projects_json")
        private val MIGRATED_TO_FILES_KEY = booleanPreferencesKey("migrated_to_files")
    }
    
    val projects: Flow<List<Project>> = combine(
        context.projectsDataStore.data,
        refreshSignal
    ) { preferences, _ ->
        preferences
    }
        .map { preferences ->
            ensureMigrated(preferences)
            projectFileStore.loadAllProjects()
        }
    
    suspend fun saveProjects(projects: List<Project>) {
        ensureMigrated(context.projectsDataStore.data.first())
        val targetById = projects.associateBy { it.id }
        val existingIds = projectFileStore.loadAllProjects().map { it.id }.toSet()
        existingIds
            .filterNot { it in targetById.keys }
            .forEach { staleId ->
                projectFileStore.deleteProject(staleId)
            }
        projects.forEach { project ->
            projectFileStore.saveProject(project)
        }
        refreshSignal.value = System.currentTimeMillis()
    }
    
    suspend fun saveProject(project: Project) {
        ensureMigrated(context.projectsDataStore.data.first())
        projectFileStore.saveProject(
            project.copy(updatedAt = System.currentTimeMillis())
        )
        refreshSignal.value = System.currentTimeMillis()
    }
    
    suspend fun deleteProject(projectId: String) {
        ensureMigrated(context.projectsDataStore.data.first())
        projectFileStore.deleteProject(projectId)
        refreshSignal.value = System.currentTimeMillis()
    }

    private suspend fun ensureMigrated(preferencesSnapshot: Preferences) {
        if (preferencesSnapshot[MIGRATED_TO_FILES_KEY] == true) return
        migrationMutex.withLock {
            val latest = context.projectsDataStore.data.first()
            if (latest[MIGRATED_TO_FILES_KEY] == true) return@withLock

            migrateLegacyProjectsJsonToFileStore(
                projectsJson = latest[PROJECTS_KEY],
                gson = gson,
                storageEngine = projectFileStore.storageEngine()
            )

            context.projectsDataStore.edit { preferences ->
                preferences[MIGRATED_TO_FILES_KEY] = true
            }
        }
    }
}

internal data class LegacyMigrationResult(
    val migratedToFiles: Boolean,
    val writtenCount: Int
)

internal suspend fun migrateLegacyProjectsJsonToFileStore(
    projectsJson: String?,
    gson: Gson,
    storageEngine: ProjectFileStorageEngine
): LegacyMigrationResult {
    if (projectsJson.isNullOrBlank()) {
        return LegacyMigrationResult(
            migratedToFiles = true,
            writtenCount = 0
        )
    }
    val parsed = runCatching {
        val type = object : TypeToken<List<ProjectJson>>() {}.type
        gson.fromJson<List<ProjectJson>>(projectsJson, type)
    }.getOrDefault(emptyList())
    parsed.forEach { projectJson ->
        storageEngine.saveProject(projectJson.toProject())
    }
    return LegacyMigrationResult(
        migratedToFiles = true,
        writtenCount = parsed.size
    )
}

// JSON data classes for Gson serialization
private data class ProjectJson(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val spaces: List<SpaceJson>? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val takeoffSession: ProjectTakeoffSessionJson? = null,
    val blueprintDocument: BlueprintDocument? = null
) {
    fun toProject(): Project {
        val blueprint = blueprintDocument?.copy(projectId = id)
            ?: legacySpacesToBlueprint(projectId = id, spaces = spaces.orEmpty())
        return Project(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            takeoffSession = takeoffSession?.toTakeoffSession() ?: ProjectTakeoffSession(),
            blueprintDocument = blueprint
        )
    }

    companion object {
        fun fromProject(p: Project) = ProjectJson(
            schemaVersion = 2,
            id = p.id,
            name = p.name,
            spaces = null,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt,
            takeoffSession = ProjectTakeoffSessionJson.fromTakeoffSession(p.takeoffSession),
            blueprintDocument = p.blueprintDocument
        )
    }
}

private data class ProjectTakeoffSessionJson(
    val selectedScope: String,
    val selectedPlaybook: String,
    val inputMode: String? = null,
    val snapSettings: BlueprintSnapSettingsJson? = null,
    val manual: ManualTakeoffSessionParamsJson? = null,
    val drywall: DrywallSessionParamsJson,
    val concrete: ConcreteSessionParamsJson,
    val gravel: GravelSessionParamsJson,
    val paint: PaintSessionParamsJson,
    val pricing: PricingSessionParamsJson
) {
    fun toTakeoffSession() = ProjectTakeoffSession(
        selectedScope = runCatching { TakeoffScope.valueOf(selectedScope) }
            .getOrDefault(TakeoffScope.DRYWALL),
        selectedPlaybook = selectedPlaybook,
        inputMode = runCatching { TakeoffInputMode.valueOf(inputMode ?: TakeoffInputMode.BLUEPRINT.name) }
            .getOrDefault(TakeoffInputMode.BLUEPRINT),
        snapSettings = snapSettings?.toDomain() ?: BlueprintSnapSettings(),
        manual = manual?.toDomain() ?: ManualTakeoffSessionParams(),
        drywall = drywall.toDomain(),
        concrete = concrete.toDomain(),
        gravel = gravel.toDomain(),
        paint = paint.toDomain(),
        pricing = pricing.toDomain()
    )

    companion object {
        fun fromTakeoffSession(session: ProjectTakeoffSession) = ProjectTakeoffSessionJson(
            selectedScope = session.selectedScope.name,
            selectedPlaybook = session.selectedPlaybook,
            inputMode = session.inputMode.name,
            snapSettings = BlueprintSnapSettingsJson.fromDomain(session.snapSettings),
            manual = ManualTakeoffSessionParamsJson.fromDomain(session.manual),
            drywall = DrywallSessionParamsJson.fromDomain(session.drywall),
            concrete = ConcreteSessionParamsJson.fromDomain(session.concrete),
            gravel = GravelSessionParamsJson.fromDomain(session.gravel),
            paint = PaintSessionParamsJson.fromDomain(session.paint),
            pricing = PricingSessionParamsJson.fromDomain(session.pricing)
        )
    }
}

private data class BlueprintSnapSettingsJson(
    val gridEnabled: Boolean,
    val endpointEnabled: Boolean,
    val midpointEnabled: Boolean,
    val angleEnabled: Boolean,
    val closureEnabled: Boolean,
    val gridStepFeet: Double,
    val angleIncrementDegrees: Int,
    val thresholdFeet: Double
) {
    fun toDomain() = BlueprintSnapSettings(
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
        fun fromDomain(settings: BlueprintSnapSettings) = BlueprintSnapSettingsJson(
            gridEnabled = settings.gridEnabled,
            endpointEnabled = settings.endpointEnabled,
            midpointEnabled = settings.midpointEnabled,
            angleEnabled = settings.angleEnabled,
            closureEnabled = settings.closureEnabled,
            gridStepFeet = settings.gridStepFeet,
            angleIncrementDegrees = settings.angleIncrementDegrees,
            thresholdFeet = settings.thresholdFeet
        )
    }
}

private data class DrywallSessionParamsJson(
    val sheetAreaSqFt: Double,
    val wastePercent: Double,
    val screwsPerSheet: Int,
    val mudGallonsPer100SqFt: Double,
    val includeCeilings: Boolean? = null
) {
    fun toDomain() = DrywallSessionParams(
        sheetAreaSqFt = sheetAreaSqFt,
        wastePercent = wastePercent,
        screwsPerSheet = screwsPerSheet,
        mudGallonsPer100SqFt = mudGallonsPer100SqFt,
        includeCeilings = includeCeilings ?: true
    )

    companion object {
        fun fromDomain(params: DrywallSessionParams) = DrywallSessionParamsJson(
            sheetAreaSqFt = params.sheetAreaSqFt,
            wastePercent = params.wastePercent,
            screwsPerSheet = params.screwsPerSheet,
            mudGallonsPer100SqFt = params.mudGallonsPer100SqFt,
            includeCeilings = params.includeCeilings
        )
    }
}

private data class ConcreteSessionParamsJson(
    val thicknessFeet: Double,
    val wastePercent: Double
) {
    fun toDomain() = ConcreteSessionParams(
        thicknessFeet = thicknessFeet,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(params: ConcreteSessionParams) = ConcreteSessionParamsJson(
            thicknessFeet = params.thicknessFeet,
            wastePercent = params.wastePercent
        )
    }
}

private data class GravelSessionParamsJson(
    val depthFeet: Double,
    val densityTonsPerYard: Double,
    val wastePercent: Double
) {
    fun toDomain() = GravelSessionParams(
        depthFeet = depthFeet,
        densityTonsPerYard = densityTonsPerYard,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(params: GravelSessionParams) = GravelSessionParamsJson(
            depthFeet = params.depthFeet,
            densityTonsPerYard = params.densityTonsPerYard,
            wastePercent = params.wastePercent
        )
    }
}

private data class PaintSessionParamsJson(
    val coverageSqFtPerGallon: Double,
    val coats: Int,
    val wastePercent: Double
) {
    fun toDomain() = PaintSessionParams(
        coverageSqFtPerGallon = coverageSqFtPerGallon,
        coats = coats,
        wastePercent = wastePercent
    )

    companion object {
        fun fromDomain(params: PaintSessionParams) = PaintSessionParamsJson(
            coverageSqFtPerGallon = params.coverageSqFtPerGallon,
            coats = params.coats,
            wastePercent = params.wastePercent
        )
    }
}

private data class PricingSessionParamsJson(
    val drywallSheetCost: Double,
    val drywallScrewCost: Double,
    val drywallMudCost: Double,
    val concreteYardCost: Double,
    val gravelYardCost: Double,
    val gravelTonCost: Double,
    val paintGallonCost: Double,
    val laborPercent: Double,
    val markupPercent: Double,
    val taxPercent: Double
) {
    fun toDomain() = PricingSessionParams(
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
        fun fromDomain(params: PricingSessionParams) = PricingSessionParamsJson(
            drywallSheetCost = params.drywallSheetCost,
            drywallScrewCost = params.drywallScrewCost,
            drywallMudCost = params.drywallMudCost,
            concreteYardCost = params.concreteYardCost,
            gravelYardCost = params.gravelYardCost,
            gravelTonCost = params.gravelTonCost,
            paintGallonCost = params.paintGallonCost,
            laborPercent = params.laborPercent,
            markupPercent = params.markupPercent,
            taxPercent = params.taxPercent
        )
    }
}

private data class SpaceJson(
    val id: String,
    val name: String,
    val geometry: GeometryJson,
    val tags: List<String> = emptyList(),
    val openings: List<OpeningJson> = emptyList(),
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

private data class RectJson(val length: Long, val width: Long)

private data class OpeningJson(
    val width: Long,
    val height: Long,
    val count: Int,
    val type: String? = null,
    val wallPositionT: Double? = null,
    val sillHeight: Long? = null,
    val id: String? = null
)

private data class SpaceTransformJson(
    val xFeet: Double,
    val yFeet: Double,
    val zFeet: Double,
    val yawDegrees: Double,
    val colorHex: Long
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
    val transform = transform ?: SpaceTransformJson(
        xFeet = 0.0,
        yFeet = 0.0,
        zFeet = 0.0,
        yawDegrees = 0.0,
        colorHex = 0xFF4E79A7
    )
    val lengthMm = geometry.length.coerceAtLeast(1L)
    val centerXmm = Millimeters.fromFeet(transform.xFeet).value
    val centerYmm = Millimeters.fromFeet(transform.zFeet).value
    val halfLengthFeet = Millimeters(lengthMm).toFeet() / 2.0
    val yawRadians = Math.toRadians(transform.yawDegrees)
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
    val transform = transform ?: SpaceTransformJson(
        xFeet = 0.0,
        yFeet = 0.0,
        zFeet = 0.0,
        yawDegrees = 0.0,
        colorHex = 0xFF4E79A7
    )
    val centerXmm = Millimeters.fromFeet(transform.xFeet).value
    val centerYmm = Millimeters.fromFeet(transform.zFeet).value
    return when (geometry.type) {
        "Rect", "Slab" -> rectanglePolygon(
            centerX = centerXmm,
            centerY = centerYmm,
            lengthMm = geometry.length.coerceAtLeast(1L),
            widthMm = (geometry.width ?: geometry.length).coerceAtLeast(1L),
            yawDegrees = transform.yawDegrees
        )
        "LShape" -> {
            val rectA = geometry.rectA ?: return null
            val rectB = geometry.rectB ?: return null
            rectanglePolygon(
                centerX = centerXmm,
                centerY = centerYmm,
                lengthMm = maxOf(rectA.length, rectB.length).coerceAtLeast(1L),
                widthMm = maxOf(rectA.width, rectB.width).coerceAtLeast(1L),
                yawDegrees = transform.yawDegrees
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

private data class ManualTakeoffSessionParamsJson(
    val drywallWallAreaSqFt: Double = 0.0,
    val drywallCeilingAreaSqFt: Double = 0.0,
    val concreteAreaSqFt: Double = 0.0,
    val gravelAreaSqFt: Double = 0.0,
    val paintAreaSqFt: Double = 0.0
) {
    fun toDomain() = ManualTakeoffSessionParams(
        drywallWallAreaSqFt = drywallWallAreaSqFt,
        drywallCeilingAreaSqFt = drywallCeilingAreaSqFt,
        concreteAreaSqFt = concreteAreaSqFt,
        gravelAreaSqFt = gravelAreaSqFt,
        paintAreaSqFt = paintAreaSqFt
    )

    companion object {
        fun fromDomain(params: ManualTakeoffSessionParams) = ManualTakeoffSessionParamsJson(
            drywallWallAreaSqFt = params.drywallWallAreaSqFt,
            drywallCeilingAreaSqFt = params.drywallCeilingAreaSqFt,
            concreteAreaSqFt = params.concreteAreaSqFt,
            gravelAreaSqFt = params.gravelAreaSqFt,
            paintAreaSqFt = params.paintAreaSqFt
        )
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
