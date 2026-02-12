package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditorActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.editTitle);
        etContent = findViewById(R.id.editContent);
        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }

        // অটো-সেভ লজিক: যখনই তুই টাইটেল বা নোট লিখবি, ওটা অটো সেভ হবে
        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoSaveNote();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etTitle.addTextChangedListener(autoSaveWatcher);
        etContent.addTextChangedListener(autoSaveWatcher);
    }

    private void autoSaveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());

        if (!title.isEmpty() || !content.isEmpty()) {
            if (noteId == -1) {
                // প্রথমবার লিখলেই আইডি জেনারেট হবে
                noteId = dbHelper.insertNoteWithId(title, content, date);
            } else {
                // প্রতিবার টাইপ করার সময় আপডেট হবে
                dbHelper.updateNote(noteId, title, content, date);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoSaveNote(); // অ্যাপ থেকে বের হওয়ার সময়ও সেভ হবে
    }
}
