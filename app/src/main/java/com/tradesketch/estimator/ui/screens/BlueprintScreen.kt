package com.tradesketch.estimator.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    initialShowAddons: Boolean = false,
    initialShowParams: Boolean = false,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    viewModel: BlueprintEditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    com.tradesketch.estimator.ui.blueprint.BlueprintScreen(
        projectId = projectId,
        modifier = modifier,
        initialShowAddons = initialShowAddons,
        initialShowParams = initialShowParams,
        onOpenTakeoff = onOpenTakeoff,
        onFullscreenBlueprintChanged = onFullscreenBlueprintChanged,
        viewModel = viewModel,
        settingsViewModel = settingsViewModel
    )
}
