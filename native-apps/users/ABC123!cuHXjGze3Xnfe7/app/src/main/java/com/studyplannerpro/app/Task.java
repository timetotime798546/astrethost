package com.studyplannerpro.app;

public class Task {
    private int id;
    private int subjectId;
    private String subjectCode;
    private String title;
    private String dueDate;
    private int status; // 0 for Pending, 1 for Completed
    private String priority;
    private int hours;

    public Task(int id, int subjectId, String subjectCode, String title, String dueDate, int status, String priority, int hours) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.title = title;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.hours = hours;
    }

    public int getId() { return id; }
    public int getSubjectId() { return subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public String getTitle() { return title; }
    public String getDueDate() { return dueDate; }
    public int getStatus() { return status; }
    public String getPriority() { return priority; }
    public int getHours() { return hours; }
}