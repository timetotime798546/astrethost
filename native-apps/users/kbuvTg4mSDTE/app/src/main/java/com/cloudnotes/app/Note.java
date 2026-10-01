package com.cloudnotes.app;

import java.io.Serializable;

public class Note implements Serializable {
    public String id;
    public String title;
    public String content;
    public String category;

    public Note(String id, String title, String content, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
    }
}