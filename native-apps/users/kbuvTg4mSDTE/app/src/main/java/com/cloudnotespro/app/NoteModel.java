package com.cloudnotespro.app;

public class NoteModel {
    private String id;
    private String title;
    private String content;
    private String category;

    public NoteModel(String id, String title, String content, String category) {
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

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }
}