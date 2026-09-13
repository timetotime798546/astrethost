package com.doctordiary.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    private static final int SCREEN_DASHBOARD = 1;
    private static final int SCREEN_PATIENTS = 2;
    private static final int SCREEN_APPOINTMENTS = 3;
    private static final int SCREEN_PRESCRIPTIONS = 4;
    private static final int SCREEN_TOOLS = 5;

    private int currentScreenId = SCREEN_DASHBOARD;

    private String selectedApptDate = "";
    private String selectedApptTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        setupDashboardNavigation();
        setupCalculators();

        // Back Button click
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Search Patient listener
        EditText etSearch = (EditText) findViewById(R.id.et_patient_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshPatientsList(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add Patient Trigger
        findViewById(R.id.btn_add_patient).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewPatientDialog();
            }
        });

        // Add Appointment Trigger
        findViewById(R.id.btn_add_appointment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewAppointmentDialog();
            }
        });

        // Add Prescription Trigger
        findViewById(R.id.btn_add_prescription).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewPrescriptionDialog("");
            }
        });

        showScreen(SCREEN_DASHBOARD);
    }

    private void showScreen(int screenId) {
        currentScreenId = screenId;

        findViewById(R.id.layout_dashboard).setVisibility(screenId == SCREEN_DASHBOARD ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_patients).setVisibility(screenId == SCREEN_PATIENTS ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_appointments).setVisibility(screenId == SCREEN_APPOINTMENTS ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_prescriptions).setVisibility(screenId == SCREEN_PRESCRIPTIONS ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_tools).setVisibility(screenId == SCREEN_TOOLS ? View.VISIBLE : View.GONE);

        findViewById(R.id.btn_back).setVisibility(screenId == SCREEN_DASHBOARD ? View.GONE : View.VISIBLE);

        TextView tvTitle = (TextView) findViewById(R.id.tv_toolbar_title);
        switch (screenId) {
            case SCREEN_DASHBOARD:
                tvTitle.setText("Doctor Diary");
                updateDashboardStats();
                break;
            case SCREEN_PATIENTS:
                tvTitle.setText("Patient Registry");
                refreshPatientsList("");
                break;
            case SCREEN_APPOINTMENTS:
                tvTitle.setText("Appointment Planner");
                refreshAppointmentsList();
                break;
            case SCREEN_PRESCRIPTIONS:
                tvTitle.setText("Prescriptions");
                refreshPrescriptionsList();
                break;
            case SCREEN_TOOLS:
                tvTitle.setText("Clinical Calculators");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreenId != SCREEN_DASHBOARD) {
            showScreen(SCREEN_DASHBOARD);
        } else {
            super.onBackPressed();
        }
    }

    private void updateDashboardStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Count Patients
        Cursor curPatients = db.rawQuery("SELECT COUNT(*) FROM patients", null);
        int totalPatients = 0;
        if (curPatients != null && curPatients.moveToFirst()) {
            totalPatients = curPatients.getInt(0);
        }
        if (curPatients != null) curPatients.close();

        // Count Pending Appointments
        Cursor curAppts = db.rawQuery("SELECT COUNT(*) FROM appointments WHERE status = 'Pending'", null);
        int activeAppts = 0;
        if (curAppts != null && curAppts.moveToFirst()) {
            activeAppts = curAppts.getInt(0);
        }
        if (curAppts != null) curAppts.close();

        TextView tvPatientsCount = (TextView) findViewById(R.id.tv_stat_patients_count);
        TextView tvApptsCount = (TextView) findViewById(R.id.tv_stat_appointments_count);

        tvPatientsCount.setText(String.valueOf(totalPatients));
        tvApptsCount.setText(String.valueOf(activeAppts));
    }

    private void setupDashboardNavigation() {
        findViewById(R.id.btn_nav_patients).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(SCREEN_PATIENTS);
            }
        });

        findViewById(R.id.btn_nav_appointments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(SCREEN_APPOINTMENTS);
            }
        });

        findViewById(R.id.btn_nav_prescriptions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(SCREEN_PRESCRIPTIONS);
            }
        });

        findViewById(R.id.btn_nav_tools).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(SCREEN_TOOLS);
            }
        });
    }

    private void refreshPatientsList(String filter) {
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.container_patients_list);
        listContainer.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (filter != null && !filter.isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM patients WHERE name LIKE ? ORDER BY name ASC", new String[]{"%" + filter + "%"});
        } else {
            cursor = db.rawQuery("SELECT * FROM patients ORDER BY name ASC", null);
        }

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (16 * scale);
        int margin = (int) (10 * scale);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                final String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
                final String gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                final String contact = cursor.getString(cursor.getColumnIndexOrThrow("contact"));
                final String history = cursor.getString(cursor.getColumnIndexOrThrow("history"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(0xFFFFFFFF);
                gd.setCornerRadius(10 * scale);
                gd.setStroke(1, 0xFFE0E0E0);
                card.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, margin);
                card.setLayoutParams(lp);
                card.setPadding(padding, padding, padding, padding);

                TextView tvName = new TextView(this);
                tvName.setText(name + " (" + age + "y, " + gender + ")");
                tvName.setTextSize(16);
                tvName.setTextColor(0xFF00796B);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvName.setPadding(0, 0, 0, 4);
                card.addView(tvName);

                if (contact != null && !contact.isEmpty()) {
                    TextView tvContact = new TextView(this);
                    tvContact.setText("Phone: " + contact);
                    tvContact.setTextSize(13);
                    tvContact.setTextColor(0xFF555555);
                    tvContact.setPadding(0, 0, 0, 2);
                    card.addView(tvContact);
                }

                if (history != null && !history.isEmpty()) {
                    TextView tvHist = new TextView(this);
                    tvHist.setText("History: " + history);
                    tvHist.setTextSize(13);
                    tvHist.setTextColor(0xFF555555);
                    tvHist.setPadding(0, 0, 0, 12);
                    card.addView(tvHist);
                } else {
                    TextView tvHist = new TextView(this);
                    tvHist.setText("History: None declared");
                    tvHist.setTextSize(13);
                    tvHist.setTextColor(0xFF9E9E9E);
                    tvHist.setPadding(0, 0, 0, 12);
                    card.addView(tvHist);
                }

                LinearLayout actionsLayout = new LinearLayout(this);
                actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionsLayout.setGravity(android.view.Gravity.RIGHT);

                Button btnPres = new Button(this);
                btnPres.setText("Prescribe");
                btnPres.setBackgroundColor(0xFF00796B);
                btnPres.setTextColor(0xFFFFFFFF);
                btnPres.setTextSize(12);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * scale));
                p1.setMargins(0, 0, 8, 0);
                btnPres.setLayoutParams(p1);
                btnPres.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openNewPrescriptionDialog(name);
                    }
                });
                actionsLayout.addView(btnPres);

                Button btnDel = new Button(this);
                btnDel.setText("Delete");
                btnDel.setBackgroundColor(0xFFD32F2F);
                btnDel.setTextColor(0xFFFFFFFF);
                btnDel.setTextSize(12);
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * scale));
                btnDel.setLayoutParams(p2);
                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDeletePatient(id, name);
                    }
                });
                actionsLayout.addView(btnDel);

                card.addView(actionsLayout);
                listContainer.addView(card);
            } while (cursor.moveToNext());
        } else {
            TextView empty = new TextView(this);
            empty.setText("No patient records found.");
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(padding, padding, padding, padding);
            empty.setTextColor(0xFF757575);
            listContainer.addView(empty);
        }
        if (cursor != null) cursor.close();
    }

    private void refreshAppointmentsList() {
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.container_appointments_list);
        listContainer.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM appointments ORDER BY app_date ASC, app_time ASC", null);

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (16 * scale);
        int margin = (int) (10 * scale);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name"));
                final String appDate = cursor.getString(cursor.getColumnIndexOrThrow("app_date"));
                final String appTime = cursor.getString(cursor.getColumnIndexOrThrow("app_time"));
                final String purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose"));
                final String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(0xFFFFFFFF);
                gd.setCornerRadius(10 * scale);
                gd.setStroke(1, 0xFFE0E0E0);
                card.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, margin);
                card.setLayoutParams(lp);
                card.setPadding(padding, padding, padding, padding);

                RelativeLayout headerLine = new RelativeLayout(this);
                headerLine.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

                TextView tvName = new TextView(this);
                tvName.setText(patientName);
                tvName.setTextSize(16);
                tvName.setTextColor(0xFF00796B);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                RelativeLayout.LayoutParams lpName = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tvName.setLayoutParams(lpName);
                headerLine.addView(tvName);

                TextView tvStatus = new TextView(this);
                tvStatus.setText(status);
                tvStatus.setTextSize(11);
                tvStatus.setPadding((int) (6 * scale), (int) (2 * scale), (int) (6 * scale), (int) (2 * scale));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);

                GradientDrawable statusGd = new GradientDrawable();
                statusGd.setCornerRadius(4 * scale);
                if ("Completed".equals(status)) {
                    tvStatus.setTextColor(0xFFFFFFFF);
                    statusGd.setColor(0xFF388E3C);
                } else {
                    tvStatus.setTextColor(0xFFFFFFFF);
                    statusGd.setColor(0xFFF57C00);
                }
                tvStatus.setBackground(statusGd);

                RelativeLayout.LayoutParams lpStatus = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpStatus.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                tvStatus.setLayoutParams(lpStatus);
                headerLine.addView(tvStatus);

                card.addView(headerLine);

                TextView tvDateTime = new TextView(this);
                tvDateTime.setText("Schedule: " + appDate + " at " + appTime);
                tvDateTime.setTextSize(13);
                tvDateTime.setTextColor(0xFF555555);
                tvDateTime.setPadding(0, 4, 0, 2);
                card.addView(tvDateTime);

                if (purpose != null && !purpose.isEmpty()) {
                    TextView tvPurpose = new TextView(this);
                    tvPurpose.setText("Purpose: " + purpose);
                    tvPurpose.setTextSize(13);
                    tvPurpose.setTextColor(0xFF757575);
                    tvPurpose.setPadding(0, 0, 0, 12);
                    card.addView(tvPurpose);
                } else {
                    TextView tvPurpose = new TextView(this);
                    tvPurpose.setText("Purpose: General consultation");
                    tvPurpose.setTextSize(13);
                    tvPurpose.setTextColor(0xFF9E9E9E);
                    tvPurpose.setPadding(0, 0, 0, 12);
                    card.addView(tvPurpose);
                }

                LinearLayout actionsLayout = new LinearLayout(this);
                actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionsLayout.setGravity(android.view.Gravity.RIGHT);

                if (!"Completed".equals(status)) {
                    Button btnComplete = new Button(this);
                    btnComplete.setText("Mark Completed");
                    btnComplete.setBackgroundColor(0xFF388E3C);
                    btnComplete.setTextColor(0xFFFFFFFF);
                    btnComplete.setTextSize(11);
                    LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, (int) (34 * scale));
                    p1.setMargins(0, 0, 8, 0);
                    btnComplete.setLayoutParams(p1);
                    btnComplete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            markAppointmentCompleted(id);
                        }
                    });
                    actionsLayout.addView(btnComplete);
                }

                Button btnDel = new Button(this);
                btnDel.setText("Delete");
                btnDel.setBackgroundColor(0xFFD32F2F);
                btnDel.setTextColor(0xFFFFFFFF);
                btnDel.setTextSize(11);
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (34 * scale));
                btnDel.setLayoutParams(p2);
                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteAppointment(id);
                    }
                });
                actionsLayout.addView(btnDel);

                card.addView(actionsLayout);
                listContainer.addView(card);
            } while (cursor.moveToNext());
        } else {
            TextView empty = new TextView(this);
            empty.setText("No appointments scheduled yet.");
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(padding, padding, padding, padding);
            empty.setTextColor(0xFF757575);
            listContainer.addView(empty);
        }
        if (cursor != null) cursor.close();
    }

    private void refreshPrescriptionsList() {
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.container_prescriptions_list);
        listContainer.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM prescriptions ORDER BY id DESC", null);

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (16 * scale);
        int margin = (int) (10 * scale);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name"));
                final String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                final String diagnosis = cursor.getString(cursor.getColumnIndexOrThrow("diagnosis"));
                final String medicines = cursor.getString(cursor.getColumnIndexOrThrow("medicines"));
                final String advice = cursor.getString(cursor.getColumnIndexOrThrow("advice"));
                final String dateCreated = cursor.getString(cursor.getColumnIndexOrThrow("date_created"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(0xFFFFFFFF);
                gd.setCornerRadius(10 * scale);
                gd.setStroke(1, 0xFFE0E0E0);
                card.setBackground(gd);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, margin);
                card.setLayoutParams(lp);
                card.setPadding(padding, padding, padding, padding);

                TextView tvPatient = new TextView(this);
                tvPatient.setText("Rx: " + patientName);
                tvPatient.setTextSize(16);
                tvPatient.setTextColor(0xFF00796B);
                tvPatient.setTypeface(null, android.graphics.Typeface.BOLD);
                tvPatient.setPadding(0, 0, 0, 2);
                card.addView(tvPatient);

                TextView tvDate = new TextView(this);
                tvDate.setText("Date: " + dateCreated);
                tvDate.setTextSize(12);
                tvDate.setTextColor(0xFF9E9E9E);
                tvDate.setPadding(0, 0, 0, 8);
                card.addView(tvDate);

                if (diagnosis != null && !diagnosis.isEmpty()) {
                    TextView tvDiag = new TextView(this);
                    tvDiag.setText("Diagnosis: " + diagnosis);
                    tvDiag.setTextSize(13);
                    tvDiag.setTextColor(0xFF333333);
                    tvDiag.setTypeface(null, android.graphics.Typeface.ITALIC);
                    tvDiag.setPadding(0, 0, 0, 4);
                    card.addView(tvDiag);
                }

                if (medicines != null && !medicines.isEmpty()) {
                    TextView tvMed = new TextView(this);
                    tvMed.setText("Meds Written:\n" + medicines);
                    tvMed.setTextSize(13);
                    tvMed.setTextColor(0xFF555555);
                    tvMed.setPadding(0, 0, 0, 12);
                    card.addView(tvMed);
                }

                LinearLayout actionsLayout = new LinearLayout(this);
                actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionsLayout.setGravity(android.view.Gravity.RIGHT);

                Button btnView = new Button(this);
                btnView.setText("View / Share");
                btnView.setBackgroundColor(0xFF00796B);
                btnView.setTextColor(0xFFFFFFFF);
                btnView.setTextSize(11);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (34 * scale));
                p1.setMargins(0, 0, 8, 0);
                btnView.setLayoutParams(p1);
                btnView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPrescriptionDetailDialog(patientName, symptoms, diagnosis, medicines, advice, dateCreated);
                    }
                });
                actionsLayout.addView(btnView);

                Button btnDel = new Button(this);
                btnDel.setText("Delete");
                btnDel.setBackgroundColor(0xFFD32F2F);
                btnDel.setTextColor(0xFFFFFFFF);
                btnDel.setTextSize(11);
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, (int) (34 * scale));
                btnDel.setLayoutParams(p2);
                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deletePrescription(id);
                    }
                });
                actionsLayout.addView(btnDel);

                card.addView(actionsLayout);
                listContainer.addView(card);
            } while (cursor.moveToNext());
        } else {
            TextView empty = new TextView(this);
            empty.setText("No history of saved prescriptions.");
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(padding, padding, padding, padding);
            empty.setTextColor(0xFF757575);
            listContainer.addView(empty);
        }
        if (cursor != null) cursor.close();
    }

    private void openNewPatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_patient, null);

        final EditText etName = (EditText) view.findViewById(R.id.et_add_patient_name);
        final EditText etAge = (EditText) view.findViewById(R.id.et_add_patient_age);
        final Spinner spGender = (Spinner) view.findViewById(R.id.sp_add_patient_gender);
        final EditText etContact = (EditText) view.findViewById(R.id.et_add_patient_contact);
        final EditText etHistory = (EditText) view.findViewById(R.id.et_add_patient_history);

        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        builder.setView(view);
        builder.setPositiveButton("Save Record", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                String age = etAge.getText().toString().trim();
                String gender = spGender.getSelectedItem().toString();
                String contact = etContact.getText().toString().trim();
                String history = etHistory.getText().toString().trim();

                if (name.isEmpty() || age.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Error: Name and Age are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("name", name);
                cv.put("age", age);
                cv.put("gender", gender);
                cv.put("contact", contact);
                cv.put("history", history);

                long val = db.insert("patients", null, cv);
                if (val != -1) {
                    Toast.makeText(MainActivity.this, "Patient Registered Successfully", Toast.LENGTH_SHORT).show();
                    refreshPatientsList("");
                    updateDashboardStats();
                } else {
                    Toast.makeText(MainActivity.this, "Insertion failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openNewAppointmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_appointment, null);

        final Spinner spPatientName = (Spinner) view.findViewById(R.id.sp_appt_patient_name);
        final EditText etPatientManual = (EditText) view.findViewById(R.id.et_appt_patient_name_manual);
        final Button btnPickDate = (Button) view.findViewById(R.id.btn_appt_pick_date);
        final Button btnPickTime = (Button) view.findViewById(R.id.btn_appt_pick_time);
        final TextView tvDateTimeStatus = (TextView) view.findViewById(R.id.tv_appt_date_time_status);
        final EditText etPurpose = (EditText) view.findViewById(R.id.et_appt_purpose);

        selectedApptDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
        selectedApptTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvDateTimeStatus.setText("Selected Date/Time: " + selectedApptDate + " at " + selectedApptTime);

        // Fetch patients for spinner
        final List<String> patientList = new ArrayList<String>();
        patientList.add("-- Use Manual Text Field --");

        SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT name FROM patients ORDER BY name ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                patientList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> patientAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, patientList);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPatientName.setAdapter(patientAdapter);

        spPatientName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    etPatientManual.setVisibility(View.VISIBLE);
                } else {
                    etPatientManual.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view1, int year, int month, int dayOfMonth) {
                        Calendar temp = Calendar.getInstance();
                        temp.set(Calendar.YEAR, year);
                        temp.set(Calendar.MONTH, month);
                        temp.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        selectedApptDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(temp.getTime());
                        tvDateTimeStatus.setText("Selected Date/Time: " + selectedApptDate + " at " + selectedApptTime);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view1, int hourOfDay, int minute) {
                        Calendar temp = Calendar.getInstance();
                        temp.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        temp.set(Calendar.MINUTE, minute);
                        selectedApptTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(temp.getTime());
                        tvDateTimeStatus.setText("Selected Date/Time: " + selectedApptDate + " at " + selectedApptTime);
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Schedule", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String patientNameSelected = "";
                if (spPatientName.getSelectedItemPosition() == 0) {
                    patientNameSelected = etPatientManual.getText().toString().trim();
                } else {
                    patientNameSelected = spPatientName.getSelectedItem().toString();
                }

                String purpose = etPurpose.getText().toString().trim();

                if (patientNameSelected.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Patient Name cannot be blank", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("patient_name", patientNameSelected);
                cv.put("app_date", selectedApptDate);
                cv.put("app_time", selectedApptTime);
                cv.put("purpose", purpose);
                cv.put("status", "Pending");

                db.insert("appointments", null, cv);
                Toast.makeText(MainActivity.this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show();
                refreshAppointmentsList();
                updateDashboardStats();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openNewPrescriptionDialog(final String prefilledName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_prescription, null);

        final Spinner spPatientName = (Spinner) view.findViewById(R.id.sp_pres_patient_name);
        final EditText etPatientManual = (EditText) view.findViewById(R.id.et_pres_patient_name_manual);
        final EditText etSymptoms = (EditText) view.findViewById(R.id.et_pres_symptoms);
        final EditText etDiagnosis = (EditText) view.findViewById(R.id.et_pres_diagnosis);
        final EditText etMedicines = (EditText) view.findViewById(R.id.et_pres_medicines);
        final EditText etAdvice = (EditText) view.findViewById(R.id.et_pres_advice);

        final List<String> patientList = new ArrayList<String>();
        patientList.add("-- Use Manual Text Field --");

        SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT name FROM patients ORDER BY name ASC", null);
        int prefillPosition = 0;
        int index = 1;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                patientList.add(name);
                if (prefilledName != null && prefilledName.equals(name)) {
                    prefillPosition = index;
                }
                index++;
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> patientAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, patientList);
        patientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPatientName.setAdapter(patientAdapter);

        if (prefillPosition > 0) {
            spPatientName.setSelection(prefillPosition);
            etPatientManual.setVisibility(View.GONE);
        }

        spPatientName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    etPatientManual.setVisibility(View.VISIBLE);
                } else {
                    etPatientManual.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(view);
        builder.setPositiveButton("Save & Draft", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String patientNameSelected = "";
                if (spPatientName.getSelectedItemPosition() == 0) {
                    patientNameSelected = etPatientManual.getText().toString().trim();
                } else {
                    patientNameSelected = spPatientName.getSelectedItem().toString();
                }

                final String symptoms = etSymptoms.getText().toString().trim();
                final String diagnosis = etDiagnosis.getText().toString().trim();
                final String medicines = etMedicines.getText().toString().trim();
                final String advice = etAdvice.getText().toString().trim();
                final String todayDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());

                if (patientNameSelected.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Patient name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("patient_name", patientNameSelected);
                cv.put("symptoms", symptoms);
                cv.put("diagnosis", diagnosis);
                cv.put("medicines", medicines);
                cv.put("advice", advice);
                cv.put("date_created", todayDate);

                long val = db.insert("prescriptions", null, cv);
                if (val != -1) {
                    Toast.makeText(MainActivity.this, "Prescription Compiled Successfully", Toast.LENGTH_SHORT).show();
                    refreshPrescriptionsList();
                    showScreen(SCREEN_PRESCRIPTIONS);

                    final String finalPatientName = patientNameSelected;
                    // Propose immediately to share
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Prescription Saved")
                            .setMessage("Do you want to instantly share this prescription with the patient?")
                            .setPositiveButton("Share Rx", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog1, int which1) {
                                    sharePrescription(finalPatientName, symptoms, diagnosis, medicines, advice, todayDate);
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showPrescriptionDetailDialog(final String name, final String symptoms, final String diagnosis, final String medicines, final String advice, final String date) {
        StringBuilder message = new StringBuilder();
        message.append("Date: ").append(date).append("\n");
        message.append("Patient: ").append(name).append("\n\n");
        if (symptoms != null && !symptoms.isEmpty()) message.append("Symptoms: ").append(symptoms).append("\n");
        if (diagnosis != null && !diagnosis.isEmpty()) message.append("Diagnosis: ").append(diagnosis).append("\n\n");
        message.append("Medicines / Dosage:\n").append(medicines).append("\n\n");
        if (advice != null && !advice.isEmpty()) message.append("Advice: ").append(advice);

        new AlertDialog.Builder(this)
                .setTitle("Rx Details")
                .setMessage(message.toString())
                .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharePrescription(name, symptoms, diagnosis, medicines, advice, date);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void sharePrescription(String name, String symptoms, String diagnosis, String medicines, String advice, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRESCRIPTION (").append(date).append(")\n");
        sb.append("-----------------------------\n");
        sb.append("Patient: ").append(name).append("\n");
        if (symptoms != null && !symptoms.isEmpty()) {
            sb.append("Symptoms: ").append(symptoms).append("\n");
        }
        if (diagnosis != null && !diagnosis.isEmpty()) {
            sb.append("Diagnosis: ").append(diagnosis).append("\n");
        }
        sb.append("\nRx (Medicines):\n").append(medicines).append("\n\n");
        if (advice != null && !advice.isEmpty()) {
            sb.append("Instructions / Advice:\n").append(advice).append("\n");
        }
        sb.append("-----------------------------\n");
        sb.append("Generated via Doctor Diary Assistant app.");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Prescription for " + name);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Send Prescription via..."));
    }

    private void confirmDeletePatient(final int id, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete all clinic profiles for " + name + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("patients", "id=?", new String[]{String.valueOf(id)});
                        Toast.makeText(MainActivity.this, "Record Deleted", Toast.LENGTH_SHORT).show();
                        refreshPatientsList("");
                        updateDashboardStats();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markAppointmentCompleted(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "Completed");
        db.update("appointments", cv, "id=?", new String[]{String.valueOf(id)});
        Toast.makeText(this, "Appointment completed!", Toast.LENGTH_SHORT).show();
        refreshAppointmentsList();
        updateDashboardStats();
    }

    private void deleteAppointment(final int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("appointments", "id=?", new String[]{String.valueOf(id)});
        Toast.makeText(this, "Appointment schedule deleted", Toast.LENGTH_SHORT).show();
        refreshAppointmentsList();
        updateDashboardStats();
    }

    private void deletePrescription(final int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("prescriptions", "id=?", new String[]{String.valueOf(id)});
        Toast.makeText(this, "Prescription deleted from storage", Toast.LENGTH_SHORT).show();
        refreshPrescriptionsList();
    }

    private void setupCalculators() {
        // BMI Logic
        findViewById(R.id.btn_calc_bmi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etWeight = (EditText) findViewById(R.id.et_bmi_weight);
                EditText etHeight = (EditText) findViewById(R.id.et_bmi_height);
                TextView tvResult = (TextView) findViewById(R.id.tv_bmi_result);

                String wStr = etWeight.getText().toString().trim();
                String hStr = etHeight.getText().toString().trim();

                if (wStr.isEmpty() || hStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter correct weight & height values", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double w = Double.parseDouble(wStr);
                    double h = Double.parseDouble(hStr);
                    if (w <= 0 || h <= 0) {
                        Toast.makeText(MainActivity.this, "Input must be greater than zero", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double hm = h / 100.0;
                    double bmi = w / (hm * hm);
                    String category;
                    if (bmi < 18.5) category = "Underweight";
                    else if (bmi < 25.0) category = "Normal weight";
                    else if (bmi < 30.0) category = "Overweight";
                    else category = "Obese";

                    tvResult.setText(String.format(Locale.getDefault(), "BMI Status: %.2f (%s)", bmi, category));
                    tvResult.setTypeface(null, android.graphics.Typeface.BOLD);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid entry.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Pediatric Dosage calculator
        final Spinner spDrug = (Spinner) findViewById(R.id.sp_calc_drug);
        String[] drugs = {
                "Paracetamol (15 mg/kg per dose)",
                "Ibuprofen (10 mg/kg per dose)",
                "Amoxicillin (15 mg/kg per dose)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, drugs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDrug.setAdapter(adapter);

        findViewById(R.id.btn_calc_pediatric).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etWt = (EditText) findViewById(R.id.et_pediatric_weight);
                TextView tvRes = (TextView) findViewById(R.id.tv_pediatric_result);

                String wtStr = etWt.getText().toString().trim();
                if (wtStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert pediatric weight", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double wt = Double.parseDouble(wtStr);
                    if (wt <= 0) {
                        Toast.makeText(MainActivity.this, "Invalid child weight value", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pos = spDrug.getSelectedItemPosition();
                    double mgPerKg = (pos == 1) ? 10.0 : 15.0;
                    double targetMg = wt * mgPerKg;

                    double targetMl = 0.0;
                    String strengthDetails = "";
                    if (pos == 0) {
                        targetMl = targetMg / 24.0; // Paracetamol 120mg/5ml suspension = 24mg/ml
                        strengthDetails = "Paracetamol (120mg / 5ml liquid syrup suspension)";
                    } else if (pos == 1) {
                        targetMl = targetMg / 20.0; // Ibuprofen 100mg/5ml suspension = 20mg/ml
                        strengthDetails = "Ibuprofen (100mg / 5ml liquid syrup suspension)";
                    } else {
                        targetMl = targetMg / 25.0; // Amoxicillin 125mg/5ml suspension = 25mg/ml
                        strengthDetails = "Amoxicillin (125mg / 5ml liquid syrup suspension)";
                    }

                    tvRes.setText(String.format(Locale.getDefault(),
                            "Target Dose: %.1f mg\nVolume to prescribe: %.2f mL\n(Based on standard %s)",
                            targetMg, targetMl, strengthDetails));
                    tvRes.setTypeface(null, android.graphics.Typeface.BOLD);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Parsing error.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}