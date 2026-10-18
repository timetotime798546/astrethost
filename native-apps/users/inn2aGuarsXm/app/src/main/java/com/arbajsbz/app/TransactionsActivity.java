package com.arbajsbz.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class TransactionsActivity extends Activity {

    private LinearLayout containerList;
    private TextView tvEmpty;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        backendApi = new BackendApi(this);
        containerList = (LinearLayout) findViewById(R.id.container_tx_list);
        tvEmpty = (TextView) findViewById(R.id.tv_tx_empty);

        findViewById(R.id.btn_tx_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadTransactions();
    }

    private void loadTransactions() {
        backendApi.readRecords("transactions", new BackendApi.ReadCallback() {
            @Override
            public void onSuccess(JSONArray records) {
                containerList.removeAllViews();
                if (records.length() == 0) {
                    tvEmpty.setText("No transactions recorded yet.");
                    containerList.addView(tvEmpty);
                    return;
                }

                try {
                    for (int i = records.length() - 1; i >= 0; i--) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.optJSONObject("data");
                        if (data != null) {
                            addTransactionView(data);
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message) {
                tvEmpty.setText("Failed to load records: " + message);
            }
        });
    }

    private void addTransactionView(JSONObject tx) {
        String category = tx.optString("category", "TX");
        String ref = tx.optString("tx_ref", "TX-0");
        double amount = tx.optDouble("amount", 0.0);
        String status = tx.optString("status", "COMPLETED");
        String date = tx.optString("timestamp", "");
        String details = tx.optString("details", "");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setPadding(24, 20, 24, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        // Header Row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvCat = new TextView(this);
        tvCat.setText(category + " (" + status + ")");
        tvCat.setTextColor(Color.WHITE);
        tvCat.setTextSize(14f);
        tvCat.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams pCat = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        topRow.addView(tvCat, pCat);

        TextView tvAmt = new TextView(this);
        String prefix = amount > 0 ? "+ ₹" : "- ₹";
        tvAmt.setText(prefix + String.format(Locale.US, "%.2f", Math.abs(amount)));
        tvAmt.setTextSize(15f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        if (amount > 0) {
            tvAmt.setTextColor(getResources().getColor(R.color.accent_green));
        } else {
            tvAmt.setTextColor(getResources().getColor(R.color.accent_red_bright));
        }
        topRow.addView(tvAmt);

        card.addView(topRow);

        TextView tvRef = new TextView(this);
        tvRef.setText("Ref: " + ref + " • " + date);
        tvRef.setTextColor(getResources().getColor(R.color.text_secondary));
        tvRef.setTextSize(11f);
        tvRef.setPadding(0, 8, 0, 4);
        card.addView(tvRef);

        if (!details.isEmpty()) {
            TextView tvDet = new TextView(this);
            tvDet.setText(details);
            tvDet.setTextColor(getResources().getColor(R.color.metallic_silver));
            tvDet.setTextSize(12f);
            card.addView(tvDet);
        }

        containerList.addView(card);
    }
}