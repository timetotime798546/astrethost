package com.studentmanagement.app;

import java.io.Serializable;

public class Student implements Serializable {
    private String id;
    private String name;
    private String phone;
    private String className; // Using className to avoid conflict with Java 'class' keyword
    private int rollNumber;

    public Student(String id, String name, String phone, String className, int rollNumber) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.className = className;
        this.rollNumber = rollNumber;
    }

    // Constructor for creating new students locally (ID will be null)
    public Student(String name, String phone, String className, int rollNumber) {
        this(null, name, phone, className, rollNumber);
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(int rollNumber) {
        this.rollNumber = rollNumber;
    }

    @Override
    public String toString() {
        return name + " (Roll: " + rollNumber + ")";
    }
}