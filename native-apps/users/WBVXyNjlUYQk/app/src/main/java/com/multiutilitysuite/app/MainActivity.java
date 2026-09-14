package com.multiutilitysuite.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // Main header title
    private TextView headerTitle;

    // View layouts
    private LinearLayout calcLayout;
    private ScrollView stopwatchLayout;
    private LinearLayout notesLayout;
    private ScrollView converterLayout;

    // Tab buttons
    private Button tabCalc;
    private Button tabStopwatch;
    private Button tabNotes;
    private Button tabConverter;

    // --- Calculator Variables ---
    private TextView calcScreen;
    private String currentInput = "0";
    private double firstNumber = Double.NaN;
    private String lastOperator = "";
    private boolean isNewOp = true;

    // --- Stopwatch & Countdown Variables ---
    private TextView stopwatchDisplay;
    private TextView tvLaps;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private Handler stopwatchHandler = new Handler();
    private boolean isRunning = false;
    private int lapCount = 1;

    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000) / 10;

            stopwatchDisplay.setText(String.format("%02d:%02d.%02d", mins, secs, milliseconds));
            stopwatchHandler.postDelayed(this, 50);
        }
    };

    // Countdown Timer Variables
    private EditText etTimerInput;
    private Button btnTimerStart;
    private Button btnTimerStop;
    private TextView timerDisplay;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    // --- Notes Variables ---
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Button btnSaveNote;
    private ListView lvNotes;
    private ArrayList<String> notesList;
    private ArrayAdapter<String> notesAdapter;

    // --- Converter Variables ---
    private Spinner spinnerCategory;
    private EditText etConverterInput;
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    private Button btnConvert;
    private TextView tvConverterResult;

    private final String[] lengthUnits = {"Meters (m)", "Kilometers (km)", "Feet (ft)", "Inches (in)"};
    private final String[] weightUnits = {"Grams (g)", "Kilograms (kg)", "Pounds (lb)"};
    private final String[] tempUnits = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Header Title View
        headerTitle = (TextView) findViewById(R.id.header_title);

        // Find Layout Views
        calcLayout = (LinearLayout) findViewById(R.id.calc_layout);
        stopwatchLayout = (ScrollView) findViewById(R.id.stopwatch_layout);
        notesLayout = (LinearLayout) findViewById(R.id.notes_layout);
        converterLayout = (ScrollView) findViewById(R.id.converter_layout);

        // Find Bottom Navigation Buttons
        tabCalc = (Button) findViewById(R.id.tab_calc);
        tabStopwatch = (Button) findViewById(R.id.tab_stopwatch);
        tabNotes = (Button) findViewById(R.id.tab_notes);
        tabConverter = (Button) findViewById(R.id.tab_converter);

        // Tab selection events
        tabCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabStopwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        tabConverter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Initialize Suite Modules
        setupCalculator();
        setupStopwatchAndTimer();
        setupNotes();
        setupConverter();

        // Default layout load
        switchTab(0);
    }

    // Switch between bottom layout tabs
    private void switchTab(int tabIndex) {
        calcLayout.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        stopwatchLayout.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        notesLayout.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        converterLayout.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        // Custom selected button background highlight switching
        tabCalc.setBackgroundColor(tabIndex == 0 ? Color.parseColor("#008080") : Color.parseColor("#DDDDDD"));
        tabCalc.setTextColor(tabIndex == 0 ? Color.WHITE : Color.parseColor("#555555"));

        tabStopwatch.setBackgroundColor(tabIndex == 1 ? Color.parseColor("#008080") : Color.parseColor("#DDDDDD"));
        tabStopwatch.setTextColor(tabIndex == 1 ? Color.WHITE : Color.parseColor("#555555"));

        tabNotes.setBackgroundColor(tabIndex == 2 ? Color.parseColor("#008080") : Color.parseColor("#DDDDDD"));
        tabNotes.setTextColor(tabIndex == 2 ? Color.WHITE : Color.parseColor("#555555"));

        tabConverter.setBackgroundColor(tabIndex == 3 ? Color.parseColor("#008080") : Color.parseColor("#DDDDDD"));
        tabConverter.setTextColor(tabIndex == 3 ? Color.WHITE : Color.parseColor("#555555"));

        switch (tabIndex) {
            case 0: headerTitle.setText("Calculator Pro"); break;
            case 1: headerTitle.setText("Timer & Stopwatch"); break;
            case 2: headerTitle.setText("Quick Notepad"); break;
            case 3: headerTitle.setText("Unit Converter"); break;
        }
    }

    // --- Calculator Engine implementation ---
    private void setupCalculator() {
        calcScreen = (TextView) findViewById(R.id.calc_screen);

        int[] numButtonIds = {
                R.id.btn_calc_0, R.id.btn_calc_1, R.id.btn_calc_2, R.id.btn_calc_3,
                R.id.btn_calc_4, R.id.btn_calc_5, R.id.btn_calc_6, R.id.btn_calc_7,
                R.id.btn_calc_8, R.id.btn_calc_9
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                appendCalculatorDigit(b.getText().toString());
            }
        };

        for (int id : numButtonIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        findViewById(R.id.btn_calc_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendCalculatorDecimal();
            }
        });

        int[] opButtonIds = {
                R.id.btn_calc_add, R.id.btn_calc_sub, R.id.btn_calc_mul, R.id.btn_calc_div, R.id.btn_calc_percent
        };

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                setCalculatorOperator(b.getText().toString());
            }
        };

        for (int id : opButtonIds) {
            findViewById(id).setOnClickListener(opListener);
        }

        findViewById(R.id.btn_calc_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCalculation();
            }
        });

        findViewById(R.id.btn_calc_c).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetCalculator();
            }
        });

        findViewById(R.id.btn_calc_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspaceCalculator();
            }
        });
    }

    private void appendCalculatorDigit(String value) {
        if (isNewOp) {
            currentInput = value;
            isNewOp = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = value;
            } else {
                currentInput += value;
            }
        }
        calcScreen.setText(currentInput);
    }

    private void appendCalculatorDecimal() {
        if (isNewOp) {
            currentInput = "0.";
            isNewOp = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        calcScreen.setText(currentInput);
    }

    private void setCalculatorOperator(String op) {
        if (!Double.isNaN(firstNumber) && !isNewOp) {
            performCalculation();
        }
        try {
            firstNumber = Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            firstNumber = 0;
        }
        lastOperator = op;
        isNewOp = true;
    }

    private void performCalculation() {
        if (Double.isNaN(firstNumber)) {
            return;
        }
        double secondNumber;
        try {
            secondNumber = Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            secondNumber = 0;
        }
        double result = 0;
        if (lastOperator.equals("+")) {
            result = firstNumber + secondNumber;
        } else if (lastOperator.equals("-")) {
            result = firstNumber - secondNumber;
        } else if (lastOperator.equals("*")) {
            result = firstNumber * secondNumber;
        } else if (lastOperator.equals("/")) {
            if (secondNumber != 0) {
                result = firstNumber / secondNumber;
            } else {
                calcScreen.setText("Error");
                firstNumber = Double.NaN;
                lastOperator = "";
                isNewOp = true;
                return;
            }
        } else if (lastOperator.equals("%")) {
            result = firstNumber % secondNumber;
        } else {
            result = secondNumber;
        }

        String resultString;
        if (result == (long) result) {
            resultString = String.format("%d", (long) result);
        } else {
            resultString = String.valueOf(result);
        }

        calcScreen.setText(resultString);
        currentInput = resultString;
        firstNumber = result;
        isNewOp = true;
    }

    private void resetCalculator() {
        firstNumber = Double.NaN;
        lastOperator = "";
        currentInput = "0";
        isNewOp = true;
        calcScreen.setText("0");
    }

    private void backspaceCalculator() {
        if (currentInput.length() > 0 && !isNewOp) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            calcScreen.setText(currentInput);
        }
    }

    // --- Stopwatch & Countdown Timer Engine ---
    private void setupStopwatchAndTimer() {
        stopwatchDisplay = (TextView) findViewById(R.id.stopwatch_display);
        tvLaps = (TextView) findViewById(R.id.tv_laps);
        tvLaps.setMovementMethod(new ScrollingMovementMethod());

        Button btnSwStart = (Button) findViewById(R.id.btn_sw_start);
        Button btnSwPause = (Button) findViewById(R.id.btn_sw_pause);
        Button btnSwLap = (Button) findViewById(R.id.btn_sw_lap);
        Button btnSwReset = (Button) findViewById(R.id.btn_sw_reset);

        btnSwStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    startTime = SystemClock.uptimeMillis();
                    stopwatchHandler.postDelayed(updateTimeRunnable, 0);
                    isRunning = true;
                }
            }
        });

        btnSwPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    timeSwapBuff += timeInMilliseconds;
                    stopwatchHandler.removeCallbacks(updateTimeRunnable);
                    isRunning = false;
                }
            }
        });

        btnSwLap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    String currentLap = "Lap " + lapCount + ": " + stopwatchDisplay.getText().toString();
                    lapCount++;
                    if (tvLaps.getText().toString().equals("Laps will appear here...")) {
                        tvLaps.setText(currentLap);
                    } else {
                        tvLaps.setText(currentLap + "\n" + tvLaps.getText().toString());
                    }
                }
            }
        });

        btnSwReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L;
                updatedTime = 0L;
                isRunning = false;
                stopwatchHandler.removeCallbacks(updateTimeRunnable);
                stopwatchDisplay.setText("00:00.00");
                tvLaps.setText("Laps will appear here...");
                lapCount = 1;
            }
        });

        // Countdown Sub-Module
        etTimerInput = (EditText) findViewById(R.id.et_timer_input);
        btnTimerStart = (Button) findViewById(R.id.btn_timer_start);
        btnTimerStop = (Button) findViewById(R.id.btn_timer_stop);
        timerDisplay = (TextView) findViewById(R.id.timer_display);

        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCountdownTimer();
            }
        });

        btnTimerStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCountdownTimer();
            }
        });
    }

    private void startCountdownTimer() {
        String inputSecs = etTimerInput.getText().toString().trim();
        if (inputSecs.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter seconds", Toast.LENGTH_SHORT).show();
            return;
        }
        long totalMillis = Long.parseLong(inputSecs) * 1000;
        if (totalMillis <= 0) {
            Toast.makeText(MainActivity.this, "Please enter more than 0 seconds", Toast.LENGTH_SHORT).show();
            return;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerDisplay.setText(formatTime(totalMillis));
        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerDisplay.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                timerDisplay.setText("00:00");
                isTimerRunning = false;
                Toast.makeText(MainActivity.this, "Countdown Finished!", Toast.LENGTH_SHORT).show();
            }
        }.start();
        isTimerRunning = true;
    }

    private void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }
        timerDisplay.setText("00:00");
        etTimerInput.setText("");
    }

    private String formatTime(long millis) {
        int secs = (int) (millis / 1000);
        int mins = secs / 60;
        secs = secs % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // --- Notes Persistent Engine implementation ---
    private void setupNotes() {
        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        etNoteContent = (EditText) findViewById(R.id.et_note_content);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);
        lvNotes = (ListView) findViewById(R.id.lv_notes);

        notesList = new ArrayList<String>();
        loadStoredNotes();

        notesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, notesList);
        lvNotes.setAdapter(notesAdapter);

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewNote();
            }
        });

        lvNotes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                notesList.remove(position);
                notesAdapter.notifyDataSetChanged();
                writeNotesToStorage();
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void saveNewNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Note content is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String compiledNote = title.isEmpty() ? content : title.toUpperCase() + "\n" + content;
        notesList.add(0, compiledNote);
        notesAdapter.notifyDataSetChanged();
        writeNotesToStorage();

        etNoteTitle.setText("");
        etNoteContent.setText("");
        Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadStoredNotes() {
        SharedPreferences prefs = getSharedPreferences("SuiteNotesData", MODE_PRIVATE);
        int count = prefs.getInt("note_count", 0);
        notesList.clear();
        for (int i = 0; i < count; i++) {
            String note = prefs.getString("note_" + i, null);
            if (note != null) {
                notesList.add(note);
            }
        }
    }

    private void writeNotesToStorage() {
        SharedPreferences.Editor editor = getSharedPreferences("SuiteNotesData", MODE_PRIVATE).edit();
        editor.putInt("note_count", notesList.size());
        for (int i = 0; i < notesList.size(); i++) {
            editor.putString("note_" + i, notesList.get(i));
        }
        editor.apply();
    }

    // --- Converter Engine implementation ---
    private void setupConverter() {
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        etConverterInput = (EditText) findViewById(R.id.et_converter_input);
        spinnerFrom = (Spinner) findViewById(R.id.spinner_from);
        spinnerTo = (Spinner) findViewById(R.id.spinner_to);
        btnConvert = (Button) findViewById(R.id.btn_convert);
        tvConverterResult = (TextView) findViewById(R.id.tv_converter_result);

        String[] categories = {"Length", "Weight", "Temperature"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSelectorUnits(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runUnitConversion();
            }
        });
    }

    private void updateSelectorUnits(int categoryIndex) {
        String[] units;
        if (categoryIndex == 0) {
            units = lengthUnits;
        } else if (categoryIndex == 1) {
            units = weightUnits;
        } else {
            units = tempUnits;
        }

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(unitAdapter);
        spinnerTo.setAdapter(unitAdapter);
    }

    private void runUnitConversion() {
        String valStr = etConverterInput.getText().toString().trim();
        if (valStr.isEmpty()) {
            Toast.makeText(this, "Please enter numeric value", Toast.LENGTH_SHORT).show();
            return;
        }

        double val;
        try {
            val = Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number entered", Toast.LENGTH_SHORT).show();
            return;
        }

        int catIdx = spinnerCategory.getSelectedItemPosition();
        int fromIdx = spinnerFrom.getSelectedItemPosition();
        int toIdx = spinnerTo.getSelectedItemPosition();

        double output = 0.0;

        if (catIdx == 0) {
            // Length
            double meters = 0.0;
            switch (fromIdx) {
                case 0: meters = val; break;
                case 1: meters = val * 1000.0; break;
                case 2: meters = val * 0.3048; break;
                case 3: meters = val * 0.0254; break;
            }
            switch (toIdx) {
                case 0: output = meters; break;
                case 1: output = meters / 1000.0; break;
                case 2: output = meters / 0.3048; break;
                case 3: output = meters / 0.0254; break;
            }
        } else if (catIdx == 1) {
            // Weight
            double grams = 0.0;
            switch (fromIdx) {
                case 0: grams = val; break;
                case 1: grams = val * 1000.0; break;
                case 2: grams = val * 453.59237; break;
            }
            switch (toIdx) {
                case 0: output = grams; break;
                case 1: output = grams / 1000.0; break;
                case 2: output = grams / 453.59237; break;
            }
        } else if (catIdx == 2) {
            // Temperature
            if (fromIdx == toIdx) {
                output = val;
            } else if (fromIdx == 0) { // Celsius
                if (toIdx == 1) output = (val * 9 / 5) + 32;
                else output = val + 273.15;
            } else if (fromIdx == 1) { // Fahrenheit
                if (toIdx == 0) output = (val - 32) * 5 / 9;
                else output = (val - 32) * 5 / 9 + 273.15;
            } else if (fromIdx == 2) { // Kelvin
                if (toIdx == 0) output = val - 273.15;
                else output = (val - 273.15) * 9 / 5 + 32;
            }
        }

        tvConverterResult.setText(String.format("Result: %.4f", output));
    }
}