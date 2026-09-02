package com.threedcalculator.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "threed_calculator_preferences";
    private static final String KEY_HISTORY = "calc_history_data";
    private static final int MAX_HISTORY = 30;

    public static void saveCalculation(Context context, String expression, String result) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<String> history = getHistory(context);
        
        String entry = expression + " = " + result;
        history.add(0, entry);
        
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append("##SPLIT##");
            }
        }
        
        prefs.edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    public static List<String> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_HISTORY, "");
        List<String> history = new ArrayList<String>();
        if (!raw.isEmpty()) {
            String[] parts = raw.split("##SPLIT##");
            for (String part : parts) {
                history.add(part);
            }
        }
        return history;
    }

    public static void clearHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}