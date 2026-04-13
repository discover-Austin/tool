package com.tradesketch.estimator.ui.blueprint

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class BlueprintViewportLayout(
    val compactWidth: Boolean,
    val ultraCompactWidth: Boolean,
    val shortHeight: Boolean,
    val overlayEdgePadding: Dp,
    val overlayTopPadding: Dp,
    val overlaySpacing: Dp,
    val topStackMaxHeight: Dp,
    val topStartStackMaxWidth: Dp,
    val topEndStackMaxWidth: Dp,
    val topControlsRowMaxWidth: Dp,
    val liveOverlayMaxWidth: Dp,
    val selectionPanelMaxWidth: Dp,
    val scopeSelectorMinWidth: Dp,
    val scopeSelectorMinHeight: Dp,
    val scopeSelectorHorizontalPadding: Dp,
    val scopeSelectorIconSize: Dp,
    val scopeSelectorFontSizeSp: Float,
    val clearAllCompact: Boolean,
    val gridBadgeCompact: Boolean,
    val liveOverlayCompact: Boolean,
    val bottomBarCompact: Boolean,
    val paramsPanelWidth: Dp,
    val paramsPanelMaxHeight: Dp,
    val openingPanelWidth: Dp,
    val openingPanelMaxHeight: Dp,
    val gridEditorWidth: Dp,
    val helpPanelWidth: Dp,
    val helpPanelMaxHeight: Dp,
    val bottomBarBottomPadding: Dp,
    val sharedBottomControlsPadding: Dp,
    val panelBottomPadding: Dp,
    val helpBottomPadding: Dp,
    val floorSwitcherBottomPadding: Dp,
    val gridEditorBottomPadding: Dp,
    val floorDockingTopStackThreshold: Dp
)

@Immutable
internal data class WorkspaceBlueprintChromeLayout(
    val railCompact: Boolean,
    val railExpandedForLargeWindow: Boolean,
    val compactBlueprintHud: Boolean,
    val collapsedRailWidth: Dp,
    val expandedRailWidth: Dp,
    val workspaceRailTopPadding: Dp,
    val workspaceCollapsedRailTopPadding: Dp,
    val workspaceRailBottomPadding: Dp,
    val headerLanePadding: Dp,
    val headerButtonSize: Dp,
    val headerSpacing: Dp,
    val headerMinWidth: Dp,
    val headerMaxWidthCap: Dp,
    val headerFieldHeight: Dp
)

internal fun calculateDockedRailOverlayInset(activeRailWidth: Dp): Dp = 4.dp + activeRailWidth + 10.dp

internal fun calculateBlueprintViewportLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    centerReservedWidth: Dp,
    bottomInset: Dp
): BlueprintViewportLayout {
    val safeHeight = (availableHeight - bottomInset).coerceAtLeast(320.dp)
    val compactWidth = availableWidth < 420.dp
    val ultraCompactWidth = availableWidth < 360.dp
    val shortHeight = safeHeight < 700.dp
    val overlayEdgePadding = if (ultraCompactWidth) 4.dp else 6.dp
    val overlayTopPadding = when {
        ultraCompactWidth -> 16.dp
        compactWidth -> 18.dp
        shortHeight -> 10.dp
        else -> 12.dp
    }
    val overlaySpacing = if (shortHeight) 4.dp else 6.dp
    val topStackMaxHeight = (safeHeight.value * 0.36f).dp.coerceIn(220.dp, 320.dp)
    val rawLiveOverlayWidth = (availableWidth.value * 0.42f).dp.coerceIn(132.dp, 194.dp)
    val rawTopStackWidth = (availableWidth.value * 0.46f).dp.coerceIn(144.dp, 238.dp)
    val maxCombinedTopWidth = (availableWidth - (overlayEdgePadding * 2) - 72.dp).coerceAtLeast(276.dp)
    val combinedTopWidth = rawLiveOverlayWidth + rawTopStackWidth
    val topWidthScale = if (combinedTopWidth > maxCombinedTopWidth) {
        (maxCombinedTopWidth.value / combinedTopWidth.value).coerceAtMost(1f)
    } else {
        1f
    }
    val topStartStackMaxWidth = (rawLiveOverlayWidth.value * topWidthScale).dp.coerceIn(132.dp, 194.dp)
    val topEndStackMaxWidth = if (combinedTopWidth > maxCombinedTopWidth) {
        (maxCombinedTopWidth - topStartStackMaxWidth).coerceIn(144.dp, 238.dp)
    } else {
        (rawTopStackWidth.value * topWidthScale).dp.coerceIn(144.dp, 238.dp)
    }
    val sideLaneWidth = if (centerReservedWidth > 0.dp && centerReservedWidth < availableWidth) {
        (((availableWidth - centerReservedWidth) / 2f) - (overlayEdgePadding / 2f))
            .coerceAtLeast(92.dp)
    } else {
        availableWidth
    }
    val liveOverlayMaxWidth = if (sideLaneWidth < topStartStackMaxWidth) {
        sideLaneWidth
    } else {
        topStartStackMaxWidth
    }
    val topControlsRowMaxWidth = if (sideLaneWidth < topEndStackMaxWidth) {
        sideLaneWidth
    } else {
        topEndStackMaxWidth
    }
    val sharedBottomBasePadding = when {
        ultraCompactWidth -> 56.dp
        compactWidth -> 60.dp
        else -> 64.dp
    }
    val joystickRailPadding = 112.dp
    val bottomBarBottomPadding = 8.dp + bottomInset
    val sharedBottomControlsPadding = sharedBottomBasePadding + bottomInset
    val panelBottomPadding = DEFAULT_PANEL_BOTTOM_PADDING + joystickRailPadding + bottomInset
    val helpBottomPadding = panelBottomPadding + 14.dp
    val floorSwitcherBottomPadding = helpBottomPadding + if (shortHeight) 8.dp else 12.dp
    val gridEditorBottomPadding = sharedBottomControlsPadding + 122.dp
    return BlueprintViewportLayout(
        compactWidth = compactWidth,
        ultraCompactWidth = ultraCompactWidth,
        shortHeight = shortHeight,
        overlayEdgePadding = overlayEdgePadding,
        overlayTopPadding = overlayTopPadding,
        overlaySpacing = overlaySpacing,
        topStackMaxHeight = topStackMaxHeight,
        topStartStackMaxWidth = topStartStackMaxWidth,
        topEndStackMaxWidth = topEndStackMaxWidth,
        topControlsRowMaxWidth = topControlsRowMaxWidth,
        liveOverlayMaxWidth = liveOverlayMaxWidth,
        selectionPanelMaxWidth = topStartStackMaxWidth,
        scopeSelectorMinWidth = if (shortHeight) 56.dp else 62.dp,
        scopeSelectorMinHeight = if (shortHeight) 28.dp else 34.dp,
        scopeSelectorHorizontalPadding = if (shortHeight) 6.dp else 8.dp,
        scopeSelectorIconSize = if (shortHeight) 10.dp else 11.dp,
        scopeSelectorFontSizeSp = if (shortHeight) 9f else 10f,
        clearAllCompact = compactWidth,
        gridBadgeCompact = compactWidth || shortHeight,
        liveOverlayCompact = compactWidth,
        bottomBarCompact = compactWidth,
        paramsPanelWidth = (availableWidth.value * 0.62f).dp.coerceIn(214.dp, 238.dp),
        paramsPanelMaxHeight = (safeHeight.value * 0.56f).dp.coerceIn(320.dp, 420.dp),
        openingPanelWidth = (availableWidth.value * 0.44f).dp.coerceIn(150.dp, 170.dp),
        openingPanelMaxHeight = (safeHeight.value * 0.45f).dp.coerceIn(280.dp, 318.dp),
        gridEditorWidth = (availableWidth.value * 0.58f).dp.coerceIn(196.dp, 214.dp),
        helpPanelWidth = (availableWidth.value * 0.84f).dp.coerceIn(248.dp, 314.dp),
        helpPanelMaxHeight = (safeHeight.value * 0.50f).dp.coerceIn(280.dp, 360.dp),
        bottomBarBottomPadding = bottomBarBottomPadding,
        sharedBottomControlsPadding = sharedBottomControlsPadding,
        panelBottomPadding = panelBottomPadding,
        helpBottomPadding = helpBottomPadding,
        floorSwitcherBottomPadding = floorSwitcherBottomPadding,
        gridEditorBottomPadding = gridEditorBottomPadding,
        floorDockingTopStackThreshold = if (compactWidth) 132.dp else 150.dp
    )
}

internal fun calculateWorkspaceBlueprintChromeLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    bottomInset: Dp
): WorkspaceBlueprintChromeLayout {
    val safeHeight = (availableHeight - bottomInset).coerceAtLeast(320.dp)
    val railCompact = availableWidth < 600.dp
    val railExpandedForLargeWindow = availableWidth >= 840.dp
    val compactBlueprintHud = availableWidth < 420.dp
    val headerFieldHeight = if (compactBlueprintHud) 52.dp else 56.dp
    return WorkspaceBlueprintChromeLayout(
        railCompact = railCompact,
        railExpandedForLargeWindow = railExpandedForLargeWindow,
        compactBlueprintHud = compactBlueprintHud,
        collapsedRailWidth = if (railCompact) 38.dp else 40.dp,
        expandedRailWidth = if (railExpandedForLargeWindow) 64.dp else 60.dp,
        workspaceRailTopPadding = if (safeHeight < 760.dp) 118.dp else 132.dp,
        workspaceCollapsedRailTopPadding = if (safeHeight < 760.dp) 56.dp else 68.dp,
        workspaceRailBottomPadding = if (safeHeight < 760.dp) 156.dp else 172.dp,
        headerLanePadding = when {
            compactBlueprintHud -> (availableWidth.value * 0.26f).dp.coerceIn(96.dp, 132.dp)
            else -> (availableWidth.value * 0.22f).dp.coerceIn(116.dp, 188.dp)
        },
        headerButtonSize = if (compactBlueprintHud) 34.dp else 38.dp,
        headerSpacing = if (compactBlueprintHud) 6.dp else 8.dp,
        headerMinWidth = if (compactBlueprintHud) 104.dp else 132.dp,
        headerMaxWidthCap = if (compactBlueprintHud) 144.dp else 188.dp,
        headerFieldHeight = headerFieldHeight
    )
}
