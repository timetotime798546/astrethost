package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentsActivity extends Activity {

    private EditText etPayMember;
    private EditText etPayAmount;
    private Button btnSavePayment;
    private ProgressBar pbPayments;
    private ListView lvPayments;

    private BackendApi backendApi;
    private List<String> paymentItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        backendApi = new BackendApi(this);

        etPayMember = (EditText) findViewById(R.id.etPayMember);
        etPayAmount = (EditText) findViewById(R.id.etPayAmount);
        btnSavePayment = (Button) findViewById(R.id.btnSavePayment);
        pbPayments = (ProgressBar) findViewById(R.id.pbPayments);
        lvPayments = (ListView) findViewById(R.id.lvPayments);

        paymentItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, paymentItems);
        lvPayments.setAdapter(adapter);

        btnSavePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordPayment();
            }
        });

        loadPayments();
    }

    private void loadPayments() {
        pbPayments.setVisibility(View.VISIBLE);
        backendApi.getCollection("payments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbPayments.setVisibility(View.GONE);
                paymentItems.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String name = data.optString("member_name", "General");
                                double amount = data.optDouble("amount", 0.0);
                                String date = data.optString("payment_date", "-");
                                paymentItems.add(name + ": $" + amount + " on " + date);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pbPayments.setVisibility(View.GONE);
                Toast.makeText(PaymentsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recordPayment() {
        String memberName = etPayMember.getText().toString().trim();
        String amountStr = etPayAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            amount = 0.0;
        }

        pbPayments.setVisibility(View.VISIBLE);
        btnSavePayment.setEnabled(false);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("member_id", "1");
            data.put("member_name", memberName.isEmpty() ? "Walk-in Member" : memberName);
            data.put("amount", amount);
            data.put("payment_date", today);
            data.put("payment_type", "Fee Collection");
            data.put("notes", "Payment recorded");

            backendApi.createRecord("payments", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbPayments.setVisibility(View.GONE);
                    btnSavePayment.setEnabled(true);
                    etPayMember.setText("");
                    etPayAmount.setText("");
                    Toast.makeText(PaymentsActivity.this, "Payment recorded", Toast.LENGTH_SHORT).show();
                    loadPayments();
                }

                @Override
                public void onError(String errorMessage) {
                    pbPayments.setVisibility(View.GONE);
                    btnSavePayment.setEnabled(true);
                    Toast.makeText(PaymentsActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbPayments.setVisibility(View.GONE);
            btnSavePayment.setEnabled(true);
        }
    }
}