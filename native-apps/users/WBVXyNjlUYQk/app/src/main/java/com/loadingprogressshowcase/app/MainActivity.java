package com.loadingprogressshowcase.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private Handler mainHandler;
    
    // UI Layout Components
    private LinearLayout overlaySplash;
    private LinearLayout overlayResetting;
    private LinearLayout tasksContainer;
    private TextView tvEmptyState;
    private EditText etTaskName;
    private Button btnAddTask;
    private Button btnResetTasks;
    private ProgressBar progressSaving;
    private CheckBox cbSimulateError;
    
    // Top Error banner components
    private LinearLayout bannerError;
    private Button btnDismissError;

    // Simulated task storage array
    private ArrayList<String> tasksList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        tasksList = new ArrayList<String>();

        // Map Views to XML structures
        overlaySplash = (LinearLayout) findViewById(R.id.overlaySplash);
        overlayResetting = (LinearLayout) findViewById(R.id.overlayResetting);
        tasksContainer = (LinearLayout) findViewById(R.id.tasksContainer);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        etTaskName = (EditText) findViewById(R.id.etTaskName);
        btnAddTask = (Button) findViewById(R.id.btnAddTask);
        btnResetTasks = (Button) findViewById(R.id.btnResetTasks);
        progressSaving = (ProgressBar) findViewById(R.id.progressSaving);
        cbSimulateError = (CheckBox) findViewById(R.id.cbSimulateError);
        bannerError = (LinearLayout) findViewById(R.id.bannerError);
        btnDismissError = (Button) findViewById(R.id.btnDismissError);

        // Prepopulate baseline tasks list
        tasksList.add("Verify responsive UI loops");
        tasksList.add("Incorporate soft animation layouts");
        tasksList.add("Test network database integrity");

        // Action Bindings
        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAddTask();
            }
        });

        btnResetTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetTasks();
            }
        });

        btnDismissError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideErrorBanner();
            }
        });

        // Initialize Splash Loading Screen loop
        runStartupLoading();
    }

    private void runStartupLoading() {
        // App launches with overlaySplash active.
        // Simulate database retrieval delay of 2.5 seconds to meet requirement 1 & 2.
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Task collection populated to UI
                renderTaskList();
                // Dismiss splash screen loader with beautiful transition (Rule 4 & 8)
                fadeOut(overlaySplash);
            }
        }, 2500);
    }

    private void handleAddTask() {
        final String taskTitle = etTaskName.getText().toString().trim();
        if (taskTitle.isEmpty()) {
            etTaskName.setError("Please supply a valid task title");
            return;
        }

        // Hide previous errors instantly
        hideErrorBanner();

        // Rule 7: Disable relevant actions during thread transactions to prevent double taps
        btnAddTask.setEnabled(false);
        etTaskName.setEnabled(false);

        // Rule 5: Show small loader transition when task is being saved
        fadeIn(progressSaving);

        // Simulating background serialization task (1.5 seconds latency) without freezing main thread (Rule 3)
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Handle Simulated Error state (Rule 10)
                if (cbSimulateError.isChecked()) {
                    fadeOut(progressSaving);
                    btnAddTask.setEnabled(true);
                    etTaskName.setEnabled(true);
                    
                    showErrorBanner("Database failure: Connection aborted while saving task '" + taskTitle + "'.");
                } else {
                    // Normal success path
                    tasksList.add(taskTitle);
                    renderTaskList();
                    etTaskName.setText("");

                    // Restore interactions and hide loader smoothly
                    fadeOut(progressSaving);
                    btnAddTask.setEnabled(true);
                    etTaskName.setEnabled(true);
                }
            }
        }, 1500);
    }

    private void handleResetTasks() {
        hideErrorBanner();

        // Prevent dual execution
        btnResetTasks.setEnabled(false);
        btnAddTask.setEnabled(false);

        // Show full screen overlay loading screen (Rule 6 & 8)
        fadeIn(overlayResetting);

        // Simulating heavy file execution (2.0 seconds)
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (cbSimulateError.isChecked()) {
                    // Dismiss load view and present proper error state (Rule 10)
                    fadeOut(overlayResetting);
                    btnResetTasks.setEnabled(true);
                    btnAddTask.setEnabled(true);
                    showErrorBanner("System error: Security verification declined storage formatting command.");
                } else {
                    // Empty list and render update
                    tasksList.clear();
                    renderTaskList();

                    fadeOut(overlayResetting);
                    btnResetTasks.setEnabled(true);
                    btnAddTask.setEnabled(true);
                }
            }
        }, 2000);
    }

    private void renderTaskList() {
        tasksContainer.removeAllViews();
        if (tasksList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            for (int i = 0; i < tasksList.size(); i++) {
                final String taskText = tasksList.get(i);
                
                // Build a custom horizontal layout item dynamically for the task list
                LinearLayout taskRow = new LinearLayout(MainActivity.this);
                taskRow.setOrientation(LinearLayout.HORIZONTAL);
                taskRow.setPadding(12, 16, 12, 16);
                taskRow.setBackgroundColor(0xFFFAFAFA);
                
                if (i > 0) {
                    View divider = new View(MainActivity.this);
                    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    divParams.setMargins(0, 4, 0, 4);
                    divider.setLayoutParams(divParams);
                    divider.setBackgroundColor(0xFFE0E0E0);
                    tasksContainer.addView(divider);
                }

                TextView tvTask = new TextView(MainActivity.this);
                LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tvTask.setLayoutParams(lpText);
                tvTask.setText(taskText);
                tvTask.setTextSize(16);
                tvTask.setTextColor(0xFF333333);

                TextView tvStatus = new TextView(MainActivity.this);
                tvStatus.setText("✓ Saved");
                tvStatus.setTextColor(0xFF4CAF50);
                tvStatus.setTextSize(12);

                taskRow.addView(tvTask);
                taskRow.addView(tvStatus);
                tasksContainer.addView(taskRow);
            }
        }
    }

    // Smooth AlphaAnimation transitions for loaders

    private void fadeIn(final View view) {
        if (view == null || view.getVisibility() == View.VISIBLE) return;
        
        view.setVisibility(View.VISIBLE);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        view.startAnimation(animation);
    }

    private void fadeOut(final View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return;

        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(300);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(animation);
    }

    private void showErrorBanner(String msg) {
        TextView tvErrorLabel = (TextView) bannerError.getChildAt(0);
        if (tvErrorLabel != null) {
            tvErrorLabel.setText("⚠️ " + msg);
        }
        fadeIn(bannerError);
    }

    private void hideErrorBanner() {
        fadeOut(bannerError);
    }
}