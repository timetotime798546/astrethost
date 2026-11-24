package com.alfariskirana.app;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class SaleBillDetailActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long saleId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_bill_detail);

        dbHelper = new DatabaseHelper(this);
        saleId = getIntent().getLongExtra("sale_id", -1);

        TextView tvInfo = (TextView) findViewById(R.id.tv_bill_sale_info);
        TextView tvItems = (TextView) findViewById(R.id.tv_bill_line_items);
        TextView tvTotals = (TextView) findViewById(R.id.tv_bill_totals);
        Button btnDone = (Button) findViewById(R.id.btn_done_bill);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadBill(tvInfo, tvItems, tvTotals);
    }

    private void loadBill(TextView tvInfo, TextView tvItems, TextView tvTotals) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT customer_name, total_amount, discount, final_amount, payment_mode, date_time FROM sales WHERE id = ?",
                new String[]{String.valueOf(saleId)});

        if (c.moveToFirst()) {
            String custName = c.getString(0);
            double total = c.getDouble(1);
            double disc = c.getDouble(2);
            double finalTotal = c.getDouble(3);
            String mode = c.getString(4);
            String date = c.getString(5);

            tvInfo.setText("Invoice #: " + saleId + "\nCustomer: " + (custName == null ? "Walk-in" : custName) + "\nDate: " + date);
            tvTotals.setText(String.format(Locale.getDefault(),
                    "Subtotal: ₹ %.2f\nDiscount: ₹ %.2f\nFinal Paid: ₹ %.2f\nMode: %s",
                    total, disc, finalTotal, mode));
        }
        c.close();

        StringBuilder sb = new StringBuilder();
        Cursor items = db.rawQuery("SELECT product_name, quantity, unit, unit_price, total_price FROM sale_items WHERE sale_id = ?",
                new String[]{String.valueOf(saleId)});

        if (items.moveToFirst()) {
            do {
                String name = items.getString(0);
                double qty = items.getDouble(1);
                String unit = items.getString(2);
                double price = items.getDouble(3);
                double lineTotal = items.getDouble(4);
                if (unit == null || unit.trim().isEmpty()) {
                    unit = "पीस (Piece)";
                }
                sb.append(String.format(Locale.getDefault(), "• %s\n   %.2f %s x ₹%.2f = ₹%.2f\n", name, qty, unit, price, lineTotal));
            } while (items.moveToNext());
        }
        items.close();

        tvItems.setText(sb.toString());
    }
}