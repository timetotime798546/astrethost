package com.randomgenerator.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    // Tab Navigation Buttons
    private Button btnTabNumber;
    private Button btnTabDice;
    private Button btnTabCoin;
    private Button btnTabHistory;

    // View Panels
    private ScrollView layoutNumber;
    private LinearLayout layoutDice;
    private LinearLayout layoutCoin;
    private LinearLayout layoutHistory;

    // Single Generator Elements
    private EditText etSingleMin;
    private EditText etSingleMax;
    private Button btnGenerateSingle;
    private TextView txtSingleResult;

    // Multiple Generator Elements
    private EditText etMultiMin;
    private EditText etMultiMax;
    private EditText etMultiCount;
    private CheckBox cbNoDuplicates;
    private Button btnGenerateMulti;
    private TextView txtMultiResult;

    // Dice Elements
    private RadioGroup rgDiceCount;
    private RadioButton rb1Die;
    private RadioButton rb2Dice;
    private RadioButton rb3Dice;
    private LinearLayout diceViewsContainer;
    private TextView txtDiceTotal;
    private Button btnRollDice;

    // Coin Elements
    private TextView coinView;
    private Button btnFlipCoin;

    // History Log Elements
    private TextView txtHistoryLog;
    private Button btnClearHistory;

    // System and Tracking Variables
    private final Random random = new Random();
    private final List<String> historyLogList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Navigation UI
        btnTabNumber = (Button) findViewById(R.id.btn_tab_number);
        btnTabDice = (Button) findViewById(R.id.btn_tab_dice);
        btnTabCoin = (Button) findViewById(R.id.btn_tab_coin);
        btnTabHistory = (Button) findViewById(R.id.btn_tab_history);

        // Bind Panels
        layoutNumber = (ScrollView) findViewById(R.id.layout_number);
        layoutDice = (LinearLayout) findViewById(R.id.layout_dice);
        layoutCoin = (LinearLayout) findViewById(R.id.layout_coin);
        layoutHistory = (LinearLayout) findViewById(R.id.layout_history);

        // Bind Single Generator
        etSingleMin = (EditText) findViewById(R.id.et_single_min);
        etSingleMax = (EditText) findViewById(R.id.et_single_max);
        btnGenerateSingle = (Button) findViewById(R.id.btn_generate_single);
        txtSingleResult = (TextView) findViewById(R.id.txt_single_result);

        // Bind Multiple Generator
        etMultiMin = (EditText) findViewById(R.id.et_multi_min);
        etMultiMax = (EditText) findViewById(R.id.et_multi_max);
        etMultiCount = (EditText) findViewById(R.id.et_multi_count);
        cbNoDuplicates = (CheckBox) findViewById(R.id.cb_no_duplicates);
        btnGenerateMulti = (Button) findViewById(R.id.btn_generate_multi);
        txtMultiResult = (TextView) findViewById(R.id.txt_multi_result);

        // Bind Dice Elements
        rgDiceCount = (RadioGroup) findViewById(R.id.rg_dice_count);
        rb1Die = (RadioButton) findViewById(R.id.rb_1_die);
        rb2Dice = (RadioButton) findViewById(R.id.rb_2_dice);
        rb3Dice = (RadioButton) findViewById(R.id.rb_3_dice);
        diceViewsContainer = (LinearLayout) findViewById(R.id.dice_views_container);
        txtDiceTotal = (TextView) findViewById(R.id.txt_dice_total);
        btnRollDice = (Button) findViewById(R.id.btn_roll_dice);

        // Bind Coin Elements
        coinView = (TextView) findViewById(R.id.coin_view);
        btnFlipCoin = (Button) findViewById(R.id.btn_flip_coin);

        // Bind History
        txtHistoryLog = (TextView) findViewById(R.id.txt_history_log);
        btnClearHistory = (Button) findViewById(R.id.btn_clear_history);

        // Setup Tab switching events
        btnTabNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(0);
            }
        });

        btnTabDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(1);
            }
        });

        btnTabCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(2);
            }
        });

        btnTabHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(3);
            }
        });

        // Initialize display content
        showTab(0);
        setupDiceContainer();

        // Single Number Generator Logic
        btnGenerateSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateSingleNumber();
            }
        });

        // Multiple Numbers Generator Logic
        btnGenerateMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateMultipleNumbers();
            }
        });

        // Dice Setup Changed Event
        rgDiceCount.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setupDiceContainer();
            }
        });

        // Dice Roll Logic
        btnRollDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDiceSimulation();
            }
        });

        // Coin Flip Logic
        btnFlipCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCoinSimulation();
            }
        });

        // Clear History Logic
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistoryLogs();
            }
        });
    }

    private void showTab(int index) {
        layoutNumber.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutDice.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutCoin.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        layoutHistory.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        updateTabStyle(btnTabNumber, index == 0);
        updateTabStyle(btnTabDice, index == 1);
        updateTabStyle(btnTabCoin, index == 2);
        updateTabStyle(btnTabHistory, index == 3);
    }

    private void updateTabStyle(Button btn, boolean isActive) {
        if (isActive) {
            btn.setBackgroundColor(Color.parseColor("#6200EE"));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundColor(Color.parseColor("#CBD5E1"));
            btn.setTextColor(Color.parseColor("#334155"));
        }
    }

    // --- 1. SINGLE NUMBER GENERATION ---
    private void generateSingleNumber() {
        int min;
        int max;
        try {
            min = Integer.parseInt(etSingleMin.getText().toString().trim());
            max = Integer.parseInt(etSingleMax.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid integers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (min >= max) {
            Toast.makeText(this, "Min must be strictly less than Max", Toast.LENGTH_SHORT).show();
            return;
        }

        int rangeLength = max - min + 1;
        if (rangeLength <= 0) {
            Toast.makeText(this, "Numeric range overflow", Toast.LENGTH_SHORT).show();
            return;
        }

        int result = random.nextInt(rangeLength) + min;

        // Apply visual bounce scale effect
        txtSingleResult.setScaleX(0.4f);
        txtSingleResult.setScaleY(0.4f);
        txtSingleResult.setText(String.valueOf(result));
        txtSingleResult.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(250)
                .start();

        addLog("Single Number (" + min + " to " + max + "): " + result);
    }

    // --- 2. MULTIPLE NUMBER GENERATION ---
    private void generateMultipleNumbers() {
        int min, max, count;
        try {
            min = Integer.parseInt(etMultiMin.getText().toString().trim());
            max = Integer.parseInt(etMultiMax.getText().toString().trim());
            count = Integer.parseInt(etMultiCount.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid integers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (min >= max) {
            Toast.makeText(this, "Min must be less than Max", Toast.LENGTH_SHORT).show();
            return;
        }

        if (count <= 0) {
            Toast.makeText(this, "Count must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        if (count > 500) {
            Toast.makeText(this, "Maximum capped count is 500", Toast.LENGTH_SHORT).show();
            return;
        }

        int range = max - min + 1;
        boolean preventDuplicates = cbNoDuplicates.isChecked();

        if (preventDuplicates && count > range) {
            Toast.makeText(this, "Cannot generate unique numbers: count exceeds range size", Toast.LENGTH_LONG).show();
            return;
        }

        List<Integer> list = new ArrayList<>();
        if (preventDuplicates) {
            Set<Integer> uniqueSet = new HashSet<>();
            while (uniqueSet.size() < count) {
                uniqueSet.add(random.nextInt(range) + min);
            }
            list.addAll(uniqueSet);
            Collections.sort(list); // Sort unique set naturally for elegant output representation
        } else {
            for (int i = 0; i < count; i++) {
                list.add(random.nextInt(range) + min);
            }
        }

        // Format outputs nicely
        StringBuilder outputBuilder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            outputBuilder.append(list.get(i));
            if (i < list.size() - 1) {
                outputBuilder.append(", ");
            }
        }

        String outputStr = outputBuilder.toString();
        txtMultiResult.setText(outputStr);
        addLog("Multiple [" + count + " numbers] (" + min + " to " + max + "): " + outputStr);
    }

    // --- 3. DICE ROLLING SIMULATOR ---
    private void setupDiceContainer() {
        diceViewsContainer.removeAllViews();
        int count = 1;
        if (rb2Dice.isChecked()) {
            count = 2;
        } else if (rb3Dice.isChecked()) {
            count = 3;
        }

        for (int i = 0; i < count; i++) {
            DieView dv = new DieView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(85),
                    dpToPx(85)
            );
            params.setMargins(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
            dv.setLayoutParams(params);
            dv.setValue(1); // Set base initial state
            diceViewsContainer.addView(dv);
        }
        txtDiceTotal.setText("Total Sum: " + count);
    }

    private void rollDiceSimulation() {
        final int childCount = diceViewsContainer.getChildCount();
        if (childCount == 0) return;

        final List<DieView> dieViews = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View child = diceViewsContainer.getChildAt(i);
            if (child instanceof DieView) {
                dieViews.add((DieView) child);
            }
        }

        final Handler rollHandler = new Handler();
        final long startTime = System.currentTimeMillis();
        final int runDuration = 900; // Simulated rotation duration
        final int stepInterval = 90;

        btnRollDice.setEnabled(false);

        final Runnable rollRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed < runDuration) {
                    // Randomize intermediate visual displays
                    for (DieView dv : dieViews) {
                        dv.setValue(random.nextInt(6) + 1);
                    }
                    rollHandler.postDelayed(this, stepInterval);
                } else {
                    // Lock-in stable calculation states
                    int sum = 0;
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("Dice Rolled [").append(dieViews.size()).append("d6]: ");

                    for (int i = 0; i < dieViews.size(); i++) {
                        int finalVal = random.nextInt(6) + 1;
                        dieViews.get(i).setValue(finalVal);
                        sum += finalVal;
                        logBuilder.append(finalVal);
                        if (i < dieViews.size() - 1) {
                            logBuilder.append(" + ");
                        }
                    }

                    txtDiceTotal.setText("Total Sum: " + sum);
                    btnRollDice.setEnabled(true);

                    logBuilder.append(" = ").append(sum);
                    addLog(logBuilder.toString());
                }
            }
        };

        rollHandler.post(rollRunnable);
    }

    // --- 4. COIN FLIP SIMULATOR ---
    private void flipCoinSimulation() {
        btnFlipCoin.setEnabled(false);

        // Standard 3D Y-axis rotation visual animation
        final ObjectAnimator flipAnimator = ObjectAnimator.ofFloat(coinView, "rotationY", 0f, 1440f);
        flipAnimator.setDuration(750);
        flipAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                btnFlipCoin.setEnabled(true);
                coinView.setRotationY(0f); // Reset standard visual alignment state

                boolean isHeads = random.nextBoolean();
                String sideResult = isHeads ? "HEADS" : "TAILS";
                coinView.setText(sideResult);

                if (isHeads) {
                    coinView.setBackgroundResource(R.drawable.coin_gold_bg);
                    coinView.setTextColor(Color.parseColor("#7A5900"));
                } else {
                    coinView.setBackgroundResource(R.drawable.coin_silver_bg);
                    coinView.setTextColor(Color.parseColor("#334155"));
                }

                addLog("Coin Flipped: " + sideResult);
            }
        });

        flipAnimator.start();
    }

    // --- 5. LOGGING HISTORY ---
    private void addLog(String detail) {
        String currentTime = timeFormat.format(new Date());
        historyLogList.add(0, "[" + currentTime + "] " + detail); // Prepend to show latest on top

        // Cap history layout memory to avoid rendering slowdowns
        if (historyLogList.size() > 100) {
            historyLogList.remove(historyLogList.size() - 1);
        }

        updateLogDisplay();
    }

    private void updateLogDisplay() {
        if (historyLogList.isEmpty()) {
            txtHistoryLog.setText("History is empty...");
            return;
        }

        StringBuilder combined = new StringBuilder();
        for (String record : historyLogList) {
            combined.append(record).append("\n\n");
        }
        txtHistoryLog.setText(combined.toString());
    }

    private void clearHistoryLogs() {
        historyLogList.clear();
        updateLogDisplay();
        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
    }

    // --- HELPER METRIC UTILITY ---
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    // --- CUSTOM DYNAMIC VECTOR DIE VIEW ---
    public static class DieView extends View {
        private int value = 1;
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF layoutRect = new RectF();

        public DieView(Context context) {
            super(context);
            init();
        }

        public DieView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            bgPaint.setColor(Color.WHITE);
            bgPaint.setStyle(Paint.Style.FILL);

            outlinePaint.setColor(Color.parseColor("#94A3B8"));
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(6f);

            pointPaint.setColor(Color.parseColor("#1E293B"));
            pointPaint.setStyle(Paint.Style.FILL);
        }

        public void setValue(int value) {
            if (value >= 1 && value <= 6) {
                this.value = value;
                invalidate();
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Keep View completely square to maintain visual circle alignment
            int finalWidth = MeasureSpec.getSize(widthMeasureSpec);
            if (finalWidth <= 0) finalWidth = 150;
            setMeasuredDimension(finalWidth, finalWidth);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float currentWidth = getWidth();
            float currentHeight = getHeight();

            float boundaryOffset = 8f;
            layoutRect.set(boundaryOffset, boundaryOffset, currentWidth - boundaryOffset, currentHeight - boundaryOffset);
            float roundingFactor = currentWidth * 0.18f;

            // Draw Base Card
            canvas.drawRoundRect(layoutRect, roundingFactor, roundingFactor, bgPaint);
            canvas.drawRoundRect(layoutRect, roundingFactor, roundingFactor, outlinePaint);

            float cx = currentWidth / 2f;
            float cy = currentHeight / 2f;
            float dotRadius = currentWidth * 0.082f;

            // Coordinate mappings for 6-dot matrix
            float leftCol = currentWidth * 0.28f;
            float rightCol = currentWidth * 0.72f;
            float topRow = currentHeight * 0.28f;
            float bottomRow = currentHeight * 0.72f;

            switch (value) {
                case 1:
                    canvas.drawCircle(cx, cy, dotRadius, pointPaint);
                    break;
                case 2:
                    canvas.drawCircle(leftCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, bottomRow, dotRadius, pointPaint);
                    break;
                case 3:
                    canvas.drawCircle(leftCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(cx, cy, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, bottomRow, dotRadius, pointPaint);
                    break;
                case 4:
                    canvas.drawCircle(leftCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(leftCol, bottomRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, bottomRow, dotRadius, pointPaint);
                    break;
                case 5:
                    canvas.drawCircle(leftCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(cx, cy, dotRadius, pointPaint);
                    canvas.drawCircle(leftCol, bottomRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, bottomRow, dotRadius, pointPaint);
                    break;
                case 6:
                    canvas.drawCircle(leftCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, topRow, dotRadius, pointPaint);
                    canvas.drawCircle(leftCol, cy, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, cy, dotRadius, pointPaint);
                    canvas.drawCircle(leftCol, bottomRow, dotRadius, pointPaint);
                    canvas.drawCircle(rightCol, bottomRow, dotRadius, pointPaint);
                    break;
            }
        }
    }
}