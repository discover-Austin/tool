package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal fun ProjectRepository.observeProject(projectId: String): Flow<Project?> {
    return getProjects().map { projects ->
        projects.find { it.id == projectId }
    }
}

internal fun projectAndSettingsFlow(
    projectRepository: ProjectRepository,
    settingsRepository: SettingsRepository,
    projectId: String
): Flow<Pair<Project?, Settings>> {
    return combine(
        projectRepository.observeProject(projectId),
        settingsRepository.getSettings()
    ) { project, settings ->
        project to settings
    }
}
