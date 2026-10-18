package com.radiofmlive.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferencesManager {
    private static final String PREF_NAME = "radio_fm_live_prefs";
    private static final String KEY_FAVORITES = "fav_stations";
    private static final String KEY_RECENTS = "recent_stations";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(String stationId) {
        Set<String> set = prefs.getStringSet(KEY_FAVORITES, new HashSet<String>());
        return set.contains(stationId);
    }

    public void toggleFavorite(String stationId) {
        Set<String> set = new HashSet<String>(prefs.getStringSet(KEY_FAVORITES, new HashSet<String>()));
        if (set.contains(stationId)) {
            set.remove(stationId);
        } else {
            set.add(stationId);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    public void addRecent(String stationId) {
        String recentsStr = prefs.getString(KEY_RECENTS, "");
        List<String> list = new ArrayList<String>();
        if (!recentsStr.isEmpty()) {
            String[] parts = recentsStr.split(",");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (!p.isEmpty() && !p.equals(stationId)) {
                    list.add(p);
                }
            }
        }
        list.add(0, stationId);
        // Keep max 15 recents
        while (list.size() > 15) {
            list.remove(list.size() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        prefs.edit().putString(KEY_RECENTS, sb.toString()).apply();
    }

    public List<String> getRecentStationIds() {
        List<String> list = new ArrayList<String>();
        String recentsStr = prefs.getString(KEY_RECENTS, "");
        if (!recentsStr.isEmpty()) {
            String[] parts = recentsStr.split(",");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (!p.isEmpty()) {
                    list.add(p);
                }
            }
        }
        return list;
    }
}