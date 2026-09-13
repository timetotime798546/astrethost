package com.teacherassistant.app;

public class Models {

    public static class Student {
        public long id;
        public String name;
        public double avgGrade;
        public int attendancePercentage;

        public Student(long id, String name) {
            this.id = id;
            this.name = name;
            this.avgGrade = -1;
            this.attendancePercentage = 100;
        }
    }

    public static class Assignment {
        public long id;
        public String title;
        public double maxPoints;

        public Assignment(long id, String title, double maxPoints) {
            this.id = id;
            this.title = title;
            this.maxPoints = maxPoints;
        }

        @Override
        public String toString() {
            return title + " (" + maxPoints + " pts)";
        }
    }

    public static class Lesson {
        public long id;
        public String title;
        public String date;
        public String notes;
        public String status;

        public Lesson(long id, String title, String date, String notes, String status) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.notes = notes;
            this.status = status;
        }
    }
}