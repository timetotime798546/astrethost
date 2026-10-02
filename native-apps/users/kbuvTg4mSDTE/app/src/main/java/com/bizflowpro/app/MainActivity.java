package com.bizflowpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private BackendApi backendApi;
    private SharedPreferences prefs;

    // View Containers
    private RelativeLayout layoutLoading;
    private ScrollView scrollLogin;
    private LinearLayout layoutWorkspace;
    private TextView txtHeaderTitle;
    private Button btnLogout;

    // Login Form Elements
    private EditText etEmail, etPassword;
    private Button btnLoginSubmit, btnRegisterSubmit;

    // Nav elements
    private Button navDashboard, navClients, navInventory, navInvoices;

    // Tab Views
    private ScrollView viewDashboard;
    private LinearLayout viewClients, viewInventory, viewInvoices;

    // Live calculations inputs & outputs (Invoicing Tab)
    private Spinner spinInvoiceClient, spinInvoiceItem;
    private EditText invoiceInputQty, invoiceInputPrice, invoiceInputDiscount, invoiceInputTax;
    private TextView calcRawSubtotal, calcDiscountDeduction, calcTaxAddition, calcNetTotal, calcMarginProfit;
    private Button btnPublishInvoice;

    // Dashboard dynamic counter outputs
    private TextView statTotalRevenue, statTotalProfit, statClientsCount, statLowStock;

    // Client Directory Form Inputs & List View
    private EditText inputClientName, inputClientPhone, inputClientEmail, inputClientAddress;
    private Button btnAddClient;
    private ListView listClients;

    // Inventory Form Inputs & List View
    private EditText inputItemName, inputItemSku, inputItemQty, inputItemCost, inputItemPrice;
    private Button btnAddItem;
    private ListView listInventory;

    private ListView listInvoicesHistory;

    // Local in-memory stores
    private ArrayList<JSONObject> clientsList = new ArrayList<>();
    private ArrayList<JSONObject> inventoryList = new ArrayList<>();
    private ArrayList<JSONObject> invoicesList = new ArrayList<>();

    // Data adapters
    private ClientsAdapter clientsAdapter;
    private InventoryAdapter inventoryAdapter;
    private InvoicesAdapter invoicesAdapter;

    // Spinner adapters
    private ArrayList<String> clientsSpinnerNames = new ArrayList<>();
    private ArrayList<String> inventorySpinnerNames = new ArrayList<>();
    private ArrayAdapter<String> clientsSpinnerAdapter;
    private ArrayAdapter<String> inventorySpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        prefs = getSharedPreferences("bizflow_prefs", Context.MODE_PRIVATE);

        initViews();
        setupListeners();
        checkSavedAuth();
    }

    private void initViews() {
        layoutLoading = (RelativeLayout) findViewById(R.id.layout_loading);
        scrollLogin = (ScrollView) findViewById(R.id.scroll_login);
        layoutWorkspace = (LinearLayout) findViewById(R.id.layout_workspace);
        txtHeaderTitle = (TextView) findViewById(R.id.txt_header_title);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        // Auth
        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLoginSubmit = (Button) findViewById(R.id.btn_login_submit);
        btnRegisterSubmit = (Button) findViewById(R.id.btn_register_submit);

        // Navigation
        navDashboard = (Button) findViewById(R.id.nav_dashboard);
        navClients = (Button) findViewById(R.id.nav_clients);
        navInventory = (Button) findViewById(R.id.nav_inventory);
        navInvoices = (Button) findViewById(R.id.nav_invoices);

        // Sub workspace views
        viewDashboard = (ScrollView) findViewById(R.id.view_dashboard);
        viewClients = (LinearLayout) findViewById(R.id.view_clients);
        viewInventory = (LinearLayout) findViewById(R.id.view_inventory);
        viewInvoices = (LinearLayout) findViewById(R.id.view_invoices);

        // Stats UI elements
        statTotalRevenue = (TextView) findViewById(R.id.stat_total_revenue);
        statTotalProfit = (TextView) findViewById(R.id.stat_total_profit);
        statClientsCount = (TextView) findViewById(R.id.stat_clients_count);
        statLowStock = (TextView) findViewById(R.id.stat_low_stock);

        // Clients inputs
        inputClientName = (EditText) findViewById(R.id.input_client_name);
        inputClientPhone = (EditText) findViewById(R.id.input_client_phone);
        inputClientEmail = (EditText) findViewById(R.id.input_client_email);
        inputClientAddress = (EditText) findViewById(R.id.input_client_address);
        btnAddClient = (Button) findViewById(R.id.btn_add_client);
        listClients = (ListView) findViewById(R.id.list_clients);

        // Inventory inputs
        inputItemName = (EditText) findViewById(R.id.input_item_name);
        inputItemSku = (EditText) findViewById(R.id.input_item_sku);
        inputItemQty = (EditText) findViewById(R.id.input_item_qty);
        inputItemCost = (EditText) findViewById(R.id.input_item_cost);
        inputItemPrice = (EditText) findViewById(R.id.input_item_price);
        btnAddItem = (Button) findViewById(R.id.btn_add_item);
        listInventory = (ListView) findViewById(R.id.list_inventory);

        // Calculator live elements
        spinInvoiceClient = (Spinner) findViewById(R.id.spin_invoice_client);
        spinInvoiceItem = (Spinner) findViewById(R.id.spin_invoice_item);
        invoiceInputQty = (EditText) findViewById(R.id.invoice_input_qty);
        invoiceInputPrice = (EditText) findViewById(R.id.invoice_input_price);
        invoiceInputDiscount = (EditText) findViewById(R.id.invoice_input_discount);
        invoiceInputTax = (EditText) findViewById(R.id.invoice_input_tax);

        calcRawSubtotal = (TextView) findViewById(R.id.calc_raw_subtotal);
        calcDiscountDeduction = (TextView) findViewById(R.id.calc_discount_deduction);
        calcTaxAddition = (TextView) findViewById(R.id.calc_tax_addition);
        calcNetTotal = (TextView) findViewById(R.id.calc_net_total);
        calcMarginProfit = (TextView) findViewById(R.id.calc_margin_profit);
        btnPublishInvoice = (Button) findViewById(R.id.btn_publish_invoice);

        listInvoicesHistory = (ListView) findViewById(R.id.list_invoices);

        // Instantiating adapters
        clientsAdapter = new ClientsAdapter();
        listClients.setAdapter(clientsAdapter);

        inventoryAdapter = new InventoryAdapter();
        listInventory.setAdapter(inventoryAdapter);

        invoicesAdapter = new InvoicesAdapter();
        listInvoicesHistory.setAdapter(invoicesAdapter);

        // Spinners binding
        clientsSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, clientsSpinnerNames);
        clientsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinInvoiceClient.setAdapter(clientsSpinnerAdapter);

        inventorySpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, inventorySpinnerNames);
        inventorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinInvoiceItem.setAdapter(inventorySpinnerAdapter);
    }

    private void setupListeners() {
        // Auth submit triggers
        btnLoginSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistrationFlow();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        // Tab switches
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        navClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        navInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        navInvoices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Add Client form
        btnAddClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewClient();
            }
        });

        // Add Inventory item form
        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewInventoryItem();
            }
        });

        // Live calculator triggers & inputs listener binding
        spinInvoiceItem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < inventoryList.size()) {
                    try {
                        JSONObject item = inventoryList.get(position);
                        double price = item.optDouble("unit_price", 0.0);
                        invoiceInputPrice.setText(String.valueOf(price));
                    } catch (Exception e) {}
                }
                triggerInvoiceCalculation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                triggerInvoiceCalculation();
            }
        };

        invoiceInputQty.addTextChangedListener(calculationWatcher);
        invoiceInputPrice.addTextChangedListener(calculationWatcher);
        invoiceInputDiscount.addTextChangedListener(calculationWatcher);
        invoiceInputTax.addTextChangedListener(calculationWatcher);

        // Create transaction post trigger
        btnPublishInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishCalculatedInvoice();
            }
        });
    }

    private void checkSavedAuth() {
        String token = prefs.getString("token", "");
        if (token != null && !token.isEmpty()) {
            backendApi.setToken(token);
            showMainSystem();
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        scrollLogin.setVisibility(View.VISIBLE);
        layoutWorkspace.setVisibility(View.GONE);
        btnLogout.setVisibility(View.GONE);
        txtHeaderTitle.setText("BizFlow Pro Account Portal");
    }

    private void showMainSystem() {
        scrollLogin.setVisibility(View.GONE);
        layoutWorkspace.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.VISIBLE);
        txtHeaderTitle.setText("BizFlow Pro Workspace");
        switchTab(0);
        refreshAllSystemData();
    }

    private void showLoading(final boolean active) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layoutLoading.setVisibility(active ? View.VISIBLE : View.GONE);
            }
        });
    }

    // Dynamic Swapper
    private void switchTab(int index) {
        // Reset navigation colors
        navDashboard.setBackgroundColor(0xFF2C3E50);
        navClients.setBackgroundColor(0xFF2C3E50);
        navInventory.setBackgroundColor(0xFF2C3E50);
        navInvoices.setBackgroundColor(0xFF2C3E50);

        navDashboard.setTextColor(0xFFCCCCCC);
        navClients.setTextColor(0xFFCCCCCC);
        navInventory.setTextColor(0xFFCCCCCC);
        navInvoices.setTextColor(0xFFCCCCCC);

        viewDashboard.setVisibility(View.GONE);
        viewClients.setVisibility(View.GONE);
        viewInventory.setVisibility(View.GONE);
        viewInvoices.setVisibility(View.GONE);

        switch (index) {
            case 0:
                navDashboard.setBackgroundColor(0xFF34495E);
                navDashboard.setTextColor(0xFFFFFFFF);
                viewDashboard.setVisibility(View.VISIBLE);
                break;
            case 1:
                navClients.setBackgroundColor(0xFF34495E);
                navClients.setTextColor(0xFFFFFFFF);
                viewClients.setVisibility(View.VISIBLE);
                break;
            case 2:
                navInventory.setBackgroundColor(0xFF34495E);
                navInventory.setTextColor(0xFFFFFFFF);
                viewInventory.setVisibility(View.VISIBLE);
                break;
            case 3:
                navInvoices.setBackgroundColor(0xFF34495E);
                navInvoices.setTextColor(0xFFFFFFFF);
                viewInvoices.setVisibility(View.VISIBLE);
                triggerInvoiceCalculation();
                break;
        }
    }

    private void triggerToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= REGISTER & LOGIN LOGIC =================
    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String pwd = etPassword.getText().toString().trim();

        if (email.isEmpty() || pwd.isEmpty()) {
            triggerToast("All credential parameters are mandatory.");
            return;
        }

        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = backendApi.login(email, pwd);
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success", false)) {
                        String token = json.getString("token");
                        backendApi.setToken(token);

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("token", token);
                        editor.apply();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMainSystem();
                            }
                        });
                    } else {
                        triggerToast("Authentication failed: Ensure parameters are accurate.");
                    }
                } catch (Exception e) {
                    triggerToast("Authentication Error: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    private void performRegistrationFlow() {
        final String email = etEmail.getText().toString().trim();
        final String pwd = etPassword.getText().toString().trim();

        if (email.isEmpty() || pwd.isEmpty()) {
            triggerToast("All parameters are necessary to register operations.");
            return;
        }

        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Step 1: Request account creation. Token is not sent in register response.
                    String registerResponse = backendApi.register(email, pwd);
                    JSONObject regJson = new JSONObject(registerResponse);

                    if (regJson.optBoolean("success", false)) {
                        // Step 2: Auto-login to generate and cache the live Bearer Token
                        String loginResponse = backendApi.login(email, pwd);
                        JSONObject logJson = new JSONObject(loginResponse);

                        if (logJson.optBoolean("success", false)) {
                            String token = logJson.getString("token");
                            backendApi.setToken(token);

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("token", token);
                            editor.apply();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showMainSystem();
                                    triggerToast("Account built successfully. Welcome aboard!");
                                }
                            });
                        } else {
                            triggerToast("Account registered, but initial session establishment failed.");
                        }
                    } else {
                        triggerToast("Registration failed. Change credential targets.");
                    }
                } catch (Exception e) {
                    triggerToast("Registration error: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    private void performLogout() {
        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    backendApi.logout();
                } catch (Exception e) {
                    // Log or handle gracefully
                } finally {
                    backendApi.setToken(null);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("token");
                    editor.apply();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLoginScreen();
                        }
                    });
                    showLoading(false);
                }
            }
        }).start();
    }

    // ================= COMPREHENSIVE DATA SYNCHRONIZATION =================
    private void refreshAllSystemData() {
        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Sync Clients
                    String clientResp = backendApi.readData("clients");
                    JSONObject clientsJson = new JSONObject(clientResp);
                    if (clientsJson.optBoolean("success", false)) {
                        JSONArray recordsArr = clientsJson.optJSONArray("records");
                        clientsList.clear();
                        clientsSpinnerNames.clear();
                        if (recordsArr != null) {
                            for (int i = 0; i < recordsArr.length(); i++) {
                                JSONObject rec = recordsArr.getJSONObject(i);
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    // Inject record id inside nested data object for simple handling
                                    data.put("_cloud_id", rec.getString("id"));
                                    clientsList.add(data);
                                    clientsSpinnerNames.add(data.optString("name", "Unnamed"));
                                }
                            }
                        }
                    }

                    // Sync Inventory
                    String inventoryResp = backendApi.readData("inventory");
                    JSONObject inventoryJson = new JSONObject(inventoryResp);
                    if (inventoryJson.optBoolean("success", false)) {
                        JSONArray recordsArr = inventoryJson.optJSONArray("records");
                        inventoryList.clear();
                        inventorySpinnerNames.clear();
                        if (recordsArr != null) {
                            for (int i = 0; i < recordsArr.length(); i++) {
                                JSONObject rec = recordsArr.getJSONObject(i);
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    data.put("_cloud_id", rec.getString("id"));
                                    inventoryList.add(data);
                                    inventorySpinnerNames.add(data.optString("item_name", "Unnamed") + " (" + data.optString("sku", "N/A") + ")");
                                }
                            }
                        }
                    }

                    // Sync Invoices
                    String invoicesResp = backendApi.readData("invoices");
                    JSONObject invoicesJson = new JSONObject(invoicesResp);
                    if (invoicesJson.optBoolean("success", false)) {
                        JSONArray recordsArr = invoicesJson.optJSONArray("records");
                        invoicesList.clear();
                        if (recordsArr != null) {
                            for (int i = 0; i < recordsArr.length(); i++) {
                                JSONObject rec = recordsArr.getJSONObject(i);
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    data.put("_cloud_id", rec.getString("id"));
                                    invoicesList.add(data);
                                }
                            }
                        }
                    }

                    // Calculations and dashboard updates
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            clientsAdapter.notifyDataSetChanged();
                            inventoryAdapter.notifyDataSetChanged();
                            invoicesAdapter.notifyDataSetChanged();

                            clientsSpinnerAdapter.notifyDataSetChanged();
                            inventorySpinnerAdapter.notifyDataSetChanged();

                            performDashboardOperationalMetrics();
                        }
                    });

                } catch (Exception e) {
                    triggerToast("Sync Pipeline Interrupted: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    // ================= CLIENT DIRECTORY WRITER =================
    private void createNewClient() {
        final String name = inputClientName.getText().toString().trim();
        final String phone = inputClientPhone.getText().toString().trim();
        final String email = inputClientEmail.getText().toString().trim();
        final String address = inputClientAddress.getText().toString().trim();

        if (name.isEmpty()) {
            triggerToast("Client designation is mandatory.");
            return;
        }

        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject rawClient = new JSONObject();
                    rawClient.put("name", name);
                    rawClient.put("phone", phone);
                    rawClient.put("email", email);
                    rawClient.put("address", address);

                    String response = backendApi.createData("clients", rawClient);
                    JSONObject resJson = new JSONObject(response);
                    if (resJson.optBoolean("success", false)) {
                        triggerToast("New client created in database.");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                inputClientName.setText("");
                                inputClientPhone.setText("");
                                inputClientEmail.setText("");
                                inputClientAddress.setText("");
                            }
                        });
                        refreshAllSystemData();
                    } else {
                        triggerToast("Error writing new client profiles.");
                    }
                } catch (Exception e) {
                    triggerToast("Client registration fault: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    // ================= INVENTORY CONTROL WRITER =================
    private void createNewInventoryItem() {
        final String name = inputItemName.getText().toString().trim();
        final String sku = inputItemSku.getText().toString().trim();
        final String qtyStr = inputItemQty.getText().toString().trim();
        final String costStr = inputItemCost.getText().toString().trim();
        final String priceStr = inputItemPrice.getText().toString().trim();

        if (name.isEmpty() || qtyStr.isEmpty() || costStr.isEmpty() || priceStr.isEmpty()) {
            triggerToast("Item name, quantity, standard costs and prices must be documented.");
            return;
        }

        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int qty = Integer.parseInt(qtyStr);
                    double cost = Double.parseDouble(costStr);
                    double price = Double.parseDouble(priceStr);

                    JSONObject rawStock = new JSONObject();
                    rawStock.put("item_name", name);
                    rawStock.put("sku", sku.isEmpty() ? "GENERIC" : sku);
                    rawStock.put("quantity", qty);
                    rawStock.put("cost_price", cost);
                    rawStock.put("unit_price", price);

                    String response = backendApi.createData("inventory", rawStock);
                    JSONObject resJson = new JSONObject(response);
                    if (resJson.optBoolean("success", false)) {
                        triggerToast("Item successfully logged to global supply registers.");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                inputItemName.setText("");
                                inputItemSku.setText("");
                                inputItemQty.setText("");
                                inputItemCost.setText("");
                                inputItemPrice.setText("");
                            }
                        });
                        refreshAllSystemData();
                    } else {
                        triggerToast("Fault generated writing items details.");
                    }
                } catch (Exception e) {
                    triggerToast("Supply inventory operation failure: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    // ================= MANDATORY OFFLINE CALCULATIONS =================
    // Local processing is completely independent and provides instant calculation feedback.
    private double currentCalculatedSubtotal = 0.0;
    private double currentCalculatedDiscountDeduction = 0.0;
    private double currentCalculatedTaxAddition = 0.0;
    private double currentCalculatedTotal = 0.0;
    private double currentCalculatedCostBasis = 0.0;
    private double currentCalculatedMarginProfit = 0.0;

    private void triggerInvoiceCalculation() {
        try {
            int qty = 1;
            try {
                qty = Integer.parseInt(invoiceInputQty.getText().toString());
            } catch (Exception e) {}

            double unitPrice = 0.0;
            try {
                unitPrice = Double.parseDouble(invoiceInputPrice.getText().toString());
            } catch (Exception e) {}

            double discountPercent = 0.0;
            try {
                discountPercent = Double.parseDouble(invoiceInputDiscount.getText().toString());
            } catch (Exception e) {}

            double taxPercent = 10.0;
            try {
                taxPercent = Double.parseDouble(invoiceInputTax.getText().toString());
            } catch (Exception e) {}

            // Cost basis derivation
            double itemCost = 0.0;
            int selectedItemPos = spinInvoiceItem.getSelectedItemPosition();
            if (selectedItemPos >= 0 && selectedItemPos < inventoryList.size()) {
                JSONObject item = inventoryList.get(selectedItemPos);
                itemCost = item.optDouble("cost_price", 0.0);
            }

            // Calculations executed offline locally
            currentCalculatedSubtotal = qty * unitPrice;
            currentCalculatedDiscountDeduction = currentCalculatedSubtotal * (discountPercent / 100.0);
            double discountedSubtotal = currentCalculatedSubtotal - currentCalculatedDiscountDeduction;
            currentCalculatedTaxAddition = discountedSubtotal * (taxPercent / 100.0);
            currentCalculatedTotal = discountedSubtotal + currentCalculatedTaxAddition;

            currentCalculatedCostBasis = qty * itemCost;
            currentCalculatedMarginProfit = currentCalculatedTotal - currentCalculatedCostBasis;

            // Update offline interface components
            calcRawSubtotal.setText(String.format("$%.2f", currentCalculatedSubtotal));
            calcDiscountDeduction.setText(String.format("-$%.2f", currentCalculatedDiscountDeduction));
            calcTaxAddition.setText(String.format("+$%.2f", currentCalculatedTaxAddition));
            calcNetTotal.setText(String.format("$%.2f", currentCalculatedTotal));
            calcMarginProfit.setText(String.format("$%.2f", currentCalculatedMarginProfit));

        } catch (Exception ex) {
            // Silence formatting calculation glitches
        }
    }

    // Dynamic stats computation on core data collections loaded from readData API
    private void performDashboardOperationalMetrics() {
        try {
            double totalRev = 0.0;
            double totalProf = 0.0;
            int clientsCount = clientsList.size();
            int lowStockCount = 0;

            // Calculations executed on invoices list
            for (JSONObject invoice : invoicesList) {
                totalRev += invoice.optDouble("total", 0.0);
                totalProf += invoice.optDouble("profit", 0.0);
            }

            // Calculation on stock list (alerts trigger at < 5 units)
            for (JSONObject stock : inventoryList) {
                int qty = stock.optInt("quantity", 0);
                if (qty < 5) {
                    lowStockCount++;
                }
            }

            // Bind values to statistics tiles
            statTotalRevenue.setText(String.format("$%.2f", totalRev));
            statTotalProfit.setText(String.format("$%.2f", totalProf));
            statClientsCount.setText(String.valueOf(clientsCount));
            statLowStock.setText(lowStockCount + " items low");

        } catch (Exception e) {
            // Silence telemetry glitches
        }
    }

    // ================= PUBLISH COMPLETED INVOICES TO CLOUD BASE =================
    private void publishCalculatedInvoice() {
        if (clientsSpinnerNames.isEmpty()) {
            triggerToast("You must register clients first to compile estimates.");
            return;
        }
        if (inventorySpinnerNames.isEmpty()) {
            triggerToast("Inventory supplies list is empty.");
            return;
        }

        final int clientIndex = spinInvoiceClient.getSelectedItemPosition();
        final int itemIndex = spinInvoiceItem.getSelectedItemPosition();

        if (clientIndex < 0 || itemIndex < 0) {
            triggerToast("Operational index selections are invalid.");
            return;
        }

        showLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject clientObj = clientsList.get(clientIndex);
                    JSONObject itemObj = inventoryList.get(itemIndex);

                    String clientName = clientObj.getString("name");
                    String summary = invoiceInputQty.getText().toString() + "x " + itemObj.getString("item_name");

                    // Gather computed variables matching structural mapping specifications
                    JSONObject rawInvoice = new JSONObject();
                    rawInvoice.put("client_name", clientName);
                    rawInvoice.put("items_summary", summary);
                    rawInvoice.put("subtotal", currentCalculatedSubtotal);
                    rawInvoice.put("discount_percent", Double.parseDouble(invoiceInputDiscount.getText().toString()));
                    rawInvoice.put("tax_percent", Double.parseDouble(invoiceInputTax.getText().toString()));
                    rawInvoice.put("total", currentCalculatedTotal);
                    rawInvoice.put("profit", currentCalculatedMarginProfit);

                    // Create remote persistent entry on backend data channel
                    String response = backendApi.createData("invoices", rawInvoice);
                    JSONObject resJson = new JSONObject(response);

                    if (resJson.optBoolean("success", false)) {
                        triggerToast("Invoice posted, logged & synchronized.");

                        // Automatically update current inventory item stock level locally & send PUT request update
                        int originalQty = itemObj.optInt("quantity", 0);
                        int usedQty = Integer.parseInt(invoiceInputQty.getText().toString());
                        int remainingQty = Math.max(0, originalQty - usedQty);

                        // Save updated supply registers using PUT API
                        JSONObject updatedStock = new JSONObject();
                        updatedStock.put("item_name", itemObj.getString("item_name"));
                        updatedStock.put("sku", itemObj.optString("sku", ""));
                        updatedStock.put("quantity", remainingQty);
                        updatedStock.put("cost_price", itemObj.optDouble("cost_price", 0.0));
                        updatedStock.put("unit_price", itemObj.optDouble("unit_price", 0.0));

                        backendApi.updateData(itemObj.getString("_cloud_id"), updatedStock);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                invoiceInputQty.setText("1");
                                invoiceInputDiscount.setText("0.0");
                            }
                        });

                        refreshAllSystemData();
                    } else {
                        triggerToast("Operational transaction failure.");
                    }
                } catch (Exception e) {
                    triggerToast("Estimate deployment error: " + e.getMessage());
                } finally {
                    showLoading(false);
                }
            }
        }).start();
    }

    // ================= DELETE RECORDS TRIGGERS =================
    private void requestClientDeletion(final String id, final String name) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Delete Operations");
        b.setMessage("Remove client: " + name + "? Historical transactions remain catalogued.");
        b.setPositiveButton("PERFORM DELETION", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLoading(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String resp = backendApi.deleteData(id);
                            JSONObject json = new JSONObject(resp);
                            if (json.optBoolean("success", false)) {
                                triggerToast("Client profiles successfully expunged.");
                                refreshAllSystemData();
                            }
                        } catch (Exception e) {
                            triggerToast("Deletion failure: " + e.getMessage());
                        } finally {
                            showLoading(false);
                        }
                    }
                }).start();
            }
        });
        b.setNegativeButton("ABORT", null);
        b.show();
    }

    private void requestInventoryDeletion(final String id, final String name) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Delete Inventory SKU");
        b.setMessage("Erase " + name + " from register logs?");
        b.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLoading(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String resp = backendApi.deleteData(id);
                            JSONObject json = new JSONObject(resp);
                            if (json.optBoolean("success", false)) {
                                triggerToast("Inventory catalog listing expunged.");
                                refreshAllSystemData();
                            }
                        } catch (Exception e) {
                            triggerToast("Deletion failure: " + e.getMessage());
                        } finally {
                            showLoading(false);
                        }
                    }
                }).start();
            }
        });
        b.setNegativeButton("ABORT", null);
        b.show();
    }

    // ================= ADAPTER CONSTRUCTIONS =================
    private class ClientsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return clientsList.size();
        }

        @Override
        public Object getItem(int position) {
            return clientsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            final JSONObject client = clientsList.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(client.optString("name", "Unnamed"));
            text1.setTextColor(0xFF2C3E50);
            text1.setTextSize(15sp);

            String subtitle = "Phone: " + client.optString("phone", "N/A") + " | Addr: " + client.optString("address", "N/A");
            text2.setText(subtitle);
            text2.setTextColor(0xFF7F8C8D);

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        requestClientDeletion(client.getString("_cloud_id"), client.getString("name"));
                    } catch (Exception e) {}
                    return true;
                }
            });

            return convertView;
        }
    }

    private class InventoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return inventoryList.size();
        }

        @Override
        public Object getItem(int position) {
            return inventoryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            final JSONObject item = inventoryList.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            int qty = item.optInt("quantity", 0);
            String alertLabel = qty < 5 ? " [CRITICAL STOCK ALARM]" : "";

            text1.setText(item.optString("item_name", "Unnamed Item") + alertLabel);
            text1.setTextColor(qty < 5 ? 0xFFC0392B : 0xFF27AE60);
            text1.setTextSize(15sp);

            String sub = "SKU: " + item.optString("sku", "N/A") + " | Available Stock: " + qty + " units | Unit Retail Price: $" + item.optDouble("unit_price", 0.0);
            text2.setText(sub);
            text2.setTextColor(0xFF5D6D7E);

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        requestInventoryDeletion(item.getString("_cloud_id"), item.getString("item_name"));
                    } catch (Exception e) {}
                    return true;
                }
            });

            return convertView;
        }
    }

    private class InvoicesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return invoicesList.size();
        }

        @Override
        public Object getItem(int position) {
            return invoicesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            JSONObject invoice = invoicesList.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText("Client: " + invoice.optString("client_name", "N/A"));
            text1.setTextColor(0xFF2C3E50);
            text1.setTextSize(15sp);

            String desc = invoice.optString("items_summary", "N/A") +
                    " | NET TOTAL: $" + String.format("%.2f", invoice.optDouble("total", 0.0)) +
                    " (Margin Profit: $" + String.format("%.2f", invoice.optDouble("profit", 0.0)) + ")";

            text2.setText(desc);
            text2.setTextColor(0xFF7F8C8D);

            return convertView;
        }
    }
}