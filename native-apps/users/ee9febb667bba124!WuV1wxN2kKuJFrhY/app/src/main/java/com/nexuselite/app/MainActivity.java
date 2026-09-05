package com.nexuselite.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    // Preferences Keys
    private static final String PREFS_NAME = "NexusElitePrefs";
    private static final String KEY_SOUNDS = "sounds_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_ANIMATIONS = "animations_enabled";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_HISTORY = "history_logs";
    private static final String KEY_TASKS = "tasks_serialized";

    // Dynamic Variables
    private boolean soundsEnabled = true;
    private boolean darkModeEnabled = false;
    private boolean animationsEnabled = true;
    private String userName = "Nexus Client";
    private String userEmail = "client@nexuselite.com";
    private List<String> favoritesList = new ArrayList<String>();
    private List<String> historyList = new ArrayList<String>();
    private List<String> tasksList = new ArrayList<String>(); // formatted "task_name|status" where status is "0" (pending) or "1" (done)
    private List<String> unreadNotifications = new ArrayList<String>();

    // UI elements
    private RelativeLayout mainRoot;
    private RelativeLayout topBar;
    private TextView topBarTitle, topBarSubtitle, profileInitials;
    private RelativeLayout profileBadgeClick;
    private FrameLayout contentContainer;
    
    // Panels
    private ScrollView panelHome, panelFeatures, panelNotifications, panelProfile, panelSettings;
    
    // Bottom Nav Tabs
    private LinearLayout bottomNavigation;
    private LinearLayout tabHome, tabFeatures, tabProfile, tabSettings;
    private RelativeLayout tabNotifications;
    private TextView tabHomeIcon, tabHomeText;
    private TextView tabFeaturesIcon, tabFeaturesText;
    private TextView tabNotificationsIcon, tabNotificationsText, notiBadge;
    private TextView tabProfileIcon, tabProfileText;
    private TextView tabSettingsIcon, tabSettingsText;

    // Splash overlay
    private RelativeLayout splashOverlay;
    private RelativeLayout splashLogoContainer;
    private TextView splashTitle, splashSubtitle, splashStatusText;
    private ProgressBar splashProgress;

    // Home components
    private LinearLayout searchBarLayout;
    private EditText searchEditText;
    private LinearLayout favoritesSection, favoritesContainer;
    private LinearLayout historySection, historyContainer;
    private TextView clearHistoryBtn;
    private TextView titleFavorites, titleHistory, titleFeaturesList;

    // Feature Cards
    private RelativeLayout cardProfile, cardNotifications, cardGoals, cardDiagnostics;
    private TextView favP, favN, favG, favD;
    private TextView txtCardProfileTitle, txtCardProfileDesc;
    private TextView txtCardNotificationsTitle, txtCardNotificationsDesc;
    private TextView txtCardGoalsTitle, txtCardGoalsDesc;
    private TextView txtCardDiagnosticsTitle, txtCardDiagnosticsDesc;

    // Features tab (Goal Tracker)
    private EditText taskEditText;
    private Button addTaskBtn;
    private LinearLayout tasksEmptyState, tasksContainer;
    private TextView titleTasksPanel, descTasksPanel, txtTasksEmptyTitle, txtTasksEmptyDesc;

    // Notifications tab
    private LinearLayout notificationsContainer, notificationsEmptyState;
    private TextView clearNotiBtn, titleNotiPanel, txtNotiEmptyTitle, txtNotiEmptyDesc;

    // Profile Tab
    private RelativeLayout profileImageLargeLayout;
    private TextView profileLargeInitials, displayProfileName, displayProfileEmail, titleCredentials, lblFormName, lblFormEmail;
    private EditText editProfileName, editProfileEmail;
    private Button saveProfileBtn;
    private LinearLayout profileInfoCard;

    // Settings Tab
    private LinearLayout settingsCard, actionsCard, aboutCard;
    private CheckBox switchDarkMode, switchSounds, switchAnimations;
    private Button clearCacheBtn;
    private TextView titleSettingsPanel, txtOptDarkMode, descOptDarkMode, txtOptSounds, descOptSounds, txtOptAnimations, descOptAnimations, txtCacheTitle, txtCacheDesc, txtAboutTitle, txtAboutBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load cached configurations
        loadConfigurations();

        // Bind Views
        initializeViews();

        // Apply visual theme styling
        applyThemeColors();

        // Set Listeners
        setupListeners();

        // Build notifications list with default entries
        initializeDefaultNotifications();

        // Initialize features content views
        renderFavorites();
        renderHistory();
        renderTasks();
        renderNotificationsList();

        // Run the animated launcher splash
        runSplashSequence();
    }

    private void loadConfigurations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        soundsEnabled = prefs.getBoolean(KEY_SOUNDS, true);
        darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, true);
        userName = prefs.getString(KEY_USER_NAME, "Nexus Client");
        userEmail = prefs.getString(KEY_USER_EMAIL, "client@nexuselite.com");

        String favs = prefs.getString(KEY_FAVORITES, "");
        favoritesList.clear();
        if (!favs.trim().isEmpty()) {
            favoritesList.addAll(Arrays.asList(favs.split(",")));
        }

        String hist = prefs.getString(KEY_HISTORY, "Platform Synchronized");
        historyList.clear();
        if (!hist.trim().isEmpty()) {
            historyList.addAll(Arrays.asList(hist.split(",")));
        }

        String tsk = prefs.getString(KEY_TASKS, "");
        tasksList.clear();
        if (!tsk.trim().isEmpty()) {
            tasksList.addAll(Arrays.asList(tsk.split(",")));
        }
    }

    private void saveConfigurations() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SOUNDS, soundsEnabled);
        editor.putBoolean(KEY_DARK_MODE, darkModeEnabled);
        editor.putBoolean(KEY_ANIMATIONS, animationsEnabled);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);

        // serialize favorites
        StringBuilder sbFav = new StringBuilder();
        for (int i = 0; i < favoritesList.size(); i++) {
            sbFav.append(favoritesList.get(i));
            if (i < favoritesList.size() - 1) sbFav.append(",");
        }
        editor.putString(KEY_FAVORITES, sbFav.toString());

        // serialize history
        StringBuilder sbHist = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            sbHist.append(historyList.get(i));
            if (i < historyList.size() - 1) sbHist.append(",");
        }
        editor.putString(KEY_HISTORY, sbHist.toString());

        // serialize tasks
        StringBuilder sbTasks = new StringBuilder();
        for (int i = 0; i < tasksList.size(); i++) {
            sbTasks.append(tasksList.get(i));
            if (i < tasksList.size() - 1) sbTasks.append(",");
        }
        editor.putString(KEY_TASKS, sbTasks.toString());

        editor.apply();
    }

    private void initializeViews() {
        mainRoot = (RelativeLayout) findViewById(R.id.main_root);
        topBar = (RelativeLayout) findViewById(R.id.top_bar);
        topBarTitle = (TextView) findViewById(R.id.top_bar_title);
        topBarSubtitle = (TextView) findViewById(R.id.top_bar_subtitle);
        profileInitials = (TextView) findViewById(R.id.profile_initials);
        profileBadgeClick = (RelativeLayout) findViewById(R.id.profile_badge_click);
        contentContainer = (FrameLayout) findViewById(R.id.content_container);

        // Panels
        panelHome = (ScrollView) findViewById(R.id.panel_home);
        panelFeatures = (ScrollView) findViewById(R.id.panel_features);
        panelNotifications = (ScrollView) findViewById(R.id.panel_notifications);
        panelProfile = (ScrollView) findViewById(R.id.panel_profile);
        panelSettings = (ScrollView) findViewById(R.id.panel_settings);

        // Navigation
        bottomNavigation = (LinearLayout) findViewById(R.id.bottom_navigation);
        tabHome = (LinearLayout) findViewById(R.id.tab_home);
        tabFeatures = (LinearLayout) findViewById(R.id.tab_features);
        tabNotifications = (RelativeLayout) findViewById(R.id.tab_notifications);
        tabProfile = (LinearLayout) findViewById(R.id.tab_profile);
        tabSettings = (LinearLayout) findViewById(R.id.tab_settings);

        tabHomeIcon = (TextView) findViewById(R.id.tab_home_icon);
        tabHomeText = (TextView) findViewById(R.id.tab_home_text);
        tabFeaturesIcon = (TextView) findViewById(R.id.tab_features_icon);
        tabFeaturesText = (TextView) findViewById(R.id.tab_features_text);
        tabNotificationsIcon = (TextView) findViewById(R.id.tab_notifications_icon);
        tabNotificationsText = (TextView) findViewById(R.id.tab_notifications_text);
        notiBadge = (TextView) findViewById(R.id.noti_badge);
        tabProfileIcon = (TextView) findViewById(R.id.tab_profile_icon);
        tabProfileText = (TextView) findViewById(R.id.tab_profile_text);
        tabSettingsIcon = (TextView) findViewById(R.id.tab_settings_icon);
        tabSettingsText = (TextView) findViewById(R.id.tab_settings_text);

        // Splash
        splashOverlay = (RelativeLayout) findViewById(R.id.splash_overlay);
        splashLogoContainer = (RelativeLayout) findViewById(R.id.splash_logo_container);
        splashTitle = (TextView) findViewById(R.id.splash_title);
        splashSubtitle = (TextView) findViewById(R.id.splash_subtitle);
        splashProgress = (ProgressBar) findViewById(R.id.splash_progress);
        splashStatusText = (TextView) findViewById(R.id.splash_status_text);

        // Search and Home listings
        searchBarLayout = (LinearLayout) findViewById(R.id.search_bar_layout);
        searchEditText = (EditText) findViewById(R.id.search_edit_text);
        favoritesSection = (LinearLayout) findViewById(R.id.favorites_section);
        favoritesContainer = (LinearLayout) findViewById(R.id.favorites_container);
        historySection = (LinearLayout) findViewById(R.id.history_section);
        historyContainer = (LinearLayout) findViewById(R.id.history_container);
        clearHistoryBtn = (TextView) findViewById(R.id.clear_history_btn);
        titleFavorites = (TextView) findViewById(R.id.title_favorites);
        titleHistory = (TextView) findViewById(R.id.title_history);
        titleFeaturesList = (TextView) findViewById(R.id.title_features_list);

        // Feature cards
        cardProfile = (RelativeLayout) findViewById(R.id.card_profile);
        cardNotifications = (RelativeLayout) findViewById(R.id.card_notifications);
        cardGoals = (RelativeLayout) findViewById(R.id.card_goals);
        cardDiagnostics = (RelativeLayout) findViewById(R.id.card_diagnostics);

        favP = (TextView) findViewById(R.id.fav_p);
        favN = (TextView) findViewById(R.id.fav_n);
        favG = (TextView) findViewById(R.id.fav_g);
        favD = (TextView) findViewById(R.id.fav_d);

        txtCardProfileTitle = (TextView) findViewById(R.id.txt_card_profile_title);
        txtCardProfileDesc = (TextView) findViewById(R.id.txt_card_profile_desc);
        txtCardNotificationsTitle = (TextView) findViewById(R.id.txt_card_notifications_title);
        txtCardNotificationsDesc = (TextView) findViewById(R.id.txt_card_notifications_desc);
        txtCardGoalsTitle = (TextView) findViewById(R.id.txt_card_goals_title);
        txtCardGoalsDesc = (TextView) findViewById(R.id.txt_card_goals_desc);
        txtCardDiagnosticsTitle = (TextView) findViewById(R.id.txt_card_diagnostics_title);
        txtCardDiagnosticsDesc = (TextView) findViewById(R.id.txt_card_diagnostics_desc);

        // Features tab (Goals)
        taskEditText = (EditText) findViewById(R.id.task_edit_text);
        addTaskBtn = (Button) findViewById(R.id.add_task_btn);
        tasksEmptyState = (LinearLayout) findViewById(R.id.tasks_empty_state);
        tasksContainer = (LinearLayout) findViewById(R.id.tasks_container);
        titleTasksPanel = (TextView) findViewById(R.id.title_tasks_panel);
        descTasksPanel = (TextView) findViewById(R.id.desc_tasks_panel);
        txtTasksEmptyTitle = (TextView) findViewById(R.id.txt_tasks_empty_title);
        txtTasksEmptyDesc = (TextView) findViewById(R.id.txt_tasks_empty_desc);

        // Notifications
        notificationsContainer = (LinearLayout) findViewById(R.id.notifications_container);
        notificationsEmptyState = (LinearLayout) findViewById(R.id.notifications_empty_state);
        clearNotiBtn = (TextView) findViewById(R.id.clear_noti_btn);
        titleNotiPanel = (TextView) findViewById(R.id.title_noti_panel);
        txtNotiEmptyTitle = (TextView) findViewById(R.id.txt_noti_empty_title);
        txtNotiEmptyDesc = (TextView) findViewById(R.id.txt_noti_empty_desc);

        // Profile components
        profileImageLargeLayout = (RelativeLayout) findViewById(R.id.profile_image_large_layout);
        profileLargeInitials = (TextView) findViewById(R.id.profile_large_initials);
        displayProfileName = (TextView) findViewById(R.id.display_profile_name);
        displayProfileEmail = (TextView) findViewById(R.id.display_profile_email);
        titleCredentials = (TextView) findViewById(R.id.title_credentials);
        lblFormName = (TextView) findViewById(R.id.lbl_form_name);
        lblFormEmail = (TextView) findViewById(R.id.lbl_form_email);
        editProfileName = (EditText) findViewById(R.id.edit_profile_name);
        editProfileEmail = (EditText) findViewById(R.id.edit_profile_email);
        saveProfileBtn = (Button) findViewById(R.id.save_profile_btn);
        profileInfoCard = (LinearLayout) findViewById(R.id.profile_info_card);

        // Settings Components
        settingsCard = (LinearLayout) findViewById(R.id.settings_card);
        actionsCard = (LinearLayout) findViewById(R.id.actions_card);
        aboutCard = (LinearLayout) findViewById(R.id.about_card);
        switchDarkMode = (CheckBox) findViewById(R.id.switch_dark_mode);
        switchSounds = (CheckBox) findViewById(R.id.switch_sounds);
        switchAnimations = (CheckBox) findViewById(R.id.switch_animations);
        clearCacheBtn = (Button) findViewById(R.id.clear_cache_btn);

        titleSettingsPanel = (TextView) findViewById(R.id.title_settings_panel);
        txtOptDarkMode = (TextView) findViewById(R.id.txt_opt_dark_mode);
        descOptDarkMode = (TextView) findViewById(R.id.desc_opt_dark_mode);
        txtOptSounds = (TextView) findViewById(R.id.txt_opt_sounds);
        descOptSounds = (TextView) findViewById(R.id.desc_opt_sounds);
        txtOptAnimations = (TextView) findViewById(R.id.txt_opt_animations);
        descOptAnimations = (TextView) findViewById(R.id.desc_opt_animations);
        txtCacheTitle = (TextView) findViewById(R.id.txt_cache_title);
        txtCacheDesc = (TextView) findViewById(R.id.txt_cache_desc);
        txtAboutTitle = (TextView) findViewById(R.id.txt_about_title);
        txtAboutBody = (TextView) findViewById(R.id.txt_about_body);

        // Set initials
        updateInitials();
        updateTopBarWelcome();
    }

    private void updateInitials() {
        String initials = "NE";
        if (userName != null && userName.trim().length() > 0) {
            String[] parts = userName.trim().split(" ");
            if (parts.length > 1) {
                initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else if (parts[0].length() > 1) {
                initials = parts[0].substring(0, 2).toUpperCase();
            } else if (parts[0].length() > 0) {
                initials = parts[0].substring(0, 1).toUpperCase();
            }
        }
        profileInitials.setText(initials);
        profileLargeInitials.setText(initials);
    }

    private void updateTopBarWelcome() {
        topBarSubtitle.setText("Welcome, " + userName);
        displayProfileName.setText(userName);
        displayProfileEmail.setText(userEmail);

        editProfileName.setText(userName);
        editProfileEmail.setText(userEmail);
    }

    private void setupListeners() {
        // Tab switching listeners
        tabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        tabFeatures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        tabNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
        tabProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(4);
            }
        });
        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(5);
            }
        });

        // Topbar Profile badge click redirects to Profile page
        profileBadgeClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(4);
            }
        });

        // Setup feature card clicks on Home screen
        cardProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHistory("Opened Profile Portal");
                switchTab(4);
            }
        });
        cardNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHistory("Opened Notification Portal");
                switchTab(3);
            }
        });
        cardGoals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHistory("Opened Goal Workspace");
                switchTab(2);
            }
        });
        cardDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHistory("Run Diagnostics");
                triggerDiagnosticsPopup();
            }
        });

        // Star click favorites toggles
        favP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite("profile");
            }
        });
        favN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite("notifications");
            }
        });
        favG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite("goals");
            }
        });
        favD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavorite("diagnostics");
            }
        });

        // Search text change watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFeaturedCards(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear history button
        clearHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                historyList.clear();
                saveConfigurations();
                renderHistory();
                Toast.makeText(MainActivity.this, "History workspace flushed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Goal Tracker Add button
        addTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskText = taskEditText.getText().toString().trim();
                if (!taskText.isEmpty()) {
                    playClickSound();
                    // Serialized as taskName|0
                    tasksList.add(taskText + "|0");
                    taskEditText.setText("");
                    saveConfigurations();
                    renderTasks();
                    addHistory("Added Task Goal");
                    Toast.makeText(MainActivity.this, "Task created successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Objective cannot be empty.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Clear Notifications button
        clearNotiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                unreadNotifications.clear();
                renderNotificationsList();
                updateNotificationsBadge();
                Toast.makeText(MainActivity.this, "Clear success.", Toast.LENGTH_SHORT).show();
            }
        });

        // Save profile credentials
        saveProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputName = editProfileName.getText().toString().trim();
                String inputEmail = editProfileEmail.getText().toString().trim();
                if (!inputName.isEmpty() && !inputEmail.isEmpty()) {
                    playClickSound();
                    userName = inputName;
                    userEmail = inputEmail;
                    saveConfigurations();
                    updateInitials();
                    updateTopBarWelcome();
                    addHistory("Modified Profile Card");
                    Toast.makeText(MainActivity.this, "Elite Credentials updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Credentials cannot be blank.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Settings checkbox state changes
        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                darkModeEnabled = isChecked;
                saveConfigurations();
                playClickSound();
                applyThemeColors();
                addHistory(isChecked ? "Activated Night Theme" : "Activated Daylight Theme");
            }
        });

        switchSounds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                soundsEnabled = isChecked;
                saveConfigurations();
                playClickSound();
            }
        });

        switchAnimations.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                animationsEnabled = isChecked;
                saveConfigurations();
                playClickSound();
            }
        });

        // Clear cache completely
        clearCacheBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                loadConfigurations();
                updateInitials();
                updateTopBarWelcome();
                renderFavorites();
                renderHistory();
                renderTasks();
                initializeDefaultNotifications();
                renderNotificationsList();
                applyThemeColors();
                Toast.makeText(MainActivity.this, "Application cache reset successfully.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runSplashSequence() {
        // Run synthesizer sound track
        playStartupSound();

        // Logo Rotate/Scale animation
        if (animationsEnabled) {
            ScaleAnimation animScale = new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animScale.setDuration(1200);
            animScale.setRepeatCount(0);
            splashLogoContainer.startAnimation(animScale);
        }

        // Status logs updater simulation
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                splashStatusText.setText("Loading profile credentials...");
            }
        }, 600);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                splashStatusText.setText("Setting theme engines...");
            }
        }, 1200);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                splashStatusText.setText("Booting Nexus workspace. Welcome.");
            }
        }, 1800);

        // Fade out overlay layout
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (animationsEnabled) {
                    AlphaAnimation animFade = new AlphaAnimation(1.0f, 0.0f);
                    animFade.setDuration(500);
                    animFade.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            splashOverlay.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    splashOverlay.startAnimation(animFade);
                } else {
                    splashOverlay.setVisibility(View.GONE);
                }
            }
        }, 2400);
    }

    private void switchTab(int tabIndex) {
        playClickSound();

        // Reset all tabs UI colors
        tabHomeText.setTextColor(Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B"));
        tabFeaturesText.setTextColor(Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B"));
        tabNotificationsText.setTextColor(Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B"));
        tabProfileText.setTextColor(Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B"));
        tabSettingsText.setTextColor(Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B"));

        // Hide all panels
        panelHome.setVisibility(View.GONE);
        panelFeatures.setVisibility(View.GONE);
        panelNotifications.setVisibility(View.GONE);
        panelProfile.setVisibility(View.GONE);
        panelSettings.setVisibility(View.GONE);

        ScrollView chosenPanel = null;

        switch (tabIndex) {
            case 1:
                chosenPanel = panelHome;
                tabHomeText.setTextColor(Color.parseColor("#3B82F6"));
                break;
            case 2:
                chosenPanel = panelFeatures;
                tabFeaturesText.setTextColor(Color.parseColor("#3B82F6"));
                addHistory("Viewed Goal Settings");
                break;
            case 3:
                chosenPanel = panelNotifications;
                tabNotificationsText.setTextColor(Color.parseColor("#3B82F6"));
                addHistory("Checked Logs");
                break;
            case 4:
                chosenPanel = panelProfile;
                tabProfileText.setTextColor(Color.parseColor("#3B82F6"));
                addHistory("Opened User Card");
                break;
            case 5:
                chosenPanel = panelSettings;
                tabSettingsText.setTextColor(Color.parseColor("#3B82F6"));
                addHistory("Loaded System Options");
                break;
        }

        if (chosenPanel != null) {
            chosenPanel.setVisibility(View.VISIBLE);
            if (animationsEnabled) {
                ScaleAnimation scale = new ScaleAnimation(0.96f, 1.0f, 0.96f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(220);
                chosenPanel.startAnimation(scale);
            }
        }
    }

    private void toggleFavorite(String itemKey) {
        playClickSound();
        if (favoritesList.contains(itemKey)) {
            favoritesList.remove(itemKey);
            addHistory("Removed Favorite Shortcut");
            Toast.makeText(this, "Shortcut unstarred.", Toast.LENGTH_SHORT).show();
        } else {
            favoritesList.add(itemKey);
            addHistory("Saved Favorite Shortcut");
            Toast.makeText(this, "Shortcut starred!", Toast.LENGTH_SHORT).show();
        }
        saveConfigurations();
        renderFavorites();
    }

    private void renderFavorites() {
        // Toggle card visual representation
        favP.setText(favoritesList.contains("profile") ? "★" : "☆");
        favP.setTextColor(Color.parseColor(favoritesList.contains("profile") ? "#F59E0B" : "#3B82F6"));

        favN.setText(favoritesList.contains("notifications") ? "★" : "☆");
        favN.setTextColor(Color.parseColor(favoritesList.contains("notifications") ? "#F59E0B" : "#3B82F6"));

        favG.setText(favoritesList.contains("goals") ? "★" : "☆");
        favG.setTextColor(Color.parseColor(favoritesList.contains("goals") ? "#F59E0B" : "#3B82F6"));

        favD.setText(favoritesList.contains("diagnostics") ? "★" : "☆");
        favD.setTextColor(Color.parseColor(favoritesList.contains("diagnostics") ? "#F59E0B" : "#3B82F6"));

        favoritesContainer.removeAllViews();
        if (favoritesList.isEmpty()) {
            favoritesSection.setVisibility(View.GONE);
            return;
        }

        favoritesSection.setVisibility(View.VISIBLE);
        for (final String item : favoritesList) {
            TextView chip = new TextView(this);
            String title = "";
            if (item.equals("profile")) title = "👤 Profile";
            else if (item.equals("notifications")) title = "🔔 Logs";
            else if (item.equals("goals")) title = "🎯 Goals";
            else if (item.equals("diagnostics")) title = "⚡ System";

            chip.setText(title);
            chip.setTextSize(13);
            chip.setPadding(32, 16, 32, 16);
            chip.setTextColor(Color.parseColor("#FFFFFF"));

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor("#3B82F6"));
            drawable.setCornerRadius(30);
            chip.setBackground(drawable);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.equals("profile")) switchTab(4);
                    else if (item.equals("notifications")) switchTab(3);
                    else if (item.equals("goals")) switchTab(2);
                    else if (item.equals("diagnostics")) triggerDiagnosticsPopup();
                }
            });

            favoritesContainer.addView(chip);
        }
    }

    private void addHistory(String action) {
        if (historyList.size() > 10) {
            historyList.remove(0); // keep it lightweight
        }
        historyList.add(action);
        saveConfigurations();
        renderHistory();
    }

    private void renderHistory() {
        historyContainer.removeAllViews();
        if (historyList.isEmpty()) {
            historySection.setVisibility(View.GONE);
            return;
        }

        historySection.setVisibility(View.VISIBLE);
        for (int i = historyList.size() - 1; i >= 0; i--) {
            final String record = historyList.get(i);
            TextView label = new TextView(this);
            label.setText(record);
            label.setTextSize(11);
            label.setPadding(24, 12, 24, 12);
            label.setTextColor(Color.parseColor(darkModeEnabled ? "#F1F5F9" : "#334155"));

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor(darkModeEnabled ? "#334155" : "#E2E8F0"));
            drawable.setCornerRadius(15);
            label.setBackground(drawable);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 0);
            label.setLayoutParams(params);

            historyContainer.addView(label);
        }
    }

    private void renderTasks() {
        tasksContainer.removeAllViews();
        if (tasksList.isEmpty()) {
            tasksEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tasksEmptyState.setVisibility(View.GONE);
        for (int i = 0; i < tasksList.size(); i++) {
            final int index = i;
            String raw = tasksList.get(i);
            if (!raw.contains("|")) continue;
            
            final String taskName = raw.substring(0, raw.indexOf("|"));
            String status = raw.substring(raw.indexOf("|") + 1);
            final boolean isCompleted = status.equals("1");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(24, 24, 24, 24);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor(darkModeEnabled ? "#1E293B" : "#FFFFFF"));
            drawable.setCornerRadius(12);
            drawable.setStroke(1, Color.parseColor(darkModeEnabled ? "#334155" : "#E2E8F0"));
            row.setBackground(drawable);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 16);
            row.setLayoutParams(rowParams);

            // Complete Toggle
            final CheckBox chk = new CheckBox(this);
            chk.setChecked(isCompleted);
            chk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickSound();
                    String newStatus = chk.isChecked() ? "1" : "0";
                    tasksList.set(index, taskName + "|" + newStatus);
                    saveConfigurations();
                    renderTasks();
                    addHistory(chk.isChecked() ? "Completed Goal" : "Restored Goal Status");
                    Toast.makeText(MainActivity.this, chk.isChecked() ? "Marked completed!" : "Task restored.", Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(chk);

            // Title
            TextView title = new TextView(this);
            title.setText(taskName);
            title.setTextSize(14);
            title.setTextColor(Color.parseColor(darkModeEnabled ? "#F8FAFC" : "#0F172A"));
            if (isCompleted) {
                title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(Color.parseColor("#94A3B8"));
            }
            LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            txtParams.setMargins(16, 0, 16, 0);
            title.setLayoutParams(txtParams);
            row.addView(title);

            // Delete
            TextView deleteBtn = new TextView(this);
            deleteBtn.setText("❌");
            deleteBtn.setTextSize(14);
            deleteBtn.setPadding(8, 8, 8, 8);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickSound();
                    tasksList.remove(index);
                    saveConfigurations();
                    renderTasks();
                    addHistory("Deleted Goal Task");
                    Toast.makeText(MainActivity.this, "Goal removed.", Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(deleteBtn);

            tasksContainer.addView(row);
        }
    }

    private void initializeDefaultNotifications() {
        if (unreadNotifications.isEmpty()) {
            unreadNotifications.add("Security update active: All network portals are fully configured under system protocol 3.4.0.");
            unreadNotifications.add("Startup successful: Premium wave sound engine synthesized successfully on memory module.");
            unreadNotifications.add("Database verified: Local data synchronization cache loaded without redundancy errors.");
        }
        updateNotificationsBadge();
    }

    private void updateNotificationsBadge() {
        if (unreadNotifications.isEmpty()) {
            notiBadge.setVisibility(View.GONE);
        } else {
            notiBadge.setVisibility(View.VISIBLE);
            notiBadge.setText(String.valueOf(unreadNotifications.size()));
        }
    }

    private void renderNotificationsList() {
        notificationsContainer.removeAllViews();
        if (unreadNotifications.isEmpty()) {
            notificationsEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        notificationsEmptyState.setVisibility(View.GONE);
        for (int i = 0; i < unreadNotifications.size(); i++) {
            final int index = i;
            String text = unreadNotifications.get(i);

            LinearLayout alertCard = new LinearLayout(this);
            alertCard.setOrientation(LinearLayout.VERTICAL);
            alertCard.setPadding(24, 24, 24, 24);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor(darkModeEnabled ? "#1E293B" : "#FFFFFF"));
            drawable.setCornerRadius(12);
            drawable.setStroke(1, Color.parseColor(darkModeEnabled ? "#334155" : "#E2E8F0"));
            alertCard.setBackground(drawable);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            alertCard.setLayoutParams(cardParams);

            TextView banner = new TextView(this);
            banner.setText("SYSTEM ALERT LOG");
            banner.setTextSize(10);
            banner.setLetterSpacing(0.1f);
            banner.setTextColor(Color.parseColor("#3B82F6"));
            banner.setPadding(0, 0, 0, 8);
            alertCard.addView(banner);

            TextView body = new TextView(this);
            body.setText(text);
            body.setTextSize(13);
            body.setTextColor(Color.parseColor(darkModeEnabled ? "#F1F5F9" : "#334155"));
            alertCard.addView(body);

            // Quick Dismiss option
            TextView dismiss = new TextView(this);
            dismiss.setText("Dismiss Alert");
            dismiss.setTextSize(11);
            dismiss.setTextColor(Color.parseColor("#EF4444"));
            dismiss.setPadding(0, 12, 0, 0);
            dismiss.setGravity(android.view.Gravity.RIGHT);
            dismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickSound();
                    unreadNotifications.remove(index);
                    renderNotificationsList();
                    updateNotificationsBadge();
                    Toast.makeText(MainActivity.this, "Log dismissed.", Toast.LENGTH_SHORT).show();
                }
            });
            alertCard.addView(dismiss);

            notificationsContainer.addView(alertCard);
        }
    }

    private void filterFeaturedCards(String query) {
        String match = query.toLowerCase().trim();
        if (match.isEmpty()) {
            cardProfile.setVisibility(View.VISIBLE);
            cardNotifications.setVisibility(View.VISIBLE);
            cardGoals.setVisibility(View.VISIBLE);
            cardDiagnostics.setVisibility(View.VISIBLE);
            return;
        }

        cardProfile.setVisibility(txtCardProfileTitle.getText().toString().toLowerCase().contains(match) ? View.VISIBLE : View.GONE);
        cardNotifications.setVisibility(txtCardNotificationsTitle.getText().toString().toLowerCase().contains(match) ? View.VISIBLE : View.GONE);
        cardGoals.setVisibility(txtCardGoalsTitle.getText().toString().toLowerCase().contains(match) ? View.VISIBLE : View.GONE);
        cardDiagnostics.setVisibility(txtCardDiagnosticsTitle.getText().toString().toLowerCase().contains(match) ? View.VISIBLE : View.GONE);
    }

    private void triggerDiagnosticsPopup() {
        playClickSound();
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("⚡ Diagnostic Report");
        
        StringBuilder details = new StringBuilder();
        details.append("Platform Architecture: Java Standard Runtime\n");
        details.append("SDK Target Level: API 34 Compliant\n");
        details.append("Cache Registry Limit: 1048 Kb (100% OK)\n");
        details.append("Unread Notifications Logs: ").append(unreadNotifications.size()).append("\n");
        details.append("Sound Wave Driver: Dynamic AudioTrack (ON)\n");
        details.append("Theme Index Layout: ").append(darkModeEnabled ? "DARK MODE ACTIVE" : "LIGHT MODE ACTIVE");

        builder.setMessage(details.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    // Programmatic Wave Sound Synthesizers
    private void playStartupSound() {
        if (!soundsEnabled) return;
        // Synthesizes a beautiful ascending electronic ambient chord (C5 - E5 - G5 - C6 arpeggio)
        double[] freqs = new double[]{523.25, 659.25, 783.99, 1046.50};
        int[] durations = new int[]{150, 150, 150, 350};
        playSynthSound(freqs, durations);
    }

    private void playClickSound() {
        if (!soundsEnabled) return;
        // Elegant short high click (E6 note for 35ms)
        double[] freqs = new double[]{1318.51};
        int[] durations = new int[]{35};
        playSynthSound(freqs, durations);
    }

    private void playSynthSound(final double[] frequencies, final int[] durationsMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 16000;
                    int totalSamples = 0;
                    for (int d : durationsMs) {
                        totalSamples += (sampleRate * d) / 1000;
                    }
                    short[] buffer = new short[totalSamples];
                    int bufferOffset = 0;

                    for (int i = 0; i < frequencies.length; i++) {
                        double freq = frequencies[i];
                        int durMs = durationsMs[i];
                        int samples = (sampleRate * durMs) / 1000;

                        for (int j = 0; j < samples; j++) {
                            double angle = 2.0 * Math.PI * j / (sampleRate / freq);
                            double fade = 1.0;
                            // Apply smooth fade out to eliminate sudden cut clicking sound
                            if (j > samples - 300) {
                                fade = (double) (samples - j) / 300.0;
                            }
                            buffer[bufferOffset + j] = (short) (Math.sin(angle) * 32767.0 * 0.3 * fade);
                        }
                        bufferOffset += samples;
                    }

                    android.media.AudioTrack audioTrack = new android.media.AudioTrack(
                            android.media.AudioManager.STREAM_MUSIC,
                            sampleRate,
                            android.media.AudioFormat.CHANNEL_OUT_MONO,
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            totalSamples * 2,
                            android.media.AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(buffer, 0, totalSamples);
                    audioTrack.play();
                    Thread.sleep(totalSamples * 1000 / sampleRate + 150);
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Runtime Interactive Color Palette Theme Switcher
    private void applyThemeColors() {
        // Set checkbox initial state without re-triggering listener
        switchDarkMode.setChecked(darkModeEnabled);
        switchSounds.setChecked(soundsEnabled);
        switchAnimations.setChecked(animationsEnabled);

        int primaryColor = Color.parseColor("#3B82F6");
        int bgCol = Color.parseColor(darkModeEnabled ? "#0F172A" : "#F8FAFC");
        int cardCol = Color.parseColor(darkModeEnabled ? "#1E293B" : "#FFFFFF");
        int strokeCol = Color.parseColor(darkModeEnabled ? "#334155" : "#E2E8F0");
        int textHeaderCol = Color.parseColor(darkModeEnabled ? "#F8FAFC" : "#0F172A");
        int textSubCol = Color.parseColor(darkModeEnabled ? "#94A3B8" : "#64748B");

        // Main Backgrounds
        mainRoot.setBackgroundColor(bgCol);
        topBar.setBackgroundColor(cardCol);
        bottomNavigation.setBackgroundColor(cardCol);

        // Header
        topBarTitle.setTextColor(textHeaderCol);
        topBarSubtitle.setTextColor(textSubCol);

        // Inputs
        searchEditText.setTextColor(textHeaderCol);
        searchEditText.setHintTextColor(textSubCol);
        
        // Dynamic search input box
        GradientDrawable searchDrawable = new GradientDrawable();
        searchDrawable.setColor(Color.parseColor(darkModeEnabled ? "#0F172A" : "#F1F5F9"));
        searchDrawable.setCornerRadius(12);
        searchDrawable.setStroke(1, strokeCol);
        searchBarLayout.setBackground(searchDrawable);

        // Lists title texts
        titleFavorites.setTextColor(textHeaderCol);
        titleHistory.setTextColor(textHeaderCol);
        titleFeaturesList.setTextColor(textHeaderCol);
        titleTasksPanel.setTextColor(textHeaderCol);
        descTasksPanel.setTextColor(textSubCol);
        txtTasksEmptyTitle.setTextColor(textHeaderCol);
        txtTasksEmptyDesc.setTextColor(textSubCol);
        clearHistoryBtn.setTextColor(primaryColor);

        // Task edit box
        GradientDrawable taskInputDrawable = new GradientDrawable();
        taskInputDrawable.setColor(Color.parseColor(darkModeEnabled ? "#0F172A" : "#F1F5F9"));
        taskInputDrawable.setCornerRadius(10);
        taskInputDrawable.setStroke(1, strokeCol);
        taskEditText.setBackground(taskInputDrawable);
        taskEditText.setTextColor(textHeaderCol);
        taskEditText.setHintTextColor(textSubCol);

        // Feature cards
        applyCardBackground(cardProfile, cardCol, strokeCol);
        applyCardBackground(cardNotifications, cardCol, strokeCol);
        applyCardBackground(cardGoals, cardCol, strokeCol);
        applyCardBackground(cardDiagnostics, cardCol, strokeCol);

        txtCardProfileTitle.setTextColor(textHeaderCol);
        txtCardProfileDesc.setTextColor(textSubCol);
        txtCardNotificationsTitle.setTextColor(textHeaderCol);
        txtCardNotificationsDesc.setTextColor(textSubCol);
        txtCardGoalsTitle.setTextColor(textHeaderCol);
        txtCardGoalsDesc.setTextColor(textSubCol);
        txtCardDiagnosticsTitle.setTextColor(textHeaderCol);
        txtCardDiagnosticsDesc.setTextColor(textSubCol);

        // Notifications Screen styling
        titleNotiPanel.setTextColor(textHeaderCol);
        clearNotiBtn.setTextColor(primaryColor);
        txtNotiEmptyTitle.setTextColor(textHeaderCol);
        txtNotiEmptyDesc.setTextColor(textSubCol);

        // Profile Screen Styling
        displayProfileName.setTextColor(textHeaderCol);
        displayProfileEmail.setTextColor(textSubCol);
        titleCredentials.setTextColor(textHeaderCol);
        lblFormName.setTextColor(textSubCol);
        lblFormEmail.setTextColor(textSubCol);
        applyCardBackground(profileInfoCard, cardCol, strokeCol);
        
        // form textfields
        GradientDrawable formInputName = new GradientDrawable();
        formInputName.setColor(Color.parseColor(darkModeEnabled ? "#0F172A" : "#F1F5F9"));
        formInputName.setCornerRadius(10);
        formInputName.setStroke(1, strokeCol);
        editProfileName.setBackground(formInputName);
        editProfileName.setTextColor(textHeaderCol);

        GradientDrawable formInputEmail = new GradientDrawable();
        formInputEmail.setColor(Color.parseColor(darkModeEnabled ? "#0F172A" : "#F1F5F9"));
        formInputEmail.setCornerRadius(10);
        formInputEmail.setStroke(1, strokeCol);
        editProfileEmail.setBackground(formInputEmail);
        editProfileEmail.setTextColor(textHeaderCol);

        // Settings Screen styling
        titleSettingsPanel.setTextColor(textHeaderCol);
        txtOptDarkMode.setTextColor(textHeaderCol);
        descOptDarkMode.setTextColor(textSubCol);
        txtOptSounds.setTextColor(textHeaderCol);
        descOptSounds.setTextColor(textSubCol);
        txtOptAnimations.setTextColor(textHeaderCol);
        descOptAnimations.setTextColor(textSubCol);
        txtCacheTitle.setTextColor(textHeaderCol);
        txtCacheDesc.setTextColor(textSubCol);
        txtAboutTitle.setTextColor(textHeaderCol);
        txtAboutBody.setTextColor(textSubCol);

        applyCardBackground(settingsCard, cardCol, strokeCol);
        applyCardBackground(actionsCard, cardCol, strokeCol);
        applyCardBackground(aboutCard, cardCol, strokeCol);

        // Re-draw tasks, notifications, and history to respect active color modifications
        renderHistory();
        renderTasks();
        renderNotificationsList();
    }

    private void applyCardBackground(View view, int solidColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(solidColor);
        drawable.setCornerRadius(14);
        drawable.setStroke(1, strokeColor);
        view.setBackground(drawable);
    }
}