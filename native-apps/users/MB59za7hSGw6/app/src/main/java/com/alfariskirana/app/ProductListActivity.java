package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvProducts;
    private EditText etSearch;
    private List<Product> productList = new ArrayList<Product>();
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        dbHelper = new DatabaseHelper(this);
        lvProducts = (ListView) findViewById(R.id.lv_products);
        etSearch = (EditText) findViewById(R.id.et_search_product);
        Button btnAdd = (Button) findViewById(R.id.btn_add_product);

        adapter = new ProductAdapter();
        lvProducts.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductListActivity.this, AddEditProductActivity.class);
                startActivity(intent);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(etSearch.getText().toString());
    }

    private void loadProducts(String query) {
        productList = dbHelper.getAllProducts(query);
        adapter.notifyDataSetChanged();
    }

    private class ProductAdapter extends BaseAdapter {
        @Override
        public int getCount() { return productList.size(); }

        @Override
        public Object getItem(int position) { return productList.get(position); }

        @Override
        public long getItemId(int position) { return productList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ProductListActivity.this).inflate(R.layout.item_product, parent, false);
            }

            final Product p = productList.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.item_product_name);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.item_product_category);
            TextView tvPrices = (TextView) convertView.findViewById(R.id.item_product_prices);
            TextView tvStock = (TextView) convertView.findViewById(R.id.item_product_stock);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_edit_product);

            tvName.setText(p.getName());
            tvCategory.setText("Category: " + (p.getCategory() == null ? "General" : p.getCategory()));
            tvPrices.setText(String.format(Locale.getDefault(), "Sell: ₹%.2f/%s | Cost: ₹%.2f", p.getSellingPrice(), p.getUnit(), p.getPurchasePrice()));
            tvStock.setText(String.format(Locale.getDefault(), "Stock: %.2f %s", p.getStock(), p.getUnit()));

            if (p.getStock() <= 5) {
                tvStock.setTextColor(getResources().getColor(R.color.danger));
            } else {
                tvStock.setTextColor(getResources().getColor(R.color.success));
            }

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ProductListActivity.this, AddEditProductActivity.class);
                    intent.putExtra("product_id", p.getId());
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}