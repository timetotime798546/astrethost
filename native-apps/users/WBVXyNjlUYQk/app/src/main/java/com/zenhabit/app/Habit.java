package com.zenhabit.app;

public class Habit {
    private String id;
    private String name;
    private String category;
    private int targetDays;
    private int completedDays;
    private boolean completedToday;

    public Habit(String id, String name, String category, int targetDays, int completedDays, boolean completedToday) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.targetDays = targetDays;
        this.completedDays = completedDays;
        this.completedToday = completedToday;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getTargetDays() { return targetDays; }
    public int getCompletedDays() { return completedDays; }
    public boolean isCompletedToday() { return completedToday; }

    public void setCompletedToday(boolean completedToday) {
        this.completedToday = completedToday;
        if (completedToday) {
            this.completedDays++;
        } else {
            this.completedDays = Math.max(0, this.completedDays - 1);
        }
    }
}