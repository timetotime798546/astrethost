package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentsActivity extends Activity {
    private EditText etMember, etAmount;
    private Button btnSave;
    private ListView lvPayments;
    private ProgressBar pb;
    private BackendApi api;
    private List<JSONObject> paymentList = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        api = new BackendApi(this);

        etMember = findViewById(R.id.etPayMember);
        etAmount = findViewById(R.id.etPayAmount);
        btnSave = findViewById(R.id.btnSavePayment);
        lvPayments = findViewById(R.id.lvPayments);
        pb = findViewById(R.id.pbPayments);

        adapter = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_2, android.R.id.text1, paymentList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                JSONObject item = getItem(position);
                if (item != null) {
                    JSONObject data = item.optJSONObject("data");
                    if (data != null) {
                        text1.setText("Member: " + data.optString("member_name", "N/A"));
                        text1.setTextColor(0xFFFFFFFF);
                        String sub = "Amount: $" + String.format(Locale.US, "%.2f", data.optDouble("amount", 0)) + " | Date: " + data.optString("payment_date");
                        text2.setText(sub);
                        text2.setTextColor(0xFF55D98B);
                    }
                }
                return view;
            }
        };

        lvPayments.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordPayment();
            }
        });

        loadPayments();
    }

    private void loadPayments() {
        pb.setVisibility(View.VISIBLE);
        api.getCollection("payments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pb.setVisibility(View.GONE);
                paymentList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            paymentList.add(r);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pb.setVisibility(View.GONE);
                Toast.makeText(PaymentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recordPayment() {
        String memberName = etMember.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (memberName.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Member and Amount required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (Exception ignored) {}

        String payDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("member_name", memberName);
            data.put("amount", amount);
            data.put("payment_date", payDate);

            pb.setVisibility(View.VISIBLE);
            api.createRecord("payments", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pb.setVisibility(View.GONE);
                    etMember.setText("");
                    etAmount.setText("");
                    Toast.makeText(PaymentsActivity.this, "Payment logged!", Toast.LENGTH_SHORT).show();
                    loadPayments();
                }

                @Override
                public void onError(String errorMessage) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(PaymentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}