package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.hasMeasuredQuantities
import com.tradesketch.estimator.domain.model.nonZeroItems
import com.tradesketch.estimator.ui.components.ReferenceActionButton
import com.tradesketch.estimator.ui.components.ReferenceBlueprintBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintInk
import com.tradesketch.estimator.ui.components.ReferenceBlueprintMuted
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavy
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaperAlt
import com.tradesketch.estimator.ui.components.ReferenceFooterNote
import com.tradesketch.estimator.ui.components.ReferenceSectionFrame
import com.tradesketch.estimator.ui.components.ReferenceWorkspaceBackdrop
import com.tradesketch.estimator.ui.components.ReferenceWorksheetPanel
import com.tradesketch.estimator.ui.components.ReferenceWorksheetTitleBar
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialBlipOverlay
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialProgress
import com.tradesketch.estimator.ui.tutorial.MaterialsGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.MaterialsGuidedTutorialTarget
import com.tradesketch.estimator.ui.viewmodel.ManualTakeoffParams
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.ui.viewmodel.TakeoffUiState
import com.tradesketch.estimator.ui.viewmodel.TakeoffViewModel
import com.tradesketch.estimator.ui.viewmodel.projectBlueprintForType
import com.tradesketch.estimator.utils.Formatters
import kotlinx.coroutines.delay

enum class TakeoffScreenMode {
    MATERIALS,
    QUANTITIES
}

