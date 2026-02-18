package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import com.tradesketch.estimator.ui.components.DangerActionButton

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import com.tradesketch.estimator.domain.model.volumeCuFt
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
import com.tradesketch.estimator.utils.Formatters
import com.tradesketch.estimator.utils.Validators
import java.util.UUID

@Composable
fun ProjectDetailScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenBlueprint: () -> Unit = {},
    onOpenTakeoff: () -> Unit = {},
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
        viewModel.recordTap("model_screen_opened")
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
                    onOpenBlueprint = {
                        haptics.tap()
                        viewModel.recordTap("detail_open_blueprint")
                        onOpenBlueprint()
                    },
                    onOpenTakeoff = {
                        haptics.tap()
                        viewModel.recordTap("detail_open_takeoff")
                        onOpenTakeoff()
                    },
                    onRenameProject = {
                        viewModel.recordTap("detail_open_rename")
                        projectNameDraft = project.name
                        showRenameDialog = true
                    },
                    onRequestAddSpace = {
                        viewModel.recordTap("detail_open_add_space")
                        showAddMethodDialog = true
                    },
                    onEditSpace = { space ->
                        viewModel.recordTap("detail_edit_space")
                        editingSpace = space
                        showSpaceEditor = true
                    },
                    onDuplicateSpace = { viewModel.duplicateSpace(it) },
                    onDeleteSpace = { viewModel.deleteSpace(it) },
                    onQuickAddWall = {
                        val wallCount = project.spaces.count { it.geometry is Geometry.Wall } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Wall $wallCount",
                                geometry = Geometry.Wall(
                                    length = mmFromFeet(12.0),
                                    height = mmFromFeet(9.0)
                                )
                            )
                        )
                    },
                    onQuickAddRoom = {
                        val roomCount = project.spaces.count { it.geometry is Geometry.Rect } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Room $roomCount",
                                geometry = Geometry.Rect(
                                    length = mmFromFeet(12.0),
                                    width = mmFromFeet(10.0)
                                )
                            )
                        )
                    },
                    onQuickAddSlab = {
                        val slabCount = project.spaces.count { it.geometry is Geometry.Slab } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Slab $slabCount",
                                geometry = Geometry.Slab(
                                    length = mmFromFeet(16.0),
                                    width = mmFromFeet(12.0),
                                    thickness = mmFromFeet(0.33)
                                )
                            )
                        )
                    },
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
    onRenameProject: () -> Unit,
    onRequestAddSpace: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onQuickAddWall: () -> Unit,
    onQuickAddRoom: () -> Unit,
    onQuickAddSlab: () -> Unit,
    onAutoLayout: () -> Unit
) {
    var detailMode by rememberSaveable(project.id) { mutableStateOf(ProjectDetailMode.OVERVIEW) }
    var showWorkspaceTools by rememberSaveable(project.id) { mutableStateOf(false) }
    var showQuickAddShortcuts by rememberSaveable(project.id) { mutableStateOf(false) }
    var showListFilters by rememberSaveable(project.id) { mutableStateOf(false) }
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
    val projectHealth = buildProjectHealthInsight(
        project = project,
        netArea = netArea,
        laneSummaries = laneSummaries
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
                PrimaryActionButton(
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
                SecondaryActionButton(
                    onClick = {
                        haptics.confirm()
                        onOpenBlueprint()
                    },
                    enabled = hasAtLeastOneSpace,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Blueprint")
                }
            }
            QuietActionButton(
                onClick = {
                    haptics.tap()
                    showWorkspaceTools = !showWorkspaceTools
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (showWorkspaceTools) {
                        "Hide Workspace Tools"
                    } else {
                        "Show Workspace Tools"
                    }
                )
            }
            if (showWorkspaceTools) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionButton(
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

                SecondaryActionButton(
                    onClick = {
                        haptics.tap()
                        showQuickAddShortcuts = !showQuickAddShortcuts
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showQuickAddShortcuts) "Hide Quick Add" else "Show Quick Add")
                }

                if (showQuickAddShortcuts) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryActionButton(
                            onClick = {
                                haptics.tap()
                                onQuickAddWall()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Quick Wall")
                        }
                        SecondaryActionButton(
                            onClick = {
                                haptics.tap()
                                onQuickAddRoom()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Quick Room")
                        }
                        SecondaryActionButton(
                            onClick = {
                                haptics.tap()
                                onQuickAddSlab()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Quick Slab")
                        }
                    }
                }
            }

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
                            label = "Gross Area",
                            value = Formatters.formatArea(totalArea),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnapshotMetric(
                            label = "Net Area",
                            value = Formatters.formatArea(netArea),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotMetric(
                            label = "Openings",
                            value = totalOpenings.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ProjectHealthCard(
                        insight = projectHealth,
                        onPrimaryAction = {
                            when (projectHealth.primaryAction) {
                                ProjectHealthAction.ADD_SPACE -> onRequestAddSpace()
                                ProjectHealthAction.AUTO_LAYOUT -> onAutoLayout()
                                ProjectHealthAction.OPEN_BLUEPRINT -> onOpenBlueprint()
                                ProjectHealthAction.OPEN_TAKEOFF -> onOpenTakeoff()
                                ProjectHealthAction.REVIEW_LIST -> detailMode = ProjectDetailMode.LIST
                            }
                        },
                        onReviewList = { detailMode = ProjectDetailMode.LIST }
                    )
                    Text(
                        text = "Next Actions",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryActionButton(
                            onClick = {
                                haptics.tap()
                                detailMode = ProjectDetailMode.LIST
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refine Space List")
                        }
                        PrimaryActionButton(
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
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            onOpenTakeoff()
                        },
                        enabled = netArea > 0.0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jump to Takeoff")
                    }
                    Text(
                        text = if (hasAtLeastOneSpace) {
                            "Lay out geometry in Blueprint, or jump to Takeoff when measurements are ready."
                        } else {
                            "Add at least one space to continue."
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
                            QuietActionButton(
                                onClick = {
                                    haptics.tap()
                                    showListFilters = !showListFilters
                                }
                            ) {
                                Text(
                                    if (showListFilters) {
                                        "Hide Filters"
                                    } else {
                                        "Show Filters"
                                    }
                                )
                            }
                            if (showListFilters) {
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
                            } else {
                                Text(
                                    text = buildString {
                                        append("Lane: ${listTradeFilter.label}")
                                        if (listSearchQuery.isNotBlank()) {
                                            append(" • Search active")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
private fun ProjectHealthCard(
    insight: ProjectHealthInsight,
    onPrimaryAction: () -> Unit,
    onReviewList: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Project Intelligence",
                style = MaterialTheme.typography.titleSmall
            )
            LinearProgressIndicator(
                progress = { (insight.score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "${insight.score}/100 • ${insight.level}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            insight.highlights.take(2).forEach { highlight ->
                Text(
                    text = "Strength: $highlight",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            insight.risks.take(2).forEach { risk ->
                Text(
                    text = "Watch: $risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(insight.primaryActionLabel)
                }
                SecondaryActionButton(
                    onClick = onReviewList,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Review Space List")
                }
            }
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
            PrimaryActionButton(
                onClick = onConfirm,
                enabled = Validators.isValidProjectName(value)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            QuietActionButton(onClick = onDismiss) {
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
                    text = "Custom Space opens the full geometry editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(onClick = onQuickRoom) {
                Text("Quick Room")
            }
        },
        dismissButton = {
            SecondaryActionButton(onClick = onCustomSpace) {
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

private enum class ProjectHealthAction {
    ADD_SPACE,
    AUTO_LAYOUT,
    OPEN_BLUEPRINT,
    OPEN_TAKEOFF,
    REVIEW_LIST
}

private data class ProjectHealthInsight(
    val score: Int,
    val level: String,
    val highlights: List<String>,
    val risks: List<String>,
    val primaryAction: ProjectHealthAction,
    val primaryActionLabel: String
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

private fun buildProjectHealthInsight(
    project: Project,
    netArea: Double,
    laneSummaries: List<TradeLaneSummary>
): ProjectHealthInsight {
    var score = 0
    val highlights = mutableListOf<String>()
    val risks = mutableListOf<String>()
    val lanesWithData = laneSummaries.count { it.count > 0 }
    val placedCount = project.spaces.count { it.transform != SpaceTransform() }

    if (project.spaces.isNotEmpty()) {
        score += 28
        highlights += "${project.spaces.size} spaces created"
    } else {
        risks += "No spaces added yet."
    }

    if (netArea > 0.0) {
        score += 24
        highlights += "${Formatters.formatArea(netArea)} measurable net area captured"
    } else {
        risks += "Net measurable area is zero."
    }

    if (lanesWithData >= 2) {
        score += 18
        highlights += "Multiple trade lanes modeled"
    } else {
        risks += "Only one trade lane is represented."
    }

    val openingCount = project.spaces.sumOf { it.openings.sumOf { opening -> opening.count } }
    if (openingCount > 0) {
        score += 12
        highlights += "$openingCount opening(s) accounted for"
    } else {
        risks += "No door/window openings captured yet."
    }

    if (project.spaces.isNotEmpty()) {
        val placementRatio = placedCount.toDouble() / project.spaces.size.toDouble()
        if (placementRatio >= 0.6) {
            score += 18
            highlights += "Layout positions are mostly arranged"
        } else {
            risks += "Space layout is still rough. Auto-arrange can speed this up."
        }
    }

    score = score.coerceIn(0, 100)
    val level = when {
        score >= 85 -> "Bid-Ready"
        score >= 65 -> "Strong Progress"
        score >= 40 -> "In Progress"
        else -> "Needs Setup"
    }

    val action = when {
        project.spaces.isEmpty() ->
            ProjectHealthAction.ADD_SPACE to "Add First Space"
        lanesWithData < 2 ->
            ProjectHealthAction.ADD_SPACE to "Add Cross-Trade Space"
        placedCount < maxOf(1, project.spaces.size / 2) ->
            ProjectHealthAction.AUTO_LAYOUT to "Auto Arrange Layout"
        netArea <= 0.0 ->
            ProjectHealthAction.REVIEW_LIST to "Review Space Metrics"
        else ->
            ProjectHealthAction.OPEN_TAKEOFF to "Run Takeoff"
    }

    return ProjectHealthInsight(
        score = score,
        level = level,
        highlights = highlights,
        risks = risks,
        primaryAction = action.first,
        primaryActionLabel = action.second
    )
}

private fun mmFromFeet(feet: Double): com.tradesketch.estimator.domain.model.Millimeters {
    return com.tradesketch.estimator.domain.model.Millimeters.fromFeet(feet)
}

