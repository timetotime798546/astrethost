package com.mumbaimedical.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
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
    }

    private void loadSales() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id AS _id, customer_name, grand_total, sale_date FROM " + DatabaseHelper.TABLE_SALES + " ORDER BY id DESC", null);

        String[] from = new String[]{"customer_name", "grand_total", "sale_date"};
        int[] to = new int[]{R.id.tvSaleCust, R.id.tvSaleAmount, R.id.tvSaleDate};

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
                if (view.getId() == R.id.tvSaleAmount) {
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