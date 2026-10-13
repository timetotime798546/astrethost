package com.quicknotes.app;

import java.util.List;

public class NoteStatsCalculator {

    /**
     * Calculates the word count of a given string.
     */
    public static int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String trimmed = text.trim();
        String[] words = trimmed.split("\\s+");
        return words.length;
    }

    /**
     * Calculates total character count including non-whitespace characters.
     */
    public static int calculateCharCount(String text) {
        if (text == null) {
            return 0;
        }
        return text.length();
    }

    /**
     * Computes estimated reading time in minutes assuming 200 words per minute average.
     */
    public static int calculateReadingTimeMinutes(int wordCount) {
        if (wordCount <= 0) {
            return 0;
        }
        int minutes = (int) Math.ceil((double) wordCount / 200.0);
        return minutes < 1 ? 1 : minutes;
    }

    /**
     * Aggregates total words across all notes currently saved.
     */
    public static int calculateTotalWords(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            total += calculateWordCount(note.getContent());
        }
        return total;
    }

    /**
     * Computes total pinned notes count.
     */
    public static int calculatePinnedCount(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).isPinned()) {
                count++;
            }
        }
        return count;
    }
}