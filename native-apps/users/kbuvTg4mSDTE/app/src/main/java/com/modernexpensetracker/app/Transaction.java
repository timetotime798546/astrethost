package com.modernexpensetracker.app;

import org.json.JSONObject;

public class Transaction {
    private String id;
    private String type;
    private double amount;
    private String category;
    private String date;
    private String note;

    public Transaction(String id, String type, double amount, String category, String date, String note) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public static Transaction fromJson(JSONObject jsonRecord) {
        try {
            String recordId = jsonRecord.getString("id");
            JSONObject data = jsonRecord.getJSONObject("data");
            String type = data.optString("type", "expense");
            double amount = data.optDouble("amount", 0.0);
            String category = data.optString("category", "General");
            String date = data.optString("date", "");
            String note = data.optString("note", "");
            return new Transaction(recordId, type, amount, category, date, note);
        } catch (Exception e) {
            return null;
        }
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}