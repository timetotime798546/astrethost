package com.dailybizmanager.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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
import java.util.ArrayList;

public class ProductsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lstProducts;
    private EditText edtProductSearch;
    private Spinner spnCategoryFilter;
    private ArrayList<Product> productList;
    private ProductAdapter adapter;
    private ArrayList<String> categories;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        dbHelper = new DatabaseHelper(this);
        lstProducts = (ListView) findViewById(R.id.lstProducts);
        edtProductSearch = (EditText) findViewById(R.id.edtProductSearch);
        spnCategoryFilter = (Spinner) findViewById(R.id.spnCategoryFilter);
        Button btnAddNewProduct = (Button) findViewById(R.id.btnAddNewProduct);

        productList = new ArrayList<>();
        categories = new ArrayList<>();

        btnAddNewProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProductsActivity.this, AddProductActivity.class));
            }
        });

        edtProductSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString(), selectedCategory);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lstProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product clicked = productList.get(position);
                Intent editIntent = new Intent(ProductsActivity.this, AddProductActivity.class);
                editIntent.putExtra("product_id", clicked.id);
                startActivity(editIntent);
            }
        });

        loadCategories();
        setupCategorySpinner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(edtProductSearch.getText().toString(), selectedCategory);
    }

    private void loadCategories() {
        categories.clear();
        categories.add("All");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != ''", null);
        while (cursor.moveToNext()) {
            categories.add(cursor.getString(0));
        }
        cursor.close();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategoryFilter.setAdapter(catAdapter);

        spnCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categories.get(position);
                loadProducts(edtProductSearch.getText().toString(), selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadProducts(String query, String category) {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = "";
        ArrayList<String> selectionArgs = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            selection += "(name LIKE ? OR sku LIKE ?)";
            selectionArgs.add("%" + query + "%");
            selectionArgs.add("%" + query + "%");
        }

        if (category != null && !category.equals("All")) {
            if (!selection.isEmpty()) selection += " AND ";
            selection += "category = ?";
            selectionArgs.add(category);
        }

        String whereClause = selection.isEmpty() ? null : selection;
        String[] argsArray = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

        Cursor cursor = db.query("products", null, whereClause, argsArray, null, null, "name ASC");

        while (cursor.moveToNext()) {
            Product p = new Product();
            p.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            p.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            p.sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
            p.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            p.purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"));
            p.sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"));
            p.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
            p.minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"));
            p.unit = cursor.getString(cursor.getColumnIndexOrThrow("unit"));
            productList.add(p);
        }
        cursor.close();

        if (adapter == null) {
            adapter = new ProductAdapter(this, productList);
            lstProducts.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    static class Product {
        int id;
        String name;
        String sku;
        String category;
        double purchasePrice;
        double sellingPrice;
        int stock;
        int minStock;
        String unit;
    }

    private class ProductAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<Product> items;

        public ProductAdapter(Context context, ArrayList<Product> items) {
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
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            Product p = items.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(p.name + " (" + p.sku + ")");
            text1.setTextSize(16sp);

            String stockInfo = "Stock: " + p.stock + " " + p.unit + " | Price: $" + String.format("%.2f", p.sellingPrice);
            text2.setText(stockInfo);

            if (p.stock <= p.minStock) {
                text2.setTextColor(Color.RED);
                text2.setText(stockInfo + " [LOW STOCK]");
            } else {
                text2.setTextColor(Color.GRAY);
            }

            return convertView;
        }
    }
}