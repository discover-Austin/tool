package com.yourcompany.tradesketch.data.repository

import com.yourcompany.tradesketch.data.local.ProjectDataStore
import com.yourcompany.tradesketch.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implementation of ProjectRepository using DataStore for persistence.
 */
class ProjectRepositoryImpl @Inject constructor(
    private val projectDataStore: ProjectDataStore
) : ProjectRepository {
    
    override fun getProjects(): Flow<List<Project>> {
        return projectDataStore.projects
    }
    
    override suspend fun getProjectById(id: String): Project? {
        return projectDataStore.projects.first().find { it.id == id }
    }
    
    override suspend fun saveProject(project: Project) {
        projectDataStore.saveProject(project)
    }
    
    override suspend fun deleteProject(projectId: String) {
        projectDataStore.deleteProject(projectId)
    }
    
    override suspend fun deleteAllProjects() {
        projectDataStore.saveProjects(emptyList())
    }
}
