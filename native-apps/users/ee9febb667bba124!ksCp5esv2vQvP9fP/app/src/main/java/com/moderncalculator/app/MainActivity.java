package com.moderncalculator.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvResult;
    private Button btnSoundToggle;
    private Button btnHistory;

    private String currentInput = "";
    private String previousInput = "";
    private String selectedOperator = "";
    private boolean isOperatorPressed = false;
    private boolean hasResultCalculated = false;

    // Audio and History features variables
    private boolean isSoundEnabled = true;
    private ToneGenerator toneGenerator;
    private ArrayList<String> historyList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind basic display views
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Bind action bar controllers
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnHistory = (Button) findViewById(R.id.btn_history);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        btnSoundToggle.setText(isSoundEnabled ? "🔊" : "🔇");

        // Initialize dynamic Tone synthesis generator
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Load persistent transaction history
        loadHistoryFromPreferences();

        // Set standard onClick listeners sequentially
        int[] actionIds = new int[]{
                R.id.btn_clear, R.id.btn_delete, R.id.btn_percent,
                R.id.btn_div, R.id.btn_mul, R.id.btn_sub, R.id.btn_add,
                R.id.btn_equal, R.id.btn_dot, R.id.btn_plus_minus,
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9, R.id.btn_sound_toggle, R.id.btn_history
        };

        for (int i = 0; i < actionIds.length; i++) {
            findViewById(actionIds[i]).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        // Play tactile feedback sound
        playToneFeedback();

        if (id == R.id.btn_sound_toggle) {
            toggleSoundSetting();
        } else if (id == R.id.btn_history) {
            displayHistoryDialog();
        } else if (id == R.id.btn_0) {
            onNumberClick("0");
        } else if (id == R.id.btn_1) {
            onNumberClick("1");
        } else if (id == R.id.btn_2) {
            onNumberClick("2");
        } else if (id == R.id.btn_3) {
            onNumberClick("3");
        } else if (id == R.id.btn_4) {
            onNumberClick("4");
        } else if (id == R.id.btn_5) {
            onNumberClick("5");
        } else if (id == R.id.btn_6) {
            onNumberClick("6");
        } else if (id == R.id.btn_7) {
            onNumberClick("7");
        } else if (id == R.id.btn_8) {
            onNumberClick("8");
        } else if (id == R.id.btn_9) {
            onNumberClick("9");
        } else if (id == R.id.btn_dot) {
            onDotClick();
        } else if (id == R.id.btn_plus_minus) {
            onPlusMinusClick();
        } else if (id == R.id.btn_clear) {
            onClearClick();
        } else if (id == R.id.btn_delete) {
            onDeleteClick();
        } else if (id == R.id.btn_percent) {
            onPercentClick();
        } else if (id == R.id.btn_add) {
            onOperatorClick("+");
        } else if (id == R.id.btn_sub) {
            onOperatorClick("-");
        } else if (id == R.id.btn_mul) {
            onOperatorClick("×");
        } else if (id == R.id.btn_div) {
            onOperatorClick("÷");
        } else if (id == R.id.btn_equal) {
            onEqualClick();
        }
    }

    // Audio Control Methods
    private void playToneFeedback() {
        if (isSoundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
            } catch (Exception e) {
                // Fail silently if device resources are restricted
            }
        }
    }

    private void toggleSoundSetting() {
        isSoundEnabled = !isSoundEnabled;
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("sound_enabled", isSoundEnabled).apply();
        btnSoundToggle.setText(isSoundEnabled ? "🔊" : "🔇");
    }

    // Calculation Logger Storage Logic
    private void loadHistoryFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        String historyData = prefs.getString("history_log", "");
        historyList.clear();
        if (!historyData.isEmpty()) {
            String[] entries = historyData.split("

");
            for (int i = 0; i < entries.length; i++) {
                if (!entries[i].trim().isEmpty()) {
                    historyList.add(entries[i]);
                }
            }
        }
    }

    private void saveHistoryToPreferences() {
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            sb.append(historyList.get(i));
            if (i < historyList.size() - 1) {
                sb.append("

");
            }
        }
        prefs.edit().putString("history_log", sb.toString()).apply();
    }

    private void logCalculation(String op1, String op, String op2, String result) {
        String entry = op1 + " " + op + " " + op2 + " = " + result;
        historyList.add(0, entry); // Insert latest at topmost position
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1); // Clamp stack
        }
        saveHistoryToPreferences();
    }

    private void displayHistoryDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history, null);
        dialog.setView(dialogView);

        final TextView tvHistoryContent = (TextView) dialogView.findViewById(R.id.tv_history_content);
        Button btnDialogClear = (Button) dialogView.findViewById(R.id.btn_dialog_clear);
        Button btnDialogClose = (Button) dialogView.findViewById(R.id.btn_dialog_close);

        // Refresh modal interface text content
        populateHistoryLabel(tvHistoryContent);

        btnDialogClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playToneFeedback();
                historyList.clear();
                saveHistoryToPreferences();
                tvHistoryContent.setText("No history yet.");
            }
        });

        btnDialogClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playToneFeedback();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void populateHistoryLabel(TextView textView) {
        if (historyList.isEmpty()) {
            textView.setText("No history yet.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < historyList.size(); i++) {
                sb.append(historyList.get(i));
                if (i < historyList.size() - 1) {
                    sb.append("

───────────────────

");
                }
            }
            textView.setText(sb.toString());
        }
    }

    // Existing mathematical engine and view methods
    private void onNumberClick(String number) {
        if (hasResultCalculated) {
            currentInput = "";
            hasResultCalculated = false;
        }
        if (isOperatorPressed) {
            currentInput = "";
            isOperatorPressed = false;
        }

        if (currentInput.equals("0") && number.equals("0")) {
            return;
        }
        if (currentInput.equals("0")) {
            currentInput = number;
        } else {
            currentInput += number;
        }
        updateResultView(currentInput);
    }

    private void onDotClick() {
        if (hasResultCalculated) {
            currentInput = "0";
            hasResultCalculated = false;
        }
        if (isOperatorPressed) {
            currentInput = "0";
            isOperatorPressed = false;
        }
        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0";
            }
            currentInput += ".";
            updateResultView(currentInput);
        }
    }

    private void onPlusMinusClick() {
        if (!currentInput.isEmpty() && !currentInput.equals("0")) {
            if (currentInput.startsWith("-")) {
                currentInput = currentInput.substring(1);
            } else {
                currentInput = "-" + currentInput;
            }
            updateResultView(currentInput);
        }
    }

    private void onClearClick() {
        currentInput = "";
        previousInput = "";
        selectedOperator = "";
        isOperatorPressed = false;
        hasResultCalculated = false;
        tvFormula.setText("");
        tvResult.setText("0");
    }

    private void onDeleteClick() {
        if (hasResultCalculated) {
            tvFormula.setText("");
            return;
        }
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                tvResult.setText("0");
            } else { 
                updateResultView(currentInput);
            }
        }
    }

    private void onPercentClick() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput);
                val = val / 100.0;
                currentInput = formatOutput(val);
                updateResultView(currentInput);
            } catch (Exception e) {
                tvResult.setText("Error");
            }
        }
    }

    private void onOperatorClick(String operator) {
        if (currentInput.isEmpty() && !previousInput.isEmpty()) {
            selectedOperator = operator;
            tvFormula.setText(previousInput + " " + selectedOperator);
            isOperatorPressed = true;
            return;
        }

        if (!currentInput.isEmpty() && !previousInput.isEmpty() && !selectedOperator.isEmpty()) {
            performCalculation();
        }

        if (!currentInput.isEmpty()) {
            previousInput = currentInput;
        }
        selectedOperator = operator;
        tvFormula.setText(previousInput + " " + selectedOperator);
        isOperatorPressed = true;
        hasResultCalculated = false;
    }

    private void onEqualClick() {
        if (previousInput.isEmpty() || currentInput.isEmpty() || selectedOperator.isEmpty()) {
            return;
        }
        tvFormula.setText(previousInput + " " + selectedOperator + " " + currentInput + " =");
        performCalculation();
        selectedOperator = "";
        hasResultCalculated = true;
    }

    private void performCalculation() {
        try {
            double op1 = Double.parseDouble(previousInput);
            double op2 = Double.parseDouble(currentInput);
            String operand1Str = previousInput;
            String operand2Str = currentInput;
            double output = 0.0;
            boolean divisionError = false;

            if (selectedOperator.equals("+")) {
                output = op1 + op2;
            } else if (selectedOperator.equals("-")) {
                output = op1 - op2;
            } else if (selectedOperator.equals("×")) {
                output = op1 * op2;
            } else if (selectedOperator.equals("÷")) {
                if (op2 == 0) {
                    divisionError = true;
                } else {
                    output = op1 / op2;
                }
            }

            if (divisionError) {
                tvResult.setText("Error: Div/0");
                currentInput = "";
                previousInput = "";
                selectedOperator = "";
            } else {
                currentInput = formatOutput(output);
                tvResult.setText(currentInput);
                
                // Save newly completed equations inside persistent log files 
                logCalculation(operand1Str, selectedOperator, operand2Str, currentInput);

                previousInput = currentInput;
            }
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private void updateResultView(String value) {
        if (value.startsWith("-") && value.length() > 1) {
            tvResult.setText(value);
        } else if (value.equals("-")) {
            tvResult.setText("-");
        } else {
            tvResult.setText(value);
        }
    }

    private String formatOutput(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            String formatted = String.format("%.8f", val);
            while (formatted.endsWith("0")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            if (formatted.endsWith(".")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            return formatted;
        }
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }
}