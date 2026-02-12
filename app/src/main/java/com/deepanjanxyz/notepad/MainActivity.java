package com.deepanjanxyz.notepad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> noteList;
    private TextView emptyView;
    private Menu mainMenu;
    private boolean isSelectionMode = false;
    private boolean isAuthenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUserTheme();
        super.onCreate(savedInstanceState);
        
        if (isLockEnabled() && !isAuthenticated) {
            setContentView(new View(this)); 
            showBiometricPrompt();
        } else {
            initUI();
        }
    }

    private void initUI() {
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();
        
        setSupportActionBar(findViewById(R.id.toolbar));
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new NoteAdapter(this, noteList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        loadNotes("");
    }

    private boolean isLockEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("pref_lock", false);
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                finish();
            }
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isAuthenticated = true;
                initUI();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Elite Memo Security")
                .setSubtitle("Unlock to access your notes")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        startActivity(intent);
    }

    @Override
    public void onSelectionModeChange(boolean selectionMode, int count) {
        this.isSelectionMode = selectionMode;
        if (mainMenu != null) {
            mainMenu.findItem(R.id.action_delete_selected).setVisible(selectionMode);
            mainMenu.findItem(R.id.action_search).setVisible(!selectionMode);
            mainMenu.findItem(R.id.action_settings).setVisible(!selectionMode);
            if (selectionMode) getSupportActionBar().setTitle(count + " Selected");
            else getSupportActionBar().setTitle("Elite Memo Pro");
        }
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) adapter.clearSelection();
        else super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mainMenu = menu;
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search notes...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { loadNotes(query); return false; }
            @Override public boolean onQueryTextChange(String newText) { loadNotes(newText); return false; }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; } 
        else if (id == R.id.action_delete_selected) { showDeleteConfirmation(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        List<Note> selected = adapter.getSelectedNotes();
        new AlertDialog.Builder(this)
            .setTitle("Delete Notes?")
            .setMessage("Delete " + selected.size() + " notes?")
            .setPositiveButton("Delete", (dialog, which) -> {
                for (Note note : selected) dbHelper.deleteNote(note.getId());
                adapter.clearSelection();
                loadNotes("");
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

    @Override protected void onResume() { 
        super.onResume(); 
        if (isAuthenticated || !isLockEnabled()) loadNotes(""); 
    }

    private void loadNotes(String query) {
        if (noteList == null) return;
        noteList.clear();
        Cursor cursor = (query.isEmpty()) ? dbHelper.getAllNotes() : dbHelper.searchNotes(query);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                noteList.add(new Note(
                    cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT)),
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        if (noteList.isEmpty()) { recyclerView.setVisibility(View.GONE); emptyView.setVisibility(View.VISIBLE); }
        else { recyclerView.setVisibility(View.VISIBLE); emptyView.setVisibility(View.GONE); }
        adapter.notifyDataSetChanged();
    }
}
