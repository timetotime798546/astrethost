package com.kkgstore.store;

import android.app.Activity;
import android.content.ContentValues;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewSaleActivity extends Activity {

    private DatabaseHelper dbHelper;

    private Spinner spCustomers, spProducts, spUnit, spPaymentMode;
    private EditText etQty, etDiscount;
    private TextView tvSubtotal, tvGrandTotal;
    private ListView lvCart;
    private CartAdapter cartAdapter;

    private List<CustomerItem> customerList = new ArrayList<>();
    private List<ProductItem> productList = new ArrayList<>();
    private List<CartItem> cartList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_new_sale);
        dbHelper = new DatabaseHelper(this);

        spCustomers = (Spinner) findViewById(R.id.sp_customers);
        spProducts = (Spinner) findViewById(R.id.sp_products);
        spUnit = (Spinner) findViewById(R.id.sp_unit);
        spPaymentMode = (Spinner) findViewById(R.id.sp_payment_mode);

        etQty = (EditText) findViewById(R.id.et_item_qty);
        etDiscount = (EditText) findViewById(R.id.et_discount);
        tvSubtotal = (TextView) findViewById(R.id.tv_bill_subtotal);
        tvGrandTotal = (TextView) findViewById(R.id.tv_bill_grand_total);
        lvCart = (ListView) findViewById(R.id.lv_cart);

        Button btnAddToCart = (Button) findViewById(R.id.btn_add_to_cart);
        Button btnComplete = (Button) findViewById(R.id.btn_complete_sale);

        cartAdapter = new CartAdapter(this, cartList);
        lvCart.setAdapter(cartAdapter);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"KG", "Piece", "Packet", "Litre", "Gram"});
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Cash (नगद)", "Udhaar (उधार)", "UPI / Online"});
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentMode.setAdapter(modeAdapter);

        loadCustomersSpinner();
        loadProductsSpinner();

        btnAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { addItemToCart(); }
        });

        etDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { calculateTotals(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { completeSale(); }
        });
    }

    private void loadCustomersSpinner() {
        customerList.clear();
        customerList.add(new CustomerItem(0, "Walk-in Customer (नगद ग्राहक)"));

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name FROM customers WHERE is_active=1 ORDER BY name ASC", null);
        while (c.moveToNext()) {
            customerList.add(new CustomerItem(c.getLong(0), c.getString(1)));
        }
        c.close();

        ArrayAdapter<CustomerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customerList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCustomers.setAdapter(adapter);
    }

    private void loadProductsSpinner() {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, sell_price, unit, stock FROM products ORDER BY name ASC", null);
        while (c.moveToNext()) {
            productList.add(new ProductItem(c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3), c.getDouble(4)));
        }
        c.close();

        ArrayAdapter<ProductItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProducts.setAdapter(adapter);
    }

    private void addItemToCart() {
        if (spProducts.getSelectedItem() == null) {
            Toast.makeText(this, "कृपया पहले प्रोडक्ट चुनें", Toast.LENGTH_SHORT).show();
            return;
        }

        ProductItem product = (ProductItem) spProducts.getSelectedItem();
        String qtyStr = etQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "मात्रा दर्ज करें", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty = Double.parseDouble(qtyStr);
        String unit = spUnit.getSelectedItem().toString();
        double total = qty * product.sellPrice;

        cartList.add(new CartItem(product.id, product.name, unit, qty, product.sellPrice, total));
        cartAdapter.notifyDataSetChanged();
        calculateTotals();
        etQty.setText("1");
    }

    private void calculateTotals() {
        double subtotal = 0;
        for (CartItem item : cartList) {
            subtotal += item.total;
        }
        String discStr = etDiscount.getText().toString().trim();
        double discount = discStr.isEmpty() ? 0 : Double.parseDouble(discStr);
        double grandTotal = Math.max(0, subtotal - discount);

        tvSubtotal.setText(String.format("₹ %.2f", subtotal));
        tvGrandTotal.setText(String.format("₹ %.2f", grandTotal));
    }

    private void completeSale() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "कार्ट खाली है! सामान जोड़ें", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomerItem cust = (CustomerItem) spCustomers.getSelectedItem();
        String mode = spPaymentMode.getSelectedItem().toString();

        double subtotal = 0;
        for (CartItem item : cartList) subtotal += item.total;

        String discStr = etDiscount.getText().toString().trim();
        double discount = discStr.isEmpty() ? 0 : Double.parseDouble(discStr);
        double grandTotal = Math.max(0, subtotal - discount);

        String saleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int billNo = 101;
            Cursor cBill = db.rawQuery("SELECT MAX(bill_no) FROM sales", null);
            if (cBill.moveToFirst() && cBill.getInt(0) > 0) {
                billNo = cBill.getInt(0) + 1;
            }
            cBill.close();

            ContentValues cvSale = new ContentValues();
            cvSale.put("bill_no", billNo);
            cvSale.put("customer_id", cust.id);
            cvSale.put("subtotal", subtotal);
            cvSale.put("discount", discount);
            cvSale.put("grand_total", grandTotal);
            cvSale.put("payment_mode", mode);
            cvSale.put("sale_date", saleDate);

            long saleId = db.insert("sales", null, cvSale);

            for (CartItem item : cartList) {
                ContentValues cvItem = new ContentValues();
                cvItem.put("sale_id", saleId);
                cvItem.put("product_id", item.productId);
                cvItem.put("product_name", item.name);
                cvItem.put("unit", item.unit);
                cvItem.put("quantity", item.quantity);
                cvItem.put("price", item.price);
                cvItem.put("total", item.total);
                db.insert("sale_items", null, cvItem);

                db.execSQL("UPDATE products SET stock = stock - " + item.quantity + " WHERE id = " + item.productId);
            }

            if (mode.contains("Udhaar") && cust.id > 0) {
                ContentValues cvUdhaar = new ContentValues();
                cvUdhaar.put("customer_id", cust.id);
                cvUdhaar.put("amount", grandTotal);
                cvUdhaar.put("notes", "Sale Bill #" + billNo);
                cvUdhaar.put("entry_date", saleDate);
                db.insert("udhaar_entries", null, cvUdhaar);
            }

            db.setTransactionSuccessful();

            Intent intent = new Intent(this, SaleBillDetailActivity.class);
            intent.putExtra("sale_id", saleId);
            startActivity(intent);
            finish();

        } finally {
            db.endTransaction();
        }
    }

    private static class CustomerItem {
        long id; String name;
        CustomerItem(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static class ProductItem {
        long id; String name, unit; double sellPrice, stock;
        ProductItem(long id, String name, double sellPrice, String unit, double stock) {
            this.id = id; this.name = name; this.sellPrice = sellPrice; this.unit = unit; this.stock = stock;
        }
        @Override public String toString() { return name + " (₹" + sellPrice + "/" + unit + ")"; }
    }

    private static class CartItem {
        long productId; String name, unit; double quantity, price, total;
        CartItem(long productId, String name, String unit, double quantity, double price, double total) {
            this.productId = productId; this.name = name; this.unit = unit;
            this.quantity = quantity; this.price = price; this.total = total;
        }
    }

    private class CartAdapter extends BaseAdapter {
        private Context context; private List<CartItem> items;
        CartAdapter(Context context, List<CartItem> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
            }
            final CartItem item = items.get(position);
            TextView tvName = (TextView) convertView.findViewById(R.id.cart_product_name);
            TextView tvDetails = (TextView) convertView.findViewById(R.id.cart_details);
            TextView tvTotal = (TextView) convertView.findViewById(R.id.cart_total_price);
            Button btnRemove = (Button) convertView.findViewById(R.id.btn_remove_cart_item);

            tvName.setText(item.name);
            tvDetails.setText(String.format("%.2f %s @ ₹%.2f", item.quantity, item.unit, item.price));
            tvTotal.setText(String.format("₹ %.2f", item.total));

            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cartList.remove(position);
                    cartAdapter.notifyDataSetChanged();
                    calculateTotals();
                }
            });
            return convertView;
        }
    }
}