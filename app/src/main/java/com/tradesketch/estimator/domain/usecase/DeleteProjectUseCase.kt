package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.data.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String) = repository.deleteProject(projectId)
}
