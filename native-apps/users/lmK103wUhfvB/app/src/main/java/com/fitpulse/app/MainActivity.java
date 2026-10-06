package com.fitpulse.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "FitPulsePrefs";
    private static final String KEY_TOKEN = "authToken";
    private static final String KEY_EMAIL = "userEmail";

    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "";

    // Auth UI elements
    private LinearLayout authContainer;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private Button forgotPasswordButton;

    // Password Reset UI elements (Dialog)
    private AlertDialog resetDialog;

    // Main App UI elements
    private LinearLayout mainContainer;
    private TextView welcomeText;
    private Button logoutButton;

    // Workout Stats UI (Local Offline Calculations)
    private TextView totalWorkoutsText;
    private TextView totalDurationText;
    private TextView totalCaloriesText;

    // BMI Calculator UI (Local Offline Calculation)
    private EditText weightInput;
    private EditText heightInput;
    private TextView bmiResultText;
    private Button calculateBmiButton;

    // Workout Log UI
    private ListView workoutListView;
    private Button addWorkoutButton;

    // Data structures
    private List<Workout> workoutList = new ArrayList<>();
    private WorkoutAdapter workoutAdapter;

    // Thread/Handler for Async calls
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAppMetadata();
        initializeViews();
        setupListeners();
        checkAuthentication();
    }

    private void loadAppMetadata() {
        try {
            InputStream is = getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            appId = json.getString("package_name");
        } catch (Exception e) {
            Log.e("FitPulse", "Error loading app-meta.json", e);
            appId = "com.fitpulse.app"; // Fallback
        }
    }

    private void initializeViews() {
        authContainer = findViewById(R.id.auth_container);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);

        mainContainer = findViewById(R.id.main_container);
        welcomeText = findViewById(R.id.welcome_text);
        logoutButton = findViewById(R.id.logout_button);

        totalWorkoutsText = findViewById(R.id.total_workouts_text);
        totalDurationText = findViewById(R.id.total_duration_text);
        totalCaloriesText = findViewById(R.id.total_calories_text);

        weightInput = findViewById(R.id.weight_input);
        heightInput = findViewById(R.id.height_input);
        bmiResultText = findViewById(R.id.bmi_result_text);
        calculateBmiButton = findViewById(R.id.calculate_bmi_button);

        workoutListView = findViewById(R.id.workout_list_view);
        addWorkoutButton = findViewById(R.id.add_workout_button);

        workoutAdapter = new WorkoutAdapter();
        workoutListView.setAdapter(workoutAdapter);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        calculateBmiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBmiOffline();
            }
        });

        addWorkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddWorkoutDialog();
            }
        });
    }

    private void checkAuthentication() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        String email = prefs.getString(KEY_EMAIL, null);

        if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(email)) {
            showDashboard(email);
        } else {
            showAuthScreen();
        }
    }

    private void showAuthScreen() {
        authContainer.setVisibility(View.VISIBLE);
        mainContainer.setVisibility(View.GONE);
    }

    private void showDashboard(String email) {
        authContainer.setVisibility(View.GONE);
        mainContainer.setVisibility(View.VISIBLE);
        welcomeText.setText("Active User: " + email);
        fetchWorkouts();
    }

    private void saveSession(String token, String email) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    private void clearSession() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_EMAIL);
        editor.apply();
    }

    private String getToken() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TOKEN, "");
    }

    // --- Offline Calculations ---

    private void calculateBmiOffline() {
        String weightStr = weightInput.getText().toString().trim();
        String heightStr = heightInput.getText().toString().trim();

        if (TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(heightStr)) {
            Toast.makeText(this, "Please fill weight and height for BMI calculation", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            double heightCm = Double.parseDouble(heightStr);
            if (heightCm <= 0 || weight <= 0) {
                bmiResultText.setText("BMI: Invalid inputs");
                return;
            }

            double heightM = heightCm / 100.0;
            double bmi = weight / (heightM * heightM);

            String category;
            if (bmi < 18.5) {
                category = "Underweight";
            } else if (bmi < 24.9) {
                category = "Normal";
            } else if (bmi < 29.9) {
                category = "Overweight";
            } else {
                category = "Obese";
            }

            bmiResultText.setText(String.format("BMI: %.2f (%s)", bmi, category));
        } catch (Exception e) {
            bmiResultText.setText("BMI: Calculation Error");
        }
    }

    private void calculateWorkoutStatsOffline() {
        int totalWorkouts = workoutList.size();
        int totalDuration = 0;
        int totalCalories = 0;

        for (int i = 0; i < workoutList.size(); i++) {
            Workout w = workoutList.get(i);
            totalDuration += w.durationMins;
            totalCalories += w.caloriesBurned;
        }

        totalWorkoutsText.setText("Total Workouts\n" + totalWorkouts);
        totalDurationText.setText("Total Mins\n" + totalDuration);
        totalCaloriesText.setText("Total Calories\n" + totalCalories);
    }

    // --- Network API Requests ---

    private void performLogin() {
        final String email = emailInput.getText().toString().trim();
        final String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            sb.append(line);
                        }
                        in.close();

                        JSONObject resp = new JSONObject(sb.toString());
                        final String token = resp.getString("token");

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                saveSession(token, email);
                                showDashboard(email);
                                Toast.makeText(MainActivity.this, "Welcome Back!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        handleNetworkError(conn);
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void performRegistration() {
        final String email = emailInput.getText().toString().trim();
        final String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        // Registration success - Now call Login to get token (as register response does not contain token)
                        autoLoginAfterRegistration(email, password);
                    } else {
                        handleNetworkError(conn);
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void autoLoginAfterRegistration(final String email, final String password) {
        try {
            URL url = new URL(apiBaseUrl + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();

                JSONObject resp = new JSONObject(sb.toString());
                final String token = resp.getString("token");

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        saveSession(token, email);
                        showDashboard(email);
                        Toast.makeText(MainActivity.this, "Registered and Authenticated successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                handleNetworkError(conn);
            }
        } catch (final Exception e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Auto-login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void performLogout() {
        final String token = getToken();
        clearSession();
        showAuthScreen();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("FitPulse", "Logout broadcast failed", e);
                }
            }
        }).start();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText emailField = new EditText(this);
        emailField.setHint("Enter registered email");
        layout.addView(emailField);

        final EditText otpField = new EditText(this);
        otpField.setHint("Verification Code (OTP)");
        otpField.setVisibility(View.GONE);
        layout.addView(otpField);

        final EditText newPasswordField = new EditText(this);
        newPasswordField.setHint("New Secure Password");
        newPasswordField.setVisibility(View.GONE);
        layout.addView(newPasswordField);

        final TextView infoText = new TextView(this);
        infoText.setText("Step 1: Request confirmation code");
        infoText.setPadding(0, 10, 0, 10);
        layout.addView(infoText);

        builder.setView(layout);

        builder.setPositiveButton("Request Code", null);
        builder.setNegativeButton("Cancel", null);

        resetDialog = builder.create();
        resetDialog.show();

        // Custom action handlers to prevent early closing
        resetDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            private boolean step2 = false;

            @Override
            public void onClick(View v) {
                final String email = emailField.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!step2) {
                    // Send Step 1: Request OTP
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL(apiBaseUrl + "/request-otp");
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json");
                                conn.setDoOutput(true);

                                JSONObject body = new JSONObject();
                                body.put("app_id", appId);
                                body.put("email", email);

                                OutputStream os = conn.getOutputStream();
                                os.write(body.toString().getBytes("UTF-8"));
                                os.flush();
                                os.close();

                                int responseCode = conn.getResponseCode();
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            step2 = true;
                                            emailField.setEnabled(false);
                                            otpField.setVisibility(View.VISIBLE);
                                            newPasswordField.setVisibility(View.VISIBLE);
                                            infoText.setText("Step 2: Enter dynamic Code & new password");
                                            resetDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText("Reset Password");
                                            Toast.makeText(MainActivity.this, "OTP sent via recovery channel!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    handleNetworkError(conn);
                                }
                            } catch (Exception e) {
                                handleException(e);
                            }
                        }
                    }).start();
                } else {
                    // Send Step 2: Reset Password
                    final String otp = otpField.getText().toString().trim();
                    final String newPass = newPasswordField.getText().toString().trim();

                    if (TextUtils.isEmpty(otp) || TextUtils.isEmpty(newPass)) {
                        Toast.makeText(MainActivity.this, "OTP and password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL(apiBaseUrl + "/reset-password");
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json");
                                conn.setDoOutput(true);

                                JSONObject body = new JSONObject();
                                body.put("app_id", appId);
                                body.put("email", email);
                                body.put("otp", otp);
                                body.put("new_password", newPass);

                                OutputStream os = conn.getOutputStream();
                                os.write(body.toString().getBytes("UTF-8"));
                                os.flush();
                                os.close();

                                int responseCode = conn.getResponseCode();
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            resetDialog.dismiss();
                                            Toast.makeText(MainActivity.this, "Password reset successful!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    handleNetworkError(conn);
                                }
                            } catch (Exception e) {
                                handleException(e);
                            }
                        }
                    }).start();
                }
            }
        });
    }

    private void fetchWorkouts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?collection=workouts");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            sb.append(line);
                        }
                        in.close();

                        JSONObject resp = new JSONObject(sb.toString());
                        JSONArray records = resp.getJSONArray("records");

                        final List<Workout> temp = new ArrayList<>();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject recordObj = records.getJSONObject(i);
                            String id = recordObj.getString("id");
                            JSONObject dataObj = recordObj.getJSONObject("data");

                            String exercise = dataObj.getString("exercise");
                            int duration = dataObj.getInt("duration_mins");
                            int calories = dataObj.getInt("calories_burned");
                            String date = dataObj.getString("date");

                            temp.add(new Workout(id, exercise, duration, calories, date));
                        }

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                workoutList.clear();
                                workoutList.addAll(temp);
                                workoutAdapter.notifyDataSetChanged();
                                calculateWorkoutStatsOffline();
                            }
                        });
                    } else {
                        handleNetworkError(conn);
                    }
                } catch (Exception e) {
                    handleException(e);
                }
            }
        }).start();
    }

    private void showAddWorkoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log New Workout");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText exerciseInput = new EditText(this);
        exerciseInput.setHint("Exercise Name (e.g. Running, Yoga)");
        layout.addView(exerciseInput);

        final EditText durationInput = new EditText(this);
        durationInput.setHint("Duration (minutes)");
        durationInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(durationInput);

        final EditText caloriesInput = new EditText(this);
        caloriesInput.setHint("Calories Burned (calculated/estimated)");
        caloriesInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(caloriesInput);

        final EditText dateInput = new EditText(this);
        dateInput.setHint("Date (YYYY-MM-DD)");
        layout.addView(dateInput);

        builder.setView(layout);

        builder.setPositiveButton("Log Entry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String exercise = exerciseInput.getText().toString().trim();
                final String durationStr = durationInput.getText().toString().trim();
                final String caloriesStr = caloriesInput.getText().toString().trim();
                final String date = dateInput.getText().toString().trim();

                if (TextUtils.isEmpty(exercise) || TextUtils.isEmpty(durationStr) || TextUtils.isEmpty(caloriesStr) || TextUtils.isEmpty(date)) {
                    Toast.makeText(MainActivity.this, "All input parameters are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int duration = Integer.parseInt(durationStr);
                            int calories = Integer.parseInt(caloriesStr);

                            URL url = new URL(apiBaseUrl + "/data");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setRequestProperty("Authorization", "Bearer " + getToken());
                            conn.setDoOutput(true);

                            JSONObject body = new JSONObject();
                            body.put("collection", "workouts");

                            JSONObject dataPayload = new JSONObject();
                            dataPayload.put("exercise", exercise);
                            dataPayload.put("duration_mins", duration);
                            dataPayload.put("calories_burned", calories);
                            dataPayload.put("date", date);

                            body.put("data", dataPayload);

                            OutputStream os = conn.getOutputStream();
                            os.write(body.toString().getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int responseCode = conn.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Workout logged successfully!", Toast.LENGTH_SHORT).show();
                                        fetchWorkouts();
                                    }
                                });
                            } else {
                                handleNetworkError(conn);
                            }
                        } catch (Exception e) {
                            handleException(e);
                        }
                    }
                }).start();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteWorkoutEntry(final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?id=" + id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Workout log deleted", Toast.LENGTH_SHORT).show();
                                fetchWorkouts();
                            }
                        });
                    } else {
                        handleNetworkError(conn);
                    }
                } catch (Exception e) {
                    handleException(e);
                }
            }
        }).start();
    }

    // --- Common Error Handlers ---

    private void handleNetworkError(final HttpURLConnection conn) {
        try {
            InputStream es = conn.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(es));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject errObj = new JSONObject(sb.toString());
            final String message = errObj.optString("error", errObj.optString("message", "Request failed"));

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Server Response: " + message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (final Exception e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Connection Error (Code: " + getResponseCodeNoThrow(conn) + ")", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private int getResponseCodeNoThrow(HttpURLConnection conn) {
        try {
            return conn.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }

    private void handleException(final Exception e) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Operation failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Entity Model ---

    private static class Workout {
        String id;
        String exercise;
        int durationMins;
        int caloriesBurned;
        String date;

        Workout(String id, String exercise, int durationMins, int caloriesBurned, String date) {
            this.id = id;
            this.exercise = exercise;
            this.durationMins = durationMins;
            this.caloriesBurned = caloriesBurned;
            this.date = date;
        }
    }

    // --- Adapter Implementation ---

    private class WorkoutAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return workoutList.size();
        }

        @Override
        public Object getItem(int position) {
            return workoutList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            final Workout item = workoutList.get(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(item.exercise + " - " + item.durationMins + " mins");
            text2.setText(item.caloriesBurned + " kcal burned on " + item.date);

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Workout")
                        .setMessage("Are you sure you want to delete this session log?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteWorkoutEntry(item.id);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                    return true;
                }
            });

            return convertView;
        }
    }
}