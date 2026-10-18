package com.radiofmlive.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {
    private static final String PREF_NAME = "radio_fm_live_prefs";
    private static final String KEY_FAVORITE_STATIONS_JSON = "fav_stations_json_v2";
    private static final String KEY_RECENTS = "recent_stations";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized boolean isFavorite(String stationId) {
        if (stationId == null || stationId.trim().isEmpty()) return false;
        List<RadioStation> favs = getFavoriteStations();
        for (int i = 0; i < favs.size(); i++) {
            if (stationId.equals(favs.get(i).getId())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void toggleFavorite(RadioStation station) {
        if (station == null || station.getId().isEmpty()) return;
        List<RadioStation> favs = getFavoriteStations();
        boolean found = false;
        for (int i = 0; i < favs.size(); i++) {
            if (favs.get(i).getId().equals(station.getId())) {
                favs.remove(i);
                found = true;
                break;
            }
        }
        if (!found) {
            favs.add(0, station);
        }
        saveFavoriteStations(favs);
    }

    public synchronized List<RadioStation> getFavoriteStations() {
        List<RadioStation> list = new ArrayList<RadioStation>();
        String raw = prefs.getString(KEY_FAVORITE_STATIONS_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                RadioStation st = RadioStation.fromJsonObject(obj);
                if (st != null) {
                    list.add(st);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private synchronized void saveFavoriteStations(List<RadioStation> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(list.get(i).toJsonObject());
        }
        prefs.edit().putString(KEY_FAVORITE_STATIONS_JSON, arr.toString()).apply();
    }

    public void addRecent(String stationId) {
        if (stationId == null || stationId.isEmpty()) return;
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
        while (list.size() > 20) {
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