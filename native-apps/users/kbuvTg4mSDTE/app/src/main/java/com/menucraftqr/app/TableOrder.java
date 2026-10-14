package com.menucraftqr.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class TableOrder {
    private String orderId;
    private int tableNumber;
    private Map<String, Integer> itemsWithQuantities; // itemId -> qty
    private double discount;
    private String status; // "Received", "Preparing", "Served", "Paid"
    private long timestamp;

    public TableOrder(String orderId, int tableNumber) {
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.itemsWithQuantities = new HashMap<String, Integer>();
        this.discount = 0.0;
        this.status = "Received";
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public int getTableNumber() { return tableNumber; }
    public Map<String, Integer> getItemsWithQuantities() { return itemsWithQuantities; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }

    public void addItem(String itemId, int qty) {
        if (itemsWithQuantities.containsKey(itemId)) {
            int current = itemsWithQuantities.get(itemId);
            int updated = current + qty;
            if (updated <= 0) {
                itemsWithQuantities.remove(itemId);
            } else {
                itemsWithQuantities.put(itemId, updated);
            }
        } else if (qty > 0) {
            itemsWithQuantities.put(itemId, qty);
        }
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("orderId", orderId);
            obj.put("tableNumber", tableNumber);
            obj.put("discount", discount);
            obj.put("status", status);
            obj.put("timestamp", timestamp);

            JSONArray itemsArr = new JSONArray();
            for (Map.Entry<String, Integer> entry : itemsWithQuantities.entrySet()) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("itemId", entry.getKey());
                itemObj.put("qty", entry.getValue());
                itemsArr.put(itemObj);
            }
            obj.put("items", itemsArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static TableOrder fromJsonObject(JSONObject obj) {
        TableOrder order = new TableOrder(
                obj.optString("orderId", "ORD-" + System.currentTimeMillis()),
                obj.optInt("tableNumber", 1)
        );
        order.setDiscount(obj.optDouble("discount", 0.0));
        order.setStatus(obj.optString("status", "Received"));
        order.timestamp = obj.optLong("timestamp", System.currentTimeMillis());

        JSONArray itemsArr = obj.optJSONArray("items");
        if (itemsArr != null) {
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.optJSONObject(i);
                if (itemObj != null) {
                    order.addItem(itemObj.optString("itemId"), itemObj.optInt("qty", 1));
                }
            }
        }
        return order;
    }
}