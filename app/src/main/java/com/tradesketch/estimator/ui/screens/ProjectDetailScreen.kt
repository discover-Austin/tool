package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.totalAreaSqFt
import com.tradesketch.estimator.domain.model.totalWallAreaSqFt
import com.tradesketch.estimator.domain.model.totalRoomAreaSqFt
import com.tradesketch.estimator.domain.model.totalOpeningCount
import com.tradesketch.estimator.domain.model.elementCount
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
import com.tradesketch.estimator.utils.Formatters
import com.tradesketch.estimator.utils.Validators

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
    var showRenameDialog by remember { mutableStateOf(false) }
    var projectNameDraft by remember { mutableStateOf("") }

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
                    }
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
}

@Composable
private fun ProjectContent(
    project: Project,
    onOpenBlueprint: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onRenameProject: () -> Unit
) {
    var detailMode by rememberSaveable(project.id) { mutableStateOf(ProjectDetailMode.OVERVIEW) }
    val haptics = rememberAppHaptics()
    val blueprint = project.blueprintDocument
    val totalArea = blueprint.totalAreaSqFt()
    val netArea = blueprint.totalWallAreaSqFt() + blueprint.totalRoomAreaSqFt()
    val totalOpenings = blueprint.totalOpeningCount()
    val hasAtLeastOneSpace = blueprint.elementCount() > 0
    val projectHealth = buildProjectHealthInsight(
        blueprint = blueprint,
        netArea = netArea
    )

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
                        text = "${blueprint.walls.size} walls, ${blueprint.rooms.size} rooms",
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
                        onOpenBlueprint()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Blueprint")
                }
                SecondaryActionButton(
                    onClick = {
                        haptics.confirm()
                        onOpenTakeoff()
                    },
                    enabled = hasAtLeastOneSpace,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Takeoff")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    label = { Text("Elements (${blueprint.elementCount()})") }
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
                            label = "Elements",
                            value = blueprint.elementCount().toString(),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotMetric(
                            label = "Total Area",
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
                                ProjectHealthAction.OPEN_BLUEPRINT -> onOpenBlueprint()
                                ProjectHealthAction.OPEN_TAKEOFF -> onOpenTakeoff()
                                ProjectHealthAction.REVIEW_LIST -> detailMode = ProjectDetailMode.LIST
                                else -> onOpenBlueprint()
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
                                text = "Blueprint Elements",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${blueprint.walls.size} wall(s), ${blueprint.rooms.size} room(s), ${blueprint.openings.size} opening(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (blueprint.elementCount() == 0) {
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
                                    text = "No elements yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use Blueprint screen to add walls, rooms, and openings.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    if (blueprint.walls.isNotEmpty()) {
                        item(key = "walls_header") {
                            Text(
                                text = "Walls (${blueprint.walls.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(blueprint.walls, key = { it.id }) { wall ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Wall",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${Formatters.formatDimension(Millimeters.fromFeet(wall.lengthFeet()))} × ${Formatters.formatDimension(wall.height)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    if (blueprint.rooms.isNotEmpty()) {
                        item(key = "rooms_header") {
                            Text(
                                text = "Rooms (${blueprint.rooms.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(blueprint.rooms, key = { it.id }) { room ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = room.name,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${Formatters.formatArea(room.areaSqFt())} floor area",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
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
                    Text("Review Element List")
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

private enum class ProjectDetailMode {
    OVERVIEW,
    LIST
}

private enum class ProjectHealthAction {
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

private fun buildProjectHealthInsight(
    blueprint: BlueprintDocument,
    netArea: Double
): ProjectHealthInsight {
    var score = 0
    val highlights = mutableListOf<String>()
    val risks = mutableListOf<String>()
    val elementCount = blueprint.elementCount()
    val hasWalls = blueprint.walls.isNotEmpty()
    val hasRooms = blueprint.rooms.isNotEmpty()
    val openingCount = blueprint.totalOpeningCount()

    if (elementCount > 0) {
        score += 28
        highlights += "$elementCount element(s) created"
    } else {
        risks += "No blueprint elements added yet."
    }

    if (netArea > 0.0) {
        score += 24
        highlights += "${Formatters.formatArea(netArea)} measurable net area captured"
    } else {
        risks += "Net measurable area is zero."
    }

    if (hasWalls && hasRooms) {
        score += 18
        highlights += "Both walls and rooms modeled"
    } else if (hasWalls || hasRooms) {
        risks += "Only one element type is represented."
    }

    if (openingCount > 0) {
        score += 12
        highlights += "$openingCount opening(s) accounted for"
    } else {
        risks += "No door/window openings captured yet."
    }

    if (elementCount > 0) {
        score += 18
        highlights += "Blueprint data is available"
    }

    score = score.coerceIn(0, 100)
    val level = when {
        score >= 85 -> "Bid-Ready"
        score >= 65 -> "Strong Progress"
        score >= 40 -> "In Progress"
        else -> "Needs Setup"
    }

    val action = when {
        elementCount == 0 ->
            ProjectHealthAction.OPEN_BLUEPRINT to "Start Blueprint"
        netArea <= 0.0 ->
            ProjectHealthAction.REVIEW_LIST to "Review Elements"
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

