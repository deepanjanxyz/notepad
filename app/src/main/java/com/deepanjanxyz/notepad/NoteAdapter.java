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
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.title.setText(note.getTitle());
        holder.content.setText(note.getDate());
        
        // এই অংশটাই আসল! ক্লিক করলে নোট এডিটর খুলবে
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteEditorActivity.class);
            intent.putExtra("note_id", note.getId()); // নোটের আইডি পাঠানো হচ্ছে
            intent.putExtra("title", note.getTitle()); // টাইটেল পাঠানো হচ্ছে
            intent.putExtra("content", note.getContent()); // আসল লেখা পাঠানো হচ্ছে
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return noteList.size(); }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content;
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            content = itemView.findViewById(android.R.id.text2);
        }
    }
}
