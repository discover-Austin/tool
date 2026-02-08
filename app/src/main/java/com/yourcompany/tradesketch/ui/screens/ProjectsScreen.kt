package com.yourcompany.tradesketch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.tradesketch.domain.model.Project
import com.yourcompany.tradesketch.domain.model.ProjectTemplate
import com.yourcompany.tradesketch.ui.viewmodel.ProjectsViewModel
import com.yourcompany.tradesketch.utils.Formatters

@Composable
fun ProjectsScreen(
    onNavigateToProject: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.error != null -> {
                ErrorCard(
                    message = uiState.error ?: "Unknown error",
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            else -> {
                ProjectsContent(
                    projects = uiState.projects,
                    onOpenProject = onNavigateToProject,
                    onDeleteProject = { viewModel.deleteProject(it) },
                    onCreateFromTemplate = { template ->
                        viewModel.createFromTemplate(template)
                    },
                    onCreateBlank = { showNewProjectDialog = true }
                )
            }
        }
    }
    
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name ->
                viewModel.createBlankProject(name)
                showNewProjectDialog = false
            }
        )
    }
}

@Composable
private fun ProjectsContent(
    projects: List<Project>,
    onOpenProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onCreateFromTemplate: (ProjectTemplate) -> Unit,
    onCreateBlank: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Recent Projects",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        if (projects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No projects yet. Start with a template or create a blank project.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onDelete = { onDeleteProject(project.id) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start with a Template",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        item {
            TemplateCard(
                template = ProjectTemplate.BEDROOM,
                onClick = { onCreateFromTemplate(ProjectTemplate.BEDROOM) }
            )
        }
        item {
            TemplateCard(
                template = ProjectTemplate.GARAGE,
                onClick = { onCreateFromTemplate(ProjectTemplate.GARAGE) }
            )
        }
        item {
            TemplateCard(
                template = ProjectTemplate.DRIVEWAY,
                onClick = { onCreateFromTemplate(ProjectTemplate.DRIVEWAY) }
            )
        }
        item {
            TemplateCard(
                template = ProjectTemplate.YARD_BED,
                onClick = { onCreateFromTemplate(ProjectTemplate.YARD_BED) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCreateBlank,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Blank Project")
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.spaces.size} spaces • ${Formatters.formatDate(project.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.displayName(),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClick) {
                Text("Use")
            }
        }
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
                text = "Error",
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
                Text("OK")
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
        title = { Text("New Project") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
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
