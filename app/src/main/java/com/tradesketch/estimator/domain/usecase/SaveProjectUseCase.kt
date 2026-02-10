package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.model.Project
import javax.inject.Inject

class SaveProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) = repository.saveProject(project)
}
