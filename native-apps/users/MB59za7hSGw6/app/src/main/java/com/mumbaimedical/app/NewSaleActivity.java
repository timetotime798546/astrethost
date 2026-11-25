package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewSaleActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spMedicines;
    private EditText etCustomerName, etSaleQty, etDiscount;
    private Button btnAddCart, btnCompleteSale;
    private ListView lvCart;
    private TextView tvSubtotal, tvGrandTotal;

    private List<Long> medicineIds = new ArrayList<>();
    private List<String> medicineNames = new ArrayList<>();
    private List<Double> medicinePrices = new ArrayList<>();
    private List<Integer> medicineStocks = new ArrayList<>();

    private List<Map<String, String>> cartItems = new ArrayList<>();
    private List<CartModel> cartModels = new ArrayList<>();

    private SimpleAdapter cartAdapter;
    private double subtotal = 0.0;
    private double grandTotal = 0.0;

    private static class CartModel {
        long medicineId;
        String name;
        int qty;
        double price;
        double total;

        CartModel(long id, String name, int qty, double price, double total) {
            this.medicineId = id;
            this.name = name;
            this.qty = qty;
            this.price = price;
            this.total = total;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sale);

        dbHelper = new DatabaseHelper(this);

        spMedicines = findViewById(R.id.spMedicines);
        etCustomerName = findViewById(R.id.etCustomerName);
        etSaleQty = findViewById(R.id.etSaleQty);
        etDiscount = findViewById(R.id.etDiscount);
        btnAddCart = findViewById(R.id.btnAddCart);
        btnCompleteSale = findViewById(R.id.btnCompleteSale);
        lvCart = findViewById(R.id.lvCart);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);

        cartAdapter = new SimpleAdapter(
                this,
                cartItems,
                R.layout.item_cart,
                new String[]{"name", "qty", "total"},
                new int[]{R.id.tvCartName, R.id.tvCartQty, R.id.tvCartTotal}
        );
        lvCart.setAdapter(cartAdapter);

        loadMedicineSpinner();

        btnAddCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart();
            }
        });

        etDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCompleteSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSale();
            }
        });
    }

    private void loadMedicineSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, selling_price, quantity FROM " + DatabaseHelper.TABLE_MEDICINES + " WHERE quantity > 0 ORDER BY name ASC", null);

        medicineIds.clear();
        medicineNames.clear();
        medicinePrices.clear();
        medicineStocks.clear();

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            double price = cursor.getDouble(2);
            int stock = cursor.getInt(3);

            medicineIds.add(id);
            medicineNames.add(name + " (₹" + price + " | Stock: " + stock + ")");
            medicinePrices.add(price);
            medicineStocks.add(stock);
        }
        cursor.close();

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicineNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMedicines.setAdapter(spinnerAdapter);
    }

    private void addToCart() {
        int selectedIndex = spMedicines.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= medicineIds.size()) {
            Toast.makeText(this, "Select a medicine", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etSaleQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        int availableStock = medicineStocks.get(selectedIndex);

        if (qty <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > availableStock) {
            Toast.makeText(this, "Cannot sell more than available stock (" + availableStock + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        long medId = medicineIds.get(selectedIndex);
        String rawName = medicineNames.get(selectedIndex).split(" \\(")[0];
        double price = medicinePrices.get(selectedIndex);
        double itemTotal = price * qty;

        cartModels.add(new CartModel(medId, rawName, qty, price, itemTotal));

        Map<String, String> map = new HashMap<>();
        map.put("name", rawName);
        map.put("qty", "x" + qty);
        map.put("total", String.format(Locale.getDefault(), "₹ %.2f", itemTotal));
        cartItems.add(map);

        cartAdapter.notifyDataSetChanged();
        etSaleQty.setText("");

        recalculateTotals();
    }

    private void recalculateTotals() {
        subtotal = 0.0;
        for (CartModel item : cartModels) {
            subtotal += item.total;
        }

        String discountStr = etDiscount.getText().toString().trim();
        double discount = discountStr.isEmpty() ? 0.0 : Double.parseDouble(discountStr);

        grandTotal = subtotal - discount;
        if (grandTotal < 0) grandTotal = 0.0;

        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %.2f", subtotal));
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", grandTotal));
    }

    private void completeSale() {
        if (cartModels.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = etCustomerName.getText().toString().trim();
        if (customerName.isEmpty()) customerName = "Walk-in Customer";

        String discountStr = etDiscount.getText().toString().trim();
        double discount = discountStr.isEmpty() ? 0.0 : Double.parseDouble(discountStr);

        String saleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues saleCv = new ContentValues();
            saleCv.put("sale_date", saleDate);
            saleCv.put("customer_name", customerName);
            saleCv.put("total_amount", subtotal);
            saleCv.put("discount", discount);
            saleCv.put("grand_total", grandTotal);
            saleCv.put("payment_method", "Cash/UPI");

            long saleId = db.insert(DatabaseHelper.TABLE_SALES, null, saleCv);

            for (CartModel item : cartModels) {
                ContentValues itemCv = new ContentValues();
                itemCv.put("sale_id", saleId);
                itemCv.put("medicine_id", item.medicineId);
                itemCv.put("medicine_name", item.name);
                itemCv.put("quantity", item.qty);
                itemCv.put("price", item.price);
                itemCv.put("total", item.total);

                db.insert(DatabaseHelper.TABLE_SALE_ITEMS, null, itemCv);

                // Deduct stock
                db.execSQL("UPDATE " + DatabaseHelper.TABLE_MEDICINES + " SET quantity = quantity - " + item.qty + " WHERE id = " + item.medicineId);
            }

            db.setTransactionSuccessful();

            new AlertDialog.Builder(this)
                    .setTitle("Sale Completed!")
                    .setMessage("Bill generated for " + customerName + "\nTotal: ₹ " + grandTotal)
                    .setPositiveButton("OK", null)
                    .show();

            cartModels.clear();
            cartItems.clear();
            cartAdapter.notifyDataSetChanged();
            recalculateTotals();
            loadMedicineSpinner();

        } finally {
            db.endTransaction();
        }
    }
}