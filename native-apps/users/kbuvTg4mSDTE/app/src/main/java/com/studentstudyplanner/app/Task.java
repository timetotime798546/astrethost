package com.studentstudyplanner.app;

public class Task {
    private long id;
    private long subjectId;
    private String title;
    private String description;
    private long dueDate; // Timestamp
    private int priority; // 0=Low, 1=Medium, 2=High
    private int status; // 0=Pending, 1=Completed

    public Task() {
        // Default constructor
    }

    public Task(long id, long subjectId, String title, String description, long dueDate, int priority, int status) {
        this.id = id;
        this.subjectId = subjectId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(long subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPriorityString() {
        switch (priority) {
            case 0: return "Low";
            case 1: return "Medium";
            case 2: return "High";
            default: return "Unknown";
        }
    }

    public String getStatusString() {
        return status == 1 ? "Completed" : "Pending";
    }
}