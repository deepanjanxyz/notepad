package com.deepanjanxyz.notepad

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local-first storage layer for notes.
 *
 * The schema, database name and database version are identical to the original
 * Java implementation, so existing installs keep every note untouched.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "create table $TABLE_NAME (" +
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

    /**
     * Inserts a note and returns the new row id, or -1 if the insert failed.
     */
    fun insertNote(title: String, content: String, date: String): Long =
        writableDatabase.insert(TABLE_NAME, null, contentValues(title, content, date))

    /**
     * Updates a note and returns the number of rows affected (0 if nothing
     * was written, e.g. the row no longer exists).
     */
    fun updateNote(id: Long, title: String, content: String, date: String): Int =
        writableDatabase.update(
            TABLE_NAME,
            contentValues(title, content, date),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
        )

    fun deleteNote(id: Long) {
        writableDatabase.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun getNoteById(id: Long): Note? =
        readableDatabase
            .rawQuery("select * from $TABLE_NAME where $COLUMN_ID = ?", arrayOf(id.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.toNote() else null }

    fun getAllNotes(sortOrder: NoteSortOrder = NoteSortOrder.NEWEST): List<Note> =
        queryNotes(selection = null, selectionArgs = emptyArray(), sortOrder = sortOrder)

    /**
     * Full-text search with a configurable scope and ordering.
     */
    fun searchNotes(
        query: String,
        scope: SearchScope = SearchScope.ALL,
        sortOrder: NoteSortOrder = NoteSortOrder.NEWEST,
    ): List<Note> {
        val pattern = "%$query%"
        val (selection, selectionArgs) = when (scope) {
            SearchScope.TITLE -> "$COLUMN_TITLE LIKE ?" to arrayOf(pattern)
            SearchScope.CONTENT -> "$COLUMN_CONTENT LIKE ?" to arrayOf(pattern)
            SearchScope.ALL -> "($COLUMN_TITLE LIKE ? OR $COLUMN_CONTENT LIKE ?)" to arrayOf(pattern, pattern)
        }
        return queryNotes(selection, selectionArgs, sortOrder)
    }

    private fun queryNotes(
        selection: String?,
        selectionArgs: Array<String>,
        sortOrder: NoteSortOrder,
    ): List<Note> {
        val whereClause = selection?.let { "WHERE $it" } ?: ""
        return readableDatabase
            .rawQuery(
                "select * from $TABLE_NAME $whereClause ${sortOrder.orderByClause}",
                selectionArgs,
            )
            .use { cursor -> cursor.toListOfNotes() }
    }

    private fun contentValues(title: String, content: String, date: String): ContentValues =
        ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_DATE, date)
        }

    private fun Cursor.toListOfNotes(): List<Note> = buildList {
        while (moveToNext()) add(toNote())
    }

    private fun Cursor.toNote(): Note =
        Note(
            id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
            title = getString(getColumnIndexOrThrow(COLUMN_TITLE)).orEmpty(),
            content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)).orEmpty(),
            date = getString(getColumnIndexOrThrow(COLUMN_DATE)).orEmpty(),
        )

    companion object {
        const val DATABASE_NAME = "notes.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "notes_table"
        const val COLUMN_ID = "ID"
        const val COLUMN_TITLE = "TITLE"
        const val COLUMN_CONTENT = "CONTENT"
        const val COLUMN_DATE = "DATE"
    }
}
