package com.habitflowtracker.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.habitflowtracker.app.model.Habit;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class HabitStorage {

    private static final String PREF_NAME = "habitflow_prefs";
    private static final String KEY_HABITS = "key_habits_json";

    private final SharedPreferences preferences;

    public HabitStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initializeSampleHabitsIfEmpty();
    }

    public List<Habit> getAllHabits() {
        List<Habit> list = new ArrayList<Habit>();
        String jsonStr = preferences.getString(KEY_HABITS, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                list.add(Habit.fromJsonObject(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveAllHabits(List<Habit> habits) {
        JSONArray arr = new JSONArray();
        if (habits != null) {
            for (int i = 0; i < habits.size(); i++) {
                arr.put(habits.get(i).toJsonObject());
            }
        }
        preferences.edit().putString(KEY_HABITS, arr.toString()).apply();
    }

    public void addHabit(Habit habit) {
        List<Habit> list = getAllHabits();
        list.add(habit);
        saveAllHabits(list);
    }

    public void updateHabit(Habit habit) {
        List<Habit> list = getAllHabits();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(habit.getId())) {
                list.set(i, habit);
                break;
            }
        }
        saveAllHabits(list);
    }

    public void deleteHabit(String habitId) {
        List<Habit> list = getAllHabits();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(habitId)) {
                list.remove(i);
                break;
            }
        }
        saveAllHabits(list);
    }

    private void initializeSampleHabitsIfEmpty() {
        String existing = preferences.getString(KEY_HABITS, null);
        if (existing == null) {
            List<Habit> defaults = new ArrayList<Habit>();

            Habit h1 = new Habit("Drink 2L Water", "Stay hydrated throughout the day", "Morning", "Health");
            h1.toggleCompletion(HabitCalculator.getPastDateString(1));
            h1.toggleCompletion(HabitCalculator.getPastDateString(2));
            h1.toggleCompletion(HabitCalculator.getTodayString());
            defaults.add(h1);

            Habit h2 = new Habit("Read 20 Pages", "Books on personal growth or fiction", "Evening", "Productivity");
            h2.toggleCompletion(HabitCalculator.getPastDateString(1));
            defaults.add(h2);

            Habit h3 = new Habit("Morning Meditation", "10 minutes of calm mindfulness", "Morning", "Mind");
            defaults.add(h3);

            Habit h4 = new Habit("Daily 30 Min Workout", "Gym, jogging, or home cardio workout", "Afternoon", "Fitness");
            h4.toggleCompletion(HabitCalculator.getPastDateString(1));
            h4.toggleCompletion(HabitCalculator.getPastDateString(2));
            defaults.add(h4);

            saveAllHabits(defaults);
        }
    }
}