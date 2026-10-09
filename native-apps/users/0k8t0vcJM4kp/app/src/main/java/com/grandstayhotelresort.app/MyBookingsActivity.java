package com.grandstayhotelresort.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class MyBookingsActivity extends Activity {
    private TextView tvMyBookingsBack, tvSummaryCalc;
    private LinearLayout containerBookings;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        api = new BackendApi(this);

        tvMyBookingsBack = (TextView) findViewById(R.id.tvMyBookingsBack);
        tvSummaryCalc = (TextView) findViewById(R.id.tvSummaryCalc);
        containerBookings = (LinearLayout) findViewById(R.id.containerBookings);

        tvMyBookingsBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadBookingRecords();
    }

    private void loadBookingRecords() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Retreiving your reservations...");
        pd.setCancelable(false);
        pd.show();

        api.readRecords("bookings", new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                pd.dismiss();
                renderBookingsList(result);
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(MyBookingsActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderBookingsList(JSONArray list) {
        containerBookings.removeAllViews();

        if (list == null || list.length() == 0) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("You have no active GrandStay reservations yet.");
            emptyTv.setTextColor(0xFF666666);
            emptyTv.setTextSize(14);
            emptyTv.setPadding(0, 48, 0, 0);
            emptyTv.setGravity(android.view.Gravity.CENTER);
            containerBookings.addView(emptyTv);

            tvSummaryCalc.setText("Total Reserved: $0.00 across 0 bookings");
            return;
        }

        // MANDATORY OFFLINE CALCULATION: Aggregate totals over fetched records locally
        double sumTotalSpent = 0.0;
        int totalBookingsCount = list.length();

        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject obj = list.getJSONObject(i);
                final String recordId = obj.optString("id");
                
                // Get inner application data
                JSONObject data = obj.getJSONObject("data");

                String suiteName = data.optString("room_name", "Luxury Suite");
                double pricePerNight = data.optDouble("price_per_night", 0.0);
                String checkIn = data.optString("check_in_date", "N/A");
                String checkOut = data.optString("check_out_date", "N/A");
                int nights = data.optInt("number_of_nights", 1);
                double totalCost = data.optDouble("total_price", 0.0);
                String requests = data.optString("special_requests", "");

                sumTotalSpent += totalCost;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(0xFFFFFFFF);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 0, 0, 24);
                card.setLayoutParams(lp);
                card.setPadding(20, 20, 20, 20);

                // Suite Name
                TextView titleTv = new TextView(this);
                titleTv.setText(suiteName);
                titleTv.setTextColor(0xFF1F2E3D);
                titleTv.setTextSize(16);
                titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                card.addView(titleTv);

                // Dates info
                TextView datesTv = new TextView(this);
                datesTv.setText("Calendar: " + checkIn + " to " + checkOut + " (" + nights + " nights)");
                datesTv.setTextColor(0xFF666666);
                datesTv.setTextSize(13);
                datesTv.setPadding(0, 8, 0, 4);
                card.addView(datesTv);

                // Costs details
                TextView costTv = new TextView(this);
                costTv.setText("Rate: $" + (int) pricePerNight + " / night   •   Total Paid: $" + String.format(Locale.US, "%.2f", totalCost));
                costTv.setTextColor(0xFFD4AF37);
                costTv.setTextSize(13);
                costTv.setTypeface(null, android.graphics.Typeface.BOLD);
                costTv.setPadding(0, 0, 0, 8);
                card.addView(costTv);

                if (!requests.isEmpty()) {
                    TextView reqTv = new TextView(this);
                    reqTv.setText("Requests: " + requests);
                    reqTv.setTextColor(0xFF888888);
                    reqTv.setTextSize(12);
                    reqTv.setPadding(0, 0, 0, 8);
                    card.addView(reqTv);
                }

                // Cancel reservation action
                Button cancelBtn = new Button(this);
                cancelBtn.setText("CANCEL RESERVATION");
                cancelBtn.setTextColor(0xFFFFFFFF);
                cancelBtn.setBackgroundResource(R.drawable.button_gold);
                
                // Style slightly distinct for warning / cancellation
                cancelBtn.setPadding(12, 4, 12, 4);
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    80
                );
                cancelBtn.setLayoutParams(btnLp);
                cancelBtn.setTextSize(11);
                
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestCancellation(recordId);
                    }
                });
                card.addView(cancelBtn);

                containerBookings.addView(card);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Render calculated sum results
        tvSummaryCalc.setText(String.format(Locale.US, "Total Reserved: $%.2f across %d premium bookings", sumTotalSpent, totalBookingsCount));
    }

    private void requestCancellation(String recordId) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Processing room cancellation...");
        pd.setCancelable(false);
        pd.show();

        api.deleteRecord(recordId, new BackendApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                pd.dismiss();
                Toast.makeText(MyBookingsActivity.this, "Reservation cancelled successfully.", Toast.LENGTH_SHORT).show();
                loadBookingRecords(); // Reload
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(MyBookingsActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}