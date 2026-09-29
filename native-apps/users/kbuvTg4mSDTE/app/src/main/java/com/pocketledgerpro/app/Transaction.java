package com.pocketledgerpro.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Transaction {
    public String id;
    public String title;
    public double amount;
    public String category;
    public String date;
    public boolean isIncome;

    public Transaction(String id, String title, double amount, String category, String date, boolean isIncome) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isIncome = isIncome;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("amount", amount);
            obj.put("category", category);
            obj.put("date", date);
            obj.put("isIncome", isIncome);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static Transaction fromJsonObject(JSONObject obj) {
        try {
            return new Transaction(
                obj.getString("id"),
                obj.getString("title"),
                obj.getDouble("amount"),
                obj.getString("category"),
                obj.getString("date"),
                obj.getBoolean("isIncome")
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}