package com.shopinventorysync.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Helper model to work with locally
    private static class Product {
        String id;
        String name;
        double price;
        int quantity;

        Product(String id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }

    private BackendApi backendApi;
    private ProgressDialog progressDialog;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private TextView tvTotalItems, tvTotalValue;
    private EditText etSearch;
    private ImageButton btnRefresh;
    private ListView lvProducts;
    private Button btnAddProduct;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        backendApi = new BackendApi(this);
        backendApi.setToken(token);

        tvTotalItems = (TextView) findViewById(R.id.tv_total_items);
        tvTotalValue = (TextView) findViewById(R.id.tv_total_value);
        etSearch = (EditText) findViewById(R.id.et_search);
        btnRefresh = (ImageButton) findViewById(R.id.btn_refresh);
        lvProducts = (ListView) findViewById(R.id.lv_products);
        btnAddProduct = (Button) findViewById(R.id.btn_add_product);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing Stock list...");
        progressDialog.setCancelable(false);

        adapter = new ProductAdapter(this, filteredProducts);
        lvProducts.setAdapter(adapter);

        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchLiveStock();
            }
        });

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProductDialog();
            }
        });

        lvProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product selectedProduct = filteredProducts.get(position);
                showUpdateStockDialog(selectedProduct);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProductList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initial live stock fetch
        fetchLiveStock();
    }

    private void logout() {
        progressDialog.setMessage("Logging out...");
        progressDialog.show();
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                clearSession();
            }

            @Override
            public void onError(String error) {
                // Fallback exit session anyway on clear logout request
                clearSession();
            }
        });
    }

    private void clearSession() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                SharedPreferences.Editor editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void fetchLiveStock() {
        progressDialog.setMessage("Syncing Stock list...");
        progressDialog.show();

        backendApi.getProducts(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        try {
                            JSONArray records = response.getJSONArray("records");
                            allProducts.clear();

                            for (int i = 0; i < records.length(); i++) {
                                JSONObject rec = records.getJSONObject(i);
                                String id = rec.getString("id");
                                JSONObject data = rec.getJSONObject("data");

                                String name = data.optString("name", "Unnamed Item");
                                double price = data.optDouble("price", 0.0);
                                int quantity = data.optInt("quantity", 0);

                                allProducts.add(new Product(id, name, price, quantity));
                            }

                            // Perform calculations and update UI
                            filterProductList(etSearch.getText().toString());

                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Response format error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Sync Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * MANDATORY OFFLINE CALCULATION:
     * Compute total stock quantities and total inventory value inside Android app.
     */
    private void performOfflineCalculations() {
        int totalQty = 0;
        double totalValue = 0.0;

        // Perform calculation over all loaded backend records
        for (Product product : allProducts) {
            totalQty += product.quantity;
            totalValue += (product.price * product.quantity);
        }

        // Display locally computed result
        tvTotalItems.setText(String.valueOf(totalQty));
        tvTotalValue.setText(String.format(Locale.US, "$%.2f", totalValue));
    }

    private void filterProductList(String text) {
        filteredProducts.clear();
        String query = text.toLowerCase().trim();

        for (Product product : allProducts) {
            if (product.name.toLowerCase().contains(query)) {
                filteredProducts.add(product);
            }
        }

        adapter.notifyDataSetChanged();
        performOfflineCalculations();
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Product");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.product_list_item, null);
        // Reuse layout visually, or just create small custom vertical form view
        LinearLayout formLayout = new LinearLayout(this);
        formLayout.setOrientation(LinearLayout.VERTICAL);
        formLayout.setPadding(36, 18, 36, 18);

        final EditText etName = new EditText(this);
        etName.setHint("Item Name");
        formLayout.addView(etName);

        final EditText etPrice = new EditText(this);
        etPrice.setHint("Price ($)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        formLayout.addView(etPrice);

        final EditText etQty = new EditText(this);
        etQty.setHint("Initial Quantity in Stock");
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        formLayout.addView(etQty);

        builder.setView(formLayout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String qtyStr = etQty.getText().toString().trim();

                if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double price = Double.parseDouble(priceStr);
                int qty = Integer.parseInt(qtyStr);

                progressDialog.setMessage("Adding product to cloud...");
                progressDialog.show();

                backendApi.createProduct(name, price, qty, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Product added successfully", Toast.LENGTH_SHORT).show();
                                fetchLiveStock();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Error adding: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showUpdateStockDialog(final Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Stock: " + product.name);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 18, 36, 18);

        TextView tvCurrent = new TextView(this);
        tvCurrent.setText("Current stock quantity: " + product.quantity);
        tvCurrent.setPadding(0, 0, 0, 16);
        layout.addView(tvCurrent);

        final EditText etNewQty = new EditText(this);
        etNewQty.setHint("Modify/New Stock level");
        etNewQty.setText(String.valueOf(product.quantity));
        etNewQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etNewQty);

        builder.setView(layout);

        builder.setPositiveButton("Update Live Stock", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String qtyStr = etNewQty.getText().toString().trim();
                if (qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Quantity is empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                int newQty = Integer.parseInt(qtyStr);

                progressDialog.setMessage("Syncing Stock update...");
                progressDialog.show();

                backendApi.updateProduct(product.id, product.name, product.price, newQty, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Live stock updated instantly!", Toast.LENGTH_SHORT).show();
                                fetchLiveStock();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Failed to update: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        builder.setNeutralButton("Remove Product", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Confirm deletion
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete item")
                        .setMessage("Are you sure you want to completely remove " + product.name + " from the synced list?")
                        .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                progressDialog.setMessage("Deleting product...");
                                progressDialog.show();
                                backendApi.deleteProduct(product.id, new BackendApi.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                                fetchLiveStock();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(final String error) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();
                                                Toast.makeText(MainActivity.this, "Failed to delete: " + error, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private static class ProductAdapter extends BaseAdapter {
        private Context context;
        private List<Product> list;

        ProductAdapter(Context context, List<Product> list) {
            this.context = context;
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
                convertView = LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false);
            }

            Product product = list.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_item_name);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.tv_item_price);
            TextView tvQty = (TextView) convertView.findViewById(R.id.tv_item_qty);
            TextView tvTotal = (TextView) convertView.findViewById(R.id.tv_item_total);

            tvName.setText(product.name);
            tvPrice.setText(String.format(Locale.US, "Price: $%.2f", product.price));
            tvQty.setText(String.format(Locale.US, "Stock: %d pcs", product.quantity));

            // Mandatory local computation: Total row stock value = item price * item quantity
            double stockValue = product.price * product.quantity;
            tvTotal.setText(String.format(Locale.US, "$%.2f", stockValue));

            return convertView;
        }
    }
}