package com.morgbilling.app;

import android.app.Activity;
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

public class CreateInvoiceActivity extends Activity {

    private BackendApi backendApi;

    private EditText etCustomerName;
    private EditText etCustomerPhone;
    private Button btnAddItem;
    private LinearLayout itemsContainer;
    private TextView tvSummarySubtotal;
    private EditText etDiscountPercent;
    private TextView tvDiscountAmount;
    private EditText etTaxPercent;
    private TextView tvTaxAmount;
    private TextView tvSummaryTotal;
    private EditText etPaidAmount;
    private TextView tvSummaryBalanceDue;
    private Button btnSaveInvoice;
    private ProgressBar saveProgress;

    private static class LineItemHolder {
        View rootView;
        EditText etItemName;
        EditText etQuantity;
        EditText etUnitPrice;
        TextView tvLineTotal;
        Button btnRemove;
    }

    private final List<LineItemHolder> lineHolders = new ArrayList<LineItemHolder>();

    private double calculatedSubtotal = 0.0;
    private double calculatedDiscount = 0.0;
    private double calculatedTax = 0.0;
    private double calculatedTotal = 0.0;
    private double calculatedBalance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_invoice);

        backendApi = new BackendApi(this);

        etCustomerName = (EditText) findViewById(R.id.etCustomerName);
        etCustomerPhone = (EditText) findViewById(R.id.etCustomerPhone);
        btnAddItem = (Button) findViewById(R.id.btnAddItem);
        itemsContainer = (LinearLayout) findViewById(R.id.itemsContainer);
        tvSummarySubtotal = (TextView) findViewById(R.id.tvSummarySubtotal);
        etDiscountPercent = (EditText) findViewById(R.id.etDiscountPercent);
        tvDiscountAmount = (TextView) findViewById(R.id.tvDiscountAmount);
        etTaxPercent = (EditText) findViewById(R.id.etTaxPercent);
        tvTaxAmount = (TextView) findViewById(R.id.tvTaxAmount);
        tvSummaryTotal = (TextView) findViewById(R.id.tvSummaryTotal);
        etPaidAmount = (EditText) findViewById(R.id.etPaidAmount);
        tvSummaryBalanceDue = (TextView) findViewById(R.id.tvSummaryBalanceDue);
        btnSaveInvoice = (Button) findViewById(R.id.btnSaveInvoice);
        saveProgress = (ProgressBar) findViewById(R.id.saveProgress);

        // Add 2 initial default items
        addNewLineItem("Professional Service", "1", "150.00");
        addNewLineItem("Hardware Component", "2", "35.00");

        setupCalculationWatchers();

        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewLineItem("", "1", "0.00");
            }
        });

        btnSaveInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInvoice();
            }
        });

        recalculateAll();
    }

    private void setupCalculationWatchers() {
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateAll();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etDiscountPercent.addTextChangedListener(tw);
        etTaxPercent.addTextChangedListener(tw);
        etPaidAmount.addTextChangedListener(tw);
    }

    private void addNewLineItem(String defaultName, String defaultQty, String defaultPrice) {
        final LineItemHolder holder = new LineItemHolder();

        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setBackgroundResource(R.drawable.bg_card);
        itemLayout.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 16);
        itemLayout.setLayoutParams(lp);

        // Row 1: Item Name & Remove Button
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        holder.etItemName = new EditText(this);
        holder.etItemName.setHint("Item / Service description");
        holder.etItemName.setText(defaultName);
        holder.etItemName.setTextSize(13);
        holder.etItemName.setBackgroundResource(R.drawable.bg_input);
        holder.etItemName.setLayoutParams(new LinearLayout.LayoutParams(0, 72, 1.0f));

        holder.btnRemove = new Button(this);
        holder.btnRemove.setText("✕");
        holder.btnRemove.setTextSize(12);
        holder.btnRemove.setTextColor(getResources().getColor(R.color.danger_red));
        holder.btnRemove.setBackgroundResource(R.drawable.bg_badge_due);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(64, 72);
        btnLp.setMargins(12, 0, 0, 0);
        holder.btnRemove.setLayoutParams(btnLp);

        row1.addView(holder.etItemName);
        row1.addView(holder.btnRemove);

        // Row 2: Qty, Unit Price, Line Total
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, 12, 0, 0);

        holder.etQuantity = new EditText(this);
        holder.etQuantity.setHint("Qty");
        holder.etQuantity.setText(defaultQty);
        holder.etQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        holder.etQuantity.setTextSize(13);
        holder.etQuantity.setBackgroundResource(R.drawable.bg_input);
        holder.etQuantity.setLayoutParams(new LinearLayout.LayoutParams(0, 72, 1.0f));

        holder.etUnitPrice = new EditText(this);
        holder.etUnitPrice.setHint("Price");
        holder.etUnitPrice.setText(defaultPrice);
        holder.etUnitPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        holder.etUnitPrice.setTextSize(13);
        holder.etUnitPrice.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams priceLp = new LinearLayout.LayoutParams(0, 72, 1.2f);
        priceLp.setMargins(10, 0, 10, 0);
        holder.etUnitPrice.setLayoutParams(priceLp);

        holder.tvLineTotal = new TextView(this);
        holder.tvLineTotal.setText("$0.00");
        holder.tvLineTotal.setTextSize(13);
        holder.tvLineTotal.setTextColor(getResources().getColor(R.color.primary));
        holder.tvLineTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        holder.tvLineTotal.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);

        row2.addView(holder.etQuantity);
        row2.addView(holder.etUnitPrice);
        row2.addView(holder.tvLineTotal);

        itemLayout.addView(row1);
        itemLayout.addView(row2);

        holder.rootView = itemLayout;
        lineHolders.add(holder);
        itemsContainer.addView(itemLayout);

        TextWatcher lineWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateAll();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        holder.etQuantity.addTextChangedListener(lineWatcher);
        holder.etUnitPrice.addTextChangedListener(lineWatcher);

        holder.btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lineHolders.size() > 1) {
                    itemsContainer.removeView(holder.rootView);
                    lineHolders.remove(holder);
                    recalculateAll();
                } else {
                    Toast.makeText(CreateInvoiceActivity.this, "Invoice requires at least one item", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recalculateAll();
    }

    // MANDATORY DETERMINISTIC LOCAL CALCULATION LOGIC
    private void recalculateAll() {
        double subtotal = 0.0;

        for (int i = 0; i < lineHolders.size(); i++) {
            LineItemHolder holder = lineHolders.get(i);
            double qty = parseDoubleSafely(holder.etQuantity.getText().toString());
            double price = parseDoubleSafely(holder.etUnitPrice.getText().toString());
            double lineTotal = BillingCalculator.calculateItemSubtotal(qty, price);
            holder.tvLineTotal.setText(BillingCalculator.formatMoney(lineTotal));
            subtotal += lineTotal;
        }

        calculatedSubtotal = subtotal;
        tvSummarySubtotal.setText(BillingCalculator.formatMoney(calculatedSubtotal));

        double discPercent = parseDoubleSafely(etDiscountPercent.getText().toString());
        calculatedDiscount = BillingCalculator.calculateDiscountAmount(calculatedSubtotal, discPercent);
        tvDiscountAmount.setText("-" + BillingCalculator.formatMoney(calculatedDiscount));

        double taxableAmount = Math.max(0.0, calculatedSubtotal - calculatedDiscount);
        double taxPercent = parseDoubleSafely(etTaxPercent.getText().toString());
        calculatedTax = BillingCalculator.calculateTaxAmount(taxableAmount, taxPercent);
        tvTaxAmount.setText("+" + BillingCalculator.formatMoney(calculatedTax));

        calculatedTotal = BillingCalculator.calculateInvoiceTotal(calculatedSubtotal, calculatedDiscount, calculatedTax);
        tvSummaryTotal.setText(BillingCalculator.formatMoney(calculatedTotal));

        double paidNow = parseDoubleSafely(etPaidAmount.getText().toString());
        calculatedBalance = BillingCalculator.calculateBalanceDue(calculatedTotal, paidNow);
        tvSummaryBalanceDue.setText(BillingCalculator.formatMoney(calculatedBalance));
    }

    private double parseDoubleSafely(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void saveInvoice() {
        String custName = etCustomerName.getText().toString().trim();
        String custPhone = etCustomerPhone.getText().toString().trim();

        if (custName.isEmpty()) {
            custName = "Walk-in Customer";
        }

        JSONArray itemsArray = new JSONArray();
        try {
            for (int i = 0; i < lineHolders.size(); i++) {
                LineItemHolder h = lineHolders.get(i);
                String desc = h.etItemName.getText().toString().trim();
                if (desc.isEmpty()) desc = "Item " + (i + 1);
                double qty = parseDoubleSafely(h.etQuantity.getText().toString());
                double price = parseDoubleSafely(h.etUnitPrice.getText().toString());
                double lineTotal = BillingCalculator.calculateItemSubtotal(qty, price);

                JSONObject it = new JSONObject();
                it.put("description", desc);
                it.put("quantity", qty);
                it.put("price", price);
                it.put("total", lineTotal);
                itemsArray.put(it);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Item encoding error", Toast.LENGTH_SHORT).show();
            return;
        }

        final double paidNow = parseDoubleSafely(etPaidAmount.getText().toString());
        final String status = (calculatedBalance <= 0.01) ? "PAID" : (paidNow > 0 ? "PARTIAL" : "UNPAID");

        JSONObject invoiceData = new JSONObject();
        try {
            invoiceData.put("invoice_number", BillingCalculator.generateInvoiceNumber());
            invoiceData.put("customer_name", custName);
            invoiceData.put("customer_phone", custPhone);
            invoiceData.put("items_json", itemsArray.toString());
            invoiceData.put("subtotal", calculatedSubtotal);
            invoiceData.put("discount_percent", parseDoubleSafely(etDiscountPercent.getText().toString()));
            invoiceData.put("discount_amount", calculatedDiscount);
            invoiceData.put("tax_percent", parseDoubleSafely(etTaxPercent.getText().toString()));
            invoiceData.put("tax_amount", calculatedTax);
            invoiceData.put("total_amount", calculatedTotal);
            invoiceData.put("paid_amount", paidNow);
            invoiceData.put("balance_due", calculatedBalance);
            invoiceData.put("payment_status", status);
            invoiceData.put("date_created", BillingCalculator.getCurrentDateTimeFormatted());
        } catch (Exception e) {
            Toast.makeText(this, "JSON build error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (backendApi.isLoggedIn()) {
            saveProgress.setVisibility(View.VISIBLE);
            btnSaveInvoice.setEnabled(false);

            backendApi.createRecord("invoices", invoiceData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    saveProgress.setVisibility(View.GONE);
                    Toast.makeText(CreateInvoiceActivity.this, "Invoice created successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    saveProgress.setVisibility(View.GONE);
                    btnSaveInvoice.setEnabled(true);
                    Toast.makeText(CreateInvoiceActivity.this, "Saved locally (Cloud: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "Invoice created locally!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
}