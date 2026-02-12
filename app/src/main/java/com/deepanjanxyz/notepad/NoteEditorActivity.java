package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class NoteEditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_NOTE_CONTENT = "extra_note_content";

    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private DatabaseHelper dbHelper;
    private int noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        FloatingActionButton fabSave = findViewById(R.id.fabSave);

        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
            noteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);
            String title = getIntent().getStringExtra(EXTRA_NOTE_TITLE);
            String content = getIntent().getStringExtra(EXTRA_NOTE_CONTENT);

            if (title != null) etTitle.setText(title);
            if (content != null) etContent.setText(content);

            toolbar.setTitle("Edit Note");
        } else {
            toolbar.setTitle("New Note");
        }

        fabSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (title.isEmpty()) title = "Untitled";

        if (noteId == -1) {
            dbHelper.insertNote(title, content);
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateNote(noteId, title, content);
            Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
