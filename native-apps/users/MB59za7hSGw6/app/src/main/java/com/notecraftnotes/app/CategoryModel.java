package com.notecraftnotes.app;

import org.json.JSONObject;

public class CategoryModel {
    private String id;
    private String name;
    private String color;

    public CategoryModel(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public static CategoryModel fromRecordJson(JSONObject recordObj) {
        if (recordObj == null) return null;
        String recId = recordObj.optString("id", "");
        JSONObject dataObj = recordObj.optJSONObject("data");
        if (dataObj == null) dataObj = new JSONObject();

        String n = dataObj.optString("name", "General");
        String c = dataObj.optString("color", "#4F46E5");

        return new CategoryModel(recId, n, c);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
}