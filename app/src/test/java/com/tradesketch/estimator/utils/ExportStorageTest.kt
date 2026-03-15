package com.tradesketch.estimator.utils

import android.content.Intent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportStorageTest {
    @Test
    fun shareIntentSpec_includesReadPermission_andMimeType() {
        val spec = ExportStorage.buildShareIntentSpec(
            mimeType = "application/pdf",
            subject = "Estimate",
            text = "Body",
            chooserTitle = "Share"
        )

        assertEquals("application/pdf", spec.mimeType)
        assertTrue((spec.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    @Test
    fun buildFileName_usesSanitizedUniqueFormat() {
        val first = ExportStorage.buildFileName(
            projectName = "My Project / Apt #12",
            suffix = "estimate",
            extension = "pdf"
        )
        val second = ExportStorage.buildFileName(
            projectName = "My Project / Apt #12",
            suffix = "estimate",
            extension = "pdf"
        )

        assertTrue(first.startsWith("My_Project_Apt_12_estimate_"))
        assertTrue(first.endsWith(".pdf"))
        assertTrue(second.endsWith(".pdf"))
        assertTrue(first != second, "Two immediate exports should not collide on filename.")
    }

    @Test
    fun exportDestination_messages_areExplicitAboutLocation() {
        assertEquals(
            "Saved estimate.pdf to Downloads/TradeSketch.",
            ExportDestination.PUBLIC_DOWNLOADS.saveSuccessMessage("estimate.pdf")
        )
        assertEquals(
            "Saved estimate.pdf to TradeSketch app storage. Use Save As to place it in a public folder.",
            ExportDestination.APP_STORAGE.saveSuccessMessage("estimate.pdf")
        )
    }
}
