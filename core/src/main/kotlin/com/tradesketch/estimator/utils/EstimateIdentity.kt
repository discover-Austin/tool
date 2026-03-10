package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EstimateIdentity {
    fun buildEstimateId(
        project: Project,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        return buildEstimateId(
            projectId = project.id,
            generatedAtMillis = generatedAtMillis
        )
    }

    fun buildEstimateId(
        projectId: String,
        generatedAtMillis: Long
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(generatedAtMillis))
        val shortId = projectId
            .filter { char -> char.isLetterOrDigit() }
            .take(8)
            .uppercase()
            .ifBlank { "PROJECT" }
        return "TS-$stamp-$shortId"
    }
}
