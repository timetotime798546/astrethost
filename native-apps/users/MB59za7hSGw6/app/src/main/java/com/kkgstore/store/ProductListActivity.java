package com.kkgstore.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class ProductListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvProducts;
    private EditText etSearch;
    private List<ProductModel> productList = new ArrayList<>();
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_product_list);
        dbHelper = new DatabaseHelper(this);

        lvProducts = (ListView) findViewById(R.id.lv_products);
        etSearch = (EditText) findViewById(R.id.et_search_product);
        Button btnAdd = (Button) findViewById(R.id.btn_add_product);

        adapter = new ProductAdapter(this, productList);
        lvProducts.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProductListActivity.this, AddEditProductActivity.class));
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DemoLockManager.checkAndEnforceLock(this)) return;
        loadProducts(etSearch.getText().toString().trim());
    }

    private void loadProducts(String filter) {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT id, name, category, unit, cost_price, sell_price, stock FROM products";
        if (filter != null && filter.length() > 0) {
            sql += " WHERE name LIKE '%" + filter + "%' OR category LIKE '%" + filter + "%'";
        }
        sql += " ORDER BY name ASC";

        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            productList.add(new ProductModel(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getDouble(6)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    public static class ProductModel {
        long id;
        String name, category, unit;
        double costPrice, sellPrice, stock;

        ProductModel(long id, String name, String category, String unit, double costPrice, double sellPrice, double stock) {
            this.id = id; this.name = name; this.category = category; this.unit = unit;
            this.costPrice = costPrice; this.sellPrice = sellPrice; this.stock = stock;
        }
    }

    private class ProductAdapter extends BaseAdapter {
        private Context context;
        private List<ProductModel> items;

        ProductAdapter(Context context, List<ProductModel> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() { return items.size(); }
        @Override
        public Object getItem(int position) { return items.get(position); }
        @Override
        public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
            }
            final ProductModel item = items.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.item_product_name);
            TextView tvCat = (TextView) convertView.findViewById(R.id.item_product_category);
            TextView tvPrices = (TextView) convertView.findViewById(R.id.item_product_prices);
            TextView tvStock = (TextView) convertView.findViewById(R.id.item_product_stock);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_edit_product);

            tvName.setText(item.name);
            tvCat.setText("Category: " + (item.category.isEmpty() ? "General" : item.category));
            tvPrices.setText(String.format("Sell: ₹%.2f/%s | Cost: ₹%.2f", item.sellPrice, item.unit, item.costPrice));
            tvStock.setText(String.format("Stock: %.2f %s", item.stock, item.unit));

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, AddEditProductActivity.class);
                    intent.putExtra("product_id", item.id);
                    context.startActivity(intent);
                }
            });

            return convertView;
        }
    }
}