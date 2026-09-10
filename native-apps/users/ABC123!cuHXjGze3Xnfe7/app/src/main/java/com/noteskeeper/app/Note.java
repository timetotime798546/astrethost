package com.noteskeeper.app;

public class Note {
    public long id;
    public String title;
    public String content;
    public String category;
    public String createdAt;

    public Note(long id, String title, String content, String category, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
    }
}