package com.boxstockmanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScanLogManager {

    private static final String TAG = "ScanLogManager";
    private static final String PREF_NAME = "BoxStockScanLogsPref";
    private static final String KEY_LOGS = "scan_text_logs_list";
    private static final int MAX_LOG_ENTRIES = 200;

    /**
     * Unified Full-Text Logging for BOTH Camera and Gallery scans.
     * Always builds fullText combining barcode/QR value and raw OCR text or [OCR-DIAG] summary.
     */
    public static synchronized void saveScanLog(Context context, String source, String barcodeValue, String fullText) {
        if (context == null) return;
        try {
            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String existingJson = pref.getString(KEY_LOGS, "[]");
            JSONArray oldArray = new JSONArray(existingJson);

            String timestamp = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());

            String barcodeClean = barcodeValue != null ? barcodeValue.trim() : "";
            String rawOcrClean = fullText != null ? fullText.trim() : "";

            StringBuilder combinedFullText = new StringBuilder();

            if (!barcodeClean.isEmpty()) {
                combinedFullText.append("[Barcode/QR]: ").append(barcodeClean);
            }

            if (!rawOcrClean.isEmpty()) {
                if (combinedFullText.length() > 0) {
                    combinedFullText.append("\n\n");
                }
                combinedFullText.append(rawOcrClean);
            } else {
                if (combinedFullText.length() > 0) {
                    combinedFullText.append("\n\n");
                }
                combinedFullText.append("[OCR-DIAG]\nNo raw text recognized by OCR engine.");
            }

            JSONObject newObj = new JSONObject();
            newObj.put("timestamp", timestamp);
            newObj.put("source", source != null ? source : "Camera");
            newObj.put("barcodeValue", barcodeClean);
            newObj.put("fullText", combinedFullText.toString());

            JSONArray newArray = new JSONArray();
            newArray.put(newObj);

            int count = Math.min(oldArray.length(), MAX_LOG_ENTRIES - 1);
            for (int i = 0; i < count; i++) {
                newArray.put(oldArray.get(i));
            }

            pref.edit().putString(KEY_LOGS, newArray.toString()).apply();
            Log.d(TAG, "Successfully saved scan log entry. Source: " + source + " | Total items: " + newArray.length());
        } catch (Exception e) {
            Log.e(TAG, "Error saving scan log entry: " + e.getMessage(), e);
        }
    }

    public static synchronized List<ScanLogItem> getScanLogs(Context context) {
        List<ScanLogItem> list = new ArrayList<>();
        if (context == null) return list;
        try {
            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonStr = pref.getString(KEY_LOGS, "[]");
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String time = obj.optString("timestamp", "");
                String src = obj.optString("source", "Camera");
                String barcode = obj.optString("barcodeValue", "");
                String text = obj.optString("fullText", "");
                list.add(new ScanLogItem(time, src, barcode, text));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving scan log entries: " + e.getMessage(), e);
        }
        return list;
    }

    public static synchronized void clearLogs(Context context) {
        if (context == null) return;
        try {
            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            pref.edit().remove(KEY_LOGS).apply();
            Log.d(TAG, "Cleared all saved scan logs.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing scan logs: " + e.getMessage(), e);
        }
    }
}