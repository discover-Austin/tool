package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Project

internal fun resolveUniqueProjectName(
    requestedName: String,
    existingProjects: Iterable<Project>,
    excludingProjectId: String? = null
): String {
    val base = requestedName.trim().ifEmpty { "Project" }
    val existingNames = existingProjects
        .asSequence()
        .filter { it.id != excludingProjectId }
        .map { it.name.trim().lowercase() }
        .toSet()

    if (base.lowercase() !in existingNames) {
        return base
    }

    var suffix = 2
    while (true) {
        val candidate = "$base ($suffix)"
        if (candidate.lowercase() !in existingNames) {
            return candidate
        }
        suffix++
    }
}
