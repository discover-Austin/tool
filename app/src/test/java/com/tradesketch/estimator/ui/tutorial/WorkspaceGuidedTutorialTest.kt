package com.tradesketch.estimator.ui.tutorial

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.tradesketch.estimator.DetailTab
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class WorkspaceGuidedTutorialTest {

    @Test
    fun `manual onboarding stays in materials and export tabs`() {
        val steps = workspaceGuidedTutorialSteps(TakeoffInputMode.MANUAL)

        assertFalse(steps.any { it.tab == DetailTab.BLUEPRINT })
        assertEquals(DetailTab.MATERIALS, steps.first().tab)
        assertEquals(DetailTab.EXPORT, steps.last().tab)
    }

    @Test
    fun `blueprint onboarding starts on blueprint before moving forward`() {
        val steps = workspaceGuidedTutorialSteps(TakeoffInputMode.BLUEPRINT)

        assertIs<WorkspaceRailGuidedTutorialStep>(steps.first())
        assertEquals(DetailTab.BLUEPRINT, steps.first().tab)
        assertEquals(DetailTab.MATERIALS, steps[4].tab)
        assertEquals(DetailTab.EXPORT, steps.last().tab)
    }

    @Test
    fun `guided tutorial bounds are normalized into overlay local space`() {
        val localized = normalizeTutorialRectsToOverlaySpace(
            targetBounds = listOf(Rect(left = 220f, top = 144f, right = 420f, bottom = 264f)),
            overlayBoundsInRoot = Rect(left = 180f, top = 96f, right = 980f, bottom = 1600f)
        )

        assertEquals(
            listOf(Rect(left = 40f, top = 48f, right = 240f, bottom = 168f)),
            localized
        )
    }

    @Test
    fun `wide multi target guided steps pin tooltip to the side and top clearance`() {
        val offset = guidedTutorialCardOffset(
            targetRect = Rect(left = 0f, top = 1240f, right = 1080f, bottom = 1740f),
            targetCount = 5,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 280,
            cardHeightPx = 220,
            minimumTopClearancePx = 176
        )

        assertEquals(IntOffset(x = 12, y = 176), offset)
    }

    @Test
    fun `single narrow guided target stays centered above when room exists`() {
        val offset = guidedTutorialCardOffset(
            targetRect = Rect(left = 400f, top = 900f, right = 520f, bottom = 980f),
            targetCount = 1,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 280,
            cardHeightPx = 220,
            minimumTopClearancePx = 160
        )

        assertEquals(IntOffset(x = 320, y = 670), offset)
    }

    @Test
    fun `rail safe start padding keeps guided card clear of expanded rail`() {
        val offset = guidedTutorialCardOffset(
            targetRect = Rect(left = 40f, top = 900f, right = 160f, bottom = 980f),
            targetCount = 1,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 280,
            cardHeightPx = 220,
            minimumTopClearancePx = 160,
            safeStartPaddingPx = 332
        )

        assertEquals(IntOffset(x = 332, y = 670), offset)
    }

    @Test
    fun `bottom preferred guided placement uses bottom slot when it preserves target visibility`() {
        val offset = guidedTutorialCardOffset(
            targetRect = Rect(left = 400f, top = 220f, right = 520f, bottom = 300f),
            targetCount = 1,
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 280,
            cardHeightPx = 220,
            minimumTopClearancePx = 160,
            preferBottomPlacement = true
        )

        assertEquals(IntOffset(x = 320, y = 1688), offset)
    }
}
