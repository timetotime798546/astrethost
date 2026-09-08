package com.calculator3dpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvFormula;
    private TextView tvAngleIndicator;
    private TextView tvMemoryIndicator;
    private LinearLayout layoutScientific;
    private RelativeLayout layoutHistoryView;
    private LinearLayout layoutHistoryEntries;

    private Button btnModeDeg;
    private Button btnToggleSci;
    private Button btnHistoryPanel;
    private Button btnClearHistory;
    private Button btnCloseHistory;

    // States
    private StringBuilder currentInput = new StringBuilder();
    private boolean isDegreeMode = true;
    private boolean isSciActive = false;
    private double memoryValue = 0.0;
    private List<String> historyList = new ArrayList<String>();
    private boolean hasEvaluated = false;

    // Vibrator tactile system
    private Vibrator tactileFeedback;

    // Decimal layout formatter
    private final DecimalFormat formatter = new DecimalFormat("#.#########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve services
        tactileFeedback = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Bind components
        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvAngleIndicator = (TextView) findViewById(R.id.tv_angle_indicator);
        tvMemoryIndicator = (TextView) findViewById(R.id.tv_memory_indicator);
        layoutScientific = (LinearLayout) findViewById(R.id.layout_scientific);
        layoutHistoryView = (RelativeLayout) findViewById(R.id.layout_history_view);
        layoutHistoryEntries = (LinearLayout) findViewById(R.id.layout_history_entries);

        btnModeDeg = (Button) findViewById(R.id.btn_mode_deg);
        btnToggleSci = (Button) findViewById(R.id.btn_toggle_sci);
        btnHistoryPanel = (Button) findViewById(R.id.btn_history_panel);
        btnClearHistory = (Button) findViewById(R.id.btn_clear_history);
        btnCloseHistory = (Button) findViewById(R.id.btn_close_history);

        // Load Persistent Variables
        loadState();
        updateDisplay();
        updateMemoryStatus();

        // Register Action Listeners
        registerKeypadListeners();
        registerUtilityListeners();
    }

    private void performTactileClick() {
        if (tactileFeedback != null && tactileFeedback.hasVibrator()) {
            tactileFeedback.vibrate(30);
        }
    }

    private void registerUtilityListeners() {
        // Toggle Angle Modes (DEG / RAD)
        btnModeDeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                isDegreeMode = !isDegreeMode;
                tvAngleIndicator.setText(isDegreeMode ? "DEG" : "RAD");
                btnModeDeg.setText(isDegreeMode ? "DEG" : "RAD");
                btnModeDeg.setBackgroundResource(isDegreeMode ? R.drawable.btn_3d_toggle_active : R.drawable.btn_3d_toggle_inactive);
            }
        });

        // Expand/Collapse Scientific Drawer
        btnToggleSci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                isSciActive = !isSciActive;
                layoutScientific.setVisibility(isSciActive ? View.VISIBLE : View.GONE);
                btnToggleSci.setBackgroundResource(isSciActive ? R.drawable.btn_3d_toggle_active : R.drawable.btn_3d_toggle_inactive);
            }
        });

        // Show History slide-panel
        btnHistoryPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                renderHistoryItems();
                layoutHistoryView.setVisibility(View.VISIBLE);
                btnHistoryPanel.setBackgroundResource(R.drawable.btn_3d_toggle_active);
            }
        });

        // Hide History panel
        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                layoutHistoryView.setVisibility(View.GONE);
                btnHistoryPanel.setBackgroundResource(R.drawable.btn_3d_toggle_inactive);
            }
        });

        // Wipe History Local database
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                historyList.clear();
                saveState();
                renderHistoryItems();
                Toast.makeText(MainActivity.this, "Calculation logs purged", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerKeypadListeners() {
        // IDs of generic buttons sending plain text character sequences
        int[] plainButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dec, R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div,
            R.id.btn_open_paren, R.id.btn_close_paren, R.id.btn_pi, R.id.btn_e, R.id.btn_pow
        };

        View.OnClickListener clickHandler = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performTactileClick();
                Button b = (Button) view;
                String pressedVal = b.getText().toString();

                if (hasEvaluated) {
                    // Check if user clicks an operator. If so, chain calculation. Otherwise, reset display.
                    char target = pressedVal.charAt(0);
                    if (target == '+' || target == '-' || target == '×' || target == '÷' || target == '^') {
                        hasEvaluated = false;
                    } else {
                        currentInput.setLength(0);
                        hasEvaluated = false;
                    }
                }

                if (pressedVal.equals("×") || pressedVal.equals("÷")) {
                    currentInput.append(pressedVal);
                } else if (pressedVal.equals("π")) {
                    currentInput.append("π");
                } else {
                    currentInput.append(pressedVal);
                }
                updateDisplay();
            }
        };

        for (int id : plainButtons) {
            findViewById(id).setOnClickListener(clickHandler);
        }

        // Functional parameters
        findViewById(R.id.btn_sin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("sin("); }
        });
        findViewById(R.id.btn_cos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("cos("); }
        });
        findViewById(R.id.btn_tan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("tan("); }
        });
        findViewById(R.id.btn_ln).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("ln("); }
        });
        findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("log("); }
        });
        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { performScientificMethod("√("); }
        });

        findViewById(R.id.btn_fact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                currentInput.append("!");
                updateDisplay();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                currentInput.append("%");
                updateDisplay();
            }
        });

        // Action Keys
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                currentInput.setLength(0);
                tvFormula.setText("");
                hasEvaluated = false;
                updateDisplay();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                if (currentInput.length() > 0) {
                    // Delete trailing scientific terms if present
                    if (currentInput.length() >= 4 && (currentInput.toString().endsWith("sin(") || currentInput.toString().endsWith("cos(") || currentInput.toString().endsWith("tan("))) {
                        currentInput.delete(currentInput.length() - 4, currentInput.length());
                    } else if (currentInput.length() >= 3 && currentInput.toString().endsWith("ln(")) {
                        currentInput.delete(currentInput.length() - 3, currentInput.length());
                    } else if (currentInput.length() >= 4 && currentInput.toString().endsWith("log(")) {
                        currentInput.delete(currentInput.length() - 4, currentInput.length());
                    } else if (currentInput.length() >= 2 && currentInput.toString().endsWith("√(")) {
                        currentInput.delete(currentInput.length() - 2, currentInput.length());
                    } else {
                        currentInput.deleteCharAt(currentInput.length() - 1);
                    }
                }
                updateDisplay();
            }
        });

        // Memory calculations
        findViewById(R.id.btn_mem_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                evaluateMemoryValue(true);
            }
        });
        findViewById(R.id.btn_mem_sub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                evaluateMemoryValue(false);
            }
        });
        findViewById(R.id.btn_mem_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                memoryValue = 0.0;
                saveState();
                updateMemoryStatus();
                Toast.makeText(MainActivity.this, "Memory cleared", Toast.LENGTH_SHORT).show();
            }
        });

        // Equals processing key
        findViewById(R.id.btn_eq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTactileClick();
                executeCalculation();
            }
        });
    }

    private void performScientificMethod(String val) {
        performTactileClick();
        if (hasEvaluated) {
            currentInput.setLength(0);
            hasEvaluated = false;
        }
        currentInput.append(val);
        updateDisplay();
    }

    private void updateDisplay() {
        if (currentInput.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentInput.toString());
        }
    }

    private void updateMemoryStatus() {
        if (memoryValue != 0.0) {
            tvMemoryIndicator.setVisibility(View.VISIBLE);
        } else {
            tvMemoryIndicator.setVisibility(View.INVISIBLE);
        }
    }

    private void evaluateMemoryValue(boolean add) {
        try {
            double currentDisplayVal = parseRawExpression(preprocessExpression(currentInput.toString()));
            if (add) {
                memoryValue += currentDisplayVal;
            } else {
                memoryValue -= currentDisplayVal;
            }
            saveState();
            updateMemoryStatus();
            Toast.makeText(this, "Memory stored: " + formatter.format(memoryValue), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Valid computation required to alter memory", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeCalculation() {
        String expression = currentInput.toString();
        if (expression.isEmpty()) return;

        tvFormula.setText(expression + " =");
        try {
            String sanitized = preprocessExpression(expression);
            double result = parseRawExpression(sanitized);

            String formattedResult = formatter.format(result);
            currentInput.setLength(0);
            currentInput.append(formattedResult);

            // Append to dynamic calculations archive
            historyList.add(0, expression + " = " + formattedResult);
            if (historyList.size() > 50) { // cap archives count
                historyList.remove(historyList.size() - 1);
            }
            saveState();

            hasEvaluated = true;
            updateDisplay();

        } catch (ArithmeticException ae) {
            tvDisplay.setText("Error: Division by 0");
            currentInput.setLength(0);
            hasEvaluated = true;
        } catch (Exception e) {
            tvDisplay.setText("Error: Invalid Expression");
            currentInput.setLength(0);
            hasEvaluated = true;
        }
    }

    private String preprocessExpression(String input) {
        if (input == null || input.isEmpty()) return "0";

        // Balance brackets internally to handle missing parentheses
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '(') count++;
            else if (input.charAt(i) == ')') count--;
        }
        StringBuilder sb = new StringBuilder(input);
        while (count > 0) {
            sb.append(")");
            count--;
        }
        String expr = sb.toString();

        // Convert skeuomorphic entities into compliant standard operators
        expr = expr.replace("×", "*");
        expr = expr.replace("÷", "/");
        expr = expr.replace("π", "π");
        expr = expr.replace("e", "e");

        // Implicit Multiplications adjustments:
        // 1) Constant implicit multiplication (e.g., "5π" to "5*π")
        expr = expr.replaceAll("(\\d)(?=π|e)", "$1*");
        // 2) Opening bracket implicit (e.g., "5(" to "5*(")
        expr = expr.replaceAll("(\\d)(?=\\()", "$1*");
        // 3) Closing bracket implicit (e.g., ")5" to ")*5")
        expr = expr.replaceAll("\\)(?=\\d)", ")*");
        // 4) Constant closing bracket implicit (e.g., "π(" to "π*(")
        expr = expr.replaceAll("(π|e)(?=\\()", "$1*");

        return expr;
    }

    // Dynamic High-Fidelity Custom History Drawer rendering logic
    private void renderHistoryItems() {
        layoutHistoryEntries.removeAllViews();
        if (historyList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No equations archived yet.");
            emptyText.setTextColor(0xFF888888);
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            emptyText.setPadding(0, 24, 0, 0);
            layoutHistoryEntries.addView(emptyText);
            return;
        }

        for (final String item : historyList) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 8, 0, 8);
            
            // Partition input/result
            String[] parts = item.split("=");
            String equationPart = parts[0].trim() + " =";
            String resultPart = parts.length > 1 ? parts[1].trim() : "";

            TextView eqView = new TextView(this);
            eqView.setText(equationPart);
            eqView.setTextColor(0xFFAAAAAA);
            eqView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            TextView resView = new TextView(this);
            resView.setText(resultPart);
            resView.setTextColor(0xFF00FFCC);
            resView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            resView.setTextStyle(android.graphics.Typeface.BOLD);
            resView.setPadding(0, 2, 0, 6);

            itemLayout.addView(eqView);
            itemLayout.addView(resView);

            // Add divider
            View divider = new View(this);
            divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFF333D42);
            itemLayout.addView(divider);

            // Tap history record to load it back into active display
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performTactileClick();
                    String loadedResult = item.substring(item.indexOf("=") + 1).trim();
                    currentInput.setLength(0);
                    currentInput.append(loadedResult);
                    tvFormula.setText(item.substring(0, item.indexOf("=")).trim());
                    hasEvaluated = false;
                    updateDisplay();
                    layoutHistoryView.setVisibility(View.GONE);
                    btnHistoryPanel.setBackgroundResource(R.drawable.btn_3d_toggle_inactive);
                }
            });

            layoutHistoryEntries.addView(itemLayout);
        }
    }

    // Math Interpreter Engine Core
    private double parseRawExpression(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean consume(int charToConsume) {
                while (ch == ' ') nextChar();
                if (ch == charToConsume) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Syntax Error: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm();
                    else if (consume('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('*')) x *= parseFactor();
                    else if (consume('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                double x = parseFactorialOrPrimary(); // Updated to ensure correct parsing priority
                for (;;) {
                    if (consume('^')) x = Math.pow(x, parseFactor());
                    else return x;
                }
            }

            double parseFactorialOrPrimary() {
                double x = parsePrimary();
                // Check post-operators (Percentage / Factorial)
                for (;;) {
                    if (consume('!')) {
                        x = performFactorialCalculation(x);
                    } else if (consume('%')) {
                        x = x / 100.0;
                    } else {
                        break;
                    }
                }
                return x;
            }

            double parsePrimary() {
                if (consume('+')) return parsePrimary();
                if (consume('-')) return -parsePrimary();

                double x;
                int startPos = this.pos;
                if (consume('(')) {
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if ((ch >= 'a' && ch <= 'z') || ch == '√' || ch == 'π') {
                    while ((ch >= 'a' && ch <= 'z') || ch == '√' || ch == 'π') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (func.equals("π")) {
                        x = Math.PI;
                    } else if (func.equals("e")) {
                        x = Math.E;
                    } else {
                        if (!consume('(')) {
                            throw new RuntimeException("Functions must encompass parameters inside parentheses");
                        }
                        double arg = parseExpression();
                        consume(')');
                        
                        if (func.equals("sin")) {
                            x = Math.sin(isDegreeMode ? Math.toRadians(arg) : arg);
                        } else if (func.equals("cos")) {
                            x = Math.cos(isDegreeMode ? Math.toRadians(arg) : arg);
                        } else if (func.equals("tan")) {
                            x = Math.tan(isDegreeMode ? Math.toRadians(arg) : arg);
                        } else if (func.equals("log")) {
                            x = Math.log10(arg);
                        } else if (func.equals("ln")) {
                            x = Math.log(arg);
                        } else if (func.equals("√")) {
                            x = Math.sqrt(arg);
                        } else {
                            throw new RuntimeException("Unknown Function: " + func);
                        }
                    }
                } else {
                    throw new RuntimeException("Unexpected Entity: " + (char) ch);
                }

                return x;
            }

            double performFactorialCalculation(double val) {
                int roundedValue = (int) Math.round(val);
                if (roundedValue < 0) return 0.0;
                double currentProduct = 1.0;
                for (int i = 1; i <= roundedValue; i++) {
                    currentProduct *= i;
                }
                return currentProduct;
            }
        }.parse();
    }

    // SharedPreferences State Preservation
    private void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences("CalculationsStorage", MODE_PRIVATE).edit();
        editor.putString("current_input", currentInput.toString());
        editor.putBoolean("degree_mode", isDegreeMode);
        editor.putFloat("memory_value", (float) memoryValue);

        // Serialize history sequence into standard structured lines
        StringBuilder serialized = new StringBuilder();
        for (String record : historyList) {
            serialized.append(record).append("##");
        }
        editor.putString("history_serialized", serialized.toString());
        editor.apply();
    }

    private void loadState() {
        SharedPreferences pref = getSharedPreferences("CalculationsStorage", MODE_PRIVATE);
        currentInput.append(pref.getString("current_input", ""));
        isDegreeMode = pref.getBoolean("degree_mode", true);
        memoryValue = pref.getFloat("memory_value", 0.0f);

        String historySerialized = pref.getString("history_serialized", "");
        if (!historySerialized.isEmpty()) {
            String[] splitString = historySerialized.split("##");
            historyList.addAll(Arrays.asList(splitString));
        }

        // Apply loaded parameters
        tvAngleIndicator.setText(isDegreeMode ? "DEG" : "RAD");
        btnModeDeg.setText(isDegreeMode ? "DEG" : "RAD");
        btnModeDeg.setBackgroundResource(isDegreeMode ? R.drawable.btn_3d_toggle_active : R.drawable.btn_3d_toggle_inactive);
    }
}