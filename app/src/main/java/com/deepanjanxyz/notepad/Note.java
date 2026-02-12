package com.deepanjanxyz.notepad;

public class Note {
    private int id;
    private String title;
    private String content;
    private String timestamp;

    public Note(int id, String title, String content, String timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
}
