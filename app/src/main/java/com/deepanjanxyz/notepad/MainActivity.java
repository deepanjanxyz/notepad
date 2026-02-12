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

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> noteList;
    private TextView emptyView;

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

        // এই সেই 'চৌকো চৌকো' ডিজাইন (Grid Layout)
        // ২ কলামের Staggered Grid ব্যবহার করা হয়েছে
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        
        adapter = new NoteAdapter(this, noteList);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        loadNotes();
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

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.main_menu, menu); return true; }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
