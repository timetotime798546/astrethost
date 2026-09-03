package com.smartnotes.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private String category;
    private String date;

    public Note(long id, String title, String content, String category, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.date = date;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
}