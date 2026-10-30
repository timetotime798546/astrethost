package com.habitflowtracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.habitflowtracker.app.model.Habit;
import com.habitflowtracker.app.util.HabitCalculator;
import com.habitflowtracker.app.util.HabitStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private HabitStorage storage;
    private List<Habit> habitList;
    private String currentFilter = "All";

    private TextView tvCurrentDate;
    private TextView tvProgressPercentage;
    private ProgressBar pbTodayProgress;
    private TextView tvCompletedFraction;
    private TextView tvStatStreak;
    private TextView tvStatActive;
    private TextView tvStatWeekly;

    private LinearLayout layoutHabitsList;
    private LinearLayout layoutEmptyState;
    private Button btnAddHabit;

    private Button tabAll;
    private Button tabMorning;
    private Button tabAfternoon;
    private Button tabEvening;
    private Button tabPending;
    private Button tabCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new HabitStorage(this);

        initViews();
        setupListeners();
        renderDateHeader();
        refreshHabitsData();
    }

    private void initViews() {
        tvCurrentDate = (TextView) findViewById(R.id.tv_current_date);
        tvProgressPercentage = (TextView) findViewById(R.id.tv_progress_percentage);
        pbTodayProgress = (ProgressBar) findViewById(R.id.pb_today_progress);
        tvCompletedFraction = (TextView) findViewById(R.id.tv_completed_fraction);
        tvStatStreak = (TextView) findViewById(R.id.tv_stat_streak);
        tvStatActive = (TextView) findViewById(R.id.tv_stat_active);
        tvStatWeekly = (TextView) findViewById(R.id.tv_stat_weekly);

        layoutHabitsList = (LinearLayout) findViewById(R.id.layout_habits_list);
        layoutEmptyState = (LinearLayout) findViewById(R.id.layout_empty_state);
        btnAddHabit = (Button) findViewById(R.id.btn_add_habit);

        tabAll = (Button) findViewById(R.id.tab_all);
        tabMorning = (Button) findViewById(R.id.tab_morning);
        tabAfternoon = (Button) findViewById(R.id.tab_afternoon);
        tabEvening = (Button) findViewById(R.id.tab_evening);
        tabPending = (Button) findViewById(R.id.tab_pending);
        tabCompleted = (Button) findViewById(R.id.tab_completed);
    }

    private void setupListeners() {
        btnAddHabit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddOrEditHabitDialog(null);
            }
        });

        tabAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("All");
            }
        });

        tabMorning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("Morning");
            }
        });

        tabAfternoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("Afternoon");
            }
        });

        tabEvening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("Evening");
            }
        });

        tabPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("Pending");
            }
        });

        tabCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("Completed");
            }
        });
    }

    private void renderDateHeader() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new Date()));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterTabsUI();
        renderHabitsList();
    }

    private void updateFilterTabsUI() {
        resetTabButton(tabAll, "All");
        resetTabButton(tabMorning, "Morning");
        resetTabButton(tabAfternoon, "Afternoon");
        resetTabButton(tabEvening, "Evening");
        resetTabButton(tabPending, "Pending");
        resetTabButton(tabCompleted, "Completed");

        if ("All".equals(currentFilter)) activateTabButton(tabAll);
        else if ("Morning".equals(currentFilter)) activateTabButton(tabMorning);
        else if ("Afternoon".equals(currentFilter)) activateTabButton(tabAfternoon);
        else if ("Evening".equals(currentFilter)) activateTabButton(tabEvening);
        else if ("Pending".equals(currentFilter)) activateTabButton(tabPending);
        else if ("Completed".equals(currentFilter)) activateTabButton(tabCompleted);
    }

    private void activateTabButton(Button btn) {
        btn.setBackgroundResource(R.drawable.bg_button_primary);
        btn.setTextColor(Color.WHITE);
    }

    private void resetTabButton(Button btn, String label) {
        btn.setBackgroundResource(R.drawable.bg_chip);
        btn.setTextColor(Color.parseColor("#64748B"));
    }

    private void refreshHabitsData() {
        habitList = storage.getAllHabits();
        recalculateDashboardStats();
        renderHabitsList();
    }

    /**
     * Local deterministic calculations for today's habits, streaks, and rates.
     */
    private void recalculateDashboardStats() {
        if (habitList == null) return;

        int totalCount = habitList.size();
        int completedCount = HabitCalculator.countCompletedToday(habitList);
        int ratePercentage = HabitCalculator.calculateTodayCompletionRate(habitList);
        int maxStreak = HabitCalculator.calculateHighestActiveStreak(habitList);
        int weeklyRate = HabitCalculator.calculateWeeklyCompletionRate(habitList);

        tvProgressPercentage.setText(ratePercentage + "%");
        pbTodayProgress.setProgress(ratePercentage);
        tvCompletedFraction.setText(completedCount + " of " + totalCount + " habits completed today");

        tvStatStreak.setText("🔥 " + maxStreak);
        tvStatActive.setText(String.valueOf(totalCount));
        tvStatWeekly.setText(weeklyRate + "%");
    }

    private void renderHabitsList() {
        layoutHabitsList.removeAllViews();
        String today = HabitCalculator.getTodayString();

        int displayedCount = 0;

        for (int i = 0; i < habitList.size(); i++) {
            final Habit habit = habitList.get(i);

            boolean isCompleted = habit.isCompletedOn(today);

            if ("Morning".equals(currentFilter) && !"Morning".equalsIgnoreCase(habit.getTimeOfDay())) {
                continue;
            } else if ("Afternoon".equals(currentFilter) && !"Afternoon".equalsIgnoreCase(habit.getTimeOfDay())) {
                continue;
            } else if ("Evening".equals(currentFilter) && !"Evening".equalsIgnoreCase(habit.getTimeOfDay())) {
                continue;
            } else if ("Pending".equals(currentFilter) && isCompleted) {
                continue;
            } else if ("Completed".equals(currentFilter) && !isCompleted) {
                continue;
            }

            displayedCount++;
            View card = createHabitCardView(habit, isCompleted, today);
            layoutHabitsList.addView(card);
        }

        if (displayedCount == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private View createHabitCardView(final Habit habit, final boolean isCompletedToday, final String today) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setPadding(16, 16, 16, 16);

        // Header Row: Checkbox, Titles, Streak Badge
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        final CheckBox cb = new CheckBox(this);
        cb.setChecked(isCompletedToday);
        cb.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.addView(cb);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMargins(12, 0, 8, 0);
        textLayout.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(habit.getTitle());
        tvTitle.setTextSize(15);
        tvTitle.setTextColor(isCompletedToday ? Color.parseColor("#94A3B8") : Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textLayout.addView(tvTitle);

        if (habit.getDescription() != null && !habit.getDescription().trim().isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(habit.getDescription());
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(Color.parseColor("#64748B"));
            tvDesc.setMaxLines(1);
            textLayout.addView(tvDesc);
        }

        headerRow.addView(textLayout);

        // Streak badge
        int currentStreak = HabitCalculator.calculateCurrentStreak(habit);
        TextView tvStreak = new TextView(this);
        tvStreak.setText("🔥 " + currentStreak + "d");
        tvStreak.setTextSize(12);
        tvStreak.setTextColor(Color.parseColor("#EA580C"));
        tvStreak.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStreak.setBackgroundResource(R.drawable.bg_badge_streak);
        tvStreak.setPadding(12, 6, 12, 6);
        headerRow.addView(tvStreak);

        card.addView(headerRow);

        // Footer Row: Category Tag + 7-Day Dots Indicator
        LinearLayout footerRow = new LinearLayout(this);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.setMargins(44, 10, 0, 0);
        footerRow.setLayoutParams(footerParams);

        TextView tvCategory = new TextView(this);
        tvCategory.setText(habit.getTimeOfDay() + " • " + habit.getCategory());
        tvCategory.setTextSize(11);
        tvCategory.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvCategory.setLayoutParams(catParams);
        footerRow.addView(tvCategory);

        // 7-day mini tracker dots
        LinearLayout dotsLayout = new LinearLayout(this);
        dotsLayout.setOrientation(LinearLayout.HORIZONTAL);
        dotsLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        for (int day = 6; day >= 0; day--) {
            String checkDay = HabitCalculator.getPastDateString(day);
            boolean done = habit.isCompletedOn(checkDay);

            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(14, 14);
            dotParams.setMargins(3, 0, 3, 0);
            dot.setLayoutParams(dotParams);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            if (done) {
                gd.setColor(Color.parseColor("#10B981")); // Accent green
            } else {
                gd.setColor(Color.parseColor("#E2E8F0")); // Light gray
            }
            dot.setBackground(gd);
            dotsLayout.addView(dot);
        }
        footerRow.addView(dotsLayout);

        card.addView(footerRow);

        // Completion checkbox toggle listener
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                habit.toggleCompletion(today);
                storage.updateHabit(habit);
                refreshHabitsData();
            }
        });

        // Open details modal on card click
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHabitDetailsDialog(habit);
            }
        });

        return card;
    }

    private void showAddOrEditHabitDialog(final Habit existingHabit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.tv_dialog_title);
        final EditText etTitle = (EditText) dialogView.findViewById(R.id.et_habit_title);
        final EditText etDesc = (EditText) dialogView.findViewById(R.id.et_habit_description);
        final Spinner spTime = (Spinner) dialogView.findViewById(R.id.spinner_time_of_day);
        final Spinner spCategory = (Spinner) dialogView.findViewById(R.id.spinner_category);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        String[] times = new String[]{"Morning", "Afternoon", "Evening", "Anytime"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, times);
        spTime.setAdapter(timeAdapter);

        String[] categories = new String[]{"Health", "Fitness", "Productivity", "Mind", "Study", "Personal"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(catAdapter);

        if (existingHabit != null) {
            tvDialogTitle.setText("Edit Habit");
            etTitle.setText(existingHabit.getTitle());
            etDesc.setText(existingHabit.getDescription());
            for (int i = 0; i < times.length; i++) {
                if (times[i].equalsIgnoreCase(existingHabit.getTimeOfDay())) {
                    spTime.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(existingHabit.getCategory())) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String time = spTime.getSelectedItem().toString();
                String category = spCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a habit title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existingHabit == null) {
                    Habit newHabit = new Habit(title, desc, time, category);
                    storage.addHabit(newHabit);
                    Toast.makeText(MainActivity.this, "Habit created!", Toast.LENGTH_SHORT).show();
                } else {
                    existingHabit.setTitle(title);
                    existingHabit.setDescription(desc);
                    existingHabit.setTimeOfDay(time);
                    existingHabit.setCategory(category);
                    storage.updateHabit(existingHabit);
                    Toast.makeText(MainActivity.this, "Habit updated!", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                refreshHabitsData();
            }
        });

        dialog.show();
    }

    private void showHabitDetailsDialog(final Habit habit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_habit_details, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvTitle = (TextView) dialogView.findViewById(R.id.tv_detail_title);
        TextView tvCategory = (TextView) dialogView.findViewById(R.id.tv_detail_category);
        TextView tvDesc = (TextView) dialogView.findViewById(R.id.tv_detail_description);
        TextView tvStreak = (TextView) dialogView.findViewById(R.id.tv_detail_streak);
        TextView tvBest = (TextView) dialogView.findViewById(R.id.tv_detail_best);
        TextView tvTotal = (TextView) dialogView.findViewById(R.id.tv_detail_total);

        Button btnDelete = (Button) dialogView.findViewById(R.id.btn_detail_delete);
        Button btnEdit = (Button) dialogView.findViewById(R.id.btn_detail_edit);
        Button btnClose = (Button) dialogView.findViewById(R.id.btn_detail_close);

        tvTitle.setText(habit.getTitle());
        tvCategory.setText(habit.getCategory() + " • " + habit.getTimeOfDay());

        if (habit.getDescription() != null && !habit.getDescription().trim().isEmpty()) {
            tvDesc.setText(habit.getDescription());
            tvDesc.setVisibility(View.VISIBLE);
        } else {
            tvDesc.setVisibility(View.GONE);
        }

        int currentStreak = HabitCalculator.calculateCurrentStreak(habit);
        int bestStreak = HabitCalculator.calculateBestStreak(habit);
        int totalCheckins = HabitCalculator.calculateTotalCheckIns(habit);

        tvStreak.setText(currentStreak + "d");
        tvBest.setText(bestStreak + "d");
        tvTotal.setText(String.valueOf(totalCheckins));

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showAddOrEditHabitDialog(habit);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Habit")
                        .setMessage("Are you sure you want to delete \"" + habit.getTitle() + "\"?")
                        .setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                storage.deleteHabit(habit.getId());
                                dialog.dismiss();
                                refreshHabitsData();
                                Toast.makeText(MainActivity.this, "Habit deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        dialog.show();
    }
}