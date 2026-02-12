package com.tradesketch.estimator.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectsViewModel
import com.tradesketch.estimator.utils.Formatters

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

    val visibleProjects = remember(uiState.projects, searchQuery, sortMode) {
        uiState.projects
            .filter { project ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.trim().lowercase()
                    project.name.lowercase().contains(query) ||
                        project.spaces.any { it.name.lowercase().contains(query) }
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
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    sortMode = sortMode,
                    onSortModeChange = { sortMode = it },
                    onOpenProject = {
                        haptics.tap()
                        onNavigateToProject(it)
                    },
                    onOpenSettings = {
                        haptics.tap()
                        onNavigateToSettings()
                    },
                    onDeleteProject = {
                        haptics.tap()
                        pendingDelete = it
                    },
                    onCreateFromTemplate = { template ->
                        haptics.confirm()
                        viewModel.createFromTemplate(template)
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

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Project") },
            text = { Text("Delete \"${project.name}\" and all spaces in it? This cannot be undone.") },
            confirmButton = {
                Button(
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
                TextButton(onClick = { pendingDelete = null }) {
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
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortMode: ProjectSortMode,
    onSortModeChange: (ProjectSortMode) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteProject: (Project) -> Unit,
    onCreateFromTemplate: (ProjectTemplate) -> Unit,
    onCreateBlank: () -> Unit
) {
    val totalSpaces = allProjects.sumOf { it.spaces.size }
    val totalArea = allProjects.sumOf { project -> project.spaces.sumOf { it.geometry.areaSqFt() } }
    val modeledProjects = allProjects.count { it.spaces.isNotEmpty() }
    val takeoffReadyProjects = allProjects.count { project ->
        project.spaces.sumOf { it.geometry.areaSqFt() } > 0.0
    }
    val workspaceFlow = listOf(
        WorkspaceFlowStep(
            label = "Create",
            detail = "${allProjects.size} project(s)",
            complete = allProjects.isNotEmpty()
        ),
        WorkspaceFlowStep(
            label = "Model",
            detail = "$modeledProjects with spaces",
            complete = modeledProjects > 0
        ),
        WorkspaceFlowStep(
            label = "Estimate",
            detail = "$takeoffReadyProjects takeoff-ready",
            complete = takeoffReadyProjects > 0
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AnimatedEntry(delayMs = 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Project Command Center",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Build faster quotes with clean model, takeoff, and export flow.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open settings",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricPill(
                                label = "Projects",
                                value = allProjects.size.toString(),
                                tone = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                                onTone = MaterialTheme.colorScheme.onPrimary
                            )
                            MetricPill(
                                label = "Spaces",
                                value = totalSpaces.toString(),
                                tone = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                                onTone = MaterialTheme.colorScheme.onPrimary
                            )
                            MetricPill(
                                label = "Tracked Area",
                                value = Formatters.formatArea(totalArea),
                                tone = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                                onTone = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedEntry(delayMs = 40) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateBlank,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Blank")
                    }
                    OutlinedButton(
                        onClick = { onCreateFromTemplate(ProjectTemplate.BEDROOM) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quick Bedroom")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.animateContentSize()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Workspace Flow",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workspaceFlow.forEach { step ->
                            WorkspaceFlowPill(
                                step = step,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedEntry(delayMs = 80) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    label = { Text("Search projects or spaces") },
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
        }

        item {
            Text(
                text = "${projects.size} visible project(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
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
                                "No projects yet"
                            } else {
                                "No matches found"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Start with a template or create a blank project to begin modeling and takeoffs."
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Template Library",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Use these as polished starting points and adjust in the 3D builder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val templates = ProjectTemplate.entries.filter { it != ProjectTemplate.BLANK }
        items(templates, key = { it.name }) { template ->
            TemplateCard(
                template = template,
                onClick = { onCreateFromTemplate(template) }
            )
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
    val projectArea = project.spaces.sumOf { it.geometry.areaSqFt() }
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
                    text = "${project.spaces.size} spaces • ${Formatters.formatArea(projectArea)}",
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
private fun TemplateCard(
    template: ProjectTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.displayName(),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = onClick) {
                Text("Use")
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
private fun WorkspaceFlowPill(
    step: WorkspaceFlowStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (step.complete) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = if (step.complete) "Ready" else "Pending",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
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
            Button(onClick = onDismiss) {
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
            Button(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        label = "Most Spaces",
        comparator = compareByDescending<Project> { it.spaces.size }
            .thenByDescending { it.updatedAt }
    )
}

private data class WorkspaceFlowStep(
    val label: String,
    val detail: String,
    val complete: Boolean
)

private fun projectLaneTags(project: Project): List<String> {
    var hasDrywall = false
    var hasConcrete = false
    var hasRooms = false
    project.spaces.forEach { space ->
        when (space.geometry) {
            is Geometry.Wall -> hasDrywall = true
            is Geometry.Slab -> hasConcrete = true
            else -> hasRooms = true
        }
    }
    val tags = mutableListOf<String>()
    if (hasDrywall) tags += "Drywall"
    if (hasConcrete) tags += "Concrete"
    if (hasRooms) tags += "Rooms"
    return tags
}
