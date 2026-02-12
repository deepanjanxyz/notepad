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
        adapter = new NoteAdapter(this, noteList);
        recyclerView.setAdapter(adapter);

        // প্লাস বাটনে ক্লিক করলে ক্র্যাশ ফিক্স করা হয়েছে
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

                if (idIndex != -1) {
                    noteList.add(new Note(
                        cursor.getLong(idIndex),
                        cursor.getString(titleIndex),
                        cursor.getString(contentIndex),
                        cursor.getString(dateIndex)
                    ));
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

    // এই অংশটা সেটিংস মেনু দেখানোর জন্য জরুরি
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
