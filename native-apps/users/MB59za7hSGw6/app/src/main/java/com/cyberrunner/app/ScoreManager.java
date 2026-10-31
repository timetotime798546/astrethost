package com.cyberrunner.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScoreManager {
    private static final String PREFS_NAME = "cyber_runner_scores";
    private static final String KEY_SCORES_JSON = "leaderboard_json";
    private static final String KEY_TOP_SCORE = "top_high_score";

    private final SharedPreferences prefs;

    public static class ScoreEntry {
        public final int score;
        public final int coins;
        public final String date;

        public ScoreEntry(int score, int coins, String date) {
            this.score = score;
            this.coins = coins;
            this.date = date;
        }
    }

    public ScoreManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTopScore() {
        return prefs.getInt(KEY_TOP_SCORE, 0);
    }

    public synchronized void recordNewScore(int score, int coins) {
        if (score <= 0) return;

        int currentTop = getTopScore();
        if (score > currentTop) {
            prefs.edit().putInt(KEY_TOP_SCORE, score).apply();
        }

        List<ScoreEntry> list = getLeaderboard();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        list.add(new ScoreEntry(score, coins, currentDate));

        Collections.sort(list, new Comparator<ScoreEntry>() {
            @Override
            public int compare(ScoreEntry a, ScoreEntry b) {
                return Integer.compare(b.score, a.score);
            }
        });

        // Retain top 20 entries
        while (list.size() > 20) {
            list.remove(list.size() - 1);
        }

        saveList(list);
    }

    public synchronized List<ScoreEntry> getLeaderboard() {
        List<ScoreEntry> list = new ArrayList<ScoreEntry>();
        String raw = prefs.getString(KEY_SCORES_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new ScoreEntry(
                        obj.optInt("score", 0),
                        obj.optInt("coins", 0),
                        obj.optString("date", "N/A")
                ));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public synchronized void clearScores() {
        prefs.edit().remove(KEY_SCORES_JSON).putInt(KEY_TOP_SCORE, 0).apply();
    }

    private void saveList(List<ScoreEntry> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            ScoreEntry entry = list.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("score", entry.score);
                obj.put("coins", entry.coins);
                obj.put("date", entry.date);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_SCORES_JSON, arr.toString()).apply();
    }
}