@Composable
internal fun TakeoffScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    screenMode: TakeoffScreenMode = TakeoffScreenMode.MATERIALS,
    onBack: (() -> Unit)? = null,
    onOpenModel: () -> Unit = {},
    onOpenBlueprint: () -> Unit = {},
    onOpenMaterials: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    guidedTutorialStep: MaterialsGuidedTutorialStep? = null,
    guidedTutorialProgress: GuidedTutorialProgress? = null,
    onGuidedTutorialBack: (() -> Unit)? = null,
    onGuidedTutorialNext: (() -> Unit)? = null,
    onGuidedTutorialSkip: (() -> Unit)? = null,
    viewModel: TakeoffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTradePicker by rememberSaveable(projectId) { mutableStateOf(false) }
    var showDetailedResults by rememberSaveable(projectId) { mutableStateOf(false) }
    var estimateInputsBounds by remember { mutableStateOf<Rect?>(null) }
    var pricingBounds by remember { mutableStateOf<Rect?>(null) }
    var titleBarBounds by remember { mutableStateOf<Rect?>(null) }
    val isMaterialsMode = screenMode == TakeoffScreenMode.MATERIALS
    val density = LocalDensity.current

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.recordTap("takeoff_screen_opened")
    }

    val selectedType = uiState.selectedType
    val scopeSummary = selectedType?.let { type ->
        scopeSummaryForType(
            project = uiState.project,
            type = type,
            includeDrywallCeilings = uiState.drywallParams.includeCeilings,
            inputMode = uiState.inputMode,
            manualParams = uiState.manualParams
        )
    }
    val warnings = if (selectedType != null && scopeSummary != null) {
        takeoffWarnings(uiState, selectedType, scopeSummary)
    } else {
        emptyList()
    }
    val tutorialTargetBounds: List<Rect> = when (guidedTutorialStep?.target) {
        MaterialsGuidedTutorialTarget.ESTIMATE_INPUTS -> listOfNotNull(estimateInputsBounds)
        MaterialsGuidedTutorialTarget.PRICING -> listOfNotNull(pricingBounds)
        null -> emptyList()
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
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
                    title = if (isMaterialsMode) "Materials & Pricing" else "Quantity Review",
                    subtitle = if (isMaterialsMode) {
                        "Review measurements, pricing, and totals."
                    } else {
                        "Review measurements and quantities."
                    },
                    onBack = onBack,
                    modifier = Modifier.onGloballyPositioned {
                        titleBarBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ReferenceSectionFrame(title = "Input Method") {
                        Text(
                            text = "Choose Blueprint or Manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ReferenceBlueprintMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReferenceChoiceChip(
                                label = "Blueprint",
                                selected = uiState.inputMode == TakeoffInputMode.BLUEPRINT,
                                onClick = { viewModel.setInputMode(TakeoffInputMode.BLUEPRINT) }
                            )
                            ReferenceChoiceChip(
                                label = "Manual",
                                selected = uiState.inputMode == TakeoffInputMode.MANUAL,
                                onClick = { viewModel.setInputMode(TakeoffInputMode.MANUAL) }
                            )
                        }
                        HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.45f))

                        ReferenceMiniLabel("Trade")
                        Text(
                            text = "Active trade for this estimate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ReferenceBlueprintMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReferenceSelectionPill(
                                label = selectedType?.displayLabel ?: "Select Trade",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ReferenceUtilityButton(
                                label = "Change Trade",
                                onClick = { showTradePicker = true }
                            )
                        }
                    }

                    if (selectedType != null && scopeSummary != null) {
                        ReferenceSectionFrame(
                            title = "Estimate Inputs",
                            modifier = Modifier.onGloballyPositioned {
                                estimateInputsBounds = Rect(it.positionInRoot(), it.size.toSize())
                            }
                        ) {
                            Text(
                                text = "Measurements and quantity settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReferenceBlueprintMuted
                            )
                            Text(
                                text = "${scopeSummary.sourceCount} ${scopeSummary.sourceLabel} included in this estimate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReferenceBlueprintInk
                            )
                            Text(
                                text = "${scopeSummary.quantityLabel}: ${Formatters.formatQuantity(scopeSummary.measuredQuantity)} ${scopeSummary.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReferenceBlueprintInk
                            )
                            if (warnings.isNotEmpty()) {
                                ReferenceWarningBlock(warnings = warnings)
                            }

                            manualFieldSpecs(
                                uiState = uiState,
                                selectedType = selectedType,
                                viewModel = viewModel
                            ).takeIf { it.isNotEmpty() }?.let { fields ->
                                ReferenceFieldGroup(
                                    title = "Manual Measurements",
                                    fields = fields
                                )
                            }

                            ReferenceFieldGroup(
                                title = "${selectedType.displayLabel} Inputs",
                                fields = tradeFieldSpecs(
                                    uiState = uiState,
                                    selectedType = selectedType,
                                    viewModel = viewModel
                                )
                            )

                            if (selectedType == TakeoffType.DRYWALL) {
                                ReferenceToggleRow(
                                    label = "Include Ceilings",
                                    supporting = "Adds room ceiling surfaces to drywall totals.",
                                    selected = uiState.drywallParams.includeCeilings,
                                    onClick = {
                                        viewModel.updateDrywallParams(
                                            includeCeilings = !uiState.drywallParams.includeCeilings
                                        )
                                    }
                                )
                            }
                        }

                        if (isMaterialsMode) {
                            ReferenceSectionFrame(
                                title = "Pricing",
                                modifier = Modifier.onGloballyPositioned {
                                    pricingBounds = Rect(it.positionInRoot(), it.size.toSize())
                                }
                            ) {
                                Text(
                                    text = "Unit costs, labor, markup, tax.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ReferenceBlueprintMuted
                                )
                                ReferenceFieldGroup(
                                    title = "Pricing Details",
                                    fields = pricingFieldSpecs(
                                        uiState = uiState,
                                        selectedType = selectedType,
                                        viewModel = viewModel
                                    )
                                )
                            }
                        }

                        ReferenceSectionFrame(
                            title = "Results"
                        ) {
                            Text(
                                text = "Current estimate summary.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReferenceBlueprintMuted
                            )
                            ResultsSummaryBlock(
                                uiState = uiState,
                                selectedType = selectedType,
                                showDetailedResults = showDetailedResults,
                                isMaterialsMode = isMaterialsMode
                            )
                        }
                    } else {
                        ReferenceSectionFrame(
                            title = "Estimate Inputs",
                            modifier = Modifier.onGloballyPositioned {
                                estimateInputsBounds = Rect(it.positionInRoot(), it.size.toSize())
                            }
                        ) {
                            Text(
                                text = "Select a trade to load estimate inputs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReferenceBlueprintMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReferenceActionButton(
                            text = if (isMaterialsMode) "Continue to Export" else "Open Materials & Pricing",
                            onClick = {
                                if (isMaterialsMode) {
                                    viewModel.recordTap("takeoff_open_export")
                                    onOpenExport()
                                } else {
                                    viewModel.recordTap("quantities_open_materials")
                                    onOpenMaterials()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            emphasize = true
                        )
                        ReferenceActionButton(
                            text = if (showDetailedResults) "Hide Detailed Results" else "Show Detailed Results",
                            onClick = { showDetailedResults = !showDetailedResults },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    uiState.error?.let { error ->
                        ReferenceWarningBlock(warnings = listOf(error))
                    }

                    ReferenceFooterNote(
                        text = "Review assumptions before purchase or install."
                    )
                }
            }
            }
            if (
                guidedTutorialStep != null &&
                    guidedTutorialProgress != null &&
                    onGuidedTutorialNext != null &&
                    onGuidedTutorialSkip != null
            ) {
                GuidedTutorialBlipOverlay(
                    title = guidedTutorialStep.title,
                    message = guidedTutorialStep.message,
                    supporting = guidedTutorialStep.supporting,
                    progress = guidedTutorialProgress,
                    targetBounds = tutorialTargetBounds,
                    primaryActionLabel = guidedTutorialStep.primaryActionLabel,
                    minimumTopClearance = with(density) {
                        (((titleBarBounds?.bottom ?: 0f).toDp()) + 12.dp).coerceAtLeast(12.dp)
                    },
                    preferBottomPlacement = true,
                    onBack = onGuidedTutorialBack,
                    onNext = onGuidedTutorialNext,
                    onSkip = onGuidedTutorialSkip,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showTradePicker) {
        AlertDialog(
            onDismissRequest = { showTradePicker = false },
            title = { Text("Change Trade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose the trade this page should estimate.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TakeoffType.entries.forEach { type ->
                        ReferenceChoiceChip(
                            label = type.displayLabel,
                            selected = selectedType == type,
                            onClick = {
                                viewModel.selectTakeoffType(type)
                                showTradePicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTradePicker = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ReferenceMiniLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = ReferenceBlueprintInk
    )
}

@Composable
private fun ReferenceChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) ReferenceBlueprintNavy else Color(0xFFFBFAF4),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) ReferenceBlueprintBorder else ReferenceBlueprintBorder.copy(alpha = 0.75f)
        ),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else ReferenceBlueprintInk
        )
    }
}

@Composable
private fun ReferenceUtilityButton(
    label: String,
    emphasized: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (emphasized) ReferenceBlueprintNavy else Color(0xFFFBFAF4),
        border = BorderStroke(1.1.dp, if (emphasized) ReferenceBlueprintBorder else ReferenceBlueprintBorder.copy(alpha = 0.78f)),
        shadowElevation = if (emphasized) 2.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimary else ReferenceBlueprintInk
        )
    }
}

