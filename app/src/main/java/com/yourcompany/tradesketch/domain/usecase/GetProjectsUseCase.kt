package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.data.repository.ProjectRepository
import com.yourcompany.tradesketch.domain.model.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> = repository.getProjects()
}
