package com.saloonbooking.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class BookingActivity extends Activity {

    private BackendApi api;

    // UI elements
    private TextView tvHeaderTitle, tvSectBook, tvLabelService, tvLabelStylist, tvLabelDatetime;
    private TextView tvCalcBase, tvCalcTax, tvCalcTotal, tvSectHistory;
    private Button btnDashLang, btnDashLogout, btnPickDate, btnPickTime, btnCreateBooking;
    private Spinner spinnerService, spinnerStylist;
    private LinearLayout containerBookingsList;
    private ProgressBar progressDash;

    // Booking Options Data
    private String[] servicesEn = {"Haircut (₹300)", "Shaving & Trim (₹150)", "Facial Therapy (₹600)", "Hair Styling & Spa (₹1000)"};
    private String[] servicesHi = {"बाल काटना (₹300)", "शेविंग और ट्रिम (₹150)", "फेशियल थेरेपी (₹600)", "हेयर स्टाइलिंग और स्पा (₹1000)"};
    private double[] servicePrices = {300.0, 150.0, 600.0, 1000.0};

    private String[] stylistsEn = {"Alex Johnson", "Rahul Sharma", "Vikram Singh", "Priya Mehta"};
    private String[] stylistsHi = {"एलेक्स जॉनसन", "राहुल शर्मा", "विक्रम सिंह", "प्रिया मेहता"};

    // Current inputs state
    private int selectedServiceIdx = 0;
    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        api = new BackendApi(this);
        SharedPreferences prefs = getSharedPreferences("salon_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        if (token.isEmpty()) {
            goToLogin();
            return;
        }
        api.setToken(token);

        // Bind layout views
        tvHeaderTitle = (TextView) findViewById(R.id.tv_header_title);
        tvSectBook = (TextView) findViewById(R.id.tv_sect_book);
        tvLabelService = (TextView) findViewById(R.id.tv_label_service);
        tvLabelStylist = (TextView) findViewById(R.id.tv_label_stylist);
        tvLabelDatetime = (TextView) findViewById(R.id.tv_label_datetime);
        tvCalcBase = (TextView) findViewById(R.id.tv_calc_base);
        tvCalcTax = (TextView) findViewById(R.id.tv_calc_tax);
        tvCalcTotal = (TextView) findViewById(R.id.tv_calc_total);
        tvSectHistory = (TextView) findViewById(R.id.tv_sect_history);

        btnDashLang = (Button) findViewById(R.id.btn_dash_lang);
        btnDashLogout = (Button) findViewById(R.id.btn_dash_logout);
        btnPickDate = (Button) findViewById(R.id.btn_pick_date);
        btnPickTime = (Button) findViewById(R.id.btn_pick_time);
        btnCreateBooking = (Button) findViewById(R.id.btn_create_booking);

        spinnerService = (Spinner) findViewById(R.id.spinner_service);
        spinnerStylist = (Spinner) findViewById(R.id.spinner_stylist);
        containerBookingsList = (LinearLayout) findViewById(R.id.container_bookings_list);
        progressDash = (ProgressBar) findViewById(R.id.progress_dash);

        // Set Language toggle click action
        btnDashLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newLang = "en".equals(Lang.getLanguage()) ? "hi" : "en";
                Lang.setLanguage(newLang);
                
                SharedPreferences.Editor editor = getSharedPreferences("salon_prefs", MODE_PRIVATE).edit();
                editor.putString("lang", newLang);
                editor.apply();

                rebuildDynamicSpinners();
                updateBookingUI();
                recalculatePrices();
            }
        });

        btnDashLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences("salon_prefs", MODE_PRIVATE).edit();
                editor.remove("token");
                editor.remove("email");
                editor.apply();
                goToLogin();
            }
        });

        // Date Picker Launcher
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(BookingActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                                btnPickDate.setText(selectedDate);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        // Time Picker Launcher
        btnPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(BookingActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                String minsStr = minute < 10 ? "0" + minute : String.valueOf(minute);
                                selectedTime = hourOfDay + ":" + minsStr;
                                btnPickTime.setText(selectedTime);
                            }
                        }, hour, minute, true);
                timePickerDialog.show();
            }
        });

        // Spinners setup
        rebuildDynamicSpinners();

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedServiceIdx = position;
                recalculatePrices();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCreateBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                    Toast.makeText(BookingActivity.this, "en".equals(Lang.getLanguage()) ? "Please choose Date and Time!" : "कृपया दिनांक और समय चुनें!", Toast.LENGTH_LONG).show();
                    return;
                }

                setLoading(true);

                final String serviceName = "en".equals(Lang.getLanguage()) ? servicesEn[selectedServiceIdx] : servicesHi[selectedServiceIdx];
                final String stylistName = "en".equals(Lang.getLanguage()) ? stylistsEn[spinnerStylist.getSelectedItemPosition()] : stylistsHi[spinnerStylist.getSelectedItemPosition()];
                final double basePrice = servicePrices[selectedServiceIdx];
                final double tax = basePrice * 0.18; // Calculated locally
                final double total = basePrice + tax; // Dynamic deterministic result mapped to booking_total in logic schema

                api.createBooking(serviceName, stylistName, selectedDate, selectedTime, basePrice, tax, total, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                Toast.makeText(BookingActivity.this, Lang.get("booking_success"), Toast.LENGTH_LONG).show();
                                // Reset inputs
                                selectedDate = "";
                                selectedTime = "";
                                btnPickDate.setText("Date Picker");
                                btnPickTime.setText("Time Picker");
                                loadBookingHistory();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                Toast.makeText(BookingActivity.this, Lang.get("booking_error") + ": " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        updateBookingUI();
        recalculatePrices();
        loadBookingHistory();
    }

    private void rebuildDynamicSpinners() {
        String[] currentServices = "en".equals(Lang.getLanguage()) ? servicesEn : servicesHi;
        String[] currentStylists = "en".equals(Lang.getLanguage()) ? stylistsEn : stylistsHi;

        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentServices);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);
        spinnerService.setSelection(selectedServiceIdx);

        ArrayAdapter<String> stylistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentStylists);
        stylistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStylist.setAdapter(stylistAdapter);
    }

    private void updateBookingUI() {
        tvHeaderTitle.setText(Lang.get("app_name"));
        tvSectBook.setText(Lang.get("book_appointment"));
        tvLabelService.setText(Lang.get("select_service"));
        tvLabelStylist.setText(Lang.get("select_stylist"));
        tvLabelDatetime.setText(Lang.get("select_date_time"));
        tvSectHistory.setText(Lang.get("my_bookings"));
        btnCreateBooking.setText(Lang.get("btn_confirm_booking"));
        btnDashLogout.setText(Lang.get("btn_logout"));
        btnDashLang.setText("en".equalsIgnoreCase(Lang.getLanguage()) ? "हिंदी" : "EN");
    }

    private void recalculatePrices() {
        double basePrice = servicePrices[selectedServiceIdx];
        double tax = basePrice * 0.18; // 18% GST standard salon tax
        double total = basePrice + tax;

        tvCalcBase.setText(Lang.get("base_price") + String.format("%.2f", basePrice));
        tvCalcTax.setText(Lang.get("gst_tax") + String.format("%.2f", tax));
        tvCalcTotal.setText(Lang.get("total_price") + String.format("%.2f", total));
    }

    private void loadBookingHistory() {
        setLoading(true);
        api.getBookings(new BackendApi.ApiListCallback() {
            @Override
            public void onSuccess(final JSONArray records) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        containerBookingsList.removeAllViews();

                        if (records.length() == 0) {
                            TextView emptyTv = new TextView(BookingActivity.this);
                            emptyTv.setText("en".equals(Lang.getLanguage()) ? "No appointments booked yet." : "अभी तक कोई नियुक्ति बुक नहीं की गई है।");
                            emptyTv.setPadding(16, 16, 16, 16);
                            emptyTv.setTextColor(Color.GRAY);
                            containerBookingsList.addView(emptyTv);
                            return;
                        }

                        for (int i = 0; i < records.length(); i++) {
                            try {
                                JSONObject recordObj = records.getJSONObject(i);
                                final String recordId = recordObj.getString("id");
                                JSONObject bookingData = recordObj.getJSONObject("data");

                                String service = bookingData.optString("service_name", "N/A");
                                String stylist = bookingData.optString("stylist_name", "N/A");
                                String date = bookingData.optString("booking_date", "N/A");
                                String time = bookingData.optString("booking_time", "N/A");
                                double totalVal = bookingData.optDouble("total_price", 0.0);

                                // Create booking history listing item dynamically
                                LinearLayout itemLayout = new LinearLayout(BookingActivity.this);
                                itemLayout.setOrientation(LinearLayout.VERTICAL);
                                itemLayout.setPadding(16, 16, 16, 16);
                                itemLayout.setBackgroundColor(Color.WHITE);
                                itemLayout.setElevation(2f);

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 0, 16);
                                itemLayout.setLayoutParams(params);

                                TextView titleText = new TextView(BookingActivity.this);
                                titleText.setText(service);
                                titleText.setTextSize(16sp);
                                titleText.setBold(true); // Helper logic or directly setTypeface
                                titleText.setTypeface(null, android.graphics.Typeface.BOLD);
                                titleText.setTextColor(Color.parseColor("#8E24AA"));
                                itemLayout.addView(titleText);

                                TextView stylistText = new TextView(BookingActivity.this);
                                stylistText.setText(( "en".equals(Lang.getLanguage()) ? "Stylist: " : "स्टाइलिस्ट: " ) + stylist);
                                stylistText.setTextColor(Color.DKGRAY);
                                itemLayout.addView(stylistText);

                                TextView dateTimeText = new TextView(BookingActivity.this);
                                dateTimeText.setText(( "en".equals(Lang.getLanguage()) ? "Schedule: " : "समय सारिणी: " ) + date + " @ " + time);
                                dateTimeText.setTextColor(Color.DKGRAY);
                                itemLayout.addView(dateTimeText);

                                TextView priceText = new TextView(BookingActivity.this);
                                priceText.setText(( "en".equals(Lang.getLanguage()) ? "Paid Total: ₹" : "कुल भुगतान: ₹" ) + String.format("%.2f", totalVal));
                                priceText.setTextColor(Color.BLACK);
                                priceText.setTypeface(null, android.graphics.Typeface.BOLD);
                                itemLayout.addView(priceText);

                                Button cancelBtn = new Button(BookingActivity.this);
                                cancelBtn.setText(Lang.get("btn_cancel"));
                                cancelBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D81B60")));
                                cancelBtn.setTextColor(Color.WHITE);
                                
                                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                btnParams.topMargin = 12;
                                cancelBtn.setLayoutParams(btnParams);

                                cancelBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(BookingActivity.this, Lang.get("cancelling"), Toast.LENGTH_SHORT).show();
                                        api.cancelBooking(recordId, new BackendApi.ApiCallback() {
                                            @Override
                                            public void onSuccess(JSONObject response) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(BookingActivity.this, "en".equals(Lang.getLanguage()) ? "Cancelled successfully!" : "सफलतापूर्वक रद्द कर दिया गया!", Toast.LENGTH_SHORT).show();
                                                        loadBookingHistory();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(final String error) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(BookingActivity.this, "Error cancelling: " + error, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });

                                itemLayout.addView(cancelBtn);
                                containerBookingsList.addView(itemLayout);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(BookingActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressDash.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateBooking.setEnabled(!isLoading);
        btnDashLang.setEnabled(!isLoading);
    }

    private void goToLogin() {
        Intent intent = new Intent(BookingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}