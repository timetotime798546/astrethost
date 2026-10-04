package com.shopinventorysync.app;

import org.json.JSONObject;

public class Sale {
    public String id;
    public String date;
    public String itemsSummary;
    public double totalRevenue;

    public Sale() {}

    public Sale(JSONObject recordObj) {
        try {
            this.id = recordObj.getString("id");
            JSONObject data = recordObj.getJSONObject("data");
            this.date = data.optString("sale_date", "");
            this.itemsSummary = data.optString("items_summary", "");
            this.totalRevenue = data.optDouble("total_revenue", 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toDataJson() {
        JSONObject data = new JSONObject();
        try {
            data.put("sale_date", date);
            data.put("items_summary", itemsSummary);
            data.put("total_revenue", totalRevenue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}