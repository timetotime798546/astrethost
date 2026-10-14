package com.caresyncpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LocalDataManager localStore;
    private BackendApi backendApi;

    // UI Tab Views
    private ScrollView viewDashboard;
    private LinearLayout viewPatients;
    private ScrollView viewOpd;
    private LinearLayout viewPharmacy;

    // Tab Buttons
    private Button tabDashboard;
    private Button tabPatients;
    private Button tabOpd;
    private Button tabPharmacy;
    private Button btnAuthAction;
    private TextView tvHeaderSubtitle;

    // Dashboard widgets
    private TextView tvStatAppointments;
    private TextView tvStatInpatients;
    private TextView tvStatLabReports;
    private TextView tvStatPharmacyRevenue;
    private TextView tvQueueCount;
    private LinearLayout containerDashboardQueue;

    // Patients Tab
    private EditText etPatientSearch;
    private ListView lvPatients;
    private PatientAdapter patientAdapter;
    private List<JSONObject> displayPatients;

    // OPD & Billing Tab
    private EditText etBookingMrn;
    private Spinner spnDoctorSelect;
    private EditText etBookingDate;
    private EditText etBookingSlot;
    private Button btnConfirmBooking;

    private EditText etRxPatient;
    private Spinner spnRxMedicine;
    private EditText etRxDosage;
    private EditText etRxDuration;
    private EditText etRxNotes;
    private EditText etBillFee;
    private EditText etBillDiscount;
    private EditText etBillTax;
    private TextView tvComputedNetBill;
    private Button btnFulfillPrescription;

    // Pharmacy Tab
    private ListView lvPharmacy;
    private MedicineAdapter medicineAdapter;
    private List<JSONObject> displayMedicines;

    private final String[] doctors = new String[]{
            "Dr. Marcus Wells (Cardiology)",
            "Dr. Sarah Jenkins (Endocrinology)",
            "Dr. Alan Hayes (General Medicine)",
            "Dr. Elena Rostova (Pediatrics)",
            "Dr. David Kim (Orthopedics)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localStore = new LocalDataManager(this);
        backendApi = new BackendApi(this);

        initViews();
        setupNavigation();
        setupDashboard();
        setupPatientsModule();
        setupOpdModule();
        setupPharmacyModule();
        updateAuthHeaderState();
        refreshAllData();
    }

    private void initViews() {
        viewDashboard = (ScrollView) findViewById(R.id.view_dashboard);
        viewPatients = (LinearLayout) findViewById(R.id.view_patients);
        viewOpd = (ScrollView) findViewById(R.id.view_opd);
        viewPharmacy = (LinearLayout) findViewById(R.id.view_pharmacy);

        tabDashboard = (Button) findViewById(R.id.tab_dashboard);
        tabPatients = (Button) findViewById(R.id.tab_patients);
        tabOpd = (Button) findViewById(R.id.tab_opd);
        tabPharmacy = (Button) findViewById(R.id.tab_pharmacy);

        btnAuthAction = (Button) findViewById(R.id.btn_auth_action);
        tvHeaderSubtitle = (TextView) findViewById(R.id.tv_header_subtitle);

        tvStatAppointments = (TextView) findViewById(R.id.tv_stat_appointments);
        tvStatInpatients = (TextView) findViewById(R.id.tv_stat_inpatients);
        tvStatLabReports = (TextView) findViewById(R.id.tv_stat_lab_reports);
        tvStatPharmacyRevenue = (TextView) findViewById(R.id.tv_stat_pharmacy_revenue);
        tvQueueCount = (TextView) findViewById(R.id.tv_queue_count);
        containerDashboardQueue = (LinearLayout) findViewById(R.id.container_dashboard_queue);

        etPatientSearch = (EditText) findViewById(R.id.et_patient_search);
        lvPatients = (ListView) findViewById(R.id.lv_patients);

        etBookingMrn = (EditText) findViewById(R.id.et_booking_mrn);
        spnDoctorSelect = (Spinner) findViewById(R.id.spn_doctor_select);
        etBookingDate = (EditText) findViewById(R.id.et_booking_date);
        etBookingSlot = (EditText) findViewById(R.id.et_booking_slot);
        btnConfirmBooking = (Button) findViewById(R.id.btn_confirm_booking);

        etRxPatient = (EditText) findViewById(R.id.et_rx_patient);
        spnRxMedicine = (Spinner) findViewById(R.id.spn_rx_medicine);
        etRxDosage = (EditText) findViewById(R.id.et_rx_dosage);
        etRxDuration = (EditText) findViewById(R.id.et_rx_duration);
        etRxNotes = (EditText) findViewById(R.id.et_rx_notes);
        etBillFee = (EditText) findViewById(R.id.et_bill_fee);
        etBillDiscount = (EditText) findViewById(R.id.et_bill_discount);
        etBillTax = (EditText) findViewById(R.id.et_bill_tax);
        tvComputedNetBill = (TextView) findViewById(R.id.tv_computed_net_bill);
        btnFulfillPrescription = (Button) findViewById(R.id.btn_fulfill_prescription);

        lvPharmacy = (ListView) findViewById(R.id.lv_pharmacy);
    }

    private void setupNavigation() {
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        tabPatients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        tabOpd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        tabPharmacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        btnAuthAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (localStore.getAuthToken().isEmpty()) {
                    showAuthDialog();
                } else {
                    showSignOutConfirmation();
                }
            }
        });
    }

    private void switchTab(int index) {
        viewDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewPatients.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewOpd.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        viewPharmacy.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        int activeColor = Color.parseColor("#0D9488");
        int inactiveColor = Color.parseColor("#64748B");

        tabDashboard.setTextColor(index == 0 ? activeColor : inactiveColor);
        tabPatients.setTextColor(index == 1 ? activeColor : inactiveColor);
        tabOpd.setTextColor(index == 2 ? activeColor : inactiveColor);
        tabPharmacy.setTextColor(index == 3 ? activeColor : inactiveColor);
    }

    private void setupDashboard() {
        findViewById(R.id.btn_quick_new_patient).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPatientDialog();
            }
        });

        findViewById(R.id.btn_quick_book_apt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
                etBookingMrn.requestFocus();
            }
        });

        findViewById(R.id.btn_quick_create_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
                etBillFee.requestFocus();
            }
        });

        findViewById(R.id.btn_quick_prescribe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
                etRxPatient.requestFocus();
            }
        });
    }

    private void setupPatientsModule() {
        displayPatients = new ArrayList<JSONObject>();
        patientAdapter = new PatientAdapter();
        lvPatients.setAdapter(patientAdapter);

        findViewById(R.id.btn_open_add_patient).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPatientDialog();
            }
        });

        etPatientSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPatients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupOpdModule() {
        ArrayAdapter<String> docAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, doctors);
        spnDoctorSelect.setAdapter(docAdapter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etBookingDate.setText(sdf.format(new Date()));
        etBookingSlot.setText("11:30 AM");

        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAppointmentBooking();
            }
        });

        // Billing real-time listeners for local calculations
        TextWatcher billWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateOpdBill();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etBillFee.addTextChangedListener(billWatcher);
        etBillDiscount.addTextChangedListener(billWatcher);
        etBillTax.addTextChangedListener(billWatcher);

        btnFulfillPrescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fulfillPrescriptionCheckout();
            }
        });
    }

    private void setupPharmacyModule() {
        displayMedicines = new ArrayList<JSONObject>();
        medicineAdapter = new MedicineAdapter();
        lvPharmacy.setAdapter(medicineAdapter);

        findViewById(R.id.btn_open_add_medicine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMedicineDialog();
            }
        });
    }

    private void recalculateOpdBill() {
        double fee = parseDouble(etBillFee.getText().toString(), 0);
        double disc = parseDouble(etBillDiscount.getText().toString(), 0);
        double tax = parseDouble(etBillTax.getText().toString(), 0);

        double net = BillingCalculator.calculateNetTotal(fee, disc, tax);
        tvComputedNetBill.setText(BillingCalculator.formatCurrency(net));
    }

    private double parseDouble(String str, double defaultVal) {
        try {
            return Double.parseDouble(str.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private void refreshAllData() {
        // Patients
        filterPatients(etPatientSearch.getText().toString());

        // Medicines
        displayMedicines.clear();
        displayMedicines.addAll(localStore.getMedicines());
        medicineAdapter.notifyDataSetChanged();

        // Update Rx medicine spinner
        List<String> medNames = new ArrayList<String>();
        for (int i = 0; i < displayMedicines.size(); i++) {
            JSONObject m = displayMedicines.get(i);
            medNames.add(m.optString("name") + " (" + m.optInt("stock_quantity") + " left)");
        }
        ArrayAdapter<String> rxMedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, medNames);
        spnRxMedicine.setAdapter(rxMedAdapter);

        // Appointments & Queue
        List<JSONObject> apts = localStore.getAppointments();
        tvStatAppointments.setText(String.valueOf(apts.size()));
        tvQueueCount.setText(apts.size() + " in Queue");

        containerDashboardQueue.removeAllViews();
        for (int i = 0; i < apts.size(); i++) {
            final JSONObject apt = apts.get(i);
            View item = createQueueItemView(apt);
            containerDashboardQueue.addView(item);
        }

        // Stats calculation
        double revenue = 0.0;
        List<JSONObject> rxList = localStore.getPrescriptions();
        for (int i = 0; i < rxList.size(); i++) {
            revenue += rxList.get(i).optDouble("net_total", 0.0);
        }
        for (int i = 0; i < apts.size(); i++) {
            revenue += apts.get(i).optDouble("consultation_fee", 0.0);
        }
        tvStatPharmacyRevenue.setText(BillingCalculator.formatCurrency(revenue));

        recalculateOpdBill();
    }

    private View createQueueItemView(final JSONObject apt) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.bg_card_rounded);
        row.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 8;
        row.setLayoutParams(params);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(apt.optString("patient_name") + " (" + apt.optString("mrn") + ")");
        tvName.setTextColor(Color.parseColor("#0F172A"));
        tvName.setTextSize(14);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        left.addView(tvName);

        TextView tvDoc = new TextView(this);
        tvDoc.setText(apt.optString("doctor_name") + " • Slot: " + apt.optString("slot_time"));
        tvDoc.setTextColor(Color.parseColor("#64748B"));
        tvDoc.setTextSize(12);
        left.addView(tvDoc);

        row.addView(left);

        TextView badge = new TextView(this);
        String status = apt.optString("status", "Waiting");
        badge.setText(status);
        badge.setTextSize(11);
        badge.setPadding(8, 4, 8, 4);

        if (status.equalsIgnoreCase("Completed")) {
            badge.setBackgroundResource(R.drawable.bg_badge_green);
            badge.setTextColor(Color.parseColor("#059669"));
        } else if (status.equalsIgnoreCase("In-Consultation")) {
            badge.setBackgroundResource(R.drawable.bg_stat_amber);
            badge.setTextColor(Color.WHITE);
        } else {
            badge.setBackgroundResource(R.drawable.bg_badge_red);
            badge.setTextColor(Color.parseColor("#E11D48"));
        }

        row.addView(badge);
        return row;
    }

    private void filterPatients(String query) {
        displayPatients.clear();
        List<JSONObject> all = localStore.getPatients();
        String q = query.toLowerCase(Locale.US).trim();

        if (q.isEmpty()) {
            displayPatients.addAll(all);
        } else {
            for (int i = 0; i < all.size(); i++) {
                JSONObject p = all.get(i);
                String name = p.optString("name", "").toLowerCase(Locale.US);
                String phone = p.optString("phone", "").toLowerCase(Locale.US);
                String mrn = p.optString("mrn", "").toLowerCase(Locale.US);

                if (name.contains(q) || phone.contains(q) || mrn.contains(q)) {
                    displayPatients.add(p);
                }
            }
        }
        patientAdapter.notifyDataSetChanged();
    }

    private void confirmAppointmentBooking() {
        String patient = etBookingMrn.getText().toString().trim();
        String date = etBookingDate.getText().toString().trim();
        String slot = etBookingSlot.getText().toString().trim();
        String doc = spnDoctorSelect.getSelectedItem() != null ? spnDoctorSelect.getSelectedItem().toString() : "Staff Doctor";

        if (patient.isEmpty() || date.isEmpty() || slot.isEmpty()) {
            Toast.makeText(this, "Please fill in patient name, date, and slot", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            final JSONObject apt = new JSONObject();
            apt.put("mrn", "MRN-" + (1000 + (int)(Math.random() * 9000)));
            apt.put("patient_name", patient);
            apt.put("doctor_name", doc);
            apt.put("department", "Outpatient Clinic");
            apt.put("slot_time", slot);
            apt.put("date", date);
            apt.put("status", "Waiting");
            apt.put("consultation_fee", 65.00);

            localStore.saveAppointment(apt);

            // Sync with backend if authenticated
            String token = localStore.getAuthToken();
            if (!token.isEmpty()) {
                backendApi.createRecord(token, "appointments", apt, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        // Synced successfully
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
            }

            etBookingMrn.setText("");
            Toast.makeText(this, "Appointment scheduled for " + patient, Toast.LENGTH_LONG).show();
            refreshAllData();
            switchTab(0);
        } catch (Exception e) {
            Toast.makeText(this, "Error booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fulfillPrescriptionCheckout() {
        String pt = etRxPatient.getText().toString().trim();
        if (pt.isEmpty()) {
            Toast.makeText(this, "Please enter patient name or MRN", Toast.LENGTH_SHORT).show();
            return;
        }

        String medSelection = spnRxMedicine.getSelectedItem() != null ? spnRxMedicine.getSelectedItem().toString() : "";
        String dosage = etRxDosage.getText().toString().trim();
        String duration = etRxDuration.getText().toString().trim();
        String notes = etRxNotes.getText().toString().trim();

        double fee = parseDouble(etBillFee.getText().toString(), 0);
        double disc = parseDouble(etBillDiscount.getText().toString(), 0);
        double tax = parseDouble(etBillTax.getText().toString(), 0);
        double net = BillingCalculator.calculateNetTotal(fee, disc, tax);

        try {
            final JSONObject rx = new JSONObject();
            rx.put("mrn", "MRN-RX-" + (int)(Math.random() * 9999));
            rx.put("patient_name", pt);
            rx.put("doctor_name", "Dr. Marcus Wells");
            rx.put("medication_list", medSelection + " | Dose: " + dosage + " (" + duration + ")");
            rx.put("clinical_notes", notes);
            rx.put("subtotal", fee);
            rx.put("discount", disc);
            rx.put("tax", tax);
            rx.put("net_total", net);
            rx.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()));

            localStore.savePrescription(rx);

            // Automated stock deduction
            if (!medSelection.isEmpty()) {
                String medName = medSelection.split(" \\(")[0];
                localStore.deductMedicineStock(medName, 1);
            }

            // Sync with backend if authenticated
            String token = localStore.getAuthToken();
            if (!token.isEmpty()) {
                backendApi.createRecord(token, "prescriptions", rx, null);
            }

            showReceiptDialog(rx);
            refreshAllData();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to fulfill prescription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showReceiptDialog(JSONObject rx) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("OPD Consultation Receipt Generated");
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt Number: ").append(rx.optString("mrn")).append("\n");
        sb.append("Patient: ").append(rx.optString("patient_name")).append("\n");
        sb.append("Medication: ").append(rx.optString("medication_list")).append("\n");
        sb.append("Subtotal: $").append(rx.optDouble("subtotal")).append("\n");
        sb.append("Discount: -$").append(rx.optDouble("discount")).append("\n");
        sb.append("Tax: +$").append(rx.optDouble("tax")).append("\n");
        sb.append("-----------------------------\n");
        sb.append("Net Total Paid: $").append(rx.optDouble("net_total")).append("\n\n");
        sb.append("Status: Fulfilled & Stock Deducted");

        b.setMessage(sb.toString());
        b.setPositiveButton("Print Receipt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Receipt dispatched to clinical printer", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Close", null);
        b.show();
    }

    private void showAddPatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Patient EHR Registration");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText etName = createDialogInput("Patient Full Name");
        final EditText etPhone = createDialogInput("Mobile Contact (e.g. +1 555-0100)");
        final EditText etDob = createDialogInput("Date of Birth (YYYY-MM-DD)");
        final EditText etBlood = createDialogInput("Blood Group (e.g. O+, A-, B+)");
        final EditText etAllergies = createDialogInput("Allergies (e.g. Penicillin, None)");
        final EditText etBp = createDialogInput("Vitals: Blood Pressure (e.g. 120/80)");
        final EditText etPulse = createDialogInput("Vitals: Pulse (bpm)");
        final EditText etSugar = createDialogInput("Vitals: Blood Sugar (mg/dL)");
        final EditText etDiag = createDialogInput("Preliminary Clinical Diagnosis");

        layout.addView(etName);
        layout.addView(etPhone);
        layout.addView(etDob);
        layout.addView(etBlood);
        layout.addView(etAllergies);
        layout.addView(etBp);
        layout.addView(etPulse);
        layout.addView(etSugar);
        layout.addView(etDiag);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        builder.setView(scroll);

        builder.setPositiveButton("Register Patient", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String dob = etDob.getText().toString().trim();
                String blood = etBlood.getText().toString().trim();
                String allergies = etAllergies.getText().toString().trim();
                String bp = etBp.getText().toString().trim();
                String pulse = etPulse.getText().toString().trim();
                String sugar = etSugar.getText().toString().trim();
                String diag = etDiag.getText().toString().trim();

                if (name.isEmpty() || dob.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Name and Date of Birth are mandatory", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Deterministic local age calculation
                int computedAge = BillingCalculator.calculateAge(dob);

                try {
                    final JSONObject pt = new JSONObject();
                    pt.put("mrn", "MRN-" + (2000 + (int)(Math.random() * 8000)));
                    pt.put("name", name);
                    pt.put("phone", phone);
                    pt.put("dob", dob);
                    pt.put("age", computedAge);
                    pt.put("blood_group", blood.isEmpty() ? "Unknown" : blood);
                    pt.put("allergies", allergies.isEmpty() ? "None reported" : allergies);
                    pt.put("vitals_bp", bp.isEmpty() ? "120/80" : bp);
                    pt.put("vitals_pulse", pulse.isEmpty() ? "72" : pulse);
                    pt.put("vitals_sugar", sugar.isEmpty() ? "95" : sugar);
                    pt.put("diagnosis", diag.isEmpty() ? "Routine OPD Evaluation" : diag);

                    localStore.savePatient(pt);

                    // Sync to Cloud Backend if logged in
                    String token = localStore.getAuthToken();
                    if (!token.isEmpty()) {
                        backendApi.createRecord(token, "patients", pt, null);
                    }

                    Toast.makeText(MainActivity.this, "Patient Registered (Age: " + computedAge + ")", Toast.LENGTH_LONG).show();
                    refreshAllData();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Registration error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddMedicineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Pharmacy Catalog Item");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText etMedName = createDialogInput("Medicine Name & Dosage");
        final EditText etBatch = createDialogInput("Batch Number (e.g. BT-9021)");
        final EditText etStock = createDialogInput("Initial Stock Units");
        etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final EditText etPrice = createDialogInput("Unit Price ($)");
        final EditText etExpiry = createDialogInput("Expiry Date (YYYY-MM-DD)");

        layout.addView(etMedName);
        layout.addView(etBatch);
        layout.addView(etStock);
        layout.addView(etPrice);
        layout.addView(etExpiry);

        builder.setView(layout);
        builder.setPositiveButton("Add Item", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etMedName.getText().toString().trim();
                String batch = etBatch.getText().toString().trim();
                int stock = (int) parseDouble(etStock.getText().toString(), 0);
                double price = parseDouble(etPrice.getText().toString(), 1.0);
                String exp = etExpiry.getText().toString().trim();

                if (name.isEmpty() || exp.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Name and Expiry date are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    final JSONObject med = new JSONObject();
                    med.put("name", name);
                    med.put("batch_number", batch.isEmpty() ? "BT-" + (1000 + (int)(Math.random() * 9000)) : batch);
                    med.put("stock_quantity", stock);
                    med.put("unit_price", price);
                    med.put("expiry_date", exp);
                    med.put("is_expiring_soon", BillingCalculator.isExpiringWithinDays(exp, 30));

                    localStore.saveMedicine(med);

                    String token = localStore.getAuthToken();
                    if (!token.isEmpty()) {
                        backendApi.createRecord(token, "medicines", med, null);
                    }

                    Toast.makeText(MainActivity.this, "Medicine Added to Inventory", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private EditText createDialogInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setPadding(12, 10, 12, 10);
        et.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 8;
        et.setLayoutParams(lp);
        return et;
    }

    private void showPatientDetailsDialog(JSONObject pt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(pt.optString("name") + " [" + pt.optString("mrn") + "]");

        StringBuilder sb = new StringBuilder();
        sb.append("Contact: ").append(pt.optString("phone")).append("\n");
        sb.append("DOB: ").append(pt.optString("dob")).append(" (Age: ").append(pt.optInt("age")).append(")\n");
        sb.append("Blood Group: ").append(pt.optString("blood_group")).append("\n");
        sb.append("Allergies: ").append(pt.optString("allergies")).append("\n");
        sb.append("Blood Pressure: ").append(pt.optString("vitals_bp")).append("\n");
        sb.append("Pulse: ").append(pt.optString("vitals_pulse")).append(" bpm\n");
        sb.append("Blood Sugar: ").append(pt.optString("vitals_sugar")).append(" mg/dL\n");
        sb.append("Current Diagnosis: ").append(pt.optString("diagnosis")).append("\n");

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Schedule Appointment", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchTab(2);
                etBookingMrn.setText(pt.optString("mrn"));
            }
        });
        builder.setNeutralButton("Prescribe", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchTab(2);
                etRxPatient.setText(pt.optString("name") + " (" + pt.optString("mrn") + ")");
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // Authentication Dialogs (Login, Register, Forgot Password with OTP)
    private void showAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Staff Cloud Sync Login");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText etEmail = createDialogInput("Clinical Staff Email");
        final EditText etPass = createDialogInput("Password");
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etEmail);
        layout.addView(etPass);

        Button btnForgot = new Button(this);
        btnForgot.setText("Forgot Password? Reset via OTP");
        btnForgot.setBackgroundColor(Color.TRANSPARENT);
        btnForgot.setTextColor(Color.parseColor("#4338CA"));
        btnForgot.setTextSize(12);
        btnForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
        layout.addView(btnForgot);

        builder.setView(layout);

        builder.setPositiveButton("Sign In", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = etEmail.getText().toString().trim();
                final String pass = etPass.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                backendApi.login(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        String token = response.optString("token", "");
                        if (!token.isEmpty()) {
                            localStore.saveAuthToken(token, email);
                            updateAuthHeaderState();
                            Toast.makeText(MainActivity.this, "Authenticated successfully as " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Login response missing token", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Login error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        builder.setNeutralButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = etEmail.getText().toString().trim();
                final String pass = etPass.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Register followed by Login per rules
                backendApi.register(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        backendApi.login(email, pass, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject loginResp) {
                                String token = loginResp.optString("token", "");
                                if (!token.isEmpty()) {
                                    localStore.saveAuthToken(token, email);
                                    updateAuthHeaderState();
                                    Toast.makeText(MainActivity.this, "Registered and Logged in as " + email, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(MainActivity.this, "Auto-login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password (OTP)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText etEmail = createDialogInput("Registered Staff Email");
        final EditText etOtp = createDialogInput("6-digit OTP code");
        final EditText etNewPass = createDialogInput("New Password");
        etNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Button btnSendOtp = new Button(this);
        btnSendOtp.setText("Step 1: Request OTP");
        btnSendOtp.setBackgroundResource(R.drawable.bg_button_secondary);
        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter email first", Toast.LENGTH_SHORT).show();
                    return;
                }
                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Toast.makeText(MainActivity.this, "OTP sent to your email inbox", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "OTP request failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        layout.addView(etEmail);
        layout.addView(btnSendOtp);
        layout.addView(etOtp);
        layout.addView(etNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Step 2: Submit Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = etEmail.getText().toString().trim();
                String otp = etOtp.getText().toString().trim();
                String newPass = etNewPass.getText().toString().trim();

                if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in email, OTP, and new password", Toast.LENGTH_SHORT).show();
                    return;
                }

                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Toast.makeText(MainActivity.this, "Password reset successfully! Please log in.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSignOutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Staff Session");
        builder.setMessage("Logged in as: " + localStore.getUserEmail() + "\n\nDo you want to log out?");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                localStore.clearAuth();
                updateAuthHeaderState();
                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateAuthHeaderState() {
        String token = localStore.getAuthToken();
        if (token.isEmpty()) {
            btnAuthAction.setText("Sign In");
            tvHeaderSubtitle.setText("Hospital Network • Offline Mode");
        } else {
            btnAuthAction.setText("Signed In");
            tvHeaderSubtitle.setText("Staff Sync Active (" + localStore.getUserEmail() + ")");
        }
    }

    // Patient List Adapter
    private class PatientAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return displayPatients.size();
        }

        @Override
        public Object getItem(int position) {
            return displayPatients.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final JSONObject pt = displayPatients.get(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundResource(R.drawable.bg_card_rounded);
            row.setPadding(14, 14, 14, 14);

            LinearLayout left = new LinearLayout(MainActivity.this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(MainActivity.this);
            tvName.setText(pt.optString("name") + "  •  " + pt.optString("mrn"));
            tvName.setTextColor(Color.parseColor("#0F172A"));
            tvName.setTextSize(14);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            left.addView(tvName);

            TextView tvDetails = new TextView(MainActivity.this);
            tvDetails.setText("Age: " + pt.optInt("age") + " | Blood: " + pt.optString("blood_group") + " | BP: " + pt.optString("vitals_bp"));
            tvDetails.setTextColor(Color.parseColor("#64748B"));
            tvDetails.setTextSize(12);
            left.addView(tvDetails);

            TextView tvDiag = new TextView(MainActivity.this);
            tvDiag.setText("Dx: " + pt.optString("diagnosis") + " • Allergies: " + pt.optString("allergies"));
            tvDiag.setTextColor(Color.parseColor("#0D9488"));
            tvDiag.setTextSize(11);
            left.addView(tvDiag);

            row.addView(left);

            Button btnView = new Button(MainActivity.this);
            btnView.setText("EHR Profile");
            btnView.setTextSize(11);
            btnView.setTextColor(Color.WHITE);
            btnView.setBackgroundResource(R.drawable.bg_button_primary);
            btnView.setPadding(8, 0, 8, 0);
            btnView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 36 * (int)getResources().getDisplayMetrics().density));
            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPatientDetailsDialog(pt);
                }
            });

            row.addView(btnView);
            return row;
        }
    }

    // Medicine List Adapter
    private class MedicineAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return displayMedicines.size();
        }

        @Override
        public Object getItem(int position) {
            return displayMedicines.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final JSONObject med = displayMedicines.get(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundResource(R.drawable.bg_card_rounded);
            row.setPadding(14, 14, 14, 14);

            LinearLayout left = new LinearLayout(MainActivity.this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(MainActivity.this);
            tvName.setText(med.optString("name"));
            tvName.setTextColor(Color.parseColor("#0F172A"));
            tvName.setTextSize(14);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            left.addView(tvName);

            TextView tvBatch = new TextView(MainActivity.this);
            tvBatch.setText("Batch: " + med.optString("batch_number") + " | Price: $" + med.optDouble("unit_price") + " | Exp: " + med.optString("expiry_date"));
            tvBatch.setTextColor(Color.parseColor("#64748B"));
            tvBatch.setTextSize(12);
            left.addView(tvBatch);

            row.addView(left);

            LinearLayout right = new LinearLayout(MainActivity.this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(android.view.Gravity.END);

            TextView tvStock = new TextView(MainActivity.this);
            int stock = med.optInt("stock_quantity", 0);
            tvStock.setText(stock + " in stock");
            tvStock.setTextColor(stock < 20 ? Color.parseColor("#E11D48") : Color.parseColor("#059669"));
            tvStock.setTextSize(12);
            tvStock.setTypeface(null, android.graphics.Typeface.BOLD);
            right.addView(tvStock);

            boolean isExpiringSoon = med.optBoolean("is_expiring_soon", false) || BillingCalculator.isExpiringWithinDays(med.optString("expiry_date"), 30);
            if (isExpiringSoon) {
                TextView tvAlert = new TextView(MainActivity.this);
                tvAlert.setText("Expiring Soon");
                tvAlert.setTextSize(10);
                tvAlert.setTextColor(Color.parseColor("#E11D48"));
                tvAlert.setBackgroundResource(R.drawable.bg_badge_red);
                tvAlert.setPadding(6, 2, 6, 2);
                right.addView(tvAlert);
            }

            row.addView(right);
            return row;
        }
    }
}