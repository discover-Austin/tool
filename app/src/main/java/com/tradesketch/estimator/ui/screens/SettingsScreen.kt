package com.tradesketch.estimator.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.tradesketch.estimator.ui.components.DangerActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.R
import com.tradesketch.estimator.BuildConfig
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.ui.components.AppFilterChip
import com.tradesketch.estimator.ui.components.BufferedInputField
import com.tradesketch.estimator.ui.components.ReferenceSectionFrame
import com.tradesketch.estimator.ui.components.ReferenceWorkspaceBackdrop
import com.tradesketch.estimator.ui.components.ReferenceWorksheetPanel
import com.tradesketch.estimator.ui.components.ReferenceWorksheetTitleBar
import com.tradesketch.estimator.ui.components.SettingSliderRow
import com.tradesketch.estimator.ui.components.SettingSwitchRow
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
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    ReferenceWorkspaceBackdrop(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 48.dp, end = 12.dp, bottom = 12.dp)
        ) {
            ReferenceWorksheetPanel(modifier = Modifier.fillMaxSize()) {
            ReferenceWorksheetTitleBar(
                title = "Settings",
                subtitle = "Project defaults, drawing controls, and estimating preferences.",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ReferenceSectionFrame(
                    title = "Project Defaults"
                ) {
                Text(
                    text = "Starting values for new projects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Default Trade",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set the starting trade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryTrade.entries.forEach { trade ->
                        AppFilterChip(
                            selected = uiState.settings.primaryTrade == trade,
                            onClick = {
                                haptics.tap()
                                viewModel.updatePrimaryTrade(trade)
                            },
                            label = trade.displayLabel()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingSwitchRow(
                    title = "Metric Units",
                    summary = "Use metric measurements.",
                    checked = uiState.settings.useMetric,
                    onCheckedChange = {
                        haptics.tap()
                        viewModel.updateUseMetric(it)
                    }
                )
                SettingSwitchRow(
                    title = "Reduced Motion",
                    summary = "Minimize interface animation.",
                    checked = uiState.settings.reducedMotionEnabled,
                    onCheckedChange = {
                        haptics.tap()
                        viewModel.updateReducedMotionEnabled(it)
                    },
                        showDivider = false
                )
            }
                BlueprintControlsCard(
                    settings = uiState.settings,
                    useMetric = uiState.settings.useMetric,
                    onUpdateBlueprintSnapDefaults = viewModel::updateBlueprintSnapDefaults,
                    onUpdateBlueprintControlDefaults = viewModel::updateBlueprintControlDefaults,
                    onHapticTap = haptics::tap
                )

                ReferenceSectionFrame(
                    title = "Quantity Inputs"
                ) {
                Text(
                    text = "Default estimating values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

                ReferenceSectionFrame(
                    title = "Pricing Defaults"
                ) {
                Text(
                    text = "Default unit costs and percentages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

                ReferenceSectionFrame(
                    title = "Business Identity"
                ) {
                Text(
                    text = "Contact details for exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

                ReferenceSectionFrame(
                    title = "Help & Support"
                ) {
                Text(
                    text = "Support and app info.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onReplayTutorial != null) {
                    QuietActionButton(
                        onClick = {
                            haptics.tap()
                            onReplayTutorial()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay Guided Tour")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SecondaryActionButton(
                    onClick = {
                        haptics.tap()
                        launchFeedbackEmail(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.send_feedback))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.feedback_section_message, stringResource(R.string.support_email)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TradeSketch Estimator v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "All project data stays on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

                QuietActionButton(
                    onClick = {
                        haptics.tap()
                        showResetConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Settings")
                }

                uiState.error?.let { error ->
                Card(
                    colors = appCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = appCardBorder(),
                    elevation = appCardElevation()
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
    }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_settings_title)) },
            text = { Text(stringResource(R.string.reset_settings_message)) },
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
        Float?,
        Float?,
        Boolean?,
        Float?
    ) -> Unit,
    onHapticTap: () -> Unit = {}
) {
    ReferenceSectionFrame(
        title = "Blueprint Defaults"
    ) {
        Text(
            text = "Drawing and snap behavior.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
            title = "Grid Snap",
            summary = "Align to the grid.",
            checked = settings.blueprintSnapGridEnabled,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintSnapDefaults(it, null, null, null, null, null)
            }
        )
        SettingSwitchRow(
            title = "Endpoint Snap",
            summary = "Snap to wall and opening ends.",
            checked = settings.blueprintSnapEndpointEnabled,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintSnapDefaults(null, it, null, null, null, null)
            }
        )
        SettingSwitchRow(
            title = "Midpoint Snap",
            summary = "Snap to center points.",
            checked = settings.blueprintSnapMidpointEnabled,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintSnapDefaults(null, null, it, null, null, null)
            }
        )
        SettingSwitchRow(
            title = "Angle Snap",
            summary = "Hold common angles.",
            checked = settings.blueprintSnapAngleEnabled,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintSnapDefaults(null, null, null, it, null, null)
            }
        )
        SettingSwitchRow(
            title = "Room Closure Snap",
            summary = "Close room outlines cleanly.",
            checked = settings.blueprintSnapClosureEnabled,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintSnapDefaults(null, null, null, null, it, null)
            }
        )
        SettingSwitchRow(
            title = "Cursor Marker",
            summary = "Show the active draft marker.",
            checked = settings.blueprintCursorVisible,
            onCheckedChange = {
                onHapticTap()
                onUpdateBlueprintControlDefaults(null, null, it, null)
            },
            showDivider = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingSliderRow(
            title = "Snap Sensitivity",
            summary = "Snap distance.",
            valueLabel = Formatters.formatSnapDistance(settings.blueprintSnapThresholdFeet, useMetric),
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
        SettingSliderRow(
            title = "Marker Size",
            summary = "Draft marker scale.",
            valueLabel = "${(settings.blueprintCursorScale * 100f).toInt()}%",
            value = settings.blueprintCursorScale.coerceIn(0.75f, 2.1f),
            onValueChange = {
                onUpdateBlueprintControlDefaults(null, null, null, it.coerceIn(0.75f, 2.1f))
            },
            valueRange = 0.75f..2.1f,
            steps = 12,
            enabled = settings.blueprintCursorVisible
        )
        SettingSliderRow(
            title = "Joystick Sensitivity",
            summary = "Joystick speed.",
            valueLabel = "${"%.2f".format(settings.blueprintJoystickSensitivity)}x",
            value = settings.blueprintJoystickSensitivity.coerceIn(0.55f, 2.2f),
            onValueChange = {
                onUpdateBlueprintControlDefaults(
                    it.coerceIn(0.55f, 2.2f),
                    null,
                    null,
                    null
                )
            },
            valueRange = 0.55f..2.2f,
            steps = 16
        )
        SettingSliderRow(
            title = "Joystick Deadzone",
            summary = "Ignored thumb travel.",
            valueLabel = "${(settings.blueprintJoystickDeadzone * 100f).toInt()}%",
            value = settings.blueprintJoystickDeadzone.coerceIn(0.08f, 0.30f),
            onValueChange = {
                onUpdateBlueprintControlDefaults(
                    null,
                    it.coerceIn(0.08f, 0.30f),
                    null,
                    null
                )
            },
            valueRange = 0.08f..0.30f,
            steps = 10,
            showDivider = false
        )
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

private fun launchFeedbackEmail(context: Context) {
    val supportEmail = context.getString(R.string.support_email)
    val subject = context.getString(R.string.feedback_email_subject, BuildConfig.VERSION_NAME)
    val body = buildString {
        appendLine(context.getString(R.string.feedback_email_prompt))
        appendLine()
        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    }
    val intent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:${Uri.encode(supportEmail)}")
    ).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        val message = if (error is ActivityNotFoundException) {
            context.getString(R.string.feedback_no_mail_app, supportEmail)
        } else {
            context.getString(R.string.feedback_open_failed, supportEmail)
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

