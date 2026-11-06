package com.facebookhome.app;

public class PostModel {
    private String id;
    private String author;
    private String content;
    private String timestamp;
    private int likes;
    private String imageUrl;
    private boolean isLiked;

    public PostModel(String id, String author, String content, String timestamp, int likes, String imageUrl) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = likes;
        this.imageUrl = imageUrl;
        this.isLiked = false;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }
}