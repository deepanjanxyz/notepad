package com.deepanjanxyz.notepad;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        // ফিক্স: তোর XML ফাইলের সঠিক নাম ব্যবহার করা হলো (editTitle, editContent)
        etTitle = findViewById(R.id.editTitle);
        etContent = findViewById(R.id.editContent);
        fabSave = findViewById(R.id.fabSave);
        
        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getLongExtra("note_id", -1);
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
        }

        // ফিক্স: তোর স্ক্রিনের গোল সেভ বাটনটা সচল করা হলো
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
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
        
        // তারিখ ফরম্যাট সুন্দর করা হলো
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId == -1) {
            dbHelper.insertNote(title, content, date);
        } else {
            dbHelper.updateNote(noteId, title, content, date);
        }
        finish();
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(16);
        
        // টাইটেল একটু বড় করে লেখা হবে
        paint.setFakeBoldText(true);
        canvas.drawText("Title: " + etTitle.getText().toString(), 40, 50, paint);
        
        // কনটেন্ট স্বাভাবিক ফন্টে
        paint.setFakeBoldText(false);
        paint.setTextSize(14);
        
        String[] lines = etContent.getText().toString().split("\n");
        int y = 90;
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
