package com.deepanjanxyz.notepad.core.data

import com.deepanjanxyz.notepad.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeBiometricLockEnabled(): Flow<Boolean>
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    fun observeIsGridLayout(): Flow<Boolean>
    suspend fun setGridLayout(isGrid: Boolean)
}
