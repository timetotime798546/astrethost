package com.barberq.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private CheckBox cbHaircut, cbShaving, cbBeardTrim, cbFacial, cbHairSpa;
    private ImageView ivHaircut, ivShaving, ivBeardTrim, ivFacial, ivHairSpa;
    private TextView tvTotalPrice;
    private Button btnProceed, btnHistory;
    private ImageView btnLogout;

    private int totalPrice = 0;
    private ArrayList<String> selectedServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedServices = new ArrayList<>();

        // Initialize checkboxes
        cbHaircut = (CheckBox) findViewById(R.id.cbHaircut);
        cbShaving = (CheckBox) findViewById(R.id.cbShaving);
        cbBeardTrim = (CheckBox) findViewById(R.id.cbBeardTrim);
        cbFacial = (CheckBox) findViewById(R.id.cbFacial);
        cbHairSpa = (CheckBox) findViewById(R.id.cbHairSpa);

        // Initialize Service Catalog ImageViews
        ivHaircut = (ImageView) findViewById(R.id.ivHaircut);
        ivShaving = (ImageView) findViewById(R.id.ivShaving);
        ivBeardTrim = (ImageView) findViewById(R.id.ivBeardTrim);
        ivFacial = (ImageView) findViewById(R.id.ivFacial);
        ivHairSpa = (ImageView) findViewById(R.id.ivHairSpa);

        tvTotalPrice = (TextView) findViewById(R.id.tvTotalPrice);
        btnProceed = (Button) findViewById(R.id.btnProceed);
        btnHistory = (Button) findViewById(R.id.btnHistory);
        btnLogout = (ImageView) findViewById(R.id.btnLogout);

        // Load images asynchronously from standard high quality Unsplash links
        loadImageFromUrl("https://images.unsplash.com/photo-1585747860715-2ba37e788b70?q=80&w=300&auto=format&fit=crop", ivHaircut);
        loadImageFromUrl("https://images.unsplash.com/photo-1621605815971-fbc98d665033?q=80&w=300&auto=format&fit=crop", ivShaving);
        loadImageFromUrl("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?q=80&w=300&auto=format&fit=crop", ivBeardTrim);
        loadImageFromUrl("https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?q=80&w=300&auto=format&fit=crop", ivFacial);
        loadImageFromUrl("https://images.unsplash.com/photo-1562322140-8baeececf3df?q=80&w=300&auto=format&fit=crop", ivHairSpa);

        // Checkbox event listeners
        CompoundButton.OnCheckedChangeListener changeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateTotal();
            }
        };

        cbHaircut.setOnCheckedChangeListener(changeListener);
        cbShaving.setOnCheckedChangeListener(changeListener);
        cbBeardTrim.setOnCheckedChangeListener(changeListener);
        cbFacial.setOnCheckedChangeListener(changeListener);
        cbHairSpa.setOnCheckedChangeListener(changeListener);

        // Actions
        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (totalPrice == 0 || selectedServices.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Select at least one treatment first.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Pack details & launch scheduler
                StringBuilder servicesText = new StringBuilder();
                for (int i = 0; i < selectedServices.size(); i++) {
                    servicesText.append(selectedServices.get(i));
                    if (i < selectedServices.size() - 1) {
                        servicesText.append(", ");
                    }
                }

                Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                intent.putExtra("services", servicesText.toString());
                intent.putExtra("totalPrice", (double) totalPrice);
                startActivity(intent);
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("BarberQPrefs", MODE_PRIVATE);
                prefs.edit().remove("token").apply();
                Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void calculateTotal() {
        totalPrice = 0;
        selectedServices.clear();

        if (cbHaircut.isChecked()) {
            totalPrice += 150;
            selectedServices.add("Classic Haircut (₹150)");
        }
        if (cbShaving.isChecked()) {
            totalPrice += 80;
            selectedServices.add("Classic Shave (₹80)");
        }
        if (cbBeardTrim.isChecked()) {
            totalPrice += 100;
            selectedServices.add("Beard Trim (₹100)");
        }
        if (cbFacial.isChecked()) {
            totalPrice += 300;
            selectedServices.add("Royal Facial (₹300)");
        }
        if (cbHairSpa.isChecked()) {
            totalPrice += 250;
            selectedServices.add("Premium Hair Spa (₹250)");
        }

        tvTotalPrice.setText("₹" + totalPrice);
    }

    private void loadImageFromUrl(final String urlString, final ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}