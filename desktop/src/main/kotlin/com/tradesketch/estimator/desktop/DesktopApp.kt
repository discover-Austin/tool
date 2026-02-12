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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import com.tradesketch.estimator.domain.model.volumeCuFt
import com.tradesketch.estimator.utils.Formatters
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private enum class ExportViewMode(val label: String) {
    SUMMARY("Summary"),
    REPORT("Full Report"),
    CSV("CSV")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSketchDesktopApp() {
    val state = remember { DesktopAppState() }
    var showSpaceEditor by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<Space?>(null) }
    var exportViewMode by remember { mutableStateOf(ExportViewMode.SUMMARY) }

    MaterialTheme {
        Scaffold { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(14.dp)
            ) {
                ProjectsSidebar(
                    state = state,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(14.dp))

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        WorkspaceHeader(state = state)
                        TabRow(selectedTabIndex = state.activeTab.ordinal) {
                            WorkspaceTab.entries.forEach { tab ->
                                Tab(
                                    selected = state.activeTab == tab,
                                    onClick = { state.activeTab = tab },
                                    text = { Text(tab.label) }
                                )
                            }
                        }

                        when (state.activeTab) {
                            WorkspaceTab.MODEL -> ModelTab(
                                state = state,
                                onAddSpace = {
                                    editingSpace = null
                                    showSpaceEditor = true
                                },
                                onEditSpace = {
                                    editingSpace = it
                                    showSpaceEditor = true
                                }
                            )
                            WorkspaceTab.TAKEOFF -> TakeoffTab(state = state)
                            WorkspaceTab.EXPORT -> ExportTab(
                                state = state,
                                exportViewMode = exportViewMode,
                                onExportViewModeChange = { exportViewMode = it }
                            )
                            WorkspaceTab.SETTINGS -> SettingsTab(state = state)
                        }
                    }
                }
            }
        }
    }

    if (showSpaceEditor && state.selectedProject != null) {
        DesktopSpaceEditorDialog(
            initialSpace = editingSpace,
            onDismiss = {
                showSpaceEditor = false
                editingSpace = null
            },
            onSave = { space ->
                state.saveSpace(space)
                showSpaceEditor = false
                editingSpace = null
            }
        )
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
                                    text = "${project.spaces.size} spaces",
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
            text = "Desktop estimator workspace",
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
private fun ModelTab(
    state: DesktopAppState,
    onAddSpace: () -> Unit,
    onEditSpace: (Space) -> Unit
) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { state.renameSelectedProject(renameDraft) }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Name")
                        }
                        OutlinedButton(onClick = onAddSpace) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Space")
                        }
                    }
                }
            }
        }

        item {
            SnapshotCard(project = project)
        }

        if (project.spaces.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No spaces in this project",
                    subtitle = "Add a wall, room, slab, circle, or L-shape."
                )
            }
        } else {
            items(project.spaces, key = { it.id }) { space ->
                SpaceCard(
                    space = space,
                    onEdit = { onEditSpace(space) },
                    onDuplicate = { state.duplicateSpace(space.id) },
                    onDelete = { state.deleteSpace(space.id) }
                )
            }
        }
    }
}

@Composable
private fun SnapshotCard(project: Project) {
    val totalArea = project.spaces.sumOf { it.geometry.areaSqFt() }
    val totalVolume = project.spaces.sumOf { it.geometry.volumeCuFt() }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Project Snapshot", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Spaces: ${project.spaces.size}")
            Text("Total area: ${Formatters.formatArea(totalArea)}")
            Text("Total volume: ${Formatters.formatQuantity(totalVolume)} cu ft")
            Text(
                text = "Updated: ${Formatters.formatDateTime(project.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpaceCard(
    space: Space,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val area = space.geometry.areaSqFt()
    val volume = space.geometry.volumeCuFt()
    val openingArea = space.openingsAreaSqFt()
    val netArea = (area - openingArea).coerceAtLeast(0.0)

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(space.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = describeGeometry(space.geometry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    TextButton(onClick = onDuplicate) {
                        Text("Duplicate")
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Area: ${Formatters.formatArea(area)}", style = MaterialTheme.typography.bodySmall)
            if (openingArea > 0.0) {
                Text(
                    "Net after openings: ${Formatters.formatArea(netArea)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (volume > 0.0) {
                Text("Volume: ${Formatters.formatQuantity(volume)} cu ft", style = MaterialTheme.typography.bodySmall)
            }
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
                if (item.unitCost != null) {
                    Text(
                        text = "@ ${Formatters.formatQuantity(item.unitCost)} each",
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

private fun describeGeometry(geometry: Geometry): String {
    return when (geometry) {
        is Geometry.Wall -> "Wall ${Formatters.formatDimension(geometry.length)} x ${Formatters.formatDimension(geometry.height)}"
        is Geometry.Rect -> "Room ${Formatters.formatDimension(geometry.length)} x ${Formatters.formatDimension(geometry.width)}"
        is Geometry.Slab -> {
            "Slab ${Formatters.formatDimension(geometry.length)} x " +
                "${Formatters.formatDimension(geometry.width)} x " +
                "${Formatters.formatDimension(geometry.thickness)}"
        }
        is Geometry.Circle -> "Circle radius ${Formatters.formatDimension(geometry.radius)}"
        is Geometry.LShape -> {
            "L-Shape (${Formatters.formatDimension(geometry.rectA.length)} x ${Formatters.formatDimension(geometry.rectA.width)}) + " +
                "(${Formatters.formatDimension(geometry.rectB.length)} x ${Formatters.formatDimension(geometry.rectB.width)})"
        }
    }
}

private fun copyToClipboard(value: String) {
    val selection = StringSelection(value)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
}
