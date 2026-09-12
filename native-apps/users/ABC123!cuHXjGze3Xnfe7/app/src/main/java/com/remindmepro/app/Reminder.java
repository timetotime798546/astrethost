package com.remindmepro.app;

public class Reminder {
    private long id;
    private String title;
    private String description;
    private long timeMillis;
    private boolean active;

    public Reminder(long id, String title, String description, long timeMillis, boolean active) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timeMillis = timeMillis;
        this.active = active;
    }

    public Reminder(String title, String description, long timeMillis, boolean active) {
        this.title = title;
        this.description = description;
        this.timeMillis = timeMillis;
        this.active = active;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimeMillis() { return timeMillis; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}