package com.deepanjanxyz.notepad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> noteList;
    private TextView emptyView;
    private Menu mainMenu;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUserTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPinLock();

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        
        // অ্যাডাপ্টারে লিসেনার পাঠানো হচ্ছে
        adapter = new NoteAdapter(this, noteList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        loadNotes();
    }

    // অ্যাডাপ্টার থেকে কল আসবে যখন নোটে ক্লিক হবে
    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        startActivity(intent);
    }

    // অ্যাডাপ্টার থেকে কল আসবে যখন সিলেকশন মোড অন/অফ হবে
    @Override
    public void onSelectionModeChange(boolean selectionMode, int count) {
        this.isSelectionMode = selectionMode;
        if (mainMenu != null) {
            MenuItem deleteItem = mainMenu.findItem(R.id.action_delete_selected);
            MenuItem settingsItem = mainMenu.findItem(R.id.action_settings);
            
            deleteItem.setVisible(selectionMode); // সিলেকশন মোডে ডিলিট বাটন দেখাবে
            settingsItem.setVisible(!selectionMode); // সেটিংস লুকিয়ে যাবে
            
            if (selectionMode) {
                getSupportActionBar().setTitle(count + " Selected");
            } else {
                getSupportActionBar().setTitle("Elite Memo Pro");
            }
        }
    }

    // ব্যাক বাটন চাপলে সিলেকশন ক্লিয়ার হবে
    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            adapter.clearSelection();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mainMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } 
        // ডিলিট বাটনে ক্লিক করলে
        else if (id == R.id.action_delete_selected) {
            showDeleteConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        List<Note> selected = adapter.getSelectedNotes();
        new AlertDialog.Builder(this)
            .setTitle("Delete Notes?")
            .setMessage("Are you sure you want to delete " + selected.size() + " notes?")
            .setPositiveButton("Delete", (dialog, which) -> {
                for (Note note : selected) {
                    dbHelper.deleteNote(note.getId());
                }
                adapter.clearSelection();
                loadNotes(); // লিস্ট রিফ্রেশ
                Toast.makeText(this, "Notes Deleted!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void applyUserTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("pref_theme", "system");
        if (theme.equals("dark")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (theme.equals("light")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void checkPinLock() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLockEnabled = prefs.getBoolean("pref_lock", false);
        String savedPin = prefs.getString("pref_pin", "");
        if (isLockEnabled && !savedPin.isEmpty()) showPinDialog(savedPin);
    }

    private void showPinDialog(String correctPin) {
        final EditText input = new EditText(this);
        input.setHint("Enter PIN");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
            .setTitle("Locked")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Unlock", (dialog, which) -> {
                if (!input.getText().toString().equals(correctPin)) { finish(); }
            })
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .show();
    }

    @Override protected void onResume() { super.onResume(); loadNotes(); }

    private void loadNotes() {
        noteList.clear();
        Cursor cursor = dbHelper.getAllNotes();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                if (idIndex != -1) {
                    noteList.add(new Note(
                        cursor.getLong(idIndex),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE))
                    ));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        if (noteList.isEmpty()) { recyclerView.setVisibility(View.GONE); emptyView.setVisibility(View.VISIBLE); }
        else { recyclerView.setVisibility(View.VISIBLE); emptyView.setVisibility(View.GONE); }
        adapter.notifyDataSetChanged();
    }
}
