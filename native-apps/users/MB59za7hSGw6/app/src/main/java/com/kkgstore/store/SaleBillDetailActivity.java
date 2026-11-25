package com.kkgstore.store;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SaleBillDetailActivity extends Activity {

    private long saleId;
    private DatabaseHelper dbHelper;
    private TextView tvSaleInfo, tvItems, tvTotals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_sale_bill_detail);
        dbHelper = new DatabaseHelper(this);

        saleId = getIntent().getLongExtra("sale_id", 0);

        tvSaleInfo = (TextView) findViewById(R.id.tv_bill_sale_info);
        tvItems = (TextView) findViewById(R.id.tv_bill_line_items);
        tvTotals = (TextView) findViewById(R.id.tv_bill_totals);

        Button btnShare = (Button) findViewById(R.id.btn_share_bill);
        Button btnDone = (Button) findViewById(R.id.btn_done_bill);

        loadBillDetails();

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { shareBill(); }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
    }

    private void loadBillDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT s.bill_no, s.subtotal, s.discount, s.grand_total, s.payment_mode, s.sale_date, c.name "
                + "FROM sales s LEFT JOIN customers c ON s.customer_id = c.id WHERE s.id=" + saleId;

        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            int billNo = c.getInt(0);
            double subtotal = c.getDouble(1);
            double discount = c.getDouble(2);
            double grandTotal = c.getDouble(3);
            String mode = c.getString(4);
            String date = c.getString(5);
            String custName = c.getString(6);
            if (custName == null || custName.isEmpty()) custName = "Walk-in Customer";

            tvSaleInfo.setText("Bill No: #" + billNo + "\nCustomer: " + custName + "\nDate: " + date);
            tvTotals.setText(String.format("Subtotal: ₹ %.2f\nDiscount: ₹ %.2f\nGrand Total: ₹ %.2f\nPayment Mode: %s", subtotal, discount, grandTotal, mode));
        }
        c.close();

        StringBuilder sb = new StringBuilder();
        Cursor cItems = db.rawQuery("SELECT product_name, quantity, unit, price, total FROM sale_items WHERE sale_id=" + saleId, null);
        while (cItems.moveToNext()) {
            sb.append(String.format("• %s (%.2f %s) @ ₹%.2f = ₹%.2f\n", cItems.getString(0), cItems.getDouble(1), cItems.getString(2), cItems.getDouble(3), cItems.getDouble(4)));
        }
        cItems.close();
        tvItems.setText(sb.toString().trim());
    }

    private void shareBill() {
        String info = tvSaleInfo.getText().toString();
        String items = tvItems.getText().toString();
        String totals = tvTotals.getText().toString();

        String msg = "🧾 *KKG STORE BILL RECEIPT*\n\n"
                + info + "\n\n"
                + "*Items List:*\n" + items + "\n\n"
                + totals + "\n\n"
                + "धन्यवाद! फिर पधारें!";

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "Share Bill"));
        } catch (Exception e) {
            Toast.makeText(this, "शेयर सुविधा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }
}