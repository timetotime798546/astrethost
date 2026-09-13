package com.moderncalculator.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvHistory;
    private TextView tvAppLabel;
    private Button btnThemeToggle;
    private LinearLayout rootLayout;
    private LinearLayout keypadContainer;
    private View dividerLine;

    // Number & Operator Buttons
    private Button btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private Button btnAdd, btnSubtract, btnMultiply, btnDivide, btnEquals;
    private Button btnClear, btnBackspace, btnPercent, btnDot, btnNegate;

    // State Variables
    private StringBuilder currentInput = new StringBuilder();
    private String historyString = "";
    private boolean isDarkTheme = true;
    private boolean isResultState = false;

    // Display Decimal Formatter
    private final DecimalFormat format = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts and text views
        rootLayout = (LinearLayout) findViewById(R.id.root_layout);
        keypadContainer = (LinearLayout) findViewById(R.id.keypad_container);
        dividerLine = findViewById(R.id.divider_line);
        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvHistory = (TextView) findViewById(R.id.tv_history);
        tvAppLabel = (TextView) findViewById(R.id.tv_app_label);
        btnThemeToggle = (Button) findViewById(R.id.btn_theme_toggle);

        // Bind numerical buttons
        btn0 = (Button) findViewById(R.id.btn_0);
        btn1 = (Button) findViewById(R.id.btn_1);
        btn2 = (Button) findViewById(R.id.btn_2);
        btn3 = (Button) findViewById(R.id.btn_3);
        btn4 = (Button) findViewById(R.id.btn_4);
        btn5 = (Button) findViewById(R.id.btn_5);
        btn6 = (Button) findViewById(R.id.btn_6);
        btn7 = (Button) findViewById(R.id.btn_7);
        btn8 = (Button) findViewById(R.id.btn_8);
        btn9 = (Button) findViewById(R.id.btn_9);

        // Bind action buttons
        btnAdd = (Button) findViewById(R.id.btn_add);
        btnSubtract = (Button) findViewById(R.id.btn_subtract);
        btnMultiply = (Button) findViewById(R.id.btn_multiply);
        btnDivide = (Button) findViewById(R.id.btn_divide);
        btnEquals = (Button) findViewById(R.id.btn_equals);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnBackspace = (Button) findViewById(R.id.btn_backspace);
        btnPercent = (Button) findViewById(R.id.btn_percent);
        btnDot = (Button) findViewById(R.id.btn_dot);
        btnNegate = (Button) findViewById(R.id.btn_negate);

        setupClickListeners();
        updateDisplay();
    }

    private void setupClickListeners() {
        // Theme toggle logic
        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTheme();
            }
        });

        // Numerical button clicks
        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultState) {
                    currentInput.setLength(0);
                    isResultState = false;
                }
                Button button = (Button) v;
                String buttonText = button.getText().toString();
                
                // Prevent multiple zeros as initial string
                if (currentInput.toString().equals("0") && buttonText.equals("0")) {
                    return;
                }
                if (currentInput.toString().equals("0")) {
                    currentInput.setLength(0);
                }
                
                currentInput.append(buttonText);
                updateDisplay();
            }
        };

        btn0.setOnClickListener(numberClickListener);
        btn1.setOnClickListener(numberClickListener);
        btn2.setOnClickListener(numberClickListener);
        btn3.setOnClickListener(numberClickListener);
        btn4.setOnClickListener(numberClickListener);
        btn5.setOnClickListener(numberClickListener);
        btn6.setOnClickListener(numberClickListener);
        btn7.setOnClickListener(numberClickListener);
        btn8.setOnClickListener(numberClickListener);
        btn9.setOnClickListener(numberClickListener);

        // Operator click listener
        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isResultState = false;
                Button button = (Button) v;
                String opSymbol = getOperatorValue(button.getText().toString());
                
                if (currentInput.length() == 0) {
                    // Check if we can change trailing operator in empty formula context
                    if (historyString.length() > 0) {
                        char lastChar = historyString.charAt(historyString.length() - 1);
                        if (isOperatorChar(lastChar)) {
                            historyString = historyString.substring(0, historyString.length() - 1) + opSymbol;
                        } else {
                            historyString += opSymbol;
                        }
                    } else {
                        // Assume 0 starting point
                        historyString = "0" + opSymbol;
                    }
                } else {
                    historyString += currentInput.toString() + opSymbol;
                    currentInput.setLength(0);
                }
                updateDisplay();
            }
        };

        btnAdd.setOnClickListener(operatorClickListener);
        btnSubtract.setOnClickListener(operatorClickListener);
        btnMultiply.setOnClickListener(operatorClickListener);
        btnDivide.setOnClickListener(operatorClickListener);

        // Clear everything
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput.setLength(0);
                historyString = "";
                isResultState = false;
                updateDisplay();
            }
        });

        // Clear only the current buffer / backspace
        btnBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultState) {
                    currentInput.setLength(0);
                    isResultState = false;
                } else if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                }
                updateDisplay();
            }
        });

        // Percent modifier
        btnPercent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    try {
                        double value = Double.parseDouble(currentInput.toString()) / 100.0;
                        currentInput.setLength(0);
                        currentInput.append(format.format(value));
                        updateDisplay();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        // Decimal separator
        btnDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultState) {
                    currentInput.setLength(0);
                    currentInput.append("0.");
                    isResultState = false;
                    updateDisplay();
                    return;
                }
                if (currentInput.length() == 0) {
                    currentInput.append("0.");
                } else if (!currentInput.toString().contains(".")) {
                    currentInput.append(".");
                }
                updateDisplay();
            }
        });

        // Sign Negation (+/-)
        btnNegate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString());
                        val = -val;
                        currentInput.setLength(0);
                        currentInput.append(format.format(val));
                        updateDisplay();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        // Equal action to trigger calculations
        btnEquals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });
    }

    private String getOperatorValue(String buttonLabel) {
        if (buttonLabel.equals("÷")) return "/";
        if (buttonLabel.equals("×")) return "*";
        if (buttonLabel.equals("−")) return "-";
        return "+";
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private void updateDisplay() {
        // Setup default placeholder if current input is empty
        String topText = historyString.replace("/", " ÷ ").replace("*", " × ").replace("-", " − ").replace("+", " + ");
        tvHistory.setText(topText);

        if (currentInput.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentInput.toString());
        }
    }

    private void calculateResult() {
        String fullExpression = historyString + currentInput.toString();
        if (fullExpression.trim().isEmpty()) {
            return;
        }

        // Clean trailing operators before solving
        char lastChar = fullExpression.charAt(fullExpression.length() - 1);
        if (isOperatorChar(lastChar)) {
            fullExpression = fullExpression.substring(0, fullExpression.length() - 1);
        }

        try {
            double finalVal = evaluateMath(fullExpression);
            
            // Format result safely
            String stringVal = format.format(finalVal);
            
            // Set dynamic transition and history logging
            historyString = "";
            currentInput.setLength(0);
            currentInput.append(stringVal);
            isResultState = true;
            updateDisplay();
        } catch (ArithmeticException ae) {
            Toast.makeText(this, "Math Error: " + ae.getMessage(), Toast.LENGTH_SHORT).show();
            clearOnError();
        } catch (Exception e) {
            Toast.makeText(this, "Calculation Error", Toast.LENGTH_SHORT).show();
            clearOnError();
        }
    }

    private void clearOnError() {
        currentInput.setLength(0);
        historyString = "";
        isResultState = false;
        updateDisplay();
    }

    // Dynamic, Robust Custom Evaluator in standard pure Java 8 (no scripts, no external libraries)
    private double evaluateMath(String str) {
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
                if (pos < str.length()) throw new RuntimeException("Unexpected expression layout: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) {
                        double denom = parseFactor();
                        if (denom == 0) throw new ArithmeticException("Divide by zero");
                        x /= denom; // division
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character sequence: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }

    // Modern theme toggle (Light and Dark custom styling programmatically changed)
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;

        if (isDarkTheme) {
            btnThemeToggle.setText("LIGHT MODE");
            btnThemeToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2C2C2E")));
            btnThemeToggle.setTextColor(Color.WHITE);

            rootLayout.setBackgroundColor(Color.parseColor("#121214"));
            tvDisplay.setTextColor(Color.WHITE);
            tvHistory.setTextColor(Color.parseColor("#8E8E93"));
            tvAppLabel.setTextColor(Color.parseColor("#8E8E93"));
            dividerLine.setBackgroundColor(Color.parseColor("#1C1C1E"));

            // Primary standard buttons
            int numericDarkBg = Color.parseColor("#1C1C1E");
            int modifierDarkBg = Color.parseColor("#2C2C2E");

            setButtonTheme(btn0, numericDarkBg, Color.WHITE);
            setButtonTheme(btn1, numericDarkBg, Color.WHITE);
            setButtonTheme(btn2, numericDarkBg, Color.WHITE);
            setButtonTheme(btn3, numericDarkBg, Color.WHITE);
            setButtonTheme(btn4, numericDarkBg, Color.WHITE);
            setButtonTheme(btn5, numericDarkBg, Color.WHITE);
            setButtonTheme(btn6, numericDarkBg, Color.WHITE);
            setButtonTheme(btn7, numericDarkBg, Color.WHITE);
            setButtonTheme(btn8, numericDarkBg, Color.WHITE);
            setButtonTheme(btn9, numericDarkBg, Color.WHITE);
            setButtonTheme(btnDot, numericDarkBg, Color.WHITE);
            setButtonTheme(btnNegate, numericDarkBg, Color.WHITE);

            setButtonTheme(btnClear, modifierDarkBg, Color.parseColor("#FF9F0A"));
            setButtonTheme(btnBackspace, modifierDarkBg, Color.parseColor("#FF9F0A"));
            setButtonTheme(btnPercent, modifierDarkBg, Color.parseColor("#30D158"));
            setButtonTheme(btnDivide, modifierDarkBg, Color.parseColor("#30D158"));
            setButtonTheme(btnMultiply, modifierDarkBg, Color.parseColor("#30D158"));
            setButtonTheme(btnSubtract, modifierDarkBg, Color.parseColor("#30D158"));
            setButtonTheme(btnAdd, modifierDarkBg, Color.parseColor("#30D158"));
            setButtonTheme(btnEquals, Color.parseColor("#30D158"), Color.WHITE);
        } else {
            btnThemeToggle.setText("DARK MODE");
            btnThemeToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E5E5EA")));
            btnThemeToggle.setTextColor(Color.BLACK);

            rootLayout.setBackgroundColor(Color.parseColor("#F2F2F7"));
            tvDisplay.setTextColor(Color.BLACK);
            tvHistory.setTextColor(Color.parseColor("#8E8E93"));
            tvAppLabel.setTextColor(Color.parseColor("#8E8E93"));
            dividerLine.setBackgroundColor(Color.parseColor("#D1D1D6"));

            // Light mode dynamic button style adjustments
            int numericLightBg = Color.parseColor("#FFFFFF");
            int modifierLightBg = Color.parseColor("#E5E5EA");

            setButtonTheme(btn0, numericLightBg, Color.BLACK);
            setButtonTheme(btn1, numericLightBg, Color.BLACK);
            setButtonTheme(btn2, numericLightBg, Color.BLACK);
            setButtonTheme(btn3, numericLightBg, Color.BLACK);
            setButtonTheme(btn4, numericLightBg, Color.BLACK);
            setButtonTheme(btn5, numericLightBg, Color.BLACK);
            setButtonTheme(btn6, numericLightBg, Color.BLACK);
            setButtonTheme(btn7, numericLightBg, Color.BLACK);
            setButtonTheme(btn8, numericLightBg, Color.BLACK);
            setButtonTheme(btn9, numericLightBg, Color.BLACK);
            setButtonTheme(btnDot, numericLightBg, Color.BLACK);
            setButtonTheme(btnNegate, numericLightBg, Color.BLACK);

            setButtonTheme(btnClear, modifierLightBg, Color.parseColor("#FF9500"));
            setButtonTheme(btnBackspace, modifierLightBg, Color.parseColor("#FF9500"));
            setButtonTheme(btnPercent, modifierLightBg, Color.parseColor("#34C759"));
            setButtonTheme(btnDivide, modifierLightBg, Color.parseColor("#34C759"));
            setButtonTheme(btnMultiply, modifierLightBg, Color.parseColor("#34C759"));
            setButtonTheme(btnSubtract, modifierLightBg, Color.parseColor("#34C759"));
            setButtonTheme(btnAdd, modifierLightBg, Color.parseColor("#34C759"));
            setButtonTheme(btnEquals, Color.parseColor("#34C759"), Color.WHITE);
        }
    }

    private void setButtonTheme(Button btn, int backgroundColor, int textColor) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(backgroundColor));
        btn.setTextColor(textColor);
    }
}