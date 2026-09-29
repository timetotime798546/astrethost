package com.procalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        final LinearLayout container = (LinearLayout) findViewById(R.id.historyContainer);
        BackendApi api = new BackendApi(this);

        api.getHistory(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray records = result.getJSONArray("records");
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                JSONObject data = record.getJSONObject("data");
                                String expr = data.getString("expression");
                                String res = data.getString("result");

                                TextView tv = new TextView(HistoryActivity.this);
                                tv.setText(expr + " = " + res);
                                // FIXED: Removed 'sp' suffix which is invalid in Java code
                                tv.setTextSize(18); 
                                tv.setPadding(0, 8, 0, 8);
                                container.addView(tv, 0); // Newest first
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(HistoryActivity.this, "Failed to load history: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}