package com.mumbaimedical.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ExpiryAlertsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvExpiringMedicines;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expiry_alerts);

        dbHelper = new DatabaseHelper(this);
        lvExpiringMedicines = findViewById(R.id.lvExpiringMedicines);

        loadExpiringMedicines();
    }

    private void loadExpiringMedicines() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT id AS _id, name, company, batch_number, selling_price, quantity, exp_date FROM " +
                DatabaseHelper.TABLE_MEDICINES + " WHERE exp_date <= date('now', '+90 days') ORDER BY exp_date ASC";

        Cursor cursor = db.rawQuery(sql, null);

        String[] from = new String[]{"name", "batch_number", "exp_date", "quantity"};
        int[] to = new int[]{R.id.tvMedName, R.id.tvMedDetails, R.id.tvExpiryInfo, R.id.tvMedQty};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_medicine,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.tvExpiryInfo) {
                    String exp = cursor.getString(cursor.getColumnIndexOrThrow("exp_date"));
                    TextView tv = (TextView) view;
                    tv.setText("EXPIRY DATE: " + exp);
                    tv.setTextColor(getResources().getColor(R.color.warning_red));
                    tv.setTypeface(null, Typeface.BOLD);
                    return true;
                }
                return false;
            }
        });

        lvExpiringMedicines.setAdapter(adapter);
    }
}