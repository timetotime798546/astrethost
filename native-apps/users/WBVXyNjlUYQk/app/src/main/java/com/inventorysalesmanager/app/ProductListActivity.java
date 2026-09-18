package com.inventorysalesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

public class ProductListActivity extends Activity {

    private ListView lvProducts;
    private TextView tvEmpty;
    private EditText etSearch;
    private Button btnClearSearch;
    private DatabaseHelper dbHelper;
    private ProductAdapter adapter;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        dbHelper = new DatabaseHelper(this);

        lvProducts = (ListView) findViewById(R.id.lv_products);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        etSearch = (EditText) findViewById(R.id.et_search);
        btnClearSearch = (Button) findViewById(R.id.btn_clear_search);
        Button btnAddProduct = (Button) findViewById(R.id.btn_add_product);

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductListActivity.this, AddEditProductActivity.class);
                startActivity(intent);
            }
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product selectedProduct = productList.get(position);
                showActionDialog(selectedProduct);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(etSearch.getText().toString());
    }

    private void loadProducts(String query) {
        productList = dbHelper.getAllProducts(query);
        if (productList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvProducts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvProducts.setVisibility(View.VISIBLE);
        }
        adapter = new ProductAdapter(this, productList);
        lvProducts.setAdapter(adapter);
    }

    private void showActionDialog(final Product product) {
        CharSequence[] options = new CharSequence[]{"View Details / Stats", "Edit Product", "Delete Product"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(product.getName());
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent intent = new Intent(ProductListActivity.this, ProductDetailActivity.class);
                    intent.putExtra("product_id", product.getId());
                    startActivity(intent);
                } else if (which == 1) {
                    Intent intent = new Intent(ProductListActivity.this, AddEditProductActivity.class);
                    intent.putExtra("product_id", product.getId());
                    startActivity(intent);
                } else if (which == 2) {
                    confirmDeletion(product);
                }
            }
        });
        builder.show();
    }

    private void confirmDeletion(final Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete '" + product.getName() + "'?\n\nWarning: This will also clear all sales records linked with this product.");
        builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteProduct(product.getId());
                Toast.makeText(ProductListActivity.this, "Product and linked sales successfully removed", Toast.LENGTH_SHORT).show();
                loadProducts(etSearch.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Custom Custom Adapter for SQLite Cursor matching
    private static class ProductAdapter extends BaseAdapter {
        private final Context context;
        private final List<Product> list;

        public ProductAdapter(Context context, List<Product> list) {
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
            return list.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false);
            }

            Product product = list.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.item_name);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.item_price);
            TextView tvSku = (TextView) convertView.findViewById(R.id.item_sku);
            TextView tvStock = (TextView) convertView.findViewById(R.id.item_stock);
            TextView tvBadge = (TextView) convertView.findViewById(R.id.item_low_stock_badge);

            tvName.setText(product.getName());
            tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", product.getSellingPrice()));
            tvSku.setText("SKU: " + (product.getSku() == null || product.getSku().isEmpty() ? "N/A" : product.getSku()));
            tvStock.setText("Stock: " + product.getStockQty() + " units");

            if (product.isLowStock()) {
                tvBadge.setVisibility(View.VISIBLE);
                tvStock.setTextColor(context.getResources().getColor(R.color.alert_red));
            } else {
                tvBadge.setVisibility(View.GONE);
                tvStock.setTextColor(context.getResources().getColor(R.color.text_primary));
            }

            return convertView;
        }
    }
}