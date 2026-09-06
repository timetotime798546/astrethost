package com.classicnotes.app;

public class Note {
    private int id;
    private String title;
    private String content;
    private int categoryId;
    private String categoryName;
    private String date;

    public Note(int id, String title, String content, int categoryId, String categoryName, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.date = date;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getDate() { return date; }
}