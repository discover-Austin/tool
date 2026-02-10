package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Settings> = repository.getSettings()
}
