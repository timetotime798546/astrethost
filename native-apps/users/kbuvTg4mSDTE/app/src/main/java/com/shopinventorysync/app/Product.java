package com.shopinventorysync.app;

import org.json.JSONObject;

public class Product {
    public String id;
    public String name;
    public String category;
    public String sku;
    public double purchasePrice;
    public double sellingPrice;
    public int quantity;
    public int lowStockThreshold;

    public Product() {}

    public Product(JSONObject recordObj) {
        try {
            this.id = recordObj.getString("id");
            JSONObject data = recordObj.getJSONObject("data");
            this.name = data.optString("name", "Unnamed Item");
            this.category = data.optString("category", "General");
            this.sku = data.optString("sku", "N/A");
            this.purchasePrice = data.optDouble("purchase_price", 0.0);
            this.sellingPrice = data.optDouble("selling_price", 0.0);
            this.quantity = data.optInt("quantity", 0);
            this.lowStockThreshold = data.optInt("low_stock_threshold", 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toDataJson() {
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("category", category);
            data.put("sku", sku);
            data.put("purchase_price", purchasePrice);
            data.put("selling_price", sellingPrice);
            data.put("quantity", quantity);
            data.put("low_stock_threshold", lowStockThreshold);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    // Local calculation Profit Margin: ((Selling - Purchase) / Selling) * 100
    public double getProfitMargin() {
        if (sellingPrice <= 0) return 0.0;
        return ((sellingPrice - purchasePrice) / sellingPrice) * 100.0;
    }

    // Local calculation Valuation: Purchase Price * Current Quantity
    public double getValuation() {
        return purchasePrice * quantity;
    }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
}