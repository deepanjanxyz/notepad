package com.deepanjanxyz.notepad;

import java.util.Objects;

public class Note {
    private long id;
    private String title;
    private String content;
    private String date;

    // কনস্ট্রাক্টর (ডেট সহ)
    public Note(long id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    // আইডি দিয়ে তুলনা, যাতে লিস্ট রিলোডের পরেও সিলেকশন ঠিক থাকে
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        return id == ((Note) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
