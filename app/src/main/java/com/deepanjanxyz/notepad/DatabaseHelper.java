package com.deepanjanxyz.notepad;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "notes.db";
    public static final String TABLE_NAME = "notes_table";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_CONTENT = "CONTENT";
    public static final String COLUMN_DATE = "DATE";

    /**
     * Creates the helper for the application's notes database.
     *
     * @param context context used to open or create the database
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    /**
     * Creates the table used to persist notes.
     *
     * @param db database being initialized
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, CONTENT TEXT, DATE TEXT)");
    }

    /**
     * Recreates the notes table when the database version changes.
     *
     * @param db database being upgraded
     * @param oldVersion previous schema version
     * @param newVersion requested schema version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Inserts a note without returning its generated identifier.
     *
     * @param title note title
     * @param content note body
     * @param date display date associated with the note
     */
    public void insertNote(String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        db.insert(TABLE_NAME, null, contentValues);
    }

    /**
     * Inserts a note and returns its generated identifier for subsequent auto-saves.
     *
     * @param title note title
     * @param content note body
     * @param date display date associated with the note
     * @return identifier of the inserted row, or {@code -1} if insertion failed
     */
    public long insertNoteWithId(String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        return db.insert(TABLE_NAME, null, contentValues);
    }

    /**
     * Replaces the stored values for an existing note.
     *
     * @param id identifier of the note to update
     * @param title replacement title
     * @param content replacement body
     * @param date replacement display date
     */
    public void updateNote(long id, String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        db.update(TABLE_NAME, contentValues, "ID = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Deletes a note by identifier.
     *
     * @param id identifier of the note to delete
     */
    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "ID = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Returns every note with the newest entries first.
     *
     * @return cursor owned by the caller and positioned before the first row
     */
    public Cursor getAllNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME + " order by ID desc", null);
    }

    /**
     * Searches note titles and bodies, returning the newest matches first.
     *
     * @param query text to match within titles or bodies
     * @return cursor owned by the caller and positioned before the first row
     */
    public Cursor searchNotes(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Keep search results in the same order as the main list
        return db.rawQuery("select * from " + TABLE_NAME + " WHERE TITLE LIKE ? OR CONTENT LIKE ? order by ID desc",
                new String[]{"%" + query + "%", "%" + query + "%"});
    }
}
