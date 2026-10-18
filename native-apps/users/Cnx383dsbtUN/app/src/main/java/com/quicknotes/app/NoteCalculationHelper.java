package com.quicknotes.app;

import java.util.List;

public class NoteCalculationHelper {

    public static int calculateWordCount(String text) {
        if (text == null) {
            return 0;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        String[] words = trimmed.split("\\s+");
        return words.length;
    }

    public static int calculateCharCount(String text) {
        if (text == null) {
            return 0;
        }
        return text.length();
    }

    public static int calculateReadingTimeMinutes(int wordCount) {
        if (wordCount <= 0) {
            return 0;
        }
        int minutes = (int) Math.ceil((double) wordCount / 200.0);
        return minutes == 0 ? 1 : minutes;
    }

    public static int countPinnedNotes(List<NoteModel> notes) {
        if (notes == null) {
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

    public static int calculateTotalWordsInNotes(List<NoteModel> notes) {
        if (notes == null) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < notes.size(); i++) {
            sum += calculateWordCount(notes.get(i).getContent());
        }
        return sum;
    }
}