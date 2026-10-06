package com.notesphere.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class NoteStorage {
    private static final String PREF_NAME = "notesphere_pref";
    private static final String KEY_NOTES = "notes_list";

    public static List<Note> loadNotes(Context context) {
        List<Note> notes = new ArrayList<>();
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = pref.getString(KEY_NOTES, null);
        if (jsonStr != null) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    notes.add(Note.fromJSONObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // Default interactive introductory guide notes for onboarding the user
            notes.add(new Note("1", "Welcome to NoteSphere", "This is your clean and lightweight personal notes workspace. You can filter by Category at the top or type to filter notes instantly.", "General", System.currentTimeMillis()));
            notes.add(new Note("2", "Groceries Checklist", "Remember to grab:\n- Raw honey\n- Fresh organic strawberries\n- Sourdough wheat bread\n- Organic dark roasted coffee grains.", "Personal", System.currentTimeMillis() - 60000));
            notes.add(new Note("3", "Project Notes", "Plan the architectural diagram layout and optimize local data storage structure.", "Work", System.currentTimeMillis() - 120000));
            notes.add(new Note("4", "Mobile App Idea", "A fully self-contained note keeping dashboard offering categories and lightning quick instant search functionality.", "Ideas", System.currentTimeMillis() - 180000));
            saveNotes(context, notes);
        }
        return notes;
    }

    public static void saveNotes(Context context, List<Note> notes) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (int i = 0; i < notes.size(); i++) {
            try {
                array.put(notes.get(i).toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        pref.edit().putString(KEY_NOTES, array.toString()).apply();
    }
}