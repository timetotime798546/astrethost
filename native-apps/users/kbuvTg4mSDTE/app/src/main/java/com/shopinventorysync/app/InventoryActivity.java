package com.shopinventorysync.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class InventoryActivity extends Activity {

    private Button btnAddProduct;
    private EditText edtSearch;
    private Spinner spinCategoryFilter;
    private ListView listProducts;

    private BackendApi api;
    private ArrayList<Product> allProducts = new ArrayList<>();
    private ArrayList<Product> filteredProducts = new ArrayList<>();
    private InventoryAdapter adapter;

    private String selectedCategory = "All";
    private String searchKeyword = "";

    private static final String[] CATEGORIES = {"All", "Groceries", "Hardware", "Electronics", "General"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        api = new BackendApi(this);

        btnAddProduct = (Button) findViewById(R.id.btnAddProduct);
        edtSearch = (EditText) findViewById(R.id.edtSearch);
        spinCategoryFilter = (Spinner) findViewById(R.id.spinCategoryFilter);
        listProducts = (ListView) findViewById(R.id.listProducts);

        // Populate spinner categories
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        spinCategoryFilter.setAdapter(spinAdapter);

        adapter = new InventoryAdapter(this, filteredProducts);
        listProducts.setAdapter(adapter);

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open creation dialog/screen with empty product ID
                Intent intent = new Intent(InventoryActivity.this, AddEditProductActivity.class);
                startActivity(intent);
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = CATEGORIES[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Click list item to edit
        listProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product p = filteredProducts.get(position);
                Intent intent = new Intent(InventoryActivity.this, AddEditProductActivity.class);
                intent.putExtra("productId", p.id);
                intent.putExtra("sku", p.sku);
                intent.putExtra("name", p.name);
                intent.putExtra("category", p.category);
                intent.putExtra("purchasePrice", p.purchasePrice);
                intent.putExtra("sellingPrice", p.sellingPrice);
                intent.putExtra("quantity", p.quantity);
                intent.putExtra("lowStockThreshold", p.lowStockThreshold);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventoryData();
    }

    private void loadInventoryData() {
        // First load from memory cache
        try {
            JSONArray cachedArr = OfflineCache.getCachedProducts(this);
            allProducts.clear();
            for (int i = 0; i < cachedArr.length(); i++) {
                allProducts.add(new Product(cachedArr.getJSONObject(i)));
            }
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Then reload from backend API
        api.getRecords("products", new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                OfflineCache.cacheProducts(InventoryActivity.this, result);
                allProducts.clear();
                for (int i = 0; i < result.length(); i++) {
                    try {
                        allProducts.add(new Product(result.getJSONObject(i)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                applyFilters();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryActivity.this, "Offline. Cached products loaded.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Dynamic Filter calculations based on criteria
    private void applyFilters() {
        filteredProducts.clear();
        String searchLower = searchKeyword.toLowerCase();

        for (int i = 0; i < allProducts.size(); i++) {
            Product p = allProducts.get(i);
            boolean matchCat = selectedCategory.equals("All") || p.category.equalsIgnoreCase(selectedCategory);
            boolean matchSearch = p.name.toLowerCase().contains(searchLower) || p.sku.toLowerCase().contains(searchLower);

            if (matchCat && matchSearch) {
                filteredProducts.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Custom non-appcompat listview adapter
    private static class InventoryAdapter extends BaseAdapter {
        private final Context ctx;
        private final ArrayList<Product> list;

        public InventoryAdapter(Context ctx, ArrayList<Product> list) {
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
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_product, parent, false);
            }

            Product p = list.get(position);

            TextView txtProdName = (TextView) convertView.findViewById(R.id.txtProdName);
            TextView txtCategoryBadge = (TextView) convertView.findViewById(R.id.txtCategoryBadge);
            TextView txtSkuCode = (TextView) convertView.findViewById(R.id.txtSkuCode);
            TextView txtMarginLabel = (TextView) convertView.findViewById(R.id.txtMarginLabel);
            TextView txtPricingLabel = (TextView) convertView.findViewById(R.id.txtPricingLabel);
            TextView txtStockLevel = (TextView) convertView.findViewById(R.id.txtStockLevel);
            TextView txtLowStockIndicator = (TextView) convertView.findViewById(R.id.txtLowStockIndicator);

            txtProdName.setText(p.name);
            txtCategoryBadge.setText(p.category);
            txtSkuCode.setText("SKU: " + p.sku);

            // Dynamic Margin calculations
            txtMarginLabel.setText(String.format("Margin: %.1f%%", p.getProfitMargin()));
            txtPricingLabel.setText(String.format("Buy: $%.2f | Sell: $%.2f", p.purchasePrice, p.sellingPrice));
            txtStockLevel.setText(p.quantity + " units");

            if (p.isLowStock()) {
                txtStockLevel.setTextColor(0xFFD32F2F);
                txtLowStockIndicator.setVisibility(View.VISIBLE);
                txtLowStockIndicator.setText("⚠️ Low Stock Alert: Below " + p.lowStockThreshold + " threshold!");
            } else {
                txtStockLevel.setTextColor(0xFF333333);
                txtLowStockIndicator.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}