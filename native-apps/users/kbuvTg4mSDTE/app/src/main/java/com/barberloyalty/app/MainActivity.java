package com.barberloyalty.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvGreeting, tvEmailSub, tvPointsCount;
    private Button btnDailyCheckIn, btnBookHaircut, btnBookShave, btnLogout;
    private LinearLayout llBookingsContainer;

    private BackendApi backendApi;
    private String userEmail;
    private String userToken;
    private String profileRecordId = null;
    private int currentPoints = 100;

    // Fixed service definitions
    private static final String SERVICE_HAIRCUT = "Executive Haircut";
    private static final int COST_HAIRCUT = 30;

    private static final String SERVICE_SHAVE = "Royal Trim & Shave";
    private static final int COST_SHAVE = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        SharedPreferences prefs = getSharedPreferences("BarberPrefs", MODE_PRIVATE);
        userEmail = prefs.getString("email", null);
        userToken = prefs.getString("token", null);

        if (userEmail == null || userToken == null) {
            goToLogin();
            return;
        }

        backendApi.setToken(userToken);

        tvGreeting = (TextView) findViewById(R.id.tvGreeting);
        tvEmailSub = (TextView) findViewById(R.id.tvEmailSub);
        tvPointsCount = (TextView) findViewById(R.id.tvPointsCount);
        btnDailyCheckIn = (Button) findViewById(R.id.btnDailyCheckIn);
        btnBookHaircut = (Button) findViewById(R.id.btnBookHaircut);
        btnBookShave = (Button) findViewById(R.id.btnBookShave);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        llBookingsContainer = (LinearLayout) findViewById(R.id.llBookingsContainer);

        tvGreeting.setText("Welcome, Guest!");
        tvEmailSub.setText(userEmail);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnDailyCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyDailyBonus();
            }
        });

        btnBookHaircut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redeemService(SERVICE_HAIRCUT, COST_HAIRCUT);
            }
        });

        btnBookShave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redeemService(SERVICE_SHAVE, COST_SHAVE);
            }
        });

        // Initialize Dynamic Adaptive Images matching context
        loadCatalogPreviews();

        // Fetch User Wallet points
        fetchWalletProfile();

        // Fetch Bookings
        fetchBookings();
    }

    private void loadCatalogPreviews() {
        // Image view references for multi-image gallery styling
        ImageView ivH1 = (ImageView) findViewById(R.id.ivHaircut1);
        ImageView ivH2 = (ImageView) findViewById(R.id.ivHaircut2);
        ImageView ivH3 = (ImageView) findViewById(R.id.ivHaircut3);

        ImageView ivS1 = (ImageView) findViewById(R.id.ivShave1);
        ImageView ivS2 = (ImageView) findViewById(R.id.ivShave2);
        ImageView ivS3 = (ImageView) findViewById(R.id.ivShave3);

        // Fetch contextual Unsplash links dynamically
        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=250&q=80", ivH1);
        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1621605815971-fbc98d665033?auto=format&fit=crop&w=250&q=80", ivH2);
        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=250&q=80", ivH3);

        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1517832606589-7a59890bab71?auto=format&fit=crop&w=250&q=80", ivS1);
        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1599351431202-1e0f0137899a?auto=format&fit=crop&w=250&q=80", ivS2);
        SimpleImageDownloader.loadImage("https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?auto=format&fit=crop&w=250&q=80", ivS3);
    }

    private void fetchWalletProfile() {
        backendApi.readRecords("user_profiles", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray records = response.optJSONArray("records");
                            if (records != null && records.length() > 0) {
                                // Find matching email profile
                                boolean foundProfile = false;
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject rec = records.getJSONObject(i);
                                    String id = rec.optString("id", "");
                                    JSONObject data = rec.optJSONObject("data");
                                    if (data != null) {
                                        String email = data.optString("email", "");
                                        if (email.equalsIgnoreCase(userEmail)) {
                                            profileRecordId = id;
                                            currentPoints = data.optInt("points", 100);
                                            foundProfile = true;
                                            break;
                                        }
                                    }
                                }

                                if (!foundProfile) {
                                    createUserProfileRecord();
                                } else {
                                    updatePointsDisplay();
                                }
                            } else {
                                createUserProfileRecord();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // If profiles collection is empty, create profile
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createUserProfileRecord();
                    }
                });
            }
        });
    }

    private void createUserProfileRecord() {
        try {
            JSONObject data = new JSONObject();
            data.put("email", userEmail);
            data.put("points", 100); // 100 points signup bonus code-defined

            backendApi.createRecord("user_profiles", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject recordObj = response.optJSONObject("record");
                                if (recordObj != null) {
                                    profileRecordId = recordObj.optString("id", null);
                                    JSONObject internalData = recordObj.optJSONObject("data");
                                    if (internalData != null) {
                                        currentPoints = internalData.optInt("points", 100);
                                    }
                                }
                                updatePointsDisplay();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed initialization: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePointsDisplay() {
        tvPointsCount.setText(currentPoints + " pts");
        // Greeting adjustment
        int domainIndex = userEmail.indexOf("@");
        String name = domainIndex != -1 ? userEmail.substring(0, domainIndex) : userEmail;
        tvGreeting.setText("Welcome back, " + name + "!");
    }

    private void applyDailyBonus() {
        // Local Business Logic Formula: current_points + 20
        int initialVal = currentPoints;
        currentPoints += 20;
        updatePointsDisplay();

        syncWalletPointsToCloud(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "🎉 20 Loyalty points added to your account!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncWalletPointsToCloud(final Runnable onDone) {
        if (profileRecordId == null) {
            createUserProfileRecord();
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("email", userEmail);
            data.put("points", currentPoints);

            backendApi.updateRecord(profileRecordId, data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Wallet Sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redeemService(final String serviceName, final int cost) {
        // Deterministic local business validation
        if (currentPoints < cost) {
            Toast.makeText(this, "Insufficient balance! Earn loyalty rewards by checking-in.", Toast.LENGTH_LONG).show();
            return;
        }

        // Apply offline calculation immediately
        currentPoints -= cost;
        updatePointsDisplay();

        // Call sync & store booking entry
        syncWalletPointsToCloud(new Runnable() {
            @Override
            public void run() {
                createBookingRecord(serviceName, cost);
            }
        });
    }

    private void createBookingRecord(final String serviceName, final int cost) {
        try {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

            JSONObject data = new JSONObject();
            data.put("email", userEmail);
            data.put("service_name", serviceName);
            data.put("cost", cost);
            data.put("booking_date", dateStr);

            backendApi.createRecord("bookings", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Success! Your style session is reserved.", Toast.LENGTH_LONG).show();
                            fetchBookings(); // Reload local scroll lists
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to create cloud booking: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchBookings() {
        backendApi.readRecords("bookings", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            llBookingsContainer.removeAllViews();
                            JSONArray records = response.optJSONArray("records");

                            if (records == null || records.length() == 0) {
                                TextView tvEmpty = new TextView(MainActivity.this);
                                tvEmpty.setText("No stylist reservations scheduled yet.");
                                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                                tvEmpty.setTextSize(14sp);
                                tvEmpty.setPadding(0, 16, 0, 0);
                                llBookingsContainer.addView(tvEmpty);
                                return;
                            }

                            boolean foundAny = false;
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject item = records.getJSONObject(i);
                                JSONObject data = item.optJSONObject("data");
                                if (data != null) {
                                    String email = data.optString("email", "");
                                    if (email.equalsIgnoreCase(userEmail)) {
                                        foundAny = true;
                                        String serviceName = data.optString("service_name", "Styling Session");
                                        int cost = data.optInt("cost", 0);
                                        String date = data.optString("booking_date", "Date not confirmed");

                                        // Inflate card programmatically
                                        LinearLayout card = new LinearLayout(MainActivity.this);
                                        card.setOrientation(LinearLayout.VERTICAL);
                                        card.setBackgroundResource(R.drawable.card_rounded);
                                        card.setPadding(16, 16, 16, 16);

                                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        params.setMargins(0, 0, 0, 12);
                                        card.setLayoutParams(params);

                                        TextView titleTv = new TextView(MainActivity.this);
                                        titleTv.setText("📌 " + serviceName);
                                        titleTv.setTextColor(Color.parseColor("#121824"));
                                        titleTv.setTextSize(15sp);
                                        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                                        card.addView(titleTv);

                                        TextView detailsTv = new TextView(MainActivity.this);
                                        detailsTv.setText("Schedule: " + date + "\nWallet Redemptions: -" + cost + " pts");
                                        detailsTv.setTextColor(Color.parseColor("#64748B"));
                                        detailsTv.setTextSize(12sp);
                                        detailsTv.setPadding(0, 6, 0, 0);
                                        card.addView(detailsTv);

                                        llBookingsContainer.addView(card, 0); // Put latest bookings on top
                                    }
                                }
                            }

                            if (!foundAny) {
                                TextView tvEmpty = new TextView(MainActivity.this);
                                tvEmpty.setText("No styling reservations active for your account.");
                                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                                tvEmpty.setTextSize(14sp);
                                tvEmpty.setPadding(0, 16, 0, 0);
                                llBookingsContainer.addView(tvEmpty);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Sync bookings: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void logoutUser() {
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences.Editor editor = getSharedPreferences("BarberPrefs", MODE_PRIVATE).edit();
                        editor.clear();
                        editor.apply();
                        goToLogin();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Clear locally regardless of network loss
                        SharedPreferences.Editor editor = getSharedPreferences("BarberPrefs", MODE_PRIVATE).edit();
                        editor.clear();
                        editor.apply();
                        goToLogin();
                    }
                });
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}