package com.dailybizmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class InvoiceActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView txtInvShopName, txtInvShopDetails, txtInvNumber, txtInvDate, txtInvCustomer;
    private TextView txtInvSubtotal, txtInvDiscount, txtInvTax, txtInvTotal, txtInvPaid, txtInvBalance;
    private LinearLayout layoutInvItems;
    private int saleId = -1;
    private String shareTextRaw = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        dbHelper = new DatabaseHelper(this);

        txtInvShopName = (TextView) findViewById(R.id.txtInvShopName);
        txtInvShopDetails = (TextView) findViewById(R.id.txtInvShopDetails);
        txtInvNumber = (TextView) findViewById(R.id.txtInvNumber);
        txtInvDate = (TextView) findViewById(R.id.txtInvDate);
        txtInvCustomer = (TextView) findViewById(R.id.txtInvCustomer);

        txtInvSubtotal = (TextView) findViewById(R.id.txtInvSubtotal);
        txtInvDiscount = (TextView) findViewById(R.id.txtInvDiscount);
        txtInvTax = (TextView) findViewById(R.id.txtInvTax);
        txtInvTotal = (TextView) findViewById(R.id.txtInvTotal);
        txtInvPaid = (TextView) findViewById(R.id.txtInvPaid);
        txtInvBalance = (TextView) findViewById(R.id.txtInvBalance);
        layoutInvItems = (LinearLayout) findViewById(R.id.layoutInvItems);

        Button btnBackToMenu = (Button) findViewById(R.id.btnBackToMenu);
        Button btnShareInvoice = (Button) findViewById(R.id.btnShareInvoice);

        if (getIntent().hasExtra("sale_id")) {
            saleId = getIntent().getIntExtra("sale_id", -1);
            loadInvoiceData();
        } else {
            Toast.makeText(this, "Invoice not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnShareInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareInvoiceDetails();
            }
        });
    }

    private void loadInvoiceData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Shop Info Settings
        Cursor setCur = db.rawQuery("SELECT shop_name, shop_phone, shop_address FROM settings LIMIT 1", null);
        String shopName = "My Retail Store";
        String shopDetails = "123 Business Lane";
        if (setCur.moveToFirst()) {
            shopName = setCur.getString(0);
            shopDetails = setCur.getString(2) + " | Tel: " + setCur.getString(1);
        }
        setCur.close();
        txtInvShopName.setText(shopName);
        txtInvShopDetails.setText(shopDetails);

        // 2. Fetch Sales record
        Cursor saleCur = db.rawQuery("SELECT s.*, c.name, c.phone FROM sales s " +
                "INNER JOIN customers c ON s.customer_id = c.id WHERE s.id = ?", new String[]{String.valueOf(saleId)});

        if (!saleCur.moveToFirst()) {
            saleCur.close();
            Toast.makeText(this, "Sale record missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String invoiceNo = saleCur.getString(saleCur.getColumnIndexOrThrow("invoice_no"));
        String date = saleCur.getString(saleCur.getColumnIndexOrThrow("date"));
        String custName = saleCur.getString(saleCur.getColumnIndexOrThrow("name"));
        String custPhone = saleCur.getString(saleCur.getColumnIndexOrThrow("phone"));

        double subtotal = saleCur.getDouble(saleCur.getColumnIndexOrThrow("subtotal"));
        double discount = saleCur.getDouble(saleCur.getColumnIndexOrThrow("discount"));
        double taxRate = saleCur.getDouble(saleCur.getColumnIndexOrThrow("tax"));
        double total = saleCur.getDouble(saleCur.getColumnIndexOrThrow("total"));
        double paid = saleCur.getDouble(saleCur.getColumnIndexOrThrow("paid"));
        double balance = saleCur.getDouble(saleCur.getColumnIndexOrThrow("balance"));

        saleCur.close();

        txtInvNumber.setText("INVOICE: " + invoiceNo);
        txtInvDate.setText("Issued: " + date);
        txtInvCustomer.setText("Customer: " + custName + " (" + custPhone + ")");

        txtInvSubtotal.setText("$" + String.format("%.2f", subtotal));
        txtInvDiscount.setText("-$" + String.format("%.2f", discount));

        double taxAmt = (subtotal - discount) * (taxRate / 100.0);
        txtInvTax.setText("$" + String.format("%.2f", taxAmt) + " (" + taxRate + "%)");
        txtInvTotal.setText("$" + String.format("%.2f", total));
        txtInvPaid.setText("$" + String.format("%.2f", paid));
        txtInvBalance.setText("$" + String.format("%.2f", balance));

        // 3. Line Items list
        layoutInvItems.removeAllViews();
        Cursor itemCur = db.rawQuery("SELECT * FROM sale_items WHERE sale_id = ?", new String[]{String.valueOf(saleId)});

        StringBuilder shareBuilder = new StringBuilder();
        shareBuilder.append("==== ").append(shopName).append(" ====\n");
        shareBuilder.append(shopDetails).append("\n\n");
        shareBuilder.append("Invoice: ").append(invoiceNo).append("\n");
        shareBuilder.append("Date: ").append(date).append("\n");
        shareBuilder.append("Customer: ").append(custName).append("\n\n");
        shareBuilder.append("Items:\n");

        while (itemCur.moveToNext()) {
            String prodName = itemCur.getString(itemCur.getColumnIndexOrThrow("product_name"));
            int qty = itemCur.getInt(itemCur.getColumnIndexOrThrow("quantity"));
            double price = itemCur.getDouble(itemCur.getColumnIndexOrThrow("selling_price"));
            double itemTotal = itemCur.getDouble(itemCur.getColumnIndexOrThrow("total"));

            TextView row = new TextView(this);
            row.setText(prodName + " (x" + qty + ") - $" + String.format("%.2f", itemTotal));
            row.setPadding(0, 4, 0, 4);
            row.setTextColor(Color.DKGRAY);
            row.setTextSize(14);

            layoutInvItems.addView(row);

            shareBuilder.append("- ").append(prodName).append(" (x").append(qty).append(") @ $")
                    .append(String.format("%.2f", price)).append(" : $").append(String.format("%.2f", itemTotal)).append("\n");
        }
        itemCur.close();

        shareBuilder.append("\nSubtotal: $").append(String.format("%.2f", subtotal));
        shareBuilder.append("\nDiscount: -$").append(String.format("%.2f", discount));
        shareBuilder.append("\nGrand Total: $").append(String.format("%.2f", total));
        shareBuilder.append("\nAmount Paid: $").append(String.format("%.2f", paid));
        shareBuilder.append("\nRemaining Balance: $").append(String.format("%.2f", balance));
        shareBuilder.append("\n\nThank you for doing business with us!");

        shareTextRaw = shareBuilder.toString();
    }

    private void shareInvoiceDetails() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Customer Invoice receipt");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareTextRaw);
        startActivity(Intent.createChooser(shareIntent, "Share digital receipt via:"));
    }
}