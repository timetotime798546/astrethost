package com.socialconnect.app;

public class PostModel {
    private String id;
    private String authorName;
    private String authorEmail;
    private String content;
    private String imageUrl;
    private int likes;
    private long timestamp;

    public PostModel(String id, String authorName, String authorEmail, String content, String imageUrl, int likes, long timestamp) {
        this.id = id;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likes = likes;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public long getTimestamp() {
        return timestamp;
    }
}