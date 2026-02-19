package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import com.tradesketch.estimator.ui.components.DangerActionButton

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.elementCount
import com.tradesketch.estimator.domain.model.totalAreaSqFt
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectsEvent
import com.tradesketch.estimator.ui.viewmodel.ProjectsViewModel
import com.tradesketch.estimator.utils.Formatters
import kotlinx.coroutines.delay

@Composable
fun ProjectsScreen(
    onNavigateToProject: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(ProjectSortMode.RECENT) }
    var pendingDelete by remember { mutableStateOf<Project?>(null) }
    var showTradeOnboardingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectsEvent.NavigateToProject -> onNavigateToProject(event.projectId)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.recordTap("projects_screen_opened")
    }

    LaunchedEffect(uiState.isLoading, uiState.settings.hasCompletedTradeOnboarding) {
        if (uiState.isLoading || uiState.settings.hasCompletedTradeOnboarding) {
            showTradeOnboardingDialog = false
            return@LaunchedEffect
        }
        // Prevent dialog flicker while settings hydrate from storage on startup.
        delay(450)
        if (!uiState.isLoading && !uiState.settings.hasCompletedTradeOnboarding) {
            showTradeOnboardingDialog = true
        }
    }

    val visibleProjects = remember(uiState.projects, searchQuery, sortMode) {
        uiState.projects
            .filter { project ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.trim().lowercase()
                    project.name.lowercase().contains(query) ||
                        project.blueprintDocument.rooms.any { it.name.lowercase().contains(query) }
                }
            }
            .sortedWith(sortMode.comparator)
    }

    Box(modifier = modifier) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error != null -> {
                ErrorCard(
                    message = uiState.error ?: "Unknown error",
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            else -> {
                ProjectsContent(
                    projects = visibleProjects,
                    allProjects = uiState.projects,
                    settings = uiState.settings,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    sortMode = sortMode,
                    onSortModeChange = { sortMode = it },
                    onSelectPrimaryTrade = {
                        viewModel.recordTap("projects_select_primary_trade")
                        viewModel.updatePrimaryTrade(it)
                    },
                    onToggleSimplifiedHome = {
                        viewModel.recordTap("projects_toggle_simplified_home")
                        viewModel.updateSimplifiedHome(it)
                    },
                    onOpenProject = {
                        haptics.tap()
                        viewModel.recordTap("projects_open_project")
                        onNavigateToProject(it)
                    },
                    onOpenSettings = {
                        haptics.tap()
                        viewModel.recordTap("projects_open_settings")
                        onNavigateToSettings()
                    },
                    onDeleteProject = {
                        haptics.tap()
                        pendingDelete = it
                    },
                    onCreateBlank = {
                        haptics.tap()
                        showNewProjectDialog = true
                    }
                )
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name ->
                haptics.confirm()
                viewModel.createBlankProject(name)
                showNewProjectDialog = false
            }
        )
    }

    if (showTradeOnboardingDialog) {
        TradeFocusOnboardingDialog(
            initialTrade = uiState.settings.primaryTrade,
            onComplete = { selectedTrade ->
                haptics.confirm()
                showTradeOnboardingDialog = false
                viewModel.completeTradeOnboarding(selectedTrade)
            }
        )
    }

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Project") },
            text = { Text("Delete \"${project.name}\" and all spaces in it? This cannot be undone.") },
            confirmButton = {
                DangerActionButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.deleteProject(project.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                QuietActionButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectsContent(
    projects: List<Project>,
    allProjects: List<Project>,
    settings: Settings,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortMode: ProjectSortMode,
    onSortModeChange: (ProjectSortMode) -> Unit,
    onSelectPrimaryTrade: (PrimaryTrade) -> Unit,
    onToggleSimplifiedHome: (Boolean) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteProject: (Project) -> Unit,
    onCreateBlank: () -> Unit
) {
    val totalSpaces = allProjects.sumOf { it.blueprintDocument.elementCount() }
    val totalArea = allProjects.sumOf { it.blueprintDocument.totalAreaSqFt() }
    val stagedDelay: (Int) -> Int = { base -> if (settings.reducedMotionEnabled) 0 else base }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AnimatedEntry(delayMs = stagedDelay(0)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Projects",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Name a project, choose what you're estimating, then build.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open settings"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrimaryTrade.entries.forEach { trade ->
                                FilterChip(
                                    selected = settings.primaryTrade == trade,
                                    onClick = { onSelectPrimaryTrade(trade) },
                                    label = { Text(trade.displayLabel()) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricPill(
                                label = "Projects",
                                value = allProjects.size.toString(),
                                tone = MaterialTheme.colorScheme.surface,
                                onTone = MaterialTheme.colorScheme.onSurface
                            )
                            MetricPill(
                                label = "Elements",
                                value = totalSpaces.toString(),
                                tone = MaterialTheme.colorScheme.surface,
                                onTone = MaterialTheme.colorScheme.onSurface
                            )
                            MetricPill(
                                label = "Area",
                                value = Formatters.formatArea(totalArea),
                                tone = MaterialTheme.colorScheme.surface,
                                onTone = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedEntry(delayMs = stagedDelay(40)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryActionButton(
                        onClick = onCreateBlank,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Project")
                    }
                }
            }
        }

        item {
            Text(
                text = "${projects.size} project(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search projects") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectSortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { onSortModeChange(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No projects yet."
                            } else {
                                "No matches found"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Tap New Project to begin."
                            } else {
                                "Try a different search term or clear your filter."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val projectArea = project.blueprintDocument.totalAreaSqFt()
    val elementCount = project.blueprintDocument.elementCount()
    val laneTags = projectLaneTags(project)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$elementCount elements • ${Formatters.formatArea(projectArea)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Updated ${Formatters.formatDateTime(project.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (laneTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        laneTags.forEach { laneTag ->
                            LaneTagChip(label = laneTag)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    tone: Color,
    onTone: Color
) {
    Row(
        modifier = Modifier
            .background(color = tone, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(onTone.copy(alpha = 0.8f), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium,
            color = onTone
        )
    }
}

@Composable
private fun LaneTagChip(label: String) {
    val (background, foreground) = when (label) {
        "Drywall" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Concrete" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "Rooms" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .background(color = background, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryActionButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                placeholder = { Text("Example: Main Floor Remodel") },
                singleLine = true
            )
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
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
private fun TradeFocusOnboardingDialog(
    initialTrade: PrimaryTrade,
    onComplete: (PrimaryTrade) -> Unit
) {
    var selectedTrade by rememberSaveable { mutableStateOf(initialTrade) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Choose Your Primary Trade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "We'll simplify your home screen and templates for this trade. You can change it anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryTrade.entries.forEach { trade ->
                        FilterChip(
                            selected = selectedTrade == trade,
                            onClick = { selectedTrade = trade },
                            label = { Text(trade.displayLabel()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(onClick = { onComplete(selectedTrade) }) {
                Text("Continue")
            }
        }
    )
}

private enum class ProjectSortMode(
    val label: String,
    val comparator: Comparator<Project>
) {
    RECENT(
        label = "Recent",
        comparator = compareByDescending<Project> { it.updatedAt }
    ),
    NAME(
        label = "A-Z",
        comparator = compareBy<Project> { it.name.lowercase() }
    ),
    SPACES(
        label = "Most Elements",
        comparator = compareByDescending<Project> { it.blueprintDocument.elementCount() }
            .thenByDescending { it.updatedAt }
    )
}

private fun projectLaneTags(project: Project): List<String> {
    val tags = mutableListOf<String>()
    val blueprint = project.blueprintDocument
    if (blueprint.walls.isNotEmpty()) tags += "Drywall"
    if (blueprint.rooms.any { room -> "slab" in room.tags || "concrete" in room.tags }) tags += "Concrete"
    if (blueprint.rooms.isNotEmpty()) tags += "Rooms"
    return tags
}

