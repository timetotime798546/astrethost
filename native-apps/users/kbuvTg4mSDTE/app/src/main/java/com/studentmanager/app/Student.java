package com.studentmanager.app;

import java.io.Serializable;

public class Student implements Serializable {
    private String id;
    private String name;
    private String rollNumber;
    private String grade;
    private String email;

    public Student(String id, String name, String rollNumber, String grade, String email) {
        this.id = id;
        this.name = name;
        this.rollNumber = rollNumber;
        this.grade = grade;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Roll: " + rollNumber + ", Grade: " + grade;
    }
}