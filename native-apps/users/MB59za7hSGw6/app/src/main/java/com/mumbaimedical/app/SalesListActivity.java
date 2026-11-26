package com.mumbaimedical.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SalesListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvSales;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_list);

        dbHelper = new DatabaseHelper(this);
        lvSales = findViewById(R.id.lvSales);

        loadSales();

        lvSales.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor c = db.rawQuery("SELECT image_path FROM " + DatabaseHelper.TABLE_SALES + " WHERE id = ?", new String[]{String.valueOf(id)});
                if (c.moveToFirst()) {
                    String photoPath = c.getString(0);
                    if (photoPath != null && !photoPath.isEmpty()) {
                        PhotoHelper.showFullScreenImage(SalesListActivity.this, photoPath);
                    }
                }
                c.close();
            }
        });
    }

    private void loadSales() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id AS _id, customer_name, grand_total, sale_date, image_path FROM " + DatabaseHelper.TABLE_SALES + " ORDER BY id DESC", null);

        String[] from = new String[]{"customer_name", "grand_total", "sale_date", "image_path"};
        int[] to = new int[]{R.id.tvSaleCust, R.id.tvSaleAmount, R.id.tvSaleDate, R.id.ivCustomerItemThumb};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_sale,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int id = view.getId();
                if (id == R.id.ivCustomerItemThumb) {
                    String photoPath = cursor.getString(columnIndex);
                    PhotoHelper.displayThumbnail(SalesListActivity.this, (ImageView) view, photoPath, 100, 100);
                    return true;
                } else if (id == R.id.tvSaleAmount) {
                    double amt = cursor.getDouble(cursor.getColumnIndexOrThrow("grand_total"));
                    ((TextView) view).setText(String.format("₹ %.2f", amt));
                    return true;
                }
                return false;
            }
        });

        lvSales.setAdapter(adapter);
    }
}