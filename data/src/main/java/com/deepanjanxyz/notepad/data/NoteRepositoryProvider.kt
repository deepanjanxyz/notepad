package com.deepanjanxyz.notepad.data

import android.content.Context
import com.deepanjanxyz.notepad.data.local.database.AppDatabase
import com.deepanjanxyz.notepad.data.repository.NoteRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.NoteRepository

object NoteRepositoryProvider {
    fun create(context: Context): NoteRepository {
        val database = AppDatabase.getInstance(context)
        return NoteRepositoryImpl(
            noteDao = database.noteDao(),
            labelDao = database.labelDao(),
            database = database
        )
    }
}
