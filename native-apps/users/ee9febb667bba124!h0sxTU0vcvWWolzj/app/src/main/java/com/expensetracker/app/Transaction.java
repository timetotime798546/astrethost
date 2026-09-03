package com.expensetracker.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Transaction {
    private String id;
    private String title;
    private double amount;
    private String type; // "INCOME" or "EXPENSE"
    private String category;
    private long timestamp;

    public Transaction(String id, String title, double amount, String type, String category, long timestamp) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("amount", amount);
        obj.put("type", type);
        obj.put("category", category);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public static Transaction fromJsonObject(JSONObject obj) throws JSONException {
        return new Transaction(
            obj.getString("id"),
            obj.getString("title"),
            obj.getDouble("amount"),
            obj.getString("type"),
            obj.getString("category"),
            obj.getLong("timestamp")
        );
    }
}