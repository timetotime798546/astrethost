package com.categorynotesapp.app;

import java.util.UUID;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;

    public Note(String id, String title, String content, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return title; // Used for ArrayAdapter display
    }

    public static Note fromJsonString(String jsonString) {
        // Simple JSON parsing, for a real app consider a library
        // Format: id|title|content|category
        String[] parts = jsonString.split("\\|", 4); // Fixed: escaped '|'
        if (parts.length == 4) {
            return new Note(parts[0], parts[1], parts[2], parts[3]);
        }
        return null;
    }

    public String toJsonString() {
        // Simple JSON string creation
        return id + "|" + title + "|" + content + "|" + category;
    }
}