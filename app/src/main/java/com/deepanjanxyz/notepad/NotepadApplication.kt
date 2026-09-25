package com.deepanjanxyz.notepad

import android.app.Application
import com.deepanjanxyz.notepad.data.NoteRepositoryProvider
import com.deepanjanxyz.notepad.domain.repository.NoteRepository

class NotepadApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val repository: NoteRepository = NoteRepositoryProvider.create(application)
}
