package com.habitflowtracker.app.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Habit {
    private String id;
    private String title;
    private String description;
    private String timeOfDay; // Morning, Afternoon, Evening, Anytime
    private String category; // Health, Fitness, Productivity, Mind, Personal
    private long createdAt;
    private Set<String> completedDates; // Format: "yyyy-MM-dd"

    public Habit() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.description = "";
        this.timeOfDay = "Morning";
        this.category = "Health";
        this.createdAt = System.currentTimeMillis();
        this.completedDates = new HashSet<String>();
    }

    public Habit(String title, String description, String timeOfDay, String category) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timeOfDay = timeOfDay;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
        this.completedDates = new HashSet<String>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getCompletedDates() {
        return completedDates;
    }

    public void setCompletedDates(Set<String> completedDates) {
        this.completedDates = completedDates;
    }

    public boolean isCompletedOn(String dateStr) {
        return completedDates != null && completedDates.contains(dateStr);
    }

    public void toggleCompletion(String dateStr) {
        if (completedDates == null) {
            completedDates = new HashSet<String>();
        }
        if (completedDates.contains(dateStr)) {
            completedDates.remove(dateStr);
        } else {
            completedDates.add(dateStr);
        }
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("description", description);
            json.put("timeOfDay", timeOfDay);
            json.put("category", category);
            json.put("createdAt", createdAt);
            JSONArray datesArr = new JSONArray();
            if (completedDates != null) {
                for (String d : completedDates) {
                    datesArr.put(d);
                }
            }
            json.put("completedDates", datesArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Habit fromJsonObject(JSONObject json) {
        Habit habit = new Habit();
        if (json == null) return habit;
        try {
            habit.id = json.optString("id", UUID.randomUUID().toString());
            habit.title = json.optString("title", "");
            habit.description = json.optString("description", "");
            habit.timeOfDay = json.optString("timeOfDay", "Morning");
            habit.category = json.optString("category", "Health");
            habit.createdAt = json.optLong("createdAt", System.currentTimeMillis());

            JSONArray arr = json.optJSONArray("completedDates");
            Set<String> set = new HashSet<String>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    set.add(arr.getString(i));
                }
            }
            habit.completedDates = set;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return habit;
    }
}