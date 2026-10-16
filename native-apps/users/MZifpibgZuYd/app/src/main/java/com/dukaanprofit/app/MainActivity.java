package com.dukaanprofit.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dukaanprofit.app.db.DatabaseHelper;
import com.dukaanprofit.app.models.Customer;
import com.dukaanprofit.app.models.Expense;
import com.dukaanprofit.app.models.Product;
import com.dukaanprofit.app.models.Sale;
import com.dukaanprofit.app.utils.ProfitCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Containers
    private View viewDashboard;
    private View viewProducts;
    private View viewBilling;
    private View viewUdhaar;
    private View viewReports;

    // Bottom Navigation Buttons & Labels
    private TextView tvNavDashboard, tvNavProducts, tvNavBilling, tvNavUdhaar, tvNavReports;

    // Dashboard UI
    private TextView tvDashSales, tvDashExpenses, tvDashGrossProfit, tvDashNetProfit, tvDashUdhaar, tvLowStockStatus;
    private LinearLayout layoutLowStockBox, layoutRecentSales;
    private TextView tvNoSales;

    // Products UI
    private EditText etSearchProduct;
    private ListView lvProducts;
    private TextView tvInventoryValuation;
    private List<Product> productList = new ArrayList<Product>();
    private ArrayAdapter<Product> productAdapter;

    // Billing / POS UI
    private Spinner spBillingCustomer, spBillingProducts;
    private EditText etBillingQty, etBillingDiscount;
    private LinearLayout layoutCartItems;
    private TextView tvCartEmpty, tvCartSubtotal, tvCartGrandTotal;
    private List<DatabaseHelper.CartItem> currentCart = new ArrayList<DatabaseHelper.CartItem>();
    private List<Customer> customerList = new ArrayList<Customer>();
    private String lastGeneratedReceipt = "";

    // Udhaar UI
    private TextView tvUdhaarHeader;
    private ListView lvCustomers;
    private ArrayAdapter<Customer> customerAdapter;

    // Reports UI
    private TextView tvRepTotalSales, tvRepCogs, tvRepGrossProfit, tvRepTotalExpenses, tvRepNetProfit, tvNoExpenses;
    private LinearLayout layoutExpenseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupNavigation();
        setupDashboard();
        setupProductsSection();
        setupBillingSection();
        setupUdhaarSection();
        setupReportsSection();

        // Default to dashboard
        showView(0);
        refreshAllData();
    }

    private void initViews() {
        viewDashboard = findViewById(R.id.view_dashboard);
        viewProducts = findViewById(R.id.view_products);
        viewBilling = findViewById(R.id.view_billing);
        viewUdhaar = findViewById(R.id.view_udhaar);
        viewReports = findViewById(R.id.view_reports);

        tvNavDashboard = findViewById(R.id.tv_nav_dashboard);
        tvNavProducts = findViewById(R.id.tv_nav_products);
        tvNavBilling = findViewById(R.id.tv_nav_billing);
        tvNavUdhaar = findViewById(R.id.tv_nav_udhaar);
        tvNavReports = findViewById(R.id.tv_nav_reports);

        // Quick Bill Header Button
        findViewById(R.id.btn_quick_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(2); // open billing view
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.nav_btn_dashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(0);
            }
        });
        findViewById(R.id.nav_btn_products).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(1);
            }
        });
        findViewById(R.id.nav_btn_billing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(2);
            }
        });
        findViewById(R.id.nav_btn_udhaar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(3);
            }
        });
        findViewById(R.id.nav_btn_reports).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(4);
            }
        });
    }

    private void showView(int index) {
        viewDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewProducts.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewBilling.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        viewUdhaar.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        viewReports.setVisibility(index == 4 ? View.VISIBLE : View.GONE);

        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = getResources().getColor(R.color.nav_inactive);

        tvNavDashboard.setTextColor(index == 0 ? activeColor : inactiveColor);
        tvNavProducts.setTextColor(index == 1 ? activeColor : inactiveColor);
        tvNavBilling.setTextColor(index == 2 ? activeColor : inactiveColor);
        tvNavUdhaar.setTextColor(index == 3 ? activeColor : inactiveColor);
        tvNavReports.setTextColor(index == 4 ? activeColor : inactiveColor);

        refreshAllData();
    }

    // --- 1. Dashboard Logic ---
    private void setupDashboard() {
        tvDashSales = findViewById(R.id.tv_dash_sales);
        tvDashExpenses = findViewById(R.id.tv_dash_expenses);
        tvDashGrossProfit = findViewById(R.id.tv_dash_gross_profit);
        tvDashNetProfit = findViewById(R.id.tv_dash_net_profit);
        tvDashUdhaar = findViewById(R.id.tv_dash_udhaar);
        tvLowStockStatus = findViewById(R.id.tv_low_stock_status);
        layoutLowStockBox = findViewById(R.id.layout_low_stock_box);
        layoutRecentSales = findViewById(R.id.layout_recent_sales);
        tvNoSales = findViewById(R.id.tv_no_sales);

        findViewById(R.id.btn_dash_view_udhaar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(3);
            }
        });
    }

    private void refreshDashboard() {
        List<Sale> sales = dbHelper.getAllSales();
        List<Expense> expenses = dbHelper.getAllExpenses();
        double totalSales = 0.0;
        double totalCogs = 0.0;

        for (Sale s : sales) {
            totalSales += s.getFinalAmount();
            totalCogs += s.getTotalCost();
        }

        double totalExpenses = 0.0;
        for (Expense e : expenses) {
            totalExpenses += e.getAmount();
        }

        double grossProfit = ProfitCalculator.calculateGrossProfit(totalSales, totalCogs);
        double netProfit = ProfitCalculator.calculateNetProfit(grossProfit, totalExpenses);
        double udhaar = dbHelper.getTotalUdhaarOutstanding();

        tvDashSales.setText("₹" + String.format(Locale.getDefault(), "%.2f", totalSales));
        tvDashExpenses.setText("₹" + String.format(Locale.getDefault(), "%.2f", totalExpenses));
        tvDashGrossProfit.setText("₹" + String.format(Locale.getDefault(), "%.2f", grossProfit));
        tvDashNetProfit.setText("₹" + String.format(Locale.getDefault(), "%.2f", netProfit));
        tvDashUdhaar.setText("₹" + String.format(Locale.getDefault(), "%.2f", udhaar) + " Pending Collection");

        // Low stock warning alerts
        List<Product> lowStock = dbHelper.getLowStockProducts();
        layoutLowStockBox.removeAllViews();
        if (lowStock.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("All inventory levels are healthy.");
            tv.setTextColor(getResources().getColor(R.color.text_muted));
            layoutLowStockBox.addView(tv);
        } else {
            for (Product p : lowStock) {
                TextView tv = new TextView(this);
                tv.setText("⚠️ " + p.getName() + " - Only " + p.getQuantity() + " units left (Min: " + p.getLowStockThreshold() + ")");
                tv.setTextColor(getResources().getColor(R.color.danger));
                tv.setTextSize(13);
                tv.setPadding(0, 4, 0, 4);
                layoutLowStockBox.addView(tv);
            }
        }

        // Recent Transactions
        layoutRecentSales.removeAllViews();
        if (sales.isEmpty()) {
            layoutRecentSales.addView(tvNoSales);
        } else {
            int count = Math.min(sales.size(), 5);
            for (int i = 0; i < count; i++) {
                Sale s = sales.get(i);
                TextView tv = new TextView(this);
                tv.setText(s.getCreatedAt() + " | #" + s.getId() + " - " + s.getCustomerName() + " | ₹" + String.format(Locale.getDefault(), "%.2f", s.getFinalAmount()) + " [" + s.getPaymentType() + "]");
                tv.setTextColor(getResources().getColor(R.color.text_main));
                tv.setPadding(0, 6, 0, 6);
                layoutRecentSales.addView(tv);
            }
        }
    }

    // --- 2. Products / Inventory Logic ---
    private void setupProductsSection() {
        etSearchProduct = findViewById(R.id.et_search_product);
        lvProducts = findViewById(R.id.lv_products);
        tvInventoryValuation = findViewById(R.id.tv_inventory_valuation);

        productAdapter = new ArrayAdapter<Product>(this, R.layout.item_product, productList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_product, parent, false);
                }
                Product p = getItem(position);
                if (p != null) {
                    TextView tvName = convertView.findViewById(R.id.item_prod_name);
                    TextView tvSku = convertView.findViewById(R.id.item_prod_sku);
                    TextView tvPricing = convertView.findViewById(R.id.item_prod_pricing);
                    TextView tvStock = convertView.findViewById(R.id.item_prod_stock);
                    TextView tvAlert = convertView.findViewById(R.id.item_prod_alert);

                    tvName.setText(p.getName());
                    tvSku.setText("SKU: " + p.getSku());
                    tvPricing.setText("Sell: ₹" + String.format(Locale.getDefault(), "%.2f", p.getSellPrice()) + " | Cost: ₹" + String.format(Locale.getDefault(), "%.2f", p.getCostPrice()));
                    tvStock.setText("Stock: " + p.getQuantity());

                    if (p.isLowStock()) {
                        tvAlert.setVisibility(View.VISIBLE);
                        tvStock.setTextColor(getContext().getResources().getColor(R.color.danger));
                    } else {
                        tvAlert.setVisibility(View.GONE);
                        tvStock.setTextColor(getContext().getResources().getColor(R.color.text_main));
                    }
                }
                return convertView;
            }
        };
        lvProducts.setAdapter(productAdapter);

        etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                refreshProducts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_add_product).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProductDialog(null);
            }
        });

        lvProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product selected = productList.get(position);
                showProductOptionsDialog(selected);
            }
        });
    }

    private void refreshProducts(String search) {
        productList.clear();
        productList.addAll(dbHelper.getAllProducts(search));
        productAdapter.notifyDataSetChanged();

        double totalCost = 0.0;
        double totalRetail = 0.0;
        for (Product p : productList) {
            totalCost += p.getInventoryCostValue();
            totalRetail += p.getInventoryRetailValue();
        }
        tvInventoryValuation.setText("Stock Value: ₹" + String.format(Locale.getDefault(), "%.2f", totalCost) + " (Cost) | ₹" + String.format(Locale.getDefault(), "%.2f", totalRetail) + " (Retail Potential)");
    }

    private void showProductOptionsDialog(final Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(product.getName());
        builder.setItems(new CharSequence[]{"Edit Product", "Delete Product"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showAddProductDialog(product);
                } else {
                    confirmDeleteProduct(product);
                }
            }
        });
        builder.show();
    }

    private void confirmDeleteProduct(final Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '" + product.getName() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteProduct(product.getId());
                        refreshAllData();
                        Toast.makeText(MainActivity.this, "Product deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddProductDialog(final Product existingProduct) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingProduct == null ? "Add New Product" : "Edit Product");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Product Name");
        if (existingProduct != null) etName.setText(existingProduct.getName());
        layout.addView(etName);

        final EditText etSku = new EditText(this);
        etSku.setHint("SKU / Barcode");
        if (existingProduct != null) etSku.setText(existingProduct.getSku());
        layout.addView(etSku);

        final EditText etCost = new EditText(this);
        etCost.setHint("Purchase / Cost Price (₹)");
        etCost.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingProduct != null) etCost.setText(String.valueOf(existingProduct.getCostPrice()));
        layout.addView(etCost);

        final EditText etSell = new EditText(this);
        etSell.setHint("Selling Price (₹)");
        etSell.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingProduct != null) etSell.setText(String.valueOf(existingProduct.getSellPrice()));
        layout.addView(etSell);

        final EditText etQty = new EditText(this);
        etQty.setHint("Available Quantity");
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (existingProduct != null) etQty.setText(String.valueOf(existingProduct.getQuantity()));
        layout.addView(etQty);

        final EditText etThreshold = new EditText(this);
        etThreshold.setHint("Low Stock Alert Threshold");
        etThreshold.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etThreshold.setText(existingProduct != null ? String.valueOf(existingProduct.getLowStockThreshold()) : "5");
        layout.addView(etThreshold);

        builder.setView(layout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String sku = etSku.getText().toString().trim();
                String costStr = etCost.getText().toString().trim();
                String sellStr = etSell.getText().toString().trim();
                String qtyStr = etQty.getText().toString().trim();
                String thresStr = etThreshold.getText().toString().trim();

                if (name.isEmpty() || costStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double cost = Double.parseDouble(costStr);
                double sell = Double.parseDouble(sellStr);
                int qty = Integer.parseInt(qtyStr);
                int threshold = thresStr.isEmpty() ? 5 : Integer.parseInt(thresStr);

                if (existingProduct == null) {
                    dbHelper.insertProduct(name, sku, cost, sell, qty, threshold);
                } else {
                    dbHelper.updateProduct(existingProduct.getId(), name, sku, cost, sell, qty, threshold);
                }
                refreshAllData();
                Toast.makeText(MainActivity.this, "Product saved successfully", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- 3. Fast Billing & POS Logic ---
    private void setupBillingSection() {
        spBillingCustomer = findViewById(R.id.sp_billing_customer);
        spBillingProducts = findViewById(R.id.sp_billing_products);
        etBillingQty = findViewById(R.id.et_billing_qty);
        etBillingDiscount = findViewById(R.id.et_billing_discount);
        layoutCartItems = findViewById(R.id.layout_cart_items);
        tvCartEmpty = findViewById(R.id.tv_cart_empty);
        tvCartSubtotal = findViewById(R.id.tv_cart_subtotal);
        tvCartGrandTotal = findViewById(R.id.tv_cart_grand_total);

        findViewById(R.id.btn_cart_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToCart();
            }
        });

        etBillingDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                recalculateCart();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_pay_cash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCheckout("CASH");
            }
        });

        findViewById(R.id.btn_pay_credit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCheckout("CREDIT");
            }
        });

        findViewById(R.id.btn_share_last_receipt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareReceipt();
            }
        });
    }

    private void refreshBillingSpinners() {
        // Customer Spinner
        customerList = dbHelper.getAllCustomers();
        List<String> custNames = new ArrayList<String>();
        custNames.add("Walk-in Cash Customer");
        for (Customer c : customerList) {
            custNames.add(c.getName() + " (Balance: ₹" + String.format(Locale.getDefault(), "%.2f", c.getOutstandingBalance()) + ")");
        }
        ArrayAdapter<String> custAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, custNames);
        spBillingCustomer.setAdapter(custAdapter);

        // Product Spinner
        List<Product> availableProducts = dbHelper.getAllProducts("");
        ArrayAdapter<Product> prodAdapter = new ArrayAdapter<Product>(this, android.R.layout.simple_spinner_dropdown_item, availableProducts);
        spBillingProducts.setAdapter(prodAdapter);
    }

    private void addItemToCart() {
        Product selected = (Product) spBillingProducts.getSelectedItem();
        String qtyStr = etBillingQty.getText().toString().trim();
        if (selected == null) {
            Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter valid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        if (qty <= 0) {
            Toast.makeText(this, "Quantity must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > selected.getQuantity()) {
            Toast.makeText(this, "Stock insufficient! Only " + selected.getQuantity() + " available.", Toast.LENGTH_LONG).show();
            return;
        }

        // Add or update in cart
        boolean found = false;
        for (DatabaseHelper.CartItem item : currentCart) {
            if (item.product.getId() == selected.getId()) {
                if (item.quantity + qty > selected.getQuantity()) {
                    Toast.makeText(this, "Total exceeds available stock (" + selected.getQuantity() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.quantity += qty;
                found = true;
                break;
            }
        }
        if (!found) {
            currentCart.add(new DatabaseHelper.CartItem(selected, qty));
        }

        etBillingQty.setText("");
        renderCartUI();
    }

    private void renderCartUI() {
        layoutCartItems.removeAllViews();
        if (currentCart.isEmpty()) {
            layoutCartItems.addView(tvCartEmpty);
        } else {
            for (int i = 0; i < currentCart.size(); i++) {
                final int index = i;
                final DatabaseHelper.CartItem item = currentCart.get(i);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);

                TextView tvDesc = new TextView(this);
                tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tvDesc.setText(item.product.getName() + " x" + item.quantity + " @ ₹" + String.format(Locale.getDefault(), "%.2f", item.product.getSellPrice()));
                tvDesc.setTextColor(getResources().getColor(R.color.text_main));
                row.addView(tvDesc);

                TextView tvTotal = new TextView(this);
                tvTotal.setText("₹" + String.format(Locale.getDefault(), "%.2f", item.getLineTotal()));
                tvTotal.setTextColor(getResources().getColor(R.color.primary));
                tvTotal.setPadding(10, 0, 10, 0);
                row.addView(tvTotal);

                Button btnRemove = new Button(this);
                btnRemove.setText("X");
                btnRemove.setTextSize(10);
                btnRemove.setTextColor(getResources().getColor(R.color.danger));
                btnRemove.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
                btnRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentCart.remove(index);
                        renderCartUI();
                    }
                });
                row.addView(btnRemove);

                layoutCartItems.addView(row);
            }
        }
        recalculateCart();
    }

    private void recalculateCart() {
        double subtotal = 0.0;
        for (DatabaseHelper.CartItem item : currentCart) {
            subtotal += item.getLineTotal();
        }

        String discStr = etBillingDiscount.getText().toString().trim();
        double discount = 0.0;
        if (!discStr.isEmpty()) {
            try {
                discount = Double.parseDouble(discStr);
            } catch (Exception ignored) {}
        }

        double grandTotal = subtotal - discount;
        if (grandTotal < 0) grandTotal = 0;

        tvCartSubtotal.setText("₹" + String.format(Locale.getDefault(), "%.2f", subtotal));
        tvCartGrandTotal.setText("₹" + String.format(Locale.getDefault(), "%.2f", grandTotal));
    }

    private void processCheckout(String paymentType) {
        if (currentCart.isEmpty()) {
            Toast.makeText(this, "Please add items to cart before completing sale", Toast.LENGTH_SHORT).show();
            return;
        }

        int custPos = spBillingCustomer.getSelectedItemPosition();
        long customerId = 0;
        String customerName = "Walk-in Cash";

        if (custPos > 0 && custPos - 1 < customerList.size()) {
            Customer c = customerList.get(custPos - 1);
            customerId = c.getId();
            customerName = c.getName();
        }

        if ("CREDIT".equalsIgnoreCase(paymentType) && customerId == 0) {
            Toast.makeText(this, "Udhaar (Credit) requires selecting a registered customer!", Toast.LENGTH_LONG).show();
            return;
        }

        String discStr = etBillingDiscount.getText().toString().trim();
        double discount = 0.0;
        if (!discStr.isEmpty()) {
            try {
                discount = Double.parseDouble(discStr);
            } catch (Exception ignored) {}
        }

        boolean success = dbHelper.executeSale(customerId, customerName, currentCart, discount, paymentType);
        if (success) {
            StringBuilder receipt = new StringBuilder();
            receipt.append("🧾 *DukaanProfit Digital Bill*\n");
            receipt.append("Date: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())).append("\n");
            receipt.append("Customer: ").append(customerName).append("\n");
            receipt.append("Payment Mode: ").append(paymentType).append("\n");
            receipt.append("---------------------------------\n");
            for (DatabaseHelper.CartItem it : currentCart) {
                receipt.append(it.product.getName()).append(" x").append(it.quantity)
                        .append(" = ₹").append(String.format(Locale.getDefault(), "%.2f", it.getLineTotal())).append("\n");
            }
            receipt.append("---------------------------------\n");
            receipt.append("Discount: ₹").append(String.format(Locale.getDefault(), "%.2f", discount)).append("\n");
            receipt.append("*Total Amount: ").append(tvCartGrandTotal.getText().toString()).append("*\n");
            receipt.append("Thank you for your business!");
            lastGeneratedReceipt = receipt.toString();

            currentCart.clear();
            renderCartUI();
            refreshAllData();

            Toast.makeText(this, "Sale completed successfully!", Toast.LENGTH_LONG).show();
            shareReceipt();
        } else {
            Toast.makeText(this, "Failed to record sale. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReceipt() {
        if (lastGeneratedReceipt == null || lastGeneratedReceipt.isEmpty()) {
            Toast.makeText(this, "No recent bill to share", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Dukaan Receipt");
        shareIntent.putExtra(Intent.EXTRA_TEXT, lastGeneratedReceipt);
        startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
    }

    // --- 4. Udhaar Customer Ledger Logic ---
    private void setupUdhaarSection() {
        tvUdhaarHeader = findViewById(R.id.tv_udhaar_header);
        lvCustomers = findViewById(R.id.lv_customers);

        customerAdapter = new ArrayAdapter<Customer>(this, R.layout.item_customer, customerList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_customer, parent, false);
                }
                Customer c = getItem(position);
                if (c != null) {
                    TextView tvName = convertView.findViewById(R.id.cust_name);
                    TextView tvPhone = convertView.findViewById(R.id.cust_phone);
                    TextView tvBal = convertView.findViewById(R.id.cust_balance);

                    tvName.setText(c.getName());
                    tvPhone.setText(c.getPhone() != null && !c.getPhone().isEmpty() ? "Phone: " + c.getPhone() : "No Phone");
                    tvBal.setText("Due: ₹" + String.format(Locale.getDefault(), "%.2f", c.getOutstandingBalance()));
                }
                return convertView;
            }
        };
        lvCustomers.setAdapter(customerAdapter);

        findViewById(R.id.btn_add_customer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomerDialog();
            }
        });

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer c = customerList.get(position);
                showCustomerLedgerDialog(c);
            }
        });
    }

    private void refreshUdhaar() {
        customerList.clear();
        customerList.addAll(dbHelper.getAllCustomers());
        customerAdapter.notifyDataSetChanged();

        double totalDue = dbHelper.getTotalUdhaarOutstanding();
        tvUdhaarHeader.setText("Total Customer Dues: ₹" + String.format(Locale.getDefault(), "%.2f", totalDue));
    }

    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Customer");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Customer Full Name");
        layout.addView(etName);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Phone Number (Optional)");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        builder.setView(layout);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Customer name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHelper.insertCustomer(name, phone);
                refreshAllData();
                Toast.makeText(MainActivity.this, "Customer added to ledger", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCustomerLedgerDialog(final Customer customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(customer.getName() + " (Balance: ₹" + String.format(Locale.getDefault(), "%.2f", customer.getOutstandingBalance()) + ")");

        List<String> history = dbHelper.getCustomerLedgerHistory(customer.getId());
        final CharSequence[] items = history.toArray(new CharSequence[0]);

        builder.setItems(items, null);
        builder.setPositiveButton("Record Payment Received", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showRecordPaymentDialog(customer);
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showRecordPaymentDialog(final Customer customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Payment from " + customer.getName());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Amount Received (₹)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etNotes = new EditText(this);
        etNotes.setHint("Notes (e.g. UPI / Cash settlement)");
        layout.addView(etNotes);

        builder.setView(layout);
        builder.setPositiveButton("Save Payment", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String amtStr = etAmount.getText().toString().trim();
                String notes = etNotes.getText().toString().trim();
                if (amtStr.isEmpty()) return;
                double amount = Double.parseDouble(amtStr);
                boolean done = dbHelper.recordCustomerPayment(customer.getId(), amount, notes.isEmpty() ? "Cash Received" : notes);
                if (done) {
                    refreshAllData();
                    Toast.makeText(MainActivity.this, "Payment recorded! Customer balance updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to record payment", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- 5. Expenses & Reports Logic ---
    private void setupReportsSection() {
        tvRepTotalSales = findViewById(R.id.tv_rep_total_sales);
        tvRepCogs = findViewById(R.id.tv_rep_cogs);
        tvRepGrossProfit = findViewById(R.id.tv_rep_gross_profit);
        tvRepTotalExpenses = findViewById(R.id.tv_rep_total_expenses);
        tvRepNetProfit = findViewById(R.id.tv_rep_net_profit);
        tvNoExpenses = findViewById(R.id.tv_no_expenses);
        layoutExpenseList = findViewById(R.id.layout_expense_list);

        findViewById(R.id.btn_add_expense).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddExpenseDialog();
            }
        });

        findViewById(R.id.btn_export_csv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportReportCsv();
            }
        });
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Business Expense");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final Spinner spCat = new Spinner(this);
        String[] categories = new String[]{"Shop Rent", "Electricity / Utilities", "Staff Wages", "Transport / Freight", "Packaging & Supplies", "Miscellaneous"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCat.setAdapter(catAdapter);
        layout.addView(spCat);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Expense Amount (₹)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etNotes = new EditText(this);
        etNotes.setHint("Notes / Reference");
        layout.addView(etNotes);

        builder.setView(layout);
        builder.setPositiveButton("Record", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String amtStr = etAmount.getText().toString().trim();
                String category = spCat.getSelectedItem().toString();
                String notes = etNotes.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount = Double.parseDouble(amtStr);
                dbHelper.insertExpense(category, amount, notes);
                refreshAllData();
                Toast.makeText(MainActivity.this, "Expense recorded", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshReports() {
        List<Sale> sales = dbHelper.getAllSales();
        List<Expense> expenses = dbHelper.getAllExpenses();

        double totalSales = 0.0;
        double totalCogs = 0.0;
        for (Sale s : sales) {
            totalSales += s.getFinalAmount();
            totalCogs += s.getTotalCost();
        }

        double totalExpenses = 0.0;
        for (Expense e : expenses) {
            totalExpenses += e.getAmount();
        }

        double grossProfit = ProfitCalculator.calculateGrossProfit(totalSales, totalCogs);
        double netProfit = ProfitCalculator.calculateNetProfit(grossProfit, totalExpenses);
        double margin = ProfitCalculator.calculateMarginPercentage(netProfit, totalSales);

        tvRepTotalSales.setText("Total Sales Revenue: ₹" + String.format(Locale.getDefault(), "%.2f", totalSales));
        tvRepCogs.setText("Cost of Goods Sold (COGS): ₹" + String.format(Locale.getDefault(), "%.2f", totalCogs));
        tvRepGrossProfit.setText("Gross Profit: ₹" + String.format(Locale.getDefault(), "%.2f", grossProfit));
        tvRepTotalExpenses.setText("Business Expenses: ₹" + String.format(Locale.getDefault(), "%.2f", totalExpenses));
        tvRepNetProfit.setText("Actual Net Profit: ₹" + String.format(Locale.getDefault(), "%.2f", netProfit) + " (" + String.format(Locale.getDefault(), "%.1f", margin) + "% Margin)");

        // Expense list
        layoutExpenseList.removeAllViews();
        if (expenses.isEmpty()) {
            layoutExpenseList.addView(tvNoExpenses);
        } else {
            for (Expense e : expenses) {
                TextView tv = new TextView(this);
                tv.setText(e.getDate() + " | " + e.getCategory() + " - ₹" + String.format(Locale.getDefault(), "%.2f", e.getAmount()) + (!e.getNotes().isEmpty() ? " (" + e.getNotes() + ")" : ""));
                tv.setTextColor(getResources().getColor(R.color.text_main));
                tv.setPadding(0, 6, 0, 6);
                layoutExpenseList.addView(tv);
            }
        }
    }

    private void exportReportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("--- DUKAANPROFIT FINANCIAL EXPORT ---\n");
        csv.append("Date,Type,Particulars,Amount,Cost,GrossProfit\n");

        List<Sale> sales = dbHelper.getAllSales();
        for (Sale s : sales) {
            csv.append(s.getCreatedAt()).append(",SALE,")
                    .append(s.getCustomerName()).append(" [").append(s.getPaymentType()).append("],")
                    .append(s.getFinalAmount()).append(",")
                    .append(s.getTotalCost()).append(",")
                    .append(s.getGrossProfit()).append("\n");
        }

        List<Expense> expenses = dbHelper.getAllExpenses();
        for (Expense e : expenses) {
            csv.append(e.getDate()).append(",EXPENSE,")
                    .append(e.getCategory()).append(" - ").append(e.getNotes()).append(",")
                    .append(e.getAmount()).append(",0.00,-").append(e.getAmount()).append("\n");
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "DukaanProfit_Financial_Report.csv");
        sendIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(sendIntent, "Export Report via"));
    }

    private void refreshAllData() {
        refreshDashboard();
        refreshProducts(etSearchProduct != null ? etSearchProduct.getText().toString() : "");
        refreshBillingSpinners();
        refreshUdhaar();
        refreshReports();
    }
}