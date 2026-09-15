package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditorActivity extends AppCompatActivity {
    /** Auto-save debounce delay: at most one DB write per 500 ms instead of one per keystroke. */
    private static final long AUTOSAVE_DELAY_MS = 500L;

    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;
    private FloatingActionButton fabSave;
    private Handler autosaveHandler;
    private boolean hasUnsavedChanges = false;

    private final Runnable saveRunnable = new Runnable() {
        /** Writes pending editor changes after the debounce delay. */
        @Override
        public void run() { saveNoteLocally(); }
    };

    private final TextWatcher watcher = new TextWatcher() {
        /** Receives the pre-change notification; no work is required. */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        /** Schedules a save whenever title or body text changes. */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { scheduleSave(); }

        /** Receives the post-change notification; no work is required. */
        @Override
        public void afterTextChanged(Editable s) {}
    };

    /**
     * Initializes the editor and restores note values supplied by the list screen.
     *
     * @param savedInstanceState previously saved activity state, if available
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.editTitle);
        etContent = findViewById(R.id.editContent);
        fabSave = findViewById(R.id.fabSave);
        dbHelper = new DatabaseHelper(this);
        autosaveHandler = new Handler(Looper.getMainLooper());

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }

        // Save button: save immediately and return to the notes list
        fabSave.setOnClickListener(new View.OnClickListener() {
            /**
             * Saves immediately and closes the editor.
             *
             * @param v save button that was clicked
             */
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

    /** Restarts the debounce timer for pending editor changes. */
    private void scheduleSave() {
        hasUnsavedChanges = true;
        autosaveHandler.removeCallbacks(saveRunnable);
        autosaveHandler.postDelayed(saveRunnable, AUTOSAVE_DELAY_MS);
    }

    /** Inserts, updates, or deletes the current note to match the editor contents. */
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

    /** Flushes pending changes before the editor leaves the foreground. */
    @Override
    protected void onPause() {
        super.onPause();
        // Flush any pending auto-save so no typed text is ever lost
        autosaveHandler.removeCallbacks(saveRunnable);
        if (hasUnsavedChanges) saveNoteLocally();
    }

    /** Removes queued callbacks when the editor is destroyed. */
    @Override
    protected void onDestroy() {
        autosaveHandler.removeCallbacks(saveRunnable);
        super.onDestroy();
    }
}
