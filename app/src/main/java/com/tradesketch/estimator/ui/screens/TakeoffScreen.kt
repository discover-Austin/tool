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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.ui.viewmodel.TakeoffViewModel
import com.tradesketch.estimator.utils.Formatters

@Composable
fun TakeoffScreen(
    projectId: String,
    onOpenModel: () -> Unit = {},
    onOpenBlueprint: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TakeoffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    val focusedType = defaultTakeoffTypeForTrade(uiState.settings.primaryTrade)
    var showAllTradeScopes by rememberSaveable(
        projectId,
        uiState.settings.primaryTrade.name,
        uiState.settings.simplifiedHome
    ) { mutableStateOf(false) }
    val availableTypes = if (
        uiState.settings.simplifiedHome &&
        focusedType != null &&
        !showAllTradeScopes
    ) {
        listOf(focusedType)
    } else {
        TakeoffType.entries.toList()
    }
    val flowSteps = listOf(
        TakeoffFlowStep(
            label = "Choose Trade",
            detail = uiState.selectedType?.displayLabel ?: "Select scope",
            complete = uiState.selectedType != null
        ),
        TakeoffFlowStep(
            label = "Tune Inputs",
            detail = if (uiState.selectedType != null) "Inputs unlocked" else "Pick trade first",
            complete = uiState.selectedType != null
        ),
        TakeoffFlowStep(
            label = "Review Results",
            detail = if (uiState.result != null) "Totals generated" else "Generate takeoff",
            complete = uiState.result != null
        )
    )
    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
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
            AnimatedEntry(delayMs = 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Takeoff Engine",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.project?.name ?: "Current Project",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Select a scope, tune assumptions, and get quantity + pricing instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Takeoff Workflow",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        flowSteps.forEach { step ->
                            TakeoffFlowStepPill(
                                step = step,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Flow Navigator",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenModel,
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("Model")
                        }
                        OutlinedButton(
                            onClick = onOpenBlueprint,
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("Blueprint")
                        }
                        Button(
                            onClick = onOpenExport,
                            enabled = uiState.result != null,
                            modifier = Modifier.width(140.dp)
                        ) {
                            Text("Continue Export")
                        }
                    }
                    Text(
                        text = if (uiState.result != null) {
                            "Takeoff results are ready for packaging and share."
                        } else {
                            "Select a trade and tune inputs to unlock export."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AnimatedEntry(delayMs = 50) {
                Card(modifier = Modifier.animateContentSize()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Trade Scope",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.settings.simplifiedHome && focusedType != null && !showAllTradeScopes) {
                                "Focused on ${uiState.settings.primaryTrade.displayLabel()} scope. Expand when you need more."
                            } else {
                                "Clear separation by trade. Choose one workflow below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.settings.simplifiedHome && focusedType != null) {
                            OutlinedButton(
                                onClick = { showAllTradeScopes = !showAllTradeScopes }
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
                                viewModel.selectTakeoffType(type)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableTypes.forEach { type ->
                                FilterChip(
                                    selected = uiState.selectedType == type,
                                    onClick = {
                                        haptics.tap()
                                        viewModel.selectTakeoffType(type)
                                    },
                                    label = { Text(type.displayLabel) }
                                )
                            }
                        }
                    }
                }
            }
        }

        uiState.selectedType?.let { type ->
            val scopeSummary = scopeSummaryForType(uiState.project, type)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Detected ${type.displayLabel} Scope",
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
                    }
                }
            }

            item {
                when (type) {
                    TakeoffType.DRYWALL -> {
                        ParameterCard(
                            title = "Drywall Inputs",
                            description = "Sheet sizing and fastener assumptions for walls.",
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
                    title = "Pricing + Profit",
                    description = "Live business math stacked on top of material quantities.",
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

            uiState.result?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Result Snapshot",
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
                        }
                    }
                }

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

                if (result.totalCost != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Engineering, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Job Cost Stack",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                SummaryLine("Materials", result.materialSubtotal)
                                SummaryLine("Labor", result.laborCost)
                                SummaryLine("Markup", result.markupCost)
                                SummaryLine("Tax", result.taxCost)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Grand Total: ${Formatters.formatMoney(result.totalCost)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                        text = "Set your scope above to generate quantities and pricing.",
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
    var text by rememberSaveable(field.label) { mutableStateOf(field.value) }
    LaunchedEffect(field.value) {
        if (field.value != text) {
            text = field.value
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            field.onChange(it)
        },
        label = { Text(field.label) },
        placeholder = { Text(field.hint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SummaryLine(label: String, amount: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = Formatters.formatMoney(amount ?: 0.0),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TakeoffFlowStepPill(
    step: TakeoffFlowStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (step.complete) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (step.complete) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (step.complete) "Ready" else "Pending",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class NumberFieldSpec(
    val label: String,
    val value: String,
    val hint: String,
    val onChange: (String) -> Unit
)

private data class TakeoffFlowStep(
    val label: String,
    val detail: String,
    val complete: Boolean
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

private val TakeoffType.displayLabel: String
    get() = when (this) {
        TakeoffType.DRYWALL -> "Drywall"
        TakeoffType.CONCRETE -> "Concrete"
        TakeoffType.GRAVEL_MULCH -> "Gravel/Mulch"
        TakeoffType.PAINT -> "Paint"
    }

private fun PrimaryTrade.displayLabel(): String = when (this) {
    PrimaryTrade.DRYWALL -> "Drywall"
    PrimaryTrade.CONCRETE -> "Concrete"
    PrimaryTrade.PAINT -> "Paint"
    PrimaryTrade.GRAVEL_MULCH -> "Gravel/Mulch"
    PrimaryTrade.MULTI -> "Multi-Trade"
}

private fun defaultTakeoffTypeForTrade(primaryTrade: PrimaryTrade): TakeoffType? {
    return when (primaryTrade) {
        PrimaryTrade.DRYWALL -> TakeoffType.DRYWALL
        PrimaryTrade.CONCRETE -> TakeoffType.CONCRETE
        PrimaryTrade.PAINT -> TakeoffType.PAINT
        PrimaryTrade.GRAVEL_MULCH -> TakeoffType.GRAVEL_MULCH
        PrimaryTrade.MULTI -> null
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

private fun scopeSummaryForType(project: Project?, type: TakeoffType): ScopeSummary {
    if (project == null) {
        return ScopeSummary(
            spaceCount = 0,
            measuredQuantity = 0.0,
            unit = "",
            quantityLabel = "Measured scope",
            guidance = "Project data is still loading."
        )
    }

    return when (type) {
        TakeoffType.DRYWALL -> {
            val spaces = project.spaces.filter { it.geometry is Geometry.Wall }
            val netArea = spaces.sumOf { (it.geometry.areaSqFt() - it.openingsAreaSqFt()).coerceAtLeast(0.0) }
            ScopeSummary(
                spaceCount = spaces.size,
                measuredQuantity = netArea,
                unit = "sq ft",
                quantityLabel = "Net wall area",
                guidance = "Walls with openings removed are used for drywall quantity."
            )
        }

        TakeoffType.CONCRETE -> {
            val spaces = project.spaces.filter { it.geometry is Geometry.Slab }
            val area = spaces.sumOf { it.geometry.areaSqFt() }
            ScopeSummary(
                spaceCount = spaces.size,
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Slab footprint",
                guidance = "Slab footprint and thickness determine concrete volume."
            )
        }

        TakeoffType.GRAVEL_MULCH -> {
            val spaces = project.spaces.filter { it.geometry !is Geometry.Wall }
            val area = spaces.sumOf { it.geometry.areaSqFt() }
            ScopeSummary(
                spaceCount = spaces.size,
                measuredQuantity = area,
                unit = "sq ft",
                quantityLabel = "Ground coverage",
                guidance = "All non-wall surfaces are treated as coverage area."
            )
        }

        TakeoffType.PAINT -> {
            val spaces = project.spaces.filter { it.geometry is Geometry.Wall || it.geometry is Geometry.Rect }
            val netArea = spaces.sumOf { (it.geometry.areaSqFt() - it.openingsAreaSqFt()).coerceAtLeast(0.0) }
            ScopeSummary(
                spaceCount = spaces.size,
                measuredQuantity = netArea,
                unit = "sq ft",
                quantityLabel = "Paintable area",
                guidance = "Walls and rectangular surfaces are included with openings deducted."
            )
        }
    }
}
