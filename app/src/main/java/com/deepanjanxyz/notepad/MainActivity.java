package com.deepanjanxyz.notepad;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> noteList;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_EliteMemoPro);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // সিম্পল কনস্ট্রাক্টর ব্যবহার করছি
        adapter = new NoteAdapter(this, noteList);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NoteEditorActivity.class)));

        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        noteList.clear();
        Cursor cursor = dbHelper.getAllNotes();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int titleIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE);
                int contentIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT);
                int dateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);

                if (idIndex != -1 && titleIndex != -1 && contentIndex != -1 && dateIndex != -1) {
                    long id = cursor.getLong(idIndex);
                    String title = cursor.getString(titleIndex);
                    String content = cursor.getString(contentIndex);
                    String date = cursor.getString(dateIndex);
                    
                    // এখানে int কাস্টিং সমস্যা এড়াতে সরাসরি long নিচ্ছি
                    noteList.add(new Note((int)id, title, content, date));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (noteList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
