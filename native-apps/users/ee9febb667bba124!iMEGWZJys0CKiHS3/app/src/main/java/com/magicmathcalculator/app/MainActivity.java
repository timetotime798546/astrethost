package com.magicmathcalculator.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends Activity {

    // Panels / Layouts
    private LinearLayout layoutCalculator;
    private LinearLayout layoutKaprekar;
    private LinearLayout layoutMindReader;
    
    // Top Tabs Navigation
    private Button btnTabCalc;
    private Button btnTabKaprekar;
    private Button btnTabMind;

    // SECTION 1: Standard / Magic Calculator Variables
    private TextView txtCalcExpression;
    private TextView txtCalcDisplay;
    private String currentInput = "";
    private String lastExpression = "";
    private double firstOperand = Double.NaN;
    private String activeOperator = "";
    private boolean hasJustEvaluated = false;

    // SECTION 2: Kaprekar Variables
    private EditText edtKaprekarInput;
    private TextView txtKaprekarOutput;

    // SECTION 3: Mind Reader Game Variables
    private LinearLayout layoutReaderIntro;
    private LinearLayout layoutReaderPlay;
    private LinearLayout layoutReaderReveal;
    private TextView txtReaderStep;
    private TextView txtReaderGrid;
    private TextView txtReaderFinalResult;
    private TextView txtReaderExplanation;
    private Button btnReaderNo;
    private Button btnReaderYes;
    private Button btnReaderStart;
    private Button btnReaderReset;

    private int mindReaderStepIndex = 0; // Steps 0 to 5 for binary digits
    private int mindReaderScoreSum = 0;   // The compiled final binary integer outcome

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Toast.makeText(this, "Error inflating layout: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Bind Tabs with safety check
        btnTabCalc = (Button) findViewById(R.id.btn_tab_calculator);
        btnTabKaprekar = (Button) findViewById(R.id.btn_tab_kaprekar);
        btnTabMind = (Button) findViewById(R.id.btn_tab_mindreader);

        // Bind Main Layout Panels
        layoutCalculator = (LinearLayout) findViewById(R.id.layout_calculator);
        layoutKaprekar = (LinearLayout) findViewById(R.id.layout_kaprekar);
        layoutMindReader = (LinearLayout) findViewById(R.id.layout_mindreader);

        if (btnTabCalc != null) {
            btnTabCalc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab("CALC");
                }
            });
        }

        if (btnTabKaprekar != null) {
            btnTabKaprekar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab("KAPREKAR");
                }
            });
        }

        if (btnTabMind != null) {
            btnTabMind.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab("MIND");
                }
            });
        }

        // Setup Components safely
        setupCalculator();
        setupKaprekar();
        setupMindReader();
    }

    private void switchTab(String tabName) {
        int activeBg = Color.parseColor("#4A0E4E");
        int activeText = Color.parseColor("#FFFFFF");
        int inactiveBg = Color.parseColor("#FFFFFF");
        int inactiveText = Color.parseColor("#4A0E4E");

        if (layoutCalculator == null || layoutKaprekar == null || layoutMindReader == null) {
            return;
        }

        if (tabName.equals("CALC")) {
            layoutCalculator.setVisibility(View.VISIBLE);
            layoutKaprekar.setVisibility(View.GONE);
            layoutMindReader.setVisibility(View.GONE);

            if (btnTabCalc != null) {
                btnTabCalc.setBackgroundColor(activeBg);
                btnTabCalc.setTextColor(activeText);
            }
            if (btnTabKaprekar != null) {
                btnTabKaprekar.setBackgroundColor(inactiveBg);
                btnTabKaprekar.setTextColor(inactiveText);
            }
            if (btnTabMind != null) {
                btnTabMind.setBackgroundColor(inactiveBg);
                btnTabMind.setTextColor(inactiveText);
            }
        } else if (tabName.equals("KAPREKAR")) {
            layoutCalculator.setVisibility(View.GONE);
            layoutKaprekar.setVisibility(View.VISIBLE);
            layoutMindReader.setVisibility(View.GONE);

            if (btnTabCalc != null) {
                btnTabCalc.setBackgroundColor(inactiveBg);
                btnTabCalc.setTextColor(inactiveText);
            }
            if (btnTabKaprekar != null) {
                btnTabKaprekar.setBackgroundColor(activeBg);
                btnTabKaprekar.setTextColor(activeText);
            }
            if (btnTabMind != null) {
                btnTabMind.setBackgroundColor(inactiveBg);
                btnTabMind.setTextColor(inactiveText);
            }
        } else {
            layoutCalculator.setVisibility(View.GONE);
            layoutKaprekar.setVisibility(View.GONE);
            layoutMindReader.setVisibility(View.VISIBLE);

            if (btnTabCalc != null) {
                btnTabCalc.setBackgroundColor(inactiveBg);
                btnTabCalc.setTextColor(inactiveText);
            }
            if (btnTabKaprekar != null) {
                btnTabKaprekar.setBackgroundColor(inactiveBg);
                btnTabKaprekar.setTextColor(inactiveText);
            }
            if (btnTabMind != null) {
                btnTabMind.setBackgroundColor(activeBg);
                btnTabMind.setTextColor(activeText);
            }
        }
    }

    // ==========================================
    // SECTION 1: Standard / Magic Calculator Business Logic
    // ==========================================
    private void setupCalculator() {
        txtCalcExpression = (TextView) findViewById(R.id.txt_calc_expression);
        txtCalcDisplay = (TextView) findViewById(R.id.txt_calc_display);

        // Bind Calculator Digits safely
        int[] digitIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9};
        for (int i = 0; i < digitIds.length; i++) {
            final int digit = i;
            View btn = findViewById(digitIds[i]);
            if (btn != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appendDigit(digit);
                    }
                });
            }
        }

        View btnDot = findViewById(R.id.btn_dot);
        if (btnDot != null) {
            btnDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!currentInput.contains(".")) {
                        if (currentInput.isEmpty()) {
                            currentInput = "0.";
                        } else {
                            currentInput += ".";
                        }
                        updateCalcDisplay();
                    }
                }
            });
        }

        View btnC = findViewById(R.id.btn_c);
        if (btnC != null) {
            btnC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearAllCalculator();
                }
            });
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentInput.length() > 0) {
                        currentInput = currentInput.substring(0, currentInput.length() - 1);
                        if (currentInput.isEmpty()) {
                            currentInput = "0";
                        }
                        updateCalcDisplay();
                    }
                }
            });
        }

        // Operators
        View btnAdd = findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setOperator("+");
                }
            });
        }

        View btnSub = findViewById(R.id.btn_sub);
        if (btnSub != null) {
            btnSub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setOperator("-");
                }
            });
        }

        View btnMul = findViewById(R.id.btn_mul);
        if (btnMul != null) {
            btnMul.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setOperator("*");
                }
            });
        }

        View btnDiv = findViewById(R.id.btn_div);
        if (btnDiv != null) {
            btnDiv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setOperator("/");
                }
            });
        }

        View btnEq = findViewById(R.id.btn_eq);
        if (btnEq != null) {
            btnEq.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    evaluateCalculatorResult();
                }
            });
        }

        // Magic Calculator 1089 Trick
        View btnMagic = findViewById(R.id.btn_magic_trick);
        if (btnMagic != null) {
            btnMagic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMagicTrickDialogue();
                }
            });
        }
    }

    private void appendDigit(int digit) {
        if (hasJustEvaluated || currentInput.equals("0")) {
            currentInput = "";
            hasJustEvaluated = false;
        }
        currentInput += String.valueOf(digit);
        updateCalcDisplay();
    }

    private void updateCalcDisplay() {
        if (txtCalcDisplay != null) {
            txtCalcDisplay.setText(currentInput.isEmpty() ? "0" : currentInput);
        }
    }

    private void clearAllCalculator() {
        currentInput = "";
        lastExpression = "";
        firstOperand = Double.NaN;
        activeOperator = "";
        hasJustEvaluated = false;
        if (txtCalcDisplay != null) {
            txtCalcDisplay.setText("0");
        }
        if (txtCalcExpression != null) {
            txtCalcExpression.setText("");
        }
    }

    private void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            try {
                firstOperand = Double.parseDouble(currentInput);
                activeOperator = op;
                lastExpression = currentInput + " " + op;
                if (txtCalcExpression != null) {
                    txtCalcExpression.setText(lastExpression);
                }
                currentInput = "";
            } catch (NumberFormatException e) {
                // Handle gracefully
            }
        }
    }

    private void evaluateCalculatorResult() {
        if (Double.isNaN(firstOperand) || currentInput.isEmpty() || activeOperator.isEmpty()) {
            return;
        }

        try {
            double secondOperand = Double.parseDouble(currentInput);
            double result = 0.0;
            boolean error = false;

            if (activeOperator.equals("+")) {
                result = firstOperand + secondOperand;
            } else if (activeOperator.equals("-")) {
                result = firstOperand - secondOperand;
            } else if (activeOperator.equals("*")) {
                result = firstOperand * secondOperand;
            } else if (activeOperator.equals("/")) {
                if (secondOperand == 0) {
                    error = true;
                } else {
                    result = firstOperand / secondOperand;
                }
            }

            if (txtCalcExpression != null) {
                txtCalcExpression.setText(lastExpression + " " + currentInput + " =");
            }

            if (error) {
                if (txtCalcDisplay != null) {
                    txtCalcDisplay.setText("Error");
                }
                currentInput = "";
            } else {
                if (result == (long) result) {
                    currentInput = String.valueOf((long) result);
                } else {
                    currentInput = String.valueOf(result);
                }
                if (txtCalcDisplay != null) {
                    txtCalcDisplay.setText(currentInput);
                }
            }

            hasJustEvaluated = true;
            firstOperand = Double.NaN;
            activeOperator = "";
        } catch (NumberFormatException e) {
            // Handled gracefully
        }
    }

    private void showMagicTrickDialogue() {
        if (txtCalcExpression != null) {
            txtCalcExpression.setText("🔮 Math Magic 1089 Activated!");
        }
        if (txtCalcDisplay != null) {
            txtCalcDisplay.setText("1089");
        }
        currentInput = "1089";
        hasJustEvaluated = true;

        Toast.makeText(this, "Magic Trick 1089: Think of a 3-digit number where digits are decreasing (e.g., 742). Reverse it (247). Subtract reverse from original (742-247=495). Now reverse that result (594) and add them (495+594). Result is ALWAYS 1089!", Toast.LENGTH_LONG).show();
    }

    // ==========================================
    // SECTION 2: Kaprekar Constant Business Logic
    // ==========================================
    private void setupKaprekar() {
        edtKaprekarInput = (EditText) findViewById(R.id.edt_kaprekar_input);
        txtKaprekarOutput = (TextView) findViewById(R.id.txt_kaprekar_output);
        Button btnCalculate = (Button) findViewById(R.id.btn_kaprekar_calculate);

        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processKaprekarMagic();
                }
            });
        }
    }

    private void processKaprekarMagic() {
        if (edtKaprekarInput == null || txtKaprekarOutput == null) {
            return;
        }
        
        String rawText = edtKaprekarInput.getText().toString().trim();
        if (rawText.length() != 4) {
            Toast.makeText(this, "Please enter a 4-digit number!", Toast.LENGTH_SHORT).show();
            return;
        }

        char[] characters = rawText.toCharArray();
        boolean allSame = true;
        for (int i = 1; i < characters.length; i++) {
            if (characters[i] != characters[0]) {
                allSame = false;
                break;
            }
        }

        if (allSame) {
            Toast.makeText(this, "Digits cannot be identical (e.g., avoid 1111, 2222)!", Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder resultLog = new StringBuilder();
        resultLog.append("Running Magic Routine for: ").append(rawText).append("\n\n");

        String currentNum = rawText;
        int stepCount = 0;
        final int MAX_STEPS = 10; 

        try {
            while (stepCount < MAX_STEPS) {
                stepCount++;
                
                while (currentNum.length() < 4) {
                    currentNum = "0" + currentNum;
                }

                char[] charArray = currentNum.toCharArray();
                Arrays.sort(charArray);

                String ascendingStr = new String(charArray);
                String descendingStr = "";
                for (int i = charArray.length - 1; i >= 0; i--) {
                    descendingStr += charArray[i];
                }

                int descVal = Integer.parseInt(descendingStr);
                int ascVal = Integer.parseInt(ascendingStr);
                int resultVal = descVal - ascVal;

                resultLog.append("Step ").append(stepCount).append(":\n")
                        .append("  • Sort Descending: ").append(descendingStr).append("\n")
                        .append("  • Sort Ascending: ").append(ascendingStr).append("\n")
                        .append("  • Math Operation: ").append(descVal).append(" - ").append(ascVal).append(" = ").append(resultVal).append("\n\n");

                currentNum = String.valueOf(resultVal);
                if (resultVal == 6174) {
                    resultLog.append("✨ BINGO! Reached Kaprekar's Constant (6174) in ")
                            .append(stepCount).append(" step(s)!");
                    break;
                }
                if (resultVal == 0) {
                    resultLog.append("Algorithm halted. Difference became 0.");
                    break;
                }
            }
        } catch (Exception e) {
            resultLog.append("Error processing algorithm calculations.");
        }

        txtKaprekarOutput.setText(resultLog.toString());
    }

    // ==========================================
    // SECTION 3: Binary Mind Reader Engine Business Logic
    // ==========================================
    private void setupMindReader() {
        layoutReaderIntro = (LinearLayout) findViewById(R.id.layout_reader_intro);
        layoutReaderPlay = (LinearLayout) findViewById(R.id.layout_reader_play);
        layoutReaderReveal = (LinearLayout) findViewById(R.id.layout_reader_reveal);

        txtReaderStep = (TextView) findViewById(R.id.txt_reader_step);
        txtReaderGrid = (TextView) findViewById(R.id.txt_reader_numbers_grid);
        txtReaderFinalResult = (TextView) findViewById(R.id.txt_reader_final_result);
        txtReaderExplanation = (TextView) findViewById(R.id.txt_reader_magic_explanation);

        btnReaderStart = (Button) findViewById(R.id.btn_reader_start);
        btnReaderNo = (Button) findViewById(R.id.btn_reader_no);
        btnReaderYes = (Button) findViewById(R.id.btn_reader_yes);
        btnReaderReset = (Button) findViewById(R.id.btn_reader_reset);

        if (btnReaderStart != null) {
            btnReaderStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startMindReaderGame();
                }
            });
        }

        if (btnReaderYes != null) {
            btnReaderYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processUserResponse(true);
                }
            });
        }

        if (btnReaderNo != null) {
            btnReaderNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processUserResponse(false);
                }
            });
        }

        if (btnReaderReset != null) {
            btnReaderReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetMindReaderGame();
                }
            });
        }
    }

    private void startMindReaderGame() {
        mindReaderStepIndex = 0;
        mindReaderScoreSum = 0;

        if (layoutReaderIntro != null) layoutReaderIntro.setVisibility(View.GONE);
        if (layoutReaderPlay != null) layoutReaderPlay.setVisibility(View.VISIBLE);
        if (layoutReaderReveal != null) layoutReaderReveal.setVisibility(View.GONE);

        loadActiveMindReaderCard();
    }

    private void loadActiveMindReaderCard() {
        if (txtReaderStep == null || txtReaderGrid == null) {
            return;
        }

        txtReaderStep.setText("Card " + (mindReaderStepIndex + 1) + " of 6");

        StringBuilder stringGridBuilder = new StringBuilder();
        int count = 0;
        for (int i = 1; i <= 63; i++) {
            if ((i & (1 << mindReaderStepIndex)) != 0) {
                String paddedNumber = String.valueOf(i);
                if (paddedNumber.length() == 1) {
                    paddedNumber = "  " + paddedNumber + "  ";
                } else {
                    paddedNumber = " " + paddedNumber + " ";
                }
                stringGridBuilder.append(paddedNumber).append("  ");
                count++;
                
                if (count % 6 == 0) {
                    stringGridBuilder.append("\n");
                }
            }
        }

        txtReaderGrid.setText(stringGridBuilder.toString().trim());
    }

    private void processUserResponse(boolean inList) {
        if (inList) {
            mindReaderScoreSum += (1 << mindReaderStepIndex);
        }

        mindReaderStepIndex++;
        if (mindReaderStepIndex < 6) {
            loadActiveMindReaderCard();
        } else {
            revealGuessedMindResult();
        }
    }

    private void revealGuessedMindResult() {
        if (layoutReaderIntro != null) layoutReaderIntro.setVisibility(View.GONE);
        if (layoutReaderPlay != null) layoutReaderPlay.setVisibility(View.GONE);
        if (layoutReaderReveal != null) layoutReaderReveal.setVisibility(View.VISIBLE);

        if (mindReaderScoreSum == 0 || mindReaderScoreSum > 63) {
            if (txtReaderFinalResult != null) txtReaderFinalResult.setText("👻");
            if (txtReaderExplanation != null) {
                txtReaderExplanation.setText("Did you answer truthfully? It seems your secret number was out of the allowed boundary range [1 - 63]. Let's try again!");
            }
        } else {
            if (txtReaderFinalResult != null) {
                txtReaderFinalResult.setText(String.valueOf(mindReaderScoreSum));
            }
            if (txtReaderExplanation != null) {
                txtReaderExplanation.setText("Magical Binary algorithm complete! This trick works since any positive integer value has a unique representation in standard base-2 binary calculations.");
            }
        }
    }

    private void resetMindReaderGame() {
        if (layoutReaderIntro != null) layoutReaderIntro.setVisibility(View.VISIBLE);
        if (layoutReaderPlay != null) layoutReaderPlay.setVisibility(View.GONE);
        if (layoutReaderReveal != null) layoutReaderReveal.setVisibility(View.GONE);
    }
}