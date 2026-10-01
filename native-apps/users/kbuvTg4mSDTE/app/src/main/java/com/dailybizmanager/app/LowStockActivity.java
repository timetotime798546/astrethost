package com.dailybizmanager.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class LowStockActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lstLowStock;
    private ArrayList<ProductsActivity.Product> lowStockList;
    private LowStockAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_low_stock);

        dbHelper = new DatabaseHelper(this);
        lstLowStock = (ListView) findViewById(R.id.lstLowStock);
        lowStockList = new ArrayList<>();

        loadLowStockItems();
    }

    private void loadLowStockItems() {
        lowStockList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM products WHERE stock <= min_stock ORDER BY name ASC", null);

        while (cursor.moveToNext()) {
            ProductsActivity.Product p = new ProductsActivity.Product();
            p.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            p.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            p.sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
            p.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
            p.minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"));
            p.unit = cursor.getString(cursor.getColumnIndexOrThrow("unit"));
            lowStockList.add(p);
        }
        cursor.close();

        adapter = new LowStockAdapter(this, lowStockList);
        lstLowStock.setAdapter(adapter);
    }

    private class LowStockAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<ProductsActivity.Product> items;

        public LowStockAdapter(Context context, ArrayList<ProductsActivity.Product> items) {
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
            ProductsActivity.Product p = items.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(p.name + " (" + p.sku + ")");
            text1.setTextColor(Color.RED);

            text2.setText("Current Stock Level: " + p.stock + " " + p.unit + " | Minimum Threshold: " + p.minStock);

            return convertView;
        }
    }
}