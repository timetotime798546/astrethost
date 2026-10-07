package com.barberq.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends Activity {

    private TextView btnBackHistory;
    private LinearLayout historyContainer, emptyView;
    private ProgressBar historyProgress;
    private BackendApi api;
    private String userToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        api = new BackendApi(this);

        SharedPreferences prefs = getSharedPreferences("BarberQPrefs", MODE_PRIVATE);
        userToken = prefs.getString("token", "");

        btnBackHistory = (TextView) findViewById(R.id.btnBackHistory);
        historyContainer = (LinearLayout) findViewById(R.id.historyContainer);
        emptyView = (LinearLayout) findViewById(R.id.emptyView);
        historyProgress = (ProgressBar) findViewById(R.id.historyProgress);

        btnBackHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadHistory();
    }

    private void loadHistory() {
        historyProgress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        historyContainer.removeAllViews();

        api.getBookings(userToken, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                historyProgress.setVisibility(View.GONE);
                try {
                    JSONArray records = response.getJSONArray("records");
                    if (records.length() == 0) {
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        JSONObject dataObj = record.getJSONObject("data");

                        addBookingCard(
                            dataObj.optString("customer_name", "N/A"),
                            dataObj.optString("services", "No Treatments"),
                            dataObj.optDouble("total_price", 0.0),
                            dataObj.optString("booking_date", "-"),
                            dataObj.optString("booking_time", "-"),
                            dataObj.optString("status", "Booked")
                        );
                    }
                } catch (Exception e) {
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(HistoryActivity.this, "Failed to parse records", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String error) {
                historyProgress.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                Toast.makeText(HistoryActivity.this, "Request failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addBookingCard(String name, String services, double price, String date, String time, String status) {
        // Design programmatic card layout to ensure stylish dark saloon motif matches original list standard
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32);
        card.setLayoutParams(params);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(32, 32, 32, 32);

        // Customer Info Row
        RelativeLayout topRow = new RelativeLayout(this);
        topRow.setLayoutParams(new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams p1 = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        p1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        tvName.setLayoutParams(p1);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status.toUpperCase());
        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams p2 = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        p2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        tvStatus.setLayoutParams(p2);

        topRow.addView(tvName);
        topRow.addView(tvStatus);
        card.addView(topRow);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dp.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(Color.parseColor("#2D2D2D"));
        card.addView(divider);

        // Services Text
        TextView tvServices = new TextView(this);
        tvServices.setText(services);
        tvServices.setTextColor(Color.parseColor("#BBBBBB"));
        tvServices.setTextSize(14);
        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spParams.setMargins(0, 0, 0, 20);
        tvServices.setLayoutParams(spParams);
        card.addView(tvServices);

        // Bottom Timing and Price details
        RelativeLayout btmRow = new RelativeLayout(this);
        btmRow.setLayoutParams(new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvDate = new TextView(this);
        tvDate.setText(date + "  •  " + time);
        tvDate.setTextColor(Color.parseColor("#888888"));
        tvDate.setTextSize(12);
        RelativeLayout.LayoutParams p3 = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        p3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        p3.addRule(RelativeLayout.CENTER_VERTICAL);
        tvDate.setLayoutParams(p3);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("₹" + (int) price);
        tvPrice.setTextColor(Color.parseColor("#C5A880"));
        tvPrice.setTextSize(16);
        tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams p4 = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        p4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        p4.addRule(RelativeLayout.CENTER_VERTICAL);
        tvPrice.setLayoutParams(p4);

        btmRow.addView(tvDate);
        btmRow.addView(tvPrice);
        card.addView(btmRow);

        historyContainer.addView(card);
    }
}