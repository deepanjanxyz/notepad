package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_EliteMemoPro);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new DatabaseHelper(this);
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        FloatingActionButton fabSave = findViewById(R.id.fabSave);

        // যদি পুরনো নোট এডিট করতে আসি, তবে তার ডেটাগুলো রিসিভ করে বসানো হচ্ছে
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            editTitle.setText(getIntent().getStringExtra("title"));
            editContent.setText(getIntent().getStringExtra("content"));
        }

        // অটো-সেভ লজিক (টাইপ করলেই সেভ হবে)
        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveNoteAuto();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTitle.addTextChangedListener(autoSaveWatcher);
        editContent.addTextChangedListener(autoSaveWatcher);

        // সেভ বাটনে ক্লিক করলেও সেভ হবে এবং বেরিয়ে আসবে
        fabSave.setOnClickListener(v -> {
            saveNoteAuto();
            finish();
        });
    }

    private void saveNoteAuto() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) return;

        if (noteId == -1) {
            noteId = dbHelper.insertNote(title, content);
        } else {
            dbHelper.updateNote(noteId, title, content);
        }
    }
}
