package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.data.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String) = repository.deleteProject(projectId)
}
