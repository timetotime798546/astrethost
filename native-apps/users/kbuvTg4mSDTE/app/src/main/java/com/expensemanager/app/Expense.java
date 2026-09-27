package com.expensemanager.app;

import org.json.JSONObject;

public class Expense {
    private String id;
    private String title;
    private double amount;
    private String category;
    private String date;
    private String note;

    public Expense() {}

    public static Expense fromJson(JSONObject obj) {
        Expense exp = new Expense();
        try {
            exp.id = obj.optString("id", "");
            JSONObject data = obj.optJSONObject("data");
            if (data != null) {
                exp.title = data.optString("title", "");
                exp.amount = data.optDouble("amount", 0.0);
                exp.category = data.optString("category", "Others");
                exp.date = data.optString("date", "");
                exp.note = data.optString("note", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exp;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}