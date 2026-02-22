package com.tradesketch.estimator.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.ui.viewmodel.buildTakeoffInputs
import com.tradesketch.estimator.ui.viewmodel.calculateForType

@Composable
fun ReviewScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    viewModel: BlueprintEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }

    val document = uiState.document
    val project = uiState.project
    if (uiState.isLoading || document == null || project == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val calculator = remember { CalculateTakeoffUseCase() }
    val inputs = remember(project, uiState.project?.takeoffSession) {
        buildTakeoffInputs(project = project, settings = Settings.DEFAULT)
    }
    val drywall = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.DRYWALL, inputs = inputs)
    }
    val concrete = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.CONCRETE, inputs = inputs)
    }
    val paint = remember(project, inputs) {
        calculator.calculateForType(project = project, type = TakeoffType.PAINT, inputs = inputs)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Review", style = MaterialTheme.typography.titleLarge)
                    Text("Blueprint-derived quantities by room, wall, and opening IDs.")
                    Text("Project: ${uiState.project?.name.orEmpty()}", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Text("Rooms", style = MaterialTheme.typography.titleMedium)
        }
        items(document.rooms, key = { it.id }) { room ->
            Card {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${room.name} (${room.id})", fontWeight = FontWeight.SemiBold)
                    Text("Area: ${"%.1f".format(room.areaSqFt())} sq ft")
                    Text("Perimeter: ${"%.1f".format(room.perimeterFeet())} ft")
                    if (room.tags.isNotEmpty()) {
                        Text("Tags: ${room.tags.joinToString()}")
                    }
                }
            }
        }

        item {
            Text("Walls", style = MaterialTheme.typography.titleMedium)
        }
        items(document.walls, key = { it.id }) { wall ->
            val openingArea = document.openings
                .filter { it.wallId == wall.id }
                .sumOf { Millimeters(it.widthMm).toFeet() * Millimeters(it.heightMm).toFeet() }
            val wallArea = Millimeters(wall.lengthMillimeters()).toFeet() * Millimeters(wall.heightMm).toFeet()
            Card {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Wall ${wall.id}", fontWeight = FontWeight.SemiBold)
                    Text("Length: ${"%.2f".format(Millimeters(wall.lengthMillimeters()).toFeet())} ft")
                    Text("Height: ${"%.2f".format(Millimeters(wall.heightMm).toFeet())} ft")
                    Text("Area: ${"%.1f".format((wallArea - openingArea).coerceAtLeast(0.0))} sq ft (net)")
                }
            }
        }

        item {
            Text("Openings", style = MaterialTheme.typography.titleMedium)
        }
        items(document.openings, key = { it.id }) { opening ->
            Card {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${opening.type.name} ${opening.id}", fontWeight = FontWeight.SemiBold)
                    Text("Wall ID: ${opening.wallId} @ t=${"%.2f".format(opening.t)}")
                    Text("Size: ${"%.2f".format(Millimeters(opening.widthMm).toFeet())}ft × ${"%.2f".format(Millimeters(opening.heightMm).toFeet())}ft")
                }
            }
        }

        item {
            Text("Trade Breakdown", style = MaterialTheme.typography.titleMedium)
        }
        item {
            ReviewTradeCard("Drywall", drywall)
        }
        item {
            ReviewTradeCard("Concrete", concrete)
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
