package com.taskflow.app;

public class Task {
    private int id;
    private String title;
    private String description;
    private String priority;
    private int completed;
    private String dueDate;

    public Task() {}

    public Task(String title, String description, String priority, int completed, String dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.completed = completed;
        this.dueDate = dueDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}