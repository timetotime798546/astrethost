package com.instagramhome.app;

import java.util.ArrayList;
import java.util.List;

public class PostModel {
    private String postId;
    private String username;
    private String avatarUrl;
    private String location;
    private String imageUrl;
    private String caption;
    private int likesCount;
    private boolean isLiked;
    private boolean isSaved;
    private String timeAgo;
    private List<CommentModel> comments;

    public PostModel(String postId, String username, String avatarUrl, String location, String imageUrl, String caption, int likesCount, String timeAgo) {
        this.postId = postId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.location = location;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.likesCount = likesCount;
        this.isLiked = false;
        this.isSaved = false;
        this.timeAgo = timeAgo;
        this.comments = new ArrayList<CommentModel>();
    }

    public String getPostId() {
        return postId;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public List<CommentModel> getComments() {
        return comments;
    }

    public void addComment(CommentModel comment) {
        this.comments.add(comment);
    }
}