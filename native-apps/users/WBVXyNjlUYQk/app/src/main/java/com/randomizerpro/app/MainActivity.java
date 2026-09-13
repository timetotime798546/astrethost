package com.randomizerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    // Tab buttons
    private Button btnTabCoinDice;
    private Button btnTabDecision;
    private Button btnTabNumbers;
    private Button btnTabPassword;

    // Content scrolls
    private ScrollView layoutCoinDice;
    private ScrollView layoutDecision;
    private ScrollView layoutNumbers;
    private ScrollView layoutPassword;

    // Coin & Dice Views
    private TextView tvCoinVisual;
    private Button btnFlipCoin;
    private TextView tvDice1;
    private TextView tvDice2;
    private TextView tvDice3;
    private Spinner spinnerDiceCount;
    private Button btnRollDice;

    // Decision Maker Views
    private EditText etOptionInput;
    private Button btnAddOption;
    private LinearLayout listOptionsContainer;
    private Button btnDecide;
    private List<String> optionsList = new ArrayList<>();

    // Number Generator Views
    private EditText etNumMin;
    private EditText etNumMax;
    private EditText etNumQty;
    private CheckBox cbAllowDuplicates;
    private Button btnGenerateNumbers;
    private TextView tvNumbersResult;
    private TextView tvNumbersHistory;
    private List<String> numbersHistoryList = new ArrayList<>();

    // Password Generator Views
    private TextView tvPwdLengthVal;
    private SeekBar sbPasswordLength;
    private CheckBox cbPwdUpper;
    private CheckBox cbPwdLower;
    private CheckBox cbPwdDigits;
    private CheckBox cbPwdSymbols;
    private Button btnGeneratePassword;
    private TextView tvPasswordResult;
    private Button btnCopyPassword;

    private int currentPasswordLength = 12;
    private final Random random = new Random();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map Views
        initTabs();
        initCoinAndDice();
        initDecisionMaker();
        initNumberGenerator();
        initPasswordGenerator();

        // Show default tab (Coin & Dice)
        switchTab(0);
    }

    private void initTabs() {
        btnTabCoinDice = (Button) findViewById(R.id.btn_tab_coin_dice);
        btnTabDecision = (Button) findViewById(R.id.btn_tab_decision);
        btnTabNumbers = (Button) findViewById(R.id.btn_tab_numbers);
        btnTabPassword = (Button) findViewById(R.id.btn_tab_password);

        layoutCoinDice = (ScrollView) findViewById(R.id.layout_coin_dice);
        layoutDecision = (ScrollView) findViewById(R.id.layout_decision);
        layoutNumbers = (ScrollView) findViewById(R.id.layout_numbers);
        layoutPassword = (ScrollView) findViewById(R.id.layout_password);

        btnTabCoinDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        btnTabDecision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        // Clear all states and backgrounds
        btnTabCoinDice.setBackgroundColor(Color.TRANSPARENT);
        btnTabDecision.setBackgroundColor(Color.TRANSPARENT);
        btnTabNumbers.setBackgroundColor(Color.TRANSPARENT);
        btnTabPassword.setBackgroundColor(Color.TRANSPARENT);

        layoutCoinDice.setVisibility(View.GONE);
        layoutDecision.setVisibility(View.GONE);
        layoutNumbers.setVisibility(View.GONE);
        layoutPassword.setVisibility(View.GONE);

        // Highlight selected tab
        int activeTabColor = Color.parseColor("#485AC8"); // Slightly lighter indigo for feedback
        switch (tabIndex) {
            case 0:
                btnTabCoinDice.setBackgroundColor(activeTabColor);
                layoutCoinDice.setVisibility(View.VISIBLE);
                break;
            case 1:
                btnTabDecision.setBackgroundColor(activeTabColor);
                layoutDecision.setVisibility(View.VISIBLE);
                break;
            case 2:
                btnTabNumbers.setBackgroundColor(activeTabColor);
                layoutNumbers.setVisibility(View.VISIBLE);
                break;
            case 3:
                btnTabPassword.setBackgroundColor(activeTabColor);
                layoutPassword.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void initCoinAndDice() {
        tvCoinVisual = (TextView) findViewById(R.id.tv_coin_visual);
        btnFlipCoin = (Button) findViewById(R.id.btn_flip_coin);
        tvDice1 = (TextView) findViewById(R.id.tv_dice_1);
        tvDice2 = (TextView) findViewById(R.id.tv_dice_2);
        tvDice3 = (TextView) findViewById(R.id.tv_dice_3);
        spinnerDiceCount = (Spinner) findViewById(R.id.spinner_dice_count);
        btnRollDice = (Button) findViewById(R.id.btn_roll_dice);

        // Setup Spinner programmatically
        Integer[] diceOptions = {1, 2, 3};
        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, diceOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDiceCount.setAdapter(spinnerAdapter);

        spinnerDiceCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int count = (int) parent.getItemAtPosition(position);
                tvDice2.setVisibility(count >= 2 ? View.VISIBLE : View.GONE);
                tvDice3.setVisibility(count == 3 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnFlipCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCoinAnimation();
            }
        });

        btnRollDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDiceAnimation();
            }
        });
    }

    private void flipCoinAnimation() {
        btnFlipCoin.setEnabled(false);

        // Scale flip animation to simulate flipping over Y axis
        ScaleAnimation flipAnim = new ScaleAnimation(1f, 0.1f, 1f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        flipAnim.setDuration(120);
        flipAnim.setRepeatCount(5);
        flipAnim.setRepeatMode(Animation.REVERSE);
        
        flipAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                boolean isHeads = random.nextBoolean();
                tvCoinVisual.setText(isHeads ? "HEADS" : "TAILS");
                tvCoinVisual.setTextColor(isHeads ? Color.parseColor("#3F51B5") : Color.parseColor("#E91E63"));
                btnFlipCoin.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        tvCoinVisual.startAnimation(flipAnim);
    }

    private void rollDiceAnimation() {
        btnRollDice.setEnabled(false);

        // Shake translation animation for tactile feel
        TranslateAnimation shake = new TranslateAnimation(-15, 15, -15, 15);
        shake.setDuration(60);
        shake.setRepeatCount(6);
        shake.setRepeatMode(Animation.REVERSE);

        final String[] unicodeDice = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};

        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                int count = (int) spinnerDiceCount.getSelectedItem();
                tvDice1.setText(unicodeDice[random.nextInt(6)]);
                if (count >= 2) {
                    tvDice2.setText(unicodeDice[random.nextInt(6)]);
                }
                if (count == 3) {
                    tvDice3.setText(unicodeDice[random.nextInt(6)]);
                }
                btnRollDice.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Keep changing values during dynamic shake
                int count = (int) spinnerDiceCount.getSelectedItem();
                tvDice1.setText(unicodeDice[random.nextInt(6)]);
                if (count >= 2) {
                    tvDice2.setText(unicodeDice[random.nextInt(6)]);
                }
                if (count == 3) {
                    tvDice3.setText(unicodeDice[random.nextInt(6)]);
                }
            }
        });

        tvDice1.startAnimation(shake);
        if (tvDice2.getVisibility() == View.VISIBLE) tvDice2.startAnimation(shake);
        if (tvDice3.getVisibility() == View.VISIBLE) tvDice3.startAnimation(shake);
    }

    private void initDecisionMaker() {
        etOptionInput = (EditText) findViewById(R.id.et_option_input);
        btnAddOption = (Button) findViewById(R.id.btn_add_option);
        listOptionsContainer = (LinearLayout) findViewById(R.id.list_options_container);
        btnDecide = (Button) findViewById(R.id.btn_decide);

        // Populate sample options so screen isn't empty
        optionsList.add("Pizza");
        optionsList.add("Burgers");
        optionsList.add("Tacos");
        updateOptionsListView();

        btnAddOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etOptionInput.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please write an option first", Toast.LENGTH_SHORT).show();
                    return;
                }
                optionsList.add(input);
                etOptionInput.setText("");
                updateOptionsListView();
            }
        });

        btnDecide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optionsList.size() < 2) {
                    Toast.makeText(MainActivity.this, "Please add at least 2 options", Toast.LENGTH_SHORT).show();
                    return;
                }
                startDecisionAnimation();
            }
        });
    }

    private void updateOptionsListView() {
        listOptionsContainer.removeAllViews();
        for (int i = 0; i < optionsList.size(); i++) {
            final int index = i;
            String text = optionsList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(8, 8, 8, 8);
            row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#F9F9F9") : Color.parseColor("#EEEEEE"));

            TextView optionText = new TextView(this);
            optionText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            optionText.setText(text);
            optionText.setTextSize(14);
            optionText.setTextColor(Color.parseColor("#212121"));

            Button removeButton = new Button(this);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.width = dpToPx(50);
            btnParams.height = dpToPx(36);
            removeButton.setLayoutParams(btnParams);
            removeButton.setText("X");
            removeButton.setTextSize(12);
            removeButton.setTextColor(Color.WHITE);
            removeButton.setBackgroundColor(Color.parseColor("#E53935"));
            removeButton.setPadding(0, 0, 0, 0);

            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    optionsList.remove(index);
                    updateOptionsListView();
                }
            });

            row.addView(optionText);
            row.addView(removeButton);
            listOptionsContainer.addView(row);
        }
    }

    private void startDecisionAnimation() {
        btnDecide.setEnabled(false);
        final Handler handler = new Handler();
        final int iterations = 12;
        
        Runnable decisionRunnable = new Runnable() {
            int runCount = 0;
            @Override
            public void run() {
                if (runCount < iterations) {
                    // Randomly highlight index rows to build anticipation
                    int highlightedIdx = random.nextInt(optionsList.size());
                    for (int i = 0; i < listOptionsContainer.getChildCount(); i++) {
                        View child = listOptionsContainer.getChildAt(i);
                        if (i == highlightedIdx) {
                            child.setBackgroundColor(Color.parseColor("#FFE082")); // bright yellow highlight
                        } else {
                            child.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#F9F9F9") : Color.parseColor("#EEEEEE"));
                        }
                    }
                    runCount++;
                    handler.postDelayed(this, 100);
                } else {
                    // Animation finish: choose actual winner
                    int winningIndex = random.nextInt(optionsList.size());
                    final String winner = optionsList.get(winningIndex);
                    
                    // Restore original rows
                    updateOptionsListView();

                    // Alert display
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("And the Winner is...")
                        .setMessage(winner)
                        .setPositiveButton("Awesome!", null)
                        .show();

                    btnDecide.setEnabled(true);
                }
            }
        };

        handler.post(decisionRunnable);
    }

    private void initNumberGenerator() {
        etNumMin = (EditText) findViewById(R.id.et_num_min);
        etNumMax = (EditText) findViewById(R.id.et_num_max);
        etNumQty = (EditText) findViewById(R.id.et_num_qty);
        cbAllowDuplicates = (CheckBox) findViewById(R.id.cb_allow_duplicates);
        btnGenerateNumbers = (Button) findViewById(R.id.btn_generate_numbers);
        tvNumbersResult = (TextView) findViewById(R.id.tv_numbers_result);
        tvNumbersHistory = (TextView) findViewById(R.id.tv_numbers_history);

        btnGenerateNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNumbers();
            }
        });
    }

    private void generateNumbers() {
        String minStr = etNumMin.getText().toString().trim();
        String maxStr = etNumMax.getText().toString().trim();
        String qtyStr = etNumQty.getText().toString().trim();

        if (minStr.isEmpty() || maxStr.isEmpty() || qtyStr.isEmpty()) {
            Toast.makeText(this, "Please enter all value fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long minVal = Long.parseLong(minStr);
            long maxVal = Long.parseLong(maxStr);
            int qtyVal = Integer.parseInt(qtyStr);

            if (minVal > maxVal) {
                Toast.makeText(this, "Min value cannot be greater than Max", Toast.LENGTH_SHORT).show();
                return;
            }

            if (qtyVal <= 0) {
                Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
                return;
            }

            long range = (maxVal - minVal) + 1;
            boolean allowDuplicates = cbAllowDuplicates.isChecked();

            if (!allowDuplicates && qtyVal > range) {
                Toast.makeText(this, "Quantity too high for non-duplicate limits", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Long> results = new ArrayList<>();
            if (allowDuplicates) {
                for (int i = 0; i < qtyVal; i++) {
                    long val = minVal + Math.abs(random.nextLong() % range);
                    results.add(val);
                }
            } else {
                if (range < 1000) {
                    List<Long> completePool = new ArrayList<>();
                    for (long i = minVal; i <= maxVal; i++) {
                        completePool.add(i);
                    }
                    Collections.shuffle(completePool, random);
                    for (int i = 0; i < qtyVal; i++) {
                        results.add(completePool.get(i));
                    }
                } else {
                    java.util.HashSet<Long> uniqueSet = new java.util.HashSet<>();
                    while (uniqueSet.size() < qtyVal) {
                        long val = minVal + Math.abs(random.nextLong() % range);
                        uniqueSet.add(val);
                    }
                    results.addAll(uniqueSet);
                }
            }

            // Build result output text
            StringBuilder resultBuilder = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                resultBuilder.append(results.get(i));
                if (i < results.size() - 1) {
                    resultBuilder.append(", ");
                }
            }
            String formattedResult = resultBuilder.toString();
            tvNumbersResult.setText(formattedResult);

            // Add to history panel
            numbersHistoryList.add(0, formattedResult + " (" + qtyVal + " numbers, range " + minVal + "-" + maxVal + ")");
            if (numbersHistoryList.size() > 5) {
                numbersHistoryList.remove(numbersHistoryList.size() - 1);
            }

            StringBuilder historyBuilder = new StringBuilder();
            for (String histItem : numbersHistoryList) {
                historyBuilder.append("• ").append(histItem).append("\n");
            }
            tvNumbersHistory.setText(historyBuilder.toString().trim());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Limit size overflow or wrong entry format", Toast.LENGTH_SHORT).show();
        }
    }

    private void initPasswordGenerator() {
        tvPwdLengthVal = (TextView) findViewById(R.id.tv_pwd_length_val);
        sbPasswordLength = (SeekBar) findViewById(R.id.sb_password_length);
        cbPwdUpper = (CheckBox) findViewById(R.id.cb_pwd_upper);
        cbPwdLower = (CheckBox) findViewById(R.id.cb_pwd_lower);
        cbPwdDigits = (CheckBox) findViewById(R.id.cb_pwd_digits);
        cbPwdSymbols = (CheckBox) findViewById(R.id.cb_pwd_symbols);
        btnGeneratePassword = (Button) findViewById(R.id.btn_generate_password);
        tvPasswordResult = (TextView) findViewById(R.id.tv_password_result);
        btnCopyPassword = (Button) findViewById(R.id.btn_copy_password);

        sbPasswordLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPasswordLength = progress + 6; // Offset minimum to 6 characters
                tvPwdLengthVal.setText(String.valueOf(currentPasswordLength));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set initial state
        sbPasswordLength.setProgress(6); // yields length 12
        tvPwdLengthVal.setText("12");

        btnGeneratePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateSecurePassword();
            }
        });

        btnCopyPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = tvPasswordResult.getText().toString();
                if (val.isEmpty()) return;

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Password", val);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(MainActivity.this, "Password copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateSecurePassword() {
        boolean hasUpper = cbPwdUpper.isChecked();
        boolean hasLower = cbPwdLower.isChecked();
        boolean hasDigits = cbPwdDigits.isChecked();
        boolean hasSymbols = cbPwdSymbols.isChecked();

        if (!hasUpper && !hasLower && !hasDigits && !hasSymbols) {
            Toast.makeText(this, "Please select at least one criteria option", Toast.LENGTH_SHORT).show();
            return;
        }

        String poolUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String poolLower = "abcdefghijklmnopqrstuvwxyz";
        String poolDigits = "0123456789";
        String poolSymbols = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        StringBuilder fullCharPool = new StringBuilder();
        List<Character> guaranteedChars = new ArrayList<>();

        if (hasUpper) {
            fullCharPool.append(poolUpper);
            guaranteedChars.add(poolUpper.charAt(secureRandom.nextInt(poolUpper.length())));
        }
        if (hasLower) {
            fullCharPool.append(poolLower);
            guaranteedChars.add(poolLower.charAt(secureRandom.nextInt(poolLower.length())));
        }
        if (hasDigits) {
            fullCharPool.append(poolDigits);
            guaranteedChars.add(poolDigits.charAt(secureRandom.nextInt(poolDigits.length())));
        }
        if (hasSymbols) {
            fullCharPool.append(poolSymbols);
            guaranteedChars.add(poolSymbols.charAt(secureRandom.nextInt(poolSymbols.length())));
        }

        String masterPool = fullCharPool.toString();
        StringBuilder passwordBuilder = new StringBuilder();

        // Fill remaining spaces
        int remainingLength = currentPasswordLength - guaranteedChars.size();
        for (int i = 0; i < remainingLength; i++) {
            passwordBuilder.append(masterPool.charAt(secureRandom.nextInt(masterPool.length())));
        }

        // Add guaranteed character sets
        for (Character c : guaranteedChars) {
            passwordBuilder.append(c);
        }

        // Shuffle password characters for security
        List<Character> passwordChars = new ArrayList<>();
        for (int i = 0; i < passwordBuilder.length(); i++) {
            passwordChars.add(passwordBuilder.charAt(i));
        }
        Collections.shuffle(passwordChars, secureRandom);

        StringBuilder finalizedPassword = new StringBuilder();
        for (Character c : passwordChars) {
            finalizedPassword.append(c);
        }

        tvPasswordResult.setText(finalizedPassword.toString());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}