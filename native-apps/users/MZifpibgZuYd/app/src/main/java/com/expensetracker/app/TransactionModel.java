package com.expensetracker.app;

public class TransactionModel {
    private String id;
    private String title;
    private double amount;
    private String type; // "income" or "expense"
    private String category;
    private String date; // YYYY-MM-DD
    private String note;

    public TransactionModel(String id, String title, double amount, String type, String category, String date, String note) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.note = note;
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

    public String getNote() {
        return note;
    }

    public boolean isIncome() {
        return "income".equalsIgnoreCase(type);
    }
}