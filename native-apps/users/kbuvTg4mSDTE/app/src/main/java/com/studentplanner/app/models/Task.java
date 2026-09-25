package com.studentplanner.app.models;

public class Task {
    private int id;
    private int subjectId;
    private String title;
    private String description;
    private long dueDateMillis; // Unix timestamp in milliseconds
    private int isCompleted; // 0 for false, 1 for true

    public Task() {
    }

    public Task(int subjectId, String title, String description, long dueDateMillis, int isCompleted) {
        this.subjectId = subjectId;
        this.title = title;
        this.description = description;
        this.dueDateMillis = dueDateMillis;
        this.isCompleted = isCompleted;
    }

    public Task(int id, int subjectId, String title, String description, long dueDateMillis, int isCompleted) {
        this.id = id;
        this.subjectId = subjectId;
        this.title = title;
        this.description = description;
        this.dueDateMillis = dueDateMillis;
        this.isCompleted = isCompleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
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

    public long getDueDateMillis() {
        return dueDateMillis;
    }

    public void setDueDateMillis(long dueDateMillis) {
        this.dueDateMillis = dueDateMillis;
    }

    public boolean isCompleted() {
        return isCompleted == 1;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed ? 1 : 0;
    }
}