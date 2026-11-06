package com.facebookhome.app;

public class StoryModel {
    private String name;
    private String avatarText;
    private boolean isUser;

    public StoryModel(String name, String avatarText, boolean isUser) {
        this.name = name;
        this.avatarText = avatarText;
        this.isUser = isUser;
    }

    public String getName() {
        return name;
    }

    public String getAvatarText() {
        return avatarText;
    }

    public boolean isUser() {
        return isUser;
    }
}