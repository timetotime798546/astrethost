package com.instagramhome.app;

public class StoryModel {
    private String username;
    private String avatarUrl;
    private String storyImageUrl;
    private boolean isMyStory;

    public StoryModel(String username, String avatarUrl, String storyImageUrl, boolean isMyStory) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.storyImageUrl = storyImageUrl;
        this.isMyStory = isMyStory;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getStoryImageUrl() {
        return storyImageUrl;
    }

    public boolean isMyStory() {
        return isMyStory;
    }
}