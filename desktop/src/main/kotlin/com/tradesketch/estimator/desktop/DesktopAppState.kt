package com.tradesketch.estimator.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.CostingInputs
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.utils.ExportFormatter
import com.tradesketch.estimator.utils.Validators

enum class DesktopTakeoffType(val label: String) {
    DRYWALL("Drywall"),
    CONCRETE("Concrete"),
    GRAVEL_MULCH("Gravel / Mulch"),
    PAINT("Paint")
}

data class DrywallParams(
    val sheetAreaSqFt: Double = 32.0,
    val wastePercent: Double = 10.0,
    val screwsPerSheet: Int = 32,
    val mudGallonsPer100SqFt: Double = 0.5
)

data class ConcreteParams(
    val thicknessFeet: Double = 0.33,
    val wastePercent: Double = 5.0
)

data class GravelParams(
    val depthFeet: Double = 0.25,
    val densityTonsPerYard: Double = 1.4,
    val wastePercent: Double = 10.0
)

data class PaintParams(
    val coverageSqFtPerGallon: Double = 350.0,
    val coats: Int = 2,
    val wastePercent: Double = 5.0
)

enum class WorkspaceTab(val label: String) {
    BLUEPRINT("Blueprint"),
    MATERIALS("Materials"),
    QUANTITIES("Quantities"),
    ADDONS("Add-ons"),
    REVIEW("Review"),
    EXPORT("Export"),
    SETTINGS_ABOUT("Settings/About")
}

