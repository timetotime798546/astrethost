package com.alfariskirana.app;

public class Expense {
    private long id;
    private String category;
    private double amount;
    private String notes;
    private String expenseDate;

    public Expense(long id, String category, double amount, String notes, String expenseDate) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.notes = notes;
        this.expenseDate = expenseDate;
    }

    public long getId() { return id; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getNotes() { return notes; }
    public String getExpenseDate() { return expenseDate; }
}