package com.deepanjanxyz.notepad.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.deepanjanxyz.notepad.data.local.dao.LabelDao
import com.deepanjanxyz.notepad.data.local.dao.NoteDao
import com.deepanjanxyz.notepad.data.local.entity.LabelEntity
import com.deepanjanxyz.notepad.data.local.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, LabelEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao

    companion object {
        private const val DATABASE_NAME = "notes.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Destructive migration is deliberately NOT enabled: silently
                    // wiping the user's notes when the schema changes is never
                    // acceptable. Exported schemas (see the room.schemaLocation
                    // argument) make it possible to author an explicit Migration
                    // and register it here via addMigrations(...) before any
                    // future version bump ships.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
