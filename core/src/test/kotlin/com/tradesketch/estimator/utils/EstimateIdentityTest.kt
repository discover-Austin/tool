package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Project
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EstimateIdentityTest {

    @Test
    fun buildEstimateId_usesProjectId_notProjectName() {
        val project = Project(
            id = "abc-123-project",
            name = "Kitchen Remodel",
            createdAt = 0L,
            updatedAt = 0L
        )
        val estimateId = EstimateIdentity.buildEstimateId(
            project = project,
            generatedAtMillis = 1_735_732_800_000L // 2025-01-01 12:00 UTC
        )
        assertTrue(estimateId.startsWith("TS-20250101-"))
        assertTrue(estimateId.endsWith("ABC123PR"))
    }

    @Test
    fun buildEstimateId_isDeterministic_forSameProjectAndTimestamp() {
        val millis = 1_735_732_800_000L
        val first = EstimateIdentity.buildEstimateId(projectId = "project-xyz", generatedAtMillis = millis)
        val second = EstimateIdentity.buildEstimateId(projectId = "project-xyz", generatedAtMillis = millis)
        assertEquals(first, second)
    }
}
