package com.deepanjanxyz.notepad.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepanjanxyz.notepad.core.data.NoteRepository
import com.deepanjanxyz.notepad.core.data.PreferenceRepository
import com.deepanjanxyz.notepad.core.model.ThemeMode
import com.deepanjanxyz.notepad.core.security.BiometricHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    context: Context,
    private val preferenceRepository: PreferenceRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val isBiometricSupported = BiometricHelper.isBiometricAvailable(context)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferenceRepository.observeThemeMode(),
        preferenceRepository.observeBiometricLockEnabled(),
        noteRepository.observeStats()
    ) { theme, lock, stats ->
        SettingsUiState(
            themeMode = theme,
            isBiometricLockEnabled = lock,
            isBiometricAvailable = isBiometricSupported,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isBiometricAvailable = isBiometricSupported)
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferenceRepository.setThemeMode(mode)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setBiometricLockEnabled(enabled)
        }
    }
}
