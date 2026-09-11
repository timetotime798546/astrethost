package com.welcomeloader.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView logoIcon;
    private TextView welcomeTitle;
    private TextView welcomeSubtitle;
    private ProgressBar loaderProgress;
    private TextView loaderStatus;
    private Button actionButton;

    private Handler handler;
    private Runnable runnable1;
    private Runnable runnable2;
    private Runnable runnable3;
    private Runnable runnable4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoIcon = (TextView) findViewById(R.id.logo_icon);
        welcomeTitle = (TextView) findViewById(R.id.welcome_title);
        welcomeSubtitle = (TextView) findViewById(R.id.welcome_subtitle);
        loaderProgress = (ProgressBar) findViewById(R.id.loader_progress);
        loaderStatus = (TextView) findViewById(R.id.loader_status);
        actionButton = (Button) findViewById(R.id.action_button);

        handler = new Handler();

        startWelcomeSequence();

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Resetting loading sequence...", Toast.LENGTH_SHORT).show();
                resetSequence();
            }
        });
    }

    private void startWelcomeSequence() {
        // Prepare original loading layouts
        logoIcon.setText("👋");
        welcomeTitle.setText("Welcome!");
        welcomeSubtitle.setText("We are preparing things for you.");
        loaderProgress.setVisibility(View.VISIBLE);
        loaderStatus.setVisibility(View.VISIBLE);
        loaderStatus.setText("Connecting to secure server...");
        actionButton.setVisibility(View.GONE);

        // Run transition steps safely using vanilla Java Runnable implementations
        runnable1 = new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Loading secure credentials...");
            }
        };
        handler.postDelayed(runnable1, 1500);

        runnable2 = new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Syncing user account configuration...");
            }
        };
        handler.postDelayed(runnable2, 3000);

        runnable3 = new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Finalizing details...");
            }
        };
        handler.postDelayed(runnable3, 4500);

        runnable4 = new Runnable() {
            @Override
            public void run() {
                // Dismiss loading indicators and finalize UI states
                loaderProgress.setVisibility(View.GONE);
                loaderStatus.setVisibility(View.GONE);

                logoIcon.setText("🚀");
                welcomeTitle.setText("Welcome Aboard!");
                welcomeSubtitle.setText("The application is initialized and fully active.");

                actionButton.setText("Restart Sequence");
                actionButton.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(runnable4, 5500);
    }

    private void resetSequence() {
        // Securely cancel any callbacks in progress to prevent leaks
        if (handler != null) {
            handler.removeCallbacks(runnable1);
            handler.removeCallbacks(runnable2);
            handler.removeCallbacks(runnable3);
            handler.removeCallbacks(runnable4);
        }
        startWelcomeSequence();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}