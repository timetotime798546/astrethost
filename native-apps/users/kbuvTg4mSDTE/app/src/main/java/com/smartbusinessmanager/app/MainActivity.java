package com.smartbusinessmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener {

    // Helper references
    private DatabaseHelper dbHelper;

    // App Preferences cache loaded from DB settings
    private String confBusinessName;
    private String confOwnerName;
    private String confPhone;
    private String confCurrency;
    private double confTaxRate;
    private String confInvoicePrefix;
    private boolean confDarkTheme;

    // View components
    private View vDashboard, vProducts, vCustomers, vSales, vExpenses;
    private LinearLayout tabDashboard, tabProducts, tabCustomers, tabSales, tabExpenses;
    private TextView tvTabDashboard, tvTabProducts, tvTabCustomers, tvTabSales, tvTabExpenses;
    private EditText etGlobalSearch;
    private Button btnClearSearch;
    private TextView tvAppTitle;

    // Dashboard metrics
    private TextView tvDashSales, tvDashExpenses, tvDashProfit, tvDashReceivables;
    private TextView tvDashTotalProducts, tvDashLowStock, tvDashAlerts;
    private LinearLayout layoutRecentTransactions;

    // Product module
    private Spinner spinnerCategoryFilter;
    private LinearLayout layoutProductsList;

    // Customer module
    private LinearLayout layoutCustomersList;

    // Sales checkout helper variables
    private View scrollSalesNew, scrollSalesHistory;
    private Button btnSalesTabNew, btnSalesTabHistory;
    private Spinner spinnerSalesCustomer, spinnerSalesProducts;
    private TextView tvSalesQty;
    private LinearLayout layoutSalesCart;
    private TextView tvSalesSubtotal, tvSalesGrandTotal, tvSalesRemainingBalance;
    private EditText etSalesDiscount, etSalesTaxRate, etSalesAmountReceived;
    private LinearLayout layoutSalesHistoryList;
    private int currentQtyCounter = 1;

    // Expense module
    private Spinner spinnerExpenseCategory;
    private EditText etExpenseAmount, etExpenseDesc;
    private LinearLayout layoutExpensesList;
    private CustomChartView customPerformanceChart;
    private TextView tvReportDaily, tvReportWeekly, tvReportMonthly, tvReportBestSeller;

    // Cart memory model
    private static class CartItem {
        int id;
        String name;
        double sellingPrice;
        int qty;
    }
    private ArrayList<CartItem> activeCart = new ArrayList<>();

    // State Variables
    private int activeTab = 0; // 0=Dash, 1=Prod, 2=Cust, 3=Sales, 4=Exp
    private String globalQuery = "";

    // Lists of categories
    private final String[] preCategories = {"General", "Food & Beverage", "Electronics", "Clothing", "Office Supplies", "Services", "Others"};
    private final String[] expenseCategories = {"Inventory Purchase", "Rent & Utilities", "Logistics & Transport", "Marketing & Advertising", "Salaries & Wages", "Tax Payment", "Maintenance", "Office Overhead", "Other Expenses"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        loadAppSettings();

        initUiControls();
        refreshActiveScreen();
        applyCurrentPalette();
    }

    private void loadAppSettings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT key, value FROM settings", null);
        if (c != null) {
            while (c.moveToNext()) {
                String k = c.getString(0);
                String v = c.getString(1);
                if ("business_name".equals(k)) confBusinessName = v;
                else if ("owner_name".equals(k)) confOwnerName = v;
                else if ("phone".equals(k)) confPhone = v;
                else if ("currency".equals(k)) confCurrency = v;
                else if ("tax_rate".equals(k)) confTaxRate = Double.parseDouble(v);
                else if ("invoice_prefix".equals(k)) confInvoicePrefix = v;
                else if ("dark_theme".equals(k)) confDarkTheme = "1".equals(v);
            }
            c.close();
        }
    }

    private void initUiControls() {
        // App top controls
        tvAppTitle = (TextView) findViewById(R.id.tv_app_title);
        findViewById(R.id.btn_notifications).setOnClickListener(this);
        findViewById(R.id.btn_settings_top).setOnClickListener(this);

        // Core Screens
        vDashboard = findViewById(R.id.view_dashboard);
        vProducts = findViewById(R.id.view_products);
        vCustomers = findViewById(R.id.view_customers);
        vSales = findViewById(R.id.view_sales);
        vExpenses = findViewById(R.id.view_expenses_reports);

        // Tabs
        tabDashboard = (LinearLayout) findViewById(R.id.tab_dashboard);
        tabProducts = (LinearLayout) findViewById(R.id.tab_products);
        tabCustomers = (LinearLayout) findViewById(R.id.tab_customers);
        tabSales = (LinearLayout) findViewById(R.id.tab_sales);
        tabExpenses = (LinearLayout) findViewById(R.id.tab_expenses);

        tvTabDashboard = (TextView) findViewById(R.id.tv_tab_dashboard);
        tvTabProducts = (TextView) findViewById(R.id.tv_tab_products);
        tvTabCustomers = (TextView) findViewById(R.id.tv_tab_customers);
        tvTabSales = (TextView) findViewById(R.id.tv_tab_sales);
        tvTabExpenses = (TextView) findViewById(R.id.tv_tab_expenses);

        tabDashboard.setOnClickListener(this);
        tabProducts.setOnClickListener(this);
        tabCustomers.setOnClickListener(this);
        tabSales.setOnClickListener(this);
        tabExpenses.setOnClickListener(this);

        // Search engine triggers
        etGlobalSearch = (EditText) findViewById(R.id.et_global_search);
        btnClearSearch = (Button) findViewById(R.id.btn_clear_search);
        btnClearSearch.setOnClickListener(this);
        etGlobalSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                globalQuery = s.toString().trim();
                refreshActiveScreen();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 1. DASHBOARD BINDINGS
        tvDashSales = (TextView) findViewById(R.id.tv_dash_sales);
        tvDashExpenses = (TextView) findViewById(R.id.tv_dash_expenses);
        tvDashProfit = (TextView) findViewById(R.id.tv_dash_profit);
        tvDashReceivables = (TextView) findViewById(R.id.tv_dash_receivables);
        tvDashTotalProducts = (TextView) findViewById(R.id.tv_dash_total_products);
        tvDashLowStock = (TextView) findViewById(R.id.tv_dash_low_stock);
        tvDashAlerts = (TextView) findViewById(R.id.tv_dashboard_alerts);
        layoutRecentTransactions = (LinearLayout) findViewById(R.id.layout_recent_transactions);

        findViewById(R.id.btn_quick_new_sale).setOnClickListener(this);
        findViewById(R.id.btn_quick_add_product).setOnClickListener(this);
        findViewById(R.id.btn_quick_add_customer).setOnClickListener(this);
        findViewById(R.id.btn_quick_add_expense).setOnClickListener(this);

        // 2. PRODUCT LIST BINDINGS
        findViewById(R.id.btn_add_product_main).setOnClickListener(this);
        spinnerCategoryFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        layoutProductsList = (LinearLayout) findViewById(R.id.layout_products_list);

        ArrayList<String> prodCategoriesList = new ArrayList<>();
        prodCategoriesList.add("All Categories");
        for (String cat : preCategories) {
            prodCategoriesList.add(cat);
        }
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, prodCategoriesList);
        spinnerCategoryFilter.setAdapter(filterAdapter);
        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshProductsList();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 3. CUSTOMER LIST BINDINGS
        findViewById(R.id.btn_add_customer_main).setOnClickListener(this);
        layoutCustomersList = (LinearLayout) findViewById(R.id.layout_customers_list);

        // 4. SALES SUB-MODULE BINDINGS
        scrollSalesNew = findViewById(R.id.scroll_sales_new);
        scrollSalesHistory = findViewById(R.id.scroll_sales_history);
        btnSalesTabNew = (Button) findViewById(R.id.btn_sales_tab_new);
        btnSalesTabHistory = (Button) findViewById(R.id.btn_sales_tab_history);

        btnSalesTabNew.setOnClickListener(this);
        btnSalesTabHistory.setOnClickListener(this);

        spinnerSalesCustomer = (Spinner) findViewById(R.id.spinner_sales_customer);
        spinnerSalesProducts = (Spinner) findViewById(R.id.spinner_sales_products);
        tvSalesQty = (TextView) findViewById(R.id.tv_sales_qty);
        layoutSalesCart = (LinearLayout) findViewById(R.id.layout_sales_cart);

        tvSalesSubtotal = (TextView) findViewById(R.id.tv_sales_subtotal);
        tvSalesGrandTotal = (TextView) findViewById(R.id.tv_sales_grand_total);
        tvSalesRemainingBalance = (TextView) findViewById(R.id.tv_sales_remaining_balance);

        etSalesDiscount = (EditText) findViewById(R.id.et_sales_discount);
        etSalesTaxRate = (EditText) findViewById(R.id.et_sales_tax_rate);
        etSalesAmountReceived = (EditText) findViewById(R.id.et_sales_amount_received);

        findViewById(R.id.btn_qty_minus).setOnClickListener(this);
        findViewById(R.id.btn_qty_plus).setOnClickListener(this);
        findViewById(R.id.btn_add_to_cart).setOnClickListener(this);
        findViewById(R.id.btn_complete_checkout).setOnClickListener(this);

        TextWatcher checkMathWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateCheckoutTotals();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etSalesDiscount.addTextChangedListener(checkMathWatcher);
        etSalesTaxRate.addTextChangedListener(checkMathWatcher);
        etSalesAmountReceived.addTextChangedListener(checkMathWatcher);

        layoutSalesHistoryList = (LinearLayout) findViewById(R.id.layout_sales_history_list);

        // 5. EXPENSES MODULE BINDINGS
        spinnerExpenseCategory = (Spinner) findViewById(R.id.spinner_expense_category);
        etExpenseAmount = (EditText) findViewById(R.id.et_expense_amount);
        etExpenseDesc = (EditText) findViewById(R.id.et_expense_desc);
        layoutExpensesList = (LinearLayout) findViewById(R.id.layout_expenses_list);
        customPerformanceChart = (CustomChartView) findViewById(R.id.custom_performance_chart);

        tvReportDaily = (TextView) findViewById(R.id.tv_report_daily);
        tvReportWeekly = (TextView) findViewById(R.id.tv_report_weekly);
        tvReportMonthly = (TextView) findViewById(R.id.tv_report_monthly);
        tvReportBestSeller = (TextView) findViewById(R.id.tv_report_best_seller);

        findViewById(R.id.btn_save_expense).setOnClickListener(this);

        ArrayAdapter<String> expCatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, expenseCategories);
        spinnerExpenseCategory.setAdapter(expCatAdapter);
    }

    private void changeNavigationTab(int tabIndex) {
        activeTab = tabIndex;
        
        tabDashboard.setBackgroundColor(Color.TRANSPARENT);
        tabProducts.setBackgroundColor(Color.TRANSPARENT);
        tabCustomers.setBackgroundColor(Color.TRANSPARENT);
        tabSales.setBackgroundColor(Color.TRANSPARENT);
        tabExpenses.setBackgroundColor(Color.TRANSPARENT);

        tvTabDashboard.setTextColor(0xFF757575);
        tvTabProducts.setTextColor(0xFF757575);
        tvTabCustomers.setTextColor(0xFF757575);
        tvTabSales.setTextColor(0xFF757575);
        tvTabExpenses.setTextColor(0xFF757575);

        int highlight = confDarkTheme ? 0xFF303F9F : 0xFFE1F5FE;
        int activeText = confDarkTheme ? 0xFFFFFFFF : 0xFF1565C0;

        if (tabIndex == 0) {
            tabDashboard.setBackgroundColor(highlight);
            tvTabDashboard.setTextColor(activeText);
        } else if (tabIndex == 1) {
            tabProducts.setBackgroundColor(highlight);
            tvTabProducts.setTextColor(activeText);
        } else if (tabIndex == 2) {
            tabCustomers.setBackgroundColor(highlight);
            tvTabCustomers.setTextColor(activeText);
        } else if (tabIndex == 3) {
            tabSales.setBackgroundColor(highlight);
            tvTabSales.setTextColor(activeText);
        } else if (tabIndex == 4) {
            tabExpenses.setBackgroundColor(highlight);
            tvTabExpenses.setTextColor(activeText);
        }

        refreshActiveScreen();
    }

    private void refreshActiveScreen() {
        vDashboard.setVisibility(activeTab == 0 ? View.VISIBLE : View.GONE);
        vProducts.setVisibility(activeTab == 1 ? View.VISIBLE : View.GONE);
        vCustomers.setVisibility(activeTab == 2 ? View.VISIBLE : View.GONE);
        vSales.setVisibility(activeTab == 3 ? View.VISIBLE : View.GONE);
        vExpenses.setVisibility(activeTab == 4 ? View.VISIBLE : View.GONE);

        if (activeTab == 0) {
            refreshDashboard();
        } else if (activeTab == 1) {
            refreshProductsList();
        } else if (activeTab == 2) {
            refreshCustomersList();
        } else if (activeTab == 3) {
            refreshSalesTab();
        } else if (activeTab == 4) {
            refreshExpensesTab();
        }
    }

    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // ==========================================
    // MODULE 1: DASHBOARD
    // ==========================================
    private void refreshDashboard() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = getTodayString();

        // 1. Calculate Today's Sales
        double salesVal = 0.0;
        Cursor c = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date = ?", new String[]{today});
        if (c != null && c.moveToFirst()) {
            salesVal = c.getDouble(0);
            c.close();
        }

        // 2. Calculate Today's Expenses
        double expenseVal = 0.0;
        c = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date = ?", new String[]{today});
        if (c != null && c.moveToFirst()) {
            expenseVal = c.getDouble(0);
            c.close();
        }

        // 3. Profit Margin
        double profitVal = salesVal - expenseVal;

        // 4. Receivables (Aggregate outstanding balances)
        double outstandingVal = 0.0;
        c = db.rawQuery("SELECT SUM(remaining_balance) FROM sales", null);
        if (c != null && c.moveToFirst()) {
            outstandingVal = c.getDouble(0);
            c.close();
        }

        // 5. Products Total
        long totalProducts = 0;
        c = db.rawQuery("SELECT COUNT(id) FROM products", null);
        if (c != null && c.moveToFirst()) {
            totalProducts = c.getLong(0);
            c.close();
        }

        // 6. Low stock calculation
        long lowStockCount = 0;
        c = db.rawQuery("SELECT COUNT(id) FROM products WHERE current_stock <= minimum_stock", null);
        if (c != null && c.moveToFirst()) {
            lowStockCount = c.getLong(0);
            c.close();
        }

        // Format and set fields
        tvDashSales.setText(confCurrency + String.format(Locale.US, "%.2f", salesVal));
        tvDashExpenses.setText(confCurrency + String.format(Locale.US, "%.2f", expenseVal));
        tvDashProfit.setText(confCurrency + String.format(Locale.US, "%.2f", profitVal));
        tvDashReceivables.setText(confCurrency + String.format(Locale.US, "%.2f", outstandingVal));
        tvDashTotalProducts.setText(String.valueOf(totalProducts));
        tvDashLowStock.setText(String.valueOf(lowStockCount));

        // Manage Alarm banners
        StringBuilder alertMsg = new StringBuilder();
        if (lowStockCount > 0) {
            alertMsg.append("⚠️ ").append(lowStockCount).append(" products are critically low in stock!\n");
        }
        if (outstandingVal > 0) {
            alertMsg.append("💳 Outstanding customer dues totaling ").append(confCurrency).append(String.format(Locale.US, "%.2f", outstandingVal)).append(" pending.");
        }

        if (alertMsg.length() > 0) {
            tvDashAlerts.setVisibility(View.VISIBLE);
            tvDashAlerts.setText(alertMsg.toString());
        } else {
            tvDashAlerts.setVisibility(View.GONE);
        }

        // Recent Invoices list layout
        layoutRecentTransactions.removeAllViews();
        Cursor logsCursor = db.rawQuery("SELECT s.invoice_no, c.name, s.grand_total, s.remaining_balance FROM sales s LEFT JOIN customers c ON s.customer_id = c.id ORDER BY s.id DESC LIMIT 5", null);
        if (logsCursor != null && logsCursor.getCount() > 0) {
            while (logsCursor.moveToNext()) {
                String inv = logsCursor.getString(0);
                String cust = logsCursor.getString(1);
                double total = logsCursor.getDouble(2);
                double rem = logsCursor.getDouble(3);

                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setPadding(10, 12, 10, 12);
                itemRow.setBackgroundColor(confDarkTheme ? 0xFF2C2C2C : 0xFFFFFFFF);

                TextView tvLogLeft = new TextView(this);
                tvLogLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
                tvLogLeft.setText(inv + " - " + (cust == null ? "Walk-in Guest" : cust));
                tvLogLeft.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
                tvLogLeft.setTextSize(13sp);

                TextView tvLogRight = new TextView(this);
                tvLogRight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
                tvLogRight.setText(confCurrency + String.format(Locale.US, "%.2f", total) + (rem > 0 ? " (Due)" : ""));
                tvLogRight.setTextColor(rem > 0 ? 0xFFD84315 : 0xFF2E7D32);
                tvLogRight.setGravity(Gravity.END);
                tvLogRight.setTextSize(13sp);

                itemRow.addView(tvLogLeft);
                itemRow.addView(tvLogRight);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(0xFFE0E0E0);

                layoutRecentTransactions.addView(itemRow);
                layoutRecentTransactions.addView(divider);
            }
            logsCursor.close();
        } else {
            TextView emptyText = new TextView(this);
            emptyText.setText("No invoices recorded yet");
            emptyText.setPadding(16, 24, 16, 24);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(Color.GRAY);
            layoutRecentTransactions.addView(emptyText);
        }
    }

    // ==========================================
    // MODULE 2: PRODUCT INVENTORY
    // ==========================================
    private void refreshProductsList() {
        layoutProductsList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String activeFilterCat = spinnerCategoryFilter.getSelectedItem() != null ? spinnerCategoryFilter.getSelectedItem().toString() : "All Categories";

        ArrayList<String> queryArgs = new ArrayList<>();
        String sql = "SELECT id, name, sku, category, purchase_price, selling_price, current_stock, minimum_stock FROM products WHERE 1=1";

        if (!"All Categories".equals(activeFilterCat)) {
            sql += " AND category = ?";
            queryArgs.add(activeFilterCat);
        }

        if (globalQuery.length() > 0) {
            sql += " AND (name LIKE ? OR sku LIKE ?)";
            queryArgs.add("%" + globalQuery + "%");
            queryArgs.add("%" + globalQuery + "%");
        }

        sql += " ORDER BY id DESC";

        String[] arguments = queryArgs.toArray(new String[0]);
        Cursor c = db.rawQuery(sql, arguments);

        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                final int pid = c.getInt(0);
                final String pName = c.getString(1);
                final String pSku = c.getString(2);
                final String pCat = c.getString(3);
                final double pPurchase = c.getDouble(4);
                final double pSelling = c.getDouble(5);
                final int pStock = c.getInt(6);
                final int pMinStock = c.getInt(7);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(14, 14, 14, 14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(lp);
                card.setBackgroundColor(confDarkTheme ? 0xFF1E1E1E : Color.WHITE);
                card.setElevation(2f);

                TextView tvHeader = new TextView(this);
                tvHeader.setText(pName + " (" + pSku + ")");
                tvHeader.setTextSize(15sp);
                tvHeader.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
                tvHeader.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);

                TextView tvMeta = new TextView(this);
                tvMeta.setText("Cat: " + pCat + " | Buy: " + confCurrency + String.format(Locale.US, "%.2f", pPurchase) + " | Sell: " + confCurrency + String.format(Locale.US, "%.2f", pSelling));
                tvMeta.setTextSize(13sp);
                tvMeta.setTextColor(confDarkTheme ? 0xFFB0BEC5 : 0xFF555555);
                tvMeta.setPadding(0, 4, 0, 4);

                TextView tvStockCount = new TextView(this);
                tvStockCount.setText("In Stock: " + pStock + " (Alert Threshold: " + pMinStock + ")");
                tvStockCount.setTextSize(13sp);
                tvStockCount.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                if (pStock <= pMinStock) {
                    tvStockCount.setTextColor(0xFFD84315);
                    tvStockCount.append(" [LOW STOCK WARNING!]");
                } else {
                    tvStockCount.setTextColor(0xFF2E7D32);
                }

                // Interactive controller row
                LinearLayout btnLayout = new LinearLayout(this);
                btnLayout.setOrientation(LinearLayout.HORIZONTAL);
                btnLayout.setPadding(0, 8, 0, 0);

                Button btnAddStock = new Button(this);
                btnAddStock.setText("Add Stock");
                btnAddStock.setTextSize(11sp);
                btnAddStock.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showQuickStockAdjustDialog(pid, pName, pStock);
                    }
                });

                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setTextSize(11sp);
                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProductFormDialog(pid, pName, pSku, pCat, pPurchase, pSelling, pStock, pMinStock);
                    }
                });

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(11sp);
                btnDelete.setTextColor(Color.RED);
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        triggerProductDeleteConfirmation(pid, pName);
                    }
                });

                btnLayout.addView(btnAddStock);
                btnLayout.addView(btnEdit);
                btnLayout.addView(btnDelete);

                card.addView(tvHeader);
                card.addView(tvMeta);
                card.addView(tvStockCount);
                card.addView(btnLayout);

                layoutProductsList.addView(card);
            }
            c.close();
        } else {
            TextView emptyText = new TextView(this);
            emptyText.setText("No inventory products matched your filter.");
            emptyText.setPadding(16, 40, 16, 40);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(Color.GRAY);
            layoutProductsList.addView(emptyText);
        }
    }

    private void showQuickStockAdjustDialog(final int pid, String name, final int currentStock) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Adjust Inventory: " + name);
        b.setMessage("Current Stock level: " + currentStock);

        final EditText etStockAdd = new EditText(this);
        etStockAdd.setHint("Units to add (e.g. 10 or -5)");
        etStockAdd.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        b.setView(etStockAdd);

        b.setPositiveButton("Apply Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = etStockAdd.getText().toString().trim();
                if (input.isEmpty()) return;
                try {
                    int change = Integer.parseInt(input);
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("UPDATE products SET current_stock = current_stock + ? WHERE id = ?", new Object[]{change, pid});
                    Toast.makeText(MainActivity.this, "Stock level successfully updated!", Toast.LENGTH_SHORT).show();
                    refreshProductsList();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                }
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void showProductFormDialog(final int pid, String pName, String pSku, String pCat, double pBuy, double pSell, int pStock, int pAlert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final boolean isEditMode = (pid != -1);
        builder.setTitle(isEditMode ? "Modify Product Details" : "Register New Product");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 24, 30, 24);

        final EditText etName = new EditText(this);
        etName.setHint("Product Name");
        etName.setText(isEditMode ? pName : "");

        final EditText etSku = new EditText(this);
        etSku.setHint("Unique SKU / Barcode");
        etSku.setText(isEditMode ? pSku : "");

        final Spinner spinnerCategorySelection = new Spinner(this);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, preCategories);
        spinnerCategorySelection.setAdapter(catAdapter);
        if (isEditMode) {
            for (int i = 0; i < preCategories.length; i++) {
                if (preCategories[i].equals(pCat)) {
                    spinnerCategorySelection.setSelection(i);
                    break;
                }
            }
        }

        final EditText etPurchase = new EditText(this);
        etPurchase.setHint("Purchase Price / Unit");
        etPurchase.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPurchase.setText(isEditMode ? String.valueOf(pBuy) : "");

        final EditText etSelling = new EditText(this);
        etSelling.setHint("Selling Price / Unit");
        etSelling.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etSelling.setText(isEditMode ? String.valueOf(pSell) : "");

        final EditText etStock = new EditText(this);
        etStock.setHint("Initial Stock Quantity");
        etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etStock.setText(isEditMode ? String.valueOf(pStock) : "");

        final EditText etAlertVal = new EditText(this);
        etAlertVal.setHint("Low Stock Alert Level");
        etAlertVal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etAlertVal.setText(isEditMode ? String.valueOf(pAlert) : "5");

        mainLayout.addView(etName);
        mainLayout.addView(etSku);
        mainLayout.addView(spinnerCategorySelection);
        mainLayout.addView(etPurchase);
        mainLayout.addView(etSelling);
        mainLayout.addView(etStock);
        mainLayout.addView(etAlertVal);

        builder.setView(mainLayout);

        builder.setPositiveButton("Save Product", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String sku = etSku.getText().toString().trim();
                String category = spinnerCategorySelection.getSelectedItem().toString();
                String rawBuy = etPurchase.getText().toString().trim();
                String rawSell = etSelling.getText().toString().trim();
                String rawStock = etStock.getText().toString().trim();
                String rawAlert = etAlertVal.getText().toString().trim();

                if (name.isEmpty() || sku.isEmpty() || rawBuy.isEmpty() || rawSell.isEmpty() || rawStock.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please complete all fields correctly", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double buy = Double.parseDouble(rawBuy);
                    double sell = Double.parseDouble(rawSell);
                    int stock = Integer.parseInt(rawStock);
                    int alertLimit = rawAlert.isEmpty() ? 5 : Integer.parseInt(rawAlert);

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("name", name);
                    cv.put("sku", sku);
                    cv.put("category", category);
                    cv.put("purchase_price", buy);
                    cv.put("selling_price", sell);
                    cv.put("current_stock", stock);
                    cv.put("minimum_stock", alertLimit);

                    if (isEditMode) {
                        db.update("products", cv, "id = ?", new String[]{String.valueOf(pid)});
                        Toast.makeText(MainActivity.this, "Product changes stored successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        long res = db.insert("products", null, cv);
                        if (res == -1) {
                            Toast.makeText(MainActivity.this, "SKU already matches another item!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "New product created", Toast.LENGTH_SHORT).show();
                        }
                    }
                    refreshProductsList();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Database update fault: Check price format", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void triggerProductDeleteConfirmation(final int pid, String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you absolutely sure you want to delete product '" + name + "'? This action is destructive.");
        builder.setPositiveButton("Delete Forever", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("products", "id = ?", new String[]{String.valueOf(pid)});
                Toast.makeText(MainActivity.this, "Product cleared from database", Toast.LENGTH_SHORT).show();
                refreshProductsList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ==========================================
    // MODULE 3: CLIENT DIRECTORY
    // ==========================================
    private void refreshCustomersList() {
        layoutCustomersList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT id, name, phone, address FROM customers";
        ArrayList<String> argsList = new ArrayList<>();

        if (globalQuery.length() > 0) {
            sql += " WHERE name LIKE ? OR phone LIKE ?";
            argsList.add("%" + globalQuery + "%");
            argsList.add("%" + globalQuery + "%");
        }

        sql += " ORDER BY id DESC";

        Cursor c = db.rawQuery(sql, argsList.toArray(new String[0]));

        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                final int cid = c.getInt(0);
                final String cName = c.getString(1);
                final String cPhone = c.getString(2);
                final String cAddr = c.getString(3);

                // Fetch dynamic cumulative statistics for this customer
                double purchaseTotal = 0.0;
                double unpaidBalance = 0.0;
                Cursor statCursor = db.rawQuery("SELECT SUM(grand_total), SUM(remaining_balance) FROM sales WHERE customer_id = ?", new String[]{String.valueOf(cid)});
                if (statCursor != null && statCursor.moveToFirst()) {
                    purchaseTotal = statCursor.getDouble(0);
                    unpaidBalance = statCursor.getDouble(1);
                    statCursor.close();
                }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(14, 14, 14, 14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(lp);
                card.setBackgroundColor(confDarkTheme ? 0xFF1E1E1E : Color.WHITE);
                card.setElevation(2f);

                TextView tvName = new TextView(this);
                tvName.setText(cName);
                tvName.setTextSize(15sp);
                tvName.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
                tvName.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);

                TextView tvContact = new TextView(this);
                tvContact.setText("📞: " + cPhone + "  |  📍: " + cAddr);
                tvContact.setTextSize(13sp);
                tvContact.setTextColor(confDarkTheme ? 0xFFB0BEC5 : 0xFF555555);
                tvContact.setPadding(0, 4, 0, 4);

                TextView tvStats = new TextView(this);
                tvStats.setText("Cumulative Purchases: " + confCurrency + String.format(Locale.US, "%.2f", purchaseTotal));
                tvStats.append("  |  ");
                tvStats.append("Dues: " + confCurrency + String.format(Locale.US, "%.2f", unpaidBalance));
                tvStats.setTextSize(13sp);
                tvStats.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                tvStats.setTextColor(unpaidBalance > 0 ? 0xFFC62828 : 0xFF2E7D32);

                // Interactive controller row
                LinearLayout btnLayout = new LinearLayout(this);
                btnLayout.setOrientation(LinearLayout.HORIZONTAL);
                btnLayout.setPadding(0, 8, 0, 0);

                Button btnHistory = new Button(this);
                btnHistory.setText("Invoices");
                btnHistory.setTextSize(11sp);
                btnHistory.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCustomerLedgerDialog(cid, cName);
                    }
                });

                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setTextSize(11sp);
                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCustomerFormDialog(cid, cName, cPhone, cAddr);
                    }
                });

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(11sp);
                btnDelete.setTextColor(Color.RED);
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        triggerCustomerDeleteConfirmation(cid, cName);
                    }
                });

                btnLayout.addView(btnHistory);
                btnLayout.addView(btnEdit);
                btnLayout.addView(btnDelete);

                card.addView(tvName);
                card.addView(tvContact);
                card.addView(tvStats);
                card.addView(btnLayout);

                layoutCustomersList.addView(card);
            }
            c.close();
        } else {
            TextView emptyText = new TextView(this);
            emptyText.setText("No customers matched database records.");
            emptyText.setPadding(16, 40, 16, 40);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(Color.GRAY);
            layoutCustomersList.addView(emptyText);
        }
    }

    private void showCustomerFormDialog(final int cid, String cName, String cPhone, String cAddr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final boolean isEditMode = (cid != -1);
        builder.setTitle(isEditMode ? "Modify Client Record" : "Add New Customer");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 24, 30, 24);

        final EditText etName = new EditText(this);
        etName.setHint("Full Name / Business Entity");
        etName.setText(isEditMode ? cName : "");

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Contact Number");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        etPhone.setText(isEditMode ? cPhone : "");

        final EditText etAddress = new EditText(this);
        etAddress.setHint("Mailing / Corporate Address");
        etAddress.setText(isEditMode ? cAddr : "");

        layout.addView(etName);
        layout.addView(etPhone);
        layout.addView(etAddress);

        builder.setView(layout);

        builder.setPositiveButton("Commit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String address = etAddress.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert name", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("name", name);
                cv.put("phone", phone);
                cv.put("address", address);

                if (isEditMode) {
                    db.update("customers", cv, "id = ?", new String[]{String.valueOf(cid)});
                    Toast.makeText(MainActivity.this, "Client information updated", Toast.LENGTH_SHORT).show();
                } else {
                    db.insert("customers", null, cv);
                    Toast.makeText(MainActivity.this, "Client added", Toast.LENGTH_SHORT).show();
                }

                refreshCustomersList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCustomerLedgerDialog(int cid, String name) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Sales Invoice Ledger: " + name);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setPadding(20, 20, 20, 20);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT invoice_no, date, grand_total, remaining_balance FROM sales WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(cid)});

        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                String inv = c.getString(0);
                String dt = c.getString(1);
                double tot = c.getDouble(2);
                double rem = c.getDouble(3);

                TextView item = new TextView(this);
                item.setPadding(0, 8, 0, 8);
                item.setTextSize(13sp);
                item.setText(inv + " (" + dt + ") - Total: " + confCurrency + String.format(Locale.US, "%.2f", tot) + " | Balance: " + confCurrency + String.format(Locale.US, "%.2f", rem));
                item.setTextColor(rem > 0 ? 0xFFC62828 : 0xFF2E7D32);
                parent.addView(item);

                View line = new View(this);
                line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                line.setBackgroundColor(Color.LTGRAY);
                parent.addView(line);
            }
            c.close();
        } else {
            TextView empty = new TextView(this);
            empty.setText("No purchases recorded for this customer");
            empty.setTextColor(Color.GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 20, 0, 20);
            parent.addView(empty);
        }

        b.setView(parent);
        b.setPositiveButton("Close", null);
        b.show();
    }

    private void triggerCustomerDeleteConfirmation(final int cid, String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Customer Account");
        builder.setMessage("This will remove customer '" + name + "'. Past sale transactions under this customer remains linked dynamically to guest logs.");
        builder.setPositiveButton("Confirm deletion", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("customers", "id = ?", new String[]{String.valueOf(cid)});
                Toast.makeText(MainActivity.this, "Client successfully cleared from system logs", Toast.LENGTH_SHORT).show();
                refreshCustomersList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ==========================================
    // MODULE 4: SALES MANAGEMENT & CART
    // ==========================================
    private void refreshSalesTab() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Bind Invoices/Sales list
        if (scrollSalesNew.getVisibility() == View.VISIBLE) {
            // New Sale form loader
            ArrayList<String> customersSpinnerList = new ArrayList<>();
            final ArrayList<Integer> customersIdList = new ArrayList<>();

            // Guest default
            customersSpinnerList.add("Walk-In Customer (Cash Sale)");
            customersIdList.add(-1);

            Cursor c = db.rawQuery("SELECT id, name FROM customers ORDER BY name ASC", null);
            if (c != null) {
                while (c.moveToNext()) {
                    customersIdList.add(c.getInt(0));
                    customersSpinnerList.add(c.getString(1));
                }
                c.close();
            }

            ArrayAdapter<String> custAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, customersSpinnerList);
            spinnerSalesCustomer.setAdapter(custAdapter);

            // Bind products dropdown
            final ArrayList<String> productsSpinnerList = new ArrayList<>();
            final ArrayList<Integer> productsIdList = new ArrayList<>();
            final ArrayList<Double> productsPriceList = new ArrayList<>();
            final ArrayList<Integer> productsStockList = new ArrayList<>();

            Cursor pCursor = db.rawQuery("SELECT id, name, sku, selling_price, current_stock FROM products WHERE current_stock > 0 ORDER BY name ASC", null);
            if (pCursor != null && pCursor.getCount() > 0) {
                while (pCursor.moveToNext()) {
                    productsIdList.add(pCursor.getInt(0));
                    String label = pCursor.getString(1) + " (" + pCursor.getString(2) + ") - " + confCurrency + String.format(Locale.US, "%.2f", pCursor.getDouble(3)) + " (Stock: " + pCursor.getInt(4) + ")";
                    productsSpinnerList.add(label);
                    productsPriceList.add(pCursor.getDouble(3));
                    productsStockList.add(pCursor.getInt(4));
                }
                pCursor.close();
            } else {
                productsSpinnerList.add("No stock available!");
            }

            ArrayAdapter<String> prodSelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, productsSpinnerList);
            spinnerSalesProducts.setAdapter(prodSelAdapter);

            // Trigger cart graphics refresh
            renderCartLayout();
            etSalesTaxRate.setText(String.valueOf(confTaxRate));

        } else {
            // Past invoices tab loader
            layoutSalesHistoryList.removeAllViews();
            String sql = "SELECT s.id, s.invoice_no, s.date, s.grand_total, s.remaining_balance, c.name FROM sales s LEFT JOIN customers c ON s.customer_id = c.id";
            ArrayList<String> args = new ArrayList<>();

            if (globalQuery.length() > 0) {
                sql += " WHERE s.invoice_no LIKE ? OR c.name LIKE ?";
                args.add("%" + globalQuery + "%");
                args.add("%" + globalQuery + "%");
            }

            sql += " ORDER BY s.id DESC";

            Cursor c = db.rawQuery(sql, args.toArray(new String[0]));

            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    final int sid = c.getInt(0);
                    final String sInv = c.getString(1);
                    final String sDate = c.getString(2);
                    final double sTotal = c.getDouble(3);
                    final double sRemaining = c.getDouble(4);
                    final String clientName = c.getString(5);

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(14, 14, 14, 14);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 10);
                    card.setLayoutParams(lp);
                    card.setBackgroundColor(confDarkTheme ? 0xFF1E1E1E : Color.WHITE);
                    card.setElevation(2f);

                    TextView tvInvNo = new TextView(this);
                    tvInvNo.setText("Invoice: " + sInv + " | Customer: " + (clientName == null ? "Walk-In" : clientName));
                    tvInvNo.setTextSize(14sp);
                    tvInvNo.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
                    tvInvNo.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);

                    TextView tvDetails = new TextView(this);
                    tvDetails.setText("Date: " + sDate + "  |  Grand Total: " + confCurrency + String.format(Locale.US, "%.2f", sTotal));
                    tvDetails.setTextSize(13sp);
                    tvDetails.setTextColor(confDarkTheme ? 0xFFB0BEC5 : 0xFF555555);
                    tvDetails.setPadding(0, 4, 0, 4);

                    TextView tvStatus = new TextView(this);
                    tvStatus.setTextSize(13sp);
                    tvStatus.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                    if (sRemaining > 0) {
                        tvStatus.setText("Outstanding Debt: " + confCurrency + String.format(Locale.US, "%.2f", sRemaining));
                        tvStatus.setTextColor(0xFFD84315);
                    } else {
                        tvStatus.setText("Status: Paid In Full");
                        tvStatus.setTextColor(0xFF2E7D32);
                    }

                    LinearLayout btnLine = new LinearLayout(this);
                    btnLine.setOrientation(LinearLayout.HORIZONTAL);
                    btnLine.setPadding(0, 8, 0, 0);

                    Button btnShowReceipt = new Button(this);
                    btnShowReceipt.setText("View Invoice / Receipt");
                    btnShowReceipt.setTextSize(10sp);
                    btnShowReceipt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showInvoicePreviewDialog(sid);
                        }
                    });

                    Button btnCancelInvoice = new Button(this);
                    btnCancelInvoice.setText("Refund / Cancel");
                    btnCancelInvoice.setTextColor(Color.RED);
                    btnCancelInvoice.setTextSize(10sp);
                    btnCancelInvoice.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            triggerRefundSalesInvoice(sid, sInv);
                        }
                    });

                    btnLine.addView(btnShowReceipt);
                    btnLine.addView(btnCancelInvoice);

                    card.addView(tvInvNo);
                    card.addView(tvDetails);
                    card.addView(tvStatus);
                    card.addView(btnLine);

                    layoutSalesHistoryList.addView(card);
                }
                c.close();
            } else {
                TextView empty = new TextView(this);
                empty.setText("No invoice histories match your parameters.");
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(16, 40, 16, 40);
                empty.setTextColor(Color.GRAY);
                layoutSalesHistoryList.addView(empty);
            }
        }
    }

    private void renderCartLayout() {
        layoutSalesCart.removeAllViews();
        if (activeCart.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Your sales cart contains no entries");
            emptyText.setPadding(16, 24, 16, 24);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(Color.GRAY);
            layoutSalesCart.addView(emptyText);
            return;
        }

        for (int i = 0; i < activeCart.size(); i++) {
            final CartItem item = activeCart.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(8, 10, 8, 10);
            row.setBackgroundColor(confDarkTheme ? 0xFF2C2C2C : 0xFFFAFAFA);

            TextView info = new TextView(this);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            info.setText(item.name + "\n" + item.qty + " units x " + confCurrency + String.format(Locale.US, "%.2f", item.sellingPrice));
            info.setTextSize(13sp);
            info.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);

            TextView sumPrice = new TextView(this);
            sumPrice.setText(confCurrency + String.format(Locale.US, "%.2f", item.sellingPrice * item.qty));
            sumPrice.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
            sumPrice.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
            sumPrice.setTextSize(13sp);
            sumPrice.setPadding(8, 0, 8, 0);

            Button rm = new Button(this);
            rm.setText("X");
            rm.setBackgroundColor(Color.LTGRAY);
            rm.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            rm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activeCart.remove(index);
                    renderCartLayout();
                    calculateCheckoutTotals();
                }
            });

            row.addView(info);
            row.addView(sumPrice);
            row.addView(rm);

            View line = new View(this);
            line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            line.setBackgroundColor(Color.LTGRAY);

            layoutSalesCart.addView(row);
            layoutSalesCart.addView(line);
        }
        calculateCheckoutTotals();
    }

    private void calculateCheckoutTotals() {
        double sub = 0.0;
        for (CartItem item : activeCart) {
            sub += item.sellingPrice * item.qty;
        }

        // Apply Discount Flat
        String rawDisc = etSalesDiscount.getText().toString().trim();
        double discount = 0.0;
        if (!rawDisc.isEmpty()) {
            try { discount = Double.parseDouble(rawDisc); } catch (Exception e) {}
        }

        // Apply Tax Rates
        String rawTax = etSalesTaxRate.getText().toString().trim();
        double taxPct = confTaxRate;
        if (!rawTax.isEmpty()) {
            try { taxPct = Double.parseDouble(rawTax); } catch (Exception e) {}
        }

        double discTotal = sub - discount;
        if (discTotal < 0) discTotal = 0;
        double taxAmt = discTotal * (taxPct / 100.0);
        double grand = discTotal + taxAmt;

        // Apply Receivables math
        String rawRec = etSalesAmountReceived.getText().toString().trim();
        double received = grand;
        if (!rawRec.isEmpty()) {
            try { received = Double.parseDouble(rawRec); } catch (Exception e) {}
        }

        double remaining = grand - received;
        if (remaining < 0) remaining = 0.0;

        tvSalesSubtotal.setText(confCurrency + String.format(Locale.US, "%.2f", sub));
        tvSalesGrandTotal.setText(confCurrency + String.format(Locale.US, "%.2f", grand));
        tvSalesRemainingBalance.setText(confCurrency + String.format(Locale.US, "%.2f", remaining));
    }

    private void addProductToInvoiceCart() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, selling_price, current_stock FROM products WHERE current_stock > 0 ORDER BY name ASC", null);

        if (c != null && c.getCount() > 0) {
            int selectedSpinnerPosition = spinnerSalesProducts.getSelectedItemPosition();
            if (selectedSpinnerPosition != -1 && c.moveToPosition(selectedSpinnerPosition)) {
                int pid = c.getInt(0);
                String name = c.getString(1);
                double price = c.getDouble(2);
                int stock = c.getInt(3);

                if (currentQtyCounter > stock) {
                    Toast.makeText(this, "Error: Not enough stock available (" + stock + " left)", Toast.LENGTH_SHORT).show();
                    c.close();
                    return;
                }

                // Check if already matches item inside cart to avoid duplicates
                boolean exists = false;
                for (CartItem item : activeCart) {
                    if (item.id == pid) {
                        int tentativeQty = item.qty + currentQtyCounter;
                        if (tentativeQty > stock) {
                            Toast.makeText(this, "Error: Cannot exceed available stock", Toast.LENGTH_SHORT).show();
                        } else {
                            item.qty = tentativeQty;
                            Toast.makeText(this, "Merged matching entry in cart", Toast.LENGTH_SHORT).show();
                        }
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    CartItem cItem = new CartItem();
                    cItem.id = pid;
                    cItem.name = name;
                    cItem.sellingPrice = price;
                    cItem.qty = currentQtyCounter;
                    activeCart.add(cItem);
                }

                currentQtyCounter = 1;
                tvSalesQty.setText(String.valueOf(currentQtyCounter));
                renderCartLayout();
            }
            c.close();
        } else {
            Toast.makeText(this, "No valid inventory product selection available", Toast.LENGTH_SHORT).show();
        }
    }

    private void finalizeSalesTransactionCheckout() {
        if (activeCart.isEmpty()) {
            Toast.makeText(this, "Add products into cart before checkout processing", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Find customer ID
        int custSpinnerPos = spinnerSalesCustomer.getSelectedItemPosition();
        int customerId = -1; // Guest
        if (custSpinnerPos > 0) {
            // Read index dynamically from DB query sequence matching pos
            Cursor cCust = db.rawQuery("SELECT id FROM customers ORDER BY name ASC", null);
            if (cCust != null) {
                if (cCust.moveToPosition(custSpinnerPos - 1)) {
                    customerId = cCust.getInt(0);
                }
                cCust.close();
            }
        }

        // Subtotal
        double sub = 0.0;
        for (CartItem ci : activeCart) {
            sub += ci.sellingPrice * ci.qty;
        }

        // Discounts and Taxes logic
        String rawDisc = etSalesDiscount.getText().toString().trim();
        double discount = 0.0;
        if (!rawDisc.isEmpty()) {
            try { discount = Double.parseDouble(rawDisc); } catch (Exception e) {}
        }

        String rawTax = etSalesTaxRate.getText().toString().trim();
        double taxPct = confTaxRate;
        if (!rawTax.isEmpty()) {
            try { taxPct = Double.parseDouble(rawTax); } catch (Exception e) {}
        }

        double taxableAmount = sub - discount;
        if (taxableAmount < 0) taxableAmount = 0.0;
        double taxValue = taxableAmount * (taxPct / 100.0);
        double grand = taxableAmount + taxValue;

        // Paid/Receivables math
        String rawRec = etSalesAmountReceived.getText().toString().trim();
        double received = grand;
        if (!rawRec.isEmpty()) {
            try { received = Double.parseDouble(rawRec); } catch (Exception e) {}
        }

        double remaining = grand - received;
        if (remaining < 0) remaining = 0.0;

        String todayDate = getTodayString();
        String invoiceNo = confInvoicePrefix + System.currentTimeMillis();

        db.beginTransaction();
        try {
            // Write core sales transaction
            ContentValues cvSales = new ContentValues();
            cvSales.put("customer_id", customerId);
            cvSales.put("date", todayDate);
            cvSales.put("subtotal", sub);
            cvSales.put("discount", discount);
            cvSales.put("tax", taxValue);
            cvSales.put("grand_total", grand);
            cvSales.put("amount_received", received);
            cvSales.put("remaining_balance", remaining);
            cvSales.put("invoice_no", invoiceNo);

            long saleInsertId = db.insert("sales", null, cvSales);

            if (saleInsertId == -1) {
                throw new RuntimeException("Fatal database anomaly recording sales transaction.");
            }

            // Write relational cart items, reducing inventory stocks accordingly
            for (CartItem ci : activeCart) {
                ContentValues cvItem = new ContentValues();
                cvItem.put("sale_id", saleInsertId);
                cvItem.put("product_id", ci.id);
                cvItem.put("quantity", ci.qty);
                cvItem.put("price", ci.sellingPrice);

                db.insert("sale_items", null, cvItem);

                // Run SQL update to decrement stock level safely
                db.execSQL("UPDATE products SET current_stock = current_stock - ? WHERE id = ?", new Object[]{ci.qty, ci.id});
            }

            db.setTransactionSuccessful();

            // Clear Cart and refresh
            activeCart.clear();
            etSalesDiscount.setText("0");
            etSalesAmountReceived.setText("0");
            Toast.makeText(this, "Invoice checkout complete: " + invoiceNo, Toast.LENGTH_LONG).show();

            // Show beautiful Receipt popup layout
            showInvoicePreviewDialog((int) saleInsertId);

        } catch (Exception e) {
            Toast.makeText(this, "An error occurred compiling transaction logs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }

        refreshSalesTab();
    }

    private void showInvoicePreviewDialog(int saleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor sc = db.rawQuery("SELECT s.invoice_no, s.date, s.subtotal, s.discount, s.tax, s.grand_total, s.amount_received, s.remaining_balance, c.name, c.phone " +
                "FROM sales s LEFT JOIN customers c ON s.customer_id = c.id WHERE s.id = ?", new String[]{String.valueOf(saleId)});

        if (sc == null || !sc.moveToFirst()) {
            if (sc != null) sc.close();
            return;
        }

        String inv = sc.getString(0);
        String date = sc.getString(1);
        double sub = sc.getDouble(2);
        double disc = sc.getDouble(3);
        double tax = sc.getDouble(4);
        double grand = sc.getDouble(5);
        double rec = sc.getDouble(6);
        double rem = sc.getDouble(7);
        String cName = sc.getString(8);
        String cPhone = sc.getString(9);
        sc.close();

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Sales Invoice Preview");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 30, 35, 30);
        layout.setBackgroundColor(Color.WHITE);

        TextView tvInvTitle = new TextView(this);
        tvInvTitle.setText(confBusinessName);
        tvInvTitle.setTextSize(18sp);
        tvInvTitle.setTextColor(Color.BLACK);
        tvInvTitle.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        tvInvTitle.setGravity(Gravity.CENTER);

        TextView tvInvSub = new TextView(this);
        tvInvSub.setText("Proprietor: " + confOwnerName + "\nPhone: " + confPhone + "\n" + date);
        tvInvSub.setTextSize(11sp);
        tvInvSub.setTextColor(Color.GRAY);
        tvInvSub.setGravity(Gravity.CENTER);
        tvInvSub.setPadding(0, 0, 0, 16);

        TextView tvBillDetails = new TextView(this);
        tvBillDetails.setText("Invoice Reference: " + inv + "\nBilled Client: " + (cName == null ? "Walk-In Cash Ledger" : cName + " (" + cPhone + ")"));
        tvBillDetails.setTextSize(13sp);
        tvBillDetails.setTextColor(Color.DKGRAY);
        tvBillDetails.setPadding(0, 4, 0, 12);

        layout.addView(tvInvTitle);
        layout.addView(tvInvSub);
        layout.addView(tvBillDetails);

        // Map invoice checkout items bought
        TextView itemsHeading = new TextView(this);
        itemsHeading.setText("Line Items Breakdown:");
        itemsHeading.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        itemsHeading.setTextColor(Color.BLACK);
        itemsHeading.setTextSize(13sp);
        layout.addView(itemsHeading);

        Cursor itemCursor = db.rawQuery("SELECT p.name, si.quantity, si.price FROM sale_items si JOIN products p ON si.product_id = p.id WHERE si.sale_id = ?", new String[]{String.valueOf(saleId)});
        if (itemCursor != null) {
            while (itemCursor.moveToNext()) {
                TextView itemRow = new TextView(this);
                itemRow.setText("• " + itemCursor.getString(0) + " (Qty: " + itemCursor.getInt(1) + ") @ " + confCurrency + String.format(Locale.US, "%.2f", itemCursor.getDouble(2)));
                itemRow.setTextColor(Color.BLACK);
                itemRow.setTextSize(12sp);
                layout.addView(itemRow);
            }
            itemCursor.close();
        }

        // Aggregate financial summaries dynamically
        TextView financeDetails = new TextView(this);
        financeDetails.setPadding(0, 12, 0, 0);
        financeDetails.setTextSize(13sp);
        financeDetails.setTextColor(Color.BLACK);
        financeDetails.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        financeDetails.setText("----------------------------------------------\n" +
                "Subtotal: " + confCurrency + String.format(Locale.US, "%.2f", sub) + "\n" +
                "Discounts Applied: " + confCurrency + String.format(Locale.US, "%.2f", disc) + "\n" +
                "Taxes Levied: " + confCurrency + String.format(Locale.US, "%.2f", tax) + "\n" +
                "GRAND TOTAL DUE: " + confCurrency + String.format(Locale.US, "%.2f", grand) + "\n" +
                "Payment Received: " + confCurrency + String.format(Locale.US, "%.2f", rec) + "\n" +
                "Outstanding Balance: " + confCurrency + String.format(Locale.US, "%.2f", rem));
        layout.addView(financeDetails);

        b.setView(layout);
        b.setPositiveButton("Print / OK", null);
        b.show();
    }

    private void triggerRefundSalesInvoice(final int saleId, String invoiceNo) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Refund / Void Transaction");
        b.setMessage("Are you sure you want to void and refund invoice: " + invoiceNo + "? This cancels outstanding payments and restores cart item quantities to our warehouse stocks.");
        b.setPositiveButton("Process Full Refund", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    // Loop relational checkout items list to restore product levels
                    Cursor c = db.rawQuery("SELECT product_id, quantity FROM sale_items WHERE sale_id = ?", new String[]{String.valueOf(saleId)});
                    if (c != null) {
                        while (c.moveToNext()) {
                            int pid = c.getInt(0);
                            int qty = c.getInt(1);
                            db.execSQL("UPDATE products SET current_stock = current_stock + ? WHERE id = ?", new Object[]{qty, pid});
                        }
                        c.close();
                    }

                    // Delete sale and details rows
                    db.delete("sale_items", "sale_id = ?", new String[]{String.valueOf(saleId)});
                    db.delete("sales", "id = ?", new String[]{String.valueOf(saleId)});

                    db.setTransactionSuccessful();
                    Toast.makeText(MainActivity.this, "Invoice successfully voided. Stock returned.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Refund error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    db.endTransaction();
                }
                refreshSalesTab();
            }
        });
        b.setNegativeButton("Abort", null);
        b.show();
    }

    // ==========================================
    // MODULE 5: EXPENSE TRACKER & PERFORMANCE
    // ==========================================
    private void refreshExpensesTab() {
        layoutExpensesList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Bind history logs
        String sql = "SELECT id, category, amount, date, description FROM expenses";
        ArrayList<String> queryArgs = new ArrayList<>();

        if (globalQuery.length() > 0) {
            sql += " WHERE category LIKE ? OR description LIKE ?";
            queryArgs.add("%" + globalQuery + "%");
            queryArgs.add("%" + globalQuery + "%");
        }

        sql += " ORDER BY id DESC";

        Cursor c = db.rawQuery(sql, queryArgs.toArray(new String[0]));

        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                final int eid = c.getInt(0);
                String cat = c.getString(1);
                double amount = c.getDouble(2);
                String date = c.getString(3);
                String desc = c.getString(4);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(14, 14, 14, 14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(lp);
                card.setBackgroundColor(confDarkTheme ? 0xFF1E1E1E : Color.WHITE);
                card.setElevation(1f);

                TextView tvHeader = new TextView(this);
                tvHeader.setText(cat + " - " + confCurrency + String.format(Locale.US, "%.2f", amount));
                tvHeader.setTextSize(14sp);
                tvHeader.setTextColor(confDarkTheme ? Color.WHITE : Color.BLACK);
                tvHeader.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);

                TextView tvDesc = new TextView(this);
                tvDesc.setText("Date: " + date + "\nMemo: " + desc);
                tvDesc.setTextSize(13sp);
                tvDesc.setTextColor(confDarkTheme ? 0xFFB0BEC5 : 0xFF555555);

                Button btnDelete = new Button(this);
                btnDelete.setText("Clear Record");
                btnDelete.setTextSize(9sp);
                btnDelete.setTextColor(Color.RED);
                btnDelete.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 36));
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("expenses", "id = ?", new String[]{String.valueOf(eid)});
                        Toast.makeText(MainActivity.this, "Expense log removed", Toast.LENGTH_SHORT).show();
                        refreshExpensesTab();
                    }
                });

                card.addView(tvHeader);
                card.addView(tvDesc);
                card.addView(btnDelete);

                layoutExpensesList.addView(card);
            }
            c.close();
        } else {
            TextView emptyText = new TextView(this);
            emptyText.setText("No business expenses logged yet.");
            emptyText.setPadding(16, 32, 16, 32);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTextColor(Color.GRAY);
            layoutExpensesList.addView(emptyText);
        }

        // Draw custom Canvas performance indexes and reports
        runOperationalFinancialReports();
    }

    private void runOperationalFinancialReports() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Pre-requisites: Fetch last 7 days of performance stats dynamically
        float[] salesArray = new float[7];
        float[] expensesArray = new float[7];
        String[] labelsArray = new String[7];

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.US);
        SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            String dbDateStr = sdfDb.format(cal.getTime());
            labelsArray[i] = sdf.format(cal.getTime());

            // Sales total for date
            salesArray[i] = 0f;
            Cursor cs = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date = ?", new String[]{dbDateStr});
            if (cs != null && cs.moveToFirst()) {
                salesArray[i] = cs.getFloat(0);
                cs.close();
            }

            // Expense total for date
            expensesArray[i] = 0f;
            Cursor ce = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date = ?", new String[]{dbDateStr});
            if (ce != null && ce.moveToFirst()) {
                expensesArray[i] = ce.getFloat(0);
                ce.close();
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Direct inputs to Custom Performance Canvas view
        customPerformanceChart.updateData(salesArray, expensesArray, labelsArray);

        // Run monthly / weekly / daily performance numbers
        String today = getTodayString();

        // 1. Daily Summary
        double dailySales = 0.0, dailyExp = 0.0;
        Cursor cr = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date = ?", new String[]{today});
        if (cr != null && cr.moveToFirst()) { dailySales = cr.getDouble(0); cr.close(); }
        cr = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date = ?", new String[]{today});
        if (cr != null && cr.moveToFirst()) { dailyExp = cr.getDouble(0); cr.close(); }
        tvReportDaily.setText("Today's Summary - Sales: " + confCurrency + String.format(Locale.US, "%.2f", dailySales) + " | Expenses: " + confCurrency + String.format(Locale.US, "%.2f", dailyExp));

        // 2. Weekly summary (Last 7 days cumulative metrics)
        double weeklySales = 0.0, weeklyExp = 0.0;
        for (float val : salesArray) weeklySales += val;
        for (float val : expensesArray) weeklyExp += val;
        tvReportWeekly.setText("Last 7 Days Balance: Profit of " + confCurrency + String.format(Locale.US, "%.2f", (weeklySales - weeklyExp)));

        // 3. Monthly Cumulative Profits
        double monthlySales = 0.0, monthlyExp = 0.0;
        Calendar startMonthCal = Calendar.getInstance();
        startMonthCal.set(Calendar.DAY_OF_MONTH, 1);
        String startMonthStr = sdfDb.format(startMonthCal.getTime());

        cr = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date >= ?", new String[]{startMonthStr});
        if (cr != null && cr.moveToFirst()) { monthlySales = cr.getDouble(0); cr.close(); }
        cr = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date >= ?", new String[]{startMonthStr});
        if (cr != null && cr.moveToFirst()) { monthlyExp = cr.getDouble(0); cr.close(); }
        tvReportMonthly.setText("Monthly Total Margin - Profit: " + confCurrency + String.format(Locale.US, "%.2f", (monthlySales - monthlyExp)));

        // 4. Best Selling product dynamically calculated
        String bestSeller = "None recorded";
        cr = db.rawQuery("SELECT p.name, SUM(si.quantity) as total_qty " +
                "FROM sale_items si JOIN products p ON si.product_id = p.id " +
                "GROUP BY si.product_id " +
                "ORDER BY total_qty DESC LIMIT 1", null);
        if (cr != null && cr.moveToFirst()) {
            bestSeller = cr.getString(0) + " (" + cr.getInt(1) + " sold)";
            cr.close();
        }
        tvReportBestSeller.setText("⭐ Top Selling Product: " + bestSeller);
    }

    private void saveOperationalExpense() {
        String amountRaw = etExpenseAmount.getText().toString().trim();
        String desc = etExpenseDesc.getText().toString().trim();
        String cat = spinnerExpenseCategory.getSelectedItem().toString();

        if (amountRaw.isEmpty()) {
            Toast.makeText(this, "Please write expense amount limit", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountRaw);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put("category", cat);
            cv.put("amount", amount);
            cv.put("date", getTodayString());
            cv.put("description", desc.isEmpty() ? "No Description" : desc);

            db.insert("expenses", null, cv);
            Toast.makeText(this, "Expense transaction recorded successfully", Toast.LENGTH_SHORT).show();

            // Clear layout inputs
            etExpenseAmount.setText("");
            etExpenseDesc.setText("");

            refreshExpensesTab();
        } catch (Exception e) {
            Toast.makeText(this, "Error processing calculations input", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // NOTIFICATIONS PANEL POPUP
    // ==========================================
    private void launchNotificationsSummaryDialog() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder message = new StringBuilder();

        // 1. Low Stock scan
        Cursor c = db.rawQuery("SELECT name, current_stock, minimum_stock FROM products WHERE current_stock <= minimum_stock", null);
        if (c != null && c.getCount() > 0) {
            message.append("📦 REORDER ALERTS:\n");
            while (c.moveToNext()) {
                message.append("• ").append(c.getString(0)).append(" is low (").append(c.getInt(1)).append(" units left)\n");
            }
            c.close();
            message.append("\n");
        }

        // 2. Overdue bills balance scan
        c = db.rawQuery("SELECT s.invoice_no, c.name, s.remaining_balance FROM sales s JOIN customers c ON s.customer_id = c.id WHERE s.remaining_balance > 0", null);
        if (c != null && c.getCount() > 0) {
            message.append("💳 OUTSTANDING DUES REMINDERS:\n");
            while (c.moveToNext()) {
                message.append("• ").append(c.getString(0)).append(" - ").append(c.getString(1)).append(" owes ").append(confCurrency).append(String.format(Locale.US, "%.2f", c.getDouble(2))).append("\n");
            }
            c.close();
            message.append("\n");
        }

        // 3. Simple daily metric summations
        String today = getTodayString();
        double salesToday = 0.0;
        c = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE date = ?", new String[]{today});
        if (c != null && c.moveToFirst()) {
            salesToday = c.getDouble(0);
            c.close();
        }
        message.append("📈 REALTIME METRICS:\n");
        message.append("• Sales captured today: ").append(confCurrency).append(String.format(Locale.US, "%.2f", salesToday));

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Smart Alert Center");
        b.setMessage(message.toString());
        b.setPositiveButton("Understand", null);
        b.show();
    }

    // ==========================================
    // SETTINGS CONTROL SYSTEM PANEL
    // ==========================================
    private void launchAppSettingsEditor() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Business Manager Configurations");

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(30, 24, 30, 24);

        final EditText etBusName = new EditText(this);
        etBusName.setHint("Business Name");
        etBusName.setText(confBusinessName);

        final EditText etOwnName = new EditText(this);
        etOwnName.setHint("Proprietor Name");
        etOwnName.setText(confOwnerName);

        final EditText etPhoneNo = new EditText(this);
        etPhoneNo.setHint("Corporate Contact Phone");
        etPhoneNo.setText(confPhone);

        final Spinner spinnerCurrencySymbol = new Spinner(this);
        final String[] symbols = {"$", "€", "£", "¥", "₹", "₱", "AED"};
        ArrayAdapter<String> symAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, symbols);
        spinnerCurrencySymbol.setAdapter(symAdapter);
        for (int i = 0; i < symbols.length; i++) {
            if (symbols[i].equals(confCurrency)) {
                spinnerCurrencySymbol.setSelection(i);
                break;
            }
        }

        final EditText etTax = new EditText(this);
        etTax.setHint("General Sales Tax Rate (%)");
        etTax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etTax.setText(String.valueOf(confTaxRate));

        final EditText etInvPrefix = new EditText(this);
        etInvPrefix.setHint("Receipt Prefix");
        etInvPrefix.setText(confInvoicePrefix);

        final Spinner spinnerThemeSelection = new Spinner(this);
        final String[] themes = {"Light Material", "Dark Mode Matrix"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, themes);
        spinnerThemeSelection.setAdapter(themeAdapter);
        spinnerThemeSelection.setSelection(confDarkTheme ? 1 : 0);

        TextView space = new TextView(this);
        space.setText("\nSYSTEM ADMINISTRATION:");
        space.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);

        Button btnPurgeData = new Button(this);
        btnPurgeData.setText("RESET DATABASE");
        btnPurgeData.setBackgroundColor(0xFFD32F2F);
        btnPurgeData.setTextColor(Color.WHITE);
        btnPurgeData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerDangerousPurgeDatabase();
            }
        });

        panel.addView(etBusName);
        panel.addView(etOwnName);
        panel.addView(etPhoneNo);
        panel.addView(spinnerCurrencySymbol);
        panel.addView(etTax);
        panel.addView(etInvPrefix);
        panel.addView(spinnerThemeSelection);
        panel.addView(space);
        panel.addView(btnPurgeData);

        b.setView(panel);

        b.setPositiveButton("Store settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String bName = etBusName.getText().toString().trim();
                String oName = etOwnName.getText().toString().trim();
                String pNo = etPhoneNo.getText().toString().trim();
                String symbol = spinnerCurrencySymbol.getSelectedItem().toString();
                String tRateRaw = etTax.getText().toString().trim();
                String invPre = etInvPrefix.getText().toString().trim();
                boolean dark = (spinnerThemeSelection.getSelectedItemPosition() == 1);

                if (bName.isEmpty() || tRateRaw.isEmpty()) return;

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'business_name'", new Object[]{bName});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'owner_name'", new Object[]{oName});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'phone'", new Object[]{pNo});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'currency'", new Object[]{symbol});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'tax_rate'", new Object[]{tRateRaw});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'invoice_prefix'", new Object[]{invPre});
                    db.execSQL("UPDATE settings SET value = ? WHERE key = 'dark_theme'", new Object[]{dark ? "1" : "0"});

                    db.setTransactionSuccessful();
                    Toast.makeText(MainActivity.this, "Settings stored successfully. Restarting graphics engine.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Settings store failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    db.endTransaction();
                }

                loadAppSettings();
                applyCurrentPalette();
                refreshActiveScreen();
            }
        });
        b.setNegativeButton("Abort", null);
        b.show();
    }

    private void triggerDangerousPurgeDatabase() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("SECURITY CHALLENGE");
        b.setMessage("This operation will delete all products, invoices, customer records, and statistics, reverting this application database back to scratch. Are you absolute sure?");
        b.setPositiveButton("YES, CONFIRM PURGE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                dbHelper.onUpgrade(db, 1, 1);
                loadAppSettings();
                applyCurrentPalette();
                refreshActiveScreen();
                Toast.makeText(MainActivity.this, "Database system purge finished.", Toast.LENGTH_LONG).show();
            }
        });
        b.setNegativeButton("ABORT DATA PURGE", null);
        b.show();
    }

    private void applyCurrentPalette() {
        int backColor, cardColor, textPrimary, textSecondary, inputBack, topBack;

        if (confDarkTheme) {
            backColor = 0xFF121212;
            cardColor = 0xFF1E1E1E;
            textPrimary = 0xFFFFFFFF;
            textSecondary = 0xFFB0BEC5;
            inputBack = 0xFF2C2C2C;
            topBack = 0xFF0D47A1;
        } else {
            backColor = 0xFFF5F5F5;
            cardColor = 0xFFFFFFFF;
            textPrimary = 0xFF212121;
            textSecondary = 0xFF757575;
            inputBack = 0xFFFFFFFF;
            topBack = 0xFF1565C0;
        }

        findViewById(R.id.main_root_layout).setBackgroundColor(backColor);
        findViewById(R.id.top_bar_layout).setBackgroundColor(topBack);
        findViewById(R.id.bottom_nav_bar).setBackgroundColor(cardColor);
        findViewById(R.id.search_bar_layout).setBackgroundColor(confDarkTheme ? 0xFF1C1C1C : 0xFFE1F5FE);

        tvAppTitle.setText(confBusinessName);
        tvAppTitle.setTextColor(Color.WHITE);

        etGlobalSearch.setBackgroundColor(inputBack);
        etGlobalSearch.setTextColor(textPrimary);

        // Subtext titles
        ((TextView) findViewById(R.id.title_financial)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_inventory)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_actions)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_recent)).setTextColor(topBack);

        // Style simple cards
        findViewById(R.id.card_today_sales).setBackgroundColor(cardColor);
        findViewById(R.id.card_today_expenses).setBackgroundColor(cardColor);
        findViewById(R.id.card_profit).setBackgroundColor(cardColor);
        findViewById(R.id.card_unpaid).setBackgroundColor(cardColor);
        findViewById(R.id.card_total_products).setBackgroundColor(cardColor);
        findViewById(R.id.card_low_stock).setBackgroundColor(cardColor);
        findViewById(R.id.layout_recent_transactions).setBackgroundColor(cardColor);

        // Product list styled headers
        ((TextView) findViewById(R.id.title_products_head)).setTextColor(topBack);
        ((TextView) findViewById(R.id.tv_filter_label)).setTextColor(textPrimary);

        // Customer ledger layout
        ((TextView) findViewById(R.id.title_customers_head)).setTextColor(topBack);

        // Sales Checkout items styles
        ((TextView) findViewById(R.id.tv_lbl_sales_cust)).setTextColor(topBack);
        ((TextView) findViewById(R.id.tv_lbl_sales_prod)).setTextColor(topBack);
        ((TextView) findViewById(R.id.tv_lbl_sales_cart)).setTextColor(topBack);
        findViewById(R.id.sales_checkout_panel).setBackgroundColor(cardColor);

        ((TextView) findViewById(R.id.tv_sales_sub_lbl)).setTextColor(textPrimary);
        ((TextView) findViewById(R.id.tv_sales_disc_lbl)).setTextColor(textPrimary);
        ((TextView) findViewById(R.id.tv_sales_tax_lbl)).setTextColor(textPrimary);
        ((TextView) findViewById(R.id.tv_sales_grand_lbl)).setTextColor(textPrimary);
        ((TextView) findViewById(R.id.tv_sales_rec_lbl)).setTextColor(textPrimary);
        ((TextView) findViewById(R.id.tv_sales_bal_lbl)).setTextColor(textPrimary);

        etSalesDiscount.setBackgroundColor(inputBack);
        etSalesDiscount.setTextColor(textPrimary);
        etSalesTaxRate.setBackgroundColor(inputBack);
        etSalesTaxRate.setTextColor(textPrimary);
        etSalesAmountReceived.setBackgroundColor(inputBack);
        etSalesAmountReceived.setTextColor(textPrimary);

        // Expense analytics forms
        ((TextView) findViewById(R.id.title_add_expense)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_chart)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_analytics)).setTextColor(topBack);
        ((TextView) findViewById(R.id.title_expense_history)).setTextColor(topBack);

        findViewById(R.id.expense_form_card).setBackgroundColor(cardColor);
        findViewById(R.id.analytics_card).setBackgroundColor(cardColor);

        etExpenseAmount.setBackgroundColor(inputBack);
        etExpenseAmount.setTextColor(textPrimary);
        etExpenseDesc.setBackgroundColor(inputBack);
        etExpenseDesc.setTextColor(textPrimary);

        tvReportDaily.setTextColor(textPrimary);
        tvReportWeekly.setTextColor(textPrimary);
        tvReportMonthly.setTextColor(textPrimary);
    }

    // ==========================================
    // ONCLICK TRIGGERS MANAGER
    // ==========================================
    @Override
    public void onClick(View v) {
        int id = v.getId();

        // 1. Tab switches
        if (id == R.id.tab_dashboard) {
            changeNavigationTab(0);
        } else if (id == R.id.tab_products) {
            changeNavigationTab(1);
        } else if (id == R.id.tab_customers) {
            changeNavigationTab(2);
        } else if (id == R.id.tab_sales) {
            changeNavigationTab(3);
        } else if (id == R.id.tab_expenses) {
            changeNavigationTab(4);
        }

        // 2. Global search actions
        else if (id == R.id.btn_clear_search) {
            etGlobalSearch.setText("");
            globalQuery = "";
            refreshActiveScreen();
        }

        // 3. Quick buttons trigger
        else if (id == R.id.btn_quick_new_sale) {
            changeNavigationTab(3);
            scrollSalesNew.setVisibility(View.VISIBLE);
            scrollSalesHistory.setVisibility(View.GONE);
            btnSalesTabNew.setBackgroundColor(Color.WHITE);
            btnSalesTabHistory.setBackgroundColor(0xFFCFD8DC);
        } else if (id == R.id.btn_quick_add_product) {
            showProductFormDialog(-1, "", "", "", 0.0, 0.0, 0, 5);
        } else if (id == R.id.btn_quick_add_customer) {
            showCustomerFormDialog(-1, "", "", "");
        } else if (id == R.id.btn_quick_add_expense) {
            changeNavigationTab(4);
        }

        // 4. Products additions
        else if (id == R.id.btn_add_product_main) {
            showProductFormDialog(-1, "", "", "", 0.0, 0.0, 0, 5);
        }

        // 5. Customers additions
        else if (id == R.id.btn_add_customer_main) {
            showCustomerFormDialog(-1, "", "", "");
        }

        // 6. Checkout sub-tabs selections
        else if (id == R.id.btn_sales_tab_new) {
            scrollSalesNew.setVisibility(View.VISIBLE);
            scrollSalesHistory.setVisibility(View.GONE);
            btnSalesTabNew.setBackgroundColor(Color.WHITE);
            btnSalesTabNew.setTextColor(0xFF1565C0);
            btnSalesTabHistory.setBackgroundColor(0xFFCFD8DC);
            btnSalesTabHistory.setTextColor(0xFF555555);
            refreshSalesTab();
        } else if (id == R.id.btn_sales_tab_history) {
            scrollSalesNew.setVisibility(View.GONE);
            scrollSalesHistory.setVisibility(View.VISIBLE);
            btnSalesTabNew.setBackgroundColor(0xFFCFD8DC);
            btnSalesTabNew.setTextColor(0xFF555555);
            btnSalesTabHistory.setBackgroundColor(Color.WHITE);
            btnSalesTabHistory.setTextColor(0xFF1565C0);
            refreshSalesTab();
        }

        // 7. Quantity adjustments
        else if (id == R.id.btn_qty_minus) {
            if (currentQtyCounter > 1) {
                currentQtyCounter--;
                tvSalesQty.setText(String.valueOf(currentQtyCounter));
            }
        } else if (id == R.id.btn_qty_plus) {
            currentQtyCounter++;
            tvSalesQty.setText(String.valueOf(currentQtyCounter));
        }

        // 8. Add item to cart
        else if (id == R.id.btn_add_to_cart) {
            addProductToInvoiceCart();
        }

        // 9. Process sale checkout invoice
        else if (id == R.id.btn_complete_checkout) {
            finalizeSalesTransactionCheckout();
        }

        // 10. Log expenses saving
        else if (id == R.id.btn_save_expense) {
            saveOperationalExpense();
        }

        // 11. Custom header triggers
        else if (id == R.id.btn_notifications) {
            launchNotificationsSummaryDialog();
        } else if (id == R.id.btn_settings_top) {
            launchAppSettingsEditor();
        }
    }
}