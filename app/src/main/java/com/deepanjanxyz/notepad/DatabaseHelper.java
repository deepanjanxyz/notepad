package com.deepanjanxyz.notepad;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "EliteMemoPro.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";

    public DatabaseHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_TITLE + " TEXT, " + COLUMN_CONTENT + " TEXT, " + COLUMN_DATE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertNote(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_DATE, getBitmapDate());
        return db.insert(TABLE_NAME, null, values);
    }

    public void updateNote(long id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_DATE, getBitmapDate());
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor getAllNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, COLUMN_ID + " DESC");
    }
    
    // নতুন সার্চ মেথড
    public Cursor searchNotes(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?", new String[]{"%" + query + "%", "%" + query + "%"}, null, null, COLUMN_ID + " DESC");
    }

    private String getBitmapDate() {
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
    }
}
