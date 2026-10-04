package com.omniflow.erp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ERPActivity extends Activity {
    private BackendApi api;
    private ProgressDialog progressDialog;

    private TextView tvUserSession;
    private Button btnSignOut;

    private Button tabDashboard, tabInventory, tabPOS, tabExpenses;

    private View panelDashboard, panelInventory, panelPOS, panelExpenses;

    private TextView tvTotalRevenue, tvTotalExpenses, tvNetOperatingFlow, tvWarehouseAlerts;
    private Button btnRefreshDashboard;

    private EditText etInvName, etInvSku, etInvWarehouse, etInvStock, etInvReorder, etInvWholesale, etInvRetail;
    private Button btnSaveInventory;
    private LinearLayout listInventoryContainer;

    private EditText etPosCustomer, etPosQty, etPosDiscount, etPosTaxRate;
    private Spinner spinnerPosItems;
    private Button btnPosAddToCart, btnSubmitInvoice;
    private TextView tvPosCartContent, tvPosSubtotal, tvPosTaxCalculated, tvPosTotalAmount;
    private LinearLayout listTransactionsContainer;

    private EditText etExpTitle, etExpCategory, etExpAmount;
    private Button btnSaveExpense;
    private LinearLayout listExpensesContainer;

    private List<JSONObject> inventoryItemsList = new ArrayList<JSONObject>();
    private List<JSONObject> transactionsList = new ArrayList<JSONObject>();
    private List<JSONObject> expensesList = new ArrayList<JSONObject>();

    private Map<String, Integer> currentCart = new HashMap<String, Integer>();
    private double currentSubtotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erp);

        api = new BackendApi(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing Enterprise Server...");
        progressDialog.setCancelable(false);

        tvUserSession = (TextView) findViewById(R.id.tvUserSession);
        btnSignOut = (Button) findViewById(R.id.btnSignOut);
        tvUserSession.setText("Session: " + api.getEmail());

        tabDashboard = (Button) findViewById(R.id.tabDashboard);
        tabInventory = (Button) findViewById(R.id.tabInventory);
        tabPOS = (Button) findViewById(R.id.tabPOS);
        tabExpenses = (Button) findViewById(R.id.tabExpenses);

        panelDashboard = findViewById(R.id.panelDashboard);
        panelInventory = findViewById(R.id.panelInventory);
        panelPOS = findViewById(R.id.panelPOS);
        panelExpenses = findViewById(R.id.panelExpenses);

        tvTotalRevenue = (TextView) findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = (TextView) findViewById(R.id.tvTotalExpenses);
        tvNetOperatingFlow = (TextView) findViewById(R.id.tvNetOperatingFlow);
        tvWarehouseAlerts = (TextView) findViewById(R.id.tvWarehouseAlerts);
        btnRefreshDashboard = (Button) findViewById(R.id.btnRefreshDashboard);

        etInvName = (EditText) findViewById(R.id.etInvName);
        etInvSku = (EditText) findViewById(R.id.etInvSku);
        etInvWarehouse = (EditText) findViewById(R.id.etInvWarehouse);
        etInvStock = (EditText) findViewById(R.id.etInvStock);
        etInvReorder = (EditText) findViewById(R.id.etInvReorder);
        etInvWholesale = (EditText) findViewById(R.id.etInvWholesale);
        etInvRetail = (EditText) findViewById(R.id.etInvRetail);
        btnSaveInventory = (Button) findViewById(R.id.btnSaveInventory);
        listInventoryContainer = (LinearLayout) findViewById(R.id.listInventoryContainer);

        etPosCustomer = (EditText) findViewById(R.id.etPosCustomer);
        etPosQty = (EditText) findViewById(R.id.etPosQty);
        etPosDiscount = (EditText) findViewById(R.id.etPosDiscount);
        etPosTaxRate = (EditText) findViewById(R.id.etPosTaxRate);
        spinnerPosItems = (Spinner) findViewById(R.id.spinnerPosItems);
        btnPosAddToCart = (Button) findViewById(R.id.btnPosAddToCart);
        btnSubmitInvoice = (Button) findViewById(R.id.btnSubmitInvoice);
        tvPosCartContent = (TextView) findViewById(R.id.tvPosCartContent);
        tvPosSubtotal = (TextView) findViewById(R.id.tvPosSubtotal);
        tvPosTaxCalculated = (TextView) findViewById(R.id.tvPosTaxCalculated);
        tvPosTotalAmount = (TextView) findViewById(R.id.tvPosTotalAmount);
        listTransactionsContainer = (LinearLayout) findViewById(R.id.listTransactionsContainer);

        etExpTitle = (EditText) findViewById(R.id.etExpTitle);
        etExpCategory = (EditText) findViewById(R.id.etExpCategory);
        etExpAmount = (EditText) findViewById(R.id.etExpAmount);
        btnSaveExpense = (Button) findViewById(R.id.btnSaveExpense);
        listExpensesContainer = (LinearLayout) findViewById(R.id.listExpensesContainer);

        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelDashboard, tabDashboard);
            }
        });

        tabInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelInventory, tabInventory);
            }
        });

        tabPOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelPOS, tabPOS);
            }
        });

        tabExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelExpenses, tabExpenses);
            }
        });

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        progressDialog.dismiss();
                        api.clearSession();
                        Toast.makeText(ERPActivity.this, "Session closed.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ERPActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        progressDialog.dismiss();
                        api.clearSession();
                        Toast.makeText(ERPActivity.this, "Forced logout.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ERPActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });

        btnRefreshDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncAllData();
            }
        });

        btnSaveInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInventoryItem();
            }
        });

        btnPosAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToPosCart();
            }
        });

        btnSubmitInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFinalInvoice();
            }
        });

        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExpenseVoucher();
            }
        });

        syncAllData();
    }

    private void switchTab(View activePanel, Button activeButton) {
        panelDashboard.setVisibility(View.GONE);
        panelInventory.setVisibility(View.GONE);
        panelPOS.setVisibility(View.GONE);
        panelExpenses.setVisibility(View.GONE);

        tabDashboard.setBackgroundColor(Color.parseColor("#424242"));
        tabInventory.setBackgroundColor(Color.parseColor("#424242"));
        tabPOS.setBackgroundColor(Color.parseColor("#424242"));
        tabExpenses.setBackgroundColor(Color.parseColor("#424242"));

        activePanel.setVisibility(View.VISIBLE);
        activeButton.setBackgroundColor(Color.parseColor("#03A9F4"));
    }

    private void syncAllData() {
        progressDialog.show();
        loadInventory(new Runnable() {
            @Override
            public void run() {
                loadExpenses(new Runnable() {
                    @Override
                    public void run() {
                        loadTransactions(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                recalculateDashboardMetrics();
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadInventory(final Runnable nextStep) {
        api.readRecords("inventory", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                inventoryItemsList.clear();
                try {
                    if (response.optBoolean("success", false)) {
                        JSONArray records = response.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            JSONObject data = rec.getJSONObject("data");
                            data.put("id", rec.getString("id"));
                            inventoryItemsList.add(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                renderInventoryList();
                updatePosSpinner();
                if (nextStep != null) {
                    nextStep.run();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ERPActivity.this, "Load inventory failed: " + message, Toast.LENGTH_SHORT).show();
                if (nextStep != null) {
                    nextStep.run();
                }
            }
        });
    }

    private void createInventoryItem() {
        String name = etInvName.getText().toString().trim();
        String sku = etInvSku.getText().toString().trim();
        String warehouse = etInvWarehouse.getText().toString().trim();
        String stockStr = etInvStock.getText().toString().trim();
        String reorderStr = etInvReorder.getText().toString().trim();
        String wholesaleStr = etInvWholesale.getText().toString().trim();
        String retailStr = etInvRetail.getText().toString().trim();

        if (name.isEmpty() || sku.isEmpty() || stockStr.isEmpty() || retailStr.isEmpty()) {
            Toast.makeText(this, "Name, SKU, Stock, and Retail Price are mandatory.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double stock = Double.parseDouble(stockStr);
            double reorder = reorderStr.isEmpty() ? 5.0 : Double.parseDouble(reorderStr);
            double wholesale = wholesaleStr.isEmpty() ? Double.parseDouble(retailStr) * 0.7 : Double.parseDouble(wholesaleStr);
            double retail = Double.parseDouble(retailStr);

            JSONObject data = new JSONObject();
            data.put("item_name", name);
            data.put("sku", sku);
            data.put("warehouse", warehouse.isEmpty() ? "General-A" : warehouse);
            data.put("stock", stock);
            data.put("reorder_level", reorder);
            data.put("wholesale_price", wholesale);
            data.put("retail_price", retail);

            progressDialog.show();
            api.createRecord("inventory", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressDialog.dismiss();
                    Toast.makeText(ERPActivity.this, "Item added to inventory!", Toast.LENGTH_SHORT).show();
                    etInvName.setText("");
                    etInvSku.setText("");
                    etInvWarehouse.setText("");
                    etInvStock.setText("");
                    etInvReorder.setText("");
                    etInvWholesale.setText("");
                    etInvRetail.setText("");

                    loadInventory(new Runnable() {
                        @Override
                        public void run() {
                            recalculateDashboardMetrics();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    progressDialog.dismiss();
                    Toast.makeText(ERPActivity.this, "Failed to create item: " + message, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Invalid numeric formats.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderInventoryList() {
        listInventoryContainer.removeAllViews();
        for (int i = 0; i < inventoryItemsList.size(); i++) {
            final JSONObject item = inventoryItemsList.get(i);
            try {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(12, 12, 12, 12);
                card.setBackgroundColor(Color.parseColor("#1E1E1E"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 12);
                card.setLayoutParams(params);

                TextView tvTitle = new TextView(this);
                tvTitle.setText(item.optString("item_name") + " (" + item.optString("sku") + ")");
                tvTitle.setTextColor(Color.WHITE);
                tvTitle.setTextSize(14f);
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvWarehouseDetails = new TextView(this);
                tvWarehouseDetails.setText("Warehouse: " + item.optString("warehouse") + " | Reorder level: " + item.optDouble("reorder_level", 0.0));
                tvWarehouseDetails.setTextColor(Color.parseColor("#B0BEC5"));
                tvWarehouseDetails.setTextSize(11f);

                TextView tvPriceDetails = new TextView(this);
                tvPriceDetails.setText("Retail Price: $" + item.optDouble("retail_price", 0.0) + " | Wholesale: $" + item.optDouble("wholesale_price", 0.0));
                tvPriceDetails.setTextColor(Color.parseColor("#03A9F4"));
                tvPriceDetails.setTextSize(11f);

                TextView tvStockDetails = new TextView(this);
                double stock = item.optDouble("stock", 0.0);
                double reorder = item.optDouble("reorder_level", 0.0);
                tvStockDetails.setText("STOCK UNITS: " + stock);
                if (stock <= reorder) {
                    tvStockDetails.setTextColor(Color.parseColor("#FF5722"));
                    tvStockDetails.setText("STOCK UNITS: " + stock + " (CRITICAL ALERT)");
                } else {
                    tvStockDetails.setTextColor(Color.parseColor("#4CAF50"));
                }
                tvStockDetails.setTextSize(12f);
                tvStockDetails.setTypeface(null, android.graphics.Typeface.BOLD);

                card.addView(tvTitle);
                card.addView(tvWarehouseDetails);
                card.addView(tvPriceDetails);
                card.addView(tvStockDetails);

                listInventoryContainer.addView(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadExpenses(final Runnable nextStep) {
        api.readRecords("expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                expensesList.clear();
                try {
                    if (response.optBoolean("success", false)) {
                        JSONArray records = response.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            JSONObject data = rec.getJSONObject("data");
                            data.put("id", rec.getString("id"));
                            expensesList.add(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                renderExpensesList();
                if (nextStep != null) {
                    nextStep.run();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ERPActivity.this, "Load expenses failure: " + message, Toast.LENGTH_SHORT).show();
                if (nextStep != null) {
                    nextStep.run();
                }
            }
        });
    }

    private void submitExpenseVoucher() {
        String title = etExpTitle.getText().toString().trim();
        String category = etExpCategory.getText().toString().trim();
        String amtStr = etExpAmount.getText().toString().trim();

        if (title.isEmpty() || category.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Fill in all fields to record expense.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("category", category);
            data.put("amount", amt);
            data.put("date", "2024-11-20");
            data.put("status", "APPROVED");

            progressDialog.show();
            api.createRecord("expenses", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressDialog.dismiss();
                    Toast.makeText(ERPActivity.this, "Expense recorded successfully!", Toast.LENGTH_SHORT).show();
                    etExpTitle.setText("");
                    etExpCategory.setText("");
                    etExpAmount.setText("");

                    loadExpenses(new Runnable() {
                        @Override
                        public void run() {
                            recalculateDashboardMetrics();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    progressDialog.dismiss();
                    Toast.makeText(ERPActivity.this, "Voucher failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Voucher amount formatting error", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderExpensesList() {
        listExpensesContainer.removeAllViews();
        for (int i = 0; i < expensesList.size(); i++) {
            JSONObject exp = expensesList.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(12, 12, 12, 12);
            card.setBackgroundColor(Color.parseColor("#1E1E1E"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(exp.optString("title"));
            tvTitle.setTextColor(Color.WHITE);
            tvTitle.setTextSize(14f);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Category: " + exp.optString("category") + " | Status: " + exp.optString("status"));
            tvMeta.setTextColor(Color.parseColor("#FF5722"));
            tvMeta.setTextSize(11f);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("Amount: $" + exp.optDouble("amount", 0.0));
            tvPrice.setTextColor(Color.WHITE);
            tvPrice.setTextSize(13f);
            tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);

            card.addView(tvTitle);
            card.addView(tvMeta);
            card.addView(tvPrice);
            listExpensesContainer.addView(card);
        }
    }

    private void loadTransactions(final Runnable nextStep) {
        api.readRecords("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                transactionsList.clear();
                try {
                    if (response.optBoolean("success", false)) {
                        JSONArray records = response.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            JSONObject data = rec.getJSONObject("data");
                            data.put("id", rec.getString("id"));
                            transactionsList.add(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                renderTransactionsList();
                if (nextStep != null) {
                    nextStep.run();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ERPActivity.this, "Transactions failed: " + message, Toast.LENGTH_SHORT).show();
                if (nextStep != null) {
                    nextStep.run();
                }
            }
        });
    }

    private void updatePosSpinner() {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < inventoryItemsList.size(); i++) {
            JSONObject item = inventoryItemsList.get(i);
            names.add(item.optString("item_name") + " [" + item.optString("sku") + "]");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPosItems.setAdapter(adapter);
    }

    private void addItemToPosCart() {
        int pos = spinnerPosItems.getSelectedItemPosition();
        if (pos < 0 || pos >= inventoryItemsList.size()) {
            Toast.makeText(this, "Select a valid warehouse item first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etPosQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter transaction quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                Toast.makeText(this, "Quantity must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject selected = inventoryItemsList.get(pos);
            String sku = selected.getString("sku");
            double stock = selected.getDouble("stock");

            int existingInCart = currentCart.containsKey(sku) ? currentCart.get(sku) : 0;
            if (existingInCart + qty > stock) {
                Toast.makeText(this, "Insufficient warehouse stock. Max Available: " + (stock - existingInCart), Toast.LENGTH_LONG).show();
                return;
            }

            currentCart.put(sku, existingInCart + qty);
            etPosQty.setText("");
            calculateLiveInvoice();
            Toast.makeText(this, "Added to active transaction cart.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Invalid POS quantity units", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateLiveInvoice() {
        if (currentCart.isEmpty()) {
            tvPosCartContent.setText("Cart empty. Add items to calculate invoice values.");
            tvPosSubtotal.setText("Subtotal: $0.00");
            tvPosTaxCalculated.setText("Calculated Tax: $0.00");
            tvPosTotalAmount.setText("Net Invoice Total: $0.00");
            currentSubtotal = 0.0;
            return;
        }

        StringBuilder cartBuilder = new StringBuilder();
        double subtotal = 0.0;

        for (Map.Entry<String, Integer> entry : currentCart.entrySet()) {
            String sku = entry.getKey();
            int qty = entry.getValue();

            JSONObject match = findInventoryBySku(sku);
            if (match != null) {
                double retail = match.optDouble("retail_price", 0.0);
                double lineTotal = retail * qty;
                subtotal += lineTotal;
                cartBuilder.append(match.optString("item_name")).append(" (x").append(qty).append(") - $").append(lineTotal).append("\n");
            }
        }

        currentSubtotal = subtotal;

        String discStr = etPosDiscount.getText().toString().trim();
        double discount = 0.0;
        if (!discStr.isEmpty()) {
            try {
                discount = Double.parseDouble(discStr);
            } catch (Exception ignored) {}
        }

        String taxStr = etPosTaxRate.getText().toString().trim();
        double taxRate = 0.10;
        if (!taxStr.isEmpty()) {
            try {
                taxRate = Double.parseDouble(taxStr);
            } catch (Exception ignored) {}
        }

        double taxableAmount = subtotal - discount;
        if (taxableAmount < 0) taxableAmount = 0;
        double taxAmt = taxableAmount * taxRate;
        double finalTotal = taxableAmount + taxAmt;

        tvPosCartContent.setText(cartBuilder.toString().trim());
        tvPosSubtotal.setText("Subtotal: $" + String.format("%.2f", subtotal) + " (Discount: $" + String.format("%.2f", discount) + ")");
        tvPosTaxCalculated.setText("Calculated Tax (" + (taxRate * 100) + "%): $" + String.format("%.2f", taxAmt));
        tvPosTotalAmount.setText("Net Invoice Total: $" + String.format("%.2f", finalTotal));
    }

    private JSONObject findInventoryBySku(String sku) {
        for (int i = 0; i < inventoryItemsList.size(); i++) {
            JSONObject item = inventoryItemsList.get(i);
            if (item.optString("sku").equals(sku)) {
                return item;
            }
        }
        return null;
    }

    private void submitFinalInvoice() {
        String customer = etPosCustomer.getText().toString().trim();
        if (customer.isEmpty()) {
            Toast.makeText(this, "Customer name is mandatory to open transaction ledger.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCart.isEmpty()) {
            Toast.makeText(this, "Active transaction cart is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double subtotal = currentSubtotal;

            String discStr = etPosDiscount.getText().toString().trim();
            double discount = 0.0;
            if (!discStr.isEmpty()) {
                try {
                    discount = Double.parseDouble(discStr);
                } catch (Exception ignored) {}
            }

            String taxStr = etPosTaxRate.getText().toString().trim();
            double taxRate = 0.10;
            if (!taxStr.isEmpty()) {
                try {
                    taxRate = Double.parseDouble(taxStr);
                } catch (Exception ignored) {}
            }

            double taxableAmount = subtotal - discount;
            if (taxableAmount < 0) taxableAmount = 0;
            double taxAmt = taxableAmount * taxRate;
            double finalTotal = taxableAmount + taxAmt;

            JSONObject data = new JSONObject();
            data.put("customer_name", customer);
            data.put("items", tvPosCartContent.getText().toString());
            data.put("subtotal", subtotal);
            data.put("discount", discount);
            data.put("tax", taxAmt);
            data.put("total", finalTotal);
            data.put("payment_status", "COMPLETED");

            progressDialog.show();
            api.createRecord("transactions", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    deductWarehouseStock(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(ERPActivity.this, "Invoice recorded in system ledger!", Toast.LENGTH_SHORT).show();
                            etPosCustomer.setText("");
                            etPosDiscount.setText("");
                            etPosTaxRate.setText("");
                            currentCart.clear();
                            calculateLiveInvoice();

                            syncAllData();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    progressDialog.dismiss();
                    Toast.makeText(ERPActivity.this, "POS Submission failed: " + message, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error in formatting final invoice calculation", Toast.LENGTH_SHORT).show();
        }
    }

    private void deductWarehouseStock(final Runnable onCompleted) {
        final List<Map.Entry<String, Integer>> cartItems = new ArrayList<Map.Entry<String, Integer>>(currentCart.entrySet());
        deductNextStockItem(cartItems, 0, onCompleted);
    }

    private void deductNextStockItem(final List<Map.Entry<String, Integer>> cartItems, final int index, final Runnable onCompleted) {
        if (index >= cartItems.size()) {
            onCompleted.run();
            return;
        }

        Map.Entry<String, Integer> entry = cartItems.get(index);
        String sku = entry.getKey();
        final int qtyDeducted = entry.getValue();

        JSONObject item = findInventoryBySku(sku);
        if (item != null) {
            try {
                double currentStock = item.getDouble("stock");
                double newStock = currentStock - qtyDeducted;
                if (newStock < 0) newStock = 0;

                JSONObject updatePayload = new JSONObject();
                updatePayload.put("item_name", item.getString("item_name"));
                updatePayload.put("sku", item.getString("sku"));
                updatePayload.put("warehouse", item.getString("warehouse"));
                updatePayload.put("stock", newStock);
                updatePayload.put("reorder_level", item.getDouble("reorder_level"));
                updatePayload.put("wholesale_price", item.getDouble("wholesale_price"));
                updatePayload.put("retail_price", item.getDouble("retail_price"));

                String recordId = item.getString("id");

                api.updateRecord(recordId, updatePayload, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        deductNextStockItem(cartItems, index + 1, onCompleted);
                    }

                    @Override
                    public void onError(String message) {
                        deductNextStockItem(cartItems, index + 1, onCompleted);
                    }
                });
            } catch (Exception e) {
                deductNextStockItem(cartItems, index + 1, onCompleted);
            }
        } else {
            deductNextStockItem(cartItems, index + 1, onCompleted);
        }
    }

    private void renderTransactionsList() {
        listTransactionsContainer.removeAllViews();
        for (int i = 0; i < transactionsList.size(); i++) {
            JSONObject tx = transactionsList.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(12, 12, 12, 12);
            card.setBackgroundColor(Color.parseColor("#1E1E1E"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            TextView tvCustomer = new TextView(this);
            tvCustomer.setText("Invoice To: " + tx.optString("customer_name"));
            tvCustomer.setTextColor(Color.WHITE);
            tvCustomer.setTextSize(14f);
            tvCustomer.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvItems = new TextView(this);
            tvItems.setText(tx.optString("items"));
            tvItems.setTextColor(Color.parseColor("#B0BEC5"));
            tvItems.setTextSize(11f);

            TextView tvTotal = new TextView(this);
            tvTotal.setText("Grand Total: $" + tx.optDouble("total", 0.0) + " (Tax: $" + tx.optDouble("tax", 0.0) + ")");
            tvTotal.setTextColor(Color.parseColor("#4CAF50"));
            tvTotal.setTextSize(13f);
            tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);

            card.addView(tvCustomer);
            card.addView(tvItems);
            card.addView(tvTotal);
            listTransactionsContainer.addView(card);
        }
    }

    private void recalculateDashboardMetrics() {
        double totalRevenue = 0.0;
        for (int i = 0; i < transactionsList.size(); i++) {
            totalRevenue += transactionsList.get(i).optDouble("total", 0.0);
        }

        double totalExpenses = 0.0;
        for (int i = 0; i < expensesList.size(); i++) {
            totalExpenses += expensesList.get(i).optDouble("amount", 0.0);
        }

        double netFlow = totalRevenue - totalExpenses;

        tvTotalRevenue.setText("$" + String.format("%.2f", totalRevenue));
        tvTotalExpenses.setText("$" + String.format("%.2f", totalExpenses));
        tvNetOperatingFlow.setText("$" + String.format("%.2f", netFlow));

        if (netFlow >= 0) {
            tvNetOperatingFlow.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvNetOperatingFlow.setTextColor(Color.parseColor("#FF5722"));
        }

        int warningCount = 0;
        StringBuilder alertsText = new StringBuilder();
        for (int i = 0; i < inventoryItemsList.size(); i++) {
            JSONObject item = inventoryItemsList.get(i);
            double stock = item.optDouble("stock", 0.0);
            double limit = item.optDouble("reorder_level", 0.0);
            if (stock <= limit) {
                warningCount++;
                alertsText.append("- ").append(item.optString("item_name"))
                        .append(" in ").append(item.optString("warehouse"))
                        .append(" has only ").append(stock).append(" left (limit ").append(limit).append(")\n");
            }
        }

        if (warningCount > 0) {
            tvWarehouseAlerts.setText("CRITICAL WAREHOUSE ALERTS (" + warningCount + "):\n" + alertsText.toString().trim());
            tvWarehouseAlerts.setBackgroundColor(Color.parseColor("#D32F2F"));
            tvWarehouseAlerts.setTextColor(Color.WHITE);
        } else {
            tvWarehouseAlerts.setText("All warehouse levels are fully stable.");
            tvWarehouseAlerts.setBackgroundColor(Color.parseColor("#1B5E20"));
            tvWarehouseAlerts.setTextColor(Color.WHITE);
        }
    }
}