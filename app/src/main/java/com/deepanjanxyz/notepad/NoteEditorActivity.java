package com.deepanjanxyz.notepad;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class NoteEditorActivity extends AppCompatActivity {
    
    // এই যে দেখ, ভেরিয়েবলগুলো ক্লাসের শুরুতেই ডিক্লেয়ার করা হলো
    private EditText etTitle;
    private EditText etContent;
    private DatabaseHelper dbHelper;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_export_pdf) {
            exportToPDF();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId == -1) {
            // ফিক্স: এখানে আমরা শুধু Title আর Content পাঠাচ্ছি (Date বাদ দিয়েছি)
            dbHelper.insertNote(title, content);
        } else {
            // ফিক্স: আপডেটের সময়ও Date বাদ
            dbHelper.updateNote(noteId, title, content);
        }
        finish();
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(14);
        
        canvas.drawText("Title: " + etTitle.getText().toString(), 40, 50, paint);
        
        String[] lines = etContent.getText().toString().split("\n");
        int y = 100;
        for (String line : lines) {
            canvas.drawText(line, 40, y, paint);
            y += 20;
        }

        document.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = "Note_" + System.currentTimeMillis() + ".pdf";
        File file = new File(downloadsDir, fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Saved: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
