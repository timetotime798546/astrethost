package com.facebookhome.app;

public class CommentItem {
    private String author;
    private String text;
    private String time;

    public CommentItem(String author, String text, String time) {
        this.author = author;
        this.text = text;
        this.time = time;
    }

    public String getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }
}