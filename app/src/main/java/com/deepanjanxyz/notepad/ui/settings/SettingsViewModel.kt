package com.deepanjanxyz.notepad.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.deepanjanxyz.notepad.data.AppSettings
import com.deepanjanxyz.notepad.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository.getInstance(application)

    val settings: StateFlow<AppSettings> = repository.settings

    fun setTheme(theme: String) = repository.setTheme(theme)

    fun setLockEnabled(enabled: Boolean) = repository.setLockEnabled(enabled)
}
