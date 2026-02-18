package com.tradesketch.estimator.di

import android.content.Context
import com.tradesketch.estimator.data.local.ProjectDataStore
import com.tradesketch.estimator.data.local.SettingsDataStore
import com.tradesketch.estimator.data.local.UxMetricsDataStore
import com.tradesketch.estimator.data.repository.ProjectRepository
import com.tradesketch.estimator.data.repository.ProjectRepositoryImpl
import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.data.repository.SettingsRepositoryImpl
import com.tradesketch.estimator.data.repository.UxMetricsRepository
import com.tradesketch.estimator.data.repository.UxMetricsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing data layer dependencies.
 * Includes DataStore instances and Repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideProjectDataStore(
        @ApplicationContext context: Context
    ): ProjectDataStore {
        return ProjectDataStore(context)
    }
    
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideUxMetricsDataStore(
        @ApplicationContext context: Context
    ): UxMetricsDataStore {
        return UxMetricsDataStore(context)
    }
    
    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDataStore: ProjectDataStore
    ): ProjectRepository {
        return ProjectRepositoryImpl(projectDataStore)
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDataStore)
    }

    @Provides
    @Singleton
    fun provideUxMetricsRepository(
        uxMetricsDataStore: UxMetricsDataStore
    ): UxMetricsRepository {
        return UxMetricsRepositoryImpl(uxMetricsDataStore)
    }
}
