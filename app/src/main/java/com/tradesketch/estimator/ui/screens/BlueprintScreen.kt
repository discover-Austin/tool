package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
import java.util.UUID

@Composable
fun BlueprintScreen(
    projectId: String,
    onOpenModel: () -> Unit = {},
    onOpenTakeoff: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    var showSpaceEditor by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<Space?>(null) }
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
                BlueprintContent(
                    project = project,
                    onRequestAddSpace = { showAddMethodDialog = true },
                    onQuickRoom = {
                        haptics.confirm()
                        showQuickRoomDialog = true
                    },
                    onQuickAddWall = {
                        haptics.confirm()
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
                    onQuickAddSlab = {
                        haptics.confirm()
                        val slabCount = project.spaces.count { it.geometry is Geometry.Slab } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Slab $slabCount",
                                geometry = Geometry.Slab(
                                    length = mmFromFeet(12.0),
                                    width = mmFromFeet(10.0),
                                    thickness = mmFromFeet(0.33)
                                )
                            )
                        )
                    },
                    onEditSpace = { space ->
                        editingSpace = space
                        showSpaceEditor = true
                    },
                    onDuplicateSpace = { viewModel.duplicateSpace(it) },
                    onDeleteSpace = { viewModel.deleteSpace(it) },
                    onUpdateSpaceTransform = { spaceId, transform ->
                        viewModel.updateSpaceTransform(spaceId, transform)
                    },
                    onAutoLayout = { viewModel.autoLayoutSpaces() },
                    onOpenModel = onOpenModel,
                    onOpenTakeoff = onOpenTakeoff,
                    onOpenExport = onOpenExport
                )
            }
        }
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
private fun BlueprintContent(
    project: Project,
    onRequestAddSpace: () -> Unit,
    onQuickRoom: () -> Unit,
    onQuickAddWall: () -> Unit,
    onQuickAddSlab: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onUpdateSpaceTransform: (String, SpaceTransform) -> Unit,
    onAutoLayout: () -> Unit,
    onOpenModel: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onOpenExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModelBuilder3DPanel(
            project = project,
            onAddSpace = onRequestAddSpace,
            onEditSpace = onEditSpace,
            onDuplicateSpace = onDuplicateSpace,
            onDeleteSpace = onDeleteSpace,
            onAutoLayout = onAutoLayout,
            onUpdateTransform = onUpdateSpaceTransform,
            immersiveMode = true,
            blueprintMode = true,
            onQuickRoom = onQuickRoom,
            onQuickAddWall = onQuickAddWall,
            onQuickAddSlab = onQuickAddSlab,
            onOpenModel = onOpenModel,
            onOpenTakeoff = onOpenTakeoff,
            onOpenExport = onOpenExport,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun mmFromFeet(feet: Double) = com.tradesketch.estimator.domain.model.Millimeters.fromFeet(feet)
