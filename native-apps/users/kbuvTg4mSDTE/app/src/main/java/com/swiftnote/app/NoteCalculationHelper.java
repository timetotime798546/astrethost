package com.swiftnote.app;

import java.util.List;

public class NoteCalculationHelper {

    public static int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String trimmed = text.trim();
        String[] words = trimmed.split("\\s+");
        return words.length;
    }

    public static int calculateCharacterCount(String text) {
        if (text == null) {
            return 0;
        }
        return text.length();
    }

    public static int calculateReadingTimeMinutes(int wordCount) {
        if (wordCount <= 0) {
            return 0;
        }
        // Formula: word_count / 200 words per minute
        int mins = wordCount / 200;
        return mins < 1 ? 1 : mins;
    }

    public static int calculateTotalWordsInNotes(List<NoteModel> notes) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < notes.size(); i++) {
            NoteModel note = notes.get(i);
            if (note != null && note.getContent() != null) {
                total += calculateWordCount(note.getContent());
            }
        }
        return total;
    }

    public static int countPinnedNotes(List<NoteModel> notes) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }
        int pinned = 0;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i) != null && notes.get(i).isPinned()) {
                pinned++;
            }
        }
        return pinned;
    }
}