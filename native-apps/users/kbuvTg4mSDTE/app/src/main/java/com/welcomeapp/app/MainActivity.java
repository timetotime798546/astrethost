package com.welcomeapp.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "WelcomeAppPrefs";
    private static final String KEY_USER_NAME = "key_user_name";
    private static final String KEY_STEP1 = "key_step1";
    private static final String KEY_STEP2 = "key_step2";
    private static final String KEY_STEP3 = "key_step3";

    private TextView tvTimeGreeting;
    private TextView tvHeroTitle;
    private TextView tvHeroSubtitle;
    private TextView tvProgressPercent;
    private ProgressBar pbOnboarding;
    private CheckBox cbStep1;
    private CheckBox cbStep2;
    private CheckBox cbStep3;
    private EditText etUserName;
    private Button btnSaveName;
    private TextView tvQuoteDisplay;
    private Button btnNewQuote;
    private Button btnResetTour;

    private SharedPreferences preferences;
    private int quoteIndex = 0;

    private final String[] quotes = new String[]{
            "\"Great things are not done by impulse, but by a series of small things brought together.\" – Vincent Van Gogh",
            "\"Simplicity is prerequisite for reliability.\" – Edsger W. Dijkstra",
            "\"The secret of getting ahead is getting started.\" – Mark Twain",
            "\"Code is like humor. When you have to explain it, it’s bad.\" – Cory House",
            "\"Stay hungry, stay foolish.\" – Steve Jobs",
            "\"The only way to do great work is to love what you do.\" – Steve Jobs"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        applyDynamicTimeGreeting();
        loadSavedData();
        setupListeners();
        updateProgress();
    }

    private void initViews() {
        tvTimeGreeting = (TextView) findViewById(R.id.tv_time_greeting);
        tvHeroTitle = (TextView) findViewById(R.id.tv_hero_title);
        tvHeroSubtitle = (TextView) findViewById(R.id.tv_hero_subtitle);
        tvProgressPercent = (TextView) findViewById(R.id.tv_progress_percent);
        pbOnboarding = (ProgressBar) findViewById(R.id.pb_onboarding);
        cbStep1 = (CheckBox) findViewById(R.id.cb_step1);
        cbStep2 = (CheckBox) findViewById(R.id.cb_step2);
        cbStep3 = (CheckBox) findViewById(R.id.cb_step3);
        etUserName = (EditText) findViewById(R.id.et_user_name);
        btnSaveName = (Button) findViewById(R.id.btn_save_name);
        tvQuoteDisplay = (TextView) findViewById(R.id.tv_quote_display);
        btnNewQuote = (Button) findViewById(R.id.btn_new_quote);
        btnResetTour = (Button) findViewById(R.id.btn_reset_tour);
    }

    private void applyDynamicTimeGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String periodGreeting;
        if (hour >= 5 && hour < 12) {
            periodGreeting = "GOOD MORNING";
        } else if (hour >= 12 && hour < 17) {
            periodGreeting = "GOOD AFTERNOON";
        } else if (hour >= 17 && hour < 22) {
            periodGreeting = "GOOD EVENING";
        } else {
            periodGreeting = "WELCOME & REST WELL";
        }

        tvTimeGreeting.setText(periodGreeting);
    }

    private void loadSavedData() {
        String savedName = preferences.getString(KEY_USER_NAME, "");
        if (!savedName.isEmpty()) {
            tvHeroTitle.setText("Welcome, " + savedName + "!");
            etUserName.setText(savedName);
        } else {
            tvHeroTitle.setText("Welcome, Explorer!");
        }

        cbStep1.setChecked(preferences.getBoolean(KEY_STEP1, false));
        cbStep2.setChecked(preferences.getBoolean(KEY_STEP2, false));
        cbStep3.setChecked(preferences.getBoolean(KEY_STEP3, false));
    }

    private void setupListeners() {
        btnSaveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etUserName.getText().toString().trim();
                if (input.isEmpty()) {
                    input = "Explorer";
                }

                preferences.edit().putString(KEY_USER_NAME, input).apply();
                tvHeroTitle.setText("Welcome, " + input + "!");

                if (!cbStep1.isChecked()) {
                    cbStep1.setChecked(true);
                }

                Toast.makeText(MainActivity.this, "Welcome " + input + "! Your dashboard is updated.", Toast.LENGTH_SHORT).show();
            }
        });

        CompoundButton.OnCheckedChangeListener checkChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit()
                        .putBoolean(KEY_STEP1, cbStep1.isChecked())
                        .putBoolean(KEY_STEP2, cbStep2.isChecked())
                        .putBoolean(KEY_STEP3, cbStep3.isChecked())
                        .apply();
                updateProgress();
            }
        };

        cbStep1.setOnCheckedChangeListener(checkChangeListener);
        cbStep2.setOnCheckedChangeListener(checkChangeListener);
        cbStep3.setOnCheckedChangeListener(checkChangeListener);

        btnNewQuote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quoteIndex = (quoteIndex + 1) % quotes.length;
                tvQuoteDisplay.setText(quotes[quoteIndex]);

                if (!cbStep3.isChecked()) {
                    cbStep3.setChecked(true);
                }
            }
        });

        btnResetTour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferences.edit().clear().apply();
                etUserName.setText("");
                cbStep1.setChecked(false);
                cbStep2.setChecked(false);
                cbStep3.setChecked(false);
                tvHeroTitle.setText("Welcome, Explorer!");
                quoteIndex = 0;
                tvQuoteDisplay.setText(quotes[0]);
                updateProgress();
                Toast.makeText(MainActivity.this, "Tour reset to beginning.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgress() {
        int count = 0;
        if (cbStep1.isChecked()) count++;
        if (cbStep2.isChecked()) count++;
        if (cbStep3.isChecked()) count++;

        int percentage = (count * 100) / 3;
        pbOnboarding.setProgress(percentage);
        tvProgressPercent.setText(percentage + "% Complete");
    }
}