@Composable
private fun ReferenceSelectionPill(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFBFAF4),
        border = BorderStroke(1.1.dp, ReferenceBlueprintBorder.copy(alpha = 0.86f)),
        shadowElevation = 1.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = ReferenceBlueprintInk
        )
    }
}

@Composable
private fun ReferenceWarningBlock(warnings: List<String>) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Review Checks",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            warnings.forEach { warning ->
                Text(
                    text = "- $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ReferenceFieldGroup(
    title: String,
    fields: List<NumberFieldSpec>
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = ReferenceBlueprintInk
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        fields.forEachIndexed { index, field ->
            ReferenceCompactNumberRow(field = field)
            if (index < fields.lastIndex) {
                HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
private fun ReferenceCompactNumberRow(field: NumberFieldSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintInk
        )
        ReferenceCompactInputField(
            fieldKey = field.label,
            initialValue = field.value,
            hint = field.hint,
            keyboardType = field.keyboardType,
            onValueCommitted = field.onChange,
            modifier = Modifier.width(88.dp)
        )
    }
}

@Composable
private fun ReferenceCompactInputField(
    fieldKey: String,
    initialValue: String,
    hint: String,
    keyboardType: KeyboardType,
    onValueCommitted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable(fieldKey) { mutableStateOf(initialValue) }
    var isFocused by rememberSaveable(fieldKey) { mutableStateOf(false) }
    var lastCommittedValue by rememberSaveable(fieldKey) { mutableStateOf(initialValue) }
    var lastObservedExternalValue by rememberSaveable(fieldKey) { mutableStateOf(initialValue) }
    val latestOnValueCommitted by rememberUpdatedState(onValueCommitted)

    LaunchedEffect(initialValue) {
        lastObservedExternalValue = initialValue
        if (!isFocused && initialValue != text) {
            text = initialValue
            lastCommittedValue = initialValue
        }
    }

    LaunchedEffect(isFocused, lastObservedExternalValue, text) {
        if (isFocused || text == lastObservedExternalValue) return@LaunchedEffect
        delay(320L)
        if (isFocused || text == lastObservedExternalValue) return@LaunchedEffect
        text = lastObservedExternalValue
        lastCommittedValue = lastObservedExternalValue
    }

    Surface(
        modifier = modifier,
        color = Color(0xFFFFFEF8),
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.66f)),
        shadowElevation = if (isFocused) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted.copy(alpha = 0.72f)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ReferenceBlueprintInk
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text != lastCommittedValue) {
                            latestOnValueCommitted(text)
                            lastCommittedValue = text
                        }
                    }
                ),
                cursorBrush = SolidColor(ReferenceBlueprintNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        val wasFocused = isFocused
                        isFocused = focusState.isFocused
                        if (wasFocused && !focusState.isFocused && text != lastCommittedValue) {
                            latestOnValueCommitted(text)
                            lastCommittedValue = text
                        }
                    }
            )
        }
    }
}

@Composable
private fun ReferenceToggleRow(
    label: String,
    supporting: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) ReferenceBlueprintPaperAlt.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.82f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReferenceUtilityButton(
                label = if (selected) "On" else "Off",
                emphasized = selected,
                onClick = onClick
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = ReferenceBlueprintInk
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted
                )
            }
        }
    }
}

@Composable
private fun ResultsSummaryBlock(
    uiState: TakeoffUiState,
    selectedType: TakeoffType,
    showDetailedResults: Boolean,
    isMaterialsMode: Boolean
) {
    val result = uiState.result
    if (result == null) {
        Text(
            text = "No estimate is ready yet. Enter measurements or draw geometry for ${selectedType.displayLabel}.",
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted
        )
        return
    }

    val measuredItems = result.nonZeroItems()
    val hasMeasuredQuantities = result.hasMeasuredQuantities()

    ReferenceResultLine(
        label = "Line Items",
        value = measuredItems.size.toString()
    )
    if (!hasMeasuredQuantities) {
        Text(
            text = "No measured quantities yet. Draw in Blueprint or enter manual values first.",
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted
        )
        return
    }

    result.materialSubtotal?.let {
        ReferenceResultLine(
            label = "Material Subtotal",
            value = Formatters.formatMoney(it)
        )
    }
    if (isMaterialsMode) {
        result.totalCost?.let {
            ReferenceResultLine(
                label = "Estimated Total",
                value = Formatters.formatMoney(it),
                emphasize = true
            )
        }
    }

    if (showDetailedResults) {
        HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.32f))
        measuredItems.forEachIndexed { index, line ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = line.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ReferenceBlueprintInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${Formatters.formatQuantity(line.quantity)} ${line.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintInk
                    )
                }
                line.extendedCost?.let { extendedCost ->
                    Text(
                        text = Formatters.formatMoney(extendedCost),
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                }
            }
            if (index < measuredItems.lastIndex) {
                HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.18f))
            }
        }
    }
}

@Composable
private fun ReferenceResultLine(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (emphasize) 8.dp else 0.dp, vertical = if (emphasize) 6.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
                color = if (emphasize) Color.White.copy(alpha = 0.9f) else ReferenceBlueprintMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
                color = if (emphasize) Color.White else ReferenceBlueprintInk
            )
        }
    }
    if (emphasize) {
        Surface(
            color = ReferenceBlueprintNavy,
            border = BorderStroke(1.dp, ReferenceBlueprintBorder),
            shadowElevation = 2.dp
        ) {
            rowContent()
        }
    } else {
        rowContent()
    }
}

private fun manualFieldSpecs(
    uiState: TakeoffUiState,
    selectedType: TakeoffType,
    viewModel: TakeoffViewModel
): List<NumberFieldSpec> {
    if (uiState.inputMode != TakeoffInputMode.MANUAL) return emptyList()
    return when (selectedType) {
        TakeoffType.DRYWALL -> listOf(
            NumberFieldSpec(
                label = "Net Wall Area (sq ft)",
                value = uiState.manualParams.drywallWallAreaSqFt.toString(),
                hint = "Wall area",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateManualParams(drywallWallAreaSqFt = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Ceiling Area (sq ft)",
                value = uiState.manualParams.drywallCeilingAreaSqFt.toString(),
                hint = "Ceiling area",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateManualParams(drywallCeilingAreaSqFt = value)
                    }
                }
            )
        )

        TakeoffType.CONCRETE -> listOf(
            NumberFieldSpec(
                label = "Slab Footprint (sq ft)",
                value = uiState.manualParams.concreteAreaSqFt.toString(),
                hint = "Footprint",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateManualParams(concreteAreaSqFt = value)
                    }
                }
            )
        )

        TakeoffType.GRAVEL_MULCH -> listOf(
            NumberFieldSpec(
                label = "Coverage Area (sq ft)",
                value = uiState.manualParams.gravelAreaSqFt.toString(),
                hint = "Coverage",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateManualParams(gravelAreaSqFt = value)
                    }
                }
            )
        )

        TakeoffType.PAINT -> listOf(
            NumberFieldSpec(
                label = "Paintable Area (sq ft)",
                value = uiState.manualParams.paintAreaSqFt.toString(),
                hint = "Paint area",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateManualParams(paintAreaSqFt = value)
                    }
                }
            )
        )
    }
}

private fun tradeFieldSpecs(
    uiState: TakeoffUiState,
    selectedType: TakeoffType,
    viewModel: TakeoffViewModel
): List<NumberFieldSpec> {
    return when (selectedType) {
        TakeoffType.DRYWALL -> listOf(
            NumberFieldSpec(
                label = "Sheet Area (sq ft)",
                value = uiState.drywallParams.sheetAreaSqFt.toString(),
                hint = "32",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateDrywallParams(sheetAreaSqFt = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Waste %",
                value = uiState.drywallParams.wastePercent.toString(),
                hint = "8.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateDrywallParams(wastePercent = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Screws per Sheet",
                value = uiState.drywallParams.screwsPerSheet.toString(),
                hint = "32",
                keyboardType = KeyboardType.Number,
                onChange = {
                    it.toIntOrNull()?.let { value ->
                        viewModel.updateDrywallParams(screwsPerSheet = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Mud (gal / 100 sq ft)",
                value = uiState.drywallParams.mudGallonsPer100SqFt.toString(),
                hint = "0.5",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateDrywallParams(mudGallonsPer100SqFt = value)
                    }
                }
            )
        )

        TakeoffType.CONCRETE -> listOf(
            NumberFieldSpec(
                label = "Thickness (feet)",
                value = uiState.concreteParams.thicknessFeet.toString(),
                hint = "0.33",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateConcreteParams(thicknessFeet = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Waste %",
                value = uiState.concreteParams.wastePercent.toString(),
                hint = "8.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateConcreteParams(wastePercent = value)
                    }
                }
            )
        )

        TakeoffType.GRAVEL_MULCH -> listOf(
            NumberFieldSpec(
                label = "Depth (feet)",
                value = uiState.gravelParams.depthFeet.toString(),
                hint = "0.25",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateGravelParams(depthFeet = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Density (tons / yard)",
                value = uiState.gravelParams.densityTonsPerYard.toString(),
                hint = "1.4",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateGravelParams(densityTonsPerYard = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Waste %",
                value = uiState.gravelParams.wastePercent.toString(),
                hint = "10.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updateGravelParams(wastePercent = value)
                    }
                }
            )
        )

        TakeoffType.PAINT -> listOf(
            NumberFieldSpec(
                label = "Coverage (sq ft / gallon)",
                value = uiState.paintParams.coverageSqFtPerGallon.toString(),
                hint = "350",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePaintParams(coverageSqFtPerGallon = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Coats",
                value = uiState.paintParams.coats.toString(),
                hint = "2",
                keyboardType = KeyboardType.Number,
                onChange = {
                    it.toIntOrNull()?.let { value ->
                        viewModel.updatePaintParams(coats = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Waste %",
                value = uiState.paintParams.wastePercent.toString(),
                hint = "8.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePaintParams(wastePercent = value)
                    }
                }
            )
        )
    }
}

private fun pricingFieldSpecs(
    uiState: TakeoffUiState,
    selectedType: TakeoffType,
    viewModel: TakeoffViewModel
): List<NumberFieldSpec> {
    val pricing = uiState.pricingParams
    val typeSpecific = when (selectedType) {
        TakeoffType.DRYWALL -> listOf(
            NumberFieldSpec(
                label = "Sheet Cost ($/sheet)",
                value = pricing.drywallSheetCost.toString(),
                hint = "18.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(drywallSheetCost = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Screw Cost ($/screw)",
                value = pricing.drywallScrewCost.toString(),
                hint = "0.04",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(drywallScrewCost = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Mud Cost ($/gallon)",
                value = pricing.drywallMudCost.toString(),
                hint = "16.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(drywallMudCost = value)
                    }
                }
            )
        )

        TakeoffType.CONCRETE -> listOf(
            NumberFieldSpec(
                label = "Concrete Cost ($/yard)",
                value = pricing.concreteYardCost.toString(),
                hint = "165.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(concreteYardCost = value)
                    }
                }
            )
        )

        TakeoffType.GRAVEL_MULCH -> listOf(
            NumberFieldSpec(
                label = "Volume Cost ($/cubic yard)",
                value = pricing.gravelYardCost.toString(),
                hint = "52.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(gravelYardCost = value)
                    }
                }
            ),
            NumberFieldSpec(
                label = "Weight Cost ($/ton)",
                value = pricing.gravelTonCost.toString(),
                hint = "36.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(gravelTonCost = value)
                    }
                }
            )
        )

        TakeoffType.PAINT -> listOf(
            NumberFieldSpec(
                label = "Paint Cost ($/gallon)",
                value = pricing.paintGallonCost.toString(),
                hint = "38.0",
                onChange = {
                    it.toDoubleOrNull()?.let { value ->
                        viewModel.updatePricingParams(paintGallonCost = value)
                    }
                }
            )
        )
    }

    return typeSpecific + listOf(
        NumberFieldSpec(
            label = "Labor %",
            value = pricing.laborPercent.toString(),
            hint = "20.0",
            onChange = {
                it.toDoubleOrNull()?.let { value ->
                    viewModel.updatePricingParams(laborPercent = value)
                }
            }
        ),
        NumberFieldSpec(
            label = "Markup %",
            value = pricing.markupPercent.toString(),
            hint = "15.0",
            onChange = {
                it.toDoubleOrNull()?.let { value ->
                    viewModel.updatePricingParams(markupPercent = value)
                }
            }
        ),
        NumberFieldSpec(
            label = "Tax %",
            value = pricing.taxPercent.toString(),
            hint = "8.0",
            onChange = {
                it.toDoubleOrNull()?.let { value ->
                    viewModel.updatePricingParams(taxPercent = value)
                }
            }
        )
    )
}

private data class NumberFieldSpec(
    val label: String,
    val value: String,
    val hint: String,
    val keyboardType: KeyboardType = KeyboardType.Decimal,
    val onChange: (String) -> Unit
)

private data class ScopeSummary(
    val spaceCount: Int,
    val sourceCount: Int,
    val sourceLabel: String,
    val measuredQuantity: Double,
    val unit: String,
    val quantityLabel: String,
    val guidance: String
)

private fun scopeSummaryForType(
    project: Project?,
    type: TakeoffType,
    includeDrywallCeilings: Boolean,
    inputMode: TakeoffInputMode,
    manualParams: ManualTakeoffParams
): ScopeSummary {
    if (inputMode == TakeoffInputMode.MANUAL) {
        return when (type) {
            TakeoffType.DRYWALL -> {
                val wallArea = manualParams.drywallWallAreaSqFt.coerceAtLeast(0.0)
                val ceilingArea = if (includeDrywallCeilings) {
                    manualParams.drywallCeilingAreaSqFt.coerceAtLeast(0.0)
                } else {
                    0.0
                }
                val manualCount = listOf(wallArea, ceilingArea).count { it > 0.0 }
                ScopeSummary(
                    spaceCount = manualCount,
                    sourceCount = manualCount,
                    sourceLabel = if (manualCount == 1) "manual entry" else "manual entries",
                    measuredQuantity = wallArea + ceilingArea,
                    unit = "sq ft",
                    quantityLabel = if (includeDrywallCeilings) "Net drywall area" else "Net wall area",
                    guidance = if (includeDrywallCeilings) {
                        "Manual wall area plus manual ceiling area are used for drywall quantity."
                    } else {
                        "Manual wall area is used for drywall quantity."
                    }
                )
            }

            TakeoffType.CONCRETE -> {
                val area = manualParams.concreteAreaSqFt.coerceAtLeast(0.0)
                val manualCount = if (area > 0.0) 1 else 0
                ScopeSummary(
                    spaceCount = manualCount,
                    sourceCount = manualCount,
                    sourceLabel = if (manualCount == 1) "manual entry" else "manual entries",
                    measuredQuantity = area,
                    unit = "sq ft",
                    quantityLabel = "Slab footprint",
                    guidance = "Manual slab footprint drives concrete volume."
                )
            }

            TakeoffType.GRAVEL_MULCH -> {
                val area = manualParams.gravelAreaSqFt.coerceAtLeast(0.0)
                val manualCount = if (area > 0.0) 1 else 0
                ScopeSummary(
                    spaceCount = manualCount,
                    sourceCount = manualCount,
                    sourceLabel = if (manualCount == 1) "manual entry" else "manual entries",
                    measuredQuantity = area,
                    unit = "sq ft",
                    quantityLabel = "Ground coverage",
                    guidance = "Manual coverage area drives gravel/mulch quantity."
                )
            }

            TakeoffType.PAINT -> {
                val area = manualParams.paintAreaSqFt.coerceAtLeast(0.0)
                val manualCount = if (area > 0.0) 1 else 0
                ScopeSummary(
                    spaceCount = manualCount,
                    sourceCount = manualCount,
                    sourceLabel = if (manualCount == 1) "manual entry" else "manual entries",
                    measuredQuantity = area,
                    unit = "sq ft",
                    quantityLabel = "Paintable wall area",
                    guidance = "Manual paintable area drives gallons and price."
                )
            }
        }
    }

    if (project == null) {
        return ScopeSummary(
            spaceCount = 0,
            sourceCount = 0,
            sourceLabel = "blueprint surfaces",
            measuredQuantity = 0.0,
            unit = "",
            quantityLabel = "Measured quantity",
            guidance = "Project data is still loading."
        )
    }

    val blueprint = projectBlueprintForType(project = project, type = type)

    return when (type) {
        TakeoffType.DRYWALL -> {
            val wallCount = blueprint.walls.size
            val eligibleCeilings = blueprint.rooms.filter { room -> room.ceiling.enabled }
            val ceilingCount = if (includeDrywallCeilings) eligibleCeilings.size else 0
            val totalCount = wallCount + ceilingCount
            val wallArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(blueprint).values.sum()
            val openingArea = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(blueprint).values.sum()
            val ceilingArea = if (includeDrywallCeilings) {
                eligibleCeilings.sumOf { it.areaSqFt() }
            } else {
                0.0
            }
            val netArea = (wallArea - openingArea + ceilingArea).coerceAtLeast(0.0)

            ScopeSummary(
                spaceCount = totalCount,
                sourceCount = totalCount,
                sourceLabel = if (totalCount == 1) "blueprint surface" else "blueprint surfaces",
                measuredQuantity = netArea,
                unit = "sq ft",
                quantityLabel = if (includeDrywallCeilings) "Net drywall area" else "Net wall area",
                guidance = if (includeDrywallCeilings) {
                    "Walls plus ceilings with openings removed are used for drywall quantity."
                } else {
                    "Walls with openings removed are used for drywall quantity."
                }
            )
        }

        TakeoffType.CONCRETE -> {
            val roomCount = blueprint.rooms.size
            val area = blueprint.rooms.sumOf { it.areaSqFt() }
            ScopeSummary(
                spaceCount = roomCount,
                sourceCount = roomCount,
                sourceLabel = if (roomCount == 1) "blueprint surface" else "blueprint surfaces",
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Slab footprint",
                guidance = "Room footprint and thickness determine concrete volume."
            )
        }

        TakeoffType.GRAVEL_MULCH -> {
            val targetRooms = BlueprintTakeoffCalculator.gravelTargetRooms(blueprint)
            val roomCount = targetRooms.size
            val area = targetRooms.sumOf { it.areaSqFt() }
            val usedTaggedRooms = targetRooms.size != blueprint.rooms.size
            ScopeSummary(
                spaceCount = roomCount,
                sourceCount = roomCount,
                sourceLabel = if (roomCount == 1) "blueprint surface" else "blueprint surfaces",
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Ground coverage",
                guidance = if (usedTaggedRooms) {
                    "Only rooms tagged gravel, mulch, or bed are used for coverage."
                } else {
                    "All room surfaces are treated as coverage area."
                }
            )
        }

        TakeoffType.PAINT -> {
            val wallCount = blueprint.walls.size
            val wallArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(blueprint).values.sum()
            val openingArea = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(blueprint).values.sum()
            val netArea = (wallArea - openingArea).coerceAtLeast(0.0)

            ScopeSummary(
                spaceCount = wallCount,
                sourceCount = wallCount,
                sourceLabel = if (wallCount == 1) "blueprint surface" else "blueprint surfaces",
                measuredQuantity = netArea,
                unit = "sq ft",
                quantityLabel = "Paintable wall area",
                guidance = "Wall surfaces are included with openings deducted."
            )
        }
    }
}

