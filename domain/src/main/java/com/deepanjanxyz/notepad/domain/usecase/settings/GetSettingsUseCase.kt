package com.deepanjanxyz.notepad.domain.usecase.settings

import com.deepanjanxyz.notepad.domain.model.AppSettings
import com.deepanjanxyz.notepad.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Observes the user's settings as a stream. */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settings
}
