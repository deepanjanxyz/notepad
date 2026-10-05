package com.deepanjanxyz.notepad.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        /**
         * Migrates the pre-Room database to the current Room schema.
         *
         * Room was only ever shipped at version 5, so the single historical schema
         * that can still be on disk is the old SQLiteOpenHelper `notes.db`
         * (`user_version` 1) with a `notes_table(ID, TITLE, CONTENT, DATE)` and no
         * labels table. Without this migration a user updating directly from a
         * pre-Room release would hit a missing-migration failure; the table is
         * recreated to match Room's expected schema and the existing notes are
         * copied across (the columns Room added fall back to their defaults).
         */
        private val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes_table_new` (" +
                        "`ID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`TITLE` TEXT NOT NULL, " +
                        "`CONTENT` TEXT NOT NULL, " +
                        "`DATE` TEXT NOT NULL, " +
                        "`PINNED` INTEGER NOT NULL DEFAULT 0, " +
                        "`COLOR_INDEX` INTEGER NOT NULL DEFAULT 0, " +
                        "`TAGS` TEXT NOT NULL DEFAULT '', " +
                        "`IN_TRASH` INTEGER NOT NULL DEFAULT 0, " +
                        "`IN_ARCHIVE` INTEGER NOT NULL DEFAULT 0, " +
                        "`REMINDER_TIME` INTEGER)"
                )
                db.execSQL(
                    "INSERT INTO `notes_table_new` (`ID`, `TITLE`, `CONTENT`, `DATE`) " +
                        "SELECT `ID`, COALESCE(`TITLE`, ''), COALESCE(`CONTENT`, ''), " +
                        "COALESCE(`DATE`, '') FROM `notes_table`"
                )
                db.execSQL("DROP TABLE `notes_table`")
                db.execSQL("ALTER TABLE `notes_table_new` RENAME TO `notes_table`")

                // The labels table did not exist before Room.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `labels_table` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL)"
                )
            }
        }

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
                    // acceptable. The legacy pre-Room schema is handled explicitly
                    // by MIGRATION_1_5, and exported schemas (see the
                    // room.schemaLocation argument) make it possible to author a
                    // further Migration for any future version bump.
                    .addMigrations(MIGRATION_1_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
