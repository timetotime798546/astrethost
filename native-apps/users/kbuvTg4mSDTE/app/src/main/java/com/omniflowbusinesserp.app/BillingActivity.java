package com.omniflowbusinesserp.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class BillingActivity extends Activity {

    private EditText etCustomerName, etProductName, etProductPrice, etDiscountRate;
    private Button btnAddToCart, btnFinalizeInvoice;
    private LinearLayout layoutCartList;
    private TextView tvSubtotal, tvTaxCalculated, tvTotalWithTax;

    private BackendApi api;
    private String token;

    // Local in-memory cart logic representing dynamic business calculations
    private static class CartItem {
        String name;
        double price;

        CartItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    private final List<CartItem> cart = new ArrayList<>();
    private double currentSubtotal = 0.0;
    private double currentDiscount = 0.0;
    private double calculatedTax = 0.0;
    private double calculatedTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        token = prefs.getString("token", null);

        api = new BackendApi(this);

        etCustomerName = (EditText) findViewById(R.id.etCustomerName);
        etProductName = (EditText) findViewById(R.id.etProductName);
        etProductPrice = (EditText) findViewById(R.id.etProductPrice);
        etDiscountRate = (EditText) findViewById(R.id.etDiscountRate);

        btnAddToCart = (Button) findViewById(R.id.btnAddToCart);
        btnFinalizeInvoice = (Button) findViewById(R.id.btnFinalizeInvoice);
        layoutCartList = (LinearLayout) findViewById(R.id.layoutCartList);

        tvSubtotal = (TextView) findViewById(R.id.tvSubtotal);
        tvTaxCalculated = (TextView) findViewById(R.id.tvTaxCalculated);
        tvTotalWithTax = (TextView) findViewById(R.id.tvTotalWithTax);

        btnAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToCart();
            }
        });

        btnFinalizeInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizeInvoice();
            }
        });

        etDiscountRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        calculateTotals();
    }

    private void addItemToCart() {
        String prodName = etProductName.getText().toString().trim();
        String prodPriceStr = etProductPrice.getText().toString().trim();

        if (prodName.isEmpty() || prodPriceStr.isEmpty()) {
            Toast.makeText(this, "Product specifications empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(prodPriceStr);
            cart.add(new CartItem(prodName, price));

            etProductName.setText("");
            etProductPrice.setText("");

            renderCart();
            calculateTotals();
        } catch (Exception e) {
            Toast.makeText(this, "Invalid price amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderCart() {
        layoutCartList.removeAllViews();
        for (int i = 0; i < cart.size(); i++) {
            final CartItem item = cart.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);
            row.setBackgroundColor(0xFFFFFFFF);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 4);
            row.setLayoutParams(params);

            TextView tvItem = new TextView(this);
            tvItem.setText(item.name + " - $" + String.format("%.2f", item.price));
            tvItem.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            tvItem.setTextColor(0xFF333333);

            Button btnRemove = new Button(this);
            btnRemove.setText("REMOVE");
            btnRemove.setBackgroundColor(0xFFC62828);
            btnRemove.setTextColor(0xFFFFFFFF);
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cart.remove(index);
                    renderCart();
                    calculateTotals();
                }
            });

            row.addView(tvItem);
            row.addView(btnRemove);

            layoutCartList.addView(row);
        }
    }

    private void calculateTotals() {
        currentSubtotal = 0.0;
        for (int i = 0; i < cart.size(); i++) {
            currentSubtotal += cart.get(i).price;
        }

        String discountStr = etDiscountRate.getText().toString().trim();
        currentDiscount = 0.0;
        if (!discountStr.isEmpty()) {
            try {
                currentDiscount = Double.parseDouble(discountStr);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Tax logic: Complex local tax algorithm matching 18% GST standard policy
        double taxableAmount = currentSubtotal - currentDiscount;
        if (taxableAmount < 0) {
            taxableAmount = 0;
        }

        calculatedTax = taxableAmount * 0.18;
        calculatedTotal = taxableAmount + calculatedTax;

        tvSubtotal.setText("Subtotal: $" + String.format("%.2f", currentSubtotal));
        tvTaxCalculated.setText("Tax Calculated (18%): $" + String.format("%.2f", calculatedTax));
        tvTotalWithTax.setText("Grand Total: $" + String.format("%.2f", calculatedTotal));
    }

    private void finalizeInvoice() {
        String customer = etCustomerName.getText().toString().trim();
        if (customer.isEmpty()) {
            Toast.makeText(this, "Customer name is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cart.isEmpty()) {
            Toast.makeText(this, "The shopping cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Build simple summary string
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < cart.size(); i++) {
                if (i > 0) summary.append(", ");
                summary.append(cart.get(i).name);
            }

            JSONObject sale = new JSONObject();
            sale.put("invoice_no", "INV-" + System.currentTimeMillis() / 1000);
            sale.put("items_summary", summary.toString());
            sale.put("subtotal", currentSubtotal);
            sale.put("discount", currentDiscount);
            sale.put("tax", calculatedTax);
            sale.put("total", calculatedTotal);
            sale.put("customer", customer);

            api.createData(token, "sales", sale, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(BillingActivity.this, "Invoice final and saved to database!", Toast.LENGTH_LONG).show();
                    cart.clear();
                    etCustomerName.setText("");
                    etDiscountRate.setText("");
                    renderCart();
                    calculateTotals();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(BillingActivity.this, "POS cloud update failure: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}