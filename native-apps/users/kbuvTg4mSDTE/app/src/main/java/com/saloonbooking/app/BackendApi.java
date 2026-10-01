package com.saloonbooking.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "";
    private String token = "";

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface ApiListCallback {
        void onSuccess(JSONArray records);
        void onError(String error);
    }

    public BackendApi(Context context) {
        loadAppMetadata(context);
    }

    public void setToken(String token) {
        this.token = token;
    }

    private void loadAppMetadata(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            JSONObject meta = new JSONObject(sb.toString());
            // Safe fallback or reading custom generated app_id
            this.appId = meta.optString("app_id", "salon-booking-custom-id");
            if (this.appId.isEmpty() || "salon-booking-custom-id".equals(this.appId)) {
                this.appId = meta.optString("package_name", "com.saloonbooking.app");
            }
        } catch (Exception e) {
            this.appId = "com.saloonbooking.app";
        }
    }

    public void register(final String email, final String password, final ApiCallback callback) {
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
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final JSONObject respObj = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(respObj);
                    } else {
                        callback.onError(respObj.optString("error", "Registration error"));
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void login(final String email, final String password, final ApiCallback callback) {
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
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final JSONObject respObj = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(respObj);
                    } else {
                        callback.onError(respObj.optString("error", "Login error"));
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void createBooking(final String service, final String stylist, final String date, final String time, final double basePrice, final double tax, final double total, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setDoOutput(true);

                    JSONObject bookingData = new JSONObject();
                    bookingData.put("service_name", service);
                    bookingData.put("stylist_name", stylist);
                    bookingData.put("booking_date", date);
                    bookingData.put("booking_time", time);
                    bookingData.put("base_price", basePrice);
                    bookingData.put("tax", tax);
                    bookingData.put("total_price", total);
                    bookingData.put("status", "Confirmed");

                    JSONObject body = new JSONObject();
                    body.put("collection", "bookings");
                    body.put("data", bookingData);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final JSONObject respObj = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && respObj.optBoolean("success", false)) {
                        callback.onSuccess(respObj.getJSONObject("record"));
                    } else {
                        callback.onError(respObj.optString("error", "Failed to book service."));
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void getBookings(final ApiListCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?collection=bookings");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final JSONObject respObj = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && respObj.optBoolean("success", false)) {
                        callback.onSuccess(respObj.getJSONArray("records"));
                    } else {
                        callback.onError(respObj.optString("error", "Failed to fetch bookings."));
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void cancelBooking(final String bookingId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?id=" + bookingId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final JSONObject respObj = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && respObj.optBoolean("success", false)) {
                        callback.onSuccess(respObj);
                    } else {
                        callback.onError(respObj.optString("error", "Failed to cancel booking."));
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }
}