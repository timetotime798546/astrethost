package com.gympro.app;

import org.json.JSONObject;

public class PlanModel {

    private String id;
    private String title;
    private int durationMonths;
    private double price;
    private String description;

    public PlanModel(String id, String title, int durationMonths, double price, String description) {
        this.id = id;
        this.title = title;
        this.durationMonths = durationMonths;
        this.price = price;
        this.description = description;
    }

    public static PlanModel fromRecord(JSONObject recordObj) {
        if (recordObj == null) return null;
        String id = recordObj.optString("id", "");
        JSONObject data = recordObj.optJSONObject("data");
        if (data == null) data = new JSONObject();

        String title = data.optString("title", "Standard Plan");
        int durationMonths = data.optInt("duration_months", 1);
        double price = data.optDouble("price", 1000.0);
        String description = data.optString("description", "Gym access");

        return new PlanModel(id, title, durationMonths, price, description);
    }

    public JSONObject toJsonObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("title", title);
            data.put("duration_months", durationMonths);
            data.put("price", price);
            data.put("description", description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getDurationMonths() { return durationMonths; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
}