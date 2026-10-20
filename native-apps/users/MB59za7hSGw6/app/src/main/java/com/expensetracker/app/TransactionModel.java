package com.expensetracker.app;

import org.json.JSONException;
import org.json.JSONObject;

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

    public static TransactionModel fromJson(String id, JSONObject data) {
        if (data == null) return null;
        String title = data.optString("title", "Untitled");
        double amount = data.optDouble("amount", 0.0);
        String type = data.optString("type", "expense");
        String category = data.optString("category", "General");
        String date = data.optString("date", "");
        String note = data.optString("note", "");
        return new TransactionModel(id, title, amount, type, category, date, note);
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("amount", amount);
            obj.put("type", type);
            obj.put("category", category);
            obj.put("date", date);
            obj.put("note", note);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }

    public boolean isExpense() {
        return "expense".equalsIgnoreCase(type);
    }
}