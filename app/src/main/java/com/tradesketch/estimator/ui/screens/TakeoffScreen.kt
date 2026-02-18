package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import com.tradesketch.estimator.ui.components.DangerActionButton

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.BufferedInputField
import com.tradesketch.estimator.ui.components.TitledSectionCard
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.defaultTakeoffTypeForTrade
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.viewmodel.TakeoffViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.utils.Formatters

@Composable
fun TakeoffScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenModel: () -> Unit = {},
    onOpenBlueprint: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    viewModel: TakeoffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    val focusedType = defaultTakeoffTypeForTrade(uiState.settings.primaryTrade)
    var showAllTradeScopes by rememberSaveable(
        projectId,
        uiState.settings.primaryTrade.name,
        uiState.settings.simplifiedHome,
        uiState.settings.calmModeEnabled
    ) { mutableStateOf(!uiState.settings.calmModeEnabled) }
    var showPricingInputs by rememberSaveable(projectId) { mutableStateOf(true) }
    var showScopeSelector by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(uiState.selectedType == null)
    }
    var showDetailedResults by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    val staggeredDelay: (Int) -> Int = { base ->
        if (uiState.settings.reducedMotionEnabled) 0 else base
    }
    val availableTypes = if (
        uiState.settings.simplifiedHome &&
        focusedType != null &&
        !showAllTradeScopes
    ) {
        listOf(focusedType)
    } else {
        TakeoffType.entries.toList()
    }
    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.recordTap("takeoff_screen_opened")
    }

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
            AnimatedEntry(delayMs = staggeredDelay(70)) {
                TitledSectionCard(
                    title = "Estimate Type",
                    subtitle = if (uiState.settings.simplifiedHome && focusedType != null && !showAllTradeScopes) {
                        "Focused on ${uiState.settings.primaryTrade.displayLabel()}."
                    } else {
                        "Set the trade for this estimate."
                    },
                    modifier = Modifier.animateContentSize()
                ) {
                    val selectedType = uiState.selectedType
                    if (selectedType != null && !showScopeSelector) {
                        Text(
                            text = "Selected type: ${selectedType.displayLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SecondaryActionButton(
                            onClick = { showScopeSelector = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Type")
                        }
                    } else {
                        if (uiState.settings.simplifiedHome && focusedType != null) {
                            SecondaryActionButton(
                                onClick = {
                                    viewModel.recordTap("takeoff_toggle_scope_list")
                                    showAllTradeScopes = !showAllTradeScopes
                                }
                            ) {
                                Text(
                                    if (showAllTradeScopes) {
                                        "Show Focused Trade Only"
                                    } else {
                                        "Show All Trades"
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        TradeScopeGrid(
                            availableTypes = availableTypes,
                            selectedType = uiState.selectedType,
                            onSelect = { type ->
                                haptics.tap()
                                viewModel.recordTap("takeoff_select_scope")
                                viewModel.selectTakeoffType(type)
                                showScopeSelector = false
                            }
                        )
                        if (selectedType != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            QuietActionButton(
                                onClick = { showScopeSelector = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap an estimate type to continue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        uiState.selectedType?.let { type ->
            val scopeSummary = scopeSummaryForType(
                project = uiState.project,
                type = type,
                includeDrywallCeilings = uiState.drywallParams.includeCeilings
            )
            val warnings = takeoffWarnings(uiState, type, scopeSummary)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${type.displayLabel} Summary",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${scopeSummary.spaceCount} matching space(s) in this project.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${scopeSummary.quantityLabel}: " +
                                "${Formatters.formatQuantity(scopeSummary.measuredQuantity)} ${scopeSummary.unit}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = scopeSummary.guidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (scopeSummary.spaceCount == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SecondaryActionButton(
                                    onClick = {
                                        viewModel.recordTap("takeoff_open_model")
                                        onOpenModel()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Model")
                                }
                                SecondaryActionButton(
                                    onClick = {
                                        viewModel.recordTap("takeoff_open_blueprint")
                                        onOpenBlueprint()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Blueprint")
                                }
                            }
                        }
                    }
                }
            }

            if (warnings.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Smart Checks",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${warnings.size} item(s) to review before sharing this estimate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                when (type) {
                    TakeoffType.DRYWALL -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ParameterCard(
                                title = "Drywall Inputs",
                                description = "Sheet sizing and fastener assumptions for walls/ceilings.",
                                fields = listOf(
                                    NumberFieldSpec(
                                        label = "Sheet Area (sq ft)",
                                        value = uiState.drywallParams.sheetAreaSqFt.toString(),
                                        hint = "Typical: 32",
                                        onChange = {
                                            it.toDoubleOrNull()?.let { value ->
                                                viewModel.updateDrywallParams(sheetAreaSqFt = value)
                                            }
                                        }
                                    ),
                                    NumberFieldSpec(
                                        label = "Waste %",
                                        value = uiState.drywallParams.wastePercent.toString(),
                                        hint = "Typical: 5-15",
                                        onChange = {
                                            it.toDoubleOrNull()?.let { value ->
                                                viewModel.updateDrywallParams(wastePercent = value)
                                            }
                                        }
                                    ),
                                    NumberFieldSpec(
                                        label = "Screws per Sheet",
                                        value = uiState.drywallParams.screwsPerSheet.toString(),
                                        hint = "Typical: 28-36",
                                        onChange = {
                                            it.toIntOrNull()?.let { value ->
                                                viewModel.updateDrywallParams(screwsPerSheet = value)
                                            }
                                        }
                                    ),
                                    NumberFieldSpec(
                                        label = "Mud (gal / 100 sq ft)",
                                        value = uiState.drywallParams.mudGallonsPer100SqFt.toString(),
                                        hint = "Typical: 0.5",
                                        onChange = {
                                            it.toDoubleOrNull()?.let { value ->
                                                viewModel.updateDrywallParams(mudGallonsPer100SqFt = value)
                                            }
                                        }
                                    )
                                )
                            )
                            Card {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Include Ceilings",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Adds rectangular surfaces (room ceilings) to drywall and screw counts.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.drywallParams.includeCeilings,
                                        onCheckedChange = { enabled ->
                                            viewModel.updateDrywallParams(includeCeilings = enabled)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    TakeoffType.CONCRETE -> {
                        ParameterCard(
                            title = "Concrete Inputs",
                            description = "Depth assumptions for slab quantities.",
                            fields = listOf(
                                NumberFieldSpec(
                                    label = "Thickness (feet)",
                                    value = uiState.concreteParams.thicknessFeet.toString(),
                                    hint = "Typical: 0.33 (4 in)",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updateConcreteParams(thicknessFeet = value)
                                        }
                                    }
                                ),
                                NumberFieldSpec(
                                    label = "Waste %",
                                    value = uiState.concreteParams.wastePercent.toString(),
                                    hint = "Typical: 5-10",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updateConcreteParams(wastePercent = value)
                                        }
                                    }
                                )
                            )
                        )
                    }

                    TakeoffType.GRAVEL_MULCH -> {
                        ParameterCard(
                            title = "Gravel / Mulch Inputs",
                            description = "Depth and density control both yards and tons.",
                            fields = listOf(
                                NumberFieldSpec(
                                    label = "Depth (feet)",
                                    value = uiState.gravelParams.depthFeet.toString(),
                                    hint = "Typical: 0.25",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updateGravelParams(depthFeet = value)
                                        }
                                    }
                                ),
                                NumberFieldSpec(
                                    label = "Density (tons / yard)",
                                    value = uiState.gravelParams.densityTonsPerYard.toString(),
                                    hint = "Typical: 1.4",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updateGravelParams(densityTonsPerYard = value)
                                        }
                                    }
                                ),
                                NumberFieldSpec(
                                    label = "Waste %",
                                    value = uiState.gravelParams.wastePercent.toString(),
                                    hint = "Typical: 5-15",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updateGravelParams(wastePercent = value)
                                        }
                                    }
                                )
                            )
                        )
                    }

                    TakeoffType.PAINT -> {
                        ParameterCard(
                            title = "Paint Inputs",
                            description = "Coverage and coats drive gallons and price.",
                            fields = listOf(
                                NumberFieldSpec(
                                    label = "Coverage (sq ft / gallon)",
                                    value = uiState.paintParams.coverageSqFtPerGallon.toString(),
                                    hint = "Typical: 300-400",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updatePaintParams(coverageSqFtPerGallon = value)
                                        }
                                    }
                                ),
                                NumberFieldSpec(
                                    label = "Coats",
                                    value = uiState.paintParams.coats.toString(),
                                    hint = "Typical: 2",
                                    onChange = {
                                        it.toIntOrNull()?.let { value ->
                                            viewModel.updatePaintParams(coats = value)
                                        }
                                    }
                                ),
                                NumberFieldSpec(
                                    label = "Waste %",
                                    value = uiState.paintParams.wastePercent.toString(),
                                    hint = "Typical: 5-10",
                                    onChange = {
                                        it.toDoubleOrNull()?.let { value ->
                                            viewModel.updatePaintParams(wastePercent = value)
                                        }
                                    }
                                )
                            )
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pricing Inputs",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Labor ${Formatters.formatQuantity(uiState.pricingParams.laborPercent)}% • " +
                                "Markup ${Formatters.formatQuantity(uiState.pricingParams.markupPercent)}% • " +
                                "Tax ${Formatters.formatQuantity(uiState.pricingParams.taxPercent)}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        SecondaryActionButton(
                            onClick = {
                                viewModel.recordTap("takeoff_toggle_pricing_inputs")
                                showPricingInputs = !showPricingInputs
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showPricingInputs) {
                                    "Hide Pricing Inputs"
                                } else {
                                    "Show Pricing Inputs"
                                }
                            )
                        }
                    }
                }
            }

            if (showPricingInputs) {
                item {
                    val pricing = uiState.pricingParams
                    val typeSpecificPricing = when (type) {
                        TakeoffType.DRYWALL -> listOf(
                            NumberFieldSpec(
                                label = "Sheet Cost ($/sheet)",
                                value = pricing.drywallSheetCost.toString(),
                                hint = "Material price",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(drywallSheetCost = value)
                                    }
                                }
                            ),
                            NumberFieldSpec(
                                label = "Screw Cost ($/screw)",
                                value = pricing.drywallScrewCost.toString(),
                                hint = "Fastener price",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(drywallScrewCost = value)
                                    }
                                }
                            ),
                            NumberFieldSpec(
                                label = "Mud Cost ($/gallon)",
                                value = pricing.drywallMudCost.toString(),
                                hint = "Compound price",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(drywallMudCost = value)
                                    }
                                }
                            )
                        )

                        TakeoffType.CONCRETE -> listOf(
                            NumberFieldSpec(
                                label = "Concrete Cost ($/cubic yard)",
                                value = pricing.concreteYardCost.toString(),
                                hint = "Batch price",
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
                                hint = "Yard pricing",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(gravelYardCost = value)
                                    }
                                }
                            ),
                            NumberFieldSpec(
                                label = "Weight Cost ($/ton)",
                                value = pricing.gravelTonCost.toString(),
                                hint = "Ton pricing",
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
                                hint = "Can price",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(paintGallonCost = value)
                                    }
                                }
                            )
                        )
                    }

                    ParameterCard(
                        title = "Pricing",
                        description = "Material costs and business percentages used in this estimate.",
                        fields = typeSpecificPricing + listOf(
                            NumberFieldSpec(
                                label = "Labor %",
                                value = pricing.laborPercent.toString(),
                                hint = "Crew labor burden",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(laborPercent = value)
                                    }
                                }
                            ),
                            NumberFieldSpec(
                                label = "Markup %",
                                value = pricing.markupPercent.toString(),
                                hint = "Gross margin add",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(markupPercent = value)
                                    }
                                }
                            ),
                            NumberFieldSpec(
                                label = "Tax %",
                                value = pricing.taxPercent.toString(),
                                hint = "Final tax applied",
                                onChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        viewModel.updatePricingParams(taxPercent = value)
                                    }
                                }
                            )
                        )
                    )
                }
            }


            uiState.result?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Estimate Total",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Line items: ${result.items.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            result.materialSubtotal?.let { materials ->
                                Text(
                                    text = "Material subtotal: ${Formatters.formatMoney(materials)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            result.totalCost?.let { total ->
                                Text(
                                    text = "Estimated total: ${Formatters.formatMoney(total)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryActionButton(
                                onClick = {
                                    viewModel.recordTap("takeoff_open_export")
                                    onOpenExport()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Continue to Export")
                            }
                            QuietActionButton(
                                onClick = {
                                    viewModel.recordTap("takeoff_toggle_detailed_results")
                                    showDetailedResults = !showDetailedResults
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (showDetailedResults) {
                                        "Hide Detailed Results"
                                    } else {
                                        "Show Detailed Results"
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDetailedResults) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Summarize, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${type.displayLabel} Results",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                result.items.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = line.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${Formatters.formatQuantity(line.quantity)} ${line.unit}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            line.unitCost?.let { cost ->
                                                Text(
                                                    text = "@ ${Formatters.formatMoney(cost)}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            line.extendedCost?.let { ext ->
                                                Text(
                                                    text = Formatters.formatMoney(ext),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                }
            } ?: item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Text(
                        text = "Choose an estimate type above to generate quantities and pricing.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
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
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
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
                    text = "Field-check all assumptions before purchasing materials or starting work.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ParameterCard(
    title: String,
    description: String,
    fields: List<NumberFieldSpec>
) {
    Card {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            fields.forEach { field ->
                BufferedNumberField(field = field)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BufferedNumberField(field: NumberFieldSpec) {
    BufferedInputField(
        label = field.label,
        initialValue = field.value,
        hint = field.hint,
        keyboardType = KeyboardType.Decimal,
        onValueChange = field.onChange
    )
}

private data class NumberFieldSpec(
    val label: String,
    val value: String,
    val hint: String,
    val onChange: (String) -> Unit
)

@Composable
private fun TradeScopeGrid(
    availableTypes: List<TakeoffType>,
    selectedType: TakeoffType?,
    onSelect: (TakeoffType) -> Unit
) {
    val options = availableTypes.map { type ->
        when (type) {
            TakeoffType.DRYWALL -> TradeScopeInfo(
                type = type,
                title = "Drywall",
                subtitle = "Sheets, screws, mud"
            )
            TakeoffType.CONCRETE -> TradeScopeInfo(
                type = type,
                title = "Concrete",
                subtitle = "Slab yards + cost stack"
            )
            TakeoffType.GRAVEL_MULCH -> TradeScopeInfo(
                type = type,
                title = "Gravel / Mulch",
                subtitle = "Yards + tons"
            )
            TakeoffType.PAINT -> TradeScopeInfo(
                type = type,
                title = "Paint",
                subtitle = "Coverage + coats"
            )
        }
    }

    options.chunked(2).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowOptions.forEach { option ->
                val selected = option.type == selectedType
                Card(
                    onClick = { onSelect(option.type) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (rowOptions.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private data class TradeScopeInfo(
    val type: TakeoffType,
    val title: String,
    val subtitle: String
)

private data class ScopeSummary(
    val spaceCount: Int,
    val measuredQuantity: Double,
    val unit: String,
    val quantityLabel: String,
    val guidance: String
)

private fun scopeSummaryForType(
    project: Project?,
    type: TakeoffType,
    includeDrywallCeilings: Boolean
): ScopeSummary {
    if (project == null) {
        return ScopeSummary(
            spaceCount = 0,
            measuredQuantity = 0.0,
            unit = "",
            quantityLabel = "Measured quantity",
            guidance = "Project data is still loading."
        )
    }

    val blueprint = project.authoritativeBlueprint()

    return when (type) {
        TakeoffType.DRYWALL -> {
            val wallCount = blueprint.walls.size
            val ceilingCount = if (includeDrywallCeilings) blueprint.rooms.size else 0
            val totalCount = wallCount + ceilingCount
            
            val wallArea = blueprint.walls.sumOf {
                val length = Millimeters(it.lengthMillimeters()).toFeet()
                val height = Millimeters(it.heightMm).toFeet()
                length * height
            }
            val openingArea = blueprint.openings.sumOf {
                Millimeters(it.widthMm).toFeet() * Millimeters(it.heightMm).toFeet()
            }
            val ceilingArea = if (includeDrywallCeilings) {
                blueprint.rooms.sumOf { it.areaSqFt() }
            } else {
                0.0
            }
            val netArea = (wallArea - openingArea + ceilingArea).coerceAtLeast(0.0)
            
            ScopeSummary(
                spaceCount = totalCount,
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
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Slab footprint",
                guidance = "Room footprint and thickness determine concrete volume."
            )
        }

        TakeoffType.GRAVEL_MULCH -> {
            val roomCount = blueprint.rooms.size
            val area = blueprint.rooms.sumOf { it.areaSqFt() }
            ScopeSummary(
                spaceCount = roomCount,
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Ground coverage",
                guidance = "All room surfaces are treated as coverage area."
            )
        }

        TakeoffType.PAINT -> {
            val wallCount = blueprint.walls.size
            val ceilingCount = blueprint.rooms.size
            val totalCount = wallCount + ceilingCount
            
            val wallArea = blueprint.walls.sumOf {
                val length = Millimeters(it.lengthMillimeters()).toFeet()
                val height = Millimeters(it.heightMm).toFeet()
                length * height
            }
            val openingArea = blueprint.openings.sumOf {
                Millimeters(it.widthMm).toFeet() * Millimeters(it.heightMm).toFeet()
            }
            val ceilingArea = blueprint.rooms.sumOf { it.areaSqFt() }
            val netArea = (wallArea - openingArea + ceilingArea).coerceAtLeast(0.0)
            
            ScopeSummary(
                spaceCount = totalCount,
                measuredQuantity = netArea,
                unit = "sq ft",
                quantityLabel = "Paintable area",
                guidance = "Walls and ceilings are included with openings deducted."
            )
        }
    }
}

private fun takeoffWarnings(
    uiState: com.tradesketch.estimator.ui.viewmodel.TakeoffUiState,
    selectedType: TakeoffType,
    scopeSummary: ScopeSummary
): List<String> {
    val warnings = mutableListOf<String>()
    if (scopeSummary.spaceCount == 0) {
        warnings += "No matching spaces were found for ${selectedType.displayLabel}. Add spaces in Model or Blueprint."
    }
    if (scopeSummary.measuredQuantity <= 0.0) {
        warnings += "Measured quantity is zero. Verify dimensions and openings."
    }
    if (selectedType == TakeoffType.DRYWALL) {
        val hasRectangles = uiState.project?.spaces?.any { it.geometry is Geometry.Rect } == true
        if (hasRectangles && !uiState.drywallParams.includeCeilings) {
            warnings += "Rectangular room ceilings exist but are excluded from drywall totals."
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

