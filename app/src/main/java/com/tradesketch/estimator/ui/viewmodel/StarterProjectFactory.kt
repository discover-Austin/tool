package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.defaultTakeoffScope

internal fun createStarterProjectForTrade(
    trade: PrimaryTrade,
    name: String,
    inputMode: TakeoffInputMode = TakeoffInputMode.BLUEPRINT
): Project {
    return ProjectTemplate.BLANK
        .createProject(name)
        .withTradeScope(trade, inputMode)
}

internal fun starterProjectNameForTrade(trade: PrimaryTrade): String = when (trade) {
    PrimaryTrade.DRYWALL -> "My Drywall Project"
    PrimaryTrade.CONCRETE -> "My Concrete Project"
    PrimaryTrade.PAINT -> "My Paint Project"
    PrimaryTrade.GRAVEL_MULCH -> "My Gravel Project"
    PrimaryTrade.MULTI -> "My Project"
}

internal fun Project.withTradeScope(
    trade: PrimaryTrade,
    inputMode: TakeoffInputMode = TakeoffInputMode.BLUEPRINT
): Project {
    val mappedScope = trade.defaultTakeoffScope(takeoffSession.selectedScope)
    val blueprint = if (blueprintDocument.projectId == id) {
        blueprintDocument
    } else {
        blueprintDocument.copy(projectId = id)
    }
    return copy(
        takeoffSession = takeoffSession.copy(
            selectedScope = mappedScope,
            inputMode = inputMode,
            selectedPlaybook = TakeoffPlaybook.BALANCED.name
        ),
        blueprintDocument = blueprint
    )
}
