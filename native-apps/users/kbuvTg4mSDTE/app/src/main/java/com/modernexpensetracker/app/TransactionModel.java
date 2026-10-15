package com.modernexpensetracker.app;

import org.json.JSONObject;

public class TransactionModel {
    private String id;
    private String title;
    private double amount;
    private String type; // "INCOME" or "EXPENSE"
    private String category;
    private String date; // YYYY-MM-DD
    private String notes;

    public TransactionModel(String id, String title, double amount, String type, String category, String date, String notes) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.notes = notes;
    }

    public static TransactionModel fromJson(String id, JSONObject data) {
        String title = data.optString("title", "Untitled");
        double amount = data.optDouble("amount", 0.0);
        String type = data.optString("type", "EXPENSE");
        String category = data.optString("category", "Other");
        String date = data.optString("date", "");
        String notes = data.optString("notes", "");
        return new TransactionModel(id, title, amount, type, category, date, notes);
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

    public String getNotes() {
        return notes;
    }

    public boolean isExpense() {
        return "EXPENSE".equalsIgnoreCase(type);
    }
}