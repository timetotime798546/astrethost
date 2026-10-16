package com.dukaanprofit.app.models;

public class Expense {
    private long id;
    private String category;
    private double amount;
    private String notes;
    private String date;

    public Expense(long id, String category, double amount, String notes, String date) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.notes = notes;
        this.date = date;
    }

    public long getId() { return id; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getNotes() { return notes; }
    public String getDate() { return date; }
}