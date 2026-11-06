package com.instagramhome.app;

public class CommentModel {
    private String username;
    private String text;
    private String timeAgo;

    public CommentModel(String username, String text, String timeAgo) {
        this.username = username;
        this.text = text;
        this.timeAgo = timeAgo;
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }

    public String getTimeAgo() {
        return timeAgo;
    }
}