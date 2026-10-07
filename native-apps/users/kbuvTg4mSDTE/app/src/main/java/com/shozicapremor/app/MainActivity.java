package com.shozicapremor.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ImageView btnCart;
    private TextView tabShop, tabOrders, tabProfile;
    private LinearLayout panelShop, panelOrders, panelProfile;
    private TextView profileEmail, profileAppId;
    private Button btnLogout;

    // Shop fields
    private TextView catAll, catRunning, catCasual, catFormal;
    private ListView listShoes;
    private List<Product> catalogList = new ArrayList<>();
    private List<Product> displayList = new ArrayList<>();
    private ShoeAdapter shoeAdapter;

    // Orders fields
    private ProgressBar progressOrders;
    private TextView emptyOrdersText;
    private ListView listOrders;
    private List<JSONObject> fetchedOrders = new ArrayList<>();
    private OrderAdapter orderAdapter;

    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        btnCart = findViewById(R.id.btn_cart);
        tabShop = findViewById(R.id.tab_shop);
        tabOrders = findViewById(R.id.tab_orders);
        tabProfile = findViewById(R.id.tab_profile);

        panelShop = findViewById(R.id.panel_shop);
        panelOrders = findViewById(R.id.panel_orders);
        panelProfile = findViewById(R.id.panel_profile);

        profileEmail = findViewById(R.id.profile_email);
        profileAppId = findViewById(R.id.profile_app_id);
        btnLogout = findViewById(R.id.btn_logout);

        // Shop panels
        catAll = findViewById(R.id.cat_all);
        catRunning = findViewById(R.id.cat_running);
        catCasual = findViewById(R.id.cat_casual);
        catFormal = findViewById(R.id.cat_formal);
        listShoes = findViewById(R.id.list_shoes);

        // Orders lists
        progressOrders = findViewById(R.id.progress_orders);
        emptyOrdersText = findViewById(R.id.empty_orders_text);
        listOrders = findViewById(R.id.list_orders);

        initCatalog();
        setupTabs();
        setupCategoryFilters();

        profileEmail.setText(api.getSavedEmail());

        btnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.clearAuth();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Setup ListView adapter
        shoeAdapter = new ShoeAdapter(this, displayList);
        listShoes.setAdapter(shoeAdapter);
        listShoes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product clicked = displayList.get(position);
                Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                intent.putExtra("shoe_id", clicked.getId());
                startActivity(intent);
            }
        });
    }

    private void initCatalog() {
        catalogList.clear();

        // Shoe 1: Air Zoom Max (Running)
        catalogList.add(new Product("1", "Air Zoom Max Extreme", "Running", 185.00,
                "Elevate your stride. Boasts dual responsive nitrogen-infused Zoom capsules coupled with customized carbon-fiber flight plate geometry.",
                new String[]{
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=500&q=80"
                }));

        // Shoe 2: Retro Sneaker Premium (Casual)
        catalogList.add(new Product("2", "Retro Sneaker Gold-Elite", "Casual", 130.00,
                "Vibrant custom retro aesthetics merged with premium durable leather trims and an Ortholite high-density comfort sockliner for all-day style.",
                new String[]{
                        "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=500&q=80"
                }));

        // Shoe 3: Royal Leather Oxford (Formal)
        catalogList.add(new Product("3", "Royal Oxford Classique", "Formal", 240.00,
                "Handcrafted luxury. Features full grain premium Italian leather uppers, precise Goodyear welted leather soles, and rich golden inner lining.",
                new String[]{
                        "https://images.unsplash.com/photo-1533867617858-e7b97e060509?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1614252369475-531eba835eb1?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1481841587433-6226118991c3?auto=format&fit=crop&w=500&q=80"
                }));

        // Shoe 4: Elite Trail Runner
        catalogList.add(new Product("4", "Elite Mountain Trail Runner", "Running", 195.00,
                "Conquer rough mountain paths with engineered water-repellant ripstop fabric and deep lugged sticky Vibram outsoles designed for maximum traction.",
                new String[]{
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80",
                        "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=500&q=80"
                }));

        displayList.addAll(catalogList);
    }

    public Product getProductById(String id) {
        for (Product p : catalogList) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private void setupTabs() {
        tabShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        tabOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        tabProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int index) {
        tabShop.setTextColor(getResources().getColor(index == 0 ? R.color.accent_gold : R.color.text_secondary));
        tabOrders.setTextColor(getResources().getColor(index == 1 ? R.color.accent_gold : R.color.text_secondary));
        tabProfile.setTextColor(getResources().getColor(index == 2 ? R.color.accent_gold : R.color.text_secondary));

        panelShop.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelOrders.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelProfile.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (index == 1) {
            fetchOrderHistory();
        }
    }

    private void setupCategoryFilters() {
        catAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory(catAll, "All");
            }
        });
        catRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory(catRunning, "Running");
            }
        });
        catCasual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory(catCasual, "Casual");
            }
        });
        catFormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory(catFormal, "Formal");
            }
        });
    }

    private void selectCategory(TextView selectedView, String category) {
        // Reset colors
        TextView[] views = {catAll, catRunning, catCasual, catFormal};
        for (TextView tv : views) {
            tv.setBackgroundColor(getResources().getColor(R.color.bg_card));
            tv.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        selectedView.setBackgroundColor(getResources().getColor(R.color.accent_gold));
        selectedView.setTextColor(getResources().getColor(R.color.bg_dark));

        displayList.clear();
        if (category.equals("All")) {
            displayList.addAll(catalogList);
        } else {
            for (Product p : catalogList) {
                if (p.getCategory().equalsIgnoreCase(category)) {
                    displayList.add(p);
                }
            }
        }
        shoeAdapter.notifyDataSetChanged();
    }

    private void fetchOrderHistory() {
        progressOrders.setVisibility(View.VISIBLE);
        emptyOrdersText.setVisibility(View.GONE);
        listOrders.setVisibility(View.GONE);

        api.readOrders(new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(final JSONArray records) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressOrders.setVisibility(View.GONE);
                        fetchedOrders.clear();
                        try {
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                // The endpoint structures each item within a "data" nested envelope
                                if (record.has("data")) {
                                    JSONObject data = record.getJSONObject("data");
                                    fetchedOrders.add(data);
                                }
                            }

                            if (fetchedOrders.isEmpty()) {
                                emptyOrdersText.setVisibility(View.VISIBLE);
                            } else {
                                emptyOrdersText.setVisibility(View.GONE);
                                listOrders.setVisibility(View.VISIBLE);
                                if (orderAdapter == null) {
                                    orderAdapter = new OrderAdapter(MainActivity.this, fetchedOrders);
                                    listOrders.setAdapter(orderAdapter);
                                } else {
                                    orderAdapter.notifyDataSetChanged();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            emptyOrdersText.setText("Parsing failed: " + e.getMessage());
                            emptyOrdersText.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressOrders.setVisibility(View.GONE);
                        emptyOrdersText.setText("Sync offline or error: " + error);
                        emptyOrdersText.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private static class ShoeAdapter extends BaseAdapter {
        private final Context context;
        private final List<Product> list;

        public ShoeAdapter(Context context, List<Product> list) {
            this.context = context;
            this.list = list;
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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_shoe, parent, false);
            }

            ImageView img = convertView.findViewById(R.id.shoe_image);
            TextView name = convertView.findViewById(R.id.shoe_name);
            TextView cat = convertView.findViewById(R.id.shoe_category);
            TextView price = convertView.findViewById(R.id.shoe_price);

            Product item = list.get(pos);

            name.setText(item.getName());
            cat.setText(item.getCategory().toUpperCase());
            price.setText("$" + String.format("%.2f", item.getPrice()));

            if (item.getImageUrls().length > 0) {
                ImageLoader.displayImage(item.getImageUrls()[0], img);
            }

            return convertView;
        }
    }

    private static class OrderAdapter extends BaseAdapter {
        private final Context context;
        private final List<JSONObject> list;

        public OrderAdapter(Context context, List<JSONObject> list) {
            this.context = context;
            this.list = list;
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

            try {
                JSONObject data = list.get(pos);
                name.setText(data.optString("product_name", "Shozica Footwear"));
                specs.setText("Size: " + data.optString("shoe_size", "9") + " | " + data.optString("shoe_color", "Gold"));
                dateText.setText("Date: " + data.optString("order_date", "Recently"));
                cost.setText("$" + String.format("%.2f", data.optDouble("total", 0.00)));
                qty.setText("Qty: " + data.optInt("quantity", 1));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}