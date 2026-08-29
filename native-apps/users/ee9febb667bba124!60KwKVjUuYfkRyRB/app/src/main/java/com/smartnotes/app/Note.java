package com.smartnotes.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private String category;
    private long date;

    public Note(long id, String title, String content, String category, long date) {
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
    public long getDate() { return date; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setDate(long date) { this.date = date; }
}