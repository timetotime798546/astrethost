package com.shozicapremor.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends Activity {

    public static class HistoricOrder {
        public String shoeName;
        public double price;
        public int quantity;
        public String size;
        public String imageUrl;
        public double totalPrice;

        public HistoricOrder(String shoeName, double price, int quantity, String size, String imageUrl, double totalPrice) {
            this.shoeName = shoeName;
            this.price = price;
            this.quantity = quantity;
            this.size = size;
            this.imageUrl = imageUrl;
            this.totalPrice = totalPrice;
        }
    }

    private ListView listOrders;
    private TextView tvOrdersEmpty;
    private OrderAdapter adapter;
    private List<HistoricOrder> orderHistoryList = new ArrayList<>();
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        api = new BackendApi(this);
        listOrders = (ListView) findViewById(R.id.listOrders);
        tvOrdersEmpty = (TextView) findViewById(R.id.tvOrdersEmpty);

        adapter = new OrderAdapter(this, orderHistoryList);
        listOrders.setAdapter(adapter);

        findViewById(R.id.btnOrdersBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        api.readOrders(new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(final JSONArray records) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            orderHistoryList.clear();

                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                // READ parses records[] and each record.data according to rule 11
                                if (record.has("data")) {
                                    JSONObject data = record.getJSONObject("data");
                                    HistoricOrder ord = new HistoricOrder(
                                        data.optString("shoe_name", data.optString("product_name", "Unknown premium shoe")),
                                        data.optDouble("price", 0.0),
                                        data.optInt("quantity", 1),
                                        data.optString("size", "N/A"),
                                        data.optString("image_url", ""),
                                        data.optDouble("total_price", data.optDouble("total", 0.0))
                                    );
                                    orderHistoryList.add(ord);
                                }
                            }

                            if (orderHistoryList.isEmpty()) {
                                tvOrdersEmpty.setVisibility(View.VISIBLE);
                            } else {
                                tvOrdersEmpty.setVisibility(View.GONE);
                            }
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Toast.makeText(OrdersActivity.this, "Error processing order catalog.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(OrdersActivity.this, "Offline mode or sync issue.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private class OrderAdapter extends BaseAdapter {
        private final Context context;
        private final List<HistoricOrder> list;

        public OrderAdapter(Context context, List<HistoricOrder> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }
        @Override
        public Object getItem(int position) { return list.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
            }

            HistoricOrder order = list.get(position);

            ImageView ivThumb = (ImageView) convertView.findViewById(R.id.ivOrderItemThumb);
            TextView tvName = (TextView) convertView.findViewById(R.id.tvOrderItemName);
            TextView tvSpecs = (TextView) convertView.findViewById(R.id.tvOrderItemSpecs);
            TextView tvTotal = (TextView) convertView.findViewById(R.id.tvOrderItemTotal);

            tvName.setText(order.shoeName);
            tvSpecs.setText("Size: " + order.size + "  |  Qty: " + order.quantity);

            // Local formatting for dynamic calculation presentation
            double displayTotal = order.totalPrice;
            if (displayTotal <= 0.0) {
                displayTotal = order.price * order.quantity;
            }
            tvTotal.setText("Atelier Total: $" + String.format("%.2f", displayTotal));

            ImageLoader.loadImage(order.imageUrl, ivThumb);

            return convertView;
        }
    }
}