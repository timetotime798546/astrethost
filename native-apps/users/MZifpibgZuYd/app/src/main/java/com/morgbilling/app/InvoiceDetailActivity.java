package com.morgbilling.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class InvoiceDetailActivity extends Activity {

    private BackendApi backendApi;

    private String recordId;
    private String invNumber;
    private String customer;
    private String phone;
    private String itemsJson;
    private double subtotal;
    private double discount;
    private double tax;
    private double total;
    private double paid;
    private double balance;
    private String status;
    private String date;

    private TextView tvDetailInvNumber;
    private TextView tvDetailStatusBadge;
    private TextView tvDetailDate;
    private TextView tvDetailCustomer;
    private TextView tvDetailPhone;
    private LinearLayout detailItemsContainer;
    private TextView tvDetailSubtotal;
    private TextView tvDetailDiscount;
    private TextView tvDetailTax;
    private TextView tvDetailTotal;
    private TextView tvDetailBalance;
    private Button btnShareReceipt;
    private Button btnMarkPaid;
    private Button btnDeleteInvoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        backendApi = new BackendApi(this);

        Intent intent = getIntent();
        recordId = intent.getStringExtra("record_id");
        invNumber = intent.getStringExtra("inv_number");
        customer = intent.getStringExtra("customer");
        phone = intent.getStringExtra("phone");
        itemsJson = intent.getStringExtra("items_json");
        subtotal = intent.getDoubleExtra("subtotal", 0.0);
        discount = intent.getDoubleExtra("discount", 0.0);
        tax = intent.getDoubleExtra("tax", 0.0);
        total = intent.getDoubleExtra("total", 0.0);
        paid = intent.getDoubleExtra("paid", 0.0);
        balance = intent.getDoubleExtra("balance", 0.0);
        status = intent.getStringExtra("status");
        date = intent.getStringExtra("date");

        tvDetailInvNumber = (TextView) findViewById(R.id.tvDetailInvNumber);
        tvDetailStatusBadge = (TextView) findViewById(R.id.tvDetailStatusBadge);
        tvDetailDate = (TextView) findViewById(R.id.tvDetailDate);
        tvDetailCustomer = (TextView) findViewById(R.id.tvDetailCustomer);
        tvDetailPhone = (TextView) findViewById(R.id.tvDetailPhone);
        detailItemsContainer = (LinearLayout) findViewById(R.id.detailItemsContainer);
        tvDetailSubtotal = (TextView) findViewById(R.id.tvDetailSubtotal);
        tvDetailDiscount = (TextView) findViewById(R.id.tvDetailDiscount);
        tvDetailTax = (TextView) findViewById(R.id.tvDetailTax);
        tvDetailTotal = (TextView) findViewById(R.id.tvDetailTotal);
        tvDetailBalance = (TextView) findViewById(R.id.tvDetailBalance);
        btnShareReceipt = (Button) findViewById(R.id.btnShareReceipt);
        btnMarkPaid = (Button) findViewById(R.id.btnMarkPaid);
        btnDeleteInvoice = (Button) findViewById(R.id.btnDeleteInvoice);

        populateUI();

        btnShareReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePrintableReceipt();
            }
        });

        btnMarkPaid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAsPaid();
            }
        });

        btnDeleteInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteInvoice();
            }
        });
    }

    private void populateUI() {
        tvDetailInvNumber.setText(invNumber != null ? invNumber : "Invoice");
        tvDetailCustomer.setText(customer != null ? customer : "Customer");
        tvDetailPhone.setText((phone != null && !phone.isEmpty()) ? phone : "No phone provided");
        tvDetailDate.setText("Generated: " + (date != null ? date : "Recent"));

        if ("PAID".equalsIgnoreCase(status)) {
            tvDetailStatusBadge.setText("PAID");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_paid);
            tvDetailStatusBadge.setTextColor(getResources().getColor(R.color.success_green));
            btnMarkPaid.setVisibility(View.GONE);
        } else {
            tvDetailStatusBadge.setText(status != null ? status.toUpperCase() : "UNPAID");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_due);
            tvDetailStatusBadge.setTextColor(getResources().getColor(R.color.danger_red));
            btnMarkPaid.setVisibility(View.VISIBLE);
        }

        tvDetailSubtotal.setText(BillingCalculator.formatMoney(subtotal));
        tvDetailDiscount.setText("-" + BillingCalculator.formatMoney(discount));
        tvDetailTax.setText("+" + BillingCalculator.formatMoney(tax));
        tvDetailTotal.setText(BillingCalculator.formatMoney(total));
        tvDetailBalance.setText(BillingCalculator.formatMoney(balance));

        renderLineItems();
    }

    private void renderLineItems() {
        detailItemsContainer.removeAllViews();
        if (itemsJson == null || itemsJson.trim().isEmpty()) return;

        try {
            JSONArray arr = new JSONArray(itemsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.getJSONObject(i);
                String desc = it.optString("description", "Item");
                double qty = it.optDouble("quantity", 1);
                double price = it.optDouble("price", 0.0);
                double lineTot = it.optDouble("total", qty * price);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 6, 0, 6);

                TextView tvDesc = new TextView(this);
                tvDesc.setText(desc + " (" + qty + " x " + BillingCalculator.formatMoney(price) + ")");
                tvDesc.setTextSize(13);
                tvDesc.setTextColor(getResources().getColor(R.color.text_main));
                tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvTot = new TextView(this);
                tvTot.setText(BillingCalculator.formatMoney(lineTot));
                tvTot.setTextSize(13);
                tvTot.setTextColor(getResources().getColor(R.color.primary));

                row.addView(tvDesc);
                row.addView(tvTot);
                detailItemsContainer.addView(row);
            }
        } catch (Exception e) {
            // parsing fallback
        }
    }

    private void markAsPaid() {
        if (recordId == null || recordId.isEmpty()) {
            status = "PAID";
            balance = 0.0;
            paid = total;
            populateUI();
            Toast.makeText(this, "Marked as paid locally", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject updatePayload = new JSONObject();
        try {
            updatePayload.put("payment_status", "PAID");
            updatePayload.put("paid_amount", total);
            updatePayload.put("balance_due", 0.0);
        } catch (Exception e) {
            return;
        }

        backendApi.updateRecord(recordId, updatePayload, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonResponse) {
                status = "PAID";
                balance = 0.0;
                paid = total;
                populateUI();
                Toast.makeText(InvoiceDetailActivity.this, "Invoice updated to PAID", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(InvoiceDetailActivity.this, "Update error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteInvoice() {
        if (recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "Invoice deleted", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backendApi.deleteRecord(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonResponse) {
                Toast.makeText(InvoiceDetailActivity.this, "Invoice deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(InvoiceDetailActivity.this, "Delete note: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void sharePrintableReceipt() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("================================\n");
        receipt.append("        MORG BILLING RECEIPT     \n");
        receipt.append("================================\n");
        receipt.append("Invoice: ").append(invNumber).append("\n");
        receipt.append("Date:    ").append(date).append("\n");
        receipt.append("Customer:").append(customer).append("\n");
        receipt.append("Phone:   ").append(phone).append("\n");
        receipt.append("--------------------------------\n");
        receipt.append("ITEMS:\n");

        try {
            JSONArray arr = new JSONArray(itemsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.getJSONObject(i);
                receipt.append("• ").append(it.optString("description"))
                    .append(" [").append(it.optDouble("quantity")).append(" x ")
                    .append(BillingCalculator.formatMoney(it.optDouble("price")))
                    .append("] = ")
                    .append(BillingCalculator.formatMoney(it.optDouble("total")))
                    .append("\n");
            }
        } catch (Exception ignored) {}

        receipt.append("--------------------------------\n");
        receipt.append("Subtotal:    ").append(BillingCalculator.formatMoney(subtotal)).append("\n");
        receipt.append("Discount:   -").append(BillingCalculator.formatMoney(discount)).append("\n");
        receipt.append("Tax/GST:    +").append(BillingCalculator.formatMoney(tax)).append("\n");
        receipt.append("TOTAL:       ").append(BillingCalculator.formatMoney(total)).append("\n");
        receipt.append("Paid Amount: ").append(BillingCalculator.formatMoney(paid)).append("\n");
        receipt.append("Balance Due: ").append(BillingCalculator.formatMoney(balance)).append("\n");
        receipt.append("Status:      ").append(status).append("\n");
        receipt.append("================================\n");
        receipt.append("    Thank you for your business!\n");

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, receipt.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Receipt"));
    }
}