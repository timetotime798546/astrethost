package com.procalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
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
                            if (records.length() == 0) {
                                TextView empty = new TextView(HistoryActivity.this);
                                empty.setText("No history found");
                                empty.setTextColor(getResources().getColor(R.color.text_secondary)); // Use color resource
                                empty.setGravity(Gravity.CENTER);
                                empty.setPadding(0, 50, 0, 0); // Add some padding for better centering
                                container.addView(empty);
                                return;
                            }

                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                JSONObject data = record.getJSONObject("data");
                                String expr = data.optString("expression", "");
                                String res = data.optString("result", "");

                                LinearLayout itemLayout = new LinearLayout(HistoryActivity.this);
                                itemLayout.setOrientation(LinearLayout.VERTICAL);
                                itemLayout.setPadding(16, 24, 16, 24);
                                // Use drawable for background instead of hardcoded color
                                itemLayout.setBackgroundResource(R.drawable.history_item_background);
                                
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 0, 16); // Increased margin between items
                                itemLayout.setLayoutParams(params);

                                TextView tvExpr = new TextView(HistoryActivity.this);
                                tvExpr.setText(expr);
                                tvExpr.setTextColor(getResources().getColor(R.color.text_secondary)); // Use color resource
                                tvExpr.setTextSize(14);
                                
                                TextView tvRes = new TextView(HistoryActivity.this);
                                tvRes.setText("= " + res);
                                tvRes.setTextColor(getResources().getColor(R.color.text_primary)); // Use color resource
                                tvRes.setTextSize(22);
                                tvRes.setGravity(Gravity.END);

                                itemLayout.addView(tvExpr);
                                itemLayout.addView(tvRes);
                                container.addView(itemLayout, 0);
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
                        Toast.makeText(HistoryActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}