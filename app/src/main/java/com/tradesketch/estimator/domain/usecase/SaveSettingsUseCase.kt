package com.tradesketch.estimator.domain.usecase

import com.tradesketch.estimator.data.repository.SettingsRepository
import com.tradesketch.estimator.domain.model.Settings
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings) = repository.saveSettings(settings)
}
