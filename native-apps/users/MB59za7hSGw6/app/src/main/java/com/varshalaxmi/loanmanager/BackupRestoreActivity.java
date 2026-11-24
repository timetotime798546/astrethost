package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupRestoreActivity extends Activity {

    private static final int REQUEST_CODE_CREATE_FILE = 1001;
    private static final int REQUEST_CODE_OPEN_FILE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        Button btnExport = (Button) findViewById(R.id.btn_export_backup);
        Button btnImport = (Button) findViewById(R.id.btn_import_backup);

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "varshalaxmi_backup_" + DatabaseHelper.getTodayDateString() + ".db");
                startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
            }
        });

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(BackupRestoreActivity.this)
                        .setTitle("Confirm Restore")
                        .setMessage("Restoring a backup will overwrite your current loan database. Are you sure you want to proceed?")
                        .setPositiveButton("Proceed & Restore", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");
                                startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_CREATE_FILE) {
                exportDatabaseToUri(uri);
            } else if (requestCode == REQUEST_CODE_OPEN_FILE) {
                importDatabaseFromUri(uri);
            }
        }
    }

    private void exportDatabaseToUri(Uri targetUri) {
        try {
            File currentDb = getDatabasePath("varshalaxmi_loans.db");
            if (!currentDb.exists()) {
                Toast.makeText(this, "Database file does not exist yet!", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream is = new FileInputStream(currentDb);
            OutputStream os = getContentResolver().openOutputStream(targetUri);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            os.flush();
            os.close();
            is.close();

            Toast.makeText(this, "Backup exported successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabaseFromUri(Uri sourceUri) {
        try {
            File currentDb = getDatabasePath("varshalaxmi_loans.db");

            InputStream is = getContentResolver().openInputStream(sourceUri);
            OutputStream os = new FileOutputStream(currentDb);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            os.flush();
            os.close();
            is.close();

            Toast.makeText(this, "Database restored successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}