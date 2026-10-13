package com.quicknotes.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NotesStorage {
    private static final String FILE_NAME = "quicknotes_data.json";
    private final Context context;

    public NotesStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized List<Note> loadNotes() {
        List<Note> notes = new ArrayList<Note>();
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String jsonString = sb.toString();
            if (jsonString.trim().length() > 0) {
                JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Note note = Note.fromJsonObject(obj);
                    if (note != null) {
                        notes.add(note);
                    }
                }
            }
        } catch (Exception e) {
            // Initial read or file not created yet; return empty list
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ignored) {}
            }
        }
        return notes;
    }

    public synchronized boolean saveNotes(List<Note> notes) {
        FileOutputStream fos = null;
        try {
            JSONArray array = new JSONArray();
            if (notes != null) {
                for (int i = 0; i < notes.size(); i++) {
                    array.put(notes.get(i).toJsonObject());
                }
            }
            fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(array.toString().getBytes("UTF-8"));
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized boolean saveOrUpdateNote(Note note) {
        List<Note> notes = loadNotes();
        boolean found = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(note.getId())) {
                notes.set(i, note);
                found = true;
                break;
            }
        }
        if (!found) {
            // New notes are added to the beginning
            notes.add(0, note);
        }
        return saveNotes(notes);
    }

    public synchronized boolean deleteNote(String noteId) {
        List<Note> notes = loadNotes();
        boolean removed = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(noteId)) {
                notes.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            return saveNotes(notes);
        }
        return false;
    }
}