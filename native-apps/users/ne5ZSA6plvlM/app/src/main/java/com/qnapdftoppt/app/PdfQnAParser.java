package com.qnapdftoppt.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Q&A Text Parser
 * Auto-detects Questions and Answers from text blocks or raw PDF inputs.
 */
public class PdfQnAParser {

    public static List<QnAItem> parseQnAFromText(String rawText) {
        List<QnAItem> list = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return list;
        }

        // Standard Q&A pattern matching: Q1: ..., Q2., Question 1:, Ans:, Answer:
        String[] blocks = rawText.split("(?i)(?=(Q\\d*[:.]|Question\\s*\\d*[:.]))");

        int idCounter = 1;
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            String question = "";
            String answer = "";

            // Split into Question and Answer parts using "Ans", "Answer", "A:", etc.
            String[] qaSplit = trimmed.split("(?i)(Ans[:.]|Answer[:.]|A[:.])", 2);
            if (qaSplit.length >= 2) {
                question = cleanPrefix(qaSplit[0]);
                answer = qaSplit[1].trim();
            } else {
                // If single text block, treat first line as question and remaining as answer
                String[] lines = trimmed.split("\n", 2);
                question = cleanPrefix(lines[0]);
                answer = lines.length > 1 ? lines[1].trim() : "Detailed explanation provided in study materials.";
            }

            if (!question.isEmpty()) {
                list.add(new QnAItem(idCounter++, question, answer));
            }
        }

        // Fallback demo parser if format wasn't explicitly structured
        if (list.isEmpty()) {
            String[] paragraphs = rawText.split("\n\n");
            for (int i = 0; i < paragraphs.length; i++) {
                String p = paragraphs[i].trim();
                if (p.length() > 5) {
                    list.add(new QnAItem(i + 1, "Question " + (i + 1) + ": " + (p.length() > 60 ? p.substring(0, 60) + "..." : p), p));
                }
            }
        }

        return list;
    }

    private static String cleanPrefix(String text) {
        return text.replaceAll("(?i)^(Q\\d*[:.]|Question\\s*\\d*[:.]|Q:)", "").trim();
    }
}