package com.moderncalculator.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;
    private Button btnModeStandard;
    private Button btnModeScientific;
    private Button btnModeTools;
    private LinearLayout panelScientific;
    private View panelTools;
    private LinearLayout layoutCalcContainer;

    private Button btnDegRad;

    // Converter Views
    private Spinner spinnerConverterType;
    private Spinner spinnerUnitFrom;
    private Spinner spinnerUnitTo;
    private EditText etUnitInput;
    private TextView tvUnitOutput;

    // Tip Views
    private EditText etBillAmount, etTipPercent, etSplitPeople;
    private Button btnCalcTip;
    private TextView tvTipResult;

    // BMI Views
    private EditText etBmiWeight, etBmiHeight;
    private Button btnCalcBmi;
    private TextView tvBmiResult;

    private CalculatorEngine engine;
    private StringBuilder currentExpression = new StringBuilder();
    private List<HistoryItem> historyList = new ArrayList<HistoryItem>();
    private DecimalFormat df = new DecimalFormat("#.########");

    private static final int MODE_STANDARD = 0;
    private static final int MODE_SCIENTIFIC = 1;
    private static final int MODE_TOOLS = 2;
    private int currentMode = MODE_STANDARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        engine = new CalculatorEngine();

        initViews();
        setupKeypad();
        setupConvertersAndTools();
    }

    private void initViews() {
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        btnModeStandard = (Button) findViewById(R.id.btn_mode_standard);
        btnModeScientific = (Button) findViewById(R.id.btn_mode_scientific);
        btnModeTools = (Button) findViewById(R.id.btn_mode_tools);

        panelScientific = (LinearLayout) findViewById(R.id.panel_scientific);
        panelTools = findViewById(R.id.panel_tools);
        layoutCalcContainer = (LinearLayout) findViewById(R.id.layout_calc_container);

        btnModeStandard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_STANDARD);
            }
        });

        btnModeScientific.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_SCIENTIFIC);
            }
        });

        btnModeTools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_TOOLS);
            }
        });

        findViewById(R.id.btn_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryDialog();
            }
        });

        tvResult.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyToClipboard(tvResult.getText().toString());
                return true;
            }
        });
    }

    private void setupKeypad() {
        int[] buttonIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_dot, R.id.btn_plus, R.id.btn_minus, R.id.btn_multiply, R.id.btn_divide,
                R.id.btn_percent, R.id.btn_clear, R.id.btn_backspace, R.id.btn_equals, R.id.btn_brackets,
                R.id.btn_sign, R.id.btn_sin, R.id.btn_cos, R.id.btn_tan, R.id.btn_sqrt,
                R.id.btn_pow, R.id.btn_log, R.id.btn_ln, R.id.btn_pi, R.id.btn_e,
                R.id.btn_fact, R.id.btn_square
        };

        for (int id : buttonIds) {
            View b = findViewById(id);
            if (b != null) {
                b.setOnClickListener(this);
            }
        }

        btnDegRad = (Button) findViewById(R.id.btn_deg_rad);
        if (btnDegRad != null) {
            btnDegRad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean nextState = !engine.isDegreeMode();
                    engine.setDegreeMode(nextState);
                    btnDegRad.setText(nextState ? "DEG" : "RAD");
                    evaluateCurrentExpression();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_0) appendText("0");
        else if (id == R.id.btn_1) appendText("1");
        else if (id == R.id.btn_2) appendText("2");
        else if (id == R.id.btn_3) appendText("3");
        else if (id == R.id.btn_4) appendText("4");
        else if (id == R.id.btn_5) appendText("5");
        else if (id == R.id.btn_6) appendText("6");
        else if (id == R.id.btn_7) appendText("7");
        else if (id == R.id.btn_8) appendText("8");
        else if (id == R.id.btn_9) appendText("9");
        else if (id == R.id.btn_dot) appendText(".");
        else if (id == R.id.btn_plus) appendOperator("+");
        else if (id == R.id.btn_minus) appendOperator("-");
        else if (id == R.id.btn_multiply) appendOperator("×");
        else if (id == R.id.btn_divide) appendOperator("÷");
        else if (id == R.id.btn_percent) appendOperator("%");
        else if (id == R.id.btn_pi) appendText("π");
        else if (id == R.id.btn_e) appendText("e");
        else if (id == R.id.btn_sin) appendFunction("sin(");
        else if (id == R.id.btn_cos) appendFunction("cos(");
        else if (id == R.id.btn_tan) appendFunction("tan(");
        else if (id == R.id.btn_sqrt) appendFunction("sqrt(");
        else if (id == R.id.btn_log) appendFunction("log(");
        else if (id == R.id.btn_ln) appendFunction("ln(");
        else if (id == R.id.btn_pow) appendOperator("^");
        else if (id == R.id.btn_square) appendOperator("^2");
        else if (id == R.id.btn_fact) appendOperator("!");
        else if (id == R.id.btn_brackets) handleBrackets();
        else if (id == R.id.btn_sign) handleSignToggle();
        else if (id == R.id.btn_clear) clearAll();
        else if (id == R.id.btn_backspace) backspace();
        else if (id == R.id.btn_equals) performEquals();
    }

    private void appendText(String str) {
        currentExpression.append(str);
        updateDisplay();
        evaluateCurrentExpression();
    }

    private void appendFunction(String funcStr) {
        currentExpression.append(funcStr);
        updateDisplay();
        evaluateCurrentExpression();
    }

    private void appendOperator(String op) {
        if (currentExpression.length() == 0) {
            if (op.equals("-")) {
                currentExpression.append("-");
            }
            updateDisplay();
            return;
        }

        char lastChar = currentExpression.charAt(currentExpression.length() - 1);
        if (isOperator(lastChar)) {
            currentExpression.setCharAt(currentExpression.length() - 1, op.charAt(0));
        } else {
            currentExpression.append(op);
        }
        updateDisplay();
        evaluateCurrentExpression();
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%' || c == '^';
    }

    private void handleBrackets() {
        int openCount = 0;
        int closeCount = 0;
        String expr = currentExpression.toString();
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') openCount++;
            if (expr.charAt(i) == ')') closeCount++;
        }

        if (openCount > closeCount && expr.length() > 0 && !isOperator(expr.charAt(expr.length() - 1)) && expr.charAt(expr.length() - 1) != '(') {
            currentExpression.append(")");
        } else {
            currentExpression.append("(");
        }
        updateDisplay();
        evaluateCurrentExpression();
    }

    private void handleSignToggle() {
        if (currentExpression.length() == 0) return;
        String expr = currentExpression.toString();
        if (expr.startsWith("-")) {
            currentExpression.deleteCharAt(0);
        } else {
            currentExpression.insert(0, "-");
        }
        updateDisplay();
        evaluateCurrentExpression();
    }

    private void clearAll() {
        currentExpression.setLength(0);
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void backspace() {
        if (currentExpression.length() > 0) {
            currentExpression.deleteCharAt(currentExpression.length() - 1);
            updateDisplay();
            evaluateCurrentExpression();
        }
    }

    private void updateDisplay() {
        tvExpression.setText(currentExpression.toString());
    }

    private void evaluateCurrentExpression() {
        if (currentExpression.length() == 0) {
            tvResult.setText("0");
            return;
        }

        try {
            double res = engine.evaluate(currentExpression.toString());
            if (Double.isNaN(res) || Double.isInfinite(res)) {
                tvResult.setText("Error");
            } else {
                tvResult.setText(df.format(res));
            }
        } catch (Exception e) {
            // Keep existing valid result or silence partial expression errors
        }
    }

    private void performEquals() {
        if (currentExpression.length() == 0) return;
        try {
            double res = engine.evaluate(currentExpression.toString());
            if (Double.isNaN(res) || Double.isInfinite(res)) {
                tvResult.setText("Error");
            } else {
                String resultStr = df.format(res);
                tvResult.setText(resultStr);

                // Add to history
                historyList.add(0, new HistoryItem(currentExpression.toString(), resultStr));

                currentExpression.setLength(0);
                currentExpression.append(resultStr);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Expression", Toast.LENGTH_SHORT).show();
            tvResult.setText("Error");
        }
    }

    private void switchMode(int mode) {
        currentMode = mode;
        btnModeStandard.setBackgroundResource(mode == MODE_STANDARD ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        btnModeScientific.setBackgroundResource(mode == MODE_SCIENTIFIC ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        btnModeTools.setBackgroundResource(mode == MODE_TOOLS ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);

        if (mode == MODE_STANDARD) {
            layoutCalcContainer.setVisibility(View.VISIBLE);
            panelScientific.setVisibility(View.GONE);
            panelTools.setVisibility(View.GONE);
        } else if (mode == MODE_SCIENTIFIC) {
            layoutCalcContainer.setVisibility(View.VISIBLE);
            panelScientific.setVisibility(View.VISIBLE);
            panelTools.setVisibility(View.GONE);
        } else if (mode == MODE_TOOLS) {
            layoutCalcContainer.setVisibility(View.GONE);
            panelTools.setVisibility(View.VISIBLE);
        }
    }

    private void setupConvertersAndTools() {
        spinnerConverterType = (Spinner) findViewById(R.id.spinner_converter_type);
        spinnerUnitFrom = (Spinner) findViewById(R.id.spinner_unit_from);
        spinnerUnitTo = (Spinner) findViewById(R.id.spinner_unit_to);
        etUnitInput = (EditText) findViewById(R.id.et_unit_input);
        tvUnitOutput = (TextView) findViewById(R.id.tv_unit_output);

        String[] types = new String[]{"Length", "Weight", "Temperature", "Area"};
        ArrayAdapter<String> adapterTypes = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerConverterType.setAdapter(adapterTypes);

        spinnerConverterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUnitSpinners(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etUnitInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateConversion();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Tip Splitter
        etBillAmount = (EditText) findViewById(R.id.et_bill_amount);
        etTipPercent = (EditText) findViewById(R.id.et_tip_percent);
        etSplitPeople = (EditText) findViewById(R.id.et_split_people);
        btnCalcTip = (Button) findViewById(R.id.btn_calc_tip);
        tvTipResult = (TextView) findViewById(R.id.tv_tip_result);

        btnCalcTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateTip();
            }
        });

        // BMI Calculator
        etBmiWeight = (EditText) findViewById(R.id.et_bmi_weight);
        etBmiHeight = (EditText) findViewById(R.id.et_bmi_height);
        btnCalcBmi = (Button) findViewById(R.id.btn_calc_bmi);
        tvBmiResult = (TextView) findViewById(R.id.tv_bmi_result);

        btnCalcBmi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBMI();
            }
        });
    }

    private void updateUnitSpinners(int typePos) {
        String[] units;
        if (typePos == 0) { // Length
            units = new String[]{"Meters", "Kilometers", "Centimeters", "Miles", "Feet", "Inches"};
        } else if (typePos == 1) { // Weight
            units = new String[]{"Kilograms", "Grams", "Pounds", "Ounces"};
        } else if (typePos == 2) { // Temperature
            units = new String[]{"Celsius", "Fahrenheit", "Kelvin"};
        } else { // Area
            units = new String[]{"Sq Meters", "Sq Km", "Sq Feet", "Acres"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerUnitFrom.setAdapter(adapter);
        spinnerUnitTo.setAdapter(adapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateConversion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerUnitFrom.setOnItemSelectedListener(listener);
        spinnerUnitTo.setOnItemSelectedListener(listener);
    }

    private void calculateConversion() {
        String inputStr = etUnitInput.getText().toString();
        if (inputStr.trim().isEmpty()) {
            tvUnitOutput.setText("0.0");
            return;
        }

        try {
            double val = Double.parseDouble(inputStr);
            int type = spinnerConverterType.getSelectedItemPosition();
            String from = (String) spinnerUnitFrom.getSelectedItem();
            String to = (String) spinnerUnitTo.getSelectedItem();

            double result = 0;

            if (type == 0) { // Length to Meters base
                double inMeters = 0;
                if (from.equals("Meters")) inMeters = val;
                else if (from.equals("Kilometers")) inMeters = val * 1000;
                else if (from.equals("Centimeters")) inMeters = val / 100;
                else if (from.equals("Miles")) inMeters = val * 1609.34;
                else if (from.equals("Feet")) inMeters = val * 0.3048;
                else if (from.equals("Inches")) inMeters = val * 0.0254;

                if (to.equals("Meters")) result = inMeters;
                else if (to.equals("Kilometers")) result = inMeters / 1000;
                else if (to.equals("Centimeters")) result = inMeters * 100;
                else if (to.equals("Miles")) result = inMeters / 1609.34;
                else if (to.equals("Feet")) result = inMeters / 0.3048;
                else if (to.equals("Inches")) result = inMeters / 0.0254;

            } else if (type == 1) { // Weight to Kg base
                double inKg = 0;
                if (from.equals("Kilograms")) inKg = val;
                else if (from.equals("Grams")) inKg = val / 1000;
                else if (from.equals("Pounds")) inKg = val * 0.453592;
                else if (from.equals("Ounces")) inKg = val * 0.0283495;

                if (to.equals("Kilograms")) result = inKg;
                else if (to.equals("Grams")) result = inKg * 1000;
                else if (to.equals("Pounds")) result = inKg / 0.453592;
                else if (to.equals("Ounces")) result = inKg / 0.0283495;

            } else if (type == 2) { // Temperature
                if (from.equals(to)) {
                    result = val;
                } else if (from.equals("Celsius") && to.equals("Fahrenheit")) {
                    result = (val * 9 / 5) + 32;
                } else if (from.equals("Celsius") && to.equals("Kelvin")) {
                    result = val + 273.15;
                } else if (from.equals("Fahrenheit") && to.equals("Celsius")) {
                    result = (val - 32) * 5 / 9;
                } else if (from.equals("Fahrenheit") && to.equals("Kelvin")) {
                    result = (val - 32) * 5 / 9 + 273.15;
                } else if (from.equals("Kelvin") && to.equals("Celsius")) {
                    result = val - 273.15;
                } else if (from.equals("Kelvin") && to.equals("Fahrenheit")) {
                    result = (val - 273.15) * 9 / 5 + 32;
                }
            } else { // Area
                double inSqMeters = 0;
                if (from.equals("Sq Meters")) inSqMeters = val;
                else if (from.equals("Sq Km")) inSqMeters = val * 1000000;
                else if (from.equals("Sq Feet")) inSqMeters = val * 0.092903;
                else if (from.equals("Acres")) inSqMeters = val * 4046.86;

                if (to.equals("Sq Meters")) result = inSqMeters;
                else if (to.equals("Sq Km")) result = inSqMeters / 1000000;
                else if (to.equals("Sq Feet")) result = inSqMeters / 0.092903;
                else if (to.equals("Acres")) result = inSqMeters / 4046.86;
            }

            tvUnitOutput.setText(df.format(result) + " " + to);
        } catch (Exception e) {
            tvUnitOutput.setText("0.0");
        }
    }

    private void calculateTip() {
        String billStr = etBillAmount.getText().toString();
        String tipStr = etTipPercent.getText().toString();
        String peopleStr = etSplitPeople.getText().toString();

        if (billStr.trim().isEmpty()) return;

        double bill = Double.parseDouble(billStr);
        double tipPct = tipStr.trim().isEmpty() ? 15 : Double.parseDouble(tipStr);
        int people = peopleStr.trim().isEmpty() ? 1 : Integer.parseInt(peopleStr);

        if (people <= 0) people = 1;

        double tipTotal = bill * (tipPct / 100);
        double grandTotal = bill + tipTotal;
        double perPerson = grandTotal / people;

        tvTipResult.setText(String.format("Total Tip: $%.2f | Per Person: $%.2f", tipTotal, perPerson));
    }

    private void calculateBMI() {
        String weightStr = etBmiWeight.getText().toString();
        String heightStr = etBmiHeight.getText().toString();

        if (weightStr.trim().isEmpty() || heightStr.trim().isEmpty()) return;

        double weight = Double.parseDouble(weightStr);
        double heightMeters = Double.parseDouble(heightStr) / 100;

        if (heightMeters <= 0) return;

        double bmi = weight / (heightMeters * heightMeters);
        String category;
        if (bmi < 18.5) category = "Underweight";
        else if (bmi < 24.9) category = "Normal weight";
        else if (bmi < 29.9) category = "Overweight";
        else category = "Obese";

        tvBmiResult.setText(String.format("BMI: %.1f (%s)", bmi, category));
    }

    private void showHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Calculation History");

        ListView listView = new ListView(this);
        HistoryAdapter adapter = new HistoryAdapter(this, historyList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setNegativeButton("Clear History", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                historyList.clear();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setPositiveButton("Close", null);

        final AlertDialog dialog = builder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HistoryItem item = historyList.get(position);
                currentExpression.setLength(0);
                currentExpression.append(item.getResult());
                updateDisplay();
                evaluateCurrentExpression();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Calculator Result", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard: " + text, Toast.LENGTH_SHORT).show();
        }
    }
}