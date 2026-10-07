package com.barberq.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.Calendar;

public class BookingActivity extends Activity {

    private TextView btnBack, tvSelectedServicesSummary, tvBookingTotal;
    private EditText etBookingName, etBookingPhone;
    private Button btnSelectDate, btnBookAppointment;
    private Button[] slotButtons;
    private ProgressBar bookingProgress;

    private String selectedServices = "";
    private double totalPrice = 0.0;
    private String selectedDate = "";
    private String selectedTime = "";
    private BackendApi api;
    private String userToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        api = new BackendApi(this);

        // Fetch User token
        SharedPreferences prefs = getSharedPreferences("BarberQPrefs", MODE_PRIVATE);
        userToken = prefs.getString("token", "");
        String defaultName = prefs.getString("userName", "");

        // Collect parameters from dashboard selection
        Intent received = getIntent();
        selectedServices = received.getStringExtra("services");
        totalPrice = received.getDoubleExtra("totalPrice", 0.0);

        // Setup views
        btnBack = (TextView) findViewById(R.id.btnBack);
        tvSelectedServicesSummary = (TextView) findViewById(R.id.tvSelectedServicesSummary);
        tvBookingTotal = (TextView) findViewById(R.id.tvBookingTotal);
        etBookingName = (EditText) findViewById(R.id.etBookingName);
        etBookingPhone = (EditText) findViewById(R.id.etBookingPhone);
        btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
        btnBookAppointment = (Button) findViewById(R.id.btnBookAppointment);
        bookingProgress = (ProgressBar) findViewById(R.id.bookingProgress);

        if (!defaultName.isEmpty()) {
            etBookingName.setText(defaultName);
        }

        tvSelectedServicesSummary.setText(selectedServices);
        tvBookingTotal.setText("₹" + (int) totalPrice);

        // Map interactive Slots
        slotButtons = new Button[]{
                (Button) findViewById(R.id.slot1),
                (Button) findViewById(R.id.slot2),
                (Button) findViewById(R.id.slot3),
                (Button) findViewById(R.id.slot4),
                (Button) findViewById(R.id.slot5),
                (Button) findViewById(R.id.slot6)
        };

        // Assign Slot selection handlers
        for (int i = 0; i < slotButtons.length; i++) {
            final int index = i;
            slotButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    highlightSlot(index);
                }
            });
        }

        // Date selection listener
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Trigger appointment submission
        btnBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitBooking();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void highlightSlot(int index) {
        for (int i = 0; i < slotButtons.length; i++) {
            slotButtons[i].setSelected(false);
        }
        slotButtons[index].setSelected(true);
        selectedTime = slotButtons[index].getText().toString();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                btnSelectDate.setText(selectedDate);
            }
        }, year, month, day);

        // Restrict reservations to future dates
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void submitBooking() {
        String name = etBookingName.getText().toString().trim();
        String phone = etBookingPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Complete your personal details.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Choose a target date.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select an available timing slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        bookingProgress.setVisibility(View.VISIBLE);
        btnBookAppointment.setEnabled(false);

        api.createBooking(userToken, name, phone, selectedServices, totalPrice, selectedDate, selectedTime, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                bookingProgress.setVisibility(View.GONE);
                btnBookAppointment.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Appointment successfully booked!", Toast.LENGTH_LONG).show();

                // Go to My Bookings
                Intent intent = new Intent(BookingActivity.this, HistoryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                bookingProgress.setVisibility(View.GONE);
                btnBookAppointment.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Failed to schedule: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}