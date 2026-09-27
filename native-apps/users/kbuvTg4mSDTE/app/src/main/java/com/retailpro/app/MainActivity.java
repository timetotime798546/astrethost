package com.retailpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {

    // Database helper instance
    private DatabaseHelper dbHelper;

    // UI Modules references
    private TextView toolbarTitle, toolbarShopName;
    private Button btnNavDash, btnNavBill, btnNavProducts, btnNavCustomers, btnNavPurchases, btnNavExpenses, btnNavHistory, btnNavReports, btnNavSettings;
    
    // View panels
    private View panelDashboard, panelBilling, panelProducts, panelCustomers, panelPurchases, panelExpenses, panelHistory, panelReports, panelSettings;

    // Current app state values
    private String currencySymbol = "₹";
    private int selectedNavIndex = 0; // 0: dash, 1: billing, etc.

    // Cart billing structures
    private static class CartItem {
        int productId;
        String productName;
        double sellingPrice;
        double mrp;
        double gstPercentage;
        double quantity;
        double stockLimit;
        String unit;
    }
    private ArrayList<CartItem> cartList = new ArrayList<>();
    private ArrayList<String> activeProductSpinnerList = new ArrayList<>();
    private ArrayList<Integer> activeProductSpinnerIds = new ArrayList<>();
    private ArrayList<String> activeCustomerSpinnerList = new ArrayList<>();
    private ArrayList<Integer> activeCustomerSpinnerIds = new ArrayList<>();
    private ArrayList<String> activeSupplierSpinnerList = new ArrayList<>();
    private ArrayList<Integer> activeSupplierSpinnerIds = new ArrayList<>();

    // Billing controls
    private EditText edtBillSearch, edtBillDiscount, edtBillPaidAmount;
    private Spinner spnBillProducts, spnBillCustomer, spnBillPayMode;
    private TextView txtBillSubtotal, txtBillTax, txtBillGrandTotal, txtBillDueAmount, txtEmptyCart;
    private LinearLayout layoutCartContainer;
    private Button btnBillAddToCart, btnBillSave;
    private TextView btnBillClearCart;

    // Dashboard controls
    private TextView txtDashSales, txtDashProfit, txtDashStockValue, txtDashLowStockCount, txtDashDueReceivables, txtDashCustomersCount, dashAlertText;
    private LinearLayout dashAlertBanner;
    private Button btnDashNewBill, btnDashAddProduct, btnDashAddExpense;

    // Product log controls
    private EditText edtProductSearch;
    private Button btnAddProductDialog;
    private ListView lstProducts;
    private TextView txtNoProducts;

    // Customer controls
    private EditText edtCustomerSearch;
    private Button btnAddCustomerDialog;
    private ListView lstCustomers;
    private TextView txtNoCustomers;

    // Purchase Panel controls
    private Spinner spnPurchaseSupplier, spnPurchaseProduct;
    private EditText edtPurchaseCost, edtPurchaseQty;
    private Button btnPurchaseSave, btnAddSupplierDialog;
    private LinearLayout layoutSuppliersList;

    // Expense Log Controls
    private EditText edtExpenseTitle, edtExpenseAmount;
    private Spinner spnExpenseCategory;
    private Button btnExpenseSave;
    private ListView lstExpenses;

    // Bill History Controls
    private ListView lstHistory;
    private TextView txtNoHistory;

    // Analytics Controls
    private TextView txtRepTotalSales, txtRepTotalPurchases, txtRepTotalExpenses, txtRepNetProfit, txtRepStockMeta;

    // Settings Configuration Inputs
    private EditText edtSetShopName, edtSetOwnerName, edtSetMobile, edtSetAddress, edtSetGst, edtSetInvPrefix, edtSetCurrency;
    private Button btnSettingsSave, btnSettingsResetData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        currencySymbol = dbHelper.getSetting("currency");

        initUI();
        setupNavigation();
        loadSettingsToInputs();
        refreshAllData();

        // Start from dashboard
        switchPanel(0);
    }

    private void initUI() {
        toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        toolbarShopName = (TextView) findViewById(R.id.toolbar_shop_name);

        // Header Shop update
        toolbarShopName.setText(dbHelper.getSetting("shop_name"));

        // Navigation Controls
        btnNavDash = (Button) findViewById(R.id.btn_nav_dashboard);
        btnNavBill = (Button) findViewById(R.id.btn_nav_billing);
        btnNavProducts = (Button) findViewById(R.id.btn_nav_products);
        btnNavCustomers = (Button) findViewById(R.id.btn_nav_customers);
        btnNavPurchases = (Button) findViewById(R.id.btn_nav_purchases);
        btnNavExpenses = (Button) findViewById(R.id.btn_nav_expenses);
        btnNavHistory = (Button) findViewById(R.id.btn_nav_history);
        btnNavReports = (Button) findViewById(R.id.btn_nav_reports);
        btnNavSettings = (Button) findViewById(R.id.btn_nav_settings);

        // Panel definitions
        panelDashboard = findViewById(R.id.panel_dashboard);
        panelBilling = findViewById(R.id.panel_billing);
        panelProducts = findViewById(R.id.panel_products);
        panelCustomers = findViewById(R.id.panel_customers);
        panelPurchases = findViewById(R.id.panel_purchases);
        panelExpenses = findViewById(R.id.panel_expenses);
        panelHistory = findViewById(R.id.panel_history);
        panelReports = findViewById(R.id.panel_reports);
        panelSettings = findViewById(R.id.panel_settings);

        // Dashboard Hooks
        txtDashSales = (TextView) findViewById(R.id.txt_dash_sales);
        txtDashProfit = (TextView) findViewById(R.id.txt_dash_profit);
        txtDashStockValue = (TextView) findViewById(R.id.txt_dash_stock_value);
        txtDashLowStockCount = (TextView) findViewById(R.id.txt_dash_low_stock_count);
        txtDashDueReceivables = (TextView) findViewById(R.id.txt_dash_due_receivables);
        txtDashCustomersCount = (TextView) findViewById(R.id.txt_dash_customers_count);
        dashAlertText = (TextView) findViewById(R.id.dash_alert_text);
        dashAlertBanner = (LinearLayout) findViewById(R.id.dash_alert_banner);
        btnDashNewBill = (Button) findViewById(R.id.btn_dash_new_bill);
        btnDashAddProduct = (Button) findViewById(R.id.btn_dash_add_product);
        btnDashAddExpense = (Button) findViewById(R.id.btn_dash_add_expense);

        // POS Billing hooks
        edtBillSearch = (EditText) findViewById(R.id.edt_bill_search);
        spnBillProducts = (Spinner) findViewById(R.id.spn_bill_products);
        btnBillAddToCart = (Button) findViewById(R.id.btn_bill_add_to_cart);
        btnBillClearCart = (TextView) findViewById(R.id.btn_bill_clear_cart);
        layoutCartContainer = (LinearLayout) findViewById(R.id.layout_cart_container);
        txtEmptyCart = (TextView) findViewById(R.id.txt_empty_cart);
        spnBillCustomer = (Spinner) findViewById(R.id.spn_bill_customer);
        spnBillPayMode = (Spinner) findViewById(R.id.spn_bill_pay_mode);
        edtBillDiscount = (EditText) findViewById(R.id.edt_bill_discount);
        edtBillPaidAmount = (EditText) findViewById(R.id.edt_bill_paid_amount);
        txtBillSubtotal = (TextView) findViewById(R.id.txt_bill_subtotal);
        txtBillTax = (TextView) findViewById(R.id.txt_bill_tax);
        txtBillGrandTotal = (TextView) findViewById(R.id.txt_bill_grand_total);
        txtBillDueAmount = (TextView) findViewById(R.id.txt_bill_due_amount);
        btnBillSave = (Button) findViewById(R.id.btn_bill_save);

        // Products Catalog Hooks
        edtProductSearch = (EditText) findViewById(R.id.edt_product_search);
        btnAddProductDialog = (Button) findViewById(R.id.btn_add_product_dialog);
        lstProducts = (ListView) findViewById(R.id.lst_products);
        txtNoProducts = (TextView) findViewById(R.id.txt_no_products);

        // Customers list Hooks
        edtCustomerSearch = (EditText) findViewById(R.id.edt_customer_search);
        btnAddCustomerDialog = (Button) findViewById(R.id.btn_add_customer_dialog);
        lstCustomers = (ListView) findViewById(R.id.lst_customers);
        txtNoCustomers = (TextView) findViewById(R.id.txt_no_customers);

        // Purchases log hooks
        spnPurchaseSupplier = (Spinner) findViewById(R.id.spn_purchase_supplier);
        spnPurchaseProduct = (Spinner) findViewById(R.id.spn_purchase_product);
        edtPurchaseCost = (EditText) findViewById(R.id.edt_purchase_cost);
        edtPurchaseQty = (EditText) findViewById(R.id.edt_purchase_qty);
        btnPurchaseSave = (Button) findViewById(R.id.btn_purchase_save);
        btnAddSupplierDialog = (Button) findViewById(R.id.btn_add_supplier_dialog);
        layoutSuppliersList = (LinearLayout) findViewById(R.id.layout_suppliers_list);

        // Expense Log hooks
        edtExpenseTitle = (EditText) findViewById(R.id.edt_expense_title);
        edtExpenseAmount = (EditText) findViewById(R.id.edt_expense_amount);
        spnExpenseCategory = (Spinner) findViewById(R.id.spn_expense_category);
        btnExpenseSave = (Button) findViewById(R.id.btn_expense_save);
        lstExpenses = (ListView) findViewById(R.id.lst_expenses);

        // Invoice History Hooks
        lstHistory = (ListView) findViewById(R.id.lst_history);
        txtNoHistory = (TextView) findViewById(R.id.txt_no_history);

        // Report hooks
        txtRepTotalSales = (TextView) findViewById(R.id.txt_rep_total_sales);
        txtRepTotalPurchases = (TextView) findViewById(R.id.txt_rep_total_purchases);
        txtRepTotalExpenses = (TextView) findViewById(R.id.txt_rep_total_expenses);
        txtRepNetProfit = (TextView) findViewById(R.id.txt_rep_net_profit);
        txtRepStockMeta = (TextView) findViewById(R.id.txt_rep_stock_meta);

        // System Settings inputs
        edtSetShopName = (EditText) findViewById(R.id.edt_set_shop_name);
        edtSetOwnerName = (EditText) findViewById(R.id.edt_set_owner_name);
        edtSetMobile = (EditText) findViewById(R.id.edt_set_mobile);
        edtSetAddress = (EditText) findViewById(R.id.edt_set_address);
        edtSetGst = (EditText) findViewById(R.id.edt_set_gst);
        edtSetInvPrefix = (EditText) findViewById(R.id.edt_set_inv_prefix);
        edtSetCurrency = (EditText) findViewById(R.id.edt_set_currency);
        btnSettingsSave = (Button) findViewById(R.id.btn_settings_save);
        btnSettingsResetData = (Button) findViewById(R.id.btn_settings_reset_data);

        // Populate standard static spinners
        ArrayAdapter<String> adapterPayMode = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Cash", "UPI", "Card", "Credit / Udhaar"});
        adapterPayMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBillPayMode.setAdapter(adapterPayMode);

        ArrayAdapter<String> adapterExpCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Utilities", "Rent & Rates", "Salaries", "Snacks & Refreshments", "Logistics", "Others"});
        adapterExpCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnExpenseCategory.setAdapter(adapterExpCat);
    }

    private void setupNavigation() {
        // Navigation Bar Listeners
        btnNavDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(0); }
        });
        btnNavBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(1); }
        });
        btnNavProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(2); }
        });
        btnNavCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(3); }
        });
        btnNavPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(4); }
        });
        btnNavExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(5); }
        });
        btnNavHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(6); }
        });
        btnNavReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(7); }
        });
        btnNavSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(8); }
        });

        // Dashboard quick links
        btnDashNewBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(1); }
        });
        btnDashAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPanel(2);
                openAddProductDialog();
            }
        });
        btnDashAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchPanel(5); }
        });

        // Catalog Search Listeners
        edtProductSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { loadProductsList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAddProductDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openAddProductDialog(); }
        });

        // Client search Listeners
        edtCustomerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { loadCustomersList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAddCustomerDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openAddCustomerDialog(); }
        });

        // POS add to cart
        btnBillAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProductToCartFromSpinner();
            }
        });

        // POS barcode text search/barcode entry
        edtBillSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleBarcodeOrSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnBillClearCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartList.clear();
                renderCartRows();
                calculateInvoiceTotals();
            }
        });

        // Realtime calculation fields
        edtBillDiscount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateInvoiceTotals(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        edtBillPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateInvoiceTotals(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnBillSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processAndSaveSaleBill();
            }
        });

        // Record restock purchases
        btnPurchaseSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordSupplierPurchase();
            }
        });

        btnAddSupplierDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddSupplierDialog();
            }
        });

        // Store Expenses recording
        btnExpenseSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordStoreExpense();
            }
        });

        // System Settings update saves
        btnSettingsSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSystemConfig();
            }
        });

        btnSettingsResetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAndResetDatabase();
            }
        });
    }

    private void switchPanel(int index) {
        selectedNavIndex = index;

        // Visual reset state for navigation buttons
        btnNavDash.setBackgroundColor(0x00000000);
        btnNavBill.setBackgroundColor(0x00000000);
        btnNavProducts.setBackgroundColor(0x00000000);
        btnNavCustomers.setBackgroundColor(0x00000000);
        btnNavPurchases.setBackgroundColor(0x00000000);
        btnNavExpenses.setBackgroundColor(0x00000000);
        btnNavHistory.setBackgroundColor(0x00000000);
        btnNavReports.setBackgroundColor(0x00000000);
        btnNavSettings.setBackgroundColor(0x00000000);

        panelDashboard.setVisibility(View.GONE);
        panelBilling.setVisibility(View.GONE);
        panelProducts.setVisibility(View.GONE);
        panelCustomers.setVisibility(View.GONE);
        panelPurchases.setVisibility(View.GONE);
        panelExpenses.setVisibility(View.GONE);
        panelHistory.setVisibility(View.GONE);
        panelReports.setVisibility(View.GONE);
        panelSettings.setVisibility(View.GONE);

        switch (index) {
            case 0:
                btnNavDash.setBackgroundColor(0x30FFFFFF);
                panelDashboard.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Retail Dashboard");
                refreshDashboardMetrics();
                break;
            case 1:
                btnNavBill.setBackgroundColor(0x30FFFFFF);
                panelBilling.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Point of Sale (POS)");
                refreshPOSSpinners();
                break;
            case 2:
                btnNavProducts.setBackgroundColor(0x30FFFFFF);
                panelProducts.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Product Management");
                loadProductsList("");
                break;
            case 3:
                btnNavCustomers.setBackgroundColor(0x30FFFFFF);
                panelCustomers.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Customer Directory");
                loadCustomersList("");
                break;
            case 4:
                btnNavPurchases.setBackgroundColor(0x30FFFFFF);
                panelPurchases.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Vendor Purchases");
                refreshPurchasesSpinners();
                loadSuppliersDirectory();
                break;
            case 5:
                btnNavExpenses.setBackgroundColor(0x30FFFFFF);
                panelExpenses.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Shop Expenses");
                loadExpensesList();
                break;
            case 6:
                btnNavHistory.setBackgroundColor(0x30FFFFFF);
                panelHistory.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Billing Invoices History");
                loadSalesHistory();
                break;
            case 7:
                btnNavReports.setBackgroundColor(0x30FFFFFF);
                panelReports.setVisibility(View.VISIBLE);
                toolbarTitle.setText("Analytical Reports");
                generateBusinessReports();
                break;
            case 8:
                btnNavSettings.setBackgroundColor(0x30FFFFFF);
                panelSettings.setVisibility(View.VISIBLE);
                toolbarTitle.setText("POS Configurations");
                loadSettingsToInputs();
                break;
        }
    }

    private void refreshAllData() {
        currencySymbol = dbHelper.getSetting("currency");
        toolbarShopName.setText(dbHelper.getSetting("shop_name"));
    }

    // --- DASHBOARD MODULE COMPONENT ---
    private void refreshDashboardMetrics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Today's sales absolute total
        double salesTotal = 0;
        Cursor cSales = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date LIKE ?", new String[]{todayString + "%"});
        if (cSales.moveToFirst()) {
            salesTotal = cSales.getDouble(0);
        }
        cSales.close();
        txtDashSales.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", salesTotal));

        // 2. Profit estimate computation (Selling price sum - purchase cost price sum for items sold today)
        double totalCostForSoldItems = 0;
        Cursor cProfit = db.rawQuery("SELECT s.quantity, p.purchase_price FROM sale_items s INNER JOIN products p ON s.product_id = p.id INNER JOIN sales sl ON s.sale_id = sl.id WHERE sl.date LIKE ?", new String[]{todayString + "%"});
        while (cProfit.moveToNext()) {
            double qty = cProfit.getDouble(0);
            double purchase = cProfit.getDouble(1);
            totalCostForSoldItems += (qty * purchase);
        }
        cProfit.close();

        // expenses logged today
        double todayExpenses = 0;
        Cursor cExp = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date LIKE ?", new String[]{todayString + "%"});
        if (cExp.moveToFirst()) {
            todayExpenses = cExp.getDouble(0);
        }
        cExp.close();

        double estimatedProfit = salesTotal - totalCostForSoldItems - todayExpenses;
        if (estimatedProfit < 0) {
            txtDashProfit.setText("-" + currencySymbol + String.format(Locale.getDefault(), "%.2f", Math.abs(estimatedProfit)));
            txtDashProfit.setTextColor(0xFFEF4444);
        } else {
            txtDashProfit.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", estimatedProfit));
            txtDashProfit.setTextColor(0xFF059669);
        }

        // 3. Current Stock Valuation Asset value
        double totalStockVal = 0;
        Cursor cStockVal = db.rawQuery("SELECT SUM(current_stock * selling_price) FROM products", null);
        if (cStockVal.moveToFirst()) {
            totalStockVal = cStockVal.getDouble(0);
        }
        cStockVal.close();
        txtDashStockValue.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", totalStockVal));

        // 4. Low stock counts
        int lowStockCount = 0;
        StringBuilder alertItems = new StringBuilder();
        Cursor cLowStock = db.rawQuery("SELECT name, current_stock, minimum_stock FROM products WHERE current_stock <= minimum_stock", null);
        while (cLowStock.moveToNext()) {
            lowStockCount++;
            if (lowStockCount <= 3) {
                alertItems.append(cLowStock.getString(0)).append(" (Qty: ").append(cLowStock.getInt(1)).append(" left)\n");
            }
        }
        cLowStock.close();
        txtDashLowStockCount.setText(String.valueOf(lowStockCount));

        if (lowStockCount > 0) {
            dashAlertBanner.setVisibility(View.VISIBLE);
            dashAlertText.setText(alertItems.toString() + (lowStockCount > 3 ? "and " + (lowStockCount - 3) + " other item(s) running out!" : ""));
        } else {
            dashAlertBanner.setVisibility(View.GONE);
        }

        // 5. Customer Outstanding debt
        double totalDebt = 0;
        Cursor cDebt = db.rawQuery("SELECT SUM(outstanding_amount) FROM customers", null);
        if (cDebt.moveToFirst()) {
            totalDebt = cDebt.getDouble(0);
        }
        cDebt.close();
        txtDashDueReceivables.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", totalDebt));

        // 6. Total customers count
        int customerCount = 0;
        Cursor cClients = db.rawQuery("SELECT COUNT(*) FROM customers", null);
        if (cClients.moveToFirst()) {
            customerCount = cClients.getInt(0);
        }
        cClients.close();
        txtDashCustomersCount.setText(String.valueOf(customerCount));
    }


    // --- POS & BILLING SYSTEM MODULE ---
    private void refreshPOSSpinners() {
        // Load active client selections
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Customers list spinner loading
        activeCustomerSpinnerList.clear();
        activeCustomerSpinnerIds.clear();

        // First option is standard non-debt general customer
        activeCustomerSpinnerList.add("Walk-In Retail Client (General)");
        activeCustomerSpinnerIds.add(0);

        Cursor cCust = db.rawQuery("SELECT id, name, mobile FROM customers ORDER BY name ASC", null);
        while (cCust.moveToNext()) {
            activeCustomerSpinnerIds.add(cCust.getInt(0));
            activeCustomerSpinnerList.add(cCust.getString(1) + " (" + cCust.getString(2) + ")");
        }
        cCust.close();

        ArrayAdapter<String> custAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activeCustomerSpinnerList);
        custAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBillCustomer.setAdapter(custAdapter);

        // Load active inventory listings for POS adding
        activeProductSpinnerList.clear();
        activeProductSpinnerIds.clear();

        Cursor cProd = db.rawQuery("SELECT id, name, selling_price FROM products WHERE current_stock > 0 ORDER BY name ASC", null);
        while (cProd.moveToNext()) {
            activeProductSpinnerIds.add(cProd.getInt(0));
            activeProductSpinnerList.add(cProd.getString(1) + " - " + currencySymbol + cProd.getDouble(2));
        }
        cProd.close();

        ArrayAdapter<String> prodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activeProductSpinnerList);
        prodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBillProducts.setAdapter(prodAdapter);
    }

    private void handleBarcodeOrSearch(String term) {
        if (term == null || term.trim().length() < 3) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM products WHERE barcode = ? OR sku = ? OR name LIKE ? LIMIT 1",
                new String[]{term.trim(), term.trim(), "%" + term.trim() + "%"});

        if (c.moveToFirst()) {
            int foundId = c.getInt(0);
            c.close();
            // Automatically add this matched item
            addIdToCartDirectly(foundId);
            edtBillSearch.setText("");
            Toast.makeText(this, "Item scanned/found successfully!", Toast.LENGTH_SHORT).show();
        } else {
            c.close();
        }
    }

    private void addProductToCartFromSpinner() {
        if (spnBillProducts.getSelectedItem() == null) {
            Toast.makeText(this, "No stock items available in store database.", Toast.LENGTH_SHORT).show();
            return;
        }
        int selIndex = spnBillProducts.getSelectedItemPosition();
        if (selIndex < 0 || selIndex >= activeProductSpinnerIds.size()) return;

        int prodId = activeProductSpinnerIds.get(selIndex);
        addIdToCartDirectly(prodId);
    }

    private void addIdToCartDirectly(int prodId) {
        // Check if item is already added in current cart session
        for (CartItem item : cartList) {
            if (item.productId == prodId) {
                if (item.quantity + 1 > item.stockLimit) {
                    Toast.makeText(this, "Cannot add more. Insufficient warehouse stock limit!", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.quantity += 1;
                renderCartRows();
                calculateInvoiceTotals();
                return;
            }
        }

        // Fresh addition, find specifications from SQLite database
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, selling_price, mrp, gst_percentage, current_stock, unit FROM products WHERE id = ?", new String[]{String.valueOf(prodId)});
        if (c.moveToFirst()) {
            CartItem freshItem = new CartItem();
            freshItem.productId = prodId;
            freshItem.productName = c.getString(0);
            freshItem.sellingPrice = c.getDouble(1);
            freshItem.mrp = c.getDouble(2);
            freshItem.gstPercentage = c.getDouble(3);
            freshItem.stockLimit = c.getDouble(4);
            freshItem.unit = c.getString(5);
            freshItem.quantity = 1;

            if (freshItem.stockLimit <= 0) {
                Toast.makeText(this, "Alert! Target product has absolute Zero warehouse stock left.", Toast.LENGTH_SHORT).show();
            } else {
                cartList.add(freshItem);
                renderCartRows();
                calculateInvoiceTotals();
            }
        }
        c.close();
    }

    private void renderCartRows() {
        layoutCartContainer.removeAllViews();

        if (cartList.isEmpty()) {
            txtEmptyCart.setVisibility(View.VISIBLE);
            return;
        }
        txtEmptyCart.setVisibility(View.GONE);

        for (int i = 0; i < cartList.size(); i++) {
            final int pos = i;
            final CartItem item = cartList.get(i);

            View row = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
            TextView text = (TextView) row.findViewById(android.R.id.text1);
            
            // Format output string representing cart line item nicely
            double lineTotal = item.sellingPrice * item.quantity;
            String label = item.productName + " (" + item.unit + ")\n" +
                    "Rate: " + currencySymbol + item.sellingPrice + "  |  Qty: " + item.quantity + "  |  Total: " + currencySymbol + String.format(Locale.getDefault(), "%.2f", lineTotal);
            text.setText(label);
            text.setTextSize(14sp);

            // Row click options (Plus, Minus, Delete dialog triggers)
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCartItemQuantityDialog(pos);
                }
            });

            layoutCartContainer.addView(row);
        }
    }

    private void showCartItemQuantityDialog(final int index) {
        final CartItem item = cartList.get(index);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.productName);
        builder.setMessage("Configure billing quantity limits below (Available Stock: " + item.stockLimit + "):");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(item.quantity));
        builder.setView(input);

        builder.setPositiveButton("Set Quantity", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    double val = Double.parseDouble(input.getText().toString().trim());
                    if (val <= 0) {
                        cartList.remove(index);
                    } else if (val > item.stockLimit) {
                        Toast.makeText(MainActivity.this, "Cannot override maximum warehouse stock limits!", Toast.LENGTH_SHORT).show();
                        item.quantity = item.stockLimit;
                    } else {
                        item.quantity = val;
                    }
                    renderCartRows();
                    calculateInvoiceTotals();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid quantity inputs.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Remove Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cartList.remove(index);
                renderCartRows();
                calculateInvoiceTotals();
            }
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void calculateInvoiceTotals() {
        double subtotal = 0;
        double taxAmount = 0;

        for (CartItem item : cartList) {
            double totalLine = item.sellingPrice * item.quantity;
            subtotal += totalLine;

            // Calculate tax component included
            double gstFactor = item.gstPercentage / (100 + item.gstPercentage);
            taxAmount += (totalLine * gstFactor);
        }

        double discount = 0;
        String discString = edtBillDiscount.getText().toString().trim();
        if (!discString.isEmpty()) {
            try { discount = Double.parseDouble(discString); } catch (Exception e) {}
        }

        double grandTotal = subtotal - discount;
        if (grandTotal < 0) grandTotal = 0;

        double amtPaid = grandTotal;
        String paidString = edtBillPaidAmount.getText().toString().trim();
        if (!paidString.isEmpty()) {
            try { amtPaid = Double.parseDouble(paidString); } catch (Exception e) {}
        }

        double dueAmt = grandTotal - amtPaid;
        if (dueAmt < 0) dueAmt = 0;

        txtBillSubtotal.setText("Subtotal: " + currencySymbol + String.format(Locale.getDefault(), "%.2f", subtotal));
        txtBillTax.setText("GST Included: " + currencySymbol + String.format(Locale.getDefault(), "%.2f", taxAmount));
        txtBillGrandTotal.setText("Total: " + currencySymbol + String.format(Locale.getDefault(), "%.2f", grandTotal));
        txtBillDueAmount.setText("Due: " + currencySymbol + String.format(Locale.getDefault(), "%.2f", dueAmt));
    }

    private void processAndSaveSaleBill() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Empty retail carts cannot yield invoice processing.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 1. Resolve custom metadata metrics
        double subtotal = 0;
        double taxAmount = 0;
        for (CartItem item : cartList) {
            double totalLine = item.sellingPrice * item.quantity;
            subtotal += totalLine;
            double gstFactor = item.gstPercentage / (100 + item.gstPercentage);
            taxAmount += (totalLine * gstFactor);
        }

        double discount = 0;
        String discString = edtBillDiscount.getText().toString().trim();
        if (!discString.isEmpty()) {
            try { discount = Double.parseDouble(discString); } catch (Exception e) {}
        }

        double grandTotal = subtotal - discount;
        if (grandTotal < 0) grandTotal = 0;

        double amtPaid = grandTotal;
        String paidString = edtBillPaidAmount.getText().toString().trim();
        if (!paidString.isEmpty()) {
            try { amtPaid = Double.parseDouble(paidString); } catch (Exception e) {}
        }

        double dueAmt = grandTotal - amtPaid;
        if (dueAmt < 0) dueAmt = 0;

        int customerSpinnerIndex = spnBillCustomer.getSelectedItemPosition();
        int customerId = activeCustomerSpinnerIds.get(customerSpinnerIndex);
        String payMode = spnBillPayMode.getSelectedItem().toString();

        // Auto force client payment type to due if debt option chosen
        if (payMode.equalsIgnoreCase("Credit / Udhaar") && dueAmt <= 0) {
            dueAmt = grandTotal;
            amtPaid = 0;
        }

        // Prevent credit sales to anonymous retail walkins
        if (customerId == 0 && dueAmt > 0) {
            Toast.makeText(this, "Warning: Credit Sales require registering a valid Client Identity!", Toast.LENGTH_LONG).show();
            return;
        }

        db.beginTransaction();
        try {
            // Fetch configuration prefixing parameters
            String prefix = dbHelper.getSetting("invoice_prefix");
            long unixTime = System.currentTimeMillis() / 1000L;
            String invoiceNum = prefix + unixTime;

            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Insert details into SQL sales table
            ContentValues salesVal = new ContentValues();
            salesVal.put("invoice_number", invoiceNum);
            salesVal.put("date", dateStr);
            salesVal.put("customer_id", customerId);
            salesVal.put("discount", discount);
            salesVal.put("tax_amount", taxAmount);
            salesVal.put("subtotal", subtotal);
            salesVal.put("grand_total", grandTotal);
            salesVal.put("payment_method", payMode);
            salesVal.put("paid_amount", amtPaid);
            salesVal.put("due_amount", dueAmt);

            long saleRowId = db.insert("sales", null, salesVal);

            // Deduct stock limits and add transaction log records
            for (CartItem item : cartList) {
                ContentValues lineVal = new ContentValues();
                lineVal.put("sale_id", saleRowId);
                lineVal.put("product_id", item.productId);
                lineVal.put("product_name", item.productName);
                lineVal.put("quantity", item.quantity);
                lineVal.put("price", item.sellingPrice);
                lineVal.put("gst_percentage", item.gstPercentage);
                lineVal.put("total", item.sellingPrice * item.quantity);

                db.insert("sale_items", null, lineVal);

                // Update catalog decrement metrics
                db.execSQL("UPDATE products SET current_stock = current_stock - ? WHERE id = ?", new Object[]{item.quantity, item.productId});
            }

            // If outstanding balance occurs, debit clients table values
            if (customerId > 0 && dueAmt > 0) {
                db.execSQL("UPDATE customers SET outstanding_amount = outstanding_amount + ? WHERE id = ?", new Object[]{dueAmt, customerId});
            }

            db.setTransactionSuccessful();

            // Clear active POS session
            cartList.clear();
            edtBillDiscount.setText("");
            edtBillPaidAmount.setText("");
            renderCartRows();
            calculateInvoiceTotals();

            Toast.makeText(this, "Invoice saved successfully!", Toast.LENGTH_SHORT).show();

            // Fire dialog presenting final thermal printable layout
            displayThermalReceiptDialog(invoiceNum, dateStr, customerSpinnerIndex, grandTotal, amtPaid, dueAmt, payMode, subtotal, taxAmount, discount, saleRowId);

        } catch (Exception e) {
            Toast.makeText(this, "Critical transaction failure writing invoices: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
    }

    private void displayThermalReceiptDialog(String invoiceNum, String dateStr, int customerSpinnerIndex, double grand, double paid, double due, String mode, double sub, double tax, double disc, long saleId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thermal Print Preview");

        // Generate printable plain-text layout
        StringBuilder slip = new StringBuilder();
        slip.append("      ").append(dbHelper.getSetting("shop_name").toUpperCase()).append("\n");
        slip.append("   ").append(dbHelper.getSetting("address")).append("\n");
        slip.append("   Phone: ").append(dbHelper.getSetting("mobile")).append("\n");
        slip.append("   GSTIN: ").append(dbHelper.getSetting("gst_number")).append("\n");
        slip.append("----------------------------------\n");
        slip.append("Inv Num: ").append(invoiceNum).append("\n");
        slip.append("Date: ").append(dateStr).append("\n");
        slip.append("Client: ").append(activeCustomerSpinnerList.get(customerSpinnerIndex)).append("\n");
        slip.append("----------------------------------\n");
        slip.append(String.format("%-18s %4s %8s\n", "Item Name", "Qty", "Total"));
        slip.append("----------------------------------\n");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT product_name, quantity, total FROM sale_items WHERE sale_id = ?", new String[]{String.valueOf(saleId)});
        while (c.moveToNext()) {
            String name = c.getString(0);
            if (name.length() > 16) name = name.substring(0, 15) + ".";
            slip.append(String.format("%-18s %4.1f %8s\n", name, c.getDouble(1), currencySymbol + String.format(Locale.getDefault(), "%.2f", c.getDouble(2))));
        }
        c.close();

        slip.append("----------------------------------\n");
        slip.append(String.format("Subtotal:                 %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", sub)));
        slip.append(String.format("GST Component (Inc):      %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", tax)));
        slip.append(String.format("Discount Allowed:         %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", disc)));
        slip.append("----------------------------------\n");
        slip.append(String.format("GRAND TOTAL:              %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", grand)));
        slip.append(String.format("Cash Paid:                %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", paid)));
        slip.append(String.format("Remaining Due:            %8s\n", currencySymbol + String.format(Locale.getDefault(), "%.2f", due)));
        slip.append("Payment Mode: ").append(mode).append("\n");
        slip.append("----------------------------------\n");
        slip.append("    Thank you for your patronage! \n");
        slip.append("       Powered by RetailPro App   \n");

        TextView printView = new TextView(this);
        printView.setText(slip.toString());
        printView.setTypeface(android.graphics.Typeface.MONOSPACE);
        printView.setTextSize(12sp);
        printView.setPadding(30, 30, 30, 30);
        printView.setBackgroundColor(0xFFFFFFFF);
        printView.setTextColor(0xFF000000);

        ScrollView scroller = new ScrollView(this);
        scroller.addView(printView);

        builder.setView(scroller);
        builder.setPositiveButton("Complete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchPanel(0); // Jump back to refresh Dashboard counters
            }
        });
        builder.show();
    }


    // --- INVENTORY / PRODUCTS COMPONENT ---
    private void loadProductsList(String search) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        if (search == null || search.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM products ORDER BY name ASC", null);
        } else {
            cursor = db.rawQuery("SELECT * FROM products WHERE name LIKE ? OR sku LIKE ? OR barcode LIKE ? OR category LIKE ? ORDER BY name ASC",
                    new String[]{"%" + search + "%", "%" + search + "%", "%" + search + "%", "%" + search + "%"});
        }

        if (cursor.getCount() == 0) {
            txtNoProducts.setVisibility(View.VISIBLE);
            lstProducts.setVisibility(View.GONE);
            cursor.close();
            return;
        }

        txtNoProducts.setVisibility(View.GONE);
        lstProducts.setVisibility(View.VISIBLE);

        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(0));
            map.put("name", cursor.getString(1));
            map.put("sku", cursor.getString(2));
            map.put("barcode", cursor.getString(3));
            map.put("category", cursor.getString(4));
            map.put("purchase_price", cursor.getString(5));
            map.put("selling_price", cursor.getString(6));
            map.put("mrp", cursor.getString(7));
            map.put("gst", cursor.getString(8));
            map.put("stock", cursor.getString(9));
            map.put("min_stock", cursor.getString(10));
            map.put("unit", cursor.getString(11));
            data.add(map);
        }
        cursor.close();

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return data.size(); }
            @Override public Object getItem(int position) { return data.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                final HashMap<String, String> item = data.get(position);

                double stockVal = Double.parseDouble(item.get("stock"));
                double minVal = Double.parseDouble(item.get("min_stock"));

                String indicator = (stockVal <= minVal) ? " ⚠️ [LOW STOCK]" : "";

                text1.setText(item.get("name") + " (" + item.get("unit") + ")" + indicator);
                if (stockVal <= minVal) {
                    text1.setTextColor(0xFFEF4444);
                } else {
                    text1.setTextColor(0xFF1F2937);
                }

                String subtitle = "Category: " + item.get("category") + " | Selling Rate: " + currencySymbol + item.get("selling_price") +
                        "\nStock: " + item.get("stock") + " | SKU: " + item.get("sku") + " | Barcode: " + item.get("barcode");
                text2.setText(subtitle);
                text2.setTextSize(12sp);

                return convertView;
            }
        };

        lstProducts.setAdapter(adapter);

        lstProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> target = data.get(position);
                openEditProductDialog(target);
            }
        });
    }

    private void openAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Product");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Product Name", container);
        final EditText edtSKU = createPopupInput("SKU (Shortcode Identifier)", container);
        final EditText edtBarcode = createPopupInput("Barcode / GTIN Number", container);
        final EditText edtCat = createPopupInput("Category Name", container);
        final EditText edtPurch = createPopupInput("Purchase Cost Rate (₹)", container);
        final EditText edtSell = createPopupInput("Retail Selling Rate (₹)", container);
        final EditText edtMrp = createPopupInput("Printed MRP (₹)", container);
        final EditText edtGst = createPopupInput("GST Included Percentage (e.g. 18)", container);
        final EditText edtStock = createPopupInput("Starting Initial Stock (Qty)", container);
        final EditText edtMinStock = createPopupInput("Minimum Alert Stock Level", container);
        final EditText edtUnit = createPopupInput("Measurement Unit (e.g. Pcs/KG/Pack)", container);

        // Prefills default values
        edtUnit.setText("Pcs");
        edtGst.setText("18");
        edtMinStock.setText("10");

        ScrollView scroller = new ScrollView(this);
        scroller.addView(container);
        builder.setView(scroller);

        builder.setPositiveButton("Insert", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String name = edtName.getText().toString().trim();
                    String sku = edtSKU.getText().toString().trim();
                    String bar = edtBarcode.getText().toString().trim();
                    String cat = edtCat.getText().toString().trim();
                    double purchase = Double.parseDouble(edtPurch.getText().toString().trim());
                    double sell = Double.parseDouble(edtSell.getText().toString().trim());
                    double mrp = Double.parseDouble(edtMrp.getText().toString().trim());
                    double gst = Double.parseDouble(edtGst.getText().toString().trim());
                    double stock = Double.parseDouble(edtStock.getText().toString().trim());
                    double minStock = Double.parseDouble(edtMinStock.getText().toString().trim());
                    String unit = edtUnit.getText().toString().trim();

                    if (name.isEmpty() || cat.isEmpty() || unit.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Mandatory specifications cannot remain empty!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues val = new ContentValues();
                    val.put("name", name);
                    val.put("sku", sku);
                    val.put("barcode", bar);
                    val.put("category", cat);
                    val.put("purchase_price", purchase);
                    val.put("selling_price", sell);
                    val.put("mrp", mrp);
                    val.put("gst_percentage", gst);
                    val.put("current_stock", stock);
                    val.put("minimum_stock", minStock);
                    val.put("unit", unit);

                    db.insert("products", null, val);
                    Toast.makeText(MainActivity.this, "Product registered into store listings!", Toast.LENGTH_SHORT).show();
                    loadProductsList("");

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Input verification failure: Check numeric fields.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openEditProductDialog(final HashMap<String, String> item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modify Product");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Product Name", container);
        final EditText edtSKU = createPopupInput("SKU", container);
        final EditText edtBarcode = createPopupInput("Barcode", container);
        final EditText edtCat = createPopupInput("Category", container);
        final EditText edtPurch = createPopupInput("Purchase Price (₹)", container);
        final EditText edtSell = createPopupInput("Selling Price (₹)", container);
        final EditText edtMrp = createPopupInput("MRP (₹)", container);
        final EditText edtGst = createPopupInput("GST %", container);
        final EditText edtStock = createPopupInput("Current Stock", container);
        final EditText edtMinStock = createPopupInput("Minimum Stock Alert Limit", container);
        final EditText edtUnit = createPopupInput("Unit", container);

        // Prepopulate text
        edtName.setText(item.get("name"));
        edtSKU.setText(item.get("sku"));
        edtBarcode.setText(item.get("barcode"));
        edtCat.setText(item.get("category"));
        edtPurch.setText(item.get("purchase_price"));
        edtSell.setText(item.get("selling_price"));
        edtMrp.setText(item.get("mrp"));
        edtGst.setText(item.get("gst"));
        edtStock.setText(item.get("stock"));
        edtMinStock.setText(item.get("min_stock"));
        edtUnit.setText(item.get("unit"));

        ScrollView scroller = new ScrollView(this);
        scroller.addView(container);
        builder.setView(scroller);

        builder.setPositiveButton("Save Updates", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues val = new ContentValues();
                    val.put("name", edtName.getText().toString().trim());
                    val.put("sku", edtSKU.getText().toString().trim());
                    val.put("barcode", edtBarcode.getText().toString().trim());
                    val.put("category", edtCat.getText().toString().trim());
                    val.put("purchase_price", Double.parseDouble(edtPurch.getText().toString().trim()));
                    val.put("selling_price", Double.parseDouble(edtSell.getText().toString().trim()));
                    val.put("mrp", Double.parseDouble(edtMrp.getText().toString().trim()));
                    val.put("gst_percentage", Double.parseDouble(edtGst.getText().toString().trim()));
                    val.put("current_stock", Double.parseDouble(edtStock.getText().toString().trim()));
                    val.put("minimum_stock", Double.parseDouble(edtMinStock.getText().toString().trim()));
                    val.put("unit", edtUnit.getText().toString().trim());

                    db.update("products", val, "id = ?", new String[]{item.get("id")});
                    Toast.makeText(MainActivity.this, "Product profile modified!", Toast.LENGTH_SHORT).show();
                    loadProductsList("");
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Input verification errors. Verify values.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNeutralButton("Delete Product", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Confirm Deletion");
                alert.setMessage("Are you absolutely sure you want to permanently erase this inventory record?");
                alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("products", "id = ?", new String[]{item.get("id")});
                        Toast.makeText(MainActivity.this, "Item wiped out.", Toast.LENGTH_SHORT).show();
                        loadProductsList("");
                    }
                });
                alert.setNegativeButton("Cancel", null);
                alert.show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    // --- CUSTOMER LEDGER AND MANAGEMENT MODULE ---
    private void loadCustomersList(String search) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        if (search == null || search.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM customers ORDER BY name ASC", null);
        } else {
            cursor = db.rawQuery("SELECT * FROM customers WHERE name LIKE ? OR mobile LIKE ? OR address LIKE ? ORDER BY name ASC",
                    new String[]{"%" + search + "%", "%" + search + "%", "%" + search + "%"});
        }

        if (cursor.getCount() == 0) {
            txtNoCustomers.setVisibility(View.VISIBLE);
            lstCustomers.setVisibility(View.GONE);
            cursor.close();
            return;
        }

        txtNoCustomers.setVisibility(View.GONE);
        lstCustomers.setVisibility(View.VISIBLE);

        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(0));
            map.put("name", cursor.getString(1));
            map.put("mobile", cursor.getString(2));
            map.put("address", cursor.getString(3));
            map.put("outstanding", cursor.getString(4));
            data.add(map);
        }
        cursor.close();

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return data.size(); }
            @Override public Object getItem(int position) { return data.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                final HashMap<String, String> map = data.get(position);

                double dueVal = Double.parseDouble(map.get("outstanding"));
                String alertText = (dueVal > 0) ? " [Udhaar: " + currencySymbol + String.format(Locale.getDefault(), "%.1f", dueVal) + "]" : " [Clear]";

                text1.setText(map.get("name") + alertText);
                if (dueVal > 0) {
                    text1.setTextColor(0xFFEF4444);
                } else {
                    text1.setTextColor(0xFF059669);
                }

                text2.setText("Mobile: " + map.get("mobile") + " | Address: " + map.get("address"));
                text2.setTextSize(12sp);

                return convertView;
            }
        };

        lstCustomers.setAdapter(adapter);

        lstCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> target = data.get(position);
                openEditCustomerDialog(target);
            }
        });
    }

    private void openAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register Client");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Customer Full Name", container);
        final EditText edtMobile = createPopupInput("Mobile Number", container);
        final EditText edtAddress = createPopupInput("Local Address Details", container);
        final EditText edtOut = createPopupInput("Starting Outstanding Credit Ledger (₹)", container);

        edtOut.setText("0");

        builder.setView(container);

        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edtName.getText().toString().trim();
                String mob = edtMobile.getText().toString().trim();
                String add = edtAddress.getText().toString().trim();
                double out = 0;
                try { out = Double.parseDouble(edtOut.getText().toString().trim()); } catch(Exception e){}

                if (name.isEmpty() || mob.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Client name and mobile cannot reside blank!", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put("name", name);
                val.put("mobile", mob);
                val.put("address", add);
                val.put("outstanding_amount", out);

                db.insert("customers", null, val);
                Toast.makeText(MainActivity.this, "Client recorded!", Toast.LENGTH_SHORT).show();
                loadCustomersList("");
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openEditCustomerDialog(final HashMap<String, String> customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Ledger & Profile");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Full Name", container);
        final EditText edtMobile = createPopupInput("Mobile Phone", container);
        final EditText edtAddress = createPopupInput("Address", container);
        final EditText edtOut = createPopupInput("Outstanding Ledger Debt (₹)", container);

        edtName.setText(customer.get("name"));
        edtMobile.setText(customer.get("mobile"));
        edtAddress.setText(customer.get("address"));
        edtOut.setText(customer.get("outstanding"));

        builder.setView(container);

        builder.setPositiveButton("Apply Changes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put("name", edtName.getText().toString().trim());
                val.put("mobile", edtMobile.getText().toString().trim());
                val.put("address", edtAddress.getText().toString().trim());
                val.put("outstanding_amount", Double.parseDouble(edtOut.getText().toString().trim()));

                db.update("customers", val, "id = ?", new String[]{customer.get("id")});
                Toast.makeText(MainActivity.this, "Client profile updated!", Toast.LENGTH_SHORT).show();
                loadCustomersList("");
            }
        });

        builder.setNeutralButton("Settle / Pay Debt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openClearDebtDialog(customer);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openClearDebtDialog(final HashMap<String, String> customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settle Udhaar");
        builder.setMessage("Client Name: " + customer.get("name") + "\nCurrent Debt Balance: " + currencySymbol + customer.get("outstanding") + "\n\nEnter amount paid back below:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount Received");
        builder.setView(input);

        builder.setPositiveButton("Receive Payment", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    double pay = Double.parseDouble(input.getText().toString().trim());
                    double currentDebt = Double.parseDouble(customer.get("outstanding"));
                    double balance = currentDebt - pay;
                    if (balance < 0) balance = 0;

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("UPDATE customers SET outstanding_amount = ? WHERE id = ?", new Object[]{balance, customer.get("id")});

                    // Log this settlement as a general negative expense or sale if needed, or simply update ledger
                    Toast.makeText(MainActivity.this, "Ledger debt cleared by " + currencySymbol + pay + "!", Toast.LENGTH_SHORT).show();
                    loadCustomersList("");
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid entry data.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    // --- SUPPLIERS & RESTOCK PURCHASES COMPONENT ---
    private void refreshPurchasesSpinners() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Suppliers list Loading
        activeSupplierSpinnerList.clear();
        activeSupplierSpinnerIds.clear();

        Cursor cSup = db.rawQuery("SELECT id, name FROM suppliers ORDER BY name ASC", null);
        while (cSup.moveToNext()) {
            activeSupplierSpinnerIds.add(cSup.getInt(0));
            activeSupplierSpinnerList.add(cSup.getString(1));
        }
        cSup.close();

        ArrayAdapter<String> adapterSup = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activeSupplierSpinnerList);
        adapterSup.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPurchaseSupplier.setAdapter(adapterSup);

        // Product selection spinner Loading
        activeProductSpinnerList.clear();
        activeProductSpinnerIds.clear();

        Cursor cProd = db.rawQuery("SELECT id, name, purchase_price FROM products ORDER BY name ASC", null);
        while (cProd.moveToNext()) {
            activeProductSpinnerIds.add(cProd.getInt(0));
            activeProductSpinnerList.add(cProd.getString(1) + " (CP: " + currencySymbol + cProd.getDouble(2) + ")");
        }
        cProd.close();

        ArrayAdapter<String> adapterProd = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activeProductSpinnerList);
        adapterProd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPurchaseProduct.setAdapter(adapterProd);
    }

    private void loadSuppliersDirectory() {
        layoutSuppliersList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM suppliers ORDER BY name ASC", null);
        if (c.getCount() == 0) {
            TextView txt = new TextView(this);
            txt.setText("No suppliers registered yet in directory.");
            txt.setGravity(android.view.Gravity.CENTER);
            txt.setTextColor(0xFF6B7280);
            txt.setPadding(10, 20, 10, 20);
            layoutSuppliersList.addView(txt);
            c.close();
            return;
        }

        while (c.moveToNext()) {
            final String id = c.getString(0);
            final String name = c.getString(1);
            final String mob = c.getString(2);
            final String add = c.getString(3);
            final String out = c.getString(4);

            View row = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
            TextView text1 = (TextView) row.findViewById(android.R.id.text1);
            TextView text2 = (TextView) row.findViewById(android.R.id.text2);

            double debtVal = Double.parseDouble(out);
            String labelDebt = (debtVal > 0) ? " (Owes: " + currencySymbol + String.format(Locale.getDefault(), "%.1f", debtVal) + ")" : "";

            text1.setText(name + labelDebt);
            if (debtVal > 0) {
                text1.setTextColor(0xFFEF4444);
            } else {
                text1.setTextColor(0xFF1F2937);
            }

            text2.setText("Phone: " + mob + " | Address: " + add);
            text2.setTextSize(12sp);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openEditSupplierDialog(id, name, mob, add, out);
                }
            });

            layoutSuppliersList.addView(row);
        }
        c.close();
    }

    private void openAddSupplierDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Supplier");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Supplier / Vendor Company Name", container);
        final EditText edtMobile = createPopupInput("Mobile Phone", container);
        final EditText edtAddress = createPopupInput("Warehouse Office Address", container);
        final EditText edtOut = createPopupInput("Ledger Outstanding Payables (₹)", container);

        edtOut.setText("0");

        builder.setView(container);

        builder.setPositiveButton("Add Vendor", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edtName.getText().toString().trim();
                String mob = edtMobile.getText().toString().trim();
                String add = edtAddress.getText().toString().trim();
                double out = 0;
                try { out = Double.parseDouble(edtOut.getText().toString().trim()); } catch(Exception e){}

                if (name.isEmpty() || mob.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fields cannot remain empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put("name", name);
                val.put("mobile", mob);
                val.put("address", add);
                val.put("outstanding_amount", out);

                db.insert("suppliers", null, val);
                Toast.makeText(MainActivity.this, "Supplier Vendor Registered!", Toast.LENGTH_SHORT).show();
                refreshPurchasesSpinners();
                loadSuppliersDirectory();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openEditSupplierDialog(final String id, String name, String mob, String add, String out) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modify Supplier");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        final EditText edtName = createPopupInput("Company Name", container);
        final EditText edtMobile = createPopupInput("Mobile Phone", container);
        final EditText edtAddress = createPopupInput("Address", container);
        final EditText edtOut = createPopupInput("Payables Debt (₹)", container);

        edtName.setText(name);
        edtMobile.setText(mob);
        edtAddress.setText(add);
        edtOut.setText(out);

        builder.setView(container);

        builder.setPositiveButton("Modify Details", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put("name", edtName.getText().toString().trim());
                val.put("mobile", edtMobile.getText().toString().trim());
                val.put("address", edtAddress.getText().toString().trim());
                val.put("outstanding_amount", Double.parseDouble(edtOut.getText().toString().trim()));

                db.update("suppliers", val, "id = ?", new String[]{id});
                Toast.makeText(MainActivity.this, "Supplier details modified!", Toast.LENGTH_SHORT).show();
                refreshPurchasesSpinners();
                loadSuppliersDirectory();
            }
        });

        builder.setNeutralButton("Record Debt Settlement Payment", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openClearSupplierDebtDialog(id, edtName.getText().toString().trim(), edtOut.getText().toString().trim());
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openClearSupplierDebtDialog(final String id, String name, final String currentOut) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settle Supplier Account");
        builder.setMessage("Supplier: " + name + "\nPending Payables Debt: " + currencySymbol + currentOut + "\n\nEnter payment amount paid to vendor:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Settlement Amount");
        builder.setView(input);

        builder.setPositiveButton("Pay Out", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    double pay = Double.parseDouble(input.getText().toString().trim());
                    double currentDebt = Double.parseDouble(currentOut);
                    double balance = currentDebt - pay;
                    if (balance < 0) balance = 0;

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("UPDATE suppliers SET outstanding_amount = ? WHERE id = ?", new Object[]{balance, id});

                    // Log as custom store operational cost expense
                    String todayString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    ContentValues expVal = new ContentValues();
                    expVal.put("title", "Vendor Settlement payment (" + id + ")");
                    expVal.put("amount", pay);
                    expVal.put("category", "Logistics");
                    expVal.put("note", "Paid back due credit to supplier ID: " + id);
                    expVal.put("date", todayString);
                    db.insert("expenses", null, expVal);

                    Toast.makeText(MainActivity.this, "Vendor account settled by paying " + currencySymbol + pay + "!", Toast.LENGTH_SHORT).show();
                    refreshPurchasesSpinners();
                    loadSuppliersDirectory();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error settling vendor ledger.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void recordSupplierPurchase() {
        if (spnPurchaseSupplier.getSelectedItem() == null || spnPurchaseProduct.getSelectedItem() == null) {
            Toast.makeText(this, "Verify registered suppliers and product options are populated.", Toast.LENGTH_SHORT).show();
            return;
        }

        int supIndex = spnPurchaseSupplier.getSelectedItemPosition();
        int prodIndex = spnPurchaseProduct.getSelectedItemPosition();

        if (supIndex < 0 || prodIndex < 0) return;

        int supplierId = activeSupplierSpinnerIds.get(supIndex);
        int productId = activeProductSpinnerIds.get(prodIndex);

        String costStr = edtPurchaseCost.getText().toString().trim();
        String qtyStr = edtPurchaseQty.getText().toString().trim();

        if (costStr.isEmpty() || qtyStr.isEmpty()) {
            Toast.makeText(this, "Cost rate & quantity inputs cannot reside empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double cost = Double.parseDouble(costStr);
            double qty = Double.parseDouble(qtyStr);
            double total = cost * qty;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            db.beginTransaction();
            try {
                // 1. Insert into SQL purchases
                ContentValues purVal = new ContentValues();
                purVal.put("supplier_id", supplierId);
                purVal.put("date", dateStr);
                purVal.put("grand_total", total);
                purVal.put("payment_method", "Cash");
                purVal.put("paid_amount", total);
                purVal.put("due_amount", 0);

                long purRowId = db.insert("purchases", null, purVal);

                // 2. Insert into purchase items table
                ContentValues lineVal = new ContentValues();
                lineVal.put("purchase_id", purRowId);
                lineVal.put("product_id", productId);
                lineVal.put("product_name", activeProductSpinnerList.get(prodIndex).split(" \\(CP")[0]);
                lineVal.put("quantity", qty);
                lineVal.put("price", cost);
                lineVal.put("gst_percentage", 0);
                lineVal.put("total", total);

                db.insert("purchase_items", null, lineVal);

                // 3. Increment stock limits on catalog items
                db.execSQL("UPDATE products SET current_stock = current_stock + ?, purchase_price = ? WHERE id = ?", new Object[]{qty, cost, productId});

                db.setTransactionSuccessful();

                edtPurchaseCost.setText("");
                edtPurchaseQty.setText("");
                Toast.makeText(this, "Purchased stock registered and warehouse levels loaded!", Toast.LENGTH_SHORT).show();

                refreshPurchasesSpinners();
                loadSuppliersDirectory();

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Purchase tracking failure: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    // --- EXPENSES LOG MODULE ---
    private void loadExpensesList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM expenses ORDER BY id DESC", null);

        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(0));
            map.put("title", cursor.getString(1));
            map.put("amount", cursor.getString(2));
            map.put("category", cursor.getString(3));
            map.put("note", cursor.getString(4));
            map.put("date", cursor.getString(5));
            data.add(map);
        }
        cursor.close();

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return data.size(); }
            @Override public Object getItem(int position) { return data.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                HashMap<String, String> item = data.get(position);

                text1.setText(item.get("title") + " - " + currencySymbol + item.get("amount"));
                text1.setTextColor(0xFFEF4444);

                text2.setText("Category: " + item.get("category") + " | Date: " + item.get("date") + " | Note: " + item.get("note"));
                text2.setTextSize(11sp);

                return convertView;
            }
        };

        lstExpenses.setAdapter(adapter);

        lstExpenses.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Delete Expense Ledger");
                alert.setMessage("Confirm wiping out this logged expense record permanently from accounts?");
                alert.setPositiveButton("Wipe", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("expenses", "id = ?", new String[]{data.get(position).get("id")});
                        Toast.makeText(MainActivity.this, "Expense ledger deleted.", Toast.LENGTH_SHORT).show();
                        loadExpensesList();
                    }
                });
                alert.setNegativeButton("Cancel", null);
                alert.show();
                return true;
            }
        });
    }

    private void recordStoreExpense() {
        String label = edtExpenseTitle.getText().toString().trim();
        String amtStr = edtExpenseAmount.getText().toString().trim();
        String cat = spnExpenseCategory.getSelectedItem().toString();

        if (label.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Verify expense title label and amounts are filled.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amt = Double.parseDouble(amtStr);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            ContentValues val = new ContentValues();
            val.put("title", label);
            val.put("amount", amt);
            val.put("category", cat);
            val.put("note", "Manual entry expense log");
            val.put("date", dateStr);

            db.insert("expenses", null, val);

            edtExpenseTitle.setText("");
            edtExpenseAmount.setText("");
            Toast.makeText(this, "Expense registered into ledger accounts!", Toast.LENGTH_SHORT).show();
            loadExpensesList();

        } catch (Exception e) {
            Toast.makeText(this, "Incorrect operational cost entries.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- SALES HISTORY LOG MODULE ---
    private void loadSalesHistory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Join customer profile details
        Cursor cursor = db.rawQuery("SELECT s.id, s.invoice_number, s.date, s.grand_total, s.payment_method, s.due_amount, c.name FROM sales s LEFT JOIN customers c ON s.customer_id = c.id ORDER BY s.id DESC", null);

        if (cursor.getCount() == 0) {
            txtNoHistory.setVisibility(View.VISIBLE);
            lstHistory.setVisibility(View.GONE);
            cursor.close();
            return;
        }

        txtNoHistory.setVisibility(View.GONE);
        lstHistory.setVisibility(View.VISIBLE);

        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(0));
            map.put("invoice", cursor.getString(1));
            map.put("date", cursor.getString(2));
            map.put("grand_total", cursor.getString(3));
            map.put("pay_mode", cursor.getString(4));
            map.put("due", cursor.getString(5));
            map.put("customer_name", cursor.getString(6) == null ? "Walk-In Client" : cursor.getString(6));
            data.add(map);
        }
        cursor.close();

        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return data.size(); }
            @Override public Object getItem(int position) { return data.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                HashMap<String, String> item = data.get(position);

                double dueVal = Double.parseDouble(item.get("due"));
                String indicator = (dueVal > 0) ? " [UNPAID DUE: " + currencySymbol + dueVal + "]" : " [PAID]";

                text1.setText("Invoice " + item.get("invoice") + " - " + currencySymbol + item.get("grand_total") + indicator);
                if (dueVal > 0) {
                    text1.setTextColor(0xFFEF4444);
                } else {
                    text1.setTextColor(0xFF059669);
                }

                text2.setText("Client: " + item.get("customer_name") + " | Mode: " + item.get("pay_mode") + " | Date: " + item.get("date"));
                text2.setTextSize(11sp);

                return convertView;
            }
        };

        lstHistory.setAdapter(adapter);

        lstHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = data.get(position);
                viewPreviousSaleDetailedDialog(item.get("id"), item.get("invoice"), item.get("date"), item.get("customer_name"), item.get("grand_total"), item.get("due"), item.get("pay_mode"));
            }
        });
    }

    private void viewPreviousSaleDetailedDialog(String saleId, String invoice, String date, String client, String grand, String due, String mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invoice details - " + invoice);

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Details:\n");
        sb.append("-----------------------------\n");
        sb.append("Date: ").append(date).append("\n");
        sb.append("Customer: ").append(client).append("\n");
        sb.append("Payment Mode: ").append(mode).append("\n");
        sb.append("Total Invoice Amt: ").append(currencySymbol).append(grand).append("\n");
        sb.append("Unsettled Credit: ").append(currencySymbol).append(due).append("\n\n");
        sb.append("Purchased Items Table:\n");
        sb.append("-----------------------------\n");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT product_name, quantity, price, total FROM sale_items WHERE sale_id = ?", new String[]{saleId});
        while (c.moveToNext()) {
            sb.append(c.getString(0))
                    .append(" | Qty: ").append(c.getDouble(1))
                    .append(" @ ").append(currencySymbol).append(c.getDouble(2))
                    .append(" = ").append(currencySymbol).append(c.getDouble(3)).append("\n");
        }
        c.close();

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Close View", null);
        builder.setNeutralButton("Erase Invoice", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Return stock levels before erasing
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    Cursor cItems = db.rawQuery("SELECT product_id, quantity FROM sale_items WHERE sale_id = ?", new String[]{saleId});
                    while (cItems.moveToNext()) {
                        db.execSQL("UPDATE products SET current_stock = current_stock + ? WHERE id = ?", new Object[]{cItems.getDouble(1), cItems.getInt(0)});
                    }
                    cItems.close();

                    db.delete("sales", "id = ?", new String[]{saleId});
                    db.delete("sale_items", "sale_id = ?", new String[]{saleId});

                    db.setTransactionSuccessful();
                    Toast.makeText(MainActivity.this, "Invoice erased, stock returned!", Toast.LENGTH_SHORT).show();
                    loadSalesHistory();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to erase invoice.", Toast.LENGTH_SHORT).show();
                } finally {
                    db.endTransaction();
                }
            }
        });

        builder.show();
    }


    // --- ANALYTICAL BUSINESS REPORTS MODULE ---
    private void generateBusinessReports() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Sales Gross values
        double totalSalesVal = 0;
        Cursor cS = db.rawQuery("SELECT SUM(grand_total) FROM sales", null);
        if (cS.moveToFirst()) {
            totalSalesVal = cS.getDouble(0);
        }
        cS.close();
        txtRepTotalSales.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", totalSalesVal));

        // 2. Purchases Logged Costs
        double totalPurCost = 0;
        Cursor cP = db.rawQuery("SELECT SUM(grand_total) FROM purchases", null);
        if (cP.moveToFirst()) {
            totalPurCost = cP.getDouble(0);
        }
        cP.close();
        txtRepTotalPurchases.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", totalPurCost));

        // 3. Gross Expenses Store operational values
        double totalExpVal = 0;
        Cursor cE = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
        if (cE.moveToFirst()) {
            totalExpVal = cE.getDouble(0);
        }
        cE.close();
        txtRepTotalExpenses.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", totalExpVal));

        // 4. Net overall business earnings projection (Total Cash received Sales - Total Expense limits)
        double earnings = totalSalesVal - totalExpVal;
        if (earnings < 0) {
            txtRepNetProfit.setText("-" + currencySymbol + String.format(Locale.getDefault(), "%.2f", Math.abs(earnings)));
            txtRepNetProfit.setTextColor(0xFFEF4444);
        } else {
            txtRepNetProfit.setText(currencySymbol + String.format(Locale.getDefault(), "%.2f", earnings));
            txtRepNetProfit.setTextColor(0xFF059669);
        }

        // 5. Stock Category Distributions summaries
        StringBuilder summary = new StringBuilder();
        summary.append("Catalog Stock Valuation Indicators:\n\n");

        Cursor cCat = db.rawQuery("SELECT category, COUNT(*), SUM(current_stock) FROM products GROUP BY category", null);
        while (cCat.moveToNext()) {
            summary.append("📁 ").append(cCat.getString(0)).append(":\n")
                    .append("   • Unique Items: ").append(cCat.getInt(1)).append("\n")
                    .append("   • Stock Units: ").append(cCat.getDouble(2)).append("\n\n");
        }
        cCat.close();

        txtRepStockMeta.setText(summary.toString());
    }


    // --- SETTINGS CONFIGURATIONS COMPONENT ---
    private void loadSettingsToInputs() {
        edtSetShopName.setText(dbHelper.getSetting("shop_name"));
        edtSetOwnerName.setText(dbHelper.getSetting("owner_name"));
        edtSetMobile.setText(dbHelper.getSetting("mobile"));
        edtSetAddress.setText(dbHelper.getSetting("address"));
        edtSetGst.setText(dbHelper.getSetting("gst_number"));
        edtSetInvPrefix.setText(dbHelper.getSetting("invoice_prefix"));
        edtSetCurrency.setText(dbHelper.getSetting("currency"));
    }

    private void saveSystemConfig() {
        String name = edtSetShopName.getText().toString().trim();
        String owner = edtSetOwnerName.getText().toString().trim();
        String mob = edtSetMobile.getText().toString().trim();
        String addr = edtSetAddress.getText().toString().trim();
        String gst = edtSetGst.getText().toString().trim();
        String pref = edtSetInvPrefix.getText().toString().trim();
        String curr = edtSetCurrency.getText().toString().trim();

        if (name.isEmpty() || owner.isEmpty() || mob.isEmpty()) {
            Toast.makeText(this, "Core Shop parameters cannot stay empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.setSetting("shop_name", name);
        dbHelper.setSetting("owner_name", owner);
        dbHelper.setSetting("mobile", mob);
        dbHelper.setSetting("address", addr);
        dbHelper.setSetting("gst_number", gst);
        dbHelper.setSetting("invoice_prefix", pref);
        dbHelper.setSetting("currency", curr);

        refreshAllData();
        Toast.makeText(this, "Configurations modified saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void confirmAndResetDatabase() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Wipe & Seed Demo Database?");
        builder.setMessage("Warning! This process wipes all transactions, products catalog, client lists, and overwrites settings with demo values. Continue?");
        builder.setPositiveButton("Reset Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                dbHelper.onUpgrade(db, 1, 1);
                refreshAllData();
                loadSettingsToInputs();
                Toast.makeText(MainActivity.this, "Store demo database seeded successfully!", Toast.LENGTH_SHORT).show();
                switchPanel(0); // Go back to dashboard
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    // --- DYNAMIC DIALOG COMPONENT BUILDERS ---
    private EditText createPopupInput(String label, LinearLayout container) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12sp);
        tv.setTextColor(0xFF4B5563);
        tv.setPadding(0, 10, 0, 4);
        container.addView(tv);

        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.edit_text_bg);
        et.setTextSize(14sp);
        et.setPadding(20, 16, 20, 16);
        et.setTextColor(0xFF1F2937);

        // Adjust soft keyboard modes dynamically based on standard fields
        if (label.contains("₹") || label.contains("Percentage") || label.contains("Stock") || label.contains("Level")) {
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else if (label.contains("Phone") || label.contains("Mobile")) {
            et.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else {
            et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        }

        container.addView(et);
        return et;
    }
}