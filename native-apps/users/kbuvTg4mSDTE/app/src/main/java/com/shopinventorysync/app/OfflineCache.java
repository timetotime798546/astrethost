package com.shopinventorysync.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;

public class OfflineCache {

    private static final String PREF_NAME = "ShopSyncOfflineCache";
    private static final String KEY_PRODUCTS = "cached_products";
    private static final String KEY_SALES = "cached_sales";

    public static void cacheProducts(Context context, JSONArray productsArray) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PRODUCTS, productsArray.toString()).apply();
    }

    public static JSONArray getCachedProducts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PRODUCTS, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void cacheSales(Context context, JSONArray salesArray) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SALES, salesArray.toString()).apply();
    }

    public static JSONArray getCachedSales(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SALES, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}