package com.deepanjanxyz.notepad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            editTitle.setText(getIntent().getStringExtra("title"));
            editContent.setText(getIntent().getStringExtra("content"));
        }

        TextWatcher autoSave = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { saveNote(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTitle.addTextChangedListener(autoSave);
        editContent.addTextChangedListener(autoSave);

        fabSave.setOnClickListener(v -> { saveNote(); finish(); });
    }

    private void saveNote() {
        String t = editTitle.getText().toString().trim();
        String c = editContent.getText().toString().trim();
        if (t.isEmpty() && c.isEmpty()) return;
        if (noteId == -1) noteId = dbHelper.insertNote(t, c);
        else dbHelper.updateNote(noteId, t, c);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            if (noteId != -1) {
                new AlertDialog.Builder(this)
                    .setTitle("Delete Note?")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteNote(noteId);
                        Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null).show();
            } else Toast.makeText(this, "Note not saved yet!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export_pdf) {
            exportToPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToPdf() {
        // সিম্পল PDF এক্সপোর্ট লজিক
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(24);
        canvas.drawText(editTitle.getText().toString(), 10, 50, paint);
        
        paint.setTextSize(14);
        int y = 80;
        for (String line : editContent.getText().toString().split("\n")) {
            canvas.drawText(line, 10, y, paint);
            y += 20;
        }
        
        document.finishPage(page);

        String fileName = "Note_" + System.currentTimeMillis() + ".pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        
        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
