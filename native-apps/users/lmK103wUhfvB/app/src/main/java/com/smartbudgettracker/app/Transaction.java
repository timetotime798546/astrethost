package com.smartbudgettracker.app;

public class Transaction {
    private String id;
    private String title;
    private double amount;
    private String type; // "income" or "expense"
    private String category;
    private String date;

    public Transaction(String id, String title, double amount, String type, String category, String date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
    }

    public String getId() {
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