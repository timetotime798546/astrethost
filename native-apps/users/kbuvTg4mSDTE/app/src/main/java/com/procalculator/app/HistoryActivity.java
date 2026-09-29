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
                            // Backend generic /data GET returns "records" array
                            JSONArray records = result.getJSONArray("records");
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                // Application fields are nested inside "data" object
                                JSONObject data = record.getJSONObject("data");
                                String expr = data.optString("expression", "");
                                String res = data.optString("result", "");

                                TextView tv = new TextView(HistoryActivity.this);
                                tv.setText(expr + " = " + res);
                                tv.setTextSize(18); // Value in SP
                                tv.setPadding(0, 16, 0, 16);
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