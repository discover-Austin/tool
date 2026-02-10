package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        // General Settings
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultWastePercent.toString(),
                        onValueChange = { 
                            it.toDoubleOrNull()?.let { value ->
                                viewModel.updateDefaultWaste(value)
                            }
                        },
                        label = { Text("Default Waste %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Use Metric Units")
                        Switch(
                            checked = uiState.settings.useMetric,
                            onCheckedChange = { viewModel.updateUseMetric(it) }
                        )
                    }
                }
            }
        }
        
        // Drywall Defaults
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Drywall Defaults",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultDrywallSheetArea.toString(),
                        onValueChange = { 
                            it.toDoubleOrNull()?.let { value ->
                                viewModel.updateDrywallDefaults(sheetArea = value)
                            }
                        },
                        label = { Text("Sheet Area (sq ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultScrewsPerSheet.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                viewModel.updateDrywallDefaults(screwsPerSheet = value)
                            }
                        },
                        label = { Text("Screws per Sheet") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultMudGallonsPer100SqFt.toString(),
                        onValueChange = { 
                            it.toDoubleOrNull()?.let { value ->
                                viewModel.updateDrywallDefaults(mudGallons = value)
                            }
                        },
                        label = { Text("Mud (gal/100 sq ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Paint Defaults
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paint Defaults",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultCoveragePerGallon.toString(),
                        onValueChange = { 
                            it.toDoubleOrNull()?.let { value ->
                                viewModel.updatePaintDefaults(coverage = value)
                            }
                        },
                        label = { Text("Coverage (sq ft/gallon)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.settings.defaultCoatsOfPaint.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                viewModel.updatePaintDefaults(coats = value)
                            }
                        },
                        label = { Text("Number of Coats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Actions
        item {
            OutlinedButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
        
        // About
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TradeSketch Estimator v1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Material takeoff calculator for skilled trades",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Privacy: All data stays on your device. No tracking, no ads, no accounts.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
