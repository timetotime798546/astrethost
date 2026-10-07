package com.shozicapremor.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartActivity extends Activity {

    private TextView btnBack;
    private ListView listCart;
    private TextView calcSubtotal, calcTax, calcShipping, calcGrandTotal, calcDiscount;
    private RelativeLayout rowDiscount;
    private Button btnPlaceOrder;
    private ProgressBar progressCheckout;

    private String cartItemName = "";
    private double cartItemPrice = 0.0;
    private String cartItemSize = "9";
    private String cartItemColor = "Gold";

    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        api = new BackendApi(this);

        btnBack = findViewById(R.id.btn_cart_back);
        listCart = findViewById(R.id.list_cart);
        calcSubtotal = findViewById(R.id.calc_subtotal);
        calcTax = findViewById(R.id.calc_tax);
        calcShipping = findViewById(R.id.calc_shipping);
        calcDiscount = findViewById(R.id.calc_discount);
        rowDiscount = findViewById(R.id.row_discount);
        calcGrandTotal = findViewById(R.id.calc_grand_total);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        progressCheckout = findViewById(R.id.progress_checkout);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadCartFromPrefs();
        runOfflinePricingCalculations();

        btnPlaceOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCheckoutOrder();
            }
        });
    }

    private void loadCartFromPrefs() {
        SharedPreferences cartPrefs = getSharedPreferences("CartPrefs", MODE_PRIVATE);
        cartItemName = cartPrefs.getString("item_name", "");
        cartItemPrice = cartPrefs.getFloat("item_price", 0.0f);
        cartItemSize = cartPrefs.getString("item_size", "9");
        cartItemColor = cartPrefs.getString("item_color", "Gold");

        List<String> items = new ArrayList<>();
        if (!cartItemName.isEmpty()) {
            items.add(cartItemName);
        }

        listCart.setAdapter(new SimpleCartAdapter(this, items, cartItemSize, cartItemColor, cartItemPrice));
    }

    /**
     * MANDATORY OFFLINE CALCULATION AND BUSINESS LOGIC
     * Runs natively in pure Java. No remote endpoint calculations.
     */
    private void runOfflinePricingCalculations() {
        if (cartItemName.isEmpty()) {
            calcSubtotal.setText("$0.00");
            calcTax.setText("$0.00");
            rowDiscount.setVisibility(View.GONE);
            calcGrandTotal.setText("$0.00");
            btnPlaceOrder.setEnabled(false);
            return;
        }

        double subtotal = cartItemPrice * 1.0; // Qty 1
        double tax = subtotal * 0.08; // 8% local state tax fraction
        double shipping = 15.00; // Flat Premium Courier Fee
        double discount = 0.0;

        // Loyalty calculation discount logic: if exceeds $150, apply 10% discount
        if (subtotal > 150.00) {
            discount = subtotal * 0.10;
            rowDiscount.setVisibility(View.VISIBLE);
            calcDiscount.setText("-$" + String.format("%.2f", discount));
        } else {
            rowDiscount.setVisibility(View.GONE);
        }

        double grandTotal = subtotal + tax + shipping - discount;

        calcSubtotal.setText("$" + String.format("%.2f", subtotal));
        calcTax.setText("$" + String.format("%.2f", tax));
        calcShipping.setText("$" + String.format("%.2f", shipping));
        calcGrandTotal.setText("$" + String.format("%.2f", grandTotal));
        btnPlaceOrder.setEnabled(true);
    }

    private void processCheckoutOrder() {
        if (cartItemName.isEmpty()) return;

        progressCheckout.setVisibility(View.VISIBLE);
        btnPlaceOrder.setVisibility(View.GONE);

        // Calculate values for submission
        double subtotal = cartItemPrice;
        double tax = subtotal * 0.08;
        double discount = subtotal > 150.00 ? subtotal * 0.10 : 0.0;
        double total = subtotal + tax + 15.00 - discount;
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        try {
            final JSONObject orderObj = new JSONObject();
            orderObj.put("product_name", cartItemName);
            orderObj.put("price", cartItemPrice);
            orderObj.put("quantity", 1);
            orderObj.put("shoe_size", cartItemSize);
            orderObj.put("shoe_color", cartItemColor);
            orderObj.put("subtotal", subtotal);
            orderObj.put("tax", tax);
            orderObj.put("discount", discount);
            orderObj.put("total", total);
            orderObj.put("order_date", dateStr);

            api.saveOrder(orderObj, new BackendApi.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressCheckout.setVisibility(View.GONE);
                            btnPlaceOrder.setVisibility(View.VISIBLE);

                            // Flush cart on success
                            getSharedPreferences("CartPrefs", MODE_PRIVATE).edit().clear().apply();
                            Toast.makeText(CartActivity.this, "Premium Order synchronized with the Cloud successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressCheckout.setVisibility(View.GONE);
                            btnPlaceOrder.setVisibility(View.VISIBLE);
                            Toast.makeText(CartActivity.this, "Cloud Sync Issue: " + error + ". Order retained offline.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            progressCheckout.setVisibility(View.GONE);
            btnPlaceOrder.setVisibility(View.VISIBLE);
        }
    }

    private static class SimpleCartAdapter extends BaseAdapter {
        private final Context context;
        private final List<String> list;
        private final String size;
        private final String color;
        private final double price;

        public SimpleCartAdapter(Context context, List<String> list, String size, String color, double price) {
            this.context = context;
            this.list = list;
            this.size = size;
            this.color = color;
            this.price = price;
        }

        @Override
        public int getCount() { return list.size(); }
        @Override
        public Object getItem(int pos) { return list.get(pos); }
        @Override
        public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_cart_order, parent, false);
            }

            TextView name = convertView.findViewById(R.id.order_name);
            TextView specs = convertView.findViewById(R.id.order_specs);
            TextView dateText = convertView.findViewById(R.id.order_date);
            TextView cost = convertView.findViewById(R.id.order_cost);
            TextView qty = convertView.findViewById(R.id.order_qty);

            name.setText(list.get(pos));
            specs.setText("Size: " + size + " | Color: " + color);
            dateText.setText("Status: Pending checkout confirmation");
            cost.setText("$" + String.format("%.2f", price));
            qty.setText("Qty: 1");

            return convertView;
        }
    }
}