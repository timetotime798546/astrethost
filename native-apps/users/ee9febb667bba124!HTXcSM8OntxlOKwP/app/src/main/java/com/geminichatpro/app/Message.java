package com.geminichatpro.app;

public class Message {
    private String text;
    private boolean isUser;
    private String time;

    public Message(String text, boolean isUser, String time) {
        this.text = text;
        this.isUser = isUser;
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public String getTime() {
        return time;
    }
}