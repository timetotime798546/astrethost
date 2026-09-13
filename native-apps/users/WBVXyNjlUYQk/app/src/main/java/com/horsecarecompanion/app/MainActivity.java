package com.horsecarecompanion.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Database Helper
    private DatabaseHelper dbHelper;

    // Tabs Layout Switchers
    private TextView tabHorses, tabCare, tabActivity, tabGuide;
    private ScrollView layoutHorses, layoutCare, layoutActivity, layoutGuide;

    // Tab 1 Layout Views
    private LinearLayout containerHorsesList;
    private Button btnAddHorse;

    // Tab 2 Layout Views
    private Spinner spinnerCareHorses;
    private Button btnAddCareLog;
    private LinearLayout containerCareLogs;

    // Tab 3 Layout Views
    private Spinner spinnerActivityHorses;
    private Spinner spinnerActivityType;
    private TextView txtTimer;
    private Button btnTimerStart, btnTimerPause, btnTimerReset, btnTimerSave;
    private LinearLayout containerActivityLogs;

    // Timer Variables
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isTimerRunning = false;

    // Active timing runnable
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;

            txtTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            timerHandler.postDelayed(this, 500);
        }
    };

    // SQLite representation model
    public static class Horse {
        public int id;
        public String name;
        public String breed;
        public int age;
        public int weight;
        public String notes;

        public Horse(int id, String name, String breed, int age, int weight, String notes) {
            this.id = id;
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.weight = weight;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SQLite Database Helper
        dbHelper = new DatabaseHelper(this);

        // Link Tab Switchers UI
        tabHorses = (TextView) findViewById(R.id.tab_horses);
        tabCare = (TextView) findViewById(R.id.tab_care);
        tabActivity = (TextView) findViewById(R.id.tab_activity);
        tabGuide = (TextView) findViewById(R.id.tab_guide);

        layoutHorses = (ScrollView) findViewById(R.id.layout_horses);
        layoutCare = (ScrollView) findViewById(R.id.layout_care);
        layoutActivity = (ScrollView) findViewById(R.id.layout_activity);
        layoutGuide = (ScrollView) findViewById(R.id.layout_guide);

        // Link Tab 1 Elements
        containerHorsesList = (LinearLayout) findViewById(R.id.container_horses_list);
        btnAddHorse = (Button) findViewById(R.id.btn_add_horse);

        // Link Tab 2 Elements
        spinnerCareHorses = (Spinner) findViewById(R.id.spinner_care_horses);
        btnAddCareLog = (Button) findViewById(R.id.btn_add_care_log);
        containerCareLogs = (LinearLayout) findViewById(R.id.container_care_logs);

        // Link Tab 3 Elements
        spinnerActivityHorses = (Spinner) findViewById(R.id.spinner_activity_horses);
        spinnerActivityType = (Spinner) findViewById(R.id.spinner_activity_type);
        txtTimer = (TextView) findViewById(R.id.txt_timer);
        btnTimerStart = (Button) findViewById(R.id.btn_timer_start);
        btnTimerPause = (Button) findViewById(R.id.btn_timer_pause);
        btnTimerReset = (Button) findViewById(R.id.btn_timer_reset);
        btnTimerSave = (Button) findViewById(R.id.btn_timer_save);
        containerActivityLogs = (LinearLayout) findViewById(R.id.container_activity_logs);

        // Configure Activity selection spinner items
        String[] activityTypes = {"Riding Training", "Lunging Work", "Field Turnout", "Grooming Session", "Groundwork / Showing"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activityTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(typeAdapter);

        // Tab Navigation click listeners
        tabHorses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabCare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        tabActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        tabGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(4);
            }
        });

        // Setup Horse profile creator trigger
        btnAddHorse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddHorseDialog();
            }
        });

        // Care log addition triggers
        btnAddCareLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCareLogDialog();
            }
        });

        // Setup Spinner updates
        spinnerCareHorses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Horse selected = (Horse) parent.getItemAtPosition(position);
                if (selected != null) {
                    loadCareLogs(selected.id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                containerCareLogs.removeAllViews();
            }
        });

        spinnerActivityHorses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Horse selected = (Horse) parent.getItemAtPosition(position);
                if (selected != null) {
                    loadActivityLogs(selected.id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                containerActivityLogs.removeAllViews();
            }
        });

        // Setup timer stop watch operations
        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTimerRunning) {
                    startTime = SystemClock.uptimeMillis();
                    timerHandler.postDelayed(updateTimerThread, 0);
                    isTimerRunning = true;
                    btnTimerStart.setEnabled(false);
                    btnTimerPause.setEnabled(true);
                    btnTimerSave.setEnabled(true);
                }
            }
        });

        btnTimerPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    timeSwapBuff += timeInMilliseconds;
                    timerHandler.removeCallbacks(updateTimerThread);
                    isTimerRunning = false;
                    btnTimerStart.setEnabled(true);
                    btnTimerPause.setEnabled(false);
                }
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSwapBuff = 0L;
                timeInMilliseconds = 0L;
                updatedTime = 0L;
                startTime = SystemClock.uptimeMillis();
                txtTimer.setText("00:00");
                if (isTimerRunning) {
                    timerHandler.removeCallbacks(updateTimerThread);
                    isTimerRunning = false;
                }
                btnTimerStart.setEnabled(true);
                btnTimerPause.setEnabled(false);
                btnTimerSave.setEnabled(false);
            }
        });

        btnTimerSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWorkoutSession();
            }
        });

        // Load Initial State data
        loadHorsesData();
    }

    private void switchTab(int tabIndex) {
        // Reset tab bar backgrounds
        tabHorses.setBackgroundColor(Color.parseColor("#E8EAE6"));
        tabHorses.setTextColor(Color.parseColor("#555555"));
        tabCare.setBackgroundColor(Color.parseColor("#E8EAE6"));
        tabCare.setTextColor(Color.parseColor("#555555"));
        tabActivity.setBackgroundColor(Color.parseColor("#E8EAE6"));
        tabActivity.setTextColor(Color.parseColor("#555555"));
        tabGuide.setBackgroundColor(Color.parseColor("#E8EAE6"));
        tabGuide.setTextColor(Color.parseColor("#555555"));

        // Hide all views
        layoutHorses.setVisibility(View.GONE);
        layoutCare.setVisibility(View.GONE);
        layoutActivity.setVisibility(View.GONE);
        layoutGuide.setVisibility(View.GONE);

        // Show target tab
        switch (tabIndex) {
            case 1:
                tabHorses.setBackgroundColor(Color.parseColor("#2E5A44"));
                tabHorses.setTextColor(Color.WHITE);
                layoutHorses.setVisibility(View.VISIBLE);
                loadHorsesData();
                break;
            case 2:
                tabCare.setBackgroundColor(Color.parseColor("#2E5A44"));
                tabCare.setTextColor(Color.WHITE);
                layoutCare.setVisibility(View.VISIBLE);
                refreshTabSpinner(spinnerCareHorses);
                break;
            case 3:
                tabActivity.setBackgroundColor(Color.parseColor("#2E5A44"));
                tabActivity.setTextColor(Color.WHITE);
                layoutActivity.setVisibility(View.VISIBLE);
                refreshTabSpinner(spinnerActivityHorses);
                break;
            case 4:
                tabGuide.setBackgroundColor(Color.parseColor("#2E5A44"));
                tabGuide.setTextColor(Color.WHITE);
                layoutGuide.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void refreshTabSpinner(Spinner spinner) {
        List<Horse> horses = getAllHorses();
        ArrayAdapter<Horse> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, horses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private List<Horse> getAllHorses() {
        List<Horse> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM horses ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String breed = cursor.getString(cursor.getColumnIndexOrThrow("breed"));
                int age = cursor.getInt(cursor.getColumnIndexOrThrow("age"));
                int weight = cursor.getInt(cursor.getColumnIndexOrThrow("weight"));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
                list.add(new Horse(id, name, breed, age, weight, notes));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private void loadHorsesData() {
        containerHorsesList.removeAllViews();
        List<Horse> horses = getAllHorses();

        if (horses.isEmpty()) {
            TextView txtEmpty = new TextView(this);
            txtEmpty.setText("No horses in your stable yet.\nClick '+ Add New Horse' to register your first companion!");
            txtEmpty.setTextSize(14);
            txtEmpty.setTextColor(Color.GRAY);
            txtEmpty.setGravity(Gravity.CENTER);
            txtEmpty.setPadding(0, 50, 0, 50);
            containerHorsesList.addView(txtEmpty);
        } else {
            for (int i = 0; i < horses.size(); i++) {
                final Horse horse = horses.get(i);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.WHITE);
                card.setPadding(24, 24, 24, 24);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 16);
                card.setLayoutParams(params);

                TextView txtName = new TextView(this);
                txtName.setText(horse.name);
                txtName.setTextSize(18);
                txtName.setTextColor(Color.parseColor("#2E5A44"));
                txtName.setTypeface(null, Typeface.BOLD);
                card.addView(txtName);

                TextView txtBreedAge = new TextView(this);
                txtBreedAge.setText("Breed: " + horse.breed + "   |   Age: " + horse.age + " yrs\nWeight: " + horse.weight + " lbs");
                txtBreedAge.setTextSize(14);
                txtBreedAge.setTextColor(Color.parseColor("#555555"));
                txtBreedAge.setPadding(0, 6, 0, 6);
                card.addView(txtBreedAge);

                if (horse.notes != null && !horse.notes.isEmpty()) {
                    TextView txtNotes = new TextView(this);
                    txtNotes.setText("Notes: " + horse.notes);
                    txtNotes.setTextSize(13);
                    txtNotes.setTextColor(Color.parseColor("#777777"));
                    txtNotes.setPadding(0, 0, 0, 8);
                    card.addView(txtNotes);
                }

                Button btnDelete = new Button(this);
                btnDelete.setText("Remove Companion");
                btnDelete.setTextSize(11);
                btnDelete.setTextColor(Color.WHITE);
                btnDelete.setBackgroundColor(Color.parseColor("#991B1B"));

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                btnParams.setMargins(0, 8, 0, 0);
                btnDelete.setLayoutParams(btnParams);

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Remove " + horse.name + "?")
                                .setMessage("Are you sure you want to remove this horse? This deletes all activity and feeding/medical history permanently.")
                                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                                        db.delete("horses", "id = ?", new String[]{String.valueOf(horse.id)});
                                        db.delete("care_logs", "horse_id = ?", new String[]{String.valueOf(horse.id)});
                                        db.delete("activities", "horse_id = ?", new String[]{String.valueOf(horse.id)});
                                        Toast.makeText(MainActivity.this, horse.name + " deleted.", Toast.LENGTH_SHORT).show();
                                        loadHorsesData();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
                card.addView(btnDelete);

                containerHorsesList.addView(card);
            }
        }
        populateSpinners();
    }

    private void populateSpinners() {
        List<Horse> horses = getAllHorses();

        ArrayAdapter<Horse> careAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, horses);
        careAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCareHorses.setAdapter(careAdapter);

        ArrayAdapter<Horse> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, horses);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityHorses.setAdapter(activityAdapter);
    }

    private void showAddHorseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Horse Profile");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        final EditText inputName = new EditText(this);
        inputName.setHint("Horse Name (e.g. Spirit)");
        container.addView(inputName);

        final EditText inputBreed = new EditText(this);
        inputBreed.setHint("Breed (e.g. Quarter Horse)");
        inputBreed.setPadding(0, 16, 0, 16);
        container.addView(inputBreed);

        final EditText inputAge = new EditText(this);
        inputAge.setHint("Age in Years (e.g. 8)");
        inputAge.setInputType(InputType.TYPE_CLASS_NUMBER);
        container.addView(inputAge);

        final EditText inputWeight = new EditText(this);
        inputWeight.setHint("Weight in lbs (e.g. 1100)");
        inputWeight.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputWeight.setPadding(0, 16, 0, 16);
        container.addView(inputWeight);

        final EditText inputNotes = new EditText(this);
        inputNotes.setHint("General Notes / Feeding needs / Health info");
        container.addView(inputNotes);

        builder.setView(container);

        builder.setPositiveButton("Add Profile", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputName.getText().toString().trim();
                String breed = inputBreed.getText().toString().trim();
                String ageStr = inputAge.getText().toString().trim();
                String weightStr = inputWeight.getText().toString().trim();
                String notes = inputNotes.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Horse name is required!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
                int weight = weightStr.isEmpty() ? 0 : Integer.parseInt(weightStr);

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("breed", breed);
                values.put("age", age);
                values.put("weight", weight);
                values.put("notes", notes);

                db.insert("horses", null, values);
                Toast.makeText(MainActivity.this, name + " profile added!", Toast.LENGTH_SHORT).show();

                loadHorsesData();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddCareLogDialog() {
        final Horse selected = (Horse) spinnerCareHorses.getSelectedItem();
        if (selected == null) {
            Toast.makeText(this, "Please register a horse profile first!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Event for " + selected.name);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        TextView typeLabel = new TextView(this);
        typeLabel.setText("Select Log Class Type:");
        typeLabel.setTextSize(12);
        typeLabel.setTextColor(Color.GRAY);
        container.addView(typeLabel);

        final Spinner spinnerClass = new Spinner(this);
        String[] logsClasses = {"Feeding/Diet", "Farrier Trim", "Vet Exam", "Deworming Schedule", "Vaccines", "Grooming Notes"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, logsClasses);
        spinnerClass.setAdapter(arrayAdapter);
        container.addView(spinnerClass);

        final EditText inputDetails = new EditText(this);
        inputDetails.setHint("Log Details (e.g. Fed alfalfa, trimmed back shoes)");
        inputDetails.setPadding(0, 24, 0, 16);
        container.addView(inputDetails);

        final EditText inputDate = new EditText(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        inputDate.setText(sdf.format(new Date()));
        inputDate.setHint("Date (YYYY-MM-DD)");
        container.addView(inputDate);

        builder.setView(container);

        builder.setPositiveButton("Log Entry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String logClass = spinnerClass.getSelectedItem().toString();
                String details = inputDetails.getText().toString().trim();
                String date = inputDate.getText().toString().trim();

                if (details.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Log details are required!", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("horse_id", selected.id);
                values.put("log_type", logClass);
                values.put("details", details);
                values.put("date", date);

                db.insert("care_logs", null, values);
                Toast.makeText(MainActivity.this, "Daily care event saved!", Toast.LENGTH_SHORT).show();

                loadCareLogs(selected.id);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadCareLogs(int horseId) {
        containerCareLogs.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM care_logs WHERE horse_id = ? ORDER BY id DESC", new String[]{String.valueOf(horseId)});

        if (cursor.getCount() == 0) {
            TextView txtEmpty = new TextView(this);
            txtEmpty.setText("No care logs recorded for this horse yet.");
            txtEmpty.setTextSize(13);
            txtEmpty.setTextColor(Color.GRAY);
            txtEmpty.setPadding(0, 20, 0, 20);
            containerCareLogs.addView(txtEmpty);
        } else {
            if (cursor.moveToFirst()) {
                do {
                    String logType = cursor.getString(cursor.getColumnIndexOrThrow("log_type"));
                    String details = cursor.getString(cursor.getColumnIndexOrThrow("details"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundColor(Color.WHITE);
                    card.setPadding(20, 20, 20, 20);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 12);
                    card.setLayoutParams(params);

                    TextView txtHeading = new TextView(this);
                    txtHeading.setText(logType + "  |  " + date);
                    txtHeading.setTextSize(14);
                    txtHeading.setTextColor(Color.parseColor("#2E5A44"));
                    txtHeading.setTypeface(null, Typeface.BOLD);
                    card.addView(txtHeading);

                    TextView txtDetailsText = new TextView(this);
                    txtDetailsText.setText(details);
                    txtDetailsText.setTextSize(13);
                    txtDetailsText.setTextColor(Color.parseColor("#333333"));
                    txtDetailsText.setPadding(0, 4, 0, 0);
                    card.addView(txtDetailsText);

                    containerCareLogs.addView(card);

                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    private void saveWorkoutSession() {
        Horse selected = (Horse) spinnerActivityHorses.getSelectedItem();
        if (selected == null) {
            Toast.makeText(this, "Select or register a horse first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate time elapsed in full minutes
        int calculatedSeconds = (int) (updatedTime / 1000);
        int durationMinutes = calculatedSeconds / 60;
        if (durationMinutes < 1) {
            durationMinutes = 1; // standard lower bound representation
        }

        String workoutType = spinnerActivityType.getSelectedItem().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = sdf.format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("horse_id", selected.id);
        values.put("activity_type", workoutType);
        values.put("duration_minutes", durationMinutes);
        values.put("date", date);

        db.insert("activities", null, values);
        Toast.makeText(this, "Workout saved: " + durationMinutes + " min of " + workoutType, Toast.LENGTH_SHORT).show();

        // Reset Clock UI
        timeSwapBuff = 0L;
        timeInMilliseconds = 0L;
        updatedTime = 0L;
        txtTimer.setText("00:00");
        if (isTimerRunning) {
            timerHandler.removeCallbacks(updateTimerThread);
            isTimerRunning = false;
        }
        btnTimerStart.setEnabled(true);
        btnTimerPause.setEnabled(false);
        btnTimerSave.setEnabled(false);

        loadActivityLogs(selected.id);
    }

    private void loadActivityLogs(int horseId) {
        containerActivityLogs.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM activities WHERE horse_id = ? ORDER BY id DESC", new String[]{String.valueOf(horseId)});

        if (cursor.getCount() == 0) {
            TextView txtEmpty = new TextView(this);
            txtEmpty.setText("No activities logged yet.\nUse the timer above to complete and record work!");
            txtEmpty.setTextSize(13);
            txtEmpty.setTextColor(Color.GRAY);
            txtEmpty.setPadding(0, 20, 0, 20);
            containerActivityLogs.addView(txtEmpty);
        } else {
            if (cursor.moveToFirst()) {
                do {
                    String workout = cursor.getString(cursor.getColumnIndexOrThrow("activity_type"));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundColor(Color.WHITE);
                    card.setPadding(20, 20, 20, 20);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 12);
                    card.setLayoutParams(params);

                    TextView txtHeading = new TextView(this);
                    txtHeading.setText(workout + "  |  " + date);
                    txtHeading.setTextSize(14);
                    txtHeading.setTextColor(Color.parseColor("#2E5A44"));
                    txtHeading.setTypeface(null, Typeface.BOLD);
                    card.addView(txtHeading);

                    TextView txtDur = new TextView(this);
                    txtDur.setText("Session length: " + duration + " minutes");
                    txtDur.setTextSize(13);
                    txtDur.setTextColor(Color.parseColor("#333333"));
                    txtDur.setPadding(0, 4, 0, 0);
                    card.addView(txtDur);

                    containerActivityLogs.addView(card);

                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    // Standard local SQLite schema
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "horse_companion_app.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE horses (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, breed TEXT, age INTEGER, weight INTEGER, notes TEXT);");
            db.execSQL("CREATE TABLE care_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, horse_id INTEGER, log_type TEXT, details TEXT, date TEXT);");
            db.execSQL("CREATE TABLE activities (id INTEGER PRIMARY KEY AUTOINCREMENT, horse_id INTEGER, activity_type TEXT, duration_minutes INTEGER, date TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS horses;");
            db.execSQL("DROP TABLE IF EXISTS care_logs;");
            db.execSQL("DROP TABLE IF EXISTS activities;");
            onCreate(db);
        }
    }
}