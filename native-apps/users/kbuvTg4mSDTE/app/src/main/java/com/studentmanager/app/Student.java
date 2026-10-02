package com.studentmanager.app;

public class Student {
    private String id;
    private String name;
    private String rollNo;
    private String grade;
    private int attendancePresent;
    private int attendanceTotal;
    private int marksObtained;
    private int marksTotal;

    public Student() {}

    public Student(String id, String name, String rollNo, String grade, 
                   int attendancePresent, int attendanceTotal, int marksObtained, int marksTotal) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.grade = grade;
        this.attendancePresent = attendancePresent;
        this.attendanceTotal = attendanceTotal;
        this.marksObtained = marksObtained;
        this.marksTotal = marksTotal;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public int getAttendancePresent() { return attendancePresent; }
    public void setAttendancePresent(int attendancePresent) { this.attendancePresent = attendancePresent; }

    public int getAttendanceTotal() { return attendanceTotal; }
    public void setAttendanceTotal(int attendanceTotal) { this.attendanceTotal = attendanceTotal; }

    public int getMarksObtained() { return marksObtained; }
    public void setMarksObtained(int marksObtained) { this.marksObtained = marksObtained; }

    public int getMarksTotal() { return marksTotal; }
    public void setMarksTotal(int marksTotal) { this.marksTotal = marksTotal; }

    // Required offline deterministic logic
    public double getAttendancePercentage() {
        if (attendanceTotal <= 0) return 0.0;
        return ((double) attendancePresent / attendanceTotal) * 100.0;
    }

    public double getMarksPercentage() {
        if (marksTotal <= 0) return 0.0;
        return ((double) marksObtained / marksTotal) * 100.0;
    }

    public String getCalculatedGrade() {
        double pct = getMarksPercentage();
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        if (pct >= 50) return "D";
        return "F";
    }
}