package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.data.repository.SettingsRepository
import com.yourcompany.tradesketch.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Settings> = repository.getSettings()
}
