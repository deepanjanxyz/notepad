package com.deepanjanxyz.notepad.domain.usecase.settings

/** Aggregate of the settings use cases, mirroring the note/label use case groups. */
data class SettingsUseCases(
    val getSettings: GetSettingsUseCase,
    val saveSettings: SaveSettingsUseCase
)
