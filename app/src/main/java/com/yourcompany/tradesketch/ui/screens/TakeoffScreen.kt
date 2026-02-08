package com.yourcompany.tradesketch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.tradesketch.ui.viewmodel.TakeoffType
import com.yourcompany.tradesketch.ui.viewmodel.TakeoffViewModel
import com.yourcompany.tradesketch.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeoffScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    viewModel: TakeoffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Type Selector
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Takeoff Type",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TakeoffType.values().forEach { type ->
                            FilterChip(
                                selected = uiState.selectedType == type,
                                onClick = { viewModel.selectTakeoffType(type) },
                                label = { Text(type.name.replace("_", " ")) }
                            )
                        }
                    }
                }
            }
        }
        
        // Parameters based on selected type
        uiState.selectedType?.let { type ->
            when (type) {
                TakeoffType.DRYWALL -> {
                    item {
                        DrywallParameters(
                            sheetArea = uiState.drywallParams.sheetAreaSqFt,
                            wastePercent = uiState.drywallParams.wastePercent,
                            screwsPerSheet = uiState.drywallParams.screwsPerSheet,
                            mudGallons = uiState.drywallParams.mudGallonsPer100SqFt,
                            onSheetAreaChange = { viewModel.updateDrywallParams(sheetAreaSqFt = it) },
                            onWasteChange = { viewModel.updateDrywallParams(wastePercent = it) },
                            onScrewsChange = { viewModel.updateDrywallParams(screwsPerSheet = it) },
                            onMudChange = { viewModel.updateDrywallParams(mudGallonsPer100SqFt = it) }
                        )
                    }
                }
                TakeoffType.CONCRETE -> {
                    item {
                        ConcreteParameters(
                            thickness = uiState.concreteParams.thicknessFeet,
                            wastePercent = uiState.concreteParams.wastePercent,
                            onThicknessChange = { viewModel.updateConcreteParams(thicknessFeet = it) },
                            onWasteChange = { viewModel.updateConcreteParams(wastePercent = it) }
                        )
                    }
                }
                TakeoffType.GRAVEL_MULCH -> {
                    item {
                        GravelParameters(
                            depth = uiState.gravelParams.depthFeet,
                            density = uiState.gravelParams.densityTonsPerYard,
                            wastePercent = uiState.gravelParams.wastePercent,
                            onDepthChange = { viewModel.updateGravelParams(depthFeet = it) },
                            onDensityChange = { viewModel.updateGravelParams(densityTonsPerYard = it) },
                            onWasteChange = { viewModel.updateGravelParams(wastePercent = it) }
                        )
                    }
                }
                TakeoffType.PAINT -> {
                    item {
                        PaintParameters(
                            coverage = uiState.paintParams.coverageSqFtPerGallon,
                            coats = uiState.paintParams.coats,
                            wastePercent = uiState.paintParams.wastePercent,
                            onCoverageChange = { viewModel.updatePaintParams(coverageSqFtPerGallon = it) },
                            onCoatsChange = { viewModel.updatePaintParams(coats = it) },
                            onWasteChange = { viewModel.updatePaintParams(wastePercent = it) }
                        )
                    }
                }
            }
            
            // Results
            uiState.result?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Results",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            result.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = item.name)
                                    Text(text = "${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "⚠️ ESTIMATE ONLY - Verify quantities, measurements, and pricing with actual site conditions, local building codes, and material suppliers before purchasing or starting work.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrywallParameters(
    sheetArea: Double,
    wastePercent: Double,
    screwsPerSheet: Int,
    mudGallons: Double,
    onSheetAreaChange: (Double) -> Unit,
    onWasteChange: (Double) -> Unit,
    onScrewsChange: (Int) -> Unit,
    onMudChange: (Double) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Drywall Parameters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            ParameterField(
                label = "Sheet Area (sq ft)",
                value = sheetArea.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onSheetAreaChange) }
            )
            ParameterField(
                label = "Waste %",
                value = wastePercent.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onWasteChange) }
            )
            ParameterField(
                label = "Screws per Sheet",
                value = screwsPerSheet.toString(),
                onValueChange = { it.toIntOrNull()?.let(onScrewsChange) }
            )
            ParameterField(
                label = "Mud (gal per 100 sq ft)",
                value = mudGallons.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onMudChange) }
            )
        }
    }
}

@Composable
private fun ConcreteParameters(
    thickness: Double,
    wastePercent: Double,
    onThicknessChange: (Double) -> Unit,
    onWasteChange: (Double) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Concrete Parameters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            ParameterField(
                label = "Thickness (feet)",
                value = thickness.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onThicknessChange) }
            )
            ParameterField(
                label = "Waste %",
                value = wastePercent.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onWasteChange) }
            )
        }
    }
}

@Composable
private fun GravelParameters(
    depth: Double,
    density: Double,
    wastePercent: Double,
    onDepthChange: (Double) -> Unit,
    onDensityChange: (Double) -> Unit,
    onWasteChange: (Double) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gravel/Mulch Parameters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            ParameterField(
                label = "Depth (feet)",
                value = depth.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onDepthChange) }
            )
            ParameterField(
                label = "Density (tons/yard)",
                value = density.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onDensityChange) }
            )
            ParameterField(
                label = "Waste %",
                value = wastePercent.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onWasteChange) }
            )
        }
    }
}

@Composable
private fun PaintParameters(
    coverage: Double,
    coats: Int,
    wastePercent: Double,
    onCoverageChange: (Double) -> Unit,
    onCoatsChange: (Int) -> Unit,
    onWasteChange: (Double) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Paint Parameters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            ParameterField(
                label = "Coverage (sq ft/gallon)",
                value = coverage.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onCoverageChange) }
            )
            ParameterField(
                label = "Coats",
                value = coats.toString(),
                onValueChange = { it.toIntOrNull()?.let(onCoatsChange) }
            )
            ParameterField(
                label = "Waste %",
                value = wastePercent.toString(),
                onValueChange = { it.toDoubleOrNull()?.let(onWasteChange) }
            )
        }
    }
}

@Composable
private fun ParameterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
}
