package com.tradesketch.estimator.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportStatusModelTest {

    @Test
    fun `info status clears when save dialog is dismissed`() {
        val opening = reduceExportStatus(
            current = null,
            event = ExportStatusEvent.Info("Opening save dialog...")
        )

        val cleared = reduceExportStatus(
            current = opening,
            event = ExportStatusEvent.ClearPending
        )

        assertEquals(ExportStatusTone.INFO, opening?.tone)
        assertNull(cleared)
    }

    @Test
    fun `success status survives pending clear and auto clears later`() {
        val saved = reduceExportStatus(
            current = reduceExportStatus(null, ExportStatusEvent.Info("Preparing export...")),
            event = ExportStatusEvent.Success("Saved file.")
        )

        val afterPendingClear = reduceExportStatus(
            current = saved,
            event = ExportStatusEvent.ClearPending
        )
        val afterAutoClear = reduceExportStatus(
            current = afterPendingClear,
            event = ExportStatusEvent.ClearTransient
        )

        assertEquals(ExportStatusTone.SUCCESS, saved?.tone)
        assertEquals(saved, afterPendingClear)
        assertNull(afterAutoClear)
    }

    @Test
    fun `failure status replaces progress and is marked for auto clear`() {
        val failed = reduceExportStatus(
            current = reduceExportStatus(null, ExportStatusEvent.Info("Preparing export...")),
            event = ExportStatusEvent.Failure("Save failed.")
        )

        assertEquals(ExportStatusTone.ERROR, failed?.tone)
        assertEquals("Save failed.", failed?.message)
        assertTrue(shouldAutoClearExportStatus(failed))
        assertFalse(shouldAutoClearExportStatus(ExportStatus.info("Preparing export...")))
    }
}
