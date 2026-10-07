package com.barberq.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private CheckBox cbHaircut, cbShaving, cbBeardTrim, cbFacial, cbHairSpa;
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

        tvTotalPrice = (TextView) findViewById(R.id.tvTotalPrice);
        btnProceed = (Button) findViewById(R.id.btnProceed);
        btnHistory = (Button) findViewById(R.id.btnHistory);
        btnLogout = (ImageView) findViewById(R.id.btnLogout);

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
}