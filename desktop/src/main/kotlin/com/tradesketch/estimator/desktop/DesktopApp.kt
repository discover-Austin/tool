package com.tradesketch.estimator.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.domain.model.elementCount
import com.tradesketch.estimator.domain.model.totalAreaSqFt
import com.tradesketch.estimator.utils.ExportFormatter
import com.tradesketch.estimator.utils.Formatters
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser

private enum class ExportViewMode(val label: String) {
    SUMMARY("Summary"),
    REPORT("Full Report"),
    CSV("CSV"),
    JSON("JSON")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSketchDesktopApp() {
    val state = remember { DesktopAppState() }
    var exportViewMode by remember { mutableStateOf(ExportViewMode.SUMMARY) }
    var showWelcomeDetail by remember { mutableStateOf(false) }
    var ritualProjectName by remember { mutableStateOf("My First Project") }
    var ritualType by remember { mutableStateOf(DesktopTakeoffType.DRYWALL) }

    MaterialTheme {
        if (state.settings.firstRun) {
            DesktopWelcomeScreen(
                showDetail = showWelcomeDetail,
                onToggleDetail = { showWelcomeDetail = it },
                projectName = ritualProjectName,
                onProjectNameChange = { ritualProjectName = it },
                selectedType = ritualType,
                onSelectType = { ritualType = it },
                onBegin = { name, type ->
                    state.completeOnboardingRitual(projectName = name, type = type)
                    showWelcomeDetail = false
                    state.activeTab = WorkspaceTab.BLUEPRINT
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                DesktopWorkspaceRail(
                    activeTab = state.activeTab,
                    onSelectTab = { state.activeTab = it },
                    modifier = Modifier.fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(10.dp))
                VerticalDivider()
                Spacer(modifier = Modifier.width(10.dp))
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        WorkspaceHeader(state = state)
                        when (state.activeTab) {
                            WorkspaceTab.BLUEPRINT,
                            WorkspaceTab.ADDONS -> DesktopBlueprintTab(
                                state = state,
                                openAddonsByDefault = state.activeTab == WorkspaceTab.ADDONS
                            )
                            WorkspaceTab.MATERIALS,
                            WorkspaceTab.QUANTITIES -> TakeoffTab(state = state)
                            WorkspaceTab.REVIEW -> ModelTab(state = state)
                            WorkspaceTab.EXPORT -> ExportTab(
                                state = state,
                                exportViewMode = exportViewMode,
                                onExportViewModeChange = { exportViewMode = it }
                            )
                            WorkspaceTab.SETTINGS_ABOUT -> SettingsTab(state = state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopWelcomeScreen(
    showDetail: Boolean,
    onToggleDetail: (Boolean) -> Unit,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    selectedType: DesktopTakeoffType,
    onSelectType: (DesktopTakeoffType) -> Unit,
    onBegin: (String, DesktopTakeoffType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = showDetail,
            transitionSpec = {
                slideInHorizontally { fullWidth -> fullWidth / 4 } + fadeIn() togetherWith
                    slideOutHorizontally { fullWidth -> -fullWidth / 4 } + fadeOut()
            },
            label = "desktop_welcome"
        ) { detailVisible ->
            if (!detailVisible) {
                Card(
                    modifier = Modifier.width(520.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Architecture,
                            contentDescription = null
                        )
                        Text(
                            text = "TradeSketch Desktop",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Blueprint-driven estimating for walls, slabs, rooms, openings, and exports.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { onToggleDetail(true) }) {
                            Text("Begin")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.width(620.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Welcome",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Start with project naming, choose what you are estimating, then draft geometry that drives quantities.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = projectName,
                            onValueChange = onProjectNameChange,
                            label = { Text("Project name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesktopTakeoffType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { onSelectType(type) },
                                    label = { Text(type.label) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onToggleDetail(false) }) {
                                Text("Back")
                            }
                            Button(onClick = { onBegin(projectName, selectedType) }) {
                                Text("Open Project")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DesktopWorkspaceRail(
    activeTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var hoverLabel by remember { mutableStateOf<String?>(null) }
    NavigationRail(
        modifier = modifier.width(60.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
        header = {
            hoverLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    ) {
        WorkspaceTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.pointerMoveFilter(
                    onEnter = {
                        hoverLabel = tab.label
                        false
                    },
                    onExit = {
                        hoverLabel = null
                        false
                    }
                ),
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
private fun ProjectsSidebar(
    state: DesktopAppState,
    modifier: Modifier = Modifier
) {
    var templateMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "TradeSketch Desktop",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { templateMenuExpanded = true }
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Project")
                }
                DropdownMenu(
                    expanded = templateMenuExpanded,
                    onDismissRequest = { templateMenuExpanded = false }
                ) {
                    ProjectTemplate.entries.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.displayName()) },
                            onClick = {
                                state.createProjectFromTemplate(template)
                                templateMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { state.reloadFromDisk() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reload")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.projects.isEmpty()) {
                EmptyStateCard(
                    title = "No projects yet",
                    subtitle = "Create one from a template to get started."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.projects, key = { it.id }) { project ->
                        val selected = project.id == state.selectedProjectId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            onClick = { state.selectProject(project.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${project.blueprintDocument.elementCount()} elements",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatDateTime(project.updatedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { state.deleteSelectedProject() },
                enabled = state.selectedProject != null
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Selected")
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(state: DesktopAppState) {
    val status = state.statusMessage
    val error = state.errorMessage
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Text(
            text = state.selectedProject?.name ?: "No project selected",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Desktop estimator",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!status.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModelTab(state: DesktopAppState) {
    val project = state.selectedProject
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateCard("No project selected", "Choose a project on the left.")
        }
        return
    }

    var renameDraft by remember(project.id, project.name) { mutableStateOf(project.name) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Project Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it },
                        label = { Text("Project name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { state.renameSelectedProject(renameDraft) }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Name")
                    }
                }
            }
        }

        item {
            SnapshotCard(project = project)
        }

        if (project.blueprintDocument.elementCount() == 0 && project.blueprintDocument.openings.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No blueprint elements in this project",
                    subtitle = "Use the Blueprint tab to add walls, rooms, and openings."
                )
            }
        } else {
            items(project.blueprintDocument.walls, key = { it.id }) { wall ->
                WallElementCard(wall = wall)
            }
            items(project.blueprintDocument.rooms, key = { it.id }) { room ->
                RoomElementCard(room = room)
            }
            items(project.blueprintDocument.openings, key = { it.id }) { opening ->
                OpeningElementCard(opening = opening)
            }
        }
    }
}

@Composable
private fun SnapshotCard(project: Project) {
    val blueprint = project.authoritativeBlueprint()
    val totalArea = blueprint.totalAreaSqFt()
    val elementCount = blueprint.elementCount() + blueprint.openings.size
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Project Snapshot", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Elements: $elementCount")
            Text("Walls: ${blueprint.walls.size}  Rooms: ${blueprint.rooms.size}  Openings: ${blueprint.openings.size}")
            Text("Total area: ${Formatters.formatArea(totalArea)}")
            Text(
                text = "Updated: ${Formatters.formatDateTime(project.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WallElementCard(wall: WallSegment) {
    val lengthFt = Millimeters(wall.lengthMillimeters()).toFeet()
    val heightFt = Millimeters(wall.heightMm).toFeet()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Wall ${wall.id}", style = MaterialTheme.typography.titleMedium)
            Text("Length: ${"%.1f".format(lengthFt)} ft", style = MaterialTheme.typography.bodySmall)
            Text("Height: ${"%.1f".format(heightFt)} ft", style = MaterialTheme.typography.bodySmall)
            Text("Type: ${wall.type.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoomElementCard(room: Room) {
    val area = room.areaSqFt()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(room.name.ifBlank { "Room ${room.id}" }, style = MaterialTheme.typography.titleMedium)
            Text("Area: ${Formatters.formatArea(area)}", style = MaterialTheme.typography.bodySmall)
            Text("Vertices: ${room.polygon.size}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (room.ceiling.enabled) "Ceiling included" else "Ceiling excluded",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OpeningElementCard(opening: BlueprintOpening) {
    val widthFt = Millimeters(opening.widthMm).toFeet()
    val heightFt = Millimeters(opening.heightMm).toFeet()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (opening.type == OpeningType.DOOR) "Door ${opening.id}" else "Window ${opening.id}",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Wall: ${opening.wallId}", style = MaterialTheme.typography.bodySmall)
            Text("Size: ${"%.1f".format(widthFt)} ft x ${"%.1f".format(heightFt)} ft", style = MaterialTheme.typography.bodySmall)
            Text("Position: ${"%.0f".format(opening.t * 100)}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TakeoffTab(state: DesktopAppState) {
    val project = state.selectedProject
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateCard("No project selected", "Choose a project to calculate takeoffs.")
        }
        return
    }

    val result = state.currentTakeoffResult

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Takeoff Type", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopTakeoffType.entries.forEach { type ->
                            FilterChip(
                                selected = state.selectedTakeoffType == type,
                                onClick = { state.selectTakeoffType(type) },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            when (state.selectedTakeoffType) {
                DesktopTakeoffType.DRYWALL -> DrywallParametersCard(state)
                DesktopTakeoffType.CONCRETE -> ConcreteParametersCard(state)
                DesktopTakeoffType.GRAVEL_MULCH -> GravelParametersCard(state)
                DesktopTakeoffType.PAINT -> PaintParametersCard(state)
            }
        }

        if (result != null) {
            item {
                ResultsCard(result = result)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = "Estimate only. Verify quantities and pricing with real site conditions before purchasing materials.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DrywallParametersCard(state: DesktopAppState) {
    val params = state.drywallParams
    ParametersCard(title = "Drywall Parameters") {
        NumberField("Sheet area (sq ft)", params.sheetAreaSqFt.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateDrywallParams(sheetAreaSqFt = value) }
        }
        NumberField("Waste (%)", params.wastePercent.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateDrywallParams(wastePercent = value) }
        }
        NumberField("Screws per sheet", params.screwsPerSheet.toString(), KeyboardType.Number) {
            it.toIntOrNull()?.let { value -> state.updateDrywallParams(screwsPerSheet = value) }
        }
        NumberField("Mud gal / 100 sq ft", params.mudGallonsPer100SqFt.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateDrywallParams(mudGallonsPer100SqFt = value) }
        }
    }
}

@Composable
private fun ConcreteParametersCard(state: DesktopAppState) {
    val params = state.concreteParams
    ParametersCard(title = "Concrete Parameters") {
        NumberField("Thickness (ft)", params.thicknessFeet.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateConcreteParams(thicknessFeet = value) }
        }
        NumberField("Waste (%)", params.wastePercent.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateConcreteParams(wastePercent = value) }
        }
    }
}

@Composable
private fun GravelParametersCard(state: DesktopAppState) {
    val params = state.gravelParams
    ParametersCard(title = "Gravel / Mulch Parameters") {
        NumberField("Depth (ft)", params.depthFeet.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateGravelParams(depthFeet = value) }
        }
        NumberField("Density (tons / yard)", params.densityTonsPerYard.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateGravelParams(densityTonsPerYard = value) }
        }
        NumberField("Waste (%)", params.wastePercent.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updateGravelParams(wastePercent = value) }
        }
    }
}

@Composable
private fun PaintParametersCard(state: DesktopAppState) {
    val params = state.paintParams
    ParametersCard(title = "Paint Parameters") {
        NumberField("Coverage (sq ft / gallon)", params.coverageSqFtPerGallon.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updatePaintParams(coverageSqFtPerGallon = value) }
        }
        NumberField("Coats", params.coats.toString(), KeyboardType.Number) {
            it.toIntOrNull()?.let { value -> state.updatePaintParams(coats = value) }
        }
        NumberField("Waste (%)", params.wastePercent.toString()) {
            it.toDoubleOrNull()?.let { value -> state.updatePaintParams(wastePercent = value) }
        }
    }
}

@Composable
private fun ParametersCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ResultsCard(result: com.tradesketch.estimator.domain.model.TakeoffResult) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Results", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            result.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name)
                    Text("${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                }
                val unitCost = item.unitCost
                if (unitCost != null) {
                    Text(
                        text = "@ ${Formatters.formatQuantity(unitCost)} each",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                item.extendedCost?.let { ext ->
                    Text(
                        text = "Line total: ${Formatters.formatQuantity(ext)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            result.materialSubtotal?.let {
                Text("Material subtotal: ${Formatters.formatQuantity(it)}", style = MaterialTheme.typography.bodySmall)
            }
            result.laborCost?.let {
                Text("Labor: ${Formatters.formatQuantity(it)}", style = MaterialTheme.typography.bodySmall)
            }
            result.markupCost?.let {
                Text("Markup: ${Formatters.formatQuantity(it)}", style = MaterialTheme.typography.bodySmall)
            }
            result.taxCost?.let {
                Text("Tax: ${Formatters.formatQuantity(it)}", style = MaterialTheme.typography.bodySmall)
            }
            result.totalCost?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total: ${Formatters.formatQuantity(it)}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ExportTab(
    state: DesktopAppState,
    exportViewMode: ExportViewMode,
    onExportViewModeChange: (ExportViewMode) -> Unit
) {
    val project = state.selectedProject
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateCard("No project selected", "Choose a project to export.")
        }
        return
    }

    val takeoff = state.currentTakeoffResult
    if (takeoff == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateCard("No takeoff result yet", "Use the Takeoff tab before exporting.")
        }
        return
    }

    val content = when (exportViewMode) {
        ExportViewMode.SUMMARY -> state.exportSummary()
        ExportViewMode.REPORT -> state.exportTextReport()
        ExportViewMode.CSV -> state.exportCsv()
        ExportViewMode.JSON -> state.exportJson()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = exportViewMode == mode,
                    onClick = { onExportViewModeChange(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { copyToClipboard(content) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy")
            }
            OutlinedButton(onClick = { copyToClipboard(state.exportTextReport()) }) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Full Report")
            }
            OutlinedButton(
                onClick = {
                    saveWithDesktopChooser(
                        suggestedName = "tradesketch_export.${if (exportViewMode == ExportViewMode.JSON) "json" else if (exportViewMode == ExportViewMode.CSV) "csv" else "txt"}",
                        bytes = content.toByteArray()
                    )
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save File")
            }
            OutlinedButton(
                onClick = {
                    val pdfBytes = buildSimplePdfBytes(
                        project = project,
                        takeoff = takeoff,
                        title = "${project.name} ${state.selectedTakeoffType.label} Export",
                        body = state.exportTextReport()
                    )
                    saveWithDesktopChooser(
                        suggestedName = "tradesketch_export.pdf",
                        bytes = pdfBytes
                    )
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save PDF")
            }
        }

        OutlinedTextField(
            value = content,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
            label = { Text("${exportViewMode.label} Output") }
        )
    }
}

@Composable
private fun SettingsTab(state: DesktopAppState) {
    var defaultWasteDraft by remember(state.settings.defaultWastePercent) {
        mutableStateOf(state.settings.defaultWastePercent.toString())
    }
    var laborDraft by remember(state.settings.laborPercent) {
        mutableStateOf(state.settings.laborPercent.toString())
    }
    var markupDraft by remember(state.settings.markupPercent) {
        mutableStateOf(state.settings.markupPercent.toString())
    }
    var taxDraft by remember(state.settings.taxPercent) {
        mutableStateOf(state.settings.taxPercent.toString())
    }

    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Global Defaults", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = defaultWasteDraft,
                    onValueChange = { defaultWasteDraft = it },
                    label = { Text("Default waste (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        defaultWasteDraft.toDoubleOrNull()?.let { waste ->
                            state.updateSettings { it.copy(defaultWastePercent = waste) }
                        }
                    }
                ) {
                    Text("Save Defaults")
                }
            }
        }

        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Costing Multipliers", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = laborDraft,
                    onValueChange = { laborDraft = it },
                    label = { Text("Labor (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = markupDraft,
                    onValueChange = { markupDraft = it },
                    label = { Text("Markup (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxDraft,
                    onValueChange = { taxDraft = it },
                    label = { Text("Tax (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val labor = laborDraft.toDoubleOrNull() ?: return@Button
                        val markup = markupDraft.toDoubleOrNull() ?: return@Button
                        val tax = taxDraft.toDoubleOrNull() ?: return@Button
                        state.updateSettings {
                            it.copy(
                                laborPercent = labor,
                                markupPercent = markup,
                                taxPercent = tax
                            )
                        }
                    }
                ) {
                    Text("Save Costing")
                }
            }
        }

        OutlinedButton(onClick = { state.resetSettings() }) {
            Text("Reset Settings")
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun WorkspaceTab.icon() = when (this) {
    WorkspaceTab.BLUEPRINT -> Icons.Default.AutoFixHigh
    WorkspaceTab.MATERIALS -> Icons.Default.Assessment
    WorkspaceTab.QUANTITIES -> Icons.Default.Straighten
    WorkspaceTab.ADDONS -> Icons.Default.Add
    WorkspaceTab.REVIEW -> Icons.Default.Description
    WorkspaceTab.EXPORT -> Icons.Default.Share
    WorkspaceTab.SETTINGS_ABOUT -> Icons.Default.Tune
}

private fun copyToClipboard(value: String) {
    val selection = StringSelection(value)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
}

private fun saveWithDesktopChooser(
    suggestedName: String,
    bytes: ByteArray
) {
    val chooser = JFileChooser().apply {
        selectedFile = File(suggestedName)
    }
    val result = chooser.showSaveDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        runCatching {
            chooser.selectedFile.writeBytes(bytes)
        }
    }
}

private fun buildSimplePdfBytes(
    project: Project,
    takeoff: com.tradesketch.estimator.domain.model.TakeoffResult,
    title: String,
    body: String
): ByteArray {
    val blueprint = project.authoritativeBlueprint()
    val stream = buildString {
        fun drawText(x: Float, y: Float, size: Int, text: String) {
            append("BT /F1 $size Tf ${x.toPdf()} ${y.toPdf()} Td (${sanitizePdfText(text)}) Tj ET\n")
        }

        drawText(50f, 770f, 14, title)
        drawText(50f, 752f, 10, "Project: ${project.name}")
        drawText(50f, 738f, 10, "Generated: ${Formatters.formatDate(System.currentTimeMillis())}")

        // Blueprint snapshot frame.
        append("0.6 w 50 450 512 250 re S\n")
        drawText(55f, 690f, 10, "Blueprint Snapshot")

        if (blueprint.walls.isNotEmpty()) {
            val minX = blueprint.walls.minOf { minOf(it.start.x, it.end.x) }
            val maxX = blueprint.walls.maxOf { maxOf(it.start.x, it.end.x) }
            val minY = blueprint.walls.minOf { minOf(it.start.y, it.end.y) }
            val maxY = blueprint.walls.maxOf { maxOf(it.start.y, it.end.y) }
            val spanX = (maxX - minX).toDouble().coerceAtLeast(1.0)
            val spanY = (maxY - minY).toDouble().coerceAtLeast(1.0)
            val boxLeft = 62.0
            val boxBottom = 462.0
            val boxWidth = 488.0
            val boxHeight = 224.0
            val scale = minOf(boxWidth / spanX, boxHeight / spanY)

            fun mapX(value: Long): Double = boxLeft + ((value - minX) * scale)
            fun mapY(value: Long): Double = boxBottom + ((value - minY) * scale)

            append("0.3 0.5 0.8 RG 1.2 w\n")
            blueprint.walls.forEach { wall ->
                val x1 = mapX(wall.start.x)
                val y1 = mapY(wall.start.y)
                val x2 = mapX(wall.end.x)
                val y2 = mapY(wall.end.y)
                append("${x1.toPdf()} ${y1.toPdf()} m ${x2.toPdf()} ${y2.toPdf()} l S\n")
            }
            append("0 0 0 RG\n")
        }

        drawText(50f, 428f, 11, "Itemized Materials")
        var itemY = 412f
        takeoff.items.forEach { item ->
            val line = "${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}"
            wrapPdfText(line, maxChars = 88).forEach { wrapped ->
                if (itemY < 122f) return@forEach
                drawText(55f, itemY, 10, wrapped)
                itemY -= 12f
            }
            if (itemY < 122f) return@forEach
        }

        var summaryY = itemY - 6f
        takeoff.totalCost?.let { total ->
            drawText(55f, summaryY, 10, "Total: ${Formatters.formatMoney(total)}")
            summaryY -= 12f
        }
        takeoff.materialSubtotal?.let { subtotal ->
            drawText(55f, summaryY, 10, "Materials: ${Formatters.formatMoney(subtotal)}")
            summaryY -= 12f
        }

        // Disclaimer block.
        val disclaimerStart = summaryY.coerceAtLeast(70f)
        drawText(50f, disclaimerStart, 9, "Disclaimer")
        var disclaimerY = disclaimerStart - 12f
        wrapPdfText(ExportFormatter.getDisclaimer(), maxChars = 102).forEach { wrapped ->
            if (disclaimerY < 40f) return@forEach
            drawText(50f, disclaimerY, 8, wrapped)
            disclaimerY -= 10f
        }

        // Keep a short text payload from the full report for traceability.
        var traceY = disclaimerY - 6f
        body.lines().take(2).forEach { line ->
            if (traceY < 24f) return@forEach
            drawText(50f, traceY, 7, line)
            traceY -= 9f
        }
    }

    val objects = mutableListOf<String>()
    objects += "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj"
    objects += "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj"
    objects += "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >> endobj"
    objects += "4 0 obj << /Length ${stream.toByteArray().size} >> stream\n$stream\nendstream endobj"
    objects += "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj"

    val builder = StringBuilder()
    builder.append("%PDF-1.4\n")
    val offsets = mutableListOf(0)
    objects.forEach { obj ->
        offsets += builder.toString().toByteArray().size
        builder.append(obj).append('\n')
    }
    val xrefOffset = builder.toString().toByteArray().size
    builder.append("xref\n0 ${objects.size + 1}\n")
    builder.append("0000000000 65535 f \n")
    offsets.drop(1).forEach { offset ->
        builder.append(offset.toString().padStart(10, '0')).append(" 00000 n \n")
    }
    builder.append("trailer << /Size ${objects.size + 1} /Root 1 0 R >>\n")
    builder.append("startxref\n$xrefOffset\n%%EOF")
    return builder.toString().toByteArray()
}

private fun Double.toPdf(): String = "%.2f".format(this)

private fun Float.toPdf(): String = "%.2f".format(this)

private fun wrapPdfText(text: String, maxChars: Int): List<String> {
    if (text.length <= maxChars) return listOf(text)
    val words = text.split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (candidate.length <= maxChars) {
            current = candidate
        } else {
            if (current.isNotBlank()) lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    return lines
}

private fun sanitizePdfText(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
}
