package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreActivity extends Activity {

    private Button btnExportBackup, btnRestoreBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        btnExportBackup = findViewById(R.id.btnExportBackup);
        btnRestoreBackup = findViewById(R.id.btnRestoreBackup);

        btnExportBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportBackupZip();
            }
        });

        btnRestoreBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRestore();
            }
        });
    }

    private void exportBackupZip() {
        try {
            File dbFile = getDatabasePath("mumbaimedical.db");
            if (!dbFile.exists()) {
                Toast.makeText(this, "Database file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            File backupDir = getExternalFilesDir(null);
            if (backupDir == null) backupDir = getFilesDir();

            File zipFile = new File(backupDir, "MumbaiMedical_Backup.zip");

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            FileInputStream fis = new FileInputStream(dbFile);

            ZipEntry zipEntry = new ZipEntry("mumbaimedical.db");
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            fis.close();
            zos.close();
            fos.close();

            new AlertDialog.Builder(this)
                    .setTitle("Backup Successful")
                    .setMessage("ZIP Backup saved at:\n" + zipFile.getAbsolutePath())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Backup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRestore() {
        new AlertDialog.Builder(this)
                .setTitle("Restore Database")
                .setMessage("This will replace current local database with the latest backup ZIP. Continue?")
                .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restoreBackupZip();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreBackupZip() {
        try {
            File backupDir = getExternalFilesDir(null);
            if (backupDir == null) backupDir = getFilesDir();

            File zipFile = new File(backupDir, "MumbaiMedical_Backup.zip");
            if (!zipFile.exists()) {
                Toast.makeText(this, "No backup file found at: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }

            File dbFile = getDatabasePath("mumbaimedical.db");

            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();

            if (ze != null && "mumbaimedical.db".equals(ze.getName())) {
                FileOutputStream fos = new FileOutputStream(dbFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                Toast.makeText(this, "Database successfully restored!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Invalid backup ZIP archive format.", Toast.LENGTH_SHORT).show();
            }

            zis.closeEntry();
            zis.close();
            fis.close();

        } catch (Exception e) {
            Toast.makeText(this, "Restore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}