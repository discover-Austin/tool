package com.tradesketch.estimator.di

import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.ProjectRepositoryImpl
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.SettingsRepositoryImpl
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to constructor-injected implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        implementation: ProjectRepositoryImpl
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUxMetricsRepository(
        implementation: UxMetricsRepositoryImpl
    ): UxMetricsRepository
}
