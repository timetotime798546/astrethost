package com.menucraftqr.app;

import org.json.JSONException;
import org.json.JSONObject;

public class FoodItem {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;
    private boolean isVeg;
    private boolean inStock;

    public FoodItem(String id, String name, String description, double price, String category, boolean isVeg, boolean inStock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isVeg = isVeg;
        this.inStock = inStock;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isVeg() { return isVeg; }
    public void setVeg(boolean veg) { isVeg = veg; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name);
            obj.put("description", description);
            obj.put("price", price);
            obj.put("category", category);
            obj.put("isVeg", isVeg);
            obj.put("inStock", inStock);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static FoodItem fromJsonObject(JSONObject obj) {
        return new FoodItem(
                obj.optString("id", String.valueOf(System.currentTimeMillis())),
                obj.optString("name", "Unnamed Item"),
                obj.optString("description", ""),
                obj.optDouble("price", 0.0),
                obj.optString("category", "Main Course"),
                obj.optBoolean("isVeg", true),
                obj.optBoolean("inStock", true)
        );
    }
}