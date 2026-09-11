package com.moderncalculator.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    // Tab Panel Fields
    private View tabCalculator, tabConverter, tabHistory;
    private View layoutCalculator, layoutConverter, layoutHistory;

    // Calculator Views
    private TextView tvFormula, tvResult;
    private String currentExpression = "";
    private boolean isEvaluated = false;

    // Converter Views & State
    private TextView tvActiveCategory, tvFromUnit, tvToUnit, tvConverterInput, tvConverterOutput;
    private ImageView btnSwapUnits;
    
    private String activeCategory = "Length"; // Length, Temperature, Weight
    private String lengthFrom = "Meters (m)";
    private String lengthTo = "Feet (ft)";
    private String tempFrom = "Celsius (°C)";
    private String tempTo = "Fahrenheit (°F)";
    private String weightFrom = "Kilograms (kg)";
    private String weightTo = "Pounds (lbs)";

    private String converterInputText = "1";

    // History Storage Fields
    private LinearLayout llHistoryList;
    private ScrollView scrollHistory;
    private TextView tvEmptyHistory;
    private SharedPreferences sharedPrefs;
    private static final String PREFS_NAME = "ModernCalcPrefs";
    private static final String KEY_HISTORY = "history_items";

    @Override
    protected void Bundle(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize Tab Navigation Views
        tabCalculator = findViewById(R.id.tab_calculator);
        tabConverter = findViewById(R.id.tab_converter);
        tabHistory = findViewById(R.id.tab_history);

        layoutCalculator = findViewById(R.id.layout_calculator);
        layoutConverter = findViewById(R.id.layout_converter);
        layoutHistory = findViewById(R.id.layout_history);

        // Standard Calculator LCD displays
        tvFormula = findViewById(R.id.tv_formula);
        tvResult = findViewById(R.id.tv_result);

        // Setup Panel Switching Events
        setupTabs();

        // Bind Standard Calculator UI Events
        setupCalculatorKeyboard();

        // Bind Unit Converter Components
        setupUnitConverter();

        // Bind History Component Events
        setupHistoryPanel();
    }

    private void setupTabs() {
        tabCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("calculator");
            }
        });

        tabConverter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("converter");
            }
        });

        tabHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("history");
            }
        });
    }

    private void switchTab(String tabTag) {
        tabCalculator.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        // Standard Unselected Design
        tabCalculator.setBackgroundResource(android.R.color.transparent);
        tabCalculator.setPressed(false);
        ((TextView) tabCalculator).setTextColor(getResources().getColor(R.color.text_grey));

        tabConverter.setBackgroundResource(android.R.color.transparent);
        tabConverter.setPressed(false);
        ((TextView) tabConverter).setTextColor(getResources().getColor(R.color.text_grey));

        tabHistory.setBackgroundResource(android.R.color.transparent);
        tabHistory.setPressed(false);
        ((TextView) tabHistory).setTextColor(getResources().getColor(R.color.text_grey));

        layoutCalculator.setVisibility(View.GONE);
        layoutConverter.setVisibility(View.GONE);
        layoutHistory.setVisibility(View.GONE);

        if ("calculator".equals(tabTag)) {
            tabCalculator.setBackgroundResource(R.drawable.tab_item_selected);
            ((TextView) tabCalculator).setTextColor(getResources().getColor(R.color.text_white));
            layoutCalculator.setVisibility(View.VISIBLE);
        } else if ("converter".equals(tabTag)) {
            tabConverter.setBackgroundResource(R.drawable.tab_item_selected);
            ((TextView) tabConverter).setTextColor(getResources().getColor(R.color.text_white));
            layoutConverter.setVisibility(View.VISIBLE);
            runConversion();
        } else if ("history".equals(tabTag)) {
            tabHistory.setBackgroundResource(R.drawable.tab_item_selected);
            ((TextView) tabHistory).setTextColor(getResources().getColor(R.color.text_white));
            layoutHistory.setVisibility(View.VISIBLE);
            loadHistoryList();
        }
    }

    private void setupCalculatorKeyboard() {
        // Digital keys bindings
        int[] numIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dot
        };

        for (int id : numIds) {
            final Button b = findViewById(id);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    appendDigit(b.getText().toString());
                }
            });
        }

        // Action Keys
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                clearAll();
            }
        });

        findViewById(R.id.btn_bracket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                insertParentheses();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                appendOperator("%");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                appendOperator("÷");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                appendOperator("×");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                appendOperator("−");
            }
        });

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                appendOperator("+");
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                toggleSign();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                executeCalculation();
            }
        });

        // Long press clear clears formula
        findViewById(R.id.btn_clear).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                clearAll();
                return true;
            }
        });
    }

    private void appendDigit(String value) {
        if (isEvaluated) {
            currentExpression = "";
            isEvaluated = false;
        }

        if ("0".equals(currentExpression)) {
            if (!".".equals(value)) {
                currentExpression = value;
            } else {
                currentExpression += value;
            }
        } else {
            currentExpression += value;
        }
        updateDisplay();
    }

    private void appendOperator(String operator) {
        if (isEvaluated) {
            currentExpression = tvResult.getText().toString();
            isEvaluated = false;
        }

        if (currentExpression.isEmpty()) {
            if ("−".equals(operator)) {
                currentExpression = "−";
            }
            updateDisplay();
            return;
        }

        // Check if last token is an operator to swap it
        String trimmed = currentExpression.trim();
        if (trimmed.endsWith("+") || trimmed.endsWith("−") || trimmed.endsWith("×") || trimmed.endsWith("÷")) {
            currentExpression = trimmed.substring(0, trimmed.length() - 1) + " " + operator + " ";
        } else {
            currentExpression += " " + operator + " ";
        }
        updateDisplay();
    }

    private void insertParentheses() {
        if (isEvaluated) {
            currentExpression = "";
            isEvaluated = false;
        }

        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < currentExpression.length(); i++) {
            char c = currentExpression.charAt(i);
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }

        if (openCount == closeCount) {
            currentExpression += "(";
        } else {
            String trimmed = currentExpression.trim();
            if (trimmed.isEmpty()) {
                currentExpression += "(";
                updateDisplay();
                return;
            }
            char lastChar = trimmed.charAt(trimmed.length() - 1);
            if (lastChar == '+' || lastChar == '−' || lastChar == '×' || lastChar == '÷' || lastChar == '(') {
                currentExpression += "(";
            } else {
                currentExpression += ")";
            }
        }
        updateDisplay();
    }

    private void toggleSign() {
        if (isEvaluated) {
            currentExpression = tvResult.getText().toString();
            isEvaluated = false;
        }

        if (currentExpression.isEmpty()) {
            currentExpression = "−";
            updateDisplay();
            return;
        }

        // Simple search back for sign toggling
        String exp = currentExpression.trim();
        int i = exp.length() - 1;
        while (i >= 0 && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.')) {
            i--;
        }

        if (i >= 0 && (exp.charAt(i) == '−' || exp.charAt(i) == '-')) {
            // Remove negative sign
            currentExpression = exp.substring(0, i) + exp.substring(i + 1);
        } else {
            // Add negative sign
            currentExpression = exp.substring(0, i + 1) + "−" + exp.substring(i + 1);
        }
        updateDisplay();
    }

    private void clearAll() {
        currentExpression = "";
        tvFormula.setText("");
        tvResult.setText("0");
        isEvaluated = false;
    }

    private void updateDisplay() {
        if (currentExpression.isEmpty()) {
            tvResult.setText("0");
        } else {
            tvResult.setText(currentExpression);
        }
    }

    private void executeCalculation() {
        if (currentExpression.trim().isEmpty()) return;

        try {
            String sanitized = currentExpression.replace("×", "*")
                                                 .replace("÷", "/")
                                                 .replace("−", "-")
                                                 .replace(" ", "");

            double output = evaluateExpression(sanitized);

            // Format Double nicely
            DecimalFormat df = new DecimalFormat("#.########");
            String resultStr = df.format(output);

            tvFormula.setText(currentExpression + " =");
            tvResult.setText(resultStr);

            // Persist into calculations log history list
            saveHistory(currentExpression + " = " + resultStr);

            isEvaluated = true;
        } catch (ArithmeticException e) {
            tvResult.setText("Cannot divide by zero");
            isEvaluated = true;
        } catch (Exception e) {
            tvResult.setText("Invalid format");
            isEvaluated = true;
        }
    }

    // High performance recursive-descent math formula parser 
    private double evaluateExpression(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Zero division");
                        x /= divisor;
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                while (eat('%')) {
                    x = x * 0.01;
                }

                return x;
            }
        }.parse();
    }

    // Unit Converter Section Implementation
    private void setupUnitConverter() {
        tvActiveCategory = findViewById(R.id.tv_active_category);
        tvFromUnit = findViewById(R.id.tv_from_unit);
        tvToUnit = findViewById(R.id.tv_to_unit);
        tvConverterInput = findViewById(R.id.tv_converter_input);
        tvConverterOutput = findViewById(R.id.tv_converter_output);
        btnSwapUnits = findViewById(R.id.btn_swap_units);

        // Click actions for selecting unit parameters
        findViewById(R.id.btn_select_category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoryDialog();
            }
        });

        findViewById(R.id.btn_select_from_unit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUnitSelectionDialog(true);
            }
        });

        findViewById(R.id.btn_select_to_unit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUnitSelectionDialog(false);
            }
        });

        btnSwapUnits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                swapUnits();
            }
        });

        // Initialize Converter Keyboard Buttons
        int[] convNumIds = {
            R.id.conv_btn_0, R.id.conv_btn_1, R.id.conv_btn_2, R.id.conv_btn_3,
            R.id.conv_btn_4, R.id.conv_btn_5, R.id.conv_btn_6, R.id.conv_btn_7,
            R.id.conv_btn_8, R.id.conv_btn_9, R.id.conv_btn_dot
        };

        for (int id : convNumIds) {
            final Button b = findViewById(id);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    appendConverterDigit(b.getText().toString());
                }
            });
        }

        findViewById(R.id.conv_btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                converterInputText = "0";
                runConversion();
            }
        });

        findViewById(R.id.conv_btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (converterInputText.length() > 1) {
                    converterInputText = converterInputText.substring(0, converterInputText.length() - 1);
                } else {
                    converterInputText = "0";
                }
                runConversion();
            }
        });

        findViewById(R.id.conv_btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (converterInputText.startsWith("-")) {
                    converterInputText = converterInputText.substring(1);
                } else {
                    if (!"0".equals(converterInputText)) {
                        converterInputText = "-" + converterInputText;
                    }
                }
                runConversion();
            }
        });
    }

    private void appendConverterDigit(String value) {
        if ("0".equals(converterInputText)) {
            if (!".".equals(value)) {
                converterInputText = value;
            } else {
                converterInputText += value;
            }
        } else {
            converterInputText += value;
        }
        runConversion();
    }

    private void swapUnits() {
        if ("Length".equals(activeCategory)) {
            String tmp = lengthFrom;
            lengthFrom = lengthTo;
            lengthTo = tmp;
        } else if ("Temperature".equals(activeCategory)) {
            String tmp = tempFrom;
            tempFrom = tempTo;
            tempTo = tmp;
        } else if ("Weight".equals(activeCategory)) {
            String tmp = weightFrom;
            weightFrom = weightTo;
            weightTo = tmp;
        }
        updateConverterSpinnerLabels();
        runConversion();
    }

    private void showCategoryDialog() {
        final String[] categories = {"Length", "Temperature", "Weight"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("Select Conversion Category");
        builder.setItems(categories, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activeCategory = categories[which];
                tvActiveCategory.setText(activeCategory + " Converter");
                updateConverterSpinnerLabels();
                runConversion();
            }
        });
        builder.create().show();
    }

    private void showUnitSelectionDialog(final boolean isFromUnit) {
        final String[] units;
        if ("Length".equals(activeCategory)) {
            units = new String[]{"Meters (m)", "Kilometers (km)", "Miles (mi)", "Feet (ft)", "Inches (in)"};
        } else if ("Temperature".equals(activeCategory)) {
            units = new String[]{"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};
        } else {
            units = new String[]{"Kilograms (kg)", "Grams (g)", "Pounds (lbs)", "Ounces (oz)"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(isFromUnit ? "Convert From" : "Convert To");
        builder.setItems(units, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ("Length".equals(activeCategory)) {
                    if (isFromUnit) lengthFrom = units[which];
                    else lengthTo = units[which];
                } else if ("Temperature".equals(activeCategory)) {
                    if (isFromUnit) tempFrom = units[which];
                    else tempTo = units[which];
                } else {
                    if (isFromUnit) weightFrom = units[which];
                    else weightTo = units[which];
                }
                updateConverterSpinnerLabels();
                runConversion();
            }
        });
        builder.create().show();
    }

    private void updateConverterSpinnerLabels() {
        if ("Length".equals(activeCategory)) {
            tvFromUnit.setText(lengthFrom);
            tvToUnit.setText(lengthTo);
        } else if ("Temperature".equals(activeCategory)) {
            tvFromUnit.setText(tempFrom);
            tvToUnit.setText(tempTo);
        } else {
            tvFromUnit.setText(weightFrom);
            tvToUnit.setText(weightTo);
        }
    }

    private void runConversion() {
        tvConverterInput.setText(converterInputText);

        double val = 0;
        try {
            val = Double.parseDouble(converterInputText);
        } catch (NumberFormatException e) {
            tvConverterOutput.setText("0");
            return;
        }

        double finalOutput = 0;
        if ("Length".equals(activeCategory)) {
            finalOutput = convertLength(val, lengthFrom, lengthTo);
        } else if ("Temperature".equals(activeCategory)) {
            finalOutput = convertTemperature(val, tempFrom, tempTo);
        } else if ("Weight".equals(activeCategory)) {
            finalOutput = convertWeight(val, weightFrom, weightTo);
        }

        DecimalFormat df = new DecimalFormat("#.######");
        tvConverterOutput.setText(df.format(finalOutput));
    }

    private double convertLength(double value, String from, String to) {
        // Core baseline reference in Meters
        double meters = value;
        if ("Kilometers (km)".equals(from)) meters = value * 1000.0;
        else if ("Miles (mi)".equals(from)) meters = value * 1609.344;
        else if ("Feet (ft)".equals(from)) meters = value * 0.3048;
        else if ("Inches (in)".equals(from)) meters = value * 0.0254;

        if ("Meters (m)".equals(to)) return meters;
        if ("Kilometers (km)".equals(to)) return meters / 1000.0;
        if ("Miles (mi)".equals(to)) return meters / 1609.344;
        if ("Feet (ft)".equals(to)) return meters / 0.3048;
        if ("Inches (in)".equals(to)) return meters / 0.0254;

        return meters;
    }

    private double convertTemperature(double value, String from, String to) {
        double celsius = value;
        if ("Fahrenheit (°F)".equals(from)) celsius = (value - 32) * 5.0 / 9.0;
        else if ("Kelvin (K)".equals(from)) celsius = value - 273.15;

        if ("Celsius (°C)".equals(to)) return celsius;
        if ("Fahrenheit (°F)".equals(to)) return (celsius * 9.0 / 5.0) + 32;
        if ("Kelvin (K)".equals(to)) return celsius + 273.15;

        return celsius;
    }

    private double convertWeight(double value, String from, String to) {
        // Core baseline reference in Grams
        double grams = value;
        if ("Kilograms (kg)".equals(from)) grams = value * 1000.0;
        else if ("Pounds (lbs)".equals(from)) grams = value * 453.59237;
        else if ("Ounces (oz)".equals(from)) grams = value * 28.34952;

        if ("Grams (g)".equals(to)) return grams;
        if ("Kilograms (kg)".equals(to)) return grams / 1000.0;
        if ("Pounds (lbs)".equals(to)) return grams / 453.59237;
        if ("Ounces (oz)".equals(to)) return grams / 28.34952;

        return grams;
    }

    // Calculation History Management
    private void setupHistoryPanel() {
        llHistoryList = findViewById(R.id.ll_history_list);
        scrollHistory = findViewById(R.id.scroll_history);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);

        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                clearHistoryList();
            }
        });
    }

    private void saveHistory(String formulaAndResult) {
        Set<String> set = sharedPrefs.getStringSet(KEY_HISTORY, null);
        List<String> list;
        if (set == null) {
            list = new ArrayList<String>();
        } else {
            list = new ArrayList<String>(set);
        }
        
        // Prefix Unix Timestamp for ordering
        list.add(System.currentTimeMillis() + "||" + formulaAndResult);

        // Limit size to last 50 entries
        if (list.size() > 50) {
            list.remove(0);
        }

        sharedPrefs.edit().putStringSet(KEY_HISTORY, new HashSet<String>(list)).apply();
    }

    private void loadHistoryList() {
        llHistoryList.removeAllViews();
        Set<String> set = sharedPrefs.getStringSet(KEY_HISTORY, null);

        if (set == null || set.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            scrollHistory.setVisibility(View.GONE);
            return;
        }

        tvEmptyHistory.setVisibility(View.GONE);
        scrollHistory.setVisibility(View.VISIBLE);

        List<String> list = new ArrayList<String>(set);
        // Sort descending by timestamp
        Collections.sort(list, Collections.reverseOrder());

        for (final String item : list) {
            String displayData = item;
            if (item.contains("||")) {
                displayData = item.split("\\|\\|")[1];
            }

            final String entryString = displayData;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);
            card.setClickable(true);
            card.setFocusable(true);

            TextView tv = new TextView(this);
            tv.setText(entryString);
            tv.setTextColor(getResources().getColor(R.color.text_white));
            tv.setTextSize(16sp);
            card.addView(tv);

            // Tap history item to restore to calculator
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (entryString.contains("=")) {
                        String[] parts = entryString.split("=");
                        currentExpression = parts[0].trim();
                        tvFormula.setText("");
                        tvResult.setText(parts[1].trim());
                        isEvaluated = true;
                    } else {
                        currentExpression = entryString;
                        tvResult.setText(currentExpression);
                        isEvaluated = false;
                    }
                    switchTab("calculator");
                }
            });

            llHistoryList.addView(card);
        }
    }

    private void clearHistoryList() {
        sharedPrefs.edit().remove(KEY_HISTORY).apply();
        loadHistoryList();
    }
}