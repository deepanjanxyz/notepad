package com.deepanjanxyz.notepad;

import android.os.Bundle;
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
    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;
    private FloatingActionButton fabSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.editTitle);
        etContent = findViewById(R.id.editContent);
        fabSave = findViewById(R.id.fabSave);
        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }

        // বাটন টিপলে জাস্ট একটা কনফার্মেশন দেখাবে
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteLocally();
                Toast.makeText(NoteEditorActivity.this, "Note Saved", Toast.LENGTH_SHORT).show();
            }
        });

        // অটো-সেভ লজিক
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveNoteLocally();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etTitle.addTextChangedListener(watcher);
        etContent.addTextChangedListener(watcher);
    }

    private void saveNoteLocally() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());

        if (!title.isEmpty() || !content.isEmpty()) {
            if (noteId == -1) {
                dbHelper.insertNote(title, content, date);
                // আইডি আপডেট করার জন্য ডাটাবেস থেকে লাস্ট আইডি নিতে হবে, আপাতত সিম্পল রাখছি
            } else {
                dbHelper.updateNote(noteId, title, content, date);
            }
        }
    }
}
