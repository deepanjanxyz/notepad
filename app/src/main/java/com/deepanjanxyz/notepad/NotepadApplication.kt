package com.deepanjanxyz.notepad

import android.app.Application
import com.deepanjanxyz.notepad.data.local.database.AppDatabase
import com.deepanjanxyz.notepad.data.repository.NoteRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class NotepadApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = AppDatabase.getInstance(application)
    val repository: NoteRepository = NoteRepositoryImpl(
        noteDao = database.noteDao(),
        labelDao = database.labelDao()
    )
}
