package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.defaultTakeoffScope

internal fun createStarterProjectForTrade(
    trade: PrimaryTrade,
    name: String
): Project {
    return ProjectTemplate.BLANK
        .createProject(name)
        .withTradeScope(trade)
}

internal fun starterProjectNameForTrade(trade: PrimaryTrade): String = when (trade) {
    PrimaryTrade.DRYWALL -> "My Drywall Project"
    PrimaryTrade.CONCRETE -> "My Concrete Project"
    PrimaryTrade.PAINT -> "My Paint Project"
    PrimaryTrade.GRAVEL_MULCH -> "My Gravel Project"
    PrimaryTrade.MULTI -> "My Project"
}

internal fun Project.withTradeScope(trade: PrimaryTrade): Project {
    val mappedScope = trade.defaultTakeoffScope(takeoffSession.selectedScope)
    val blueprint = if (blueprintDocument.projectId == id) {
        blueprintDocument
    } else {
        blueprintDocument.copy(projectId = id)
    }
    return copy(
        takeoffSession = takeoffSession.copy(
            selectedScope = mappedScope,
            selectedPlaybook = TakeoffPlaybook.BALANCED.name
        ),
        blueprintDocument = blueprint
    )
}
