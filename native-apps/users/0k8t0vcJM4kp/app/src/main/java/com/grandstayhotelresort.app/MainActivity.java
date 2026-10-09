package com.grandstayhotelresort.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView tvGreeting, tvLogout;
    private Button btnMyBookings, btnClearFilter;
    private EditText etSearch, etMaxPrice;
    private LinearLayout containerRooms;
    private BackendApi api;

    public static final Room[] ROOMS = new Room[]{
        new Room(
            "Grand Presidential Suite",
            "Experience royal luxury with panoramic ocean views, a private jacuzzi, and customizable room service.",
            350.0,
            4.9,
            new String[]{
                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=600",
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600",
                "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=600"
            }
        ),
        new Room(
            "Deluxe Ocean View Room",
            "Elegant room facing the pristine blue coast with a private balcony, king-sized bed, and modern amenities.",
            220.0,
            4.7,
            new String[]{
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=600",
                "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=600",
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=600"
            }
        ),
        new Room(
            "Executive Garden Oasis",
            "Serene garden-facing suite surrounded by tropical blooms, complete with rainfall shower and a workspace.",
            180.0,
            4.6,
            new String[]{
                "https://images.unsplash.com/photo-1591088398332-8a7791972843?w=600",
                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600",
                "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=600"
            }
        ),
        new Room(
            "Premium Family Haven",
            "Spacious dual-bedroom setup designed for family comfort, featuring custom kid-friendly details.",
            290.0,
            4.8,
            new String[]{
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=600",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=600"
            }
        )
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        tvGreeting = (TextView) findViewById(R.id.tvGreeting);
        tvLogout = (TextView) findViewById(R.id.tvLogout);
        btnMyBookings = (Button) findViewById(R.id.btnMyBookings);
        btnClearFilter = (Button) findViewById(R.id.btnClearFilter);
        etSearch = (EditText) findViewById(R.id.etSearch);
        etMaxPrice = (EditText) findViewById(R.id.etMaxPrice);
        containerRooms = (LinearLayout) findViewById(R.id.containerRooms);

        tvGreeting.setText("Welcome, " + api.getGuestName());

        btnMyBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MyBookingsActivity.class));
            }
        });

        tvLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        Toast.makeText(MainActivity.this, "Session closed.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });

        btnClearFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
                etMaxPrice.setText("");
                renderRooms();
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderRooms();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etSearch.addTextChangedListener(watcher);
        etMaxPrice.addTextChangedListener(watcher);

        renderRooms();
    }

    private void renderRooms() {
        containerRooms.removeAllViews();

        String query = etSearch.getText().toString().trim().toLowerCase();
        String maxPriceStr = etMaxPrice.getText().toString().trim();
        double maxPrice = Double.MAX_VALUE;
        if (!maxPriceStr.isEmpty()) {
            try {
                maxPrice = Double.parseDouble(maxPriceStr);
            } catch (Exception e) {}
        }

        for (int i = 0; i < ROOMS.length; i++) {
            final Room room = ROOMS[i];
            final int index = i;

            if (!room.name.toLowerCase().contains(query) && !room.description.toLowerCase().contains(query)) {
                continue;
            }

            if (room.pricePerNight > maxPrice) {
                continue;
            }

            View card = LayoutInflater.from(this).inflate(android.R.layout.activity_list_item, null);
            
            LinearLayout outerLayout = new LinearLayout(this);
            outerLayout.setOrientation(LinearLayout.VERTICAL);
            outerLayout.setBackgroundColor(0xFFFFFFFF);
            
            // Set margins and backgrounds
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 32);
            outerLayout.setLayoutParams(lp);
            outerLayout.setPadding(24, 24, 24, 24);

            // Thumbnail Image representing 1 Image (Single Image Rule Context)
            ImageView thumbnail = new ImageView(this);
            thumbnail.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350));
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.loadImage(room.imageUrls[0], thumbnail);
            outerLayout.addView(thumbnail);

            // Title & Pricing Row
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setPadding(0, 16, 0, 8);
            
            TextView nameTv = new TextView(this);
            nameTv.setText(room.name);
            nameTv.setTextColor(0xFF1F2E3D);
            nameTv.setTextSize(18);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleRow.addView(nameTv, nameLp);

            TextView priceTv = new TextView(this);
            priceTv.setText("$" + (int) room.pricePerNight + "/Night");
            priceTv.setTextColor(0xFFD4AF37);
            priceTv.setTextSize(16);
            priceTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleRow.addView(priceTv);
            
            outerLayout.addView(titleRow);

            // Star Rating
            TextView ratingTv = new TextView(this);
            ratingTv.setText("★ " + room.rating + " / 5.0 Rating");
            ratingTv.setTextColor(0xFFD4AF37);
            ratingTv.setTextSize(12);
            ratingTv.setPadding(0, 0, 0, 12);
            outerLayout.addView(ratingTv);

            // Description text
            TextView descTv = new TextView(this);
            descTv.setText(room.description);
            descTv.setTextColor(0xFF666666);
            descTv.setTextSize(13);
            descTv.setPadding(0, 0, 0, 16);
            outerLayout.addView(descTv);

            // View Room Button
            Button actionBtn = new Button(this);
            actionBtn.setText("VIEW DETAILS & BOOK");
            actionBtn.setTextColor(0xFFFFFFFF);
            actionBtn.setBackgroundResource(R.drawable.button_gold);
            actionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, RoomDetailsActivity.class);
                    intent.putExtra("ROOM_INDEX", index);
                    startActivity(intent);
                }
            });
            outerLayout.addView(actionBtn);

            containerRooms.addView(outerLayout);
        }
    }
}