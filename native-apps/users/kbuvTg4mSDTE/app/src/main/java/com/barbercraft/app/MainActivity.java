package com.barbercraft.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private BackendApi backendApi;
    private TextView tvWelcomeUser;
    private Button btnAuthAction;
    private Button btnBookings;
    private LinearLayout servicesContainer;
    private List<ServiceItem> serviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvWelcomeUser = (TextView) findViewById(R.id.tvWelcomeUser);
        btnAuthAction = (Button) findViewById(R.id.btnAuthAction);
        btnBookings = (Button) findViewById(R.id.btnBookings);
        servicesContainer = (LinearLayout) findViewById(R.id.servicesContainer);

        initSampleServices();
        renderServicesList();

        btnAuthAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    backendApi.logout(new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                            updateAuthUI();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Session cleared", Toast.LENGTH_SHORT).show();
                            updateAuthUI();
                        }
                    });
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        btnBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!backendApi.isLoggedIn()) {
                    Toast.makeText(MainActivity.this, "Please sign in to view your bookings", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, AppointmentsListActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthUI();
    }

    private void updateAuthUI() {
        if (backendApi.isLoggedIn()) {
            tvWelcomeUser.setText("Welcome, " + backendApi.getUserEmail());
            btnAuthAction.setText("Logout");
        } else {
            tvWelcomeUser.setText("Welcome to BarberCraft");
            btnAuthAction.setText("Login");
        }
    }

    private void initSampleServices() {
        serviceList = new ArrayList<ServiceItem>();

        // Haircut Service - Single thumbnail for list + 3 dynamic photos for details
        serviceList.add(new ServiceItem(
                "srv_1",
                "Executive Fade & Scissor Cut",
                "Precision fade, shear work on top, refreshing scalp massage, and straight razor neck clean.",
                38.00,
                35,
                "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=400&auto=format&fit=crop&q=80",
                new String[]{
                        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=800&auto=format&fit=crop&q=80"
                }
        ));

        // Shave Service - Hot towel shave
        serviceList.add(new ServiceItem(
                "srv_2",
                "Traditional Hot Towel Shave",
                "Steamed botanical towels, warm artisan lather, double-pass straight razor shave, and soothing balm.",
                32.00,
                30,
                "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=400&auto=format&fit=crop&q=80",
                new String[]{
                        "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1517832606589-7629c339590a?w=800&auto=format&fit=crop&q=80"
                }
        ));

        // Beard Styling
        serviceList.add(new ServiceItem(
                "srv_3",
                "Beard Sculpting & Conditioning",
                "Custom beard shaping, cheek line razor definition, organic beard oil hydration, and ozone steam.",
                25.00,
                25,
                "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=400&auto=format&fit=crop&q=80",
                new String[]{
                        "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800&auto=format&fit=crop&q=80"
                }
        ));

        // Deluxe Grooming Package
        serviceList.add(new ServiceItem(
                "srv_4",
                "Royal Craft Grooming Combo",
                "Full artisan haircut, facial charcoal mask, beard sculpt or hot lather shave, plus revitalizing tonic.",
                65.00,
                60,
                "https://images.unsplash.com/photo-1517832606589-7629c339590a?w=400&auto=format&fit=crop&q=80",
                new String[]{
                        "https://images.unsplash.com/photo-1517832606589-7629c339590a?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=800&auto=format&fit=crop&q=80",
                        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=800&auto=format&fit=crop&q=80"
                }
        ));
    }

    private void renderServicesList() {
        servicesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < serviceList.size(); i++) {
            final ServiceItem item = serviceList.get(i);
            View row = inflater.inflate(R.layout.item_service, servicesContainer, false);

            ImageView ivThumbnail = (ImageView) row.findViewById(R.id.ivServiceThumbnail);
            TextView tvName = (TextView) row.findViewById(R.id.tvServiceName);
            TextView tvDesc = (TextView) row.findViewById(R.id.tvServiceDescription);
            TextView tvPrice = (TextView) row.findViewById(R.id.tvServicePrice);
            TextView tvDuration = (TextView) row.findViewById(R.id.tvServiceDuration);
            Button btnView = (Button) row.findViewById(R.id.btnViewDetails);

            tvName.setText(item.getName());
            tvDesc.setText(item.getDescription());
            tvPrice.setText("$" + String.format("%.2f", item.getPrice()));
            tvDuration.setText(item.getDurationMinutes() + " mins");

            // Adaptive single image loading context
            ImageLoader.getInstance().displayImage(item.getThumbnailUrl(), ivThumbnail);

            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ServiceDetailActivity.class);
                    intent.putExtra("service_item", item);
                    startActivity(intent);
                }
            });

            servicesContainer.addView(row);
        }
    }
}