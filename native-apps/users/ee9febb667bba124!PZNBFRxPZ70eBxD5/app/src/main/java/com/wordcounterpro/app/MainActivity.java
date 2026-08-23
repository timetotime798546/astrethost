package com.wordcounterpro.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText edtInputText;
    private TextView txtWordsCount, txtCharsCount, txtSentencesCount, txtParagraphsCount;
    private TextView txtReadingTime, txtSpeakingTime, txtReadability, txtAvgWordLen, txtCharsNoSpaces;
    private TextView txtDensityResults;
    private EditText edtFind, edtReplace;
    private Button btnPaste, btnCopy, btnClear, btnShare, btnReplaceAll;
    private Button btnCaseUpper, btnCaseLower, btnCaseTitle, btnCaseSentence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        initViews();
        setupListeners();
    }

    private void initViews() {
        edtInputText = (EditText) findViewById(getResources().getIdentifier("edt_input_text", "id", getPackageName()));
        
        txtWordsCount = (TextView) findViewById(getResources().getIdentifier("txt_words_count", "id", getPackageName()));
        txtCharsCount = (TextView) findViewById(getResources().getIdentifier("txt_chars_count", "id", getPackageName()));
        txtSentencesCount = (TextView) findViewById(getResources().getIdentifier("txt_sentences_count", "id", getPackageName()));
        txtParagraphsCount = (TextView) findViewById(getResources().getIdentifier("txt_paragraphs_count", "id", getPackageName()));

        txtReadingTime = (TextView) findViewById(getResources().getIdentifier("txt_reading_time", "id", getPackageName()));
        txtSpeakingTime = (TextView) findViewById(getResources().getIdentifier("txt_speaking_time", "id", getPackageName()));
        txtReadability = (TextView) findViewById(getResources().getIdentifier("txt_readability", "id", getPackageName()));
        txtAvgWordLen = (TextView) findViewById(getResources().getIdentifier("txt_avg_word_len", "id", getPackageName()));
        txtCharsNoSpaces = (TextView) findViewById(getResources().getIdentifier("txt_chars_no_spaces", "id", getPackageName()));
        txtDensityResults = (TextView) findViewById(getResources().getIdentifier("txt_density_results", "id", getPackageName()));

        edtFind = (EditText) findViewById(getResources().getIdentifier("edt_find", "id", getPackageName()));
        edtReplace = (EditText) findViewById(getResources().getIdentifier("edt_replace", "id", getPackageName()));

        btnPaste = (Button) findViewById(getResources().getIdentifier("btn_paste", "id", getPackageName()));
        btnCopy = (Button) findViewById(getResources().getIdentifier("btn_copy", "id", getPackageName()));
        btnClear = (Button) findViewById(getResources().getIdentifier("btn_clear", "id", getPackageName()));
        btnShare = (Button) findViewById(getResources().getIdentifier("btn_share", "id", getPackageName()));
        btnReplaceAll = (Button) findViewById(getResources().getIdentifier("btn_replace_all", "id", getPackageName()));

        btnCaseUpper = (Button) findViewById(getResources().getIdentifier("btn_case_upper", "id", getPackageName()));
        btnCaseLower = (Button) findViewById(getResources().getIdentifier("btn_case_lower", "id", getPackageName()));
        btnCaseTitle = (Button) findViewById(getResources().getIdentifier("btn_case_title", "id", getPackageName()));
        btnCaseSentence = (Button) findViewById(getResources().getIdentifier("btn_case_sentence", "id", getPackageName()));
    }

    private void setupListeners() {
        edtInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                analyzeText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        edtInputText.append(item.getText());
                        Toast.makeText(MainActivity.this, "Text pasted successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                if (!text.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Word Counter Copy", text);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtInputText.setText("");
                Toast.makeText(MainActivity.this, "Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                if (!text.isEmpty()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(shareIntent, "Share analysis text"));
                } else {
                    Toast.makeText(MainActivity.this, "Nothing to share", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReplaceAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String findText = edtFind.getText().toString();
                String replaceText = edtReplace.getText().toString();
                String mainText = edtInputText.getText().toString();
                if (!findText.isEmpty() && !mainText.isEmpty()) {
                    String updated = mainText.replace(findText, replaceText);
                    edtInputText.setText(updated);
                    Toast.makeText(MainActivity.this, "Replacements done!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Case Converters
        btnCaseUpper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                edtInputText.setText(text.toUpperCase());
            }
        });

        btnCaseLower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                edtInputText.setText(text.toLowerCase());
            }
        });

        btnCaseTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                edtInputText.setText(convertToTitleCase(text));
            }
        });

        btnCaseSentence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = edtInputText.getText().toString();
                edtInputText.setText(convertToSentenceCase(text));
            }
        });
    }

    private void analyzeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            resetStats();
            return;
        }

        int charCount = text.length();
        int charNoSpaces = text.replace(" ", "").replace("\n", "").length();

        // Word count
        String[] wordsArray = text.trim().split("\\s+");
        int wordCount = (wordsArray.length == 1 && wordsArray[0].isEmpty()) ? 0 : wordsArray.length;

        // Sentence count
        String[] sentencesArray = text.split("[.!?]+");
        int sentenceCount = 0;
        for (int i = 0; i < sentencesArray.length; i++) {
            if (!sentencesArray[i].trim().isEmpty()) {
                sentenceCount++;
            }
        }
        if (sentenceCount == 0 && wordCount > 0) {
            sentenceCount = 1;
        }

        // Paragraph count
        String[] paragraphsArray = text.split("\n+");
        int paragraphCount = 0;
        for (int i = 0; i < paragraphsArray.length; i++) {
            if (!paragraphsArray[i].trim().isEmpty()) {
                paragraphCount++;
            }
        }
        if (paragraphCount == 0 && wordCount > 0) {
            paragraphCount = 1;
        }

        // Estimated reading time (~225 words per minute)
        double readingTimeMinutes = (double) wordCount / 225.0;
        String readingTimeString;
        if (readingTimeMinutes < 1.0) {
            readingTimeString = (int) Math.round(readingTimeMinutes * 60.0) + " sec";
        } else {
            readingTimeString = String.format("%.1f min", readingTimeMinutes);
        }

        // Estimated speaking time (~130 words per minute)
        double speakingTimeMinutes = (double) wordCount / 130.0;
        String speakingTimeString;
        if (speakingTimeMinutes < 1.0) {
            speakingTimeString = (int) Math.round(speakingTimeMinutes * 60.0) + " sec";
        } else {
            speakingTimeString = String.format("%.1f min", speakingTimeMinutes);
        }

        // Readability analysis: Flesch Reading Ease level
        int syllableCount = calculateTotalSyllables(wordsArray);
        double fleschScore = 100;
        if (wordCount > 0 && sentenceCount > 0) {
            fleschScore = 206.835 - 1.015 * ((double) wordCount / sentenceCount) - 84.6 * ((double) syllableCount / wordCount);
        }
        String readabilityInterpretation = getReadabilityInterpretation(fleschScore);

        // Average Word Length
        double avgWordLength = 0.0;
        if (wordCount > 0) {
            avgWordLength = (double) charNoSpaces / wordCount;
        }

        // Update UI counters
        txtWordsCount.setText(String.valueOf(wordCount));
        txtCharsCount.setText(String.valueOf(charCount));
        txtSentencesCount.setText(String.valueOf(sentenceCount));
        txtParagraphsCount.setText(String.valueOf(paragraphCount));

        txtReadingTime.setText(readingTimeString);
        txtSpeakingTime.setText(speakingTimeString);
        txtReadability.setText(String.format("%.1f (%s)", fleschScore, readabilityInterpretation));
        txtAvgWordLen.setText(String.format("%.1f chars", avgWordLength));
        txtCharsNoSpaces.setText(String.valueOf(charNoSpaces));

        // Keyword density calculation
        calculateKeywordDensity(wordsArray);
    }

    private int calculateTotalSyllables(String[] words) {
        int total = 0;
        for (int i = 0; i < words.length; i++) {
            total += countSyllables(words[i]);
        }
        return total;
    }

    private int countSyllables(String word) {
        word = word.toLowerCase().replaceAll("[^a-z]", "");
        if (word.isEmpty()) return 0;
        if (word.length() <= 3) return 1;
        int count = 0;
        boolean lastWasVowel = false;
        String vowels = "aeiouy";
        for (int i = 0; i < word.length(); i++) {
            boolean isVowel = vowels.indexOf(word.charAt(i)) != -1;
            if (isVowel && !lastWasVowel) {
                count++;
            }
            lastWasVowel = isVowel;
        }
        if (word.endsWith("e")) {
            count--;
        }
        return count <= 0 ? 1 : count;
    }

    private String getReadabilityInterpretation(double score) {
        if (score >= 90) return "Very Easy (5th Grade)";
        if (score >= 80) return "Easy (6th Grade)";
        if (score >= 70) return "Fairly Easy (7th Grade)";
        if (score >= 60) return "Standard (8th-9th Grade)";
        if (score >= 50) return "Fairly Difficult (High School)";
        if (score >= 30) return "Difficult (College)";
        return "Very Confusing (Graduate level)";
    }

    private void calculateKeywordDensity(String[] words) {
        if (words == null || words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
            txtDensityResults.setText("Write some text to analyze keyword frequency.");
            return;
        }

        final HashMap<String, Integer> freqMap = new HashMap<>();
        int validWordCount = 0;

        for (int i = 0; i < words.length; i++) {
            String clean = words[i].toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (clean.length() > 2) { // Only count words longer than 2 characters
                validWordCount++;
                if (freqMap.containsKey(clean)) {
                    freqMap.put(clean, freqMap.get(clean) + 1);
                } else {
                    freqMap.put(clean, 1);
                }
            }
        }

        if (freqMap.isEmpty()) {
            txtDensityResults.setText("No significant keywords found.");
            return;
        }

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(freqMap.entrySet());
        Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        StringBuilder builder = new StringBuilder();
        int size = Math.min(sortedList.size(), 5);
        for (int i = 0; i < size; i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            double percentage = ((double) entry.getValue() / validWordCount) * 100.0;
            builder.append(i + 1)
                    .append(". ")
                    .append(entry.getKey())
                    .append(" : ")
                    .append(entry.getValue())
                    .append(" times (")
                    .append(String.format("%.1f", percentage))
                    .append("%)\n");
        }
        txtDensityResults.setText(builder.toString().trim());
    }

    private String convertToTitleCase(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String convertToSentenceCase(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!') {
                capitalizeNext = true;
                result.append(c);
            } else if (Character.isLetter(c) && capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private void resetStats() {
        txtWordsCount.setText("0");
        txtCharsCount.setText("0");
        txtSentencesCount.setText("0");
        txtParagraphsCount.setText("0");
        txtReadingTime.setText("0 min");
        txtSpeakingTime.setText("0 min");
        txtReadability.setText("100 (Very Easy)");
        txtAvgWordLen.setText("0.0 chars");
        txtCharsNoSpaces.setText("0");
        txtDensityResults.setText("Write some text to analyze keyword frequency.");
    }
}