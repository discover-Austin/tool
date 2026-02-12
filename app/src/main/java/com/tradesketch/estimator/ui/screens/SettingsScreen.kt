package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val pricingValues = listOf(
        uiState.settings.drywallSheetUnitCost,
        uiState.settings.drywallScrewUnitCost,
        uiState.settings.drywallMudUnitCost,
        uiState.settings.concreteYardUnitCost,
        uiState.settings.gravelYardUnitCost,
        uiState.settings.gravelTonUnitCost,
        uiState.settings.paintGallonUnitCost
    )
    val pricingReady = pricingValues.count { it > 0.0 }
    val readinessPercent = (pricingReady * 100) / pricingValues.size

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AnimatedEntry(delayMs = 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tune defaults once and speed up every estimate.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Readiness: $readinessPercent%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$pricingReady / ${pricingValues.size} pricing defaults configured",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "General",
                subtitle = "Global assumptions and unit behavior."
            ) {
                BufferedDoubleField(
                    label = "Default Waste %",
                    initial = uiState.settings.defaultWastePercent.toString(),
                    hint = "Typical: 5-15",
                    onValidValue = { viewModel.updateDefaultWaste(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use Metric Units", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Affects dimensions and labels in new workflows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.settings.useMetric,
                        onCheckedChange = {
                            haptics.tap()
                            viewModel.updateUseMetric(it)
                        }
                    )
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Drywall Defaults",
                subtitle = "Used by drywall takeoff and export calculations."
            ) {
                BufferedDoubleField(
                    label = "Sheet Area (sq ft)",
                    initial = uiState.settings.defaultDrywallSheetArea.toString(),
                    hint = "Default: 32",
                    onValidValue = { viewModel.updateDrywallDefaults(sheetArea = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedIntField(
                    label = "Screws per Sheet",
                    initial = uiState.settings.defaultScrewsPerSheet.toString(),
                    hint = "Default: 32",
                    onValidValue = { viewModel.updateDrywallDefaults(screwsPerSheet = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Mud (gal / 100 sq ft)",
                    initial = uiState.settings.defaultMudGallonsPer100SqFt.toString(),
                    hint = "Default: 0.5",
                    onValidValue = { viewModel.updateDrywallDefaults(mudGallons = it) }
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Paint Defaults",
                subtitle = "Coverage assumptions for paint takeoffs."
            ) {
                BufferedDoubleField(
                    label = "Coverage (sq ft / gallon)",
                    initial = uiState.settings.defaultCoveragePerGallon.toString(),
                    hint = "Default: 350",
                    onValidValue = { viewModel.updatePaintDefaults(coverage = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedIntField(
                    label = "Number of Coats",
                    initial = uiState.settings.defaultCoatsOfPaint.toString(),
                    hint = "Default: 2",
                    onValidValue = { viewModel.updatePaintDefaults(coats = it) }
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Pricing Defaults",
                subtitle = "Cost baselines used across takeoff and export."
            ) {
                BufferedDoubleField(
                    label = "Drywall Sheet ($/sheet)",
                    initial = uiState.settings.drywallSheetUnitCost.toString(),
                    hint = "Material cost",
                    onValidValue = { viewModel.updatePricingDefaults(drywallSheetCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Drywall Screw ($/screw)",
                    initial = uiState.settings.drywallScrewUnitCost.toString(),
                    hint = "Fastener cost",
                    onValidValue = { viewModel.updatePricingDefaults(drywallScrewCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Joint Compound ($/gallon)",
                    initial = uiState.settings.drywallMudUnitCost.toString(),
                    hint = "Mud cost",
                    onValidValue = { viewModel.updatePricingDefaults(drywallMudCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Concrete ($/cubic yard)",
                    initial = uiState.settings.concreteYardUnitCost.toString(),
                    hint = "Concrete cost",
                    onValidValue = { viewModel.updatePricingDefaults(concreteYardCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Gravel/Mulch ($/cubic yard)",
                    initial = uiState.settings.gravelYardUnitCost.toString(),
                    hint = "Volume cost",
                    onValidValue = { viewModel.updatePricingDefaults(gravelYardCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Gravel/Mulch ($/ton)",
                    initial = uiState.settings.gravelTonUnitCost.toString(),
                    hint = "Weight cost",
                    onValidValue = { viewModel.updatePricingDefaults(gravelTonCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Paint ($/gallon)",
                    initial = uiState.settings.paintGallonUnitCost.toString(),
                    hint = "Paint cost",
                    onValidValue = { viewModel.updatePricingDefaults(paintGallonCost = it) }
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Business Defaults",
                subtitle = "Margins and tax used in grand total calculations."
            ) {
                BufferedDoubleField(
                    label = "Labor %",
                    initial = uiState.settings.laborPercent.toString(),
                    hint = "Labor overhead",
                    onValidValue = { viewModel.updateBusinessDefaults(laborPercent = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Markup %",
                    initial = uiState.settings.markupPercent.toString(),
                    hint = "Margin uplift",
                    onValidValue = { viewModel.updateBusinessDefaults(markupPercent = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Tax %",
                    initial = uiState.settings.taxPercent.toString(),
                    hint = "Applied tax",
                    onValidValue = { viewModel.updateBusinessDefaults(taxPercent = it) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    haptics.tap()
                    showResetConfirm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All Defaults")
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
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
                        text = "Privacy: all data stays on your device. No tracking, no ads, no account required.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        uiState.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Defaults") },
            text = { Text("Reset all settings back to factory defaults?") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        viewModel.resetToDefaults()
                        showResetConfirm = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BufferedDoubleField(
    label: String,
    initial: String,
    hint: String,
    onValidValue: (Double) -> Unit
) {
    var text by rememberSaveable(label) { mutableStateOf(initial) }
    LaunchedEffect(initial) {
        if (initial != text) {
            text = initial
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.takeIf { value -> value >= 0.0 }?.let(onValidValue)
        },
        label = { Text(label) },
        placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BufferedIntField(
    label: String,
    initial: String,
    hint: String,
    onValidValue: (Int) -> Unit
) {
    var text by rememberSaveable(label) { mutableStateOf(initial) }
    LaunchedEffect(initial) {
        if (initial != text) {
            text = initial
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.takeIf { value -> value >= 0 }?.let(onValidValue)
        },
        label = { Text(label) },
        placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
