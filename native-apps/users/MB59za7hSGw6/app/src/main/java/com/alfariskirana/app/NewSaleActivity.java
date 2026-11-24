package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewSaleActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spCustomers;
    private Spinner spProducts;
    private EditText etQty;
    private ListView lvCart;
    private TextView tvSubtotal;
    private EditText etDiscount;
    private TextView tvGrandTotal;
    private Spinner spPaymentMode;

    private List<Customer> customerList = new ArrayList<Customer>();
    private List<Product> productList = new ArrayList<Product>();
    private List<SaleItem> cartItems = new ArrayList<SaleItem>();
    private CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sale);

        dbHelper = new DatabaseHelper(this);

        spCustomers = (Spinner) findViewById(R.id.sp_customers);
        spProducts = (Spinner) findViewById(R.id.sp_products);
        etQty = (EditText) findViewById(R.id.et_item_qty);
        lvCart = (ListView) findViewById(R.id.lv_cart);
        tvSubtotal = (TextView) findViewById(R.id.tv_bill_subtotal);
        etDiscount = (EditText) findViewById(R.id.et_discount);
        tvGrandTotal = (TextView) findViewById(R.id.tv_bill_grand_total);
        spPaymentMode = (Spinner) findViewById(R.id.sp_payment_mode);
        Button btnAdd = (Button) findViewById(R.id.btn_add_to_cart);
        Button btnComplete = (Button) findViewById(R.id.btn_complete_sale);

        cartAdapter = new CartAdapter();
        lvCart.setAdapter(cartAdapter);

        setupSpinners();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToCart();
            }
        });

        etDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSale();
            }
        });
    }

    private void setupSpinners() {
        // Customer Spinner
        customerList.clear();
        customerList.add(new Customer(0L, "Walk-in Customer (नगद ग्राहक)", "", 0.0));
        customerList.addAll(dbHelper.getAllCustomers(""));

        List<String> customerNames = new ArrayList<String>();
        for (int i = 0; i < customerList.size(); i++) {
            Customer c = customerList.get(i);
            customerNames.add(c.getId() == 0 ? c.getName() : c.getName() + " (बाकी: ₹" + String.format(Locale.getDefault(), "%.2f", c.getPendingBalance()) + ")");
        }
        ArrayAdapter<String> custAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, customerNames);
        spCustomers.setAdapter(custAdapter);

        // Product Spinner
        productList = dbHelper.getAllProducts("");
        List<String> prodNames = new ArrayList<String>();
        for (int i = 0; i < productList.size(); i++) {
            Product p = productList.get(i);
            prodNames.add(p.getName() + " - ₹" + p.getSellingPrice() + "/" + p.getUnit() + " (Stock: " + p.getStock() + ")");
        }
        ArrayAdapter<String> prodAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, prodNames);
        spProducts.setAdapter(prodAdapter);

        // Required Payment Type Selector options (Default: नगद (Cash))
        String[] paymentModes = {"नगद (Cash)", "उधार (Credit)"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, paymentModes);
        spPaymentMode.setAdapter(modeAdapter);
        spPaymentMode.setSelection(0); // Default नगद
    }

    private void addItemToCart() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = spProducts.getSelectedItemPosition();
        Product selectedProduct = productList.get(selectedIndex);

        String qtyStr = etQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty = Double.parseDouble(qtyStr);
        if (qty <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = false;
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem item = cartItems.get(i);
            if (item.getProductId() == selectedProduct.getId()) {
                item.setQuantity(item.getQuantity() + qty);
                exists = true;
                break;
            }
        }

        if (!exists) {
            double total = qty * selectedProduct.getSellingPrice();
            SaleItem item = new SaleItem(selectedProduct.getId(), selectedProduct.getName(), qty, selectedProduct.getSellingPrice(), total, selectedProduct.getUnit());
            cartItems.add(item);
        }

        cartAdapter.notifyDataSetChanged();
        calculateTotals();
        etQty.setText("1");
    }

    private void calculateTotals() {
        double subtotal = 0.0;
        for (int i = 0; i < cartItems.size(); i++) {
            subtotal += cartItems.get(i).getTotalPrice();
        }

        String discStr = etDiscount.getText().toString().trim();
        double discount = discStr.isEmpty() ? 0.0 : Double.parseDouble(discStr);
        double grandTotal = Math.max(0, subtotal - discount);

        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %.2f", subtotal));
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", grandTotal));
    }

    private void completeSale() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Please add items to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        int custIdx = spCustomers.getSelectedItemPosition();
        Customer cust = customerList.get(custIdx);
        int modeIdx = spPaymentMode.getSelectedItemPosition();
        boolean isCreditSale = (modeIdx == 1); // "उधार (Credit)"

        // Rule validation: If उधार is selected, customer selection is required
        if (isCreditSale && cust.getId() == 0) {
            Toast.makeText(this, "उधार बिक्री के लिए ग्राहक चुनना अनिवार्य है!", Toast.LENGTH_LONG).show();
            return;
        }

        double subtotal = 0.0;
        for (int i = 0; i < cartItems.size(); i++) {
            subtotal += cartItems.get(i).getTotalPrice();
        }
        String discStr = etDiscount.getText().toString().trim();
        double discount = discStr.isEmpty() ? 0.0 : Double.parseDouble(discStr);
        double grandTotal = Math.max(0, subtotal - discount);

        String dbPaymentMode = isCreditSale ? "Udhaar" : "Cash";

        long saleId = dbHelper.processSale(cust.getId(), cust.getName(), subtotal, discount, grandTotal, dbPaymentMode, cartItems);

        Toast.makeText(this, (isCreditSale ? "उधार " : "नगद ") + "बिक्री #" + saleId + " दर्ज की गई!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, SaleBillDetailActivity.class);
        intent.putExtra("sale_id", saleId);
        startActivity(intent);
        finish();
    }

    private class CartAdapter extends BaseAdapter {
        @Override
        public int getCount() { return cartItems.size(); }

        @Override
        public Object getItem(int position) { return cartItems.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(NewSaleActivity.this).inflate(R.layout.item_cart, parent, false);
            }

            final SaleItem item = cartItems.get(position);
            TextView tvName = (TextView) convertView.findViewById(R.id.cart_product_name);
            TextView tvDetails = (TextView) convertView.findViewById(R.id.cart_details);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.cart_total_price);
            Button btnRemove = (Button) convertView.findViewById(R.id.btn_remove_cart_item);

            tvName.setText(item.getProductName());
            tvDetails.setText(String.format(Locale.getDefault(), "%.2f %s @ ₹%.2f", item.getQuantity(), item.getUnit(), item.getUnitPrice()));
            tvPrice.setText(String.format(Locale.getDefault(), "₹ %.2f", item.getTotalPrice()));

            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cartItems.remove(position);
                    notifyDataSetChanged();
                    calculateTotals();
                }
            });

            return convertView;
        }
    }
}