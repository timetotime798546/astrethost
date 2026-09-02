package com.smartnotes.app;

public class Note {
    public long id;
    public String title;
    public String content;
    public String category;
    public String timestamp;

    public Note(long id, String title, String content, String category, String timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
    }
}