package com.vyaparpro.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KhataActivity extends Activity {

    private BackendApi api;
    private LinearLayout layoutAddCustomerForm;
    private LinearLayout containerCustomers;
    private ProgressBar progressKhata;
    private EditText etCustomerName;
    private EditText etCustomerPhone;
    private EditText etSearchCustomer;
    private Button btnToggleAddCustomer;

    private List<JSONObject> customerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_khata);

        api = new BackendApi(this);

        layoutAddCustomerForm = (LinearLayout) findViewById(R.id.layoutAddCustomerForm);
        containerCustomers = (LinearLayout) findViewById(R.id.containerCustomers);
        progressKhata = (ProgressBar) findViewById(R.id.progressKhata);
        etCustomerName = (EditText) findViewById(R.id.etCustomerName);
        etCustomerPhone = (EditText) findViewById(R.id.etCustomerPhone);
        etSearchCustomer = (EditText) findViewById(R.id.etSearchCustomer);
        btnToggleAddCustomer = (Button) findViewById(R.id.btnToggleAddCustomer);

        findViewById(R.id.btnKhataBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnToggleAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutAddCustomerForm.getVisibility() == View.VISIBLE) {
                    layoutAddCustomerForm.setVisibility(View.GONE);
                    btnToggleAddCustomer.setText("+ New Customer");
                } else {
                    layoutAddCustomerForm.setVisibility(View.VISIBLE);
                    btnToggleAddCustomer.setText("Cancel");
                }
            }
        });

        findViewById(R.id.btnSaveCustomer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers();
    }

    private void saveCustomer() {
        String name = etCustomerName.getText().toString().trim();
        String phone = etCustomerPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please provide customer name and phone.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressKhata.setVisibility(View.VISIBLE);
        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("phone", phone);

            api.createData("customers", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressKhata.setVisibility(View.GONE);
                    Toast.makeText(KhataActivity.this, "Customer added to Khata!", Toast.LENGTH_SHORT).show();
                    layoutAddCustomerForm.setVisibility(View.GONE);
                    btnToggleAddCustomer.setText("+ New Customer");
                    etCustomerName.setText("");
                    etCustomerPhone.setText("");
                    loadCustomers();
                }

                @Override
                public void onError(String errorMessage) {
                    progressKhata.setVisibility(View.GONE);
                    Toast.makeText(KhataActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadCustomers() {
        progressKhata.setVisibility(View.VISIBLE);
        api.getData("customers", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressKhata.setVisibility(View.GONE);
                customerList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            customerList.add(r);
                        }
                    }
                }
                renderCustomerList(customerList);
            }

            @Override
            public void onError(String errorMessage) {
                progressKhata.setVisibility(View.GONE);
                Toast.makeText(KhataActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterCustomers(String query) {
        String q = query.toLowerCase().trim();
        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject item : customerList) {
            JSONObject data = item.optJSONObject("data");
            if (data != null) {
                String name = data.optString("name", "").toLowerCase();
                String phone = data.optString("phone", "").toLowerCase();
                if (name.contains(q) || phone.contains(q)) {
                    filtered.add(item);
                }
            }
        }
        renderCustomerList(filtered);
    }

    private void renderCustomerList(List<JSONObject> list) {
        containerCustomers.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No customers match your search.");
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setPadding(16, 32, 16, 32);
            containerCustomers.addView(empty);
            return;
        }

        for (final JSONObject r : list) {
            final String id = r.optString("id", "");
            final JSONObject data = r.optJSONObject("data");
            if (data == null) continue;

            final String name = data.optString("name", "Unknown");
            final String phone = data.optString("phone", "");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(Color.WHITE);
            card.setElevation(2f);
            card.setPadding(20, 18, 20, 18);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 12);
            card.setLayoutParams(lp);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextColor(Color.parseColor("#0F172A"));
            tvName.setTextSize(16f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(tvName);

            TextView tvPhone = new TextView(this);
            tvPhone.setText(phone);
            tvPhone.setTextColor(Color.parseColor("#64748B"));
            tvPhone.setTextSize(13f);
            info.addView(tvPhone);

            card.addView(info, infoLp);

            Button btnView = new Button(this);
            btnView.setText("Open Khata");
            btnView.setTextSize(12f);
            btnView.setBackgroundColor(Color.parseColor("#2563EB"));
            btnView.setTextColor(Color.WHITE);
            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(KhataActivity.this, CustomerDetailActivity.class);
                    intent.putExtra("customer_id", id);
                    intent.putExtra("customer_name", name);
                    intent.putExtra("customer_phone", phone);
                    startActivity(intent);
                }
            });

            card.addView(btnView);
            containerCustomers.addView(card);
        }
    }
}