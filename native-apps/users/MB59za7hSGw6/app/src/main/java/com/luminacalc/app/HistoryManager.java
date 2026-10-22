package com.luminacalc.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "lumina_calc_history";
    private static final String KEY_ITEMS = "history_items";
    private static final int MAX_ENTRIES = 50;
    private SharedPreferences preferences;

    public HistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<CalculationItem> getHistory() {
        List<CalculationItem> list = new ArrayList<CalculationItem>();
        String json = preferences.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                CalculationItem item = CalculationItem.fromJSONObject(array.getJSONObject(i));
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addEntry(String expression, String result) {
        List<CalculationItem> current = getHistory();
        current.add(0, new CalculationItem(expression, result, System.currentTimeMillis()));

        // Keep within reasonable size
        if (current.size() > MAX_ENTRIES) {
            current = current.subList(0, MAX_ENTRIES);
        }

        JSONArray array = new JSONArray();
        for (int i = 0; i < current.size(); i++) {
            array.put(current.get(i).toJSONObject());
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public void clearHistory() {
        preferences.edit().remove(KEY_ITEMS).apply();
    }
}