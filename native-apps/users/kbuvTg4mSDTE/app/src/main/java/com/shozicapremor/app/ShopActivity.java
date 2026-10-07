package com.shozicapremor.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends Activity {

    public static class Shoe {
        public String name;
        public String category;
        public double price;
        public String description;
        public String[] images; // MULTIPLE IMAGES PER PRODUCT

        public Shoe(String name, String category, double price, String description, String[] images) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.description = description;
            this.images = images;
        }
    }

    public static List<Shoe> shoeDatabase = new ArrayList<>();
    static {
        shoeDatabase.add(new Shoe("Air Max Sport", "Performance Sport", 120.0, 
            "Engineered to unleash maximum athleticism. Features multi-layered cushioned sole with reactive micro-pods for flawless street energy.", 
            new String[]{
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?auto=format&fit=crop&w=600&q=80"
            }));

        shoeDatabase.add(new Shoe("Classic Leather", "Classic Leather", 85.0, 
            "The timeless retro silhouette crafted from supple tanned leather. Combines absolute class with durable performance stitching.", 
            new String[]{
                "https://images.unsplash.com/photo-1491553895911-0055eca6402d?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=600&q=80"
            }));

        shoeDatabase.add(new Shoe("Ultraboost Run", "Performance Sport", 180.0, 
            "Ultimate elite runner with precision dynamic fit. The perfect footwear for tracks and everyday sports requirements.", 
            new String[]{
                "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1508180589062-f1dc3e1eeaa4?auto=format&fit=crop&w=600&q=80"
            }));

        shoeDatabase.add(new Shoe("Retro High-Top", "Classic Leather", 150.0, 
            "Premium court design reimagined for luxury street culture. Outstanding high ankle support with durable premium compound rubber base.", 
            new String[]{
                "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=600&q=80"
            }));

        shoeDatabase.add(new Shoe("Urban Walker", "Urban Walker", 95.0, 
            "Extremely lightweight casual footwear designed for everyday luxury walks. Breathable weave mesh with premium styled loops.", 
            new String[]{
                "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=600&q=80"
            }));
    }

    private List<Shoe> filteredShoes = new ArrayList<>(shoeDatabase);
    private GridView gridShoes;
    private ShoeAdapter adapter;
    private BackendApi api;
    private TextView tvCartBadge;
    private String activeCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        api = BackendApi.getInstance(getApplicationContext());
        gridShoes = (GridView) findViewById(R.id.gridShoes);
        tvCartBadge = (TextView) findViewById(R.id.tvCartBadge);

        adapter = new ShoeAdapter(this, filteredShoes);
        gridShoes.setAdapter(adapter);

        gridShoes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Shoe selected = filteredShoes.get(position);
                Intent intent = new Intent(ShopActivity.this, DetailActivity.class);
                intent.putExtra("shoe_index", shoeDatabase.indexOf(selected));
                startActivity(intent);
            }
        });

        // Cart Action
        findViewById(R.id.btnHeaderCart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShopActivity.this, CartActivity.class));
            }
        });

        // Order history Action
        findViewById(R.id.btnHeaderOrders).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShopActivity.this, OrdersActivity.class));
            }
        });

        // Search text watcher
        EditText etSearch = (EditText) findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filters Action Bar
        final Button btnCatAll = (Button) findViewById(R.id.btnCatAll);
        final Button btnCatSport = (Button) findViewById(R.id.btnCatSport);
        final Button btnCatClassic = (Button) findViewById(R.id.btnCatClassic);
        final Button btnCatCasual = (Button) findViewById(R.id.btnCatCasual);

        btnCatAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCategory = "All";
                setButtonSelected(btnCatAll, true);
                setButtonSelected(btnCatSport, false);
                setButtonSelected(btnCatClassic, false);
                setButtonSelected(btnCatCasual, false);
                filterData();
            }
        });

        btnCatSport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCategory = "Performance Sport";
                setButtonSelected(btnCatAll, false);
                setButtonSelected(btnCatSport, true);
                setButtonSelected(btnCatClassic, false);
                setButtonSelected(btnCatCasual, false);
                filterData();
            }
        });

        btnCatClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCategory = "Classic Leather";
                setButtonSelected(btnCatAll, false);
                setButtonSelected(btnCatSport, false);
                setButtonSelected(btnCatClassic, true);
                setButtonSelected(btnCatCasual, false);
                filterData();
            }
        });

        btnCatCasual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeCategory = "Urban Walker";
                setButtonSelected(btnCatAll, false);
                setButtonSelected(btnCatSport, false);
                setButtonSelected(btnCatClassic, false);
                setButtonSelected(btnCatCasual, true);
                filterData();
            }
        });

        findViewById(R.id.btnShopLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(ShopActivity.this);
                CartManager.getInstance().clear();
                startActivity(new Intent(ShopActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void setButtonSelected(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundColor(0xFF111111);
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundColor(0xFFFFFFFF);
            btn.setTextColor(0xFF333333);
        }
    }

    private void filterData() {
        filteredShoes.clear();
        for (Shoe shoe : shoeDatabase) {
            boolean matchesCat = activeCategory.equals("All") || shoe.category.equals(activeCategory);
            boolean matchesSearch = shoe.name.toLowerCase().contains(searchQuery) || shoe.description.toLowerCase().contains(searchQuery);
            if (matchesCat && matchesSearch) {
                filteredShoes.add(shoe);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update local cart badge count
        int count = 0;
        for (CartManager.CartItem item : CartManager.getInstance().getItems()) {
            count += item.quantity;
        }
        tvCartBadge.setText(String.valueOf(count));
    }

    private static class ShoeAdapter extends BaseAdapter {
        private final Context context;
        private final List<Shoe> list;

        public ShoeAdapter(Context context, List<Shoe> list) {
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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_shoe, parent, false);
            }

            Shoe shoe = list.get(position);

            ImageView ivThumb = (ImageView) convertView.findViewById(R.id.ivShoeThumbnail);
            TextView tvCat = (TextView) convertView.findViewById(R.id.tvShoeCat);
            TextView tvName = (TextView) convertView.findViewById(R.id.tvShoeName);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.tvShoePrice);

            tvCat.setText(shoe.category.toUpperCase());
            tvName.setText(shoe.name);
            tvPrice.setText("$" + String.format("%.2f", shoe.price));

            // Load primary preview image
            ImageLoader.loadImage(shoe.images[0], ivThumb);

            return convertView;
        }
    }
}