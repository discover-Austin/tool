package com.tradesketch.estimator.ui.blueprint

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlueprintAdaptiveLayoutTest {

    private data class PhoneClass(
        val name: String,
        val width: Dp,
        val height: Dp,
        val bottomInset: Dp
    )

    private val phoneClasses = listOf(
        PhoneClass(name = "small", width = 360.dp, height = 640.dp, bottomInset = 24.dp),
        PhoneClass(name = "normal", width = 393.dp, height = 851.dp, bottomInset = 24.dp),
        PhoneClass(name = "tall-narrow", width = 360.dp, height = 800.dp, bottomInset = 24.dp),
        PhoneClass(name = "large", width = 412.dp, height = 915.dp, bottomInset = 24.dp),
        PhoneClass(name = "gesture-nav", width = 393.dp, height = 851.dp, bottomInset = 34.dp)
    )

    @Test
    fun blueprintViewportLayout_keepsTopHudInsideSharedWidthBudget() {
        phoneClasses.forEach { phone ->
            val layout = calculateBlueprintViewportLayout(
                availableWidth = phone.width,
                availableHeight = phone.height,
                centerReservedWidth = 0.dp,
                bottomInset = phone.bottomInset
            )

            val combinedTopWidth = layout.topStartStackMaxWidth + layout.topEndStackMaxWidth
            val maxCombinedTopWidth = (phone.width - (layout.overlayEdgePadding * 2) - 72.dp)
                .coerceAtLeast(276.dp)

            assertTrue(
                combinedTopWidth.value <= maxCombinedTopWidth.value + 0.01f,
                "${phone.name} top HUD overflowed its shared width budget"
            )
            assertTrue(
                layout.liveOverlayMaxWidth.value in 132f..194f,
                "${phone.name} live overlay width escaped its clamp range"
            )
            assertTrue(
                layout.topEndStackMaxWidth.value in 144f..238f,
                "${phone.name} top stack width escaped its clamp range"
            )
        }
    }

    @Test
    fun blueprintViewportLayout_usesBottomInsetOncePerAnchoredRegionWithJoystickRail() {
        val gestureLayout = calculateBlueprintViewportLayout(
            availableWidth = 393.dp,
            availableHeight = 851.dp,
            centerReservedWidth = 0.dp,
            bottomInset = 34.dp
        )

        assertEquals(42.dp, gestureLayout.bottomBarBottomPadding)
        assertEquals(94.dp, gestureLayout.sharedBottomControlsPadding)
        assertEquals(DEFAULT_PANEL_BOTTOM_PADDING + 112.dp + 34.dp, gestureLayout.panelBottomPadding)
        assertEquals(
            gestureLayout.sharedBottomControlsPadding + 122.dp,
            gestureLayout.gridEditorBottomPadding
        )
    }

    @Test
    fun workspaceChromeLayout_scalesHeaderAndRailFromSafeViewport() {
        phoneClasses.forEach { phone ->
            val layout = calculateWorkspaceBlueprintChromeLayout(
                availableWidth = phone.width,
                availableHeight = phone.height,
                bottomInset = phone.bottomInset
            )
            val headerFieldWidth = (
                phone.width -
                    (layout.headerLanePadding * 2) -
                    layout.headerButtonSize -
                    layout.headerSpacing
                ).coerceIn(layout.headerMinWidth, layout.headerMaxWidthCap)

            assertTrue(
                headerFieldWidth.value in layout.headerMinWidth.value..layout.headerMaxWidthCap.value,
                "${phone.name} header field width fell outside its safe clamp range"
            )
            assertTrue(
                layout.expandedRailWidth.value in 60f..64f,
                "${phone.name} expanded rail width escaped its clamp range"
            )
            assertTrue(
                layout.workspaceRailTopPadding.value >= 118f,
                "${phone.name} rail top padding dropped below its minimum"
            )
            assertTrue(
                layout.workspaceRailBottomPadding.value >= 156f,
                "${phone.name} rail bottom padding dropped below its minimum"
            )
        }
    }

    @Test
    fun dockedRailOverlayInset_clearsCollapsedAndExpandedRailWidths() {
        assertEquals(52.dp, calculateDockedRailOverlayInset(38.dp))
        assertEquals(74.dp, calculateDockedRailOverlayInset(60.dp))
        assertEquals(78.dp, calculateDockedRailOverlayInset(64.dp))
    }

    @Test
    fun shortCompactViewport_preservesCompactControlTargetsWithoutCollapsingPanels() {
        val layout = calculateBlueprintViewportLayout(
            availableWidth = 360.dp,
            availableHeight = 640.dp,
            centerReservedWidth = 0.dp,
            bottomInset = 24.dp
        )

        assertTrue(layout.compactWidth)
        assertTrue(layout.shortHeight)
        assertTrue(layout.bottomBarCompact)
        assertEquals(56.dp, layout.scopeSelectorMinWidth)
        assertEquals(28.dp, layout.scopeSelectorMinHeight)
        assertTrue(layout.paramsPanelWidth.value >= 214f)
        assertTrue(layout.helpPanelWidth.value >= 248f)
        assertTrue(layout.openingPanelWidth.value >= 150f)
    }

    @Test
    fun blueprintViewportLayout_reservesHorizontalLaneForProjectBar() {
        val reservedHeaderWidth = 184.dp
        val layout = calculateBlueprintViewportLayout(
            availableWidth = 393.dp,
            availableHeight = 851.dp,
            centerReservedWidth = reservedHeaderWidth,
            bottomInset = 24.dp
        )
        val sideLaneWidth =
            (((393.dp - reservedHeaderWidth) / 2f) - (layout.overlayEdgePadding / 2f)).coerceAtLeast(92.dp)

        assertTrue(
            layout.liveOverlayMaxWidth <= sideLaneWidth,
            "Live quantities should stay inside the left lane beside the project bar"
        )
        assertTrue(
            layout.topControlsRowMaxWidth <= sideLaneWidth,
            "Trade/delete controls should stay inside the right lane beside the project bar"
        )
    }
}
