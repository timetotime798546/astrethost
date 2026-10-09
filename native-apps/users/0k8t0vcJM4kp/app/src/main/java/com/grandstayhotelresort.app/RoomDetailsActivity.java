package com.grandstayhotelresort.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

public class RoomDetailsActivity extends Activity {
    private TextView tvBackToDashboard, tvRoomDetailName, tvRoomDetailRating, tvRoomDetailDesc;
    private TextView tvCalcPrice, tvCalcNights, tvCalcSubtotal, tvCalcTaxes, tvCalcTotal;
    private Button btnCheckInDate, btnCheckOutDate, btnBookRoom;
    private EditText etSpecialRequests;
    private LinearLayout layoutImagesGallery;

    private Room room;
    private BackendApi api;
    private Calendar checkInCalendar, checkOutCalendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // Business Logic values
    private double pricePerNight;
    private int numberOfNights = 0;
    private double subtotal = 0.0;
    private double tax = 0.0;
    private double totalPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        api = new BackendApi(this);

        int roomIndex = getIntent().getIntExtra("ROOM_INDEX", 0);
        room = MainActivity.ROOMS[roomIndex];
        pricePerNight = room.pricePerNight;

        tvBackToDashboard = (TextView) findViewById(R.id.tvBackToDashboard);
        tvRoomDetailName = (TextView) findViewById(R.id.tvRoomDetailName);
        tvRoomDetailRating = (TextView) findViewById(R.id.tvRoomDetailRating);
        tvRoomDetailDesc = (TextView) findViewById(R.id.tvRoomDetailDesc);
        
        tvCalcPrice = (TextView) findViewById(R.id.tvCalcPrice);
        tvCalcNights = (TextView) findViewById(R.id.tvCalcNights);
        tvCalcSubtotal = (TextView) findViewById(R.id.tvCalcSubtotal);
        tvCalcTaxes = (TextView) findViewById(R.id.tvCalcTaxes);
        tvCalcTotal = (TextView) findViewById(R.id.tvCalcTotal);

        btnCheckInDate = (Button) findViewById(R.id.btnCheckInDate);
        btnCheckOutDate = (Button) findViewById(R.id.btnCheckOutDate);
        btnBookRoom = (Button) findViewById(R.id.btnBookRoom);
        etSpecialRequests = (EditText) findViewById(R.id.etSpecialRequests);
        layoutImagesGallery = (LinearLayout) findViewById(R.id.layoutImagesGallery);

        tvBackToDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Bind data
        tvRoomDetailName.setText(room.name);
        tvRoomDetailRating.setText("★ " + room.rating + " Luxury Guest Rating");
        tvRoomDetailDesc.setText(room.description);
        tvCalcPrice.setText("$" + (int) pricePerNight + " / night");

        // Adaptive Multi-Image implementation: inject multiple images into linear scrollview
        for (int i = 0; i < room.imageUrls.length; i++) {
            ImageView imgView = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(500, LinearLayout.LayoutParams.MATCH_PARENT);
            imgLp.setMargins(0, 0, 16, 0);
            imgView.setLayoutParams(imgLp);
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.loadImage(room.imageUrls[i], imgView);
            layoutImagesGallery.addView(imgView);
        }

        // Initialize Dates to Today and Tomorrow
        checkInCalendar = Calendar.getInstance();
        checkOutCalendar = Calendar.getInstance();
        checkOutCalendar.add(Calendar.DATE, 1);

        updateDateButtons();
        recalculateBookingCosts();

        btnCheckInDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(true);
            }
        });

        btnCheckOutDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(false);
            }
        });

        btnBookRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitResortBooking();
            }
        });
    }

    private void showDatePickerDialog(final boolean isCheckIn) {
        final Calendar workingCal = isCheckIn ? checkInCalendar : checkOutCalendar;
        DatePickerDialog dialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth);

                    if (isCheckIn) {
                        if (chosen.before(Calendar.getInstance())) {
                            Toast.makeText(RoomDetailsActivity.this, "Check-in cannot be prior to today.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        checkInCalendar = chosen;
                        if (checkOutCalendar.before(checkInCalendar) || checkOutCalendar.equals(checkInCalendar)) {
                            checkOutCalendar = (Calendar) checkInCalendar.clone();
                            checkOutCalendar.add(Calendar.DATE, 1);
                        }
                    } else {
                        if (chosen.before(checkInCalendar) || chosen.equals(checkInCalendar)) {
                            Toast.makeText(RoomDetailsActivity.this, "Check-out must be after check-in date.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        checkOutCalendar = chosen;
                    }
                    updateDateButtons();
                    recalculateBookingCosts();
                }
            },
            workingCal.get(Calendar.YEAR),
            workingCal.get(Calendar.MONTH),
            workingCal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateButtons() {
        btnCheckInDate.setText(dateFormat.format(checkInCalendar.getTime()));
        btnCheckOutDate.setText(dateFormat.format(checkOutCalendar.getTime()));
    }

    // MANDATORY OFFLINE CALCULATION: calculated locally in Android
    private void recalculateBookingCosts() {
        long difference = checkOutCalendar.getTimeInMillis() - checkInCalendar.getTimeInMillis();
        long days = difference / (24 * 60 * 60 * 1000);
        if (days <= 0) {
            days = 1; // Minimum booking validation
        }

        numberOfNights = (int) days;
        subtotal = pricePerNight * numberOfNights;
        tax = subtotal * 0.10; // Tax configuration
        totalPrice = subtotal + tax;

        // Render calculated results
        tvCalcNights.setText(numberOfNights + (numberOfNights == 1 ? " night" : " nights"));
        tvCalcSubtotal.setText(String.format(Locale.US, "$%.2f", subtotal));
        tvCalcTaxes.setText(String.format(Locale.US, "$%.2f", tax));
        tvCalcTotal.setText(String.format(Locale.US, "$%.2f", totalPrice));
    }

    private void submitResortBooking() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Securing hotel booking details...");
        pd.setCancelable(false);
        pd.show();

        try {
            JSONObject data = new JSONObject();
            data.put("room_name", room.name);
            data.put("price_per_night", pricePerNight);
            data.put("check_in_date", dateFormat.format(checkInCalendar.getTime()));
            data.put("check_out_date", dateFormat.format(checkOutCalendar.getTime()));
            data.put("number_of_nights", numberOfNights);
            data.put("subtotal", subtotal);
            data.put("tax", tax);
            data.put("total_price", totalPrice);
            data.put("guest_name", api.getGuestName());
            data.put("special_requests", etSpecialRequests.getText().toString().trim());

            api.createRecord("bookings", data, new BackendApi.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    pd.dismiss();
                    Toast.makeText(RoomDetailsActivity.this, "Booking Successful! GrandStay awaits you.", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    pd.dismiss();
                    Toast.makeText(RoomDetailsActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            pd.dismiss();
            Toast.makeText(this, "Preparation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}