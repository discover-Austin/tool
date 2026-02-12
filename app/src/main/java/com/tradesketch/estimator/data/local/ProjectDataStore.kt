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
    val updatedAt: Long
) {
    fun toProject() = Project(id, name, spaces.map { it.toSpace() }, createdAt, updatedAt)
    companion object {
        fun fromProject(p: Project) = ProjectJson(p.id, p.name, p.spaces.map { SpaceJson.fromSpace(it) }, p.createdAt, p.updatedAt)
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
