package com.facebookhome.app;

import java.util.ArrayList;
import java.util.List;

public class PostItem {
    private String id;
    private String authorName;
    private String timestamp;
    private String content;
    private int avatarColor;
    private int likeCount;
    private boolean isLiked;
    private List<CommentItem> comments;

    public PostItem(String id, String authorName, String timestamp, String content, int avatarColor, int initialLikes) {
        this.id = id;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.content = content;
        this.avatarColor = avatarColor;
        this.likeCount = initialLikes;
        this.isLiked = false;
        this.comments = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    public int getAvatarColor() {
        return avatarColor;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public List<CommentItem> getComments() {
        return comments;
    }

    public void toggleLike() {
        if (isLiked) {
            isLiked = false;
            likeCount--;
        } else {
            isLiked = true;
            likeCount++;
        }
    }

    public void addComment(CommentItem comment) {
        comments.add(comment);
    }
}