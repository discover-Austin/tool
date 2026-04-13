package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.PointMm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintExportManagerTest {

    @Test
    fun `aspect fit rect preserves square blueprint inside pdf bounds`() {
        val fitted = aspectFitBounds(
            sourceWidth = 1800,
            sourceHeight = 1800,
            boundsLeft = 56,
            boundsTop = 150,
            boundsRight = 1598,
            boundsBottom = 2169
        )

        assertEquals(56, fitted.left)
        assertEquals(388, fitted.top)
        assertEquals(1598, fitted.right)
        assertEquals(1930, fitted.bottom)
    }

    @Test
    fun `pdf layout reserves a large direct drawing area with safe annotation inset`() {
        val layout = resolveBlueprintPdfLayout(
            pageWidth = 1654,
            pageHeight = 2339
        )

        assertTrue(layout.drawingFrameRect.top < 240f)
        assertTrue(layout.drawingFrameRect.bottom - layout.drawingFrameRect.top > 1800f)
        assertTrue(layout.drawingViewportRect.bottom - layout.drawingViewportRect.top > 1700f)
        assertTrue(layout.drawingViewportRect.left > layout.drawingFrameRect.left)
        assertTrue(layout.drawingViewportRect.top > layout.drawingFrameRect.top)
        assertTrue(layout.drawingViewportRect.right < layout.drawingFrameRect.right)
        assertTrue(layout.drawingViewportRect.bottom < layout.drawingFrameRect.bottom)
        assertTrue(layout.footerRect.top - layout.drawingFrameRect.bottom >= 24f)
    }

    @Test
    fun `render layout centers wide geometry inside square viewport`() {
        val layout = resolveBlueprintRenderFrame(
            contentLeft = 0f,
            contentTop = 0f,
            contentRight = 1000f,
            contentBottom = 1000f,
            points = listOf(
                PointMm(0, 0),
                PointMm(12_000, 0),
                PointMm(12_000, 3_000),
                PointMm(0, 3_000)
            )
        )

        assertEquals(0f, layout.frameLeft, 0.01f)
        assertEquals(1000f, layout.frameRight, 0.01f)
        assertTrue(layout.frameTop > 0f)
        assertTrue(layout.frameBottom < 1000f)
        val geometryWidth = layout.geometryRight - layout.geometryLeft
        val geometryHeight = layout.geometryBottom - layout.geometryTop
        assertEquals(4f, geometryWidth / geometryHeight, 0.05f)
        assertTrue(layout.geometryLeft > layout.frameLeft)
    }

    @Test
    fun `render layout centers tall geometry inside wide viewport`() {
        val layout = resolveBlueprintRenderFrame(
            contentLeft = 0f,
            contentTop = 0f,
            contentRight = 1600f,
            contentBottom = 560f,
            points = listOf(
                PointMm(0, 0),
                PointMm(3_000, 0),
                PointMm(3_000, 12_000),
                PointMm(0, 12_000)
            )
        )

        assertEquals(0f, layout.frameTop, 0.01f)
        assertEquals(560f, layout.frameBottom, 0.01f)
        assertTrue(layout.frameLeft > 0f)
        assertTrue(layout.frameRight < 1600f)
        val geometryWidth = layout.geometryRight - layout.geometryLeft
        val geometryHeight = layout.geometryBottom - layout.geometryTop
        assertEquals(0.25f, geometryWidth / geometryHeight, 0.05f)
        assertTrue(layout.geometryTop > layout.frameTop)
    }
}
