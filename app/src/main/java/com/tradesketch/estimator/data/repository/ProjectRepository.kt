package com.tradesketch.estimator.data.repository

import com.tradesketch.estimator.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Project data operations.
 * Provides abstraction over data sources (DataStore, Room, etc.)
 */
interface ProjectRepository {
    /**
     * Get all projects as a Flow for reactive updates.
     */
    fun getProjects(): Flow<List<Project>>

    /**
     * Get a single project by ID.
     */
    suspend fun getProjectById(id: String): Project?

    /**
     * Save a new project or update existing one.
     */
    suspend fun saveProject(project: Project)

    /**
     * Delete a project by ID.
     */
    suspend fun deleteProject(projectId: String)

    /**
     * Delete all projects (for testing or reset).
     */
    suspend fun deleteAllProjects()
}
