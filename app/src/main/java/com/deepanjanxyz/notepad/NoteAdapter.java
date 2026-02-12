package com.deepanjanxyz.notepad;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private Context context;
    private List<Note> noteList;

    public NoteAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // আমাদের নতুন কার্ড ডিজাইন লোড হচ্ছে
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.title.setText(note.getTitle());
        holder.date.setText(note.getDate());
        holder.content.setText(note.getContent());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteEditorActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("title", note.getTitle());
            intent.putExtra("content", note.getContent());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return noteList.size(); }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, content;
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            date = itemView.findViewById(R.id.textDate);
            content = itemView.findViewById(R.id.textContent);
        }
    }
}
