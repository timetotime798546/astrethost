package com.studentstudyplanner.app;

public class Reminder {
    private long id;
    private long taskId;
    private String message;
    private long reminderTime; // Timestamp
    private int isActive; // 0=false, 1=true

    public Reminder() {
        // Default constructor
    }

    public Reminder(long id, long taskId, String message, long reminderTime, int isActive) {
        this.id = id;
        this.taskId = taskId;
        this.message = message;
        this.reminderTime = reminderTime;
        this.isActive = isActive;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
}