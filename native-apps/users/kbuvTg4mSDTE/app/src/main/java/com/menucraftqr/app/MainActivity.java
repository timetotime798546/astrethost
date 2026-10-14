package com.menucraftqr.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private LocalDataManager dataManager;
    private List<FoodItem> foodItemList;
    private List<TableOrder> orderList;

    // Views & Tabs
    private ScrollView viewDashboard;
    private LinearLayout viewMenu;
    private ScrollView viewQr;
    private ScrollView viewPos;

    private Button tabDashboard;
    private Button tabMenu;
    private Button tabQr;
    private Button tabPos;

    // Dashboard widgets
    private TextView tvMetricRevenue;
    private TextView tvMetricOrders;
    private TextView tvMetricActiveTables;
    private TextView tvMetricMenuCount;
    private LinearLayout layoutTableGridContainer;
    private LinearLayout layoutRecentOrdersList;

    // Menu Management widgets
    private EditText etSearchMenu;
    private LinearLayout layoutCategoryTabs;
    private LinearLayout layoutMenuItemsContainer;
    private String selectedCategoryFilter = "All";

    // QR Code widgets
    private Spinner spinnerQrTable;
    private ImageView ivQrCodeDisplay;
    private TextView tvQrTableLabel;
    private Button btnGenerateQr;
    private Button btnSimulateCustomerScan;
    private int selectedQrTableNum = 1;

    // POS Billing widgets
    private Spinner spinnerPosTable;
    private LinearLayout layoutPosCartContainer;
    private Button btnAddItemToPos;
    private TextView tvPosSubtotal;
    private TextView tvPosGst;
    private EditText etPosDiscount;
    private TextView tvPosGrandTotal;
    private Button btnPosSaveOrder;
    private Button btnPosBillAndPrint;
    private int selectedPosTableNum = 1;
    private TableOrder currentPosOrder;

    private DecimalFormat currencyFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = new LocalDataManager(this);
        foodItemList = dataManager.getFoodItems();
        orderList = dataManager.getOrders();

        initViews();
        setupNavigationTabs();
        setupDashboard();
        setupMenuManagement();
        setupQrGenerator();
        setupPosBilling();

        refreshDashboardMetrics();
    }

    private void initViews() {
        viewDashboard = (ScrollView) findViewById(R.id.view_dashboard);
        viewMenu = (LinearLayout) findViewById(R.id.view_menu);
        viewQr = (ScrollView) findViewById(R.id.view_qr);
        viewPos = (ScrollView) findViewById(R.id.view_pos);

        tabDashboard = (Button) findViewById(R.id.tab_dashboard);
        tabMenu = (Button) findViewById(R.id.tab_menu);
        tabQr = (Button) findViewById(R.id.tab_qr);
        tabPos = (Button) findViewById(R.id.tab_pos);

        tvMetricRevenue = (TextView) findViewById(R.id.tv_metric_revenue);
        tvMetricOrders = (TextView) findViewById(R.id.tv_metric_orders);
        tvMetricActiveTables = (TextView) findViewById(R.id.tv_metric_active_tables);
        tvMetricMenuCount = (TextView) findViewById(R.id.tv_metric_menu_count);
        layoutTableGridContainer = (LinearLayout) findViewById(R.id.layout_table_grid_container);
        layoutRecentOrdersList = (LinearLayout) findViewById(R.id.layout_recent_orders_list);

        etSearchMenu = (EditText) findViewById(R.id.et_search_menu);
        layoutCategoryTabs = (LinearLayout) findViewById(R.id.layout_category_tabs);
        layoutMenuItemsContainer = (LinearLayout) findViewById(R.id.layout_menu_items_container);

        spinnerQrTable = (Spinner) findViewById(R.id.spinner_qr_table);
        ivQrCodeDisplay = (ImageView) findViewById(R.id.iv_qr_code_display);
        tvQrTableLabel = (TextView) findViewById(R.id.tv_qr_table_label);
        btnGenerateQr = (Button) findViewById(R.id.btn_generate_qr);
        btnSimulateCustomerScan = (Button) findViewById(R.id.btn_simulate_customer_scan);

        spinnerPosTable = (Spinner) findViewById(R.id.spinner_pos_table);
        layoutPosCartContainer = (LinearLayout) findViewById(R.id.layout_pos_cart_container);
        btnAddItemToPos = (Button) findViewById(R.id.btn_add_item_to_pos);
        tvPosSubtotal = (TextView) findViewById(R.id.tv_pos_subtotal);
        tvPosGst = (TextView) findViewById(R.id.tv_pos_gst);
        etPosDiscount = (EditText) findViewById(R.id.et_pos_discount);
        tvPosGrandTotal = (TextView) findViewById(R.id.tv_pos_grand_total);
        btnPosSaveOrder = (Button) findViewById(R.id.btn_pos_save_order);
        btnPosBillAndPrint = (Button) findViewById(R.id.btn_pos_bill_and_print);

        Button btnQuickNewOrder = (Button) findViewById(R.id.btn_quick_new_order);
        Button btnQuickAddItem = (Button) findViewById(R.id.btn_quick_add_item);
        Button btnOpenAddDishModal = (Button) findViewById(R.id.btn_open_add_dish_modal);

        btnQuickNewOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3); // Go to POS
            }
        });

        btnQuickAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditDishDialog(null);
            }
        });

        btnOpenAddDishModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditDishDialog(null);
            }
        });
    }

    private void setupNavigationTabs() {
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(0); }
        });
        tabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(1); }
        });
        tabQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(2); }
        });
        tabPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(3); }
        });
    }

    private void switchTab(int index) {
        viewDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewMenu.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewQr.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        viewPos.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        int activeColor = Color.parseColor("#E67E22");
        int inactiveColor = Color.parseColor("#7F8C8D");

        tabDashboard.setTextColor(index == 0 ? activeColor : inactiveColor);
        tabMenu.setTextColor(index == 1 ? activeColor : inactiveColor);
        tabQr.setTextColor(index == 2 ? activeColor : inactiveColor);
        tabPos.setTextColor(index == 3 ? activeColor : inactiveColor);

        if (index == 0) refreshDashboardMetrics();
        if (index == 1) renderMenuCatalog();
        if (index == 2) generateQrCodeForTable(selectedQrTableNum);
        if (index == 3) loadPosForTable(selectedPosTableNum);
    }

    // ---------------------------------------------------------------------------------------------
    // MODULE 1: INTERACTIVE RESTAURANT DASHBOARD
    // ---------------------------------------------------------------------------------------------
    private void setupDashboard() {
        renderTableGrid();
        renderRecentOrders();
    }

    private void refreshDashboardMetrics() {
        double revenue = 0.0;
        int activeTablesCount = 0;

        for (TableOrder order : orderList) {
            if ("Paid".equalsIgnoreCase(order.getStatus())) {
                revenue += calculateGrandTotal(order);
            }
        }

        for (int i = 1; i <= 8; i++) {
            if (!"Available".equalsIgnoreCase(dataManager.getTableStatus(i))) {
                activeTablesCount++;
            }
        }

        tvMetricRevenue.setText("₹" + currencyFormat.format(revenue));
        tvMetricOrders.setText(String.valueOf(orderList.size()));
        tvMetricActiveTables.setText(activeTablesCount + " / 8");
        tvMetricMenuCount.setText(String.valueOf(foodItemList.size()));

        renderTableGrid();
        renderRecentOrders();
    }

    private void renderTableGrid() {
        layoutTableGridContainer.removeAllViews();

        for (int row = 0; row < 4; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rowLayout.setPadding(0, 0, 0, 8);

            for (int col = 1; col <= 2; col++) {
                final int tableNumber = (row * 2) + col;
                final String currentStatus = dataManager.getTableStatus(tableNumber);

                LinearLayout tableCard = new LinearLayout(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (col == 1) lp.setMargins(0, 0, 6, 0);
                else lp.setMargins(6, 0, 0, 0);
                tableCard.setLayoutParams(lp);
                tableCard.setOrientation(LinearLayout.VERTICAL);
                tableCard.setBackgroundResource(R.drawable.card_background);
                tableCard.setPadding(12, 12, 12, 12);

                TextView tvTableName = new TextView(this);
                tvTableName.setText("Table " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
                tvTableName.setTextSize(14);
                tvTableName.setTextColor(Color.parseColor("#1A1A1A"));
                tvTableName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvStatus = new TextView(this);
                tvStatus.setText(currentStatus);
                tvStatus.setTextSize(11);
                tvStatus.setPadding(0, 4, 0, 0);

                if ("Available".equalsIgnoreCase(currentStatus)) {
                    tvStatus.setTextColor(Color.parseColor("#27AE60"));
                } else if ("Occupied".equalsIgnoreCase(currentStatus)) {
                    tvStatus.setTextColor(Color.parseColor("#E67E22"));
                } else {
                    tvStatus.setTextColor(Color.parseColor("#C0392B"));
                }

                tableCard.addView(tvTableName);
                tableCard.addView(tvStatus);

                tableCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTableStatusDialog(tableNumber);
                    }
                });

                rowLayout.addView(tableCard);
            }
            layoutTableGridContainer.addView(rowLayout);
        }
    }

    private void showTableStatusDialog(final int tableNumber) {
        final String[] statuses = {"Available", "Occupied", "Billing Pending"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Status for Table " + tableNumber);
        builder.setItems(statuses, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dataManager.setTableStatus(tableNumber, statuses[which]);
                refreshDashboardMetrics();
                Toast.makeText(MainActivity.this, "Table " + tableNumber + " is now " + statuses[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void renderRecentOrders() {
        layoutRecentOrdersList.removeAllViews();
        if (orderList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No orders placed yet.");
            tvEmpty.setTextColor(Color.parseColor("#7F8C8D"));
            tvEmpty.setPadding(8, 8, 8, 8);
            layoutRecentOrdersList.addView(tvEmpty);
            return;
        }

        int count = 0;
        for (int i = orderList.size() - 1; i >= 0 && count < 5; i--, count++) {
            final TableOrder order = orderList.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_background);
            card.setPadding(12, 10, 12, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(lp);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvTitle.setText("Table " + order.getTableNumber() + " (" + order.getOrderId() + ")");
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(Color.parseColor("#1A1A1A"));

            TextView tvTotal = new TextView(this);
            tvTotal.setText("₹" + currencyFormat.format(calculateGrandTotal(order)));
            tvTotal.setTextColor(Color.parseColor("#E67E22"));
            tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);

            header.addView(tvTitle);
            header.addView(tvTotal);

            TextView tvStatus = new TextView(this);
            tvStatus.setText("Status: " + order.getStatus() + " | " + order.getItemsWithQuantities().size() + " items");
            tvStatus.setTextSize(12);
            tvStatus.setTextColor(Color.parseColor("#7F8C8D"));
            tvStatus.setPadding(0, 4, 0, 0);

            card.addView(header);
            card.addView(tvStatus);

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOrderActionsDialog(order);
                }
            });

            layoutRecentOrdersList.addView(card);
        }
    }

    private void showOrderActionsDialog(final TableOrder order) {
        final String[] statusOptions = {"Preparing", "Served", "Paid", "View Digital Invoice"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Order Options (" + order.getOrderId() + ")");
        builder.setItems(statusOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 3) {
                    showInvoiceDialog(order);
                } else {
                    order.setStatus(statusOptions[which]);
                    if ("Paid".equals(statusOptions[which])) {
                        dataManager.setTableStatus(order.getTableNumber(), "Available");
                    }
                    dataManager.saveOrders(orderList);
                    refreshDashboardMetrics();
                }
            }
        });
        builder.show();
    }

    // ---------------------------------------------------------------------------------------------
    // MODULE 2: DIGITAL MENU CARD & INVENTORY MANAGEMENT
    // ---------------------------------------------------------------------------------------------
    private void setupMenuManagement() {
        setupCategoryTabs();

        etSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderMenuCatalog();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        renderMenuCatalog();
    }

    private void setupCategoryTabs() {
        layoutCategoryTabs.removeAllViews();
        String[] categories = {"All", "Starters", "Main Course", "Beverages", "Desserts", "Veg", "Non-Veg"};

        for (final String cat : categories) {
            Button btn = new Button(this);
            btn.setText(cat);
            btn.setTextSize(12);
            btn.setPadding(16, 6, 16, 6);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 6, 0);
            btn.setLayoutParams(lp);

            if (selectedCategoryFilter.equals(cat)) {
                btn.setBackgroundColor(Color.parseColor("#E67E22"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#EDE7DD"));
                btn.setTextColor(Color.parseColor("#1A1A1A"));
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedCategoryFilter = cat;
                    setupCategoryTabs();
                    renderMenuCatalog();
                }
            });

            layoutCategoryTabs.addView(btn);
        }
    }

    private void renderMenuCatalog() {
        layoutMenuItemsContainer.removeAllViews();
        String query = etSearchMenu.getText().toString().trim().toLowerCase(Locale.ROOT);

        for (final FoodItem item : foodItemList) {
            // Apply category filter
            if (!"All".equals(selectedCategoryFilter)) {
                if ("Veg".equals(selectedCategoryFilter) && !item.isVeg()) continue;
                if ("Non-Veg".equals(selectedCategoryFilter) && item.isVeg()) continue;
                if (!"Veg".equals(selectedCategoryFilter) && !"Non-Veg".equals(selectedCategoryFilter)
                        && !item.getCategory().equalsIgnoreCase(selectedCategoryFilter)) {
                    continue;
                }
            }

            // Apply search query
            if (!query.isEmpty()) {
                boolean matchesName = item.getName().toLowerCase(Locale.ROOT).contains(query);
                boolean matchesCategory = item.getCategory().toLowerCase(Locale.ROOT).contains(query);
                if (!matchesName && !matchesCategory) continue;
            }

            // Create Food Item Row Card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_background);
            card.setPadding(14, 12, 14, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 10);
            card.setLayoutParams(lp);

            // Row 1: Veg indicator + Name + Price
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvBadge = new TextView(this);
            tvBadge.setText(item.isVeg() ? "VEG" : "NON-VEG");
            tvBadge.setTextSize(10);
            tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBadge.setPadding(6, 2, 6, 2);
            tvBadge.setBackgroundResource(item.isVeg() ? R.drawable.badge_veg : R.drawable.badge_nonveg);
            tvBadge.setTextColor(item.isVeg() ? Color.parseColor("#27AE60") : Color.parseColor("#C0392B"));

            TextView tvName = new TextView(this);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            nameParams.setMargins(8, 0, 8, 0);
            tvName.setLayoutParams(nameParams);
            tvName.setText(item.getName());
            tvName.setTextColor(Color.parseColor("#1A1A1A"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextSize(15);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("₹" + currencyFormat.format(item.getPrice()));
            tvPrice.setTextColor(Color.parseColor("#E67E22"));
            tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPrice.setTextSize(15);

            row1.addView(tvBadge);
            row1.addView(tvName);
            row1.addView(tvPrice);

            // Description
            TextView tvDesc = new TextView(this);
            tvDesc.setText(item.getDescription());
            tvDesc.setTextColor(Color.parseColor("#7F8C8D"));
            tvDesc.setTextSize(12);
            tvDesc.setPadding(0, 4, 0, 6);

            // Row 2: In-stock Switch + Edit Button
            LinearLayout rowActions = new LinearLayout(this);
            rowActions.setOrientation(LinearLayout.HORIZONTAL);
            rowActions.setGravity(Gravity.CENTER_VERTICAL);

            Switch switchStock = new Switch(this);
            switchStock.setText(item.isInStock() ? "In Stock" : "Out of Stock");
            switchStock.setChecked(item.isInStock());
            switchStock.setTextSize(12);
            switchStock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            switchStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((Switch) v).isChecked();
                    item.setInStock(checked);
                    dataManager.saveFoodItems(foodItemList);
                    ((Switch) v).setText(checked ? "In Stock" : "Out of Stock");
                    Toast.makeText(MainActivity.this, item.getName() + " updated", Toast.LENGTH_SHORT).show();
                }
            });

            Button btnEdit = new Button(this);
            btnEdit.setText("Edit");
            btnEdit.setTextSize(11);
            btnEdit.setPadding(10, 2, 10, 2);
            btnEdit.setBackgroundColor(Color.parseColor("#EFEBE4"));
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddEditDishDialog(item);
                }
            });

            rowActions.addView(switchStock);
            rowActions.addView(btnEdit);

            card.addView(row1);
            card.addView(tvDesc);
            card.addView(rowActions);

            layoutMenuItemsContainer.addView(card);
        }
    }

    private void showAddEditDishDialog(final FoodItem existingItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingItem == null ? "Add Food Item" : "Edit Food Item");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 16, 20, 16);

        final EditText etName = new EditText(this);
        etName.setHint("Item Name (e.g., Paneer Tikka)");
        if (existingItem != null) etName.setText(existingItem.getName());
        layout.addView(etName);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Description");
        if (existingItem != null) etDesc.setText(existingItem.getDescription());
        layout.addView(etDesc);

        final EditText etPrice = new EditText(this);
        etPrice.setHint("Price in ₹");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingItem != null) etPrice.setText(String.valueOf(existingItem.getPrice()));
        layout.addView(etPrice);

        final Spinner spinnerCategory = new Spinner(this);
        String[] cats = {"Starters", "Main Course", "Beverages", "Desserts"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        spinnerCategory.setAdapter(catAdapter);
        if (existingItem != null) {
            for (int i = 0; i < cats.length; i++) {
                if (cats[i].equalsIgnoreCase(existingItem.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(spinnerCategory);

        final CheckBox cbVeg = new CheckBox(this);
        cbVeg.setText("Is Vegetarian Dish");
        cbVeg.setChecked(existingItem == null || existingItem.isVeg());
        layout.addView(cbVeg);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                double price = 0.0;
                try {
                    price = Double.parseDouble(priceStr);
                } catch (Exception e) {}

                if (name.isEmpty() || price <= 0) {
                    Toast.makeText(MainActivity.this, "Please enter valid item name and price", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = spinnerCategory.getSelectedItem().toString();
                boolean isVeg = cbVeg.isChecked();

                if (existingItem != null) {
                    existingItem.setName(name);
                    existingItem.setDescription(desc);
                    existingItem.setPrice(price);
                    existingItem.setCategory(category);
                    existingItem.setVeg(isVeg);
                } else {
                    String id = String.valueOf(System.currentTimeMillis());
                    FoodItem newItem = new FoodItem(id, name, desc, price, category, isVeg, true);
                    foodItemList.add(newItem);
                }

                dataManager.saveFoodItems(foodItemList);
                renderMenuCatalog();
                refreshDashboardMetrics();
                Toast.makeText(MainActivity.this, "Dish saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ---------------------------------------------------------------------------------------------
    // MODULE 3: QR CODE GENERATOR & CUSTOMER SIMULATION
    // ---------------------------------------------------------------------------------------------
    private void setupQrGenerator() {
        List<String> tableOptions = new ArrayList<String>();
        for (int i = 1; i <= 8; i++) {
            tableOptions.add("Table " + (i < 10 ? "0" + i : i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, tableOptions);
        spinnerQrTable.setAdapter(adapter);

        spinnerQrTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedQrTableNum = position + 1;
                generateQrCodeForTable(selectedQrTableNum);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQrCodeForTable(selectedQrTableNum);
                Toast.makeText(MainActivity.this, "QR Code regenerated for Table " + selectedQrTableNum, Toast.LENGTH_SHORT).show();
            }
        });

        btnSimulateCustomerScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomerSimulationDialog(selectedQrTableNum);
            }
        });

        generateQrCodeForTable(1);
    }

    private void generateQrCodeForTable(int tableNumber) {
        String payload = "menucraft://order?table=" + tableNumber + "&time=" + System.currentTimeMillis();
        Bitmap qrBitmap = NativeQrGenerator.generateTableQrBitmap(payload, 450);
        ivQrCodeDisplay.setImageBitmap(qrBitmap);
        tvQrTableLabel.setText("Scan to Order - Table 0" + tableNumber);
    }

    private void showCustomerSimulationDialog(final int tableNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Customer View (Table " + tableNumber + ")");

        ScrollView scrollView = new ScrollView(this);
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 16, 16, 16);

        TextView tvHeader = new TextView(this);
        tvHeader.setText("Welcome! Tap items to add to Table " + tableNumber + " Cart:");
        tvHeader.setTextSize(14);
        tvHeader.setTextColor(Color.parseColor("#1A1A1A"));
        content.addView(tvHeader);

        final Map<String, Integer> customerCart = new HashMap<String, Integer>();

        for (final FoodItem item : foodItemList) {
            if (!item.isInStock()) continue;

            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(0, 10, 0, 10);

            TextView tvItem = new TextView(this);
            tvItem.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvItem.setText((item.isVeg() ? "[V] " : "[NV] ") + item.getName() + " (₹" + currencyFormat.format(item.getPrice()) + ")");
            tvItem.setTextColor(Color.parseColor("#1A1A1A"));

            final Button btnAdd = new Button(this);
            btnAdd.setText("+ Add");
            btnAdd.setTextSize(11);
            btnAdd.setBackgroundColor(Color.parseColor("#E67E22"));
            btnAdd.setTextColor(Color.WHITE);

            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int count = customerCart.containsKey(item.getId()) ? customerCart.get(item.getId()) + 1 : 1;
                    customerCart.put(item.getId(), count);
                    btnAdd.setText("Added (" + count + ")");
                }
            });

            itemRow.addView(tvItem);
            itemRow.addView(btnAdd);
            content.addView(itemRow);
        }

        scrollView.addView(content);
        builder.setView(scrollView);

        builder.setPositiveButton("Place Order", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (customerCart.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No items selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                TableOrder order = new TableOrder("ORD-" + System.currentTimeMillis() % 10000, tableNumber);
                for (Map.Entry<String, Integer> entry : customerCart.entrySet()) {
                    order.addItem(entry.getKey(), entry.getValue());
                }

                orderList.add(order);
                dataManager.saveOrders(orderList);
                dataManager.setTableStatus(tableNumber, "Occupied");
                refreshDashboardMetrics();

                Toast.makeText(MainActivity.this, "Order received from Table " + tableNumber + "!", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // ---------------------------------------------------------------------------------------------
    // MODULE 4: TABLE POS & ORDER BILLING
    // ---------------------------------------------------------------------------------------------
    private void setupPosBilling() {
        List<String> tableOptions = new ArrayList<String>();
        for (int i = 1; i <= 8; i++) {
            tableOptions.add("Table " + (i < 10 ? "0" + i : i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, tableOptions);
        spinnerPosTable.setAdapter(adapter);

        spinnerPosTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosTableNum = position + 1;
                loadPosForTable(selectedPosTableNum);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddItemToPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemToOrderDialog();
            }
        });

        etPosDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentPosOrder != null) {
                    try {
                        double disc = Double.parseDouble(s.toString());
                        currentPosOrder.setDiscount(disc);
                    } catch (Exception e) {
                        currentPosOrder.setDiscount(0.0);
                    }
                    updatePosCalculations();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPosSaveOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPosOrder != null) {
                    dataManager.saveOrders(orderList);
                    dataManager.setTableStatus(selectedPosTableNum, "Occupied");
                    refreshDashboardMetrics();
                    Toast.makeText(MainActivity.this, "Order sent to kitchen!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPosBillAndPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPosOrder != null) {
                    currentPosOrder.setStatus("Paid");
                    dataManager.saveOrders(orderList);
                    dataManager.setTableStatus(selectedPosTableNum, "Available");
                    refreshDashboardMetrics();
                    showInvoiceDialog(currentPosOrder);
                }
            }
        });
    }

    private void loadPosForTable(int tableNumber) {
        currentPosOrder = null;
        // Search active order for this table
        for (int i = orderList.size() - 1; i >= 0; i--) {
            TableOrder o = orderList.get(i);
            if (o.getTableNumber() == tableNumber && !"Paid".equalsIgnoreCase(o.getStatus())) {
                currentPosOrder = o;
                break;
            }
        }

        // If no active order, create a fresh one
        if (currentPosOrder == null) {
            currentPosOrder = new TableOrder("ORD-" + (System.currentTimeMillis() % 10000), tableNumber);
            orderList.add(currentPosOrder);
        }

        renderPosCartItems();
        updatePosCalculations();
    }

    private void renderPosCartItems() {
        layoutPosCartContainer.removeAllViews();

        if (currentPosOrder.getItemsWithQuantities().isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No items added yet. Click '+ Add Item' below.");
            tvEmpty.setTextColor(Color.parseColor("#7F8C8D"));
            tvEmpty.setPadding(8, 8, 8, 8);
            layoutPosCartContainer.addView(tvEmpty);
            return;
        }

        for (Map.Entry<String, Integer> entry : currentPosOrder.getItemsWithQuantities().entrySet()) {
            final String itemId = entry.getKey();
            final int qty = entry.getValue();
            final FoodItem item = findFoodItemById(itemId);
            if (item == null) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 6, 0, 6);

            TextView tvItemInfo = new TextView(this);
            tvItemInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvItemInfo.setText(item.getName() + " (₹" + currencyFormat.format(item.getPrice()) + ")");
            tvItemInfo.setTextColor(Color.parseColor("#1A1A1A"));

            Button btnMinus = new Button(this);
            btnMinus.setText("-");
            btnMinus.setPadding(4, 2, 4, 2);
            btnMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentPosOrder.addItem(itemId, -1);
                    renderPosCartItems();
                    updatePosCalculations();
                }
            });

            TextView tvQty = new TextView(this);
            tvQty.setText(" " + qty + " ");
            tvQty.setTypeface(null, android.graphics.Typeface.BOLD);

            Button btnPlus = new Button(this);
            btnPlus.setText("+");
            btnPlus.setPadding(4, 2, 4, 2);
            btnPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentPosOrder.addItem(itemId, 1);
                    renderPosCartItems();
                    updatePosCalculations();
                }
            });

            TextView tvTotal = new TextView(this);
            tvTotal.setText(" ₹" + currencyFormat.format(item.getPrice() * qty));
            tvTotal.setTextColor(Color.parseColor("#E67E22"));
            tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvItemInfo);
            row.addView(btnMinus);
            row.addView(tvQty);
            row.addView(btnPlus);
            row.addView(tvTotal);

            layoutPosCartContainer.addView(row);
        }
    }

    private void showAddItemToOrderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Dish to Add");

        final List<FoodItem> availableItems = new ArrayList<FoodItem>();
        List<String> names = new ArrayList<String>();
        for (FoodItem item : foodItemList) {
            if (item.isInStock()) {
                availableItems.add(item);
                names.add(item.getName() + " - ₹" + currencyFormat.format(item.getPrice()));
            }
        }

        builder.setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FoodItem selected = availableItems.get(which);
                currentPosOrder.addItem(selected.getId(), 1);
                renderPosCartItems();
                updatePosCalculations();
            }
        });
        builder.show();
    }

    private void updatePosCalculations() {
        if (currentPosOrder == null) return;

        double subtotal = calculateSubtotal(currentPosOrder);
        double gst = subtotal * 0.05; // 5% GST
        double discount = currentPosOrder.getDiscount();
        double grandTotal = Math.max(0.0, (subtotal + gst) - discount);

        tvPosSubtotal.setText("₹" + currencyFormat.format(subtotal));
        tvPosGst.setText("₹" + currencyFormat.format(gst));
        tvPosGrandTotal.setText("₹" + currencyFormat.format(grandTotal));
    }

    private double calculateSubtotal(TableOrder order) {
        double subtotal = 0.0;
        for (Map.Entry<String, Integer> entry : order.getItemsWithQuantities().entrySet()) {
            FoodItem item = findFoodItemById(entry.getKey());
            if (item != null) {
                subtotal += (item.getPrice() * entry.getValue());
            }
        }
        return subtotal;
    }

    private double calculateGrandTotal(TableOrder order) {
        double subtotal = calculateSubtotal(order);
        double gst = subtotal * 0.05;
        return Math.max(0.0, (subtotal + gst) - order.getDiscount());
    }

    private FoodItem findFoodItemById(String id) {
        for (FoodItem item : foodItemList) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }

    private void showInvoiceDialog(final TableOrder order) {
        double subtotal = calculateSubtotal(order);
        double gst = subtotal * 0.05;
        double discount = order.getDiscount();
        double grandTotal = Math.max(0.0, (subtotal + gst) - discount);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String dateFormatted = sdf.format(new Date(order.getTimestamp()));

        final StringBuilder invoice = new StringBuilder();
        invoice.append("================================\n");
        invoice.append("         MENUCRAFT QR           \n");
        invoice.append("     Smart Dining & Billing     \n");
        invoice.append("================================\n");
        invoice.append("Invoice: ").append(order.getOrderId()).append("\n");
        invoice.append("Table:   Table 0").append(order.getTableNumber()).append("\n");
        invoice.append("Date:    ").append(dateFormatted).append("\n");
        invoice.append("Status:  PAID\n");
        invoice.append("--------------------------------\n");
        invoice.append(String.format(Locale.getDefault(), "%-18s %3s %9s\n", "Item", "Qty", "Price"));
        invoice.append("--------------------------------\n");

        for (Map.Entry<String, Integer> entry : order.getItemsWithQuantities().entrySet()) {
            FoodItem item = findFoodItemById(entry.getKey());
            if (item != null) {
                double lineTotal = item.getPrice() * entry.getValue();
                String name = item.getName();
                if (name.length() > 18) name = name.substring(0, 15) + "...";
                invoice.append(String.format(Locale.getDefault(), "%-18s %3d ₹%8.2f\n", name, entry.getValue(), lineTotal));
            }
        }

        invoice.append("--------------------------------\n");
        invoice.append(String.format(Locale.getDefault(), "Subtotal:             ₹%9.2f\n", subtotal));
        invoice.append(String.format(Locale.getDefault(), "GST (5%%):             ₹%9.2f\n", gst));
        invoice.append(String.format(Locale.getDefault(), "Discount:            -₹%9.2f\n", discount));
        invoice.append("--------------------------------\n");
        invoice.append(String.format(Locale.getDefault(), "GRAND TOTAL:          ₹%9.2f\n", grandTotal));
        invoice.append("================================\n");
        invoice.append("       Thank You! Visit Again   \n");
        invoice.append("================================\n");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Customer Invoice Receipt");

        TextView tvInvoice = new TextView(this);
        tvInvoice.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvInvoice.setText(invoice.toString());
        tvInvoice.setPadding(30, 20, 30, 20);
        tvInvoice.setTextColor(Color.parseColor("#1A1A1A"));
        tvInvoice.setTextSize(12);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(tvInvoice);
        builder.setView(scroll);

        builder.setPositiveButton("Share Receipt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "MenuCraft QR Invoice - " + order.getOrderId());
                shareIntent.putExtra(Intent.EXTRA_TEXT, invoice.toString());
                startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
            }
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }
}