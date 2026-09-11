package com.studyplannerpro.app;

public class Subject {
    private int id;
    private String name;
    private String code;

    public Subject(int id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}