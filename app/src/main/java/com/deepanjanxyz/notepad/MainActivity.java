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
    private boolean isAuthenticated = false; // লক স্ট্যাটাস ট্র্যাক করার জন্য

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUserTheme();
        super.onCreate(savedInstanceState);
        
        // লক চেক করা হচ্ছে অ্যাপ লোড হওয়ার আগেই
        if (isLockEnabled() && !isAuthenticated) {
            // স্ক্রিন খালি রাখার জন্য কন্টেন্ট ভিউ পরে সেট করা হবে বা হাইড রাখা হবে
            setContentView(new View(this)); 
            showBiometricPrompt();
        } else {
            initUI();
        }
    }

    // আসল UI লোড করার মেথড
    private void initUI() {
        setContentView(R.layout.activity_main);
        
        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new NoteAdapter(this, noteList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        loadNotes();
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
                Toast.makeText(getApplicationContext(), "Authentication required!", Toast.LENGTH_SHORT).show();
                finish(); // অথেন্টিকেশন না দিলে অ্যাপ বন্ধ
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isAuthenticated = true;
                initUI(); // পাসওয়ার্ড মিললে অ্যাপ খুলবে
                Toast.makeText(getApplicationContext(), "Unlocked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Elite Memo Security")
                .setSubtitle("Unlock to access your notes")
                // এই লাইনটা ম্যাজিক! এটা ফিঙ্গারপ্রিন্ট না থাকলে পিন বা প্যাটার্ন চাইবে
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
                loadNotes();
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
        if (isAuthenticated || !isLockEnabled()) loadNotes(); 
    }

    private void loadNotes() {
        if (noteList == null) return;
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