private fun takeoffWarnings(
    uiState: TakeoffUiState,
    selectedType: TakeoffType,
    scopeSummary: ScopeSummary
): List<String> {
    val warnings = mutableListOf<String>()
    if (scopeSummary.spaceCount == 0) {
        if (uiState.inputMode == TakeoffInputMode.MANUAL) {
            warnings += "Manual quantity is empty for ${selectedType.displayLabel}. Enter at least one measurement."
        } else {
            warnings += "No matching blueprint surfaces were found for ${selectedType.displayLabel}. Draw geometry in Blueprint."
        }
    }
    if (scopeSummary.measuredQuantity <= 0.0) {
        warnings += "Measured quantity is zero. Verify dimensions and openings."
    }
    if (selectedType == TakeoffType.DRYWALL) {
        if (uiState.inputMode == TakeoffInputMode.MANUAL) {
            if (uiState.drywallParams.includeCeilings && uiState.manualParams.drywallCeilingAreaSqFt <= 0.0) {
                warnings += "Include Ceilings is enabled, but manual ceiling area is zero."
            }
        } else {
            val hasCeilings = uiState.project
                ?.let { project -> projectBlueprintForType(project = project, type = selectedType) }
                ?.rooms
                ?.any { room -> room.ceiling.enabled } == true
            if (hasCeilings && !uiState.drywallParams.includeCeilings) {
                warnings += "Room ceilings exist but are excluded from drywall totals."
            }
        }
    }
    if (uiState.inputMode == TakeoffInputMode.BLUEPRINT) {
        val blueprint = uiState.project?.let { project ->
            projectBlueprintForType(project = project, type = selectedType)
        }
        if (blueprint != null) {
            val floorTags = buildSet {
                blueprint.walls.forEach { wall ->
                    wall.tags.firstOrNull { tag -> tag.startsWith("floor:") }?.let(::add)
                }
                blueprint.rooms.forEach { room ->
                    room.tags.firstOrNull { tag -> tag.startsWith("floor:") }?.let(::add)
                }
            }
            if (floorTags.size > 1) {
                warnings += "Blueprint takeoff currently includes geometry from all floors."
            }
            val hasStairOpenings = blueprint.openings.any { opening ->
                opening.type == OpeningType.STAIR_UP || opening.type == OpeningType.STAIR_DOWN
            }
            if (hasStairOpenings && (selectedType == TakeoffType.DRYWALL || selectedType == TakeoffType.PAINT)) {
                warnings += "Stair openings are not deducted from drywall/paint wall area."
            }
        }
    }

    val wastePercent = when (selectedType) {
        TakeoffType.DRYWALL -> uiState.drywallParams.wastePercent
        TakeoffType.CONCRETE -> uiState.concreteParams.wastePercent
        TakeoffType.GRAVEL_MULCH -> uiState.gravelParams.wastePercent
        TakeoffType.PAINT -> uiState.paintParams.wastePercent
    }
    if (wastePercent > 25.0) {
        warnings += "Waste is above 25%. Confirm this is intentional."
    }
    if (wastePercent < 2.0) {
        warnings += "Waste is very low. Material shortages may occur."
    }
    if (uiState.pricingParams.markupPercent < 8.0) {
        warnings += "Markup is below 8%. Profit protection may be too thin."
    }
    if (uiState.pricingParams.laborPercent < 8.0) {
        warnings += "Labor percent is very low for most field jobs."
    }
    return warnings
}
