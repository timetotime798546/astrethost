package com.cctvrecordbook.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView mTxtTotalCount;
    private TextView mTxtTotalPending;
    private EditText mEtSearch;
    private ListView mListCustomers;
    private TextView mTxtEmptyState;
    private ProgressBar mMainProgress;
    private Button mBtnAddCustomer;
    private Button mBtnLogout;

    private BackendApi mApi;
    private List<Customer> mAllCustomers = new ArrayList<>();
    private List<Customer> mFilteredCustomers = new ArrayList<>();
    private CustomerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApi = new BackendApi(this);

        mTxtTotalCount = findViewById(R.id.txt_total_customers);
        mTxtTotalPending = findViewById(R.id.txt_total_pending);
        mEtSearch = findViewById(R.id.et_search);
        mListCustomers = findViewById(R.id.list_customers);
        mTxtEmptyState = findViewById(R.id.txt_empty_state);
        mMainProgress = findViewById(R.id.main_progress);
        mBtnAddCustomer = findViewById(R.id.btn_add_customer);
        mBtnLogout = findViewById(R.id.btn_logout);

        mAdapter = new CustomerAdapter(mFilteredCustomers);
        mListCustomers.setAdapter(mAdapter);

        mBtnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mApi.logout();
                Toast.makeText(MainActivity.this, "Successfully Logged Out!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        mBtnAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditCustomerActivity.class));
            }
        });

        mListCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer clickedCustomer = mFilteredCustomers.get(position);
                Intent i = new Intent(MainActivity.this, CustomerDetailsActivity.class);
                i.putExtra("customer", clickedCustomer);
                startActivity(i);
            }
        });

        // Search feature
        mEtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCustomerData();
    }

    private void fetchCustomerData() {
        mMainProgress.setVisibility(View.VISIBLE);
        mTxtEmptyState.setVisibility(View.GONE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject response = mApi.readRecords("cctv_customers");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMainProgress.setVisibility(View.GONE);
                            if (response.optBoolean("success")) {
                                mAllCustomers.clear();
                                JSONArray records = response.optJSONArray("records");
                                if (records != null) {
                                    for (int i = 0; i < records.length(); i++) {
                                        JSONObject rec = records.optJSONObject(i);
                                        if (rec != null) {
                                            mAllCustomers.add(new Customer(rec));
                                        }
                                    }
                                }
                                updateUIAndMetrics();
                            } else {
                                Toast.makeText(MainActivity.this, "Session expired! Please log in again.", Toast.LENGTH_LONG).show();
                                mApi.clearToken();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMainProgress.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Error syncing records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void updateUIAndMetrics() {
        // Calculate Metrics
        int totalCount = mAllCustomers.size();
        double totalPending = 0.0;
        for (int i = 0; i < mAllCustomers.size(); i++) {
            totalPending += mAllCustomers.get(i).balance;
        }

        mTxtTotalCount.setText(String.valueOf(totalCount));
        mTxtTotalPending.setText(String.format("₹%.2f", totalPending));

        // Reapply search filter
        filterList(mEtSearch.getText().toString());
    }

    private void filterList(String query) {
        mFilteredCustomers.clear();
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) {
            mFilteredCustomers.addAll(mAllCustomers);
        } else {
            for (int i = 0; i < mAllCustomers.size(); i++) {
                Customer c = mAllCustomers.get(i);
                if (c.name.toLowerCase().contains(q) || c.phone.contains(q)) {
                    mFilteredCustomers.add(c);
                }
            }
        }

        mAdapter.notifyDataSetChanged();

        if (mFilteredCustomers.isEmpty()) {
            mTxtEmptyState.setVisibility(View.VISIBLE);
        } else {
            mTxtEmptyState.setVisibility(View.GONE);
        }
    }

    private class CustomerAdapter extends BaseAdapter {
        private List<Customer> list;

        public CustomerAdapter(List<Customer> customersList) {
            this.list = customersList;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_customer, parent, false);
            }

            Customer c = list.get(position);

            TextView nameTxt = convertView.findViewById(R.id.txt_cust_name);
            TextView summaryTxt = convertView.findViewById(R.id.txt_cust_summary);
            TextView balanceTxt = convertView.findViewById(R.id.txt_cust_balance);
            TextView phoneTxt = convertView.findViewById(R.id.txt_cust_phone);

            nameTxt.setText(c.name);
            summaryTxt.setText(c.cameraCount + " Cam | " + c.date);
            phoneTxt.setText(c.phone);

            if (c.balance > 0) {
                balanceTxt.setText(String.format("₹%.2f बकाया", c.balance));
                balanceTxt.setTextColor(getResources().getColor(R.color.red));
            } else {
                balanceTxt.setText("Paid (पूर्ण भुगतान)");
                balanceTxt.setTextColor(getResources().getColor(R.color.green));
            }

            return convertView;
        }
    }
}