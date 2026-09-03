package com.expensetrackerpro.app;

public class Transaction {
    private long id;
    private double amount;
    private String type; // "Income" or "Expense"
    private String category;
    private String description;
    private String date; // format: YYYY-MM-DD

    public Transaction(long id, double amount, String type, String category, String description, String date) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public long getId() {
        return id;
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

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }
}