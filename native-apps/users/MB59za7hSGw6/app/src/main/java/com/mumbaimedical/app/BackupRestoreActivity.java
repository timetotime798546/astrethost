package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreActivity extends Activity {

    private static final int REQUEST_CREATE_BACKUP = 2001;
    private static final int REQUEST_SELECT_RESTORE = 2002;

    private Button btnExportBackup, btnRestoreBackup;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        btnExportBackup = findViewById(R.id.btnExportBackup);
        btnRestoreBackup = findViewById(R.id.btnRestoreBackup);

        btnExportBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSaveLocationPicker();
            }
        });

        btnRestoreBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRestore();
            }
        });
    }

    private void openSaveLocationPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "MumbaiMedical_Backup_" + timeStamp + ".zip");
        startActivityForResult(intent, REQUEST_CREATE_BACKUP);
    }

    private void confirmRestore() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_confirm_title)
                .setMessage(R.string.restore_confirm_msg)
                .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openRestoreFilePicker();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openRestoreFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_SELECT_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CREATE_BACKUP) {
                writeBackupToUri(uri);
            } else if (requestCode == REQUEST_SELECT_RESTORE) {
                restoreBackupFromUri(uri);
            }
        }
    }

    private void writeBackupToUri(Uri destUri) {
        try {
            File dbFile = getDatabasePath("mumbaimedical.db");
            if (!dbFile.exists()) {
                Toast.makeText(this, "Database file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream os = getContentResolver().openOutputStream(destUri);
            if (os == null) {
                Toast.makeText(this, "Cannot write to selected destination", Toast.LENGTH_SHORT).show();
                return;
            }

            ZipOutputStream zos = new ZipOutputStream(os);
            FileInputStream fis = new FileInputStream(dbFile);

            ZipEntry zipEntry = new ZipEntry("mumbaimedical.db");
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[2048];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            fis.close();
            zos.close();
            os.close();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.backup)
                    .setMessage(getString(R.string.backup_success) + "\n\nLocation: " + destUri.getPath())
                    .setPositiveButton(R.string.ok, null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Backup Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreBackupFromUri(Uri sourceUri) {
        File tempDbFile = new File(getCacheDir(), "temp_restore.db");
        try {
            InputStream is = getContentResolver().openInputStream(sourceUri);
            if (is == null) {
                Toast.makeText(this, "Cannot read selected file", Toast.LENGTH_SHORT).show();
                return;
            }

            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry ze = zis.getNextEntry();

            boolean restored = false;
            while (ze != null) {
                if ("mumbaimedical.db".equals(ze.getName())) {
                    FileOutputStream fos = new FileOutputStream(tempDbFile);
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    restored = true;
                    break;
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
            is.close();

            if (restored && tempDbFile.exists() && tempDbFile.length() > 0) {
                File targetDbFile = getDatabasePath("mumbaimedical.db");
                
                // Copy temp file over actual database
                FileInputStream in = new FileInputStream(tempDbFile);
                FileOutputStream out = new FileOutputStream(targetDbFile);
                byte[] buf = new byte[2048];
                int bytesRead;
                while ((bytesRead = in.read(buf)) > 0) {
                    out.write(buf, 0, bytesRead);
                }
                in.close();
                out.close();

                tempDbFile.delete();

                new AlertDialog.Builder(this)
                        .setTitle(R.string.restore)
                        .setMessage(getString(R.string.restore_success))
                        .setPositiveButton(R.string.ok, null)
                        .show();
            } else {
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Restore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (tempDbFile.exists()) tempDbFile.delete();
        }
    }
}