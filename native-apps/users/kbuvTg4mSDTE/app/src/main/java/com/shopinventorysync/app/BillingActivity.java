package com.shopinventorysync.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillingActivity extends Activity {

    private Spinner spinBillingProducts;
    private EditText edtBillingQty;
    private Button btnAddToBill, btnCheckout, btnShareInvoice;
    private ListView listBillingCart;
    private TextView txtBillingTotal;

    private BackendApi api;
    private ProgressDialog progressDialog;

    private ArrayList<Product> availableProducts = new ArrayList<>();
    private ArrayList<CartItem> cartItems = new ArrayList<>();
    private CartAdapter cartAdapter;

    private double cartGrandTotal = 0.0;

    private static class CartItem {
        public Product product;
        public int sellQty;
        public double subtotal;

        public CartItem(Product product, int sellQty) {
            this.product = product;
            this.sellQty = sellQty;
            this.subtotal = product.sellingPrice * sellQty;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        api = new BackendApi(this);

        spinBillingProducts = (Spinner) findViewById(R.id.spinBillingProducts);
        edtBillingQty = (EditText) findViewById(R.id.edtBillingQty);
        btnAddToBill = (Button) findViewById(R.id.btnAddToBill);
        btnCheckout = (Button) findViewById(R.id.btnCheckout);
        btnShareInvoice = (Button) findViewById(R.id.btnShareInvoice);
        listBillingCart = (ListView) findViewById(R.id.listBillingCart);
        txtBillingTotal = (TextView) findViewById(R.id.txtBillingTotal);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deducting inventory records...");
        progressDialog.setCancelable(false);

        cartAdapter = new CartAdapter(this, cartItems);
        listBillingCart.setAdapter(cartAdapter);

        // Fetch products list first to support dropdown selection
        loadDropdownProducts();

        btnAddToBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart();
            }
        });

        btnCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCartCheckout();
            }
        });

        btnShareInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareInvoiceText();
            }
        });
    }

    private void loadDropdownProducts() {
        // Read cached list first to ensure high-performance spinner options loading
        try {
            JSONArray cachedArr = OfflineCache.getCachedProducts(this);
            populateDropdown(cachedArr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        api.getRecords("products", new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                populateDropdown(result);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BillingActivity.this, "Offline. Cached items loaded for sale.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDropdown(JSONArray arr) {
        availableProducts.clear();
        ArrayList<String> spinnerOptions = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            try {
                Product p = new Product(arr.getJSONObject(i));
                availableProducts.add(p);
                spinnerOptions.add(p.name + " (" + p.sku + ") - Price: $" + p.sellingPrice + " (Stock: " + p.quantity + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        spinBillingProducts.setAdapter(adapter);
    }

    private void addToCart() {
        if (availableProducts.isEmpty()) {
            Toast.makeText(this, "No stock items available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIdx = spinBillingProducts.getSelectedItemPosition();
        if (selectedIdx < 0 || selectedIdx >= availableProducts.size()) return;

        Product p = availableProducts.get(selectedIdx);
        String qtyStr = edtBillingQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter valid sales amount", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        if (qty <= 0) {
            Toast.makeText(this, "Sales quantity must be positive", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > p.quantity) {
            Toast.makeText(this, "Insufficient stock! Current quantity is " + p.quantity, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if item already in cart, update local values
        boolean exists = false;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            if (item.product.id.equals(p.id)) {
                item.sellQty += qty;
                item.subtotal = item.product.sellingPrice * item.sellQty;
                exists = true;
                break;
            }
        }

        if (!exists) {
            cartItems.add(new CartItem(p, qty));
        }

        edtBillingQty.setText("");
        recalculateCartTotal();
    }

    // Dynamic POS local subtotal calculation engine
    private void recalculateCartTotal() {
        cartGrandTotal = 0.0;
        for (int i = 0; i < cartItems.size(); i++) {
            cartGrandTotal += cartItems.get(i).subtotal;
        }
        txtBillingTotal.setText(String.format("$%.2f", cartGrandTotal));
        cartAdapter.notifyDataSetChanged();
    }

    private void performCartCheckout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Basket is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        checkoutNextItem(0);
    }

    // Process synchronous queue items one-by-one to safely update inventory quantites
    private void checkoutNextItem(final int index) {
        if (index >= cartItems.size()) {
            // Done with updating product inventory! Create transaction log
            createTransactionRecord();
            return;
        }

        final CartItem item = cartItems.get(index);
        final Product p = item.product;

        // Perform stock deduction subtraction calculation locally!
        int remainingQty = p.quantity - item.sellQty;

        JSONObject fields = p.toDataJson();
        try {
            fields.put("quantity", remainingQty); // deduct stock field
        } catch (Exception e) {
            e.printStackTrace();
        }

        api.updateRecord(p.id, fields, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Next checkout queue update
                checkoutNextItem(index + 1);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(BillingActivity.this, "Network Sync Error: Could not deduct stock for " + p.name, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createTransactionRecord() {
        // Compile summary list of sold items
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem ci = cartItems.get(i);
            summary.append(ci.product.name).append(" x").append(ci.sellQty).append("; ");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        JSONObject saleObj = new JSONObject();
        try {
            saleObj.put("sale_date", dateStr);
            saleObj.put("items_summary", summary.toString());
            saleObj.put("total_revenue", cartGrandTotal);
        } catch (Exception e) {
            e.printStackTrace();
        }

        api.createRecord("sales", saleObj, new BackendApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                progressDialog.dismiss();
                Toast.makeText(BillingActivity.this, "POS Checkout Completed! Stock updated.", Toast.LENGTH_LONG).show();
                cartItems.clear();
                recalculateCartTotal();
                // Load updated stock dropdowns
                loadDropdownProducts();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(BillingActivity.this, "Invoice checkout complete, but sales record failed: " + error, Toast.LENGTH_LONG).show();
                cartItems.clear();
                recalculateCartTotal();
                loadDropdownProducts();
            }
        });
    }

    private void shareInvoiceText() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Add items before sharing invoice", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder invoice = new StringBuilder();
        invoice.append("*--- SHOP STOCK INVOICE ---*\n");
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem ci = cartItems.get(i);
            invoice.append(ci.product.name)
                    .append(" (Qty: ").append(ci.sellQty).append(") - ")
                    .append(String.format("$%.2f", ci.subtotal))
                    .append("\n");
        }
        invoice.append("----------------------------\n");
        invoice.append("*GRAND TOTAL: ").append(String.format("$%.2f*", cartGrandTotal)).append("\n");
        invoice.append("Thank you for choosing our store!");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, invoice.toString());
        startActivity(Intent.createChooser(intent, "Send Stock Invoice"));
    }

    // Non-AppCompat baseadapter for list items
    private static class CartAdapter extends BaseAdapter {
        private final Context ctx;
        private final ArrayList<CartItem> list;

        public CartAdapter(Context ctx, ArrayList<CartItem> list) {
            this.ctx = ctx;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            CartItem item = list.get(position);

            TextView txt1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView txt2 = (TextView) convertView.findViewById(android.R.id.text2);

            txt1.setText(item.product.name + " (x" + item.sellQty + ")");
            txt2.setText(String.format("Price per unit: $%.2f | Subtotal: $%.2f", item.product.sellingPrice, item.subtotal));

            return convertView;
        }
    }
}