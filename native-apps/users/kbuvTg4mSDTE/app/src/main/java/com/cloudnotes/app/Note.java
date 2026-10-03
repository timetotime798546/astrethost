package com.cloudnotes.app;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private String date;

    public Note(String id, String title, String content, String category, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    // MANDATORY OFFLINE CALCULATION: Dynamic helper to calculate word count locally inside app code.
    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }
}