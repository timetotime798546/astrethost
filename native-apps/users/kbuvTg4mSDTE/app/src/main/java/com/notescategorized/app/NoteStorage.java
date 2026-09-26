package com.notescategorized.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteStorage {
    private static final String PREFS_NAME = "NotesCategorizedPrefs";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_CATEGORIES = "categories";

    private SharedPreferences sharedPreferences;

    public NoteStorage(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Note> loadNotes() {
        List<Note> notes = new ArrayList<>();
        String notesJsonString = sharedPreferences.getString(KEY_NOTES, "[]");
        try {
            JSONArray jsonArray = new JSONArray(notesJsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteJson = jsonArray.getJSONObject(i);
                String id = noteJson.getString("id");
                String title = noteJson.getString("title");
                String content = noteJson.getString("content");
                String category = noteJson.optString("category", "Uncategorized"); // Handle older notes without category
                notes.add(new Note(id, title, content, category));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public void saveNotes(List<Note> notes) {
        JSONArray jsonArray = new JSONArray();
        for (Note note : notes) {
            try {
                JSONObject noteJson = new JSONObject();
                noteJson.put("id", note.getId());
                noteJson.put("title", note.getTitle());
                noteJson.put("content", note.getContent());
                noteJson.put("category", note.getCategory());
                jsonArray.put(noteJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPreferences.edit().putString(KEY_NOTES, jsonArray.toString()).apply();
    }

    public Note getNoteById(String noteId) {
        List<Note> notes = loadNotes();
        for (Note note : notes) {
            if (note.getId().equals(noteId)) {
                return note;
            }
        }
        return null;
    }

    public void addOrUpdateNote(Note newNote) {
        List<Note> notes = loadNotes();
        boolean found = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(newNote.getId())) {
                notes.set(i, newNote);
                found = true;
                break;
            }
        }
        if (!found) {
            if (newNote.getId() == null || newNote.getId().isEmpty()) {
                newNote.setId(UUID.randomUUID().toString());
            }
            notes.add(newNote);
        }
        saveNotes(notes);
    }

    public void deleteNote(String noteId) {
        List<Note> notes = loadNotes();
        List<Note> updatedNotes = new ArrayList<>();
        for (Note note : notes) {
            if (!note.getId().equals(noteId)) {
                updatedNotes.add(note);
            }
        }
        saveNotes(updatedNotes);
    }

    public List<String> loadCategories() {
        List<String> categories = new ArrayList<>();
        // FIX: Escaped double quotes within the JSON string literal
        String categoriesJsonString = sharedPreferences.getString(KEY_CATEGORIES, "["All", "Uncategorized"]");
        try {
            JSONArray jsonArray = new JSONArray(categoriesJsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                categories.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Ensure "All" and "Uncategorized" are always present if not already.
        if (!categories.contains("All")) {
            categories.add(0, "All");
        }
        if (!categories.contains("Uncategorized")) {
            if (categories.size() > 0 && "All".equals(categories.get(0))) {
                categories.add(1, "Uncategorized");
            } else {
                categories.add("Uncategorized");
            }
        }
        return categories;
    }

    public void saveCategories(List<String> categories) {
        JSONArray jsonArray = new JSONArray();
        for (String category : categories) {
            if (!"All".equals(category) && !"Uncategorized".equals(category)) { // Don't save default categories
                jsonArray.put(category);
            }
        }
        sharedPreferences.edit().putString(KEY_CATEGORIES, jsonArray.toString()).apply();
    }

    public void addCategory(String category) {
        List<String> categories = loadCategories();
        if (!categories.contains(category) && !"All".equals(category) && !"Uncategorized".equals(category)) {
            categories.add(category);
            saveCategories(categories); // Save only custom categories
        }
    }

    public void deleteCategory(String category) {
        List<String> categories = loadCategories();
        if (categories.contains(category) && !"All".equals(category) && !"Uncategorized".equals(category)) {
            categories.remove(category);
            saveCategories(categories); // Save only custom categories
            // Also update notes that belong to this category to "Uncategorized"
            List<Note> notes = loadNotes();
            for (Note note : notes) {
                if (note.getCategory().equals(category)) {
                    note.setCategory("Uncategorized");
                }
            }
            saveNotes(notes);
        }
    }
}
[2026-09-26 05:48:08] [INFO]