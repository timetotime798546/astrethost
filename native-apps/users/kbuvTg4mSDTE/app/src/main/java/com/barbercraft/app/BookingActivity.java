package com.barbercraft.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class BookingActivity extends Activity {

    private ServiceItem serviceItem;
    private BackendApi backendApi;

    private EditText etCustomerName;
    private EditText etCustomerPhone;
    private EditText etAppointmentDate;
    private EditText etAppointmentTime;
    private Spinner spnBarber;
    private CheckBox cbApplyMemberDiscount;

    private TextView tvCalcBasePrice;
    private TextView tvCalcTax;
    private TextView tvCalcDiscount;
    private TextView tvCalcGrandTotal;
    private Button btnConfirmBooking;
    private ProgressBar pbBookingLoading;

    private static final double TAX_RATE = 0.08; // 8% local studio tax
    private static final double DISCOUNT_RATE = 0.10; // 10% membership discount

    private double calculatedTotal = 0.0;
    private double calculatedTax = 0.0;
    private double calculatedDiscount = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        backendApi = new BackendApi(this);
        serviceItem = (ServiceItem) getIntent().getSerializableExtra("service_item");

        if (serviceItem == null) {
            Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button btnBack = (Button) findViewById(R.id.btnBookingBack);
        TextView tvServiceName = (TextView) findViewById(R.id.tvBookingServiceName);
        TextView tvBasePrice = (TextView) findViewById(R.id.tvBookingBasePrice);

        etCustomerName = (EditText) findViewById(R.id.etCustomerName);
        etCustomerPhone = (EditText) findViewById(R.id.etCustomerPhone);
        etAppointmentDate = (EditText) findViewById(R.id.etAppointmentDate);
        etAppointmentTime = (EditText) findViewById(R.id.etAppointmentTime);
        spnBarber = (Spinner) findViewById(R.id.spnBarber);
        cbApplyMemberDiscount = (CheckBox) findViewById(R.id.cbApplyMemberDiscount);

        tvCalcBasePrice = (TextView) findViewById(R.id.tvCalcBasePrice);
        tvCalcTax = (TextView) findViewById(R.id.tvCalcTax);
        tvCalcDiscount = (TextView) findViewById(R.id.tvCalcDiscount);
        tvCalcGrandTotal = (TextView) findViewById(R.id.tvCalcGrandTotal);
        btnConfirmBooking = (Button) findViewById(R.id.btnConfirmBooking);
        pbBookingLoading = (ProgressBar) findViewById(R.id.pbBookingLoading);

        tvServiceName.setText(serviceItem.getName());
        tvBasePrice.setText("Base Price: $" + String.format("%.2f", serviceItem.getPrice()));

        // Populate Barbers Spinner
        String[] barbers = new String[]{
                "Marcus Vance - Senior Stylist",
                "Alexander Cole - Master Barber",
                "Derrick Hayes - Beard Specialist",
                "Elena Stone - Precision Artisan"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, barbers);
        spnBarber.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Offline deterministic calculation when discount changes
        cbApplyMemberDiscount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                recalculateFees();
            }
        });

        recalculateFees();

        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAppointment();
            }
        });
    }

    private void recalculateFees() {
        double basePrice = serviceItem.getPrice();
        double discountRate = cbApplyMemberDiscount.isChecked() ? DISCOUNT_RATE : 0.0;

        calculatedTax = CalculationHelper.calculateTax(basePrice, TAX_RATE);
        calculatedDiscount = CalculationHelper.calculateDiscount(basePrice, discountRate);
        calculatedTotal = CalculationHelper.calculateFinalTotal(basePrice, TAX_RATE, discountRate);

        tvCalcBasePrice.setText("$" + String.format("%.2f", basePrice));
        tvCalcTax.setText("$" + String.format("%.2f", calculatedTax));
        tvCalcDiscount.setText("-$" + String.format("%.2f", calculatedDiscount));
        tvCalcGrandTotal.setText("$" + String.format("%.2f", calculatedTotal));
    }

    private void submitAppointment() {
        String name = etCustomerName.getText().toString().trim();
        String phone = etCustomerPhone.getText().toString().trim();
        String date = etAppointmentDate.getText().toString().trim();
        String time = etAppointmentTime.getText().toString().trim();
        String barber = (String) spnBarber.getSelectedItem();

        if (name.isEmpty() || phone.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill out all booking details", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        pbBookingLoading.setVisibility(View.VISIBLE);

        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("service_name", serviceItem.getName());
            dataObj.put("customer_name", name);
            dataObj.put("customer_phone", phone);
            dataObj.put("appointment_date", date);
            dataObj.put("appointment_time", time);
            dataObj.put("barber_name", barber);
            dataObj.put("base_price", serviceItem.getPrice());
            dataObj.put("tax_amount", calculatedTax);
            dataObj.put("discount_amount", calculatedDiscount);
            dataObj.put("total_amount", calculatedTotal);
            dataObj.put("status", "Confirmed");

            backendApi.createData("appointments", dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    btnConfirmBooking.setEnabled(true);
                    pbBookingLoading.setVisibility(View.GONE);
                    Toast.makeText(BookingActivity.this, "Appointment successfully confirmed!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    btnConfirmBooking.setEnabled(true);
                    pbBookingLoading.setVisibility(View.GONE);
                    Toast.makeText(BookingActivity.this, "Booking error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            btnConfirmBooking.setEnabled(true);
            pbBookingLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Error building payload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}