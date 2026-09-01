package com.deepanjanxyz.notepad.di

import android.content.Context
import com.deepanjanxyz.notepad.core.data.DatabaseHelper
import com.deepanjanxyz.notepad.core.data.NoteRepository
import com.deepanjanxyz.notepad.core.data.NoteRepositoryImpl
import com.deepanjanxyz.notepad.core.data.PreferenceRepository
import com.deepanjanxyz.notepad.core.data.PreferenceRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val noteRepository: NoteRepository
    val preferenceRepository: PreferenceRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val databaseHelper: DatabaseHelper by lazy {
        DatabaseHelper(context)
    }

    override val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(databaseHelper).also { repo ->
            applicationScope.launch {
                repo.initialize()
            }
        }
    }

    override val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl(context)
    }
}
