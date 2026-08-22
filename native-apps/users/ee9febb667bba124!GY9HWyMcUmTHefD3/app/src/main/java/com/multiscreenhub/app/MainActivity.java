package com.multiscreenhub.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends Activity {

    private FrameLayout contentContainer;
    private Button btnBack;
    private TextView txtAppTitle;
    private LinearLayout topBar;
    private SharedPreferences prefs;

    // Screen State Tracker: 0 = Dashboard, 1 to 19 = Screens 2-20
    private int currentScreen = 0;

    // Title tracking
    private static final String[] SCREEN_TITLES = {
        "Dashboard Hub",
        "1. Profile Creator",
        "2. Word & Text Counter",
        "3. Math Calculator",
        "4. Random Quote Board",
        "5. BMI & BMR Fitness",
        "6. Notes Keeper",
        "7. Digital Dice Roller",
        "8. RGB Color Mixer",
        "9. Stopwatch Tracker",
        "10. Multi-Unit Converter",
        "11. Tip Calculator",
        "12. Currency Estimator",
        "13. Decision Maker 8-Ball",
        "14. Tic-Tac-Toe Game",
        "15. Click Speed Test",
        "16. Metronome Visualizer",
        "17. Date Calculator",
        "18. Drawing Paint Board",
        "19. System Settings & Info"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        prefs = getSharedPreferences("MultiHubPrefs", MODE_PRIVATE);
        contentContainer = findViewById(getResources().getIdentifier("contentContainer", "id", getPackageName()));
        btnBack = findViewById(getResources().getIdentifier("btnBack", "id", getPackageName()));
        txtAppTitle = findViewById(getResources().getIdentifier("txtAppTitle", "id", getPackageName()));
        topBar = findViewById(getResources().getIdentifier("topBar", "id", getPackageName()));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        applyHeaderColor();
        loadScreen(0);
    }

    private void applyHeaderColor() {
        int savedColor = prefs.getInt("theme_color", 0xFF1E3799);
        topBar.setBackgroundColor(savedColor);
    }

    private void loadScreen(int index) {
        currentScreen = index;
        txtAppTitle.setText(SCREEN_TITLES[index]);
        btnBack.setVisibility(index == 0 ? View.GONE : View.VISIBLE);

        contentContainer.removeAllViews();
        
        View viewToLoad;
        if (index == 0) {
            viewToLoad = buildDashboardScreen();
        } else {
            viewToLoad = buildScreenByIndex(index);
        }

        contentContainer.addView(viewToLoad);
    }

    @Override
    public void onBackPressed() {
        if (currentScreen > 0) {
            loadScreen(0);
        } else {
            super.onBackPressed();
        }
    }

    // Helper styling utilities
    private LinearLayout createVerticalContainer() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private TextView createHeading(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20);
        tv.setTextColor(0xFF2C3E50);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 8, 0, 16);
        return tv;
    }

    private EditText createInput(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setPadding(24, 24, 24, 24);
        et.setTextSize(16);
        et.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 16);
        et.setLayoutParams(lp);
        return et;
    }

    private Button createStandardButton(String text, final View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(0xFF3498DB);
        btn.setTextSize(16);
        btn.setPadding(16, 24, 16, 24);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 12);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    private View buildDashboardScreen() {
        ScrollView sv = new ScrollView(this);
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(24, 24, 24, 24);

        TextView introText = new TextView(this);
        introText.setText("Welcome to Mega Hub 20!\nExplore 20 fully-functional screens right inside this lightweight layout module. Choose a tool below:");
        introText.setTextSize(15);
        introText.setTextColor(0xFF57606F);
        introText.setPadding(0, 8, 0, 24);
        introText.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContainer.addView(introText);

        // Create Grid Buttons dynamically
        for (int i = 1; i < SCREEN_TITLES.length; i++) {
            final int targetIndex = i;
            Button btn = new Button(this);
            btn.setText(SCREEN_TITLES[i]);
            btn.setAllCaps(false);
            btn.setTextColor(0xFF2C3E50);
            btn.setBackgroundColor(0xFFE8ECEF);
            btn.setTextSize(15);
            btn.setPadding(16, 20, 16, 20);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 6, 0, 6);
            btn.setLayoutParams(lp);
            
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadScreen(targetIndex);
                }
            });
            mainContainer.addView(btn);
        }

        sv.addView(mainContainer);
        return sv;
    }

    private View buildScreenByIndex(int index) {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = createVerticalContainer();
        
        switch(index) {
            case 1:
                buildProfileCreator(container);
                break;
            case 2:
                buildTextAnalyzer(container);
                break;
            case 3:
                buildMathCalculator(container);
                break;
            case 4:
                buildQuoteBoard(container);
                break;
            case 5:
                buildBMICalculator(container);
                break;
            case 6:
                buildNotesKeeper(container);
                break;
            case 7:
                buildDiceRoller(container);
                break;
            case 8:
                buildColorMixer(container);
                break;
            case 9:
                buildStopwatch(container);
                break;
            case 10:
                buildUnitConverter(container);
                break;
            case 11:
                buildTipCalculator(container);
                break;
            case 12:
                buildCurrencyEstimator(container);
                break;
            case 13:
                buildEightBall(container);
                break;
            case 14:
                buildTicTacToe(container);
                break;
            case 15:
                buildTapSpeed(container);
                break;
            case 16:
                buildMetronome(container);
                break;
            case 17:
                buildDateCalc(container);
                break;
            case 18:
                // Custom drawing view - bypass ScrollView wrapper to allow proper paint events
                return buildDrawingBoard();
            case 19:
                buildSettings(container);
                break;
            default:
                TextView tv = new TextView(this);
                tv.setText("Unsupported Screen");
                container.addView(tv);
                break;
        }
        
        sv.addView(container);
        return sv;
    }

    // SCREEN 1: Profile Creator
    private void buildProfileCreator(final LinearLayout container) {
        container.addView(createHeading("Save Profile Details"));

        final EditText etName = createInput("Name", android.text.InputType.TYPE_CLASS_TEXT);
        final EditText etEmail = createInput("Email", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText etBio = createInput("Short Bio", android.text.InputType.TYPE_CLASS_TEXT);

        etName.setText(prefs.getString("prof_name", ""));
        etEmail.setText(prefs.getString("prof_email", ""));
        etBio.setText(prefs.getString("prof_bio", ""));

        container.addView(etName);
        container.addView(etEmail);
        container.addView(etBio);

        Button btnSave = createStandardButton("Save Information", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit()
                    .putString("prof_name", etName.getText().toString())
                    .putString("prof_email", etEmail.getText().toString())
                    .putString("prof_bio", etBio.getText().toString())
                    .apply();
                Toast.makeText(MainActivity.this, "Profile successfully saved!", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnSave);
    }

    // SCREEN 2: Word & Text Counter
    private void buildTextAnalyzer(LinearLayout container) {
        container.addView(createHeading("Text Analytics Engine"));

        final EditText etArea = new EditText(this);
        etArea.setHint("Begin entering your paragraph text here...");
        etArea.setGravity(Gravity.TOP);
        etArea.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etArea.setMinLines(5);
        etArea.setPadding(20, 20, 20, 20);
        etArea.setBackgroundColor(Color.WHITE);
        container.addView(etArea);

        final TextView tvStats = new TextView(this);
        tvStats.setText("Characters: 0 | Words: 0");
        tvStats.setPadding(0, 16, 0, 16);
        tvStats.setTextSize(16);
        tvStats.setTypeface(null, Typeface.BOLD);
        container.addView(tvStats);

        etArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int chars = s.length();
                String raw = s.toString().trim();
                int words = raw.isEmpty() ? 0 : raw.split("\\s+").length;
                tvStats.setText("Characters: " + chars + " | Words: " + words);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        container.addView(createStandardButton("Convert to UPPERCASE", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etArea.setText(etArea.getText().toString().toUpperCase());
            }
        }));
    }

    // SCREEN 3: Math Calculator
    private String lastOp = "";
    private double calcAccumulator = 0;
    private boolean startNewNumber = true;
    private void buildMathCalculator(LinearLayout container) {
        container.addView(createHeading("Arithmetic Calculator"));

        final TextView tvDisplay = new TextView(this);
        tvDisplay.setText("0");
        tvDisplay.setTextSize(32);
        tvDisplay.setGravity(Gravity.RIGHT);
        tvDisplay.setPadding(16, 32, 16, 32);
        tvDisplay.setBackgroundColor(Color.BLACK);
        tvDisplay.setTextColor(Color.GREEN);
        container.addView(tvDisplay);

        LinearLayout gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);
        gridLayout.setPadding(0, 16, 0, 0);

        String[][] keys = {
            {"7", "8", "9", "/"},
            {"4", "5", "6", "*"},
            {"1", "2", "3", "-"},
            {"C", "0", "=", "+"}
        };

        for (int r = 0; r < 4; r++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 4; c++) {
                final String key = keys[r][c];
                Button b = new Button(this);
                b.setText(key);
                b.setTextSize(20);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                lp.setMargins(4, 4, 4, 4);
                b.setLayoutParams(lp);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Character.isDigit(key.charAt(0))) {
                            if (startNewNumber || tvDisplay.getText().toString().equals("0")) {
                                tvDisplay.setText(key);
                                startNewNumber = false;
                            } else {
                                tvDisplay.append(key);
                            }
                        } else if (key.equals("C")) {
                            tvDisplay.setText("0");
                            calcAccumulator = 0;
                            lastOp = "";
                            startNewNumber = true;
                        } else if (key.equals("=")) {
                            double secondVal = Double.parseDouble(tvDisplay.getText().toString());
                            double result = calcAccumulator;
                            if (lastOp.equals("+")) result += secondVal;
                            else if (lastOp.equals("-")) result -= secondVal;
                            else if (lastOp.equals("*")) result *= secondVal;
                            else if (lastOp.equals("/") && secondVal != 0) result /= secondVal;
                            tvDisplay.setText(String.valueOf(result));
                            calcAccumulator = result;
                            startNewNumber = true;
                        } else {
                            lastOp = key;
                            calcAccumulator = Double.parseDouble(tvDisplay.getText().toString());
                            startNewNumber = true;
                        }
                    }
                });
                rowLayout.addView(b);
            }
            gridLayout.addView(rowLayout);
        }
        container.addView(gridLayout);
    }

    // SCREEN 4: Quote Board
    private void buildQuoteBoard(LinearLayout container) {
        container.addView(createHeading("Daily Quote Board"));

        final String[] quotes = {
            "\"The only way to do great work is to love what you do.\" - Steve Jobs",
            "\"The best way to predict the future is to create it.\" - Peter Drucker",
            "\"Simple can be harder than complex: You have to work hard to get your thinking clean to make it simple.\" - Steve Jobs",
            "\"Do not wait; the time will never be 'just right.'\" - Napoleon Hill",
            "\"Believe you can and you're halfway there.\" - Theodore Roosevelt"
        };

        final TextView tvQuote = new TextView(this);
        tvQuote.setText(quotes[0]);
        tvQuote.setTextSize(18);
        tvQuote.setTypeface(null, Typeface.ITALIC);
        tvQuote.setPadding(16, 32, 16, 32);
        tvQuote.setGravity(Gravity.CENTER);
        container.addView(tvQuote);

        container.addView(createStandardButton("Generate Next Quote", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int idx = new Random().nextInt(quotes.length);
                tvQuote.setText(quotes[idx]);
            }
        }));
    }

    // SCREEN 5: BMI & BMR Fitness Calculator
    private void buildBMICalculator(LinearLayout container) {
        container.addView(createHeading("Fitness BMI Calculator"));

        final EditText etWeight = createInput("Weight (kg)", android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText etHeight = createInput("Height (cm)", android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(etWeight);
        container.addView(etHeight);

        final TextView tvResult = new TextView(this);
        tvResult.setTextSize(16);
        tvResult.setPadding(0, 16, 0, 16);
        container.addView(tvResult);

        container.addView(createStandardButton("Calculate Fitness", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double w = Double.parseDouble(etWeight.getText().toString());
                    double h = Double.parseDouble(etHeight.getText().toString()) / 100.0;
                    double bmi = w / (h * h);
                    String status = bmi < 18.5 ? "Underweight" : (bmi < 25 ? "Normal" : "Overweight");
                    tvResult.setText(String.format("BMI Score: %.2f\nClassification: %s", bmi, status));
                } catch(Exception ex) {
                    Toast.makeText(MainActivity.this, "Enter valid numbers!", Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    // SCREEN 6: Notes Keeper
    private void buildNotesKeeper(final LinearLayout container) {
        container.addView(createHeading("Notes Notebook"));

        final EditText etNote = createInput("Type private note here...", android.text.InputType.TYPE_CLASS_TEXT);
        container.addView(etNote);

        final LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        final Runnable refreshNotes = new Runnable() {
            @Override
            public void run() {
                listLayout.removeAllViews();
                String notesRaw = prefs.getString("app_notes_raw", "");
                if(!notesRaw.trim().isEmpty()) {
                    String[] split = notesRaw.split("##NOTE_SPLIT##");
                    for(int i = 0; i < split.length; i++) {
                        final int index = i;
                        final String currentNote = split[i];
                        LinearLayout item = new LinearLayout(MainActivity.this);
                        item.setOrientation(LinearLayout.HORIZONTAL);
                        item.setPadding(8, 8, 8, 8);

                        TextView tv = new TextView(MainActivity.this);
                        tv.setText(currentNote);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                        item.addView(tv);

                        Button bDel = new Button(MainActivity.this);
                        bDel.setText("X");
                        bDel.setTextColor(Color.RED);
                        bDel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String oldStr = prefs.getString("app_notes_raw", "");
                                String[] original = oldStr.split("##NOTE_SPLIT##");
                                StringBuilder sb = new StringBuilder();
                                for(int k = 0; k < original.length; k++) {
                                    if(k != index) {
                                        if(sb.length() > 0) sb.append("##NOTE_SPLIT##");
                                        sb.append(original[k]);
                                    }
                                }
                                prefs.edit().putString("app_notes_raw", sb.toString()).apply();
                                loadScreen(6); // Reload notes list
                            }
                        });
                        item.addView(bDel);
                        listLayout.addView(item);
                    }
                }
            }
        };

        container.addView(createStandardButton("Save Note Entry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = etNote.getText().toString().trim();
                if(!txt.isEmpty()) {
                    String existing = prefs.getString("app_notes_raw", "");
                    if(!existing.isEmpty()) existing += "##NOTE_SPLIT##";
                    existing += txt;
                    prefs.edit().putString("app_notes_raw", existing).apply();
                    etNote.setText("");
                    refreshNotes.run();
                }
            }
        }));

        container.addView(listLayout);
        refreshNotes.run();
    }

    // SCREEN 7: Digital Dice Roller
    private void buildDiceRoller(LinearLayout container) {
        container.addView(createHeading("Interactive Dice Roller"));

        final TextView tvDice = new TextView(this);
        tvDice.setText("⚅");
        tvDice.setTextSize(90);
        tvDice.setGravity(Gravity.CENTER);
        tvDice.setPadding(0, 16, 0, 16);
        container.addView(tvDice);

        container.addView(createStandardButton("Roll Dice", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = new Random().nextInt(6) + 1;
                String icon = "⚀";
                if (num == 2) icon = "⚁";
                else if (num == 3) icon = "⚂";
                else if (num == 4) icon = "⚃";
                else if (num == 5) icon = "⚄";
                else if (num == 6) icon = "⚅";
                tvDice.setText(icon);
            }
        }));
    }

    // SCREEN 8: RGB Color Mixer
    private void buildColorMixer(LinearLayout container) {
        container.addView(createHeading("Interactive RGB Mixer"));

        final View colorPanel = new View(this);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 250);
        vlp.setMargins(0, 16, 0, 32);
        colorPanel.setLayoutParams(vlp);
        colorPanel.setBackgroundColor(Color.BLACK);
        container.addView(colorPanel);

        final TextView tvHex = new TextView(this);
        tvHex.setText("Color Hex: #000000");
        tvHex.setTextSize(16);
        tvHex.setPadding(0, 0, 0, 16);
        container.addView(tvHex);

        final SeekBar rSeek = new SeekBar(this);
        final SeekBar gSeek = new SeekBar(this);
        final SeekBar bSeek = new SeekBar(this);
        
        rSeek.setMax(255); gSeek.setMax(255); bSeek.setMax(255);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = rSeek.getProgress();
                int g = gSeek.getProgress();
                int b = bSeek.getProgress();
                colorPanel.setBackgroundColor(Color.rgb(r, g, b));
                tvHex.setText(String.format("Color Hex: #%02X%02X%02X", r, g, b));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        rSeek.setOnSeekBarChangeListener(listener);
        gSeek.setOnSeekBarChangeListener(listener);
        bSeek.setOnSeekBarChangeListener(listener);

        container.addView(new TextView(this) {{ setText("Red Slider"); }});
        container.addView(rSeek);
        container.addView(new TextView(this) {{ setText("Green Slider"); }});
        container.addView(gSeek);
        container.addView(new TextView(this) {{ setText("Blue Slider"); }});
        container.addView(bSeek);
    }

    // SCREEN 9: Stopwatch Tracker
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private boolean isTimerRunning = false;
    private TextView tvStopwatch;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                long millis = System.currentTimeMillis() - startTime;
                int secs = (int) (millis / 1000);
                int mins = secs / 60;
                secs = secs % 60;
                int fraction = (int) ((millis % 1000) / 100);
                if (tvStopwatch != null) {
                    tvStopwatch.setText(String.format("%02d:%02d.%d", mins, secs, fraction));
                }
                timerHandler.postDelayed(this, 100);
            }
        }
    };

    private void buildStopwatch(LinearLayout container) {
        container.addView(createHeading("Interactive Stopwatch"));

        tvStopwatch = new TextView(this);
        tvStopwatch.setText("00:00.0");
        tvStopwatch.setTextSize(48);
        tvStopwatch.setGravity(Gravity.CENTER);
        tvStopwatch.setPadding(0, 32, 0, 32);
        container.addView(tvStopwatch);

        container.addView(createStandardButton("Start Timer", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isTimerRunning) {
                    isTimerRunning = true;
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                }
            }
        }));

        container.addView(createStandardButton("Pause Timer", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTimerRunning = false;
            }
        }));
    }

    // SCREEN 10: Multi-Unit Converter
    private void buildUnitConverter(LinearLayout container) {
        container.addView(createHeading("Conversion Hub (Meters <-> Feet)"));

        final EditText etInputVal = createInput("Input value", android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(etInputVal);

        final TextView tvConvertOutput = new TextView(this);
        tvConvertOutput.setTextSize(16);
        tvConvertOutput.setPadding(0, 16, 0, 16);
        container.addView(tvConvertOutput);

        container.addView(createStandardButton("Convert Meters to Feet", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double m = Double.parseDouble(etInputVal.getText().toString());
                    tvConvertOutput.setText(String.format("%.2f Meters = %.2f Feet", m, m * 3.28084));
                } catch(Exception ex) {}
            }
        }));

        container.addView(createStandardButton("Convert Feet to Meters", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double f = Double.parseDouble(etInputVal.getText().toString());
                    tvConvertOutput.setText(String.format("%.2f Feet = %.2f Meters", f, f / 3.28084));
                } catch(Exception ex) {}
            }
        }));
    }

    // SCREEN 11: Tip Calculator
    private void buildTipCalculator(LinearLayout container) {
        container.addView(createHeading("Gratuity & Tip Calculator"));

        final EditText etBill = createInput("Bill Amount ($)", android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(etBill);

        final TextView tvTipStats = new TextView(this);
        tvTipStats.setText("Tip: 15%");
        tvTipStats.setTextSize(14);
        container.addView(tvTipStats);

        final SeekBar seekTip = new SeekBar(this);
        seekTip.setMax(30);
        seekTip.setProgress(15);
        container.addView(seekTip);

        final TextView tvResultTip = new TextView(this);
        tvResultTip.setTextSize(16);
        tvResultTip.setPadding(0, 16, 0, 16);
        container.addView(tvResultTip);

        seekTip.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTipStats.setText("Tip: " + progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        container.addView(createStandardButton("Calculate Total Split", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double bill = Double.parseDouble(etBill.getText().toString());
                    double pct = seekTip.getProgress() / 100.0;
                    double tip = bill * pct;
                    tvResultTip.setText(String.format("Tip Amount: $%.2f\nTotal Bill: $%.2f", tip, bill + tip));
                } catch(Exception ex) {}
            }
        }));
    }

    // SCREEN 12: Currency Estimator
    private void buildCurrencyEstimator(LinearLayout container) {
        container.addView(createHeading("Exchange Estimator (Base USD)"));

        final EditText etUsdVal = createInput("Amount in USD ($)", android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(etUsdVal);

        final TextView tvCurrencyOut = new TextView(this);
        tvCurrencyOut.setTextSize(16);
        tvCurrencyOut.setPadding(0, 16, 0, 16);
        container.addView(tvCurrencyOut);

        container.addView(createStandardButton("Estimate to EUR (€)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double val = Double.parseDouble(etUsdVal.getText().toString());
                    tvCurrencyOut.setText(String.format("$%.2f USD = €%.2f EUR (Rate: 0.92)", val, val * 0.92));
                } catch (Exception ex) {}
            }
        }));

        container.addView(createStandardButton("Estimate to INR (₹)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double val = Double.parseDouble(etUsdVal.getText().toString());
                    tvCurrencyOut.setText(String.format("$%.2f USD = ₹%.2f INR (Rate: 83.0)", val, val * 83.0));
                } catch (Exception ex) {}
            }
        }));
    }

    // SCREEN 13: Decision Maker 8-Ball
    private void buildEightBall(LinearLayout container) {
        container.addView(createHeading("Magic 8-Ball Decision Maker"));

        final EditText etQuestion = createInput("Ask a question...", android.text.InputType.TYPE_CLASS_TEXT);
        container.addView(etQuestion);

        final TextView tvResponse = new TextView(this);
        tvResponse.setText("Shake the 8-Ball for an answer.");
        tvResponse.setTextSize(18);
        tvResponse.setPadding(0, 32, 0, 32);
        tvResponse.setGravity(Gravity.CENTER);
        container.addView(tvResponse);

        final String[] answers = {
            "It is certain.", "Reply hazy, try again.", "Don't count on it.",
            "My sources say no.", "Outlook good.", "Signs point to yes."
        };

        container.addView(createStandardButton("Shake 8-Ball", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!etQuestion.getText().toString().trim().isEmpty()) {
                    int idx = new Random().nextInt(answers.length);
                    tvResponse.setText(answers[idx]);
                } else {
                    Toast.makeText(MainActivity.this, "Ask a question first!", Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    // SCREEN 14: Tic-Tac-Toe Game
    private boolean isPlayerX = true;
    private Button[] tttButtons = new Button[9];
    private void buildTicTacToe(LinearLayout container) {
        container.addView(createHeading("Tic-Tac-Toe Battle"));

        final TextView tvTurn = new TextView(this);
        tvTurn.setText("Turn: Player X");
        tvTurn.setTextSize(16);
        tvTurn.setPadding(0, 0, 0, 16);
        container.addView(tvTurn);

        LinearLayout gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);

        final Runnable checkWinner = new Runnable() {
            @Override
            public void run() {
                int[][] wins = {
                    {0,1,2}, {3,4,5}, {6,7,8},
                    {0,3,6}, {1,4,7}, {2,5,8},
                    {0,4,8}, {2,4,6}
                };
                for (int[] w : wins) {
                    String b1 = tttButtons[w[0]].getText().toString();
                    String b2 = tttButtons[w[1]].getText().toString();
                    String b3 = tttButtons[w[2]].getText().toString();
                    if (!b1.isEmpty() && b1.equals(b2) && b2.equals(b3)) {
                        tvTurn.setText(b1 + " Wins the Game!");
                        for (Button b : tttButtons) b.setEnabled(false);
                        return;
                    }
                }
            }
        };

        for (int r = 0; r < 3; r++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 3; c++) {
                final int idx = r * 3 + c;
                final Button b = new Button(this);
                b.setText("");
                b.setTextSize(24);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                lp.setMargins(4, 4, 4, 4);
                b.setLayoutParams(lp);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (b.getText().toString().isEmpty()) {
                            b.setText(isPlayerX ? "X" : "O");
                            isPlayerX = !isPlayerX;
                            tvTurn.setText("Turn: Player " + (isPlayerX ? "X" : "O"));
                            checkWinner.run();
                        }
                    }
                });
                tttButtons[idx] = b;
                rowLayout.addView(b);
            }
            gridLayout.addView(rowLayout);
        }

        container.addView(gridLayout);
        container.addView(createStandardButton("Reset Game", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Button b : tttButtons) {
                    b.setText("");
                    b.setEnabled(true);
                }
                isPlayerX = true;
                tvTurn.setText("Turn: Player X");
            }
        }));
    }

    // SCREEN 15: Click Speed Test
    private int clickCount = 0;
    private boolean isTestRunning = false;
    private void buildTapSpeed(LinearLayout container) {
        container.addView(createHeading("Tap Speed Challenge"));

        final TextView tvTaps = new TextView(this);
        tvTaps.setText("Clicks: 0");
        tvTaps.setTextSize(24);
        tvTaps.setPadding(0, 16, 0, 16);
        container.addView(tvTaps);

        final Button btnTap = createStandardButton("START / TAP!", null);
        container.addView(btnTap);

        btnTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isTestRunning) {
                    isTestRunning = true;
                    clickCount = 1;
                    tvTaps.setText("Clicks: 1");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isTestRunning = false;
                            btnTap.setEnabled(false);
                            Toast.makeText(MainActivity.this, "Finished! Total clicks: " + clickCount, Toast.LENGTH_LONG).show();
                        }
                    }, 5000);
                } else {
                    clickCount++;
                    tvTaps.setText("Clicks: " + clickCount);
                }
            }
        });

        container.addView(createStandardButton("Reset Challenge", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCount = 0;
                isTestRunning = false;
                btnTap.setEnabled(true);
                tvTaps.setText("Clicks: 0");
            }
        }));
    }

    // SCREEN 16: Metronome Visualizer
    private int metronomeBpm = 100;
    private boolean isMetronomeRunning = false;
    private Handler metronomeHandler = new Handler();
    private View metronomeLight;
    private boolean metronomeState = false;
    private Runnable metronomeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMetronomeRunning) {
                metronomeState = !metronomeState;
                if (metronomeLight != null) {
                    metronomeLight.setBackgroundColor(metronomeState ? Color.GREEN : Color.LTGRAY);
                }
                long delay = (60 * 1000) / metronomeBpm;
                metronomeHandler.postDelayed(this, delay);
            }
        }
    };

    private void buildMetronome(LinearLayout container) {
        container.addView(createHeading("Tempo Metronome visualizer"));

        metronomeLight = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        lp.setMargins(0, 16, 0, 32);
        metronomeLight.setLayoutParams(lp);
        metronomeLight.setBackgroundColor(Color.LTGRAY);
        container.addView(metronomeLight);

        final TextView tvBpm = new TextView(this);
        tvBpm.setText("BPM: " + metronomeBpm);
        tvBpm.setTextSize(16);
        container.addView(tvBpm);

        SeekBar seekBpm = new SeekBar(this);
        seekBpm.setMax(200);
        seekBpm.setProgress(metronomeBpm - 40);
        seekBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                metronomeBpm = progress + 40;
                tvBpm.setText("BPM: " + metronomeBpm);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(seekBpm);

        container.addView(createStandardButton("Start Metronome", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isMetronomeRunning) {
                    isMetronomeRunning = true;
                    metronomeHandler.postDelayed(metronomeRunnable, 0);
                }
            }
        }));

        container.addView(createStandardButton("Stop Metronome", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMetronomeRunning = false;
                if(metronomeLight != null) {
                    metronomeLight.setBackgroundColor(Color.LTGRAY);
                }
            }
        }));
    }

    // SCREEN 17: Date Calculator
    private void buildDateCalc(LinearLayout container) {
        container.addView(createHeading("Target Days Calculator"));

        final DatePicker dp = new DatePicker(this);
        dp.setCalendarViewShown(false);
        container.addView(dp);

        final TextView tvDateRes = new TextView(this);
        tvDateRes.setTextSize(16);
        tvDateRes.setPadding(0, 16, 0, 16);
        container.addView(tvDateRes);

        container.addView(createStandardButton("Calculate Days Remaining", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar selected = Calendar.getInstance();
                selected.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                Calendar today = Calendar.getInstance();
                long diff = selected.getTimeInMillis() - today.getTimeInMillis();
                long days = diff / (24 * 60 * 60 * 1000);
                tvDateRes.setText("Days difference from today: " + days + " days");
            }
        }));
    }

    // SCREEN 18: Drawing Paint Board (Canvas API View wrapper helper)
    private DrawView drawView;
    private View buildDrawingBoard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        TextView header = createHeading("Finger Draw Canvas");
        layout.addView(header);

        drawView = new DrawView(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        clp.setMargins(0, 16, 0, 16);
        drawView.setLayoutParams(clp);
        drawView.setBackgroundColor(Color.WHITE);
        layout.addView(drawView);

        Button bClear = createStandardButton("Clear Canvas", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drawView != null) drawView.clear();
            }
        });
        layout.addView(bClear);
        return layout;
    }

    private static class DrawView extends View {
        private android.graphics.Paint paint;
        private android.graphics.Path path;
        public DrawView(Context context) {
            super(context);
            paint = new android.graphics.Paint();
            path = new android.graphics.Path();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(12f);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setColor(Color.RED);
        }
        public void clear() {
            path.reset();
            invalidate();
        }
        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            canvas.drawPath(path, paint);
        }
        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }

    // SCREEN 19: System Settings & Info
    private void buildSettings(LinearLayout container) {
        container.addView(createHeading("Hub Customizer & System Info"));

        TextView tvOS = new TextView(this);
        tvOS.setText("Android SDK API Level: " + android.os.Build.VERSION.SDK_INT + "\nDevice model: " + android.os.Build.MODEL);
        tvOS.setPadding(0, 0, 0, 24);
        tvOS.setTextSize(16);
        container.addView(tvOS);

        container.addView(new TextView(this) {{ setText("Select Navigation Header Color Theme:"); setTextSize(15); }});

        container.addView(createStandardButton("Ocean Blue Theme", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putInt("theme_color", 0xFF1E3799).apply();
                applyHeaderColor();
                Toast.makeText(MainActivity.this, "Ocean Blue applied!", Toast.LENGTH_SHORT).show();
            }
        }));

        container.addView(createStandardButton("Sunset Pink Theme", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putInt("theme_color", 0xFFEB2F06).apply();
                applyHeaderColor();
                Toast.makeText(MainActivity.this, "Sunset Pink applied!", Toast.LENGTH_SHORT).show();
            }
        }));
    }
}