package com.deepanjanxyz.notepad.core.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.deepanjanxyz.notepad.core.model.Note

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "notes.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "notes_table"
        const val COLUMN_ID = "ID"
        const val COLUMN_TITLE = "TITLE"
        const val COLUMN_CONTENT = "CONTENT"
        const val COLUMN_DATE = "DATE"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TITLE TEXT, " +
                "$COLUMN_CONTENT TEXT, " +
                "$COLUMN_DATE TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertNote(title: String, content: String, date: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_DATE, date)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun updateNote(id: Long, title: String, content: String, date: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_DATE, date)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteNotes(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val db = writableDatabase
        val placeholders = ids.joinToString(",") { "?" }
        val stringIds = ids.map { it.toString() }.toTypedArray()
        return db.delete(TABLE_NAME, "$COLUMN_ID IN ($placeholders)", stringIds)
    }

    fun getNoteById(id: Long): Note? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                mapCursorToNote(it)
            } else {
                null
            }
        }
    }

    fun getAllNotes(): List<Note> {
        val db = readableDatabase
        val notes = mutableListOf<Note>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                notes.add(mapCursorToNote(it))
            }
        }
        return notes
    }

    fun searchNotes(query: String): List<Note> {
        val db = readableDatabase
        val notes = mutableListOf<Note>()
        val wildQuery = "%$query%"
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_TITLE LIKE ? OR $COLUMN_CONTENT LIKE ? ORDER BY $COLUMN_ID DESC",
            arrayOf(wildQuery, wildQuery)
        )
        cursor.use {
            while (it.moveToNext()) {
                notes.add(mapCursorToNote(it))
            }
        }
        return notes
    }

    private fun mapCursorToNote(cursor: Cursor): Note {
        val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
        val titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
        val contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT)
        val dateIndex = cursor.getColumnIndexOrThrow(COLUMN_DATE)

        return Note(
            id = cursor.getLong(idIndex),
            title = cursor.getString(titleIndex) ?: "",
            content = cursor.getString(contentIndex) ?: "",
            date = cursor.getString(dateIndex) ?: ""
        )
    }
}
