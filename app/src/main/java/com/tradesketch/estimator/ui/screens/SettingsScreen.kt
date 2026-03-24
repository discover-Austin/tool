package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.DangerActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.BuildConfig
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.ui.components.BufferedInputField
import com.tradesketch.estimator.ui.components.TitledSectionCard
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel
import com.tradesketch.estimator.utils.Formatters

@Composable
fun SettingsScreen(
    onReplayTutorial: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = appCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = appCardBorder(accented = true),
                elevation = appCardElevation()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Keep only what matters: project preferences, quantities, and pricing.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            TitledSectionCard(
                title = "Core Preferences",
                subtitle = "Used across projects and tabs."
            ) {
                Text(
                    text = "Primary Trade",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryTrade.entries.forEach { trade ->
                        FilterChip(
                            selected = uiState.settings.primaryTrade == trade,
                            onClick = {
                                haptics.tap()
                                viewModel.updatePrimaryTrade(trade)
                            },
                            label = { Text(trade.displayLabel()) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use Metric Units", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Controls blueprint dimensions, geometry previews, and review readouts. Quantity inputs stay in estimating units.",
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Disables extra animation for a faster interface.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.settings.reducedMotionEnabled,
                        onCheckedChange = {
                            haptics.tap()
                            viewModel.updateReducedMotionEnabled(it)
                        }
                    )
                }
            }
        }

        if (onReplayTutorial != null) {
            item {
                TitledSectionCard(
                    title = "Help & Onboarding",
                    subtitle = "Replay the blueprint control tour for the current input mode."
                ) {
                    Text(
                        text = "Need a refresher? Replay the control tour to spotlight each blueprint control with brief guidance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    QuietActionButton(
                        onClick = {
                            haptics.tap()
                            onReplayTutorial()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay Control Tour")
                    }
                }
            }
        }

        item {
            BlueprintControlsCard(
                settings = uiState.settings,
                useMetric = uiState.settings.useMetric,
                onUpdateBlueprintSnapDefaults = viewModel::updateBlueprintSnapDefaults,
                onUpdateBlueprintControlDefaults = viewModel::updateBlueprintControlDefaults,
                onHapticTap = haptics::tap
            )
        }

        item {
            TitledSectionCard(
                title = "Quantity Inputs",
                subtitle = "Starting assumptions used when creating estimates. These stay in estimating units."
            ) {
                BufferedDoubleField(
                    label = "Waste %",
                    initial = uiState.settings.defaultWastePercent.toString(),
                    hint = "Typical: 5-15",
                    onValidValue = { viewModel.updateDefaultWaste(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Drywall Sheet Area (sq ft)",
                    initial = uiState.settings.defaultDrywallSheetArea.toString(),
                    hint = "Typical: 32",
                    onValidValue = { viewModel.updateDrywallDefaults(sheetArea = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedIntField(
                    label = "Screws per Sheet",
                    initial = uiState.settings.defaultScrewsPerSheet.toString(),
                    hint = "Typical: 28-36",
                    onValidValue = { viewModel.updateDrywallDefaults(screwsPerSheet = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Mud (gal / 100 sq ft)",
                    initial = uiState.settings.defaultMudGallonsPer100SqFt.toString(),
                    hint = "Typical: 0.5",
                    onValidValue = { viewModel.updateDrywallDefaults(mudGallons = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Paint Coverage (sq ft / gallon)",
                    initial = uiState.settings.defaultCoveragePerGallon.toString(),
                    hint = "Typical: 300-400",
                    onValidValue = { viewModel.updatePaintDefaults(coverage = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedIntField(
                    label = "Paint Coats",
                    initial = uiState.settings.defaultCoatsOfPaint.toString(),
                    hint = "Typical: 2",
                    onValidValue = { viewModel.updatePaintDefaults(coats = it) }
                )
            }
        }

        item {
            TitledSectionCard(
                title = "Pricing & Margins",
                subtitle = "Material and business values used in totals."
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
                    label = "Mud ($/gallon)",
                    initial = uiState.settings.drywallMudUnitCost.toString(),
                    hint = "Compound cost",
                    onValidValue = { viewModel.updatePricingDefaults(drywallMudCost = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Concrete ($/cubic yard)",
                    initial = uiState.settings.concreteYardUnitCost.toString(),
                    hint = "Batch cost",
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
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Labor %",
                    initial = uiState.settings.laborPercent.toString(),
                    hint = "Crew + overhead",
                    onValidValue = { viewModel.updateBusinessDefaults(laborPercent = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Markup %",
                    initial = uiState.settings.markupPercent.toString(),
                    hint = "Margin add",
                    onValidValue = { viewModel.updateBusinessDefaults(markupPercent = it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedDoubleField(
                    label = "Tax %",
                    initial = uiState.settings.taxPercent.toString(),
                    hint = "Final tax",
                    onValidValue = { viewModel.updateBusinessDefaults(taxPercent = it) }
                )
            }
        }

        item {
            TitledSectionCard(
                title = "Business Identity",
                subtitle = "Printed and shared estimate header details."
            ) {
                BufferedTextField(
                    label = "Business Name",
                    initial = uiState.settings.businessName,
                    hint = "Your company name",
                    onValueChange = { viewModel.updateBusinessIdentity(businessName = it.trim()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedTextField(
                    label = "Phone",
                    initial = uiState.settings.businessPhone,
                    hint = "(555) 555-5555",
                    onValueChange = { viewModel.updateBusinessIdentity(businessPhone = it.trim()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedTextField(
                    label = "Email",
                    initial = uiState.settings.businessEmail,
                    hint = "estimating@company.com",
                    onValueChange = { viewModel.updateBusinessIdentity(businessEmail = it.trim()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedTextField(
                    label = "Address",
                    initial = uiState.settings.businessAddress,
                    hint = "Street, City, State ZIP",
                    onValueChange = { viewModel.updateBusinessIdentity(businessAddress = it.trim()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BufferedTextField(
                    label = "License # (optional)",
                    initial = uiState.settings.businessLicense,
                    hint = "Contractor license",
                    onValueChange = { viewModel.updateBusinessIdentity(businessLicense = it.trim()) }
                )
            }
        }

        item {
            QuietActionButton(
                onClick = {
                    haptics.tap()
                    showResetConfirm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Settings")
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
                        text = "TradeSketch Estimator v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All project data stays on your device.",
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
            title = { Text("Reset Settings") },
            text = { Text("Reset all values back to factory defaults?") },
            confirmButton = {
                DangerActionButton(
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
                QuietActionButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
internal fun BlueprintControlsCard(
    settings: Settings,
    useMetric: Boolean,
    onUpdateBlueprintSnapDefaults: (
        Boolean?,
        Boolean?,
        Boolean?,
        Boolean?,
        Boolean?,
        Double?
    ) -> Unit,
    onUpdateBlueprintControlDefaults: (
        Boolean?,
        Float?,
        Float?,
        Boolean?,
        Float?
    ) -> Unit,
    onHapticTap: () -> Unit = {}
) {
    TitledSectionCard(
        title = "Blueprint Controls",
        subtitle = "Snap and control-mode defaults for the Blueprint tab."
    ) {
        Text(
            text = "Snap Toggles",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = settings.blueprintSnapGridEnabled,
                onClick = {
                    onHapticTap()
                    onUpdateBlueprintSnapDefaults(
                        !settings.blueprintSnapGridEnabled,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                },
                label = { Text("Grid") }
            )
            FilterChip(
                selected = settings.blueprintSnapAngleEnabled,
                onClick = {
                    onHapticTap()
                    onUpdateBlueprintSnapDefaults(
                        null,
                        null,
                        null,
                        !settings.blueprintSnapAngleEnabled,
                        null,
                        null
                    )
                },
                label = { Text("Angle") }
            )
            FilterChip(
                selected = settings.blueprintSnapEndpointEnabled,
                onClick = {
                    onHapticTap()
                    onUpdateBlueprintSnapDefaults(
                        null,
                        !settings.blueprintSnapEndpointEnabled,
                        null,
                        null,
                        null,
                        null
                    )
                },
                label = { Text("Endpoints") }
            )
            FilterChip(
                selected = settings.blueprintSnapMidpointEnabled,
                onClick = {
                    onHapticTap()
                    onUpdateBlueprintSnapDefaults(
                        null,
                        null,
                        !settings.blueprintSnapMidpointEnabled,
                        null,
                        null,
                        null
                    )
                },
                label = { Text("Midpoints") }
            )
            FilterChip(
                selected = settings.blueprintSnapClosureEnabled,
                onClick = {
                    onHapticTap()
                    onUpdateBlueprintSnapDefaults(
                        null,
                        null,
                        null,
                        null,
                        !settings.blueprintSnapClosureEnabled,
                        null
                    )
                },
                label = { Text("Room closure") }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Snap sensitivity: ${Formatters.formatSnapDistance(settings.blueprintSnapThresholdFeet, useMetric)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = settings.blueprintSnapThresholdFeet.coerceIn(0.2, 2.0).toFloat(),
            onValueChange = {
                onUpdateBlueprintSnapDefaults(
                    null,
                    null,
                    null,
                    null,
                    null,
                    it.coerceIn(0.2f, 2.0f).toDouble()
                )
            },
            valueRange = 0.2f..2.0f
        )
        Text(
            text = "Lower values require closer alignment. Higher values snap more aggressively.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Control mode",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !settings.blueprintDualJoysticksEnabled,
                onClick = {
                    if (settings.blueprintDualJoysticksEnabled) {
                        onHapticTap()
                        onUpdateBlueprintControlDefaults(false, null, null, null, null)
                    }
                },
                label = { Text("Touch mode") }
            )
            FilterChip(
                selected = settings.blueprintDualJoysticksEnabled,
                onClick = {
                    if (!settings.blueprintDualJoysticksEnabled) {
                        onHapticTap()
                        onUpdateBlueprintControlDefaults(true, null, null, null, null)
                    }
                },
                label = { Text("Dual joysticks") }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cursor marker", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Shows the active draft marker and highlights precise geometry targets while you edit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.blueprintCursorVisible,
                onCheckedChange = {
                    onHapticTap()
                    onUpdateBlueprintControlDefaults(null, null, null, it, null)
                }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Marker size: ${(settings.blueprintCursorScale * 100f).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = settings.blueprintCursorScale.coerceIn(0.75f, 2.1f),
            onValueChange = {
                onUpdateBlueprintControlDefaults(null, null, null, null, it.coerceIn(0.75f, 2.1f))
            },
            valueRange = 0.75f..2.1f,
            steps = 12,
            enabled = settings.blueprintCursorVisible
        )
        Text(
            text = "Smaller keeps the blueprint cleaner. Larger makes the active marker easier to follow.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (settings.blueprintDualJoysticksEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Joystick sensitivity: ${"%.2f".format(settings.blueprintJoystickSensitivity)}x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.blueprintJoystickSensitivity.coerceIn(0.55f, 2.2f),
                onValueChange = {
                    onUpdateBlueprintControlDefaults(
                        null,
                        it.coerceIn(0.55f, 2.2f),
                        null,
                        null,
                        null
                    )
                },
                valueRange = 0.55f..2.2f,
                steps = 16
            )
            Text(
                text = "Joystick deadzone: ${(settings.blueprintJoystickDeadzone * 100f).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.blueprintJoystickDeadzone.coerceIn(0.08f, 0.30f),
                onValueChange = {
                    onUpdateBlueprintControlDefaults(
                        null,
                        null,
                        it.coerceIn(0.08f, 0.30f),
                        null,
                        null
                    )
                },
                valueRange = 0.08f..0.30f,
                steps = 10
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Joystick tuning appears when Dual joysticks is active.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BufferedTextField(
    label: String,
    initial: String,
    hint: String,
    onValueChange: (String) -> Unit
) {
    BufferedField(
        label = label,
        initial = initial,
        hint = hint,
        keyboardType = KeyboardType.Text,
        onTextChanged = onValueChange
    )
}

@Composable
private fun BufferedDoubleField(
    label: String,
    initial: String,
    hint: String,
    onValidValue: (Double) -> Unit
) {
    BufferedField(
        label = label,
        initial = initial,
        hint = hint,
        keyboardType = KeyboardType.Decimal,
        onTextChanged = { text ->
            text.toDoubleOrNull()
                ?.takeIf { value -> value >= 0.0 }
                ?.let(onValidValue)
        }
    )
}

@Composable
private fun BufferedIntField(
    label: String,
    initial: String,
    hint: String,
    onValidValue: (Int) -> Unit
) {
    BufferedField(
        label = label,
        initial = initial,
        hint = hint,
        keyboardType = KeyboardType.Number,
        onTextChanged = { text ->
            text.toIntOrNull()
                ?.takeIf { value -> value >= 0 }
                ?.let(onValidValue)
        }
    )
}

@Composable
private fun BufferedField(
    label: String,
    initial: String,
    hint: String,
    keyboardType: KeyboardType,
    onTextChanged: (String) -> Unit
) {
    BufferedInputField(
        label = label,
        initialValue = initial,
        hint = hint,
        keyboardType = keyboardType,
        onValueChange = onTextChanged
    )
}

