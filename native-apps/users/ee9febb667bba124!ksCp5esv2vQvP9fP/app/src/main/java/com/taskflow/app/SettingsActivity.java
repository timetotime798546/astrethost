package com.taskflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class SettingsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private int loggedUserId;
    private Button btnExport, btnImport, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("taskflow_session", MODE_PRIVATE);
        loggedUserId = prefs.getInt("logged_user_id", -1);

        btnExport = (Button) findViewById(R.id.btnSettingsExport);
        btnImport = (Button) findViewById(R.id.btnSettingsImport);
        btnBack = (Button) findViewById(R.id.btnSettingsBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performJsonBackupExport();
            }
        });

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Confirm Restore")
                    .setMessage("Do you want to override and import your local device tasks backup data? This action cannot be reversed.")
                    .setPositiveButton("Proceed Import", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            performJsonBackupImport();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
    }

    private void performJsonBackupExport() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(DatabaseHelper.TABLE_ITEMS, null, DatabaseHelper.ITEM_COL_USER_ID + "=?",
                    new String[]{String.valueOf(loggedUserId)}, null, null, null);

            JSONArray array = new JSONArray();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("title", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_TITLE)));
                    obj.put("description", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DESC)));
                    obj.put("category", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_CATEGORY)));
                    obj.put("due_date", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DUE_DATE)));
                    obj.put("is_completed", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_COMPLETED)));
                    obj.put("is_favorite", cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_FAVORITE)));
                    array.put(obj);
                }
                cursor.close();
            }

            File file = new File(getExternalFilesDir(null), "taskflow_local_backup.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array.toString().getBytes());
            fos.close();

            dbHelper.logActivity(loggedUserId, "Data Backup Exported", "Successfully backed up tasks array to files path " + file.getName());
            Toast.makeText(this, "Backup successfully saved to " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup creation failed locally: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performJsonBackupImport() {
        try {
            File file = new File(getExternalFilesDir(null), "taskflow_local_backup.json");
            if (!file.exists()) {
                Toast.makeText(this, "No backup file located! Export backup structure first.", Toast.LENGTH_LONG).show();
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String rawString = new String(data, "UTF-8");
            JSONArray array = new JSONArray(rawString);

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Clear existing first for the user context
                db.delete(DatabaseHelper.TABLE_ITEMS, DatabaseHelper.ITEM_COL_USER_ID + "=?", new String[]{String.valueOf(loggedUserId)});

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ContentValues val = new ContentValues();
                    val.put(DatabaseHelper.ITEM_COL_USER_ID, loggedUserId);
                    val.put(DatabaseHelper.ITEM_COL_TITLE, obj.getString("title"));
                    val.put(DatabaseHelper.ITEM_COL_DESC, obj.getString("description"));
                    val.put(DatabaseHelper.ITEM_COL_CATEGORY, obj.getString("category"));
                    val.put(DatabaseHelper.ITEM_COL_DUE_DATE, obj.getString("due_date"));
                    val.put(DatabaseHelper.ITEM_COL_COMPLETED, obj.getInt("is_completed"));
                    val.put(DatabaseHelper.ITEM_COL_FAVORITE, obj.getInt("is_favorite"));
                    db.insert(DatabaseHelper.TABLE_ITEMS, null, val);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            dbHelper.logActivity(loggedUserId, "Data Backup Restored", "Restored total objects count: " + array.length());
            Toast.makeText(this, "Success! Imported " + array.length() + " tasks successfully.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup restoration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}