package com.example.elitememopro;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList = new ArrayList<>();
    private List<Note> filteredList = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);

        // নোট ক্লিক করলে এডিটর খুলবে (Data pass করে)
        noteAdapter = new NoteAdapter(this, filteredList, note -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.getId());
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_TITLE, note.getTitle());
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_CONTENT, note.getContent());
            startActivity(intent);
        }, note -> {
            dbHelper.deleteNote(note.getId());
            loadNotes();
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(noteAdapter);

        // নতুন নোট অ্যাড করার জন্য এডিটর খোলা
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoteEditorActivity.class));
        });

        loadNotes();
    }

    private void loadNotes() {
        noteList.clear();
        filteredList.clear();
        Cursor cursor = dbHelper.getAllNotes();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
                noteList.add(new Note(id, title, content, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        filteredList.addAll(noteList);
        if (noteAdapter != null) noteAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });
        return true;
    }

    private void filterNotes(String query) {
        filteredList.clear();
        if (query.trim().isEmpty()) {
            filteredList.addAll(noteList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Note note : noteList) {
                if (note.getTitle().toLowerCase().contains(lowerQuery) ||
                    note.getContent().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(note);
                }
            }
        }
        noteAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            dbHelper.deleteAllNotes();
            loadNotes();
            Toast.makeText(this, "All notes deleted", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
