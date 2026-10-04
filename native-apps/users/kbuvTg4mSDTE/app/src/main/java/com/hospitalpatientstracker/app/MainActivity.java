package com.hospitalpatientstracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private BackendApi api;
    private String appId;
    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    // Application state lists
    private final ArrayList<Patient> fullPatientsList = new ArrayList<>();
    private final ArrayList<Patient> filteredPatientsList = new ArrayList<>();
    private PatientAdapter listAdapter;

    // View Groups (Containers) representing screens
    private ScrollView loginContainer;
    private ScrollView registerContainer;
    private ScrollView forgotPasswordContainer;
    private LinearLayout dashboardContainer;
    private ScrollView formContainer;

    // Loading overlay
    private RelativeLayout loadingOverlay;
    private TextView loadingText;

    // Dashboard references
    private TextView statTotalPatients;
    private TextView statActiveAdmitted;
    private TextView statAvgAge;
    private EditText searchQuery;
    private TextView filteredCountText;
    private ListView patientsListView;

    // Input views for authentication
    private EditText loginEmail;
    private EditText loginPassword;
    private EditText regEmail;
    private EditText regPassword;
    private EditText regConfirmPassword;
    private EditText forgotEmail;
    private EditText forgotOtp;
    private EditText forgotNewPassword;

    // Input views for Form entry
    private EditText formName;
    private EditText formAge;
    private Spinner formGenderSpinner;
    private EditText formIllness;
    private EditText formRoom;
    private Spinner formStatusSpinner;
    private EditText formDate;
    private TextView formTitle;

    // Context record tracking (creation vs editing updates)
    private Patient editingPatient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadMetadata();
        initializeApi();
        bindViewElements();
        setupInputSpinners();
        setupViewListeners();
        checkPersistentSession();
    }

    private void loadMetadata() {
        try {
            InputStream is = getAssets().open("app-meta.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            JSONObject meta = new JSONObject(new String(buf, "UTF-8"));
            appId = meta.optString("app_id", "com.hospitalpatientstracker.app");
        } catch (Exception e) {
            e.printStackTrace();
            appId = "com.hospitalpatientstracker.app"; // local fallback if missing
        }
    }

    private void initializeApi() {
        api = new BackendApi(apiBaseUrl, appId);
    }

    private void bindViewElements() {
        // Screens
        loginContainer = (ScrollView) findViewById(R.id.login_container);
        registerContainer = (ScrollView) findViewById(R.id.register_container);
        forgotPasswordContainer = (ScrollView) findViewById(R.id.forgot_password_container);
        dashboardContainer = (LinearLayout) findViewById(R.id.dashboard_container);
        formContainer = (ScrollView) findViewById(R.id.form_container);

        // Global Overlay
        loadingOverlay = (RelativeLayout) findViewById(R.id.loading_overlay);
        loadingText = (TextView) findViewById(R.id.loading_text);

        // Auth input elements
        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPassword = (EditText) findViewById(R.id.login_password);
        regEmail = (EditText) findViewById(R.id.reg_email);
        regPassword = (EditText) findViewById(R.id.reg_password);
        regConfirmPassword = (EditText) findViewById(R.id.reg_confirm_password);
        forgotEmail = (EditText) findViewById(R.id.forgot_email);
        forgotOtp = (EditText) findViewById(R.id.forgot_otp);
        forgotNewPassword = (EditText) findViewById(R.id.forgot_new_password);

        // Dashboard
        statTotalPatients = (TextView) findViewById(R.id.stat_total_patients);
        statActiveAdmitted = (TextView) findViewById(R.id.stat_active_admitted);
        statAvgAge = (TextView) findViewById(R.id.stat_avg_age);
        searchQuery = (EditText) findViewById(R.id.search_query);
        filteredCountText = (TextView) findViewById(R.id.filtered_count_text);
        patientsListView = (ListView) findViewById(R.id.patients_listview);

        // Patients form inputs
        formName = (EditText) findViewById(R.id.form_name);
        formAge = (EditText) findViewById(R.id.form_age);
        formGenderSpinner = (Spinner) findViewById(R.id.form_gender_spinner);
        formIllness = (EditText) findViewById(R.id.form_illness);
        formRoom = (EditText) findViewById(R.id.form_room);
        formStatusSpinner = (Spinner) findViewById(R.id.form_status_spinner);
        formDate = (EditText) findViewById(R.id.form_date);
        formTitle = (TextView) findViewById(R.id.form_title);

        listAdapter = new PatientAdapter();
        patientsListView.setAdapter(listAdapter);
    }

    private void setupInputSpinners() {
        String[] genders = {"Male", "Female", "Other", "Undisclosed"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        formGenderSpinner.setAdapter(genderAdapter);

        String[] statuses = {"Admitted", "Discharged"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        formStatusSpinner.setAdapter(statusAdapter);
    }

    private void showLoading(String msg) {
        loadingText.setText(msg);
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    private void navigateTo(View viewToShow) {
        loginContainer.setVisibility(View.GONE);
        registerContainer.setVisibility(View.GONE);
        forgotPasswordContainer.setVisibility(View.GONE);
        dashboardContainer.setVisibility(View.GONE);
        formContainer.setVisibility(View.GONE);

        viewToShow.setVisibility(View.VISIBLE);
    }

    private void checkPersistentSession() {
        SharedPreferences prefs = getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE);
        String savedToken = prefs.getString("user_token", null);
        if (savedToken != null) {
            api.setToken(savedToken);
            navigateTo(dashboardContainer);
            loadPatientData();
        } else {
            navigateTo(loginContainer);
        }
    }

    private void saveSessionToken(String token) {
        SharedPreferences prefs = getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("user_token", token).commit();
    }

    private void clearSession() {
        SharedPreferences prefs = getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE);
        prefs.edit().remove("user_token").commit();
        api.setToken(null);
    }

    private void setupViewListeners() {
        // AUTH TRIGGERS
        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = loginEmail.getText().toString().trim();
                final String pass = loginPassword.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Provide login credentials.", Toast.LENGTH_SHORT).show();
                    return;
                }
                showLoading("Verifying Credentials...");
                api.login(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        hideLoading();
                        try {
                            JSONObject root = new JSONObject(rawResponse);
                            if (root.optBoolean("success", false)) {
                                String token = root.getString("token");
                                api.setToken(token);
                                saveSessionToken(token);
                                Toast.makeText(MainActivity.this, "Authentication Succeeded!", Toast.LENGTH_SHORT).show();
                                navigateTo(dashboardContainer);
                                loadPatientData();
                                loginPassword.setText("");
                            } else {
                                Toast.makeText(MainActivity.this, "Authentication failed. Check entry details.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Data parse issue: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Access Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        findViewById(R.id.txt_go_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo(registerContainer);
            }
        });

        findViewById(R.id.txt_forgot_pwd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo(forgotPasswordContainer);
            }
        });

        findViewById(R.id.txt_go_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo(loginContainer);
            }
        });

        findViewById(R.id.txt_back_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo(loginContainer);
            }
        });

        // OTP RESET TRIGGERS
        findViewById(R.id.btn_request_otp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailVal = forgotEmail.getText().toString().trim();
                if (emailVal.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter registered email.", Toast.LENGTH_SHORT).show();
                    return;
                }
                showLoading("Requesting OTP...");
                api.requestOtp(emailVal, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "OTP sent successfully if email exists.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "OTP Request failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        findViewById(R.id.btn_reset_pwd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailVal = forgotEmail.getText().toString().trim();
                String otpVal = forgotOtp.getText().toString().trim();
                String newPass = forgotNewPassword.getText().toString().trim();

                if (emailVal.isEmpty() || otpVal.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "All fields required to reset.", Toast.LENGTH_SHORT).show();
                    return;
                }

                showLoading("Resetting Password...");
                api.resetPassword(emailVal, otpVal, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Reset successful! Please login with new password.", Toast.LENGTH_LONG).show();
                        navigateTo(loginContainer);
                        forgotOtp.setText("");
                        forgotNewPassword.setText("");
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Reset failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = regEmail.getText().toString().trim();
                final String pass = regPassword.getText().toString().trim();
                String confirm = regConfirmPassword.getText().toString().trim();

                if (email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill out all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!pass.equals(confirm)) {
                    Toast.makeText(MainActivity.this, "Passwords mismatch.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pass.length() < 6) {
                    Toast.makeText(MainActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                    return;
                }

                showLoading("Registering User...");
                api.register(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        // Success confirms signup, does NOT contain token.
                        // Authenticate right after registration!
                        api.login(email, pass, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(String loginResponse) {
                                hideLoading();
                                try {
                                    JSONObject loginObj = new JSONObject(loginResponse);
                                    if (loginObj.optBoolean("success", false)) {
                                        String token = loginObj.getString("token");
                                        api.setToken(token);
                                        saveSessionToken(token);
                                        Toast.makeText(MainActivity.this, "Account Active!", Toast.LENGTH_SHORT).show();
                                        navigateTo(dashboardContainer);
                                        loadPatientData();
                                        regEmail.setText("");
                                        regPassword.setText("");
                                        regConfirmPassword.setText("");
                                    } else {
                                        navigateTo(loginContainer);
                                    }
                                } catch (Exception e) {
                                    navigateTo(loginContainer);
                                }
                            }

                            @Override
                            public void onError(String errorMsg) {
                                hideLoading();
                                navigateTo(loginContainer);
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Sign up Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // DASHBOARD ACTIONS
        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading("Signing Out...");
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        hideLoading();
                        clearSession();
                        navigateTo(loginContainer);
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        clearSession();
                        navigateTo(loginContainer);
                    }
                });
            }
        });

        findViewById(R.id.btn_add_patient_trigger).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editingPatient = null;
                formTitle.setText("New Admission Log");
                formName.setText("");
                formAge.setText("");
                formIllness.setText("");
                formRoom.setText("");
                formGenderSpinner.setSelection(0);
                formStatusSpinner.setSelection(0);
                
                // Set current date prefilled
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                formDate.setText(sdf.format(new Date()));

                navigateTo(formContainer);
            }
        });

        // Search text watcher
        searchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                applyLocalSearchFilter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // FORM CRUD BUTTONS
        findViewById(R.id.btn_cancel_form).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateTo(dashboardContainer);
            }
        });

        findViewById(R.id.btn_save_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePatientRecordFromInputs();
            }
        });
    }

    private void loadPatientData() {
        showLoading("Updating Patient Database...");
        api.readRecords("patients", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String rawResponse) {
                hideLoading();
                try {
                    JSONObject root = new JSONObject(rawResponse);
                    if (root.optBoolean("success", false)) {
                        JSONArray recordsArr = root.optJSONArray("records");
                        fullPatientsList.clear();
                        if (recordsArr != null) {
                            for (int i = 0; i < recordsArr.length(); i++) {
                                Patient p = Patient.fromJson(recordsArr.getJSONObject(i));
                                if (p != null) {
                                    fullPatientsList.add(p);
                                }
                            }
                        }
                        applyLocalSearchFilter(searchQuery.getText().toString());
                    } else {
                        Toast.makeText(MainActivity.this, "Data read error.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Render issue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMsg) {
                hideLoading();
                Toast.makeText(MainActivity.this, "Database fetch issue: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // MANDATORY LOCAL OFFLINE CALCULATIONS
    private void computeOfflineStatistics() {
        int total = fullPatientsList.size();
        int activeCount = 0;
        int totalAgeSum = 0;
        int ageValidCount = 0;

        for (int i = 0; i < fullPatientsList.size(); i++) {
            Patient p = fullPatientsList.get(i);
            if ("Admitted".equalsIgnoreCase(p.status)) {
                activeCount++;
            }
            if (p.age >= 0) {
                totalAgeSum += p.age;
                ageValidCount++;
            }
        }

        double averageAge = ageValidCount > 0 ? ((double) totalAgeSum / ageValidCount) : 0.0;

        // Round off avg age to single decimal place
        double roundedAvgAge = Math.round(averageAge * 10.0) / 10.0;

        statTotalPatients.setText(String.valueOf(total));
        statActiveAdmitted.setText(String.valueOf(activeCount));
        statAvgAge.setText(String.valueOf(roundedAvgAge));
    }

    private void applyLocalSearchFilter(String query) {
        filteredPatientsList.clear();
        String lowercaseQuery = query.toLowerCase().trim();

        for (int i = 0; i < fullPatientsList.size(); i++) {
            Patient p = fullPatientsList.get(i);
            if (lowercaseQuery.isEmpty() || p.name.toLowerCase().contains(lowercaseQuery) || p.illness.toLowerCase().contains(lowercaseQuery)) {
                filteredPatientsList.add(p);
            }
        }

        if (!lowercaseQuery.isEmpty()) {
            filteredCountText.setText("Filtered Results: " + filteredPatientsList.size() + " of " + fullPatientsList.size());
        } else {
            filteredCountText.setText("");
        }

        computeOfflineStatistics();
        listAdapter.notifyDataSetChanged();
    }

    private void savePatientRecordFromInputs() {
        String name = formName.getText().toString().trim();
        String ageStr = formAge.getText().toString().trim();
        String gender = formGenderSpinner.getSelectedItem().toString();
        String illness = formIllness.getText().toString().trim();
        String room = formRoom.getText().toString().trim();
        String status = formStatusSpinner.getSelectedItem().toString();
        String date = formDate.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || illness.isEmpty() || room.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Complete all patient details fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 0 || age > 150) {
                Toast.makeText(this, "Please enter a valid age.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Numeric format error on age.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate basic date format
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Enter date in proper YYYY-MM-DD template.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Transmitting Admission Record...");

        final Patient uploadObject = new Patient();
        uploadObject.name = name;
        uploadObject.age = age;
        uploadObject.gender = gender;
        uploadObject.illness = illness;
        uploadObject.roomNumber = room;
        uploadObject.status = status;
        uploadObject.admissionDate = date;

        if (editingPatient == null) {
            // New Create request
            api.createRecord("patients", uploadObject.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String rawResponse) {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Patient logged successfully!", Toast.LENGTH_SHORT).show();
                    navigateTo(dashboardContainer);
                    loadPatientData();
                }

                @Override
                public void onError(String errorMsg) {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Network save failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Edit update request
            api.updateRecord(editingPatient.id, uploadObject.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String rawResponse) {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Patient update applied!", Toast.LENGTH_SHORT).show();
                    navigateTo(dashboardContainer);
                    loadPatientData();
                }

                @Override
                public void onError(String errorMsg) {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Database update failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void deletePatientRecord(final Patient p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you certain you want to purge patient record: " + p.name + "?");
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showLoading("Purging Patient Records...");
                api.deleteRecord(p.id, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String rawResponse) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Record purged.", Toast.LENGTH_SHORT).show();
                        loadPatientData();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Purge failure: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void editPatientTrigger(Patient p) {
        editingPatient = p;
        formTitle.setText("Update Record: " + p.name);
        formName.setText(p.name);
        formAge.setText(String.valueOf(p.age));
        formIllness.setText(p.illness);
        formRoom.setText(p.roomNumber);
        formDate.setText(p.admissionDate);

        // Select Gender
        int genderIndex = 0;
        String[] genders = {"Male", "Female", "Other", "Undisclosed"};
        for (int i = 0; i < genders.length; i++) {
            if (genders[i].equalsIgnoreCase(p.gender)) {
                genderIndex = i;
                break;
            }
        }
        formGenderSpinner.setSelection(genderIndex);

        // Select Status
        int statusIndex = 0;
        if ("Discharged".equalsIgnoreCase(p.status)) {
            statusIndex = 1;
        }
        formStatusSpinner.setSelection(statusIndex);

        navigateTo(formContainer);
    }

    // Dynamic Patient Adapter List Class
    private class PatientAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredPatientsList.size();
        }

        @Override
        public Object getItem(int i) {
            return filteredPatientsList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            final Patient p = filteredPatientsList.get(position);

            // Create custom inner relative view representation
            LinearLayout mainLay = new LinearLayout(MainActivity.this);
            mainLay.setOrientation(LinearLayout.VERTICAL);
            mainLay.setPadding(14, 14, 14, 14);
            mainLay.setBackgroundResource(R.drawable.card_bg);

            // Title Line (Name, Age, Gender)
            LinearLayout titleLay = new LinearLayout(MainActivity.this);
            titleLay.setOrientation(LinearLayout.HORIZONTAL);
            
            TextView txtName = new TextView(MainActivity.this);
            txtName.setText(p.name);
            txtName.setTextSize(16spToPx());
            txtName.setTextColor(Color.parseColor("#004D40"));
            txtName.setTypeface(null, android.graphics.Typeface.BOLD);
            titleLay.addView(txtName);

            TextView txtSubInfo = new TextView(MainActivity.this);
            txtSubInfo.setText(" (" + p.gender + ", Age: " + p.age + ")");
            txtSubInfo.setTextSize(14spToPx());
            txtSubInfo.setTextColor(Color.parseColor("#757575"));
            titleLay.addView(txtSubInfo);

            mainLay.addView(titleLay);

            // Illness and Room Assignment details
            TextView txtIllness = new TextView(MainActivity.this);
            txtIllness.setText("Diagnosis: " + p.illness);
            txtIllness.setPadding(0, 4, 0, 2);
            txtIllness.setTextColor(Color.parseColor("#37474F"));
            txtIllness.setTextSize(14spToPx());
            mainLay.addView(txtIllness);

            TextView txtRoomAndDate = new TextView(MainActivity.this);
            txtRoomAndDate.setText("Loc: " + p.roomNumber + " | Admitted: " + p.admissionDate);
            txtRoomAndDate.setTextColor(Color.parseColor("#546E7A"));
            txtRoomAndDate.setTextSize(13spToPx());
            mainLay.addView(txtRoomAndDate);

            // Status Indicator tag
            TextView txtStatus = new TextView(MainActivity.this);
            txtStatus.setText(p.status.toUpperCase());
            txtStatus.setTextSize(11spToPx());
            txtStatus.setPadding(10, 4, 10, 4);
            txtStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            txtStatus.setTextColor(Color.WHITE);
            
            LinearLayout.LayoutParams lpStatus = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpStatus.topMargin = 8;
            txtStatus.setLayoutParams(lpStatus);

            if ("Admitted".equalsIgnoreCase(p.status)) {
                txtStatus.setBackgroundColor(Color.parseColor("#E65100")); // Orange
            } else {
                txtStatus.setBackgroundColor(Color.parseColor("#2E7D32")); // Green
            }
            mainLay.addView(txtStatus);

            // Controls row (Edit / Delete)
            LinearLayout actionsLay = new LinearLayout(MainActivity.this);
            actionsLay.setOrientation(LinearLayout.HORIZONTAL);
            actionsLay.setGravity(android.view.Gravity.RIGHT);
            actionsLay.setPadding(0, 6, 0, 0);

            Button editBtn = new Button(MainActivity.this);
            editBtn.setText("EDIT");
            editBtn.setTextSize(11spToPx());
            editBtn.setTextColor(Color.parseColor("#006064"));
            editBtn.setBackgroundColor(Color.TRANSPARENT);
            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editPatientTrigger(p);
                }
            });
            actionsLay.addView(editBtn);

            Button delBtn = new Button(MainActivity.this);
            delBtn.setText("DISMISS");
            delBtn.setTextSize(11spToPx());
            delBtn.setTextColor(Color.parseColor("#C62828"));
            delBtn.setBackgroundColor(Color.TRANSPARENT);
            delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deletePatientRecord(p);
                }
            });
            actionsLay.addView(delBtn);

            mainLay.addView(actionsLay);
            return mainLay;
        }

        private float spToPx() {
            return 14.0f; // Scale helper logic representation
        }

        private float spToPx(int size) {
            return (float) size;
        }

        private float 16spToPx() { return 16.0f; }
        private float 14spToPx() { return 14.0f; }
        private float 13spToPx() { return 13.0f; }
        private float 11spToPx() { return 11.0f; }
    }
}