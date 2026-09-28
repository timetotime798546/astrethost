package com.studentdatasaver.app;

public class Student {
    private String backendId; // This is the 'id' from the backend's record object
    private String name;
    private String studentId; // This is the 'student_id' field in the 'data' object

    public Student(String backendId, String name, String studentId) {
        this.backendId = backendId;
        this.name = name;
        this.studentId = studentId;
    }

    public String getBackendId() {
        return backendId;
    }

    public void setBackendId(String backendId) {
        this.backendId = backendId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return studentId;
    }

    public void setId(String studentId) {
        this.studentId = studentId;
    }
}