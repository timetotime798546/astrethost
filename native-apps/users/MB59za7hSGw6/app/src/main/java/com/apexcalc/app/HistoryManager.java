package com.apexcalc.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "apex_calc_prefs";
    private static final String KEY_HISTORY = "calc_history_data";
    private static final int MAX_HISTORY = 40;

    private SharedPreferences preferences;

    public HistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<HistoryItem> getHistory() {
        List<HistoryItem> items = new ArrayList<HistoryItem>();
        String jsonStr = preferences.getString(KEY_HISTORY, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                HistoryItem item = HistoryItem.fromJSONObject(arr.getJSONObject(i));
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void addRecord(String expression, String result) {
        List<HistoryItem> all = getHistory();
        String id = String.valueOf(System.currentTimeMillis());
        HistoryItem newItem = new HistoryItem(id, expression, result, System.currentTimeMillis());
        all.add(0, newItem);

        if (all.size() > MAX_HISTORY) {
            all = all.subList(0, MAX_HISTORY);
        }

        JSONArray arr = new JSONArray();
        for (int i = 0; i < all.size(); i++) {
            arr.put(all.get(i).toJSONObject());
        }
        preferences.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    public void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply();
    }
}