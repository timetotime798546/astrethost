package com.notecraft.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private long categoryId;
    private String categoryName;
    private String timestamp;

    public Note(long id, String title, String content, long categoryId, String categoryName, String timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.timestamp = timestamp;
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

    public long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
