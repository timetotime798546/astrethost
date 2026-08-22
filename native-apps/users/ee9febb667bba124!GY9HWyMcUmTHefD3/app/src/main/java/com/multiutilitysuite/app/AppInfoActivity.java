package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AppInfoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("System & App Info");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Technical metrics container
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        addCard(layout, "App Name", "Multi Utility Suite");
        addCard(layout, "Implementation Arch", "10 Modular Native Java Files");
        addCard(layout, "Gradle Platform", "8.5 / AGP 8.3.2");
        addCard(layout, "Target SDK Version", "API Level " + Build.VERSION.SDK_INT);
        addCard(layout, "Device Brand", Build.BRAND);
        addCard(layout, "Hardware Model", Build.MODEL);
        addCard(layout, "Java VM Version", System.getProperty("java.vm.version"));
        addCard(layout, "OS Build Profile", Build.DISPLAY);

        scrollView.addView(layout);
        parent.addView(scrollView);
        setContentView(parent);
    }

    private void addCard(LinearLayout root, String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 12);
        card.setLayoutParams(params);

        TextView cardLabel = new TextView(this);
        cardLabel.setText(label);
        cardLabel.setTextSize(12);
        cardLabel.setTextColor(Color.parseColor("#757575"));
        card.addView(cardLabel);

        TextView cardVal = new TextView(this);
        cardVal.setText(value);
        cardVal.setTextSize(18);
        cardVal.setTextColor(Color.BLACK);
        cardVal.setPadding(0, 8, 0, 0);
        card.addView(cardVal);

        root.addView(card);
    }
}