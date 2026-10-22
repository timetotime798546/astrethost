package com.studymasterplanner.app.models;

import org.json.JSONException;
import org.json.JSONObject;

public class StudyTask {
    private String id;
    private String title;
    private String subjectName;
    private String dueDate;
    private String priority;
    private boolean completed;
    private int estimatedMinutes;
    private String notes;

    public StudyTask(String id, String title, String subjectName, String dueDate, String priority, boolean completed, int estimatedMinutes, String notes) {
        this.id = id;
        this.title = title;
        this.subjectName = subjectName;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = completed;
        this.estimatedMinutes = estimatedMinutes;
        this.notes = notes;
    }

    public static StudyTask fromJson(String id, JSONObject data) {
        String title = data.optString("title", "");
        String subject = data.optString("subject_name", "General");
        String due = data.optString("due_date", "Today");
        String priority = data.optString("priority", "Medium");
        boolean completed = data.optBoolean("completed", false);
        int mins = data.optInt("estimated_minutes", 30);
        String notes = data.optString("notes", "");
        return new StudyTask(id, title, subject, due, priority, completed, mins, notes);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("subject_name", subjectName);
            obj.put("due_date", dueDate);
            obj.put("priority", priority);
            obj.put("completed", completed);
            obj.put("estimated_minutes", estimatedMinutes);
            obj.put("notes", notes);
        } catch (JSONException ignored) {}
        return obj;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubjectName() { return subjectName; }
    public String getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public String getNotes() { return notes; }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}