package com.yourcompany.tradesketch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.tradesketch.domain.model.Geometry
import com.yourcompany.tradesketch.domain.model.Millimeters
import com.yourcompany.tradesketch.domain.model.Space
import com.yourcompany.tradesketch.ui.viewmodel.ProjectDetailViewModel
import com.yourcompany.tradesketch.utils.Formatters
import java.util.UUID

@Composable
fun ProjectDetailScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSpaceDialog by remember { mutableStateOf(false) }
    
    if (showAddSpaceDialog) {
        AddSpaceDialog(
            onDismiss = { showAddSpaceDialog = false },
            onAddSpace = { space ->
                viewModel.addSpace(space)
                showAddSpaceDialog = false
            }
        )
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
                ProjectContent(
                    project = uiState.project!!,
                    onDeleteSpace = { viewModel.deleteSpace(it) },
                    onAddSpaceClick = { showAddSpaceDialog = true }
                )
            }
        }
    }
}

@Composable
private fun ProjectContent(
    project: com.yourcompany.tradesketch.domain.model.Project,
    onDeleteSpace: (String) -> Unit,
    onAddSpaceClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                            text = "Add walls, slabs, or rooms to start calculating takeoffs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(project.spaces, key = { it.id }) { space ->
                SpaceCard(
                    space = space,
                    onDelete = { onDeleteSpace(space.id) }
                )
            }
        }
        
        item {
            OutlinedButton(
                onClick = onAddSpaceClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Space")
            }
        }
    }
}

@Composable
private fun SpaceCard(
    space: com.yourcompany.tradesketch.domain.model.Space,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                    text = when (val geom = space.geometry) {
                        is Geometry.Wall -> {
                            val length = Formatters.formatDimension(geom.length)
                            val height = Formatters.formatDimension(geom.height)
                            "Wall: $length × $height"
                        }
                        is Geometry.Rect -> {
                            val length = Formatters.formatDimension(geom.length)
                            val width = Formatters.formatDimension(geom.width)
                            "Room: $length × $width"
                        }
                        is Geometry.Slab -> {
                            val length = Formatters.formatDimension(geom.length)
                            val width = Formatters.formatDimension(geom.width)
                            "Slab: $length × $width"
                        }
                        is Geometry.Circle -> {
                            val radius = Formatters.formatDimension(geom.radius)
                            "Circle: radius $radius"
                        }
                        is Geometry.LShape -> {
                            "L-shaped area"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (space.openings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${space.openings.sumOf { it.count }} openings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSpaceDialog(
    onDismiss: () -> Unit,
    onAddSpace: (Space) -> Unit
) {
    var spaceName by remember { mutableStateOf("") }
    var spaceType by remember { mutableStateOf("Wall") }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val spaceTypes = listOf("Wall", "Room", "Slab")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Space") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = spaceName,
                    onValueChange = { spaceName = it },
                    label = { Text("Space Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = spaceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        spaceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    spaceType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = { Text("Length (feet)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (spaceType != "Wall") {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width (feet)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (spaceType == "Wall" || spaceType == "Room") {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (feet)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lengthValue = length.toDoubleOrNull() ?: 0.0
                    val widthValue = width.toDoubleOrNull() ?: 0.0
                    val heightValue = height.toDoubleOrNull() ?: 0.0
                    
                    if (spaceName.isNotBlank() && lengthValue > 0) {
                        val geometry = when (spaceType) {
                            "Wall" -> {
                                if (heightValue > 0) {
                                    Geometry.Wall(
                                        length = Millimeters.fromFeet(lengthValue),
                                        height = Millimeters.fromFeet(heightValue)
                                    )
                                } else null
                            }
                            "Room" -> {
                                if (widthValue > 0) {
                                    Geometry.Rect(
                                        length = Millimeters.fromFeet(lengthValue),
                                        width = Millimeters.fromFeet(widthValue)
                                    )
                                } else null
                            }
                            "Slab" -> {
                                if (widthValue > 0) {
                                    Geometry.Slab(
                                        length = Millimeters.fromFeet(lengthValue),
                                        width = Millimeters.fromFeet(widthValue),
                                        thickness = Millimeters.fromInches(4.0) // Default 4" thickness
                                    )
                                } else null
                            }
                            else -> null
                        }
                        
                        geometry?.let {
                            val space = Space(
                                id = UUID.randomUUID().toString(),
                                name = spaceName,
                                geometry = it,
                                openings = emptyList()
                            )
                            onAddSpace(space)
                        }
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
