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

public class PosActivity extends Activity {

    private BackendApi api;
    private EditText etPosCustomerName;
    private LinearLayout containerPosProducts;
    private LinearLayout containerCartItems;
    private TextView tvPosSubtotal;
    private EditText etPosDiscount;
    private EditText etPosTaxPercent;
    private TextView tvPosGrandTotal;
    private Button btnCheckoutSale;
    private ProgressBar progressPos;

    private static class CartItem {
        String id;
        String name;
        double price;
        int qty;
    }

    private final List<CartItem> cart = new ArrayList<>();
    private double calculatedSubtotal = 0.0;
    private double calculatedGrandTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);

        api = new BackendApi(this);

        etPosCustomerName = (EditText) findViewById(R.id.etPosCustomerName);
        containerPosProducts = (LinearLayout) findViewById(R.id.containerPosProducts);
        containerCartItems = (LinearLayout) findViewById(R.id.containerCartItems);
        tvPosSubtotal = (TextView) findViewById(R.id.tvPosSubtotal);
        etPosDiscount = (EditText) findViewById(R.id.etPosDiscount);
        etPosTaxPercent = (EditText) findViewById(R.id.etPosTaxPercent);
        tvPosGrandTotal = (TextView) findViewById(R.id.tvPosGrandTotal);
        btnCheckoutSale = (Button) findViewById(R.id.btnCheckoutSale);
        progressPos = (ProgressBar) findViewById(R.id.progressPos);

        findViewById(R.id.btnPosBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextWatcher recalcWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTotals();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etPosDiscount.addTextChangedListener(recalcWatcher);
        etPosTaxPercent.addTextChangedListener(recalcWatcher);

        btnCheckoutSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkoutSale();
            }
        });

        loadAvailableProducts();
    }

    private void loadAvailableProducts() {
        progressPos.setVisibility(View.VISIBLE);
        api.getData("products", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressPos.setVisibility(View.GONE);
                containerPosProducts.removeAllViews();
                JSONArray records = response.optJSONArray("records");

                if (records == null || records.length() == 0) {
                    TextView tv = new TextView(PosActivity.this);
                    tv.setText("No inventory available. Go to Stock to add items first.");
                    tv.setTextColor(Color.parseColor("#64748B"));
                    containerPosProducts.addView(tv);
                    return;
                }

                for (int i = 0; i < records.length(); i++) {
                    final JSONObject r = records.optJSONObject(i);
                    if (r == null) continue;
                    final String id = r.optString("id", "");
                    final JSONObject data = r.optJSONObject("data");
                    if (data == null) continue;

                    final String name = data.optString("name", "Product");
                    final double sell = data.optDouble("sell_price", 0.0);
                    final int stock = data.optInt("stock", 0);

                    LinearLayout pRow = new LinearLayout(PosActivity.this);
                    pRow.setOrientation(LinearLayout.HORIZONTAL);
                    pRow.setBackgroundColor(Color.WHITE);
                    pRow.setPadding(16, 12, 16, 12);
                    pRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 8);
                    pRow.setLayoutParams(lp);

                    LinearLayout info = new LinearLayout(PosActivity.this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

                    TextView tvName = new TextView(PosActivity.this);
                    tvName.setText(name);
                    tvName.setTextColor(Color.parseColor("#0F172A"));
                    tvName.setTextSize(14f);
                    tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                    info.addView(tvName);

                    TextView tvP = new TextView(PosActivity.this);
                    tvP.setText(BusinessLogic.getFormattedCurrency(sell) + " | Stock: " + stock);
                    tvP.setTextColor(Color.parseColor("#64748B"));
                    tvP.setTextSize(12f);
                    info.addView(tvP);

                    pRow.addView(info, infoLp);

                    Button btnAdd = new Button(PosActivity.this);
                    btnAdd.setText("+ Add");
                    btnAdd.setTextSize(12f);
                    btnAdd.setBackgroundColor(Color.parseColor("#2563EB"));
                    btnAdd.setTextColor(Color.WHITE);
                    btnAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addToCart(id, name, sell);
                        }
                    });
                    pRow.addView(btnAdd);

                    containerPosProducts.addView(pRow);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressPos.setVisibility(View.GONE);
                Toast.makeText(PosActivity.this, "Failed to load products: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToCart(String id, String name, double price) {
        for (CartItem item : cart) {
            if (item.id.equals(id)) {
                item.qty++;
                renderCart();
                return;
            }
        }
        CartItem newItem = new CartItem();
        newItem.id = id;
        newItem.name = name;
        newItem.price = price;
        newItem.qty = 1;
        cart.add(newItem);
        renderCart();
    }

    private void renderCart() {
        containerCartItems.removeAllViews();
        calculatedSubtotal = 0.0;

        if (cart.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Cart is empty. Add products from the catalog above.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(8, 12, 8, 12);
            containerCartItems.addView(empty);
            recalculateTotals();
            return;
        }

        for (final CartItem item : cart) {
            double lineTotal = item.price * item.qty;
            calculatedSubtotal += lineTotal;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(4, 6, 4, 6);

            TextView tvItem = new TextView(this);
            tvItem.setText(item.name + " (" + BusinessLogic.getFormattedCurrency(item.price) + ")");
            tvItem.setTextColor(Color.parseColor("#0F172A"));
            tvItem.setTextSize(13f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tvItem, lp);

            Button btnMinus = new Button(this);
            btnMinus.setText("-");
            btnMinus.setTextSize(12f);
            btnMinus.setLayoutParams(new LinearLayout.LayoutParams(90, 80));
            btnMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.qty--;
                    if (item.qty <= 0) {
                        cart.remove(item);
                    }
                    renderCart();
                }
            });
            row.addView(btnMinus);

            TextView tvQty = new TextView(this);
            tvQty.setText("  " + item.qty + "  ");
            tvQty.setTextSize(13f);
            tvQty.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvQty);

            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setTextSize(12f);
            btnPlus.setLayoutParams(new LinearLayout.LayoutParams(90, 80));
            btnPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.qty++;
                    renderCart();
                }
            });
            row.addView(btnPlus);

            TextView tvLine = new TextView(this);
            tvLine.setText(" " + BusinessLogic.getFormattedCurrency(lineTotal));
            tvLine.setTextColor(Color.parseColor("#0F172A"));
            tvLine.setTextSize(13f);
            tvLine.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvLine);

            containerCartItems.addView(row);
        }

        recalculateTotals();
    }

    private void recalculateTotals() {
        tvPosSubtotal.setText(BusinessLogic.getFormattedCurrency(calculatedSubtotal));

        double discount = 0.0;
        try {
            String dStr = etPosDiscount.getText().toString().trim();
            if (!dStr.isEmpty()) discount = Double.parseDouble(dStr);
        } catch (Exception ignored) {}

        double taxPercent = 0.0;
        try {
            String tStr = etPosTaxPercent.getText().toString().trim();
            if (!tStr.isEmpty()) taxPercent = Double.parseDouble(tStr);
        } catch (Exception ignored) {}

        calculatedGrandTotal = BusinessLogic.calculateInvoiceTotal(calculatedSubtotal, discount, taxPercent);
        tvPosGrandTotal.setText(BusinessLogic.getFormattedCurrency(calculatedGrandTotal));
    }

    private void checkoutSale() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Add at least one item to cart.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String customerName = etPosCustomerName.getText().toString().trim().isEmpty() ?
                "Walk-in Customer" : etPosCustomerName.getText().toString().trim();

        final String invoiceNo = "INV-" + (System.currentTimeMillis() % 100000);
        final StringBuilder summary = new StringBuilder();
        for (CartItem ci : cart) {
            summary.append(ci.name).append(" x").append(ci.qty).append(", ");
        }

        double discount = 0.0;
        try {
            discount = Double.parseDouble(etPosDiscount.getText().toString().trim());
        } catch (Exception ignored) {}

        double taxPercent = 0.0;
        try {
            taxPercent = Double.parseDouble(etPosTaxPercent.getText().toString().trim());
        } catch (Exception ignored) {}

        double taxAmount = ((calculatedSubtotal - discount) * taxPercent) / 100.0;
        if (taxAmount < 0) taxAmount = 0;

        progressPos.setVisibility(View.VISIBLE);
        btnCheckoutSale.setEnabled(false);

        try {
            JSONObject data = new JSONObject();
            data.put("invoice_no", invoiceNo);
            data.put("customer_name", customerName);
            data.put("subtotal", calculatedSubtotal);
            data.put("discount", discount);
            data.put("tax", taxAmount);
            data.put("total", calculatedGrandTotal);
            data.put("payment_mode", "Cash / UPI");
            data.put("items_summary", summary.toString());
            data.put("date", BusinessLogic.getCurrentDate());

            api.createData("sales", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressPos.setVisibility(View.GONE);
                    btnCheckoutSale.setEnabled(true);
                    Toast.makeText(PosActivity.this, "Sale Recorded Successfully!", Toast.LENGTH_SHORT).show();

                    // Generate shareable invoice summary
                    String invoiceText = "================================\n"
                            + "          VYAPAR PRO INVOICE\n"
                            + "================================\n"
                            + "Invoice No: " + invoiceNo + "\n"
                            + "Date: " + BusinessLogic.getCurrentDate() + "\n"
                            + "Customer: " + customerName + "\n"
                            + "--------------------------------\n"
                            + "Items:\n";

                    for (CartItem ci : cart) {
                        invoiceText += "- " + ci.name + " (" + ci.qty + "x) : "
                                + BusinessLogic.getFormattedCurrency(ci.price * ci.qty) + "\n";
                    }

                    invoiceText += "--------------------------------\n"
                            + "Subtotal: " + BusinessLogic.getFormattedCurrency(calculatedSubtotal) + "\n"
                            + "Grand Total: " + BusinessLogic.getFormattedCurrency(calculatedGrandTotal) + "\n"
                            + "================================\n"
                            + "Thank you for your business!";

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "VyaparPro Invoice " + invoiceNo);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, invoiceText);
                    startActivity(Intent.createChooser(shareIntent, "Share Invoice Summary"));

                    cart.clear();
                    renderCart();
                }

                @Override
                public void onError(String errorMessage) {
                    progressPos.setVisibility(View.GONE);
                    btnCheckoutSale.setEnabled(true);
                    Toast.makeText(PosActivity.this, "Error saving sale: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressPos.setVisibility(View.GONE);
            btnCheckoutSale.setEnabled(true);
        }
    }
}