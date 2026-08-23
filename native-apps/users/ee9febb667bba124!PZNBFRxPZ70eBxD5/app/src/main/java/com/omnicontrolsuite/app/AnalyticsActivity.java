package com.omnicontrolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnalyticsActivity extends Activity {

    private AnalyticsView analyticsView;
    private TextView txtLogConsole;
    private StringBuilder logBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        analyticsView = (AnalyticsView) findViewById(R.id.analyticsCanvasView);
        txtLogConsole = (TextView) findViewById(R.id.txtLogConsole);
        logBuffer = new StringBuilder();

        appendLog("System diagnostics initialised successfully.");
        appendLog("Canvas plotting rendering dimensions loaded.");

        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnRefresh = (Button) findViewById(R.id.btnRefreshChart);
        Button btnReset = (Button) findViewById(R.id.btnResetMetrics);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsView.generateRandomData();
                appendLog("Regenerated core metrics dataset parameters.");
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsView.clearData();
                appendLog("Purged metrics stream cache index.");
            }
        });
    }

    private void appendLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        logBuffer.append("[").append(timestamp).append("] ").append(message).append("\n");
        txtLogConsole.setText(logBuffer.toString());
    }
}