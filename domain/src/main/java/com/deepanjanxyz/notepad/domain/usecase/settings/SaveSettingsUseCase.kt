package com.deepanjanxyz.notepad.domain.usecase.settings

import com.deepanjanxyz.notepad.domain.repository.SettingsRepository

/** Persists changes to individual settings. */
class SaveSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend fun setThemeMode(themeMode: String) = settingsRepository.setThemeMode(themeMode)

    suspend fun setGridLayout(isGridLayout: Boolean) = settingsRepository.setGridLayout(isGridLayout)

    suspend fun setLockEnabled(enabled: Boolean) = settingsRepository.setLockEnabled(enabled)
}
