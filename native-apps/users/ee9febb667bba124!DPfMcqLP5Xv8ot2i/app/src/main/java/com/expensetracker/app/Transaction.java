package com.expensetracker.app;

public class Transaction {
    private int id;
    private String title;
    private double amount;
    private String type; // "INCOME" or "EXPENSE"
    private String category;
    private String date; // "YYYY-MM-DD"

    public Transaction(int id, String title, double amount, String type, String category, String date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }
}