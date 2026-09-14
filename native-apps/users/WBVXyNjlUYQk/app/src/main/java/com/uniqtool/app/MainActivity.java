package com.uniqtool.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText etInput;
    private EditText etOutput;
    private CheckBox cbAdjacentOnly;
    private CheckBox cbCaseInsensitive;
    private CheckBox cbIgnoreWhitespace;
    private CheckBox cbCountOccurrences;
    private CheckBox cbSortOutput;
    private TextView tvStatistics;

    private Button btnProcess;
    private Button btnSample;
    private Button btnClear;
    private Button btnCopy;
    private Button btnShare;

    // Helper holder class for tracking line value and occurrences
    private static class LineRecord {
        String originalText;
        int count;

        LineRecord(String originalText) {
            this.originalText = originalText;
            this.count = 1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        etInput = (EditText) findViewById(R.id.et_input);
        etOutput = (EditText) findViewById(R.id.et_output);
        cbAdjacentOnly = (CheckBox) findViewById(R.id.cb_adjacent_only);
        cbCaseInsensitive = (CheckBox) findViewById(R.id.cb_case_insensitive);
        cbIgnoreWhitespace = (CheckBox) findViewById(R.id.cb_ignore_whitespace);
        cbCountOccurrences = (CheckBox) findViewById(R.id.cb_count_occurrences);
        cbSortOutput = (CheckBox) findViewById(R.id.cb_sort_output);
        tvStatistics = (TextView) findViewById(R.id.tv_statistics);

        btnProcess = (Button) findViewById(R.id.btn_process);
        btnSample = (Button) findViewById(R.id.btn_sample);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnCopy = (Button) findViewById(R.id.btn_copy);
        btnShare = (Button) findViewById(R.id.btn_share);

        // Click Listeners
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processText();
            }
        });

        btnSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSampleData();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFields();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResult();
            }
        });
    }

    private void loadSampleData() {
        String sample = "Apple\n" +
                "Apple\n" +
                "Banana\n" +
                "apple\n" +
                "Cherry\n" +
                "Cherry\n" +
                "Banana\n" +
                "  Orange  \n" +
                "Orange\n" +
                "Apple";
        etInput.setText(sample);
        Toast.makeText(this, "Sample text loaded", Toast.LENGTH_SHORT).show();
    }

    private void clearFields() {
        etInput.setText("");
        etOutput.setText("");
        tvStatistics.setText("Stats: Input Lines: 0 | Output Lines: 0");
        Toast.makeText(this, "Fields cleared", Toast.LENGTH_SHORT).show();
    }

    private String normalize(String s, boolean trim, boolean caseInsensitive) {
        String result = s;
        if (trim) {
            result = result.trim();
        }
        if (caseInsensitive) {
            result = result.toLowerCase();
        }
        return result;
    }

    private void processText() {
        String rawInput = etInput.getText().toString();
        if (rawInput.isEmpty()) {
            etOutput.setText("");
            tvStatistics.setText("Stats: Input Lines: 0 | Output Lines: 0");
            Toast.makeText(this, "Please insert input text first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split input into lines (supports both Unix and Windows newlines)
        String[] rawLines = rawInput.split("\\r?\\n");
        int totalInputLines = rawLines.length;

        final boolean adjOnly = cbAdjacentOnly.isChecked();
        final boolean caseInsensitive = cbCaseInsensitive.isChecked();
        final boolean trim = cbIgnoreWhitespace.isChecked();
        final boolean countEnabled = cbCountOccurrences.isChecked();
        final boolean sortEnabled = cbSortOutput.isChecked();

        List<LineRecord> finalRecords = new ArrayList<>();

        if (adjOnly) {
            // Processing adjacent duplicates only (Traditional Unix Uniq Behavior)
            for (int i = 0; i < rawLines.length; i++) {
                String currentRaw = rawLines[i];
                String currentNorm = normalize(currentRaw, trim, caseInsensitive);

                if (finalRecords.isEmpty()) {
                    finalRecords.add(new LineRecord(currentRaw));
                } else {
                    LineRecord lastRecord = finalRecords.get(finalRecords.size() - 1);
                    String lastNorm = normalize(lastRecord.originalText, trim, caseInsensitive);

                    if (currentNorm.equals(lastNorm)) {
                        lastRecord.count++;
                    } else {
                        finalRecords.add(new LineRecord(currentRaw));
                    }
                }
            }
        } else {
            // Processing overall duplicates across the entire document
            // Using LinkedHashMap to keep track of the insertion order of unique records
            Map<String, LineRecord> map = new LinkedHashMap<>();

            for (int i = 0; i < rawLines.length; i++) {
                String currentRaw = rawLines[i];
                String currentNorm = normalize(currentRaw, trim, caseInsensitive);

                if (map.containsKey(currentNorm)) {
                    map.get(currentNorm).count++;
                } else {
                    map.put(currentNorm, new LineRecord(currentRaw));
                }
            }
            finalRecords.addAll(map.values());
        }

        // Alphabetical sorting if selected (case-insensitive option respected)
        if (sortEnabled) {
            Collections.sort(finalRecords, new Comparator<LineRecord>() {
                @Override
                public int compare(LineRecord r1, LineRecord r2) {
                    String norm1 = normalize(r1.originalText, trim, caseInsensitive);
                    String norm2 = normalize(r2.originalText, trim, caseInsensitive);
                    return norm1.compareTo(norm2);
                }
            });
        }

        // Construct processed output
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < finalRecords.size(); i++) {
            LineRecord record = finalRecords.get(i);
            String lineValue = record.originalText;
            if (trim) {
                lineValue = lineValue.trim();
            }

            if (countEnabled) {
                sb.append(record.count).append("\t").append(lineValue);
            } else {
                sb.append(lineValue);
            }

            if (i < finalRecords.size() - 1) {
                sb.append("\n");
            }
        }

        etOutput.setText(sb.toString());
        tvStatistics.setText("Stats: Input Lines: " + totalInputLines + " | Output Lines: " + finalRecords.size());
        Toast.makeText(this, "De-duplication completed successfully", Toast.LENGTH_SHORT).show();
    }

    private void copyToClipboard() {
        String outputStr = etOutput.getText().toString();
        if (outputStr.isEmpty()) {
            Toast.makeText(this, "Nothing to copy!", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Uniq Output Text", outputStr);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied output text to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareResult() {
        String outputStr = etOutput.getText().toString();
        if (outputStr.isEmpty()) {
            Toast.makeText(this, "Nothing to share!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, outputStr);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Unique Output");
        startActivity(shareIntent);
    }
}