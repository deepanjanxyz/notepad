package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Editor screen for a single note. Edits are auto-saved with a short debounce
 * and the note's database id is preserved across activity recreation so that
 * rotating or otherwise recreating the editor updates the existing row instead
 * of inserting a duplicate note.
 */
public class NoteEditorActivity extends AppCompatActivity {
    /** Auto-save debounce delay: at most one DB write per 500 ms instead of one per keystroke. */
    private static final long AUTOSAVE_DELAY_MS = 500L;
    /** Key used both for the intent extra and the saved instance state that carries the note's database id. */
    private static final String KEY_NOTE_ID = "note_id";

    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;
    private FloatingActionButton fabSave;
    private Handler autosaveHandler;
    private boolean hasUnsavedChanges = false;

    /** Runs the pending auto-save when the debounce delay elapses. */
    private final Runnable saveRunnable = new Runnable() {
        @Override public void run() { saveNoteLocally(); }
    };

    /** Schedules a debounced auto-save whenever the title or content changes. */
    private final TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { scheduleSave(); }
        @Override public void afterTextChanged(Editable s) {}
    };

    /** Sets up the editor: restores the note id after recreation, or loads the note from the launch intent. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.editTitle);
        etContent = findViewById(R.id.editContent);
        fabSave = findViewById(R.id.fabSave);
        dbHelper = new DatabaseHelper(this);
        autosaveHandler = new Handler(Looper.getMainLooper());

        if (savedInstanceState != null) {
            // Recreated after rotation/config change: keep the id of the note
            // this instance already saved so the next auto-save updates that
            // row instead of inserting a duplicate. The typed text is restored
            // automatically from the saved view state.
            noteId = savedInstanceState.getLong(KEY_NOTE_ID, -1);
        } else if (getIntent().hasExtra(KEY_NOTE_ID)) {
            noteId = getIntent().getLongExtra(KEY_NOTE_ID, -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }

        // Save button: save immediately and return to the notes list
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteLocally();
                Toast.makeText(NoteEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Debounced auto-save: text edits are flushed at most once every 500 ms
        etTitle.addTextChangedListener(watcher);
        etContent.addTextChangedListener(watcher);
    }

    /**
     * Persists the note's database id across activity recreation. Any pending
     * auto-save is flushed first so the saved id always matches the database.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        autosaveHandler.removeCallbacks(saveRunnable);
        if (hasUnsavedChanges) saveNoteLocally();
        outState.putLong(KEY_NOTE_ID, noteId);
        super.onSaveInstanceState(outState);
    }

    /** (Re)schedules the debounced auto-save after a text change. */
    private void scheduleSave() {
        hasUnsavedChanges = true;
        autosaveHandler.removeCallbacks(saveRunnable);
        autosaveHandler.postDelayed(saveRunnable, AUTOSAVE_DELAY_MS);
    }

    /**
     * Writes the current title/content to the database: inserts a new row for a
     * brand-new note, updates the existing row afterwards, and deletes the
     * note entirely once all of its text has been cleared.
     */
    private void saveNoteLocally() {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();
        String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
        hasUnsavedChanges = false;

        boolean hasText = !title.trim().isEmpty() || !content.trim().isEmpty();
        if (hasText) {
            if (noteId == -1) {
                noteId = dbHelper.insertNoteWithId(title, content, date);
            } else {
                dbHelper.updateNote(noteId, title, content, date);
            }
        } else if (noteId != -1) {
            // The note was emptied out: remove it instead of leaving a stale
            // copy of the previous text in the list
            dbHelper.deleteNote(noteId);
            noteId = -1;
        }
    }

    /** Flushes any pending auto-save so no typed text is ever lost when leaving the editor. */
    @Override
    protected void onPause() {
        super.onPause();
        autosaveHandler.removeCallbacks(saveRunnable);
        if (hasUnsavedChanges) saveNoteLocally();
    }

    /** Cancels any pending auto-save when the editor is destroyed. */
    @Override
    protected void onDestroy() {
        autosaveHandler.removeCallbacks(saveRunnable);
        super.onDestroy();
    }
}
