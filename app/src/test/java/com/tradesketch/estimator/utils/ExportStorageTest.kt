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
}
