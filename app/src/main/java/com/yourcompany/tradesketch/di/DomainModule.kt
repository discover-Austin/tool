package com.yourcompany.tradesketch.di

import com.yourcompany.tradesketch.domain.calc.TakeoffCalculator
import com.yourcompany.tradesketch.domain.usecase.*
import com.yourcompany.tradesketch.data.repository.ProjectRepository
import com.yourcompany.tradesketch.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing domain layer dependencies.
 * Includes use cases and domain calculators.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideTakeoffCalculator(): TakeoffCalculator {
        return TakeoffCalculator
    }
    
    @Provides
    fun provideGetProjectsUseCase(
        repository: ProjectRepository
    ): GetProjectsUseCase {
        return GetProjectsUseCase(repository)
    }
    
    @Provides
    fun provideSaveProjectUseCase(
        repository: ProjectRepository
    ): SaveProjectUseCase {
        return SaveProjectUseCase(repository)
    }
    
    @Provides
    fun provideDeleteProjectUseCase(
        repository: ProjectRepository
    ): DeleteProjectUseCase {
        return DeleteProjectUseCase(repository)
    }
    
    @Provides
    fun provideCalculateTakeoffUseCase(
        calculator: TakeoffCalculator
    ): CalculateTakeoffUseCase {
        return CalculateTakeoffUseCase(calculator)
    }
    
    @Provides
    fun provideCreateProjectFromTemplateUseCase(): CreateProjectFromTemplateUseCase {
        return CreateProjectFromTemplateUseCase()
    }
    
    @Provides
    fun provideGetSettingsUseCase(
        repository: SettingsRepository
    ): GetSettingsUseCase {
        return GetSettingsUseCase(repository)
    }
    
    @Provides
    fun provideSaveSettingsUseCase(
        repository: SettingsRepository
    ): SaveSettingsUseCase {
        return SaveSettingsUseCase(repository)
    }
}
