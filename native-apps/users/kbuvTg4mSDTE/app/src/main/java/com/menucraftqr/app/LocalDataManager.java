package com.menucraftqr.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class LocalDataManager {
    private static final String PREF_NAME = "menucraft_qr_prefs";
    private static final String KEY_FOOD_ITEMS = "food_items_json";
    private static final String KEY_ORDERS = "table_orders_json";
    private static final String KEY_TABLE_STATUS = "table_status_prefix_";

    private final SharedPreferences prefs;

    public LocalDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        seedDefaultItemsIfEmpty();
    }

    private void seedDefaultItemsIfEmpty() {
        if (!prefs.contains(KEY_FOOD_ITEMS)) {
            List<FoodItem> initialMenu = new ArrayList<FoodItem>();
            initialMenu.add(new FoodItem("1", "Crispy Paneer Tikka", "Charcoal roasted spiced cottage cheese skewers with mint chutney", 260.0, "Starters", true, true));
            initialMenu.add(new FoodItem("2", "Fiery Chicken Wings", "Glazed in smoky tandoori barbecue sauce", 320.0, "Starters", false, true));
            initialMenu.add(new FoodItem("3", "Royal Butter Chicken", "Tender chicken simmered in creamy makhani cashew gravy", 390.0, "Main Course", false, true));
            initialMenu.add(new FoodItem("4", "Dal Bukhara Gourmet", "Slow-cooked black lentils with butter and aromatic herbs", 280.0, "Main Course", true, true));
            initialMenu.add(new FoodItem("5", "Cold Brew Spice Tonic", "Espresso with hints of cinnamon, citrus and tonic", 180.0, "Beverages", true, true));
            initialMenu.add(new FoodItem("6", "Warm Saffron Jalebi", "Crispy spiral dessert served with rich condensed rabri", 190.0, "Desserts", true, true));
            saveFoodItems(initialMenu);
        }
    }

    public List<FoodItem> getFoodItems() {
        List<FoodItem> list = new ArrayList<FoodItem>();
        String json = prefs.getString(KEY_FOOD_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(FoodItem.fromJsonObject(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveFoodItems(List<FoodItem> items) {
        JSONArray arr = new JSONArray();
        for (FoodItem item : items) {
            arr.put(item.toJsonObject());
        }
        prefs.edit().putString(KEY_FOOD_ITEMS, arr.toString()).apply();
    }

    public List<TableOrder> getOrders() {
        List<TableOrder> list = new ArrayList<TableOrder>();
        String json = prefs.getString(KEY_ORDERS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(TableOrder.fromJsonObject(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveOrders(List<TableOrder> orders) {
        JSONArray arr = new JSONArray();
        for (TableOrder order : orders) {
            arr.put(order.toJsonObject());
        }
        prefs.edit().putString(KEY_ORDERS, arr.toString()).apply();
    }

    public String getTableStatus(int tableNo) {
        return prefs.getString(KEY_TABLE_STATUS + tableNo, "Available");
    }

    public void setTableStatus(int tableNo, String status) {
        prefs.edit().putString(KEY_TABLE_STATUS + tableNo, status).apply();
    }
}