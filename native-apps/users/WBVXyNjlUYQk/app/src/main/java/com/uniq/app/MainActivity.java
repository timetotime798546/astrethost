package com.uniq.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    // Tab Navigation
    private Button btnTabDedup;
    private Button btnTabCompare;
    private Button btnTabGen;

    // View Containers
    private LinearLayout layoutDedup;
    private LinearLayout layoutCompare;
    private LinearLayout layoutGen;

    // Tab 1 Deduplication views
    private EditText etDedupInput;
    private Spinner spinnerDelimiter;
    private CheckBox cbIgnoreCase;
    private CheckBox cbTrimSpaces;
    private CheckBox cbSort;
    private CheckBox cbFreq;
    private Button btnDedupRun;
    private Button btnDedupClear;

    // Tab 2 List Comparison views
    private EditText etCompareA;
    private EditText etCompareB;
    private RadioGroup rgCompareOp;
    private Button btnCompareRun;
    private Button btnCompareClear;

    // Tab 3 Unique Generator views
    private Spinner spinnerGenType;
    private LinearLayout panelGenNumbers;
    private LinearLayout panelGenStrings;
    private EditText etNumMin;
    private EditText etNumMax;
    private EditText etNumCount;
    private EditText etStrLen;
    private EditText etStrCount;
    private CheckBox cbStrUpper;
    private CheckBox cbStrLower;
    private CheckBox cbStrDigits;
    private CheckBox cbStrSpecial;
    private Button btnGenRun;

    // Output panel views
    private EditText etOutput;
    private TextView tvStatusInfo;
    private Button btnActionCopy;
    private Button btnActionShare;

    private int activeTab = 1; // 1 = Dedup, 2 = Compare, 3 = Generator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigationTabs();
        setupSelectors();
        setupDeduplicatorModule();
        setupCompareModule();
        setupGeneratorModule();
        setupOutputActions();
    }

    private void initializeViews() {
        btnTabDedup = (Button) findViewById(R.id.btn_tab_dedup);
        btnTabCompare = (Button) findViewById(R.id.btn_tab_compare);
        btnTabGen = (Button) findViewById(R.id.btn_tab_gen);

        layoutDedup = (LinearLayout) findViewById(R.id.layout_dedup);
        layoutCompare = (LinearLayout) findViewById(R.id.layout_compare);
        layoutGen = (LinearLayout) findViewById(R.id.layout_gen);

        etDedupInput = (EditText) findViewById(R.id.et_dedup_input);
        spinnerDelimiter = (Spinner) findViewById(R.id.spinner_dedup_delimiter);
        cbIgnoreCase = (CheckBox) findViewById(R.id.cb_dedup_case);
        cbTrimSpaces = (CheckBox) findViewById(R.id.cb_dedup_trim);
        cbSort = (CheckBox) findViewById(R.id.cb_dedup_sort);
        cbFreq = (CheckBox) findViewById(R.id.cb_dedup_freq);
        btnDedupRun = (Button) findViewById(R.id.btn_dedup_run);
        btnDedupClear = (Button) findViewById(R.id.btn_dedup_clear);

        etCompareA = (EditText) findViewById(R.id.et_compare_a);
        etCompareB = (EditText) findViewById(R.id.et_compare_b);
        rgCompareOp = (RadioGroup) findViewById(R.id.rg_compare_op);
        btnCompareRun = (Button) findViewById(R.id.btn_compare_run);
        btnCompareClear = (Button) findViewById(R.id.btn_compare_clear);

        spinnerGenType = (Spinner) findViewById(R.id.spinner_gen_type);
        panelGenNumbers = (LinearLayout) findViewById(R.id.panel_gen_numbers);
        panelGenStrings = (LinearLayout) findViewById(R.id.panel_gen_strings);
        etNumMin = (EditText) findViewById(R.id.et_num_min);
        etNumMax = (EditText) findViewById(R.id.et_num_max);
        etNumCount = (EditText) findViewById(R.id.et_num_count);
        etStrLen = (EditText) findViewById(R.id.et_str_len);
        etStrCount = (EditText) findViewById(R.id.et_str_count);
        cbStrUpper = (CheckBox) findViewById(R.id.cb_str_upper);
        cbStrLower = (CheckBox) findViewById(R.id.cb_str_lower);
        cbStrDigits = (CheckBox) findViewById(R.id.cb_str_digits);
        cbStrSpecial = (CheckBox) findViewById(R.id.cb_str_special);
        btnGenRun = (Button) findViewById(R.id.btn_gen_run);

        etOutput = (EditText) findViewById(R.id.et_output);
        tvStatusInfo = (TextView) findViewById(R.id.tv_status_info);
        btnActionCopy = (Button) findViewById(R.id.btn_action_copy);
        btnActionShare = (Button) findViewById(R.id.btn_action_share);
    }

    private void setupNavigationTabs() {
        btnTabDedup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabGen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        activeTab = tabIndex;
        // Reset navigation colors (Native simulation of selection highlighting)
        btnTabDedup.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0x80FFFFFF);
        btnTabCompare.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0x80FFFFFF);
        btnTabGen.setTextColor(tabIndex == 3 ? 0xFFFFFFFF : 0x80FFFFFF);

        btnTabDedup.setTextStyle(tabIndex == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnTabCompare.setTextStyle(tabIndex == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnTabGen.setTextStyle(tabIndex == 3 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // Toggle layouts
        layoutDedup.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutCompare.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutGen.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        tvStatusInfo.setText("Ready");
    }

    private void setupSelectors() {
        // Delimiters
        String[] separators = {"New Line", "Comma (,)", "Semicolon (;)", "Space"};
        ArrayAdapter<String> delimiterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, separators);
        spinnerDelimiter.setAdapter(delimiterAdapter);

        // Generator Types
        String[] genTypes = {"Unique Random Numbers", "Unique Cryptic Passwords"};
        ArrayAdapter<String> genTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genTypes);
        spinnerGenType.setAdapter(genTypeAdapter);

        spinnerGenType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    panelGenNumbers.setVisibility(View.VISIBLE);
                    panelGenStrings.setVisibility(View.GONE);
                } else {
                    panelGenNumbers.setVisibility(View.GONE);
                    panelGenStrings.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupDeduplicatorModule() {
        btnDedupRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDeduplicate();
            }
        });

        btnDedupClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etDedupInput.setText("");
                etOutput.setText("");
                tvStatusInfo.setText("Cleared");
            }
        });
    }

    private void runDeduplicate() {
        String inputStr = etDedupInput.getText().toString();
        if (inputStr.trim().isEmpty()) {
            Toast.makeText(this, "Please enter some items to filter", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine dynamic delimiter configuration
        String selectedDelimiter = "\n";
        int index = spinnerDelimiter.getSelectedItemPosition();
        if (index == 1) {
            selectedDelimiter = ",";
        } else if (index == 2) {
            selectedDelimiter = ";";
        } else if (index == 3) {
            selectedDelimiter = " ";
        }

        // Token splitting
        String[] rawTokens;
        if (selectedDelimiter.equals("\n")) {
            rawTokens = inputStr.split("\n");
        } else {
            rawTokens = inputStr.split(selectedDelimiter);
        }

        boolean trim = cbTrimSpaces.isChecked();
        boolean ignoreCase = cbIgnoreCase.isChecked();
        boolean sort = cbSort.isChecked();
        boolean showFreq = cbFreq.isChecked();

        // Process elements and count occurrences
        LinkedHashMap<String, Integer> frequencyMap = new LinkedHashMap<>();
        int originalCount = 0;

        for (int i = 0; i < rawTokens.length; i++) {
            String token = rawTokens[i];
            if (token == null) continue;
            originalCount++;

            if (trim) {
                token = token.trim();
            }

            // Clean-up empty spaces if checked
            if (trim && token.isEmpty()) {
                continue;
            }

            String matchKey = ignoreCase ? token.toLowerCase() : token;
            if (frequencyMap.containsKey(matchKey)) {
                // Fetch the original case version but update frequency
                frequencyMap.put(matchKey, frequencyMap.get(matchKey) + 1);
            } else {
                frequencyMap.put(matchKey, 1);
            }
        }

        // Extract list maintaining original appearance case where applicable
        List<String> results = new ArrayList<>();
        // Reconstruct items
        final Map<String, String> keyToOriginalCase = new HashMap<>();
        for (int i = 0; i < rawTokens.length; i++) {
            String token = rawTokens[i];
            if (trim) token = token.trim();
            if (trim && token.isEmpty()) continue;

            String matchKey = ignoreCase ? token.toLowerCase() : token;
            if (!keyToOriginalCase.containsKey(matchKey)) {
                keyToOriginalCase.put(matchKey, token);
                results.add(matchKey);
            }
        }

        // Sort items if selected
        if (sort) {
            Collections.sort(results, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    String clean1 = keyToOriginalCase.get(o1);
                    String clean2 = keyToOriginalCase.get(o2);
                    return clean1.compareToIgnoreCase(clean2);
                }
            });
        }

        // Build result output string
        StringBuilder outputBuilder = new StringBuilder();
        String outSeparator = selectedDelimiter.equals("\n") ? "\n" : selectedDelimiter + " ";

        for (int i = 0; i < results.size(); i++) {
            String key = results.get(i);
            String displayVal = keyToOriginalCase.get(key);
            outputBuilder.append(displayVal);
            if (showFreq) {
                outputBuilder.append(" (").append(frequencyMap.get(key)).append("x)");
            }
            if (i < results.size() - 1) {
                outputBuilder.append(outSeparator);
            }
        }

        int finalUniqueSize = results.size();
        int duplicatesRemoved = originalCount - finalUniqueSize;

        etOutput.setText(outputBuilder.toString());
        tvStatusInfo.setText("Unique: " + finalUniqueSize + " | Removed: " + (duplicatesRemoved < 0 ? 0 : duplicatesRemoved));
    }

    private void setupCompareModule() {
        btnCompareRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runCompare();
            }
        });

        btnCompareClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etCompareA.setText("");
                etCompareB.setText("");
                etOutput.setText("");
                tvStatusInfo.setText("Cleared");
            }
        });
    }

    private void runCompare() {
        String rawA = etCompareA.getText().toString();
        String rawB = etCompareB.getText().toString();

        String[] tokensA = rawA.split("\n");
        String[] tokensB = rawB.split("\n");

        // Clean up both lists into sets to prevent internally redundant calculation
        Set<String> setA = new LinkedHashSet<>();
        for (int i = 0; i < tokensA.length; i++) {
            String s = tokensA[i].trim();
            if (!s.isEmpty()) {
                setA.add(s);
            }
        }

        Set<String> setB = new LinkedHashSet<>();
        for (int i = 0; i < tokensB.length; i++) {
            String s = tokensB[i].trim();
            if (!s.isEmpty()) {
                setB.add(s);
            }
        }

        List<String> resultList = new ArrayList<>();
        String statusLabel = "";

        int checkedRadioId = rgCompareOp.getCheckedRadioButtonId();
        if (checkedRadioId == R.id.rb_only_a) {
            // A - B
            for (String item : setA) {
                if (!setB.contains(item)) {
                    resultList.add(item);
                }
            }
            statusLabel = "Unique to A: " + resultList.size();
        } else if (checkedRadioId == R.id.rb_only_b) {
            // B - A
            for (String item : setB) {
                if (!setA.contains(item)) {
                    resultList.add(item);
                }
            }
            statusLabel = "Unique to B: " + resultList.size();
        } else if (checkedRadioId == R.id.rb_intersect) {
            // Intersection
            for (String item : setA) {
                if (setB.contains(item)) {
                    resultList.add(item);
                }
            }
            statusLabel = "Intersection: " + resultList.size();
        } else if (checkedRadioId == R.id.rb_union) {
            // Union
            Set<String> unionSet = new LinkedHashSet<>(setA);
            unionSet.addAll(setB);
            resultList.addAll(unionSet);
            statusLabel = "Union Set: " + resultList.size();
        }

        // Sort comparison output alphabetically
        Collections.sort(resultList);

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < resultList.size(); i++) {
            out.append(resultList.get(i));
            if (i < resultList.size() - 1) {
                out.append("\n");
            }
        }

        etOutput.setText(out.toString());
        tvStatusInfo.setText(statusLabel);
    }

    private void setupGeneratorModule() {
        btnGenRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = spinnerGenType.getSelectedItemPosition();
                if (mode == 0) {
                    runGenerateNumbers();
                } else {
                    runGeneratePasswords();
                }
            }
        });
    }

    private void runGenerateNumbers() {
        try {
            int minVal = Integer.parseInt(etNumMin.getText().toString());
            int maxVal = Integer.parseInt(etNumMax.getText().toString());
            int count = Integer.parseInt(etNumCount.getText().toString());

            if (minVal > maxVal) {
                Toast.makeText(this, "Minimum cannot exceed Maximum!", Toast.LENGTH_SHORT).show();
                return;
            }

            long totalPossible = (long) maxVal - minVal + 1;
            if (count > totalPossible) {
                Toast.makeText(this, "Range contains fewer than " + count + " unique values!", Toast.LENGTH_LONG).show();
                count = (int) totalPossible;
            }

            if (count <= 0) {
                Toast.makeText(this, "Count must be greater than zero!", Toast.LENGTH_SHORT).show();
                return;
            }

            Random rand = new Random();
            Set<Integer> uniqueNums = new LinkedHashSet<>();
            while (uniqueNums.size() < count) {
                int val = rand.nextInt((maxVal - minVal) + 1) + minVal;
                uniqueNums.add(val);
            }

            StringBuilder sb = new StringBuilder();
            List<Integer> list = new ArrayList<>(uniqueNums);
            // Default sort generated numeric set
            Collections.sort(list);

            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }

            etOutput.setText(sb.toString());
            tvStatusInfo.setText("Generated: " + list.size() + " Numbers");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please insert valid numbers inside values inputs.", Toast.LENGTH_SHORT).show();
        }
    }

    private void runGeneratePasswords() {
        try {
            int len = Integer.parseInt(etStrLen.getText().toString());
            int count = Integer.parseInt(etStrCount.getText().toString());

            if (len <= 0 || count <= 0) {
                Toast.makeText(this, "Length and count must be positive!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build characters pool based on selected parameters
            String uppers = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            String lowers = "abcdefghijklmnopqrstuvwxyz";
            String digits = "0123456789";
            String specials = "!@#$%^&*()_+-=[]{}|;:,.<>?";

            StringBuilder pool = new StringBuilder();
            if (cbStrUpper.isChecked()) pool.append(uppers);
            if (cbStrLower.isChecked()) pool.append(lowers);
            if (cbStrDigits.isChecked()) pool.append(digits);
            if (cbStrSpecial.isChecked()) pool.append(specials);

            String charPool = pool.toString();
            if (charPool.isEmpty()) {
                Toast.makeText(this, "Select at least one character set!", Toast.LENGTH_SHORT).show();
                return;
            }

            Random rand = new Random();
            Set<String> uniquePasswords = new LinkedHashSet<>();

            // Avoid infinite loops if matching unique sets is mathematically impossible
            int attempts = 0;
            while (uniquePasswords.size() < count && attempts < 5000) {
                attempts++;
                StringBuilder pass = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    int rIdx = rand.nextInt(charPool.length());
                    pass.append(charPool.charAt(rIdx));
                }
                uniquePasswords.add(pass.toString());
            }

            StringBuilder sb = new StringBuilder();
            List<String> list = new ArrayList<>(uniquePasswords);
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) {
                    sb.append("\n");
                }
            }

            etOutput.setText(sb.toString());
            tvStatusInfo.setText("Generated: " + list.size() + " Unique Strings");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid lengths and numbers.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOutputActions() {
        btnActionCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String output = etOutput.getText().toString();
                if (output.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Nothing to copy!", Toast.LENGTH_SHORT).show();
                    return;
                }

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Uniq Output Data", output);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(MainActivity.this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        btnActionShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String output = etOutput.getText().toString();
                if (output.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Nothing to share!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, output);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Cleaned List via Uniq App");
                startActivity(Intent.createChooser(shareIntent, "Share Results via:"));
            }
        });
    }

    // Helper compatibility function to set button text styles natively
    private static class StyledButton {
        // Dummy wrapper for button manipulation if needed
    }
}

// Extends Button natively to support programmatic clean styling updates
class CustomButtonHelper {
    // Utility for style configurations
}