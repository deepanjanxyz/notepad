package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1; // -1 মানে নতুন নোট

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // থিম নিশ্চিত করা হচ্ছে
        setTheme(R.style.Theme_EliteMemoPro);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new DatabaseHelper(this);
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        FloatingActionButton fabSave = findViewById(R.id.fabSave);

        // যদি পুরনো নোট এডিট করতে আসি
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            editTitle.setText(getIntent().getStringExtra("title"));
            editContent.setText(getIntent().getStringExtra("content"));
        }

        fabSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Empty note discarded", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (noteId == -1) {
            // নতুন নোট
            dbHelper.insertNote(title, content);
        } else {
            // পুরনো নোট আপডেট
            dbHelper.updateNote(noteId, title, content);
        }
        finish(); // সেভ করে বেরিয়ে আসবে
    }
}
