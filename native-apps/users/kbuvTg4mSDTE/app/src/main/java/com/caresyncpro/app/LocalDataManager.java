package com.caresyncpro.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalDataManager {

    private static final String PREF_NAME = "caresync_local_store";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_PATIENTS = "cached_patients";
    private static final String KEY_APPOINTMENTS = "cached_appointments";
    private static final String KEY_MEDICINES = "cached_medicines";
    private static final String KEY_PRESCRIPTIONS = "cached_prescriptions";

    private final SharedPreferences prefs;

    public LocalDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initDefaultMedicinesIfEmpty();
    }

    public void saveAuthToken(String token, String email) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void clearAuth() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_EMAIL).apply();
    }

    private void initDefaultMedicinesIfEmpty() {
        String existing = prefs.getString(KEY_MEDICINES, null);
        if (existing == null || existing.length() < 10) {
            JSONArray arr = new JSONArray();
            try {
                arr.put(createMedJson("Paracetamol 650mg", "BT2401", 140, 1.50, "2025-11-20"));
                arr.put(createMedJson("Amoxicillin 500mg", "BT2402", 45, 6.20, "2024-05-15"));
                arr.put(createMedJson("Metformin 500mg", "BT2399", 220, 2.10, "2026-03-10"));
                arr.put(createMedJson("Atorvastatin 10mg", "BT2410", 18, 8.40, "2024-04-28"));
                arr.put(createMedJson("Cetirizine 10mg", "BT2415", 300, 0.80, "2025-08-14"));
                arr.put(createMedJson("Azithromycin 250mg", "BT2418", 12, 12.00, "2024-05-02"));
            } catch (Exception ignored) {}
            prefs.edit().putString(KEY_MEDICINES, arr.toString()).apply();
        }

        // Initialize default patients if empty
        String existingPts = prefs.getString(KEY_PATIENTS, null);
        if (existingPts == null || existingPts.length() < 10) {
            JSONArray pArr = new JSONArray();
            try {
                pArr.put(createPtJson("MRN-1001", "Eleanor Vance", "+1 555-0192", "1988-04-12", "O+", "Penicillin", "120/80", "72", "98", "Hypertension"));
                pArr.put(createPtJson("MRN-1002", "Robert Sterling", "+1 555-0144", "1965-09-24", "A-", "Sulfa drugs", "138/88", "80", "145", "Type 2 Diabetes"));
                pArr.put(createPtJson("MRN-1003", "Aaliyah Khan", "+1 555-0188", "1995-12-05", "B+", "None", "115/75", "68", "92", "Seasonal Rhinitis"));
            } catch (Exception ignored) {}
            prefs.edit().putString(KEY_PATIENTS, pArr.toString()).apply();
        }

        // Initialize default appointments if empty
        String existingApts = prefs.getString(KEY_APPOINTMENTS, null);
        if (existingApts == null || existingApts.length() < 10) {
            JSONArray aArr = new JSONArray();
            try {
                aArr.put(createAptJson("MRN-1001", "Eleanor Vance", "Dr. Marcus Wells", "Cardiology", "09:30 AM", "Waiting", 75.00));
                aArr.put(createAptJson("MRN-1002", "Robert Sterling", "Dr. Sarah Jenkins", "Endocrinology", "10:15 AM", "In-Consultation", 90.00));
                aArr.put(createAptJson("MRN-1003", "Aaliyah Khan", "Dr. Alan Hayes", "General Medicine", "11:00 AM", "Completed", 50.00));
            } catch (Exception ignored) {}
            prefs.edit().putString(KEY_APPOINTMENTS, aArr.toString()).apply();
        }
    }

    private JSONObject createMedJson(String name, String batch, int stock, double price, String exp) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("batch_number", batch);
        obj.put("stock_quantity", stock);
        obj.put("unit_price", price);
        obj.put("expiry_date", exp);
        obj.put("is_expiring_soon", BillingCalculator.isExpiringWithinDays(exp, 30));
        return obj;
    }

    private JSONObject createPtJson(String mrn, String name, String phone, String dob, String blood, String allergies, String bp, String pulse, String sugar, String diag) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("mrn", mrn);
        obj.put("name", name);
        obj.put("phone", phone);
        obj.put("dob", dob);
        obj.put("age", BillingCalculator.calculateAge(dob));
        obj.put("blood_group", blood);
        obj.put("allergies", allergies);
        obj.put("vitals_bp", bp);
        obj.put("vitals_pulse", pulse);
        obj.put("vitals_sugar", sugar);
        obj.put("diagnosis", diag);
        return obj;
    }

    private JSONObject createAptJson(String mrn, String patient, String doctor, String dept, String slot, String status, double fee) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("mrn", mrn);
        obj.put("patient_name", patient);
        obj.put("doctor_name", doctor);
        obj.put("department", dept);
        obj.put("slot_time", slot);
        obj.put("status", status);
        obj.put("consultation_fee", fee);
        return obj;
    }

    // Patients
    public List<JSONObject> getPatients() {
        return readJsonList(KEY_PATIENTS);
    }

    public void savePatient(JSONObject pt) {
        List<JSONObject> list = getPatients();
        list.add(0, pt);
        writeJsonList(KEY_PATIENTS, list);
    }

    // Appointments
    public List<JSONObject> getAppointments() {
        return readJsonList(KEY_APPOINTMENTS);
    }

    public void saveAppointment(JSONObject apt) {
        List<JSONObject> list = getAppointments();
        list.add(0, apt);
        writeJsonList(KEY_APPOINTMENTS, list);
    }

    // Medicines
    public List<JSONObject> getMedicines() {
        return readJsonList(KEY_MEDICINES);
    }

    public void saveMedicine(JSONObject med) {
        List<JSONObject> list = getMedicines();
        list.add(0, med);
        writeJsonList(KEY_MEDICINES, list);
    }

    public void deductMedicineStock(String medicineName, int quantityToDeduct) {
        List<JSONObject> list = getMedicines();
        for (int i = 0; i < list.size(); i++) {
            JSONObject med = list.get(i);
            if (med.optString("name").equalsIgnoreCase(medicineName)) {
                int cur = med.optInt("stock_quantity", 0);
                int updated = Math.max(0, cur - quantityToDeduct);
                try {
                    med.put("stock_quantity", updated);
                } catch (Exception ignored) {}
                break;
            }
        }
        writeJsonList(KEY_MEDICINES, list);
    }

    // Prescriptions
    public List<JSONObject> getPrescriptions() {
        return readJsonList(KEY_PRESCRIPTIONS);
    }

    public void savePrescription(JSONObject rx) {
        List<JSONObject> list = getPrescriptions();
        list.add(0, rx);
        writeJsonList(KEY_PRESCRIPTIONS, list);
    }

    private List<JSONObject> readJsonList(String key) {
        List<JSONObject> result = new ArrayList<JSONObject>();
        String data = prefs.getString(key, "[]");
        try {
            JSONArray arr = new JSONArray(data);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getJSONObject(i));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private void writeJsonList(String key, List<JSONObject> list) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(list.get(i));
        }
        prefs.edit().putString(key, arr.toString()).apply();
    }
}