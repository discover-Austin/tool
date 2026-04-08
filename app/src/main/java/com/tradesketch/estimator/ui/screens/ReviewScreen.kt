package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.ui.components.WorkspacePageHeaderCard
import com.tradesketch.estimator.ui.components.WorkspaceSectionHeading
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.ui.viewmodel.buildTakeoffInputs
import com.tradesketch.estimator.ui.viewmodel.calculateForType
import com.tradesketch.estimator.ui.viewmodel.projectBlueprintForType
import com.tradesketch.estimator.ui.viewmodel.toTakeoffType
import com.tradesketch.estimator.utils.Formatters

@Composable
fun ReviewScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    viewModel: BlueprintEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }

    val project = uiState.project
    if (uiState.isLoading || project == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val settings = uiState.settings
    val useMetric = settings.useMetric
    val calculator = remember { CalculateTakeoffUseCase() }
    val reviewType = remember(project.takeoffSession.selectedScope) {
        project.takeoffSession.selectedScope.toTakeoffType()
    }
    val document = remember(project, reviewType) {
        projectBlueprintForType(project = project, type = reviewType)
    }
    val inputs = remember(project, settings, uiState.project?.takeoffSession) {
        buildTakeoffInputs(project = project, settings = settings)
    }
    val drywall = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.DRYWALL, inputs = inputs)
    }
    val concrete = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.CONCRETE, inputs = inputs)
    }
    val gravel = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.GRAVEL_MULCH, inputs = inputs)
    }
    val paint = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.PAINT, inputs = inputs)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkspacePageHeaderCard(
                title = "Review",
                subtitle = "Current scope geometry with trade-specific quantity traces for ${uiState.project?.name.orEmpty()}.",
                eyebrow = reviewType.displayLabel
            )
        }

        item {
            WorkspaceSectionHeading(
                title = "Rooms",
                detail = "Area, perimeter, and room-level tags for the active trade scope."
            )
        }
        items(document.rooms, key = { it.id }) { room ->
            Card(
                colors = appCardColors(),
                border = appCardBorder(),
                elevation = appCardElevation()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${room.name} (${room.id})", fontWeight = FontWeight.SemiBold)
                    Text("Area: ${Formatters.formatArea(room.areaSqFt(), useMetric)}")
                    Text("Perimeter: ${Formatters.formatLength(room.perimeterFeet(), useMetric)}")
                    if (room.tags.isNotEmpty()) {
                        Text("Tags: ${room.tags.joinToString()}")
                    }
                }
            }
        }

        item {
            WorkspaceSectionHeading(
                title = "Walls",
                detail = "Net wall geometry after openings are deducted from the measured surface."
            )
        }
        items(document.walls, key = { it.id }) { wall ->
            val openingArea = document.openings
                .filter { it.wallId == wall.id }
                .sumOf { Millimeters(it.widthMm).toFeet() * Millimeters(it.heightMm).toFeet() }
            val wallArea = Millimeters(wall.lengthMillimeters()).toFeet() * Millimeters(wall.heightMm).toFeet()
            Card(
                colors = appCardColors(),
                border = appCardBorder(),
                elevation = appCardElevation()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Wall ${wall.id}", fontWeight = FontWeight.SemiBold)
                    Text("Length: ${Formatters.formatLength(Millimeters(wall.lengthMillimeters()), useMetric)}")
                    Text("Height: ${Formatters.formatLength(Millimeters(wall.heightMm), useMetric)}")
                    Text("Area: ${Formatters.formatArea((wallArea - openingArea).coerceAtLeast(0.0), useMetric)} (net)")
                }
            }
        }

        item {
            WorkspaceSectionHeading(
                title = "Openings",
                detail = "Door, window, and stair placements linked to each measured wall."
            )
        }
        items(document.openings, key = { it.id }) { opening ->
            Card(
                colors = appCardColors(),
                border = appCardBorder(),
                elevation = appCardElevation()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${opening.type.name} ${opening.id}", fontWeight = FontWeight.SemiBold)
                    Text("Wall ID: ${opening.wallId} @ t=${"%.2f".format(opening.t)}")
                    Text(
                        "Size: ${Formatters.formatLength(Millimeters(opening.widthMm), useMetric)} × " +
                            Formatters.formatLength(Millimeters(opening.heightMm), useMetric)
                    )
                }
            }
        }

        item {
            WorkspaceSectionHeading(
                title = "Trade Breakdown",
                detail = "Calculated quantities and trace references for every supported trade."
            )
        }
        item {
            ReviewTradeCard("Drywall", drywall)
        }
        item {
            ReviewTradeCard("Concrete", concrete)
        }
        item {
            ReviewTradeCard("Gravel / Mulch", gravel)
        }
        item {
            ReviewTradeCard("Paint", paint)
        }
    }
}

@Composable
private fun ReviewTradeCard(
    label: String,
    result: com.tradesketch.estimator.domain.model.TakeoffResult
) {
    Card(
        colors = appCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
        ),
        elevation = appCardElevation()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            result.items.forEach { item ->
                Text("${item.name}: ${"%.2f".format(item.quantity)} ${item.unit}")
            }
            if (result.traces.isNotEmpty()) {
                Text("Trace IDs:", style = MaterialTheme.typography.labelLarge)
                result.traces.take(5).forEach { trace ->
                    Text(
                        text = "${trace.metric} ${"%.2f".format(trace.value)} ${trace.unit} [room=${trace.roomId ?: "-"}, wall=${trace.wallId ?: "-"}, opening=${trace.openingId ?: "-"}]",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
