package com.deepanjanxyz.notepad;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** SQLite helper that stores and queries the notes table. */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "notes.db";
    public static final String TABLE_NAME = "notes_table";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_CONTENT = "CONTENT";
    public static final String COLUMN_DATE = "DATE";

    /** Opens (and creates if needed) the notes database. */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    /** Creates the notes table when the database is first opened. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, CONTENT TEXT, DATE TEXT)");
    }

    /** Recreates the notes table on a schema version change. */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /** Inserts a new note; kept for backwards compatibility (no row id returned). */
    public void insertNote(String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        db.insert(TABLE_NAME, null, contentValues);
    }

    /** Inserts a new note and returns its row id (used by auto-save). */
    public long insertNoteWithId(String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        return db.insert(TABLE_NAME, null, contentValues);
    }

    /** Updates the note with the given row id. */
    public void updateNote(long id, String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TITLE, title);
        contentValues.put(COLUMN_CONTENT, content);
        contentValues.put(COLUMN_DATE, date);
        db.update(TABLE_NAME, contentValues, "ID = ?", new String[]{String.valueOf(id)});
    }

    /** Deletes the note with the given row id. */
    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "ID = ?", new String[]{String.valueOf(id)});
    }

    /** Returns a cursor over every note, newest first. */
    public Cursor getAllNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME + " order by ID desc", null);
    }

    /**
     * Returns notes whose title or content contains {@code query}, newest first.
     * SQL {@code LIKE} wildcards in the query are matched literally.
     */
    public Cursor searchNotes(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Escape SQL LIKE wildcards so a "%" or "_" typed by the user matches
        // literally instead of acting as a wildcard
        String escaped = (query == null ? "" : query)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return db.rawQuery("select * from " + TABLE_NAME
                        + " WHERE TITLE LIKE ? ESCAPE '\\' OR CONTENT LIKE ? ESCAPE '\\' order by ID desc",
                new String[]{"%" + escaped + "%", "%" + escaped + "%"});
    }
}
