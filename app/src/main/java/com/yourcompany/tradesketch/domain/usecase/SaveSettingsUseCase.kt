package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.data.repository.SettingsRepository
import com.yourcompany.tradesketch.domain.model.Settings
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings) = repository.saveSettings(settings)
}
