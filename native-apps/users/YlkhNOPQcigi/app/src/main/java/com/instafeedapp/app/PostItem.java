package com.instafeedapp.app;

public class PostItem {
    private String id;
    private String username;
    private String caption;
    private String imageUrl;
    private String location;
    private int likes;
    private boolean isLiked;
    private boolean isBookmarked;

    public PostItem(String id, String username, String caption, String imageUrl, String location, int likes) {
        this.id = id;
        this.username = username;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.location = location;
        this.likes = likes;
        this.isLiked = false;
        this.isBookmarked = false;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getCaption() { return caption; }
    public String getImageUrl() { return imageUrl; }
    public String getLocation() { return location; }
    public int getLikes() { return likes; }
    public boolean isLiked() { return isLiked; }
    public boolean isBookmarked() { return isBookmarked; }

    public void setLikes(int likes) { this.likes = likes; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}