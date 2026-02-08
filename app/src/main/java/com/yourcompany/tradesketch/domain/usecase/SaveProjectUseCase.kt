package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.data.repository.ProjectRepository
import com.yourcompany.tradesketch.domain.model.Project
import javax.inject.Inject

class SaveProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) = repository.saveProject(project)
}
