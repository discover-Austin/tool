package com.tradesketch.estimator.desktop

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Opening
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.Space
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE

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
    val id: String,
    val name: String,
    val spaces: List<SpaceJson>,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toProject(): Project = Project(
        id = id,
        name = name,
        spaces = spaces.map { it.toSpace() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromProject(project: Project): ProjectJson = ProjectJson(
            id = project.id,
            name = project.name,
            spaces = project.spaces.map { SpaceJson.fromSpace(it) },
            createdAt = project.createdAt,
            updatedAt = project.updatedAt
        )
    }
}

private data class SpaceJson(
    val id: String,
    val name: String,
    val geometry: GeometryJson,
    val openings: List<OpeningJson>
) {
    fun toSpace(): Space = Space(
        id = id,
        name = name,
        geometry = geometry.toGeometry(),
        openings = openings.map { it.toOpening() }
    )

    companion object {
        fun fromSpace(space: Space): SpaceJson = SpaceJson(
            id = space.id,
            name = space.name,
            geometry = GeometryJson.fromGeometry(space.geometry),
            openings = space.openings.map { OpeningJson.fromOpening(it) }
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
        else -> throw IllegalArgumentException("Unknown geometry type: $type")
    }

    companion object {
        fun fromGeometry(geometry: Geometry): GeometryJson = when (geometry) {
            is Geometry.Rect -> GeometryJson("Rect", geometry.length.value, geometry.width.value)
            is Geometry.Wall -> GeometryJson("Wall", geometry.length.value, height = geometry.height.value)
            is Geometry.Slab -> GeometryJson(
                "Slab",
                geometry.length.value,
                geometry.width.value,
                thickness = geometry.thickness.value
            )
            is Geometry.Circle -> GeometryJson("Circle", radius = geometry.radius.value)
            is Geometry.LShape -> GeometryJson(
                "LShape",
                rectA = RectJson.fromRect(geometry.rectA),
                rectB = RectJson.fromRect(geometry.rectB)
            )
        }
    }
}

private data class RectJson(
    val length: Long,
    val width: Long
) {
    fun toRect(): Geometry.Rect = Geometry.Rect(Millimeters(length), Millimeters(width))

    companion object {
        fun fromRect(rect: Geometry.Rect): RectJson = RectJson(rect.length.value, rect.width.value)
    }
}

private data class OpeningJson(
    val width: Long,
    val height: Long,
    val count: Int
) {
    fun toOpening(): Opening = Opening(
        width = Millimeters(width),
        height = Millimeters(height),
        count = count
    )

    companion object {
        fun fromOpening(opening: Opening): OpeningJson = OpeningJson(
            width = opening.width.value,
            height = opening.height.value,
            count = opening.count
        )
    }
}