class DesktopAppState(
    private val storage: DesktopStorage = DesktopStorage()
) {
    private val blueprintUndo = ArrayDeque<BlueprintDocument>()
    private val blueprintRedo = ArrayDeque<BlueprintDocument>()

    var projects by mutableStateOf<List<Project>>(emptyList())
        private set

    var selectedProjectId by mutableStateOf<String?>(null)
        private set

    var settings by mutableStateOf(Settings.DEFAULT)
        private set

    var selectedTakeoffType by mutableStateOf(DesktopTakeoffType.DRYWALL)
        private set

    var activeTab by mutableStateOf(WorkspaceTab.BLUEPRINT)

    var drywallParams by mutableStateOf(DrywallParams())
        private set

    var concreteParams by mutableStateOf(ConcreteParams())
        private set

    var gravelParams by mutableStateOf(GravelParams())
        private set

    var paintParams by mutableStateOf(PaintParams())
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val selectedProject: Project?
        get() = projects.firstOrNull { it.id == selectedProjectId }

    val currentTakeoffResult: TakeoffResult?
        get() = calculateTakeoff()

    val canUndoBlueprint: Boolean
        get() = blueprintUndo.isNotEmpty()

    val canRedoBlueprint: Boolean
        get() = blueprintRedo.isNotEmpty()

    init {
        reloadFromDisk()
    }

    fun reloadFromDisk() {
        settings = storage.loadSettings()
        projects = storage.loadProjects().sortedByDescending { it.updatedAt }
        selectedProjectId = selectedProjectId?.takeIf { id -> projects.any { it.id == id } }
            ?: projects.firstOrNull()?.id
        statusMessage = "Loaded ${projects.size} project(s)"
        errorMessage = null
    }

    fun selectProject(projectId: String) {
        if (projects.any { it.id == projectId }) {
            selectedProjectId = projectId
            blueprintUndo.clear()
            blueprintRedo.clear()
            errorMessage = null
        }
    }

    fun createProjectFromTemplate(template: ProjectTemplate, customName: String? = null) {
        val projectName = customName?.trim().takeUnless { it.isNullOrBlank() } ?: template.displayName()
        if (!Validators.isValidProjectName(projectName)) {
            errorMessage = "Project name must be 1-100 characters."
            return
        }
        val created = template.createProject(projectName)
        projects = (projects + created).sortedByDescending { it.updatedAt }
        selectedProjectId = created.id
        persistProjects("Created ${template.displayName()} project")
    }

    fun deleteSelectedProject() {
        val target = selectedProject ?: return
        projects = projects.filterNot { it.id == target.id }
        selectedProjectId = projects.firstOrNull()?.id
        persistProjects("Deleted ${target.name}")
    }

    fun renameSelectedProject(newName: String) {
        val trimmed = newName.trim()
        if (!Validators.isValidProjectName(trimmed)) {
            errorMessage = "Project name must be 1-100 characters."
            return
        }
        updateSelectedProject {
            it.copy(
                name = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        }
        persistProjects("Project renamed")
    }

    fun updateBlueprintDocument(updated: BlueprintDocument, trackHistory: Boolean = true) {
        val project = selectedProject ?: return
        if (trackHistory) {
            blueprintUndo.addLast(project.authoritativeBlueprint())
            if (blueprintUndo.size > 120) {
                blueprintUndo.removeFirst()
            }
            blueprintRedo.clear()
        }
        updateSelectedProject {
            it.copy(
                blueprintDocument = updated.copy(projectId = it.id),
                updatedAt = System.currentTimeMillis()
            )
        }
        persistProjects("Blueprint updated")
    }

    fun undoBlueprint() {
        val project = selectedProject ?: return
        if (blueprintUndo.isEmpty()) return
        val previous = blueprintUndo.removeLast()
        blueprintRedo.addLast(project.authoritativeBlueprint())
        updateBlueprintDocument(previous, trackHistory = false)
    }

    fun redoBlueprint() {
        val project = selectedProject ?: return
        if (blueprintRedo.isEmpty()) return
        val next = blueprintRedo.removeLast()
        blueprintUndo.addLast(project.authoritativeBlueprint())
        updateBlueprintDocument(next, trackHistory = false)
    }

    fun selectTakeoffType(type: DesktopTakeoffType) {
        selectedTakeoffType = type
        errorMessage = null
    }

    fun updateDrywallParams(
        sheetAreaSqFt: Double? = null,
        wastePercent: Double? = null,
        screwsPerSheet: Int? = null,
        mudGallonsPer100SqFt: Double? = null
    ) {
        drywallParams = drywallParams.copy(
            sheetAreaSqFt = sheetAreaSqFt ?: drywallParams.sheetAreaSqFt,
            wastePercent = wastePercent ?: drywallParams.wastePercent,
            screwsPerSheet = screwsPerSheet ?: drywallParams.screwsPerSheet,
            mudGallonsPer100SqFt = mudGallonsPer100SqFt ?: drywallParams.mudGallonsPer100SqFt
        )
    }

    fun updateConcreteParams(
        thicknessFeet: Double? = null,
        wastePercent: Double? = null
    ) {
        concreteParams = concreteParams.copy(
            thicknessFeet = thicknessFeet ?: concreteParams.thicknessFeet,
            wastePercent = wastePercent ?: concreteParams.wastePercent
        )
    }

    fun updateGravelParams(
        depthFeet: Double? = null,
        densityTonsPerYard: Double? = null,
        wastePercent: Double? = null
    ) {
        gravelParams = gravelParams.copy(
            depthFeet = depthFeet ?: gravelParams.depthFeet,
            densityTonsPerYard = densityTonsPerYard ?: gravelParams.densityTonsPerYard,
            wastePercent = wastePercent ?: gravelParams.wastePercent
        )
    }

    fun updatePaintParams(
        coverageSqFtPerGallon: Double? = null,
        coats: Int? = null,
        wastePercent: Double? = null
    ) {
        paintParams = paintParams.copy(
            coverageSqFtPerGallon = coverageSqFtPerGallon ?: paintParams.coverageSqFtPerGallon,
            coats = coats ?: paintParams.coats,
            wastePercent = wastePercent ?: paintParams.wastePercent
        )
    }

    fun updateSettings(update: (Settings) -> Settings) {
        settings = update(settings)
        persistSettings("Settings updated")
    }

    fun resetSettings() {
        settings = Settings.DEFAULT
        persistSettings("Settings reset")
    }

    fun completeOnboardingRitual(projectName: String, type: DesktopTakeoffType) {
        val normalizedName = projectName.trim()
        if (!Validators.isValidProjectName(normalizedName)) {
            errorMessage = "Project name must be 1-100 characters."
            return
        }
        val template = when (type) {
            DesktopTakeoffType.DRYWALL -> ProjectTemplate.BEDROOM
            DesktopTakeoffType.CONCRETE -> ProjectTemplate.GARAGE
            DesktopTakeoffType.GRAVEL_MULCH -> ProjectTemplate.YARD_BED
            DesktopTakeoffType.PAINT -> ProjectTemplate.BEDROOM
        }
        val created = template.createProject(normalizedName)
        projects = (projects + created).sortedByDescending { it.updatedAt }
        selectedProjectId = created.id
        selectedTakeoffType = type
        settings = settings.copy(
            primaryTrade = type.toPrimaryTrade(),
            firstRun = false,
            hasCompletedTradeOnboarding = true
        )
        activeTab = WorkspaceTab.BLUEPRINT
        persistProjects("Created ${created.name}")
        persistSettings("Welcome complete")
    }

    fun clearMessages() {
        statusMessage = null
        errorMessage = null
    }

    fun exportSummary(): String {
        val project = selectedProject ?: return ""
        val result = currentTakeoffResult ?: return ""
        return ExportFormatter.formatAsSummary(
            project = project,
            settings = settings,
            takeoffType = selectedTakeoffType.label,
            result = result
        )
    }

    fun exportTextReport(): String {
        val project = selectedProject ?: return ""
        val result = currentTakeoffResult ?: return ""
        return ExportFormatter.formatAsText(
            project = project,
            settings = settings,
            takeoffType = selectedTakeoffType.label,
            result = result
        )
    }

    fun exportCsv(): String {
        val project = selectedProject ?: return ""
        val result = currentTakeoffResult ?: return ""
        return ExportFormatter.formatAsCSV(
            project = project,
            settings = settings,
            takeoffType = selectedTakeoffType.label,
            result = result
        )
    }

    fun exportJson(): String {
        val project = selectedProject ?: return ""
        val result = currentTakeoffResult ?: return ""
        return ExportFormatter.formatAsJson(
            project = project,
            settings = settings,
            takeoffType = selectedTakeoffType.label,
            result = result
        )
    }

    private fun calculateTakeoff(): TakeoffResult? {
        val project = selectedProject ?: return null
        val blueprint = project.authoritativeBlueprint()
        return runCatching {
            when (selectedTakeoffType) {
                DesktopTakeoffType.DRYWALL -> {
                    BlueprintTakeoffCalculator.drywallTakeoff(
                        document = blueprint,
                        sheetAreaSqFt = drywallParams.sheetAreaSqFt,
                        wastePercent = drywallParams.wastePercent,
                        screwsPerSheet = drywallParams.screwsPerSheet,
                        mudGallonsPer100SqFt = drywallParams.mudGallonsPer100SqFt,
                        includeCeilings = true,
                        costing = costingInputsFor(selectedTakeoffType)
                    )
                }
                DesktopTakeoffType.CONCRETE -> {
                    BlueprintTakeoffCalculator.concreteTakeoff(
                        document = blueprint,
                        thicknessFeet = concreteParams.thicknessFeet,
                        wastePercent = concreteParams.wastePercent,
                        costing = costingInputsFor(selectedTakeoffType)
                    )
                }
                DesktopTakeoffType.GRAVEL_MULCH -> {
                    BlueprintTakeoffCalculator.gravelMulchTakeoff(
                        document = blueprint,
                        depthFeet = gravelParams.depthFeet,
                        densityTonsPerYard = gravelParams.densityTonsPerYard,
                        wastePercent = gravelParams.wastePercent,
                        costing = costingInputsFor(selectedTakeoffType)
                    )
                }
                DesktopTakeoffType.PAINT -> {
                    BlueprintTakeoffCalculator.paintTakeoff(
                        document = blueprint,
                        coverageSqFtPerGallon = paintParams.coverageSqFtPerGallon,
                        coats = paintParams.coats,
                        wastePercent = paintParams.wastePercent,
                        costing = costingInputsFor(selectedTakeoffType)
                    )
                }
            }
        }.onFailure {
            errorMessage = "Takeoff failed: ${it.message}"
        }.getOrNull()
    }

    private fun costingInputsFor(type: DesktopTakeoffType): CostingInputs {
        val lineCosts = when (type) {
            DesktopTakeoffType.DRYWALL -> mapOf(
                "Drywall sheets" to settings.drywallSheetUnitCost,
                "Drywall screws" to settings.drywallScrewUnitCost,
                "Joint compound" to settings.drywallMudUnitCost
            )
            DesktopTakeoffType.CONCRETE -> mapOf(
                "Concrete volume" to settings.concreteYardUnitCost
            )
            DesktopTakeoffType.GRAVEL_MULCH -> mapOf(
                "Material volume" to settings.gravelYardUnitCost,
                "Material weight" to settings.gravelTonUnitCost
            )
            DesktopTakeoffType.PAINT -> mapOf(
                "Paint" to settings.paintGallonUnitCost
            )
        }
        return CostingInputs(
            unitCostByLineName = lineCosts,
            laborPercent = settings.laborPercent,
            markupPercent = settings.markupPercent,
            taxPercent = settings.taxPercent
        )
    }

    private fun updateSelectedProject(update: (Project) -> Project) {
        val projectId = selectedProjectId ?: return
        var updated = false
        projects = projects.map { project ->
            if (project.id == projectId) {
                updated = true
                update(project)
            } else {
                project
            }
        }.sortedByDescending { it.updatedAt }

        if (!updated) {
            errorMessage = "Project not found."
        }
    }

    private fun persistProjects(successMessage: String) {
        runCatching {
            storage.saveProjects(projects)
        }.onSuccess {
            statusMessage = successMessage
            errorMessage = null
        }.onFailure {
            errorMessage = "Unable to save projects: ${it.message}"
        }
    }

    private fun persistSettings(successMessage: String) {
        runCatching {
            storage.saveSettings(settings)
        }.onSuccess {
            statusMessage = successMessage
            errorMessage = null
        }.onFailure {
            errorMessage = "Unable to save settings: ${it.message}"
        }
    }
}

private fun DesktopTakeoffType.toPrimaryTrade(): PrimaryTrade {
    return when (this) {
        DesktopTakeoffType.DRYWALL -> PrimaryTrade.DRYWALL
        DesktopTakeoffType.CONCRETE -> PrimaryTrade.CONCRETE
        DesktopTakeoffType.GRAVEL_MULCH -> PrimaryTrade.GRAVEL_MULCH
        DesktopTakeoffType.PAINT -> PrimaryTrade.PAINT
    }
}
