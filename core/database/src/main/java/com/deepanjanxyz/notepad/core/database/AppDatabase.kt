package com.deepanjanxyz.notepad.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.deepanjanxyz.notepad.core.database.dao.LabelDao
import com.deepanjanxyz.notepad.core.database.dao.NoteDao
import com.deepanjanxyz.notepad.core.database.entity.LabelEntity
import com.deepanjanxyz.notepad.core.database.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, LabelEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
