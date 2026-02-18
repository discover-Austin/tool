package com.tradesketch.estimator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.projectsDataStore: DataStore<Preferences> by preferencesDataStore(name = "projects")

@Singleton
class ProjectDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    
    companion object {
        private val PROJECTS_KEY = stringPreferencesKey("projects_json")
    }
    
    val projects: Flow<List<Project>> = context.projectsDataStore.data
        .map { preferences ->
            val projectsJson = preferences[PROJECTS_KEY] ?: return@map emptyList()
            try {
                val type = object : TypeToken<List<ProjectJson>>() {}.type
                val projectJsonList: List<ProjectJson> = gson.fromJson(projectsJson, type)
                projectJsonList.map { it.toProject() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    suspend fun saveProjects(projects: List<Project>) {
        context.projectsDataStore.edit { preferences ->
            val projectJsonList = projects.map { ProjectJson.fromProject(it) }
            preferences[PROJECTS_KEY] = gson.toJson(projectJsonList)
        }
    }
    
    suspend fun saveProject(project: Project) {
        val currentProjects = projects.first().toMutableList()
        val index = currentProjects.indexOfFirst { it.id == project.id }
        if (index != -1) {
            currentProjects[index] = project.copy(updatedAt = System.currentTimeMillis())
        } else {
            currentProjects.add(project)
        }
        saveProjects(currentProjects)
    }
    
    suspend fun deleteProject(projectId: String) {
        val currentProjects = projects.first().toMutableList()
        currentProjects.removeAll { it.id == projectId }
        saveProjects(currentProjects)
    }
}

// JSON data classes for Gson serialization
private data class ProjectJson(
    val id: String,
    val name: String,
    val spaces: List<SpaceJson>,
    val createdAt: Long,
    val updatedAt: Long,
    val takeoffSession: ProjectTakeoffSessionJson? = null
) {
    fun toProject() = Project(
        id = id,
        name = name,
        spaces = spaces.map { it.toSpace() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        takeoffSession = takeoffSession?.toTakeoffSession() ?: ProjectTakeoffSession()
    )

    companion object {
        fun fromProject(p: Project) = ProjectJson(
            id = p.id,
            name = p.name,
            spaces = p.spaces.map { SpaceJson.fromSpace(it) },
            createdAt = p.createdAt,
            updatedAt = p.updatedAt,
            takeoffSession = ProjectTakeoffSessionJson.fromTakeoffSession(p.takeoffSession)
        )
    }
}

private data class ProjectTakeoffSessionJson(
    val selectedScope: String,
    val selectedPlaybook: String,
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
            drywall = DrywallSessionParamsJson.fromDomain(session.drywall),
            concrete = ConcreteSessionParamsJson.fromDomain(session.concrete),
            gravel = GravelSessionParamsJson.fromDomain(session.gravel),
            paint = PaintSessionParamsJson.fromDomain(session.paint),
            pricing = PricingSessionParamsJson.fromDomain(session.pricing)
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
    val openings: List<OpeningJson> = emptyList(),
    val transform: SpaceTransformJson? = null
) {
    fun toSpace() = Space(
        id = id,
        name = name,
        geometry = geometry.toGeometry(),
        openings = openings.map { it.toOpening() },
        transform = transform?.toSpaceTransform() ?: SpaceTransform()
    )
    companion object {
        fun fromSpace(s: Space) = SpaceJson(
            id = s.id,
            name = s.name,
            geometry = GeometryJson.fromGeometry(s.geometry),
            openings = s.openings.map { OpeningJson.fromOpening(it) },
            transform = SpaceTransformJson.fromTransform(s.transform)
        )
    }
}

private data class GeometryJson(
    val type: String,
    val length: Long = 0,
    val width: Long? = null,
    val height: Long? = null,
    val thickness: Long? = null,
    val radius: Long? = null,
    val rectA: RectJson? = null,
    val rectB: RectJson? = null
) {
    fun toGeometry(): Geometry = when (type) {
        "Rect" -> Geometry.Rect(Millimeters(length), Millimeters(width!!))
        "Wall" -> Geometry.Wall(Millimeters(length), Millimeters(height!!))
        "Slab" -> Geometry.Slab(Millimeters(length), Millimeters(width!!), Millimeters(thickness!!))
        "Circle" -> Geometry.Circle(Millimeters(radius!!))
        "LShape" -> Geometry.LShape(rectA!!.toRect(), rectB!!.toRect())
        else -> throw IllegalArgumentException("Unknown type: $type")
    }
    
    companion object {
        fun fromGeometry(g: Geometry): GeometryJson = when (g) {
            is Geometry.Rect -> GeometryJson("Rect", g.length.value, g.width.value)
            is Geometry.Wall -> GeometryJson("Wall", g.length.value, height = g.height.value)
            is Geometry.Slab -> GeometryJson("Slab", g.length.value, g.width.value, thickness = g.thickness.value)
            is Geometry.Circle -> GeometryJson("Circle", radius = g.radius.value)
            is Geometry.LShape -> GeometryJson("LShape", rectA = RectJson.fromRect(g.rectA), rectB = RectJson.fromRect(g.rectB))
        }
    }
}

private data class RectJson(val length: Long, val width: Long) {
    fun toRect() = Geometry.Rect(Millimeters(length), Millimeters(width))
    companion object {
        fun fromRect(r: Geometry.Rect) = RectJson(r.length.value, r.width.value)
    }
}

private data class OpeningJson(val width: Long, val height: Long, val count: Int) {
    fun toOpening() = Opening(Millimeters(width), Millimeters(height), count)
    companion object {
        fun fromOpening(o: Opening) = OpeningJson(o.width.value, o.height.value, o.count)
    }
}

private data class SpaceTransformJson(
    val xFeet: Double,
    val yFeet: Double,
    val zFeet: Double,
    val yawDegrees: Double,
    val colorHex: Long
) {
    fun toSpaceTransform() = SpaceTransform(
        xFeet = xFeet,
        yFeet = yFeet,
        zFeet = zFeet,
        yawDegrees = yawDegrees,
        colorHex = colorHex
    )

    companion object {
        fun fromTransform(transform: SpaceTransform) = SpaceTransformJson(
            xFeet = transform.xFeet,
            yFeet = transform.yFeet,
            zFeet = transform.zFeet,
            yawDegrees = transform.yawDegrees,
            colorHex = transform.colorHex
        )
    }
}
