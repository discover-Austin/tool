package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.domain.model.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> = repository.getProjects()
}
