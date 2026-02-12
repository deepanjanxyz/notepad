package com.deepanjanxyz.notepad;

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
}
