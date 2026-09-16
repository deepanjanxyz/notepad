package com.deepanjanxyz.notepad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private Context context;
    private List<Note> noteList;
    private List<Note> selectedNotes = new ArrayList<>();
    private boolean isSelectionMode = false;
    private OnNoteListener listener;

    // ইন্টারফেস মেইন অ্যাক্টিভিটির সাথে কথা বলার জন্য
    public interface OnNoteListener {
        void onNoteClick(Note note);
        void onSelectionModeChange(boolean isSelectionMode, int selectedCount);
    }

    public NoteAdapter(Context context, List<Note> noteList, OnNoteListener listener) {
        this.context = context;
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    /**
     * Binds a note card, deriving a blank title from the first content line or
     * an untitled placeholder, and displaying a missing date as empty text.
     */
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        // Never render a blank heading: fall back to the first line of the
        // note's content when no title was typed, or a placeholder when the
        // note is otherwise empty
        String title = note.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String content = note.getContent() == null ? "" : note.getContent();
            title = content.trim().isEmpty() ? "(Untitled)" : content.split("\n", 2)[0].trim();
        }
        holder.title.setText(title);
        holder.date.setText(note.getDate() == null ? "" : note.getDate());
        holder.content.setText(note.getContent());

        // সিলেকশন মোড চেক করা
        if (isSelectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedNotes.contains(note));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }

        // ক্লিক লজিক
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(note);
            } else {
                listener.onNoteClick(note);
            }
        });

        // লং প্রেস লজিক
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                selectedNotes.clear();
                toggleSelection(note);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(Note note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note);
        } else {
            selectedNotes.add(note);
        }
        
        // যদি সব আনচেক করে দেয়, মোড বন্ধ হয়ে যাবে
        if (selectedNotes.isEmpty()) {
            isSelectionMode = false;
        }
        
        notifyDataSetChanged();
        listener.onSelectionModeChange(isSelectionMode, selectedNotes.size());
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedNotes.clear();
        notifyDataSetChanged();
        listener.onSelectionModeChange(false, 0);
    }
    
    public List<Note> getSelectedNotes() {
        return selectedNotes;
    }

    @Override
    public int getItemCount() { return noteList.size(); }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, content;
        CheckBox checkBox;
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            date = itemView.findViewById(R.id.textDate);
            content = itemView.findViewById(R.id.textContent);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
