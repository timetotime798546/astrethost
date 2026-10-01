package com.dailybizmanager.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import java.util.Date;
import java.util.Locale;

public class SalesActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spnCustomers, spnProducts;
    private EditText edtAddQty, edtDiscount, edtTaxRate, edtPaidAmount;
    private TextView txtSubtotal, txtGrandTotal, txtBalanceDue;
    private LinearLayout layoutCartItems;

    private ArrayList<CustomersActivity.Customer> customers;
    private ArrayList<ProductsActivity.Product> products;
    private ArrayList<CartItem> cart;

    private double subtotal = 0.0;
    private double discount = 0.0;
    private double taxRate = 5.0;
    private double grandTotal = 0.0;
    private double amountPaid = 0.0;
    private double balanceDue = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        dbHelper = new DatabaseHelper(this);

        spnCustomers = (Spinner) findViewById(R.id.spnCustomers);
        spnProducts = (Spinner) findViewById(R.id.spnProducts);
        edtAddQty = (EditText) findViewById(R.id.edtAddQty);
        edtDiscount = (EditText) findViewById(R.id.edtDiscount);
        edtTaxRate = (EditText) findViewById(R.id.edtTaxRate);
        edtPaidAmount = (EditText) findViewById(R.id.edtPaidAmount);

        txtSubtotal = (TextView) findViewById(R.id.txtSubtotal);
        txtGrandTotal = (TextView) findViewById(R.id.txtGrandTotal);
        txtBalanceDue = (TextView) findViewById(R.id.txtBalanceDue);
        layoutCartItems = (LinearLayout) findViewById(R.id.layoutCartItems);

        Button btnAddProductToCart = (Button) findViewById(R.id.btnAddProductToCart);
        Button btnCancelSale = (Button) findViewById(R.id.btnCancelSale);
        Button btnSaveSale = (Button) findViewById(R.id.btnSaveSale);

        customers = new ArrayList<>();
        products = new ArrayList<>();
        cart = new ArrayList<>();

        loadCustomers();
        loadProducts();
        loadDefaultTax();

        btnAddProductToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProductToCart();
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotals();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtDiscount.addTextChangedListener(textWatcher);
        edtTaxRate.addTextChangedListener(textWatcher);
        edtPaidAmount.addTextChangedListener(textWatcher);

        btnCancelSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSaveSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInvoiceSale();
            }
        });
    }

    private void loadCustomers() {
        customers.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("customers", null, null, null, null, null, "name ASC");

        ArrayList<String> customerNames = new ArrayList<>();
        while (cursor.moveToNext()) {
            CustomersActivity.Customer c = new CustomersActivity.Customer();
            c.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            c.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            c.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            c.balance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
            customers.add(c);
            customerNames.add(c.name + " (" + c.phone + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCustomers.setAdapter(adapter);
    }

    private void loadProducts() {
        products.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("products", null, null, null, null, null, "name ASC");

        ArrayList<String> productNames = new ArrayList<>();
        while (cursor.moveToNext()) {
            ProductsActivity.Product p = new ProductsActivity.Product();
            p.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            p.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            p.sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
            p.sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"));
            p.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
            p.minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"));
            products.add(p);
            productNames.add(p.name + " ($" + String.format("%.2f", p.sellingPrice) + " | stock: " + p.stock + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnProducts.setAdapter(adapter);
    }

    private void loadDefaultTax() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT tax FROM settings LIMIT 1", null);
        if (cursor.moveToFirst()) {
            edtTaxRate.setText(String.valueOf(cursor.getDouble(0)));
        }
        cursor.close();
    }

    private void addProductToCart() {
        if (products.isEmpty()) return;

        int selectedIndex = spnProducts.getSelectedItemPosition();
        if (selectedIndex == -1) return;

        ProductsActivity.Product p = products.get(selectedIndex);
        String qtyStr = edtAddQty.getText().toString().trim();

        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        if (qty <= 0) {
            Toast.makeText(this, "Quantity must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > p.stock) {
            Toast.makeText(this, "Insufficient stock! Available: " + p.stock, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if item already in cart
        boolean exists = false;
        for (CartItem item : cart) {
            if (item.product.id == p.id) {
                if (item.quantity + qty > p.stock) {
                    Toast.makeText(this, "Total quantity exceeds stock level!", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.quantity += qty;
                exists = true;
                break;
            }
        }

        if (!exists) {
            cart.add(new CartItem(p, qty));
        }

        edtAddQty.setText("");
        renderCart();
        calculateTotals();
    }

    private void renderCart() {
        layoutCartItems.removeAllViews();
        for (int i = 0; i < cart.size(); i++) {
            final int index = i;
            CartItem item = cart.get(i);

            LinearLayout itemView = new LinearLayout(this);
            itemView.setOrientation(LinearLayout.HORIZONTAL);
            itemView.setPadding(8, 8, 8, 8);

            TextView txtItemInfo = new TextView(this);
            txtItemInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            txtItemInfo.setText(item.product.name + " x" + item.quantity + " - $" + String.format("%.2f", item.product.sellingPrice * item.quantity));
            txtItemInfo.setTextSize(14);

            Button btnRemove = new Button(this);
            btnRemove.setText("X");
            btnRemove.setBackgroundColor(Color.RED);
            btnRemove.setTextColor(Color.WHITE);
            btnRemove.setMinWidth(40);
            btnRemove.setMinHeight(30);
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cart.remove(index);
                    renderCart();
                    calculateTotals();
                }
            });

            itemView.addView(txtItemInfo);
            itemView.addView(btnRemove);
            layoutCartItems.addView(itemView);
        }
    }

    private void calculateTotals() {
        subtotal = 0.0;
        for (CartItem item : cart) {
            subtotal += item.product.sellingPrice * item.quantity;
        }
        txtSubtotal.setText("$" + String.format("%.2f", subtotal));

        String discStr = edtDiscount.getText().toString().trim();
        discount = discStr.isEmpty() ? 0.0 : Double.parseDouble(discStr);

        String taxStr = edtTaxRate.getText().toString().trim();
        taxRate = taxStr.isEmpty() ? 0.0 : Double.parseDouble(taxStr);

        double taxAmount = (subtotal - discount) * (taxRate / 100.0);
        grandTotal = subtotal - discount + taxAmount;
        if (grandTotal < 0) grandTotal = 0;

        txtGrandTotal.setText("$" + String.format("%.2f", grandTotal));

        String paidStr = edtPaidAmount.getText().toString().trim();
        amountPaid = paidStr.isEmpty() ? 0.0 : Double.parseDouble(paidStr);

        balanceDue = grandTotal - amountPaid;
        txtBalanceDue.setText("$" + String.format("%.2f", balanceDue));
    }

    private void saveInvoiceSale() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Shopping cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customers.isEmpty()) {
            Toast.makeText(this, "Add a customer before saving sales", Toast.LENGTH_SHORT).show();
            return;
        }

        int customerIdx = spnCustomers.getSelectedItemPosition();
        if (customerIdx == -1) return;
        CustomersActivity.Customer c = customers.get(customerIdx);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            String invoiceNo = "INV-" + System.currentTimeMillis();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // Insert into sales table
            ContentValues saleVal = new ContentValues();
            saleVal.put("invoice_no", invoiceNo);
            saleVal.put("customer_id", c.id);
            saleVal.put("date", dateStr);
            saleVal.put("subtotal", subtotal);
            saleVal.put("discount", discount);
            saleVal.put("tax", taxRate);
            saleVal.put("total", grandTotal);
            saleVal.put("paid", amountPaid);
            saleVal.put("balance", balanceDue);

            long saleId = db.insert("sales", null, saleVal);

            // Update product stocks and insert sales items
            for (CartItem item : cart) {
                ContentValues itemVal = new ContentValues();
                itemVal.put("sale_id", saleId);
                itemVal.put("product_id", item.product.id);
                itemVal.put("product_name", item.product.name);
                itemVal.put("quantity", item.quantity);
                itemVal.put("selling_price", item.product.sellingPrice);
                itemVal.put("total", item.product.sellingPrice * item.quantity);
                db.insert("sale_items", null, itemVal);

                // Reduce inventory stock
                db.execSQL("UPDATE products SET stock = stock - ? WHERE id = ?", new Object[]{item.quantity, item.product.id});
            }

            // Update customer totals & outstanding metrics
            db.execSQL("UPDATE customers SET total_purchases = total_purchases + ?, amount_paid = amount_paid + ?, balance = balance + ? WHERE id = ?",
                    new Object[]{grandTotal, amountPaid, balanceDue, c.id});

            db.setTransactionSuccessful();

            Toast.makeText(this, "Invoice saved successfully!", Toast.LENGTH_SHORT).show();

            // Open Printed Invoice Activity
            Intent invoiceIntent = new Intent(SalesActivity.this, InvoiceActivity.class);
            invoiceIntent.putExtra("sale_id", (int) saleId);
            startActivity(invoiceIntent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Database failure saving invoice", Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
    }

    private static class CartItem {
        ProductsActivity.Product product;
        int quantity;

        CartItem(ProductsActivity.Product p, int q) {
            this.product = p;
            this.quantity = q;
        }
    }
}