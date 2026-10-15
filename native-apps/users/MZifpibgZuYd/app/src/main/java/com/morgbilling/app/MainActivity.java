package com.morgbilling.app;

import android.app.Activity;
import android.content.Intent;
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

public class MainActivity extends Activity {

    private static final int REQ_AUTH = 101;
    private static final int REQ_CREATE_INVOICE = 102;

    private BackendApi backendApi;

    private TextView tvUserEmail;
    private Button btnAuthToggle;
    private Button btnNavInventory;
    private TextView tvStatTotalBilled;
    private TextView tvStatPaid;
    private TextView tvStatTotalDue;
    private TextView tvStatCount;
    private Button btnNewInvoice;
    private Button btnRefresh;
    private EditText etSearch;
    private ProgressBar mainProgressBar;
    private LinearLayout invoicesListContainer;
    private TextView tvEmptyState;

    private static class InvoiceItemRecord {
        String id;
        String invoiceNumber;
        String customerName;
        String customerPhone;
        String itemsJson;
        double subtotal;
        double discountAmount;
        double taxAmount;
        double totalAmount;
        double paidAmount;
        double balanceDue;
        String paymentStatus;
        String dateCreated;
    }

    private final List<InvoiceItemRecord> allInvoices = new ArrayList<InvoiceItemRecord>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvUserEmail = (TextView) findViewById(R.id.tvUserEmail);
        btnAuthToggle = (Button) findViewById(R.id.btnAuthToggle);
        btnNavInventory = (Button) findViewById(R.id.btnNavInventory);
        tvStatTotalBilled = (TextView) findViewById(R.id.tvStatTotalBilled);
        tvStatPaid = (TextView) findViewById(R.id.tvStatPaid);
        tvStatTotalDue = (TextView) findViewById(R.id.tvStatTotalDue);
        tvStatCount = (TextView) findViewById(R.id.tvStatCount);
        btnNewInvoice = (Button) findViewById(R.id.btnNewInvoice);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        etSearch = (EditText) findViewById(R.id.etSearch);
        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);
        invoicesListContainer = (LinearLayout) findViewById(R.id.invoicesListContainer);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);

        setupListeners();
        updateAuthHeader();
        loadInvoices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthHeader();
    }

    private void setupListeners() {
        btnAuthToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    backendApi.logout(new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject jsonResponse) {
                            Toast.makeText(MainActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
                            updateAuthHeader();
                            loadInvoices();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            updateAuthHeader();
                        }
                    });
                } else {
                    Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                    startActivityForResult(intent, REQ_AUTH);
                }
            }
        });

        btnNavInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InventoryActivity.class);
                startActivity(intent);
            }
        });

        btnNewInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateInvoiceActivity.class);
                startActivityForResult(intent, REQ_CREATE_INVOICE);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInvoices();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInvoices(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateAuthHeader() {
        if (backendApi.isLoggedIn()) {
            tvUserEmail.setText("Logged in: " + backendApi.getUserEmail());
            btnAuthToggle.setText("Sign Out");
        } else {
            tvUserEmail.setText("Guest Mode (Local Storage)");
            btnAuthToggle.setText("Sign In");
        }
    }

    private void loadInvoices() {
        if (!backendApi.isLoggedIn()) {
            // Display empty or locally stored state
            renderInvoiceList(allInvoices);
            calculateDashboardMetrics(allInvoices);
            return;
        }

        mainProgressBar.setVisibility(View.VISIBLE);
        backendApi.readRecords("invoices", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonResponse) {
                mainProgressBar.setVisibility(View.GONE);
                allInvoices.clear();
                try {
                    JSONArray records = jsonResponse.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String recordId = rec.optString("id");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                InvoiceItemRecord item = new InvoiceItemRecord();
                                item.id = recordId;
                                item.invoiceNumber = data.optString("invoice_number", "INV-" + (i + 1));
                                item.customerName = data.optString("customer_name", "Walk-in Customer");
                                item.customerPhone = data.optString("customer_phone", "");
                                item.itemsJson = data.optString("items_json", "[]");
                                item.subtotal = data.optDouble("subtotal", 0.0);
                                item.discountAmount = data.optDouble("discount_amount", 0.0);
                                item.taxAmount = data.optDouble("tax_amount", 0.0);
                                item.totalAmount = data.optDouble("total_amount", 0.0);
                                item.paidAmount = data.optDouble("paid_amount", 0.0);
                                item.balanceDue = data.optDouble("balance_due", 0.0);
                                item.paymentStatus = data.optString("payment_status", "UNPAID");
                                item.dateCreated = data.optString("date_created", "");
                                allInvoices.add(item);
                            }
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                // Deterministic local business calculation across all fetched records
                calculateDashboardMetrics(allInvoices);
                filterInvoices(etSearch.getText().toString());
            }

            @Override
            public void onError(String errorMessage) {
                mainProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Sync note: " + errorMessage, Toast.LENGTH_SHORT).show();
                calculateDashboardMetrics(allInvoices);
                filterInvoices(etSearch.getText().toString());
            }
        });
    }

    // Pure local Java calculation across records
    private void calculateDashboardMetrics(List<InvoiceItemRecord> invoices) {
        double totalBilled = 0.0;
        double totalPaid = 0.0;
        double totalDue = 0.0;
        int count = invoices.size();

        for (int i = 0; i < invoices.size(); i++) {
            InvoiceItemRecord inv = invoices.get(i);
            totalBilled += inv.totalAmount;
            totalPaid += inv.paidAmount;
            totalDue += inv.balanceDue;
        }

        tvStatTotalBilled.setText(BillingCalculator.formatMoney(totalBilled));
        tvStatPaid.setText("Collected: " + BillingCalculator.formatMoney(totalPaid));
        tvStatTotalDue.setText(BillingCalculator.formatMoney(totalDue));
        tvStatCount.setText(count + (count == 1 ? " Invoice" : " Invoices"));
    }

    private void filterInvoices(String query) {
        if (query == null || query.trim().isEmpty()) {
            renderInvoiceList(allInvoices);
            return;
        }
        String lower = query.toLowerCase();
        List<InvoiceItemRecord> filtered = new ArrayList<InvoiceItemRecord>();
        for (int i = 0; i < allInvoices.size(); i++) {
            InvoiceItemRecord inv = allInvoices.get(i);
            if (inv.customerName.toLowerCase().contains(lower) ||
                inv.invoiceNumber.toLowerCase().contains(lower) ||
                inv.paymentStatus.toLowerCase().contains(lower)) {
                filtered.add(inv);
            }
        }
        renderInvoiceList(filtered);
    }

    private void renderInvoiceList(List<InvoiceItemRecord> list) {
        invoicesListContainer.removeAllViews();
        if (list.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyState.setVisibility(View.GONE);

        for (int i = 0; i < list.size(); i++) {
            final InvoiceItemRecord inv = list.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable/bg_card);
            card.setPadding(36, 32, 36, 32);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 24);
            card.setLayoutParams(lp);

            // Header Row: Customer Name & Amount
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvCust = new TextView(this);
            tvCust.setText(inv.customerName);
            tvCust.setTextSize(16);
            tvCust.setTextColor(getResources().getColor(R.color.primary));
            tvCust.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvAmt = new TextView(this);
            tvAmt.setText(BillingCalculator.formatMoney(inv.totalAmount));
            tvAmt.setTextSize(16);
            tvAmt.setTextColor(getResources().getColor(R.color.primary));

            row1.addView(tvCust);
            row1.addView(tvAmt);

            // Sub row: Invoice #, Date, Status Badge
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setPadding(0, 10, 0, 0);

            TextView tvSub = new TextView(this);
            tvSub.setText(inv.invoiceNumber + " • " + (inv.dateCreated.isEmpty() ? "Recent" : inv.dateCreated));
            tvSub.setTextSize(12);
            tvSub.setTextColor(getResources().getColor(R.color.text_muted));
            tvSub.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView badge = new TextView(this);
            badge.setText(inv.paymentStatus.toUpperCase());
            badge.setTextSize(10);
            if ("PAID".equalsIgnoreCase(inv.paymentStatus)) {
                badge.setBackgroundResource(R.drawable.bg_badge_paid);
                badge.setTextColor(getResources().getColor(R.color.success_green));
            } else {
                badge.setBackgroundResource(R.drawable.bg_badge_due);
                badge.setTextColor(getResources().getColor(R.color.danger_red));
            }

            row2.addView(tvSub);
            row2.addView(badge);

            // Balance indicator if due
            if (inv.balanceDue > 0) {
                TextView tvDue = new TextView(this);
                tvDue.setText("Due: " + BillingCalculator.formatMoney(inv.balanceDue));
                tvDue.setTextSize(11);
                tvDue.setTextColor(getResources().getColor(R.color.danger_red));
                tvDue.setPadding(0, 8, 0, 0);
                card.addView(row1);
                card.addView(row2);
                card.addView(tvDue);
            } else {
                card.addView(row1);
                card.addView(row2);
            }

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent detailIntent = new Intent(MainActivity.this, InvoiceDetailActivity.class);
                    detailIntent.putExtra("record_id", inv.id);
                    detailIntent.putExtra("inv_number", inv.invoiceNumber);
                    detailIntent.putExtra("customer", inv.customerName);
                    detailIntent.putExtra("phone", inv.customerPhone);
                    detailIntent.putExtra("items_json", inv.itemsJson);
                    detailIntent.putExtra("subtotal", inv.subtotal);
                    detailIntent.putExtra("discount", inv.discountAmount);
                    detailIntent.putExtra("tax", inv.taxAmount);
                    detailIntent.putExtra("total", inv.totalAmount);
                    detailIntent.putExtra("paid", inv.paidAmount);
                    detailIntent.putExtra("balance", inv.balanceDue);
                    detailIntent.putExtra("status", inv.paymentStatus);
                    detailIntent.putExtra("date", inv.dateCreated);
                    startActivity(detailIntent);
                }
            });

            invoicesListContainer.addView(card);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_AUTH && resultCode == RESULT_OK) {
            updateAuthHeader();
            loadInvoices();
        } else if (requestCode == REQ_CREATE_INVOICE && resultCode == RESULT_OK) {
            loadInvoices();
        }
    }
}