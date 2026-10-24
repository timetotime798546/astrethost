package com.offlinenotes.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotesManager {
    private static final String PREF_NAME = "offline_notes_storage";
    private static final String KEY_NOTES_ARRAY = "notes_json_array";
    private SharedPreferences preferences;

    public NotesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<Note> getAllNotes() {
        List<Note> list = new ArrayList<Note>();
        String jsonStr = preferences.getString(KEY_NOTES_ARRAY, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(Note.fromJson(obj));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Sort: pinned first, then by latest updatedAt descending
        Collections.sort(list, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                if (n1.isPinned() != n2.isPinned()) {
                    return n1.isPinned() ? -1 : 1;
                }
                if (n2.getUpdatedAt() > n1.getUpdatedAt()) {
                    return 1;
                } else if (n2.getUpdatedAt() < n1.getUpdatedAt()) {
                    return -1;
                }
                return 0;
            }
        });

        return list;
    }

    public synchronized void saveNote(Note note) {
        List<Note> list = getAllNotes();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(note.getId())) {
                list.set(i, note);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, note);
        }
        saveAll(list);
    }

    public synchronized void deleteNote(String noteId) {
        List<Note> list = getAllNotes();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(noteId)) {
                list.remove(i);
                break;
            }
        }
        saveAll(list);
    }

    public synchronized Note getNoteById(String noteId) {
        List<Note> list = getAllNotes();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(noteId)) {
                return list.get(i);
            }
        }
        return null;
    }

    private synchronized void saveAll(List<Note> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(list.get(i).toJson());
        }
        preferences.edit().putString(KEY_NOTES_ARRAY, arr.toString()).apply();
    }

    // Offline calculations: total words across all notes
    public int calculateTotalWords(List<Note> notes) {
        int total = 0;
        for (int i = 0; i < notes.size(); i++) {
            total += notes.get(i).calculateWordCount();
        }
        return total;
    }

    // Filter notes by search query and category
    public List<Note> filterNotes(List<Note> notes, String query, String category) {
        List<Note> filtered = new ArrayList<Note>();
        String lowerQuery = (query != null) ? query.toLowerCase().trim() : "";

        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);

            boolean matchesCategory = true;
            if (category != null && !category.equals("All")) {
                matchesCategory = n.getCategory().equalsIgnoreCase(category);
            }

            boolean matchesQuery = true;
            if (!lowerQuery.isEmpty()) {
                String title = n.getTitle().toLowerCase();
                String content = n.getContent().toLowerCase();
                matchesQuery = title.contains(lowerQuery) || content.contains(lowerQuery);
            }

            if (matchesCategory && matchesQuery) {
                filtered.add(n);
            }
        }
        return filtered;
    }
}