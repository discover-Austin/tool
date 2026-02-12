package com.tradesketch.estimator.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import com.tradesketch.estimator.domain.model.volumeCuFt
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
import com.tradesketch.estimator.utils.Formatters
import com.tradesketch.estimator.utils.Validators

@Composable
fun ProjectDetailScreen(
    projectId: String,
    onOpenBlueprint: () -> Unit = {},
    onOpenTakeoff: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    var showSpaceEditor by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<Space?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var projectNameDraft by remember { mutableStateOf("") }
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var showQuickRoomDialog by remember { mutableStateOf(false) }
    var quickRoomDialogKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }

    Box(modifier = modifier) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.project != null -> {
                val project = uiState.project!!
                ProjectContent(
                    project = project,
                    onOpenBlueprint = onOpenBlueprint,
                    onOpenTakeoff = onOpenTakeoff,
                    onOpenExport = onOpenExport,
                    onRenameProject = {
                        projectNameDraft = project.name
                        showRenameDialog = true
                    },
                    onRequestAddSpace = {
                        showAddMethodDialog = true
                    },
                    onEditSpace = { space ->
                        editingSpace = space
                        showSpaceEditor = true
                    },
                    onDuplicateSpace = { viewModel.duplicateSpace(it) },
                    onDeleteSpace = { viewModel.deleteSpace(it) },
                    onAutoLayout = { viewModel.autoLayoutSpaces() }
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameProjectDialog(
            value = projectNameDraft,
            onValueChange = { projectNameDraft = it },
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                if (Validators.isValidProjectName(projectNameDraft)) {
                    haptics.confirm()
                    viewModel.updateProjectName(projectNameDraft.trim())
                    showRenameDialog = false
                }
            }
        )
    }

    if (showAddMethodDialog) {
        AddSpaceMethodDialog(
            onDismiss = { showAddMethodDialog = false },
            onQuickRoom = {
                haptics.confirm()
                showAddMethodDialog = false
                showQuickRoomDialog = true
            },
            onCustomSpace = {
                haptics.tap()
                showAddMethodDialog = false
                editingSpace = null
                showSpaceEditor = true
            }
        )
    }

    if (showQuickRoomDialog) {
        QuickRoomDialog(
            dialogKey = quickRoomDialogKey,
            onDismiss = { showQuickRoomDialog = false },
            onSave = { spaces, continueToNextRoom ->
                haptics.confirm()
                viewModel.addSpaces(spaces)
                if (continueToNextRoom) {
                    quickRoomDialogKey += 1
                } else {
                    showQuickRoomDialog = false
                }
            }
        )
    }

    if (showSpaceEditor && uiState.project != null) {
        SpaceEditorDialog(
            initialSpace = editingSpace,
            onDismiss = { showSpaceEditor = false },
            onSave = { space ->
                haptics.confirm()
                if (editingSpace == null) {
                    viewModel.addSpace(space)
                } else {
                    viewModel.updateSpace(space)
                }
                showSpaceEditor = false
            }
        )
    }
}

@Composable
private fun ProjectContent(
    project: Project,
    onOpenBlueprint: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onOpenExport: () -> Unit,
    onRenameProject: () -> Unit,
    onRequestAddSpace: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onAutoLayout: () -> Unit
) {
    var detailMode by rememberSaveable(project.id) { mutableStateOf(ProjectDetailMode.OVERVIEW) }
    var listTradeFilter by rememberSaveable(project.id) { mutableStateOf(SpaceTradeFilter.ALL) }
    var listSearchQuery by rememberSaveable(project.id) { mutableStateOf("") }
    val haptics = rememberAppHaptics()
    val totalArea = project.spaces.sumOf { it.geometry.areaSqFt() }
    val netArea = project.spaces.sumOf { (it.geometry.areaSqFt() - it.openingsAreaSqFt()).coerceAtLeast(0.0) }
    val totalVolume = project.spaces.sumOf { it.geometry.volumeCuFt() }
    val totalOpenings = project.spaces.sumOf { space -> space.openings.sumOf { it.count } }
    val normalizedSearchQuery = listSearchQuery.trim().lowercase()
    val spacesByLane = project.spaces.groupBy(::tradeLaneForSpace)
    val filteredByLane = if (normalizedSearchQuery.isBlank()) {
        spacesByLane
    } else {
        spacesByLane.mapValues { laneEntry ->
            laneEntry.value.filter { spaceMatchesQuery(it, normalizedSearchQuery) }
        }
    }
    val laneSummaries = ProjectTradeLane.entries.map { lane ->
        val laneSpaces = spacesByLane[lane].orEmpty()
        TradeLaneSummary(
            lane = lane,
            count = laneSpaces.size,
            areaSqFt = laneSpaces.sumOf { it.geometry.areaSqFt() }
        )
    }
    val workflowSteps = listOf(
        WorkflowStepStatus(
            label = "Model Ready",
            detail = if (project.spaces.isNotEmpty()) "${project.spaces.size} spaces created" else "Add your first space",
            complete = project.spaces.isNotEmpty()
        ),
        WorkflowStepStatus(
            label = "Trade Mix",
            detail = if ((spacesByLane[ProjectTradeLane.DRYWALL].orEmpty().size +
                    spacesByLane[ProjectTradeLane.CONCRETE].orEmpty().size +
                    spacesByLane[ProjectTradeLane.ROOMS].orEmpty().size) >= 2
            ) {
                "Multiple trade lanes present"
            } else {
                "Add at least one more lane"
            },
            complete = (spacesByLane[ProjectTradeLane.DRYWALL].orEmpty().isNotEmpty() &&
                spacesByLane[ProjectTradeLane.CONCRETE].orEmpty().isNotEmpty()) ||
                (spacesByLane[ProjectTradeLane.DRYWALL].orEmpty().isNotEmpty() &&
                    spacesByLane[ProjectTradeLane.ROOMS].orEmpty().isNotEmpty()) ||
                (spacesByLane[ProjectTradeLane.CONCRETE].orEmpty().isNotEmpty() &&
                    spacesByLane[ProjectTradeLane.ROOMS].orEmpty().isNotEmpty())
        ),
        WorkflowStepStatus(
            label = "Estimate Ready",
            detail = if (netArea > 0.0) "Net area captured" else "Capture measurable area",
            complete = netArea > 0.0
        )
    )
    val visibleLaneGroups = if (listTradeFilter.lane == null) {
        ProjectTradeLane.entries.mapNotNull { lane ->
            filteredByLane[lane]
                ?.takeIf { it.isNotEmpty() }
                ?.let { lane to it }
        }
    } else {
        val lane = listTradeFilter.lane!!
        listOf(lane to filteredByLane[lane].orEmpty())
    }
    val visibleSpaces = visibleLaneGroups.sumOf { it.second.size }
    val filterScroll = rememberScrollState()
    val hasAtLeastOneSpace = project.spaces.isNotEmpty()
    val hasTradeMix = workflowSteps[1].complete

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${project.spaces.size} spaces",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    haptics.tap()
                    onRenameProject()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename project"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        haptics.confirm()
                        onRequestAddSpace()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Space / Room")
                }
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onAutoLayout()
                    },
                    enabled = project.spaces.size > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto Arrange")
                }
            }

            Text(
                text = "Follow the flow: Overview and Space List here, Blueprint tab for 3D layout, then Takeoff and Export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = detailMode == ProjectDetailMode.OVERVIEW,
                    onClick = {
                        haptics.tap()
                        detailMode = ProjectDetailMode.OVERVIEW
                    },
                    label = { Text("Overview") }
                )
                FilterChip(
                    selected = detailMode == ProjectDetailMode.LIST,
                    onClick = {
                        haptics.tap()
                        detailMode = ProjectDetailMode.LIST
                    },
                    label = { Text("Space List (${project.spaces.size})") }
                )
            }
        }

        if (detailMode == ProjectDetailMode.OVERVIEW) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Project Snapshot",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnapshotMetric(
                            label = "Spaces",
                            value = project.spaces.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotMetric(
                            label = "Openings",
                            value = totalOpenings.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnapshotMetric(
                            label = "Gross Area",
                            value = Formatters.formatArea(totalArea),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotMetric(
                            label = "Net Area",
                            value = Formatters.formatArea(netArea),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SnapshotMetric(
                        label = "Volume",
                        value = "${Formatters.formatQuantity(totalVolume)} cu ft",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Trade Lanes",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        laneSummaries.forEach { laneSummary ->
                            TradeLaneMetric(
                                summary = laneSummary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "Workflow Readiness",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workflowSteps.forEach { step ->
                            WorkflowStepPill(
                                status = step,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "Guided Flow",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                detailMode = ProjectDetailMode.LIST
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refine Space List")
                        }
                        Button(
                            onClick = {
                                haptics.confirm()
                                onOpenBlueprint()
                            },
                            enabled = hasAtLeastOneSpace,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue to Blueprint")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                onOpenTakeoff()
                            },
                            enabled = hasAtLeastOneSpace,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Go to Takeoff")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                onOpenExport()
                            },
                            enabled = hasAtLeastOneSpace && hasTradeMix,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Jump to Export")
                        }
                    }
                    Text(
                        text = if (hasAtLeastOneSpace) {
                            "Recommended order: model setup -> blueprint layout -> takeoff -> export."
                        } else {
                            "Add at least one space to unlock full flow."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Updated: ${Formatters.formatDateTime(project.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (detailMode == ProjectDetailMode.LIST) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Space List Filters",
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                value = listSearchQuery,
                                onValueChange = { listSearchQuery = it },
                                label = { Text("Search spaces by name or geometry") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SpaceTradeFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = filter == listTradeFilter,
                                        onClick = {
                                            haptics.tap()
                                            listTradeFilter = filter
                                        },
                                        label = { Text(filter.label) }
                                    )
                                }
                            }
                            Text(
                                text = "$visibleSpaces visible space(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (project.spaces.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No spaces yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use Quick Room for fast room setup or Custom Space for full control.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else if (visibleSpaces == 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "No spaces in this trade lane",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Switch to another filter or add a new space to this lane.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    visibleLaneGroups.forEach { laneGroup ->
                        val lane = laneGroup.first
                        val laneSpaces = laneGroup.second
                        item(key = "lane_${lane.name}") {
                            Text(
                                text = "${lane.label} (${laneSpaces.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(laneSpaces, key = { it.id }) { space ->
                            SpaceCard(
                                space = space,
                                onEdit = { onEditSpace(space) },
                                onDuplicate = { onDuplicateSpace(space.id) },
                                onDelete = { onDeleteSpace(space.id) },
                                laneLabel = lane.label
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun SnapshotMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SpaceCard(
    space: Space,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    laneLabel: String
) {
    val haptics = rememberAppHaptics()
    val area = space.geometry.areaSqFt()
    val volume = space.geometry.volumeCuFt()
    val openingArea = space.openingsAreaSqFt()
    val netArea = (area - openingArea).coerceAtLeast(0.0)

    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = geometryDescription(space.geometry),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Trade Lane: $laneLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = {
                        haptics.tap()
                        onDuplicate()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate space"
                        )
                    }
                    IconButton(onClick = {
                        haptics.confirm()
                        onDelete()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete space",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Area: ${Formatters.formatArea(area)}",
                style = MaterialTheme.typography.bodySmall
            )
            if (openingArea > 0.0) {
                Text(
                    text = "Net after openings: ${Formatters.formatArea(netArea)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (volume > 0.0) {
                Text(
                    text = "Volume: ${Formatters.formatQuantity(volume)} cu ft",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TradeLaneMetric(
    summary: TradeLaneSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = summary.lane.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${summary.count} space(s)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = Formatters.formatArea(summary.areaSqFt),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun WorkflowStepPill(
    status: WorkflowStepStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (status.complete) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (status.complete) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (status.complete) "Ready" else "Pending",
                style = MaterialTheme.typography.bodySmall,
                color = if (status.complete) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Text(
                text = status.detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RenameProjectDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Project") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Project Name") },
                singleLine = true,
                supportingText = {
                    Text("Max 100 characters")
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = Validators.isValidProjectName(value)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun AddSpaceMethodDialog(
    onDismiss: () -> Unit,
    onQuickRoom: () -> Unit,
    onCustomSpace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Space") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Room walks you through common room questions: walls, doors, windows, optional ceiling.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Custom Space opens the advanced geometry editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onQuickRoom) {
                Text("Quick Room")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCustomSpace) {
                Text("Custom Space")
            }
        }
    )
}

private fun geometryDescription(geometry: Geometry): String {
    return when (geometry) {
        is Geometry.Wall -> {
            val length = Formatters.formatDimension(geometry.length)
            val height = Formatters.formatDimension(geometry.height)
            "Wall: $length x $height"
        }

        is Geometry.Rect -> {
            val length = Formatters.formatDimension(geometry.length)
            val width = Formatters.formatDimension(geometry.width)
            "Room: $length x $width"
        }

        is Geometry.Slab -> {
            val length = Formatters.formatDimension(geometry.length)
            val width = Formatters.formatDimension(geometry.width)
            val thickness = Formatters.formatDimension(geometry.thickness)
            "Slab: $length x $width x $thickness"
        }

        is Geometry.Circle -> {
            val radius = Formatters.formatDimension(geometry.radius)
            "Circle: radius $radius"
        }

        is Geometry.LShape -> {
            val aLength = Formatters.formatDimension(geometry.rectA.length)
            val aWidth = Formatters.formatDimension(geometry.rectA.width)
            val bLength = Formatters.formatDimension(geometry.rectB.length)
            val bWidth = Formatters.formatDimension(geometry.rectB.width)
            "L-shape: ($aLength x $aWidth) + ($bLength x $bWidth)"
        }
    }
}

private enum class ProjectDetailMode {
    OVERVIEW,
    LIST
}

private enum class ProjectTradeLane(val label: String) {
    DRYWALL("Drywall"),
    CONCRETE("Concrete"),
    ROOMS("Rooms")
}

private enum class SpaceTradeFilter(
    val label: String,
    val lane: ProjectTradeLane?
) {
    ALL(label = "All", lane = null),
    DRYWALL(label = "Drywall", lane = ProjectTradeLane.DRYWALL),
    CONCRETE(label = "Concrete", lane = ProjectTradeLane.CONCRETE),
    ROOMS(label = "Rooms", lane = ProjectTradeLane.ROOMS)
}

private data class TradeLaneSummary(
    val lane: ProjectTradeLane,
    val count: Int,
    val areaSqFt: Double
)

private data class WorkflowStepStatus(
    val label: String,
    val detail: String,
    val complete: Boolean
)

private fun tradeLaneForSpace(space: Space): ProjectTradeLane {
    return when (space.geometry) {
        is Geometry.Wall -> ProjectTradeLane.DRYWALL
        is Geometry.Slab -> ProjectTradeLane.CONCRETE
        else -> ProjectTradeLane.ROOMS
    }
}

private fun spaceMatchesQuery(space: Space, normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return space.name.lowercase().contains(normalizedQuery) ||
        geometryDescription(space.geometry).lowercase().contains(normalizedQuery)
}
