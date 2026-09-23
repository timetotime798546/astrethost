package com.notewise.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private long categoryId;
    private String categoryName;
    private int colorIndex;
    private long timestamp;

    public Note(long id, String title, String content, long categoryId, String categoryName, int colorIndex, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.colorIndex = colorIndex;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public int getColorIndex() { return colorIndex; }
    public long getTimestamp() { return timestamp; }
}