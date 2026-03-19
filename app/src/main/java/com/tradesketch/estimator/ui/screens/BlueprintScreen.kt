package com.tradesketch.estimator.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    initialShowAddons: Boolean = false,
    initialShowParams: Boolean = false,
    leftEdgeDialInset: Dp = 0.dp,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    tutorialMode: Boolean = false,
    onExitTutorialMode: () -> Unit = {},
    viewModel: BlueprintEditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    com.tradesketch.estimator.ui.blueprint.BlueprintScreen(
        projectId = projectId,
        modifier = modifier,
        initialShowAddons = initialShowAddons,
        initialShowParams = initialShowParams,
        leftEdgeDialInset = leftEdgeDialInset,
        onOpenTakeoff = onOpenTakeoff,
        onFullscreenBlueprintChanged = onFullscreenBlueprintChanged,
        tutorialMode = tutorialMode,
        onExitTutorialMode = onExitTutorialMode,
        viewModel = viewModel,
        settingsViewModel = settingsViewModel
    )
}
