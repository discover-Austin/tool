package com.tradesketch.estimator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.UxMetricsSnapshot
import com.tradesketch.estimator.domain.usecase.CalculateTakeoffUseCase
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class TakeoffViewModelTest {
    @Test
    fun updatePricingParams_persistsLatestSession_whenOlderSaveCompletesLast() = runBlocking {
        val mainThread = newSingleMainThread("takeoff-main")
        Dispatchers.setMain(mainThread)
        try {
            val seed = ProjectTemplate.BEDROOM.createProject("Race Test")
            val project = seed.copy(
                id = "project-1",
                blueprintDocument = seed.blueprintDocument.copy(projectId = "project-1")
            )
            val projectRepository = RaceProjectRepository(project)
            val viewModel = TakeoffViewModel(
                repository = projectRepository,
                settingsRepository = FakeSettingsRepository(Settings.DEFAULT),
                uxMetricsRepository = FakeUxMetricsRepository(),
                calculateTakeoffUseCase = CalculateTakeoffUseCase(),
                savedStateHandle = SavedStateHandle()
            )

            viewModel.setProjectId(project.id)
            delay(250)

            viewModel.updatePricingParams(laborPercent = 11.0)
            viewModel.updatePricingParams(laborPercent = 12.0)

            delay(900)

            assertEquals(12.0, projectRepository.currentProject().takeoffSession.pricing.laborPercent)
            assertTrue(projectRepository.savedLaborPercents.contains(12.0))
            assertEquals(12.0, projectRepository.savedLaborPercents.last())
        } finally {
            Dispatchers.resetMain()
            mainThread.close()
        }
    }
}

private class RaceProjectRepository(
    initialProject: Project
) : ProjectRepository {
    private val projectsFlow = MutableStateFlow(listOf(initialProject))
    val savedLaborPercents = CopyOnWriteArrayList<Double>()

    override fun getProjects(): Flow<List<Project>> = projectsFlow.asStateFlow()

    override suspend fun getProjectById(id: String): Project? {
        return projectsFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun saveProject(project: Project) {
        val laborPercent = project.takeoffSession.pricing.laborPercent
        val delayMs = if (laborPercent == 11.0) 250L else 25L
        delay(delayMs)
        savedLaborPercents += laborPercent
        projectsFlow.value = projectsFlow.value.map { existing ->
            if (existing.id == project.id) project else existing
        }
    }

    override suspend fun deleteProject(projectId: String) {
        projectsFlow.value = projectsFlow.value.filterNot { it.id == projectId }
    }

    override suspend fun deleteAllProjects() {
        projectsFlow.value = emptyList()
    }

    fun currentProject(): Project = projectsFlow.value.single()
}

private class FakeSettingsRepository(
    initialSettings: Settings
) : SettingsRepository {
    private val settingsFlow = MutableStateFlow(initialSettings)

    override fun getSettings(): Flow<Settings> = settingsFlow.asStateFlow()

    override suspend fun saveSettings(settings: Settings) {
        settingsFlow.value = settings
    }

    override suspend fun resetSettings() {
        settingsFlow.value = Settings.DEFAULT
    }
}

private class FakeUxMetricsRepository : UxMetricsRepository {
    private val metricsFlow = MutableStateFlow(UxMetricsSnapshot())

    override fun getMetrics(): Flow<UxMetricsSnapshot> = metricsFlow.asStateFlow()

    override suspend fun recordTap(task: String) = Unit

    override suspend fun recordTimeToFirstEstimate(timeMs: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
private fun newSingleMainThread(name: String): ExecutorCoroutineDispatcher {
    return newSingleThreadContext(name)
}
