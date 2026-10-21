package com.studymasterplanner.app.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Subject {
    private String id;
    private String title;
    private String colorHex;
    private double targetHours;

    public Subject(String id, String title, String colorHex, double targetHours) {
        this.id = id;
        this.title = title;
        this.colorHex = colorHex;
        this.targetHours = targetHours;
    }

    public static Subject fromJson(String id, JSONObject data) {
        String title = data.optString("title", "General");
        String color = data.optString("color_hex", "#3F51B5");
        double hours = data.optDouble("target_hours", 2.0);
        return new Subject(id, title, color, hours);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("color_hex", colorHex);
            obj.put("target_hours", targetHours);
        } catch (JSONException ignored) {}
        return obj;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getColorHex() { return colorHex; }
    public double getTargetHours() { return targetHours; }
}