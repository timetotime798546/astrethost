package com.alfariskirana.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    private static final String BACKUP_META_FILE = "backup_meta.json";
    private static final String DB_FILE_NAME = "alfaris_kirana.db";
    private static final String EXPECTED_PACKAGE_NAME = "com.alfariskirana.app";

    public static class BackupResult {
        public boolean success;
        public String message;

        public BackupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static BackupResult createZipBackup(Context context, Uri outputUri, DatabaseHelper dbHelper) {
        if (context == null || outputUri == null) {
            return new BackupResult(false, "Invalid destination parameters.");
        }

        OutputStream os = null;
        ZipOutputStream zos = null;
        try {
            // Step 1: Force SQLite WAL checkpoint and close connection safely
            if (dbHelper != null) {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null);
                    if (c != null) c.close();
                } catch (Exception ignored) {}
                dbHelper.close();
            }

            File dbFile = context.getDatabasePath(DB_FILE_NAME);
            if (!dbFile.exists()) {
                return new BackupResult(false, "Database file not found on device.");
            }

            // Step 2: Open target ZIP output stream from SAF URI
            os = context.getContentResolver().openOutputStream(outputUri);
            if (os == null) {
                return new BackupResult(false, "Unable to open storage output stream.");
            }

            zos = new ZipOutputStream(new BufferedOutputStream(os));

            // Step 3: Write metadata JSON file into ZIP
            JSONObject metaJson = new JSONObject();
            metaJson.put("app_name", "Al Faris Kirana");
            metaJson.put("package_name", EXPECTED_PACKAGE_NAME);
            metaJson.put("app_id", "app_6b052812c42cd");
            metaJson.put("database_version", 4);
            metaJson.put("backup_version", 1);
            metaJson.put("timestamp", getCurrentDateTime());

            byte[] metaBytes = metaJson.toString(2).getBytes("UTF-8");
            ZipEntry metaEntry = new ZipEntry(BACKUP_META_FILE);
            metaEntry.setSize(metaBytes.length);
            zos.putNextEntry(metaEntry);
            zos.write(metaBytes);
            zos.closeEntry();

            // Step 4: Write database binary file into ZIP
            FileInputStream fis = new FileInputStream(dbFile);
            ZipEntry dbEntry = new ZipEntry(DB_FILE_NAME);
            dbEntry.setSize(dbFile.length());
            zos.putNextEntry(dbEntry);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            fis.close();
            zos.closeEntry();

            zos.flush();
            zos.close();
            os.close();

            return new BackupResult(true, "ZIP Backup created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            try { if (zos != null) zos.close(); } catch (Exception ignored) {}
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            return new BackupResult(false, "Backup failed: " + e.getLocalizedMessage());
        }
    }

    public static BackupResult restoreZipBackup(Context context, Uri inputUri, DatabaseHelper dbHelper) {
        if (context == null || inputUri == null) {
            return new BackupResult(false, "Invalid backup file selected.");
        }

        File tempDir = new File(context.getCacheDir(), "restore_temp_" + System.currentTimeMillis());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return new BackupResult(false, "Failed to prepare temporary restore space.");
        }

        InputStream is = null;
        ZipInputStream zis = null;
        File extractedDbFile = null;
        File extractedMetaFile = null;

        try {
            // Step 1: Read ZIP stream and extract components
            is = context.getContentResolver().openInputStream(inputUri);
            if (is == null) {
                cleanDir(tempDir);
                return new BackupResult(false, "Cannot open selected backup file.");
            }

            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File targetFile = new File(tempDir, entryName);

                // Security path traversal check
                if (!targetFile.getCanonicalPath().startsWith(tempDir.getCanonicalPath())) {
                    cleanDir(tempDir);
                    return new BackupResult(false, "Security validation failed: Invalid zip entry path.");
                }

                if (!entry.isDirectory()) {
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();

                    if (BACKUP_META_FILE.equalsIgnoreCase(entryName)) {
                        extractedMetaFile = targetFile;
                    } else if (DB_FILE_NAME.equalsIgnoreCase(entryName)) {
                        extractedDbFile = targetFile;
                    }
                }
                zis.closeEntry();
            }
            zis.close();
            is.close();

            // Step 2: Validate database extraction
            if (extractedDbFile == null || !extractedDbFile.exists() || extractedDbFile.length() == 0) {
                cleanDir(tempDir);
                return new BackupResult(false, "Invalid ZIP backup: Database file missing.");
            }

            // Step 3: Check app metadata compatibility
            if (extractedMetaFile != null && extractedMetaFile.exists()) {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(extractedMetaFile), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject metaObj = new JSONObject(sb.toString());
                String pkg = metaObj.optString("package_name", "");
                if (!pkg.isEmpty() && !EXPECTED_PACKAGE_NAME.equals(pkg)) {
                    cleanDir(tempDir);
                    return new BackupResult(false, "Incompatible backup file! Belongs to: " + pkg);
                }
            }

            // Step 4: Verify SQLite database structure
            SQLiteDatabase testDb = null;
            try {
                testDb = SQLiteDatabase.openDatabase(extractedDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = testDb.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('products', 'customers', 'sales', 'udhaar_transactions')", null);
                int tableCount = 0;
                if (cursor.moveToFirst()) {
                    tableCount = cursor.getInt(0);
                }
                cursor.close();
                testDb.close();

                if (tableCount < 2) {
                    cleanDir(tempDir);
                    return new BackupResult(false, "Corrupted database structure in backup file.");
                }
            } catch (Exception e) {
                if (testDb != null && testDb.isOpen()) testDb.close();
                cleanDir(tempDir);
                return new BackupResult(false, "Database verification error: " + e.getMessage());
            }

            // Step 5: Close existing active database instance
            if (dbHelper != null) {
                dbHelper.close();
            }

            File currentDbFile = context.getDatabasePath(DB_FILE_NAME);
            File backupSafetyFile = new File(currentDbFile.getParentFile(), DB_FILE_NAME + ".bak");

            // Step 6: Create automatic safety backup of current database
            if (currentDbFile.exists()) {
                copyFile(currentDbFile, backupSafetyFile);
            }

            // Step 7: Overwrite current database with restored database
            deleteDbAndAuxiliaryFiles(context, currentDbFile);
            boolean replaceSuccess = copyFile(extractedDbFile, currentDbFile);

            if (!replaceSuccess) {
                if (backupSafetyFile.exists()) {
                    copyFile(backupSafetyFile, currentDbFile);
                }
                cleanDir(tempDir);
                return new BackupResult(false, "Failed to replace database file during restore.");
            }

            // Step 8: Perform active database validation test
            try {
                SQLiteDatabase reTest = dbHelper.getWritableDatabase();
                Cursor c = reTest.rawQuery("SELECT count(*) FROM products", null);
                if (c != null) c.close();
            } catch (Exception e) {
                if (backupSafetyFile.exists()) {
                    copyFile(backupSafetyFile, currentDbFile);
                }
                cleanDir(tempDir);
                return new BackupResult(false, "Restored database failed runtime validation test. Reverted to previous state.");
            }

            // Cleanup temp safety files
            if (backupSafetyFile.exists()) {
                backupSafetyFile.delete();
            }
            cleanDir(tempDir);

            return new BackupResult(true, "Database successfully restored! All records, products, customers, and sales refreshed.");

        } catch (Exception e) {
            e.printStackTrace();
            cleanDir(tempDir);
            return new BackupResult(false, "Restore failed: " + e.getLocalizedMessage());
        }
    }

    private static void deleteDbAndAuxiliaryFiles(Context context, File dbFile) {
        if (dbFile.exists()) dbFile.delete();
        File wal = new File(dbFile.getPath() + "-wal");
        if (wal.exists()) wal.delete();
        File shm = new File(dbFile.getPath() + "-shm");
        if (shm.exists()) shm.delete();
        File journal = new File(dbFile.getPath() + "-journal");
        if (journal.exists()) journal.delete();
    }

    private static boolean copyFile(File src, File dst) {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(src);
            outStream = new FileOutputStream(dst);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
            outStream.flush();
            inStream.close();
            outStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (inStream != null) inStream.close(); } catch (Exception ignored) {}
            try { if (outStream != null) outStream.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    private static void cleanDir(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        cleanDir(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String exportAllDataToCsv(Context context, DatabaseHelper dbHelper) {
        try {
            File exportDir = context.getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = context.getFilesDir();
            }
            File file = new File(exportDir, "alfaris_kirana_backup.csv");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            osw.write("=== PRODUCTS ===\n");
            osw.write("ID,Name,Category,CostPrice,SellingPrice,Stock,Unit\n");
            List<Product> products = dbHelper.getAllProducts("");
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\"\n",
                        p.getId(), p.getName(), p.getCategory(), p.getPurchasePrice(), p.getSellingPrice(), p.getStock(), p.getUnit()));
            }

            osw.write("\n=== CUSTOMERS & KHATA ===\n");
            osw.write("ID,Name,Mobile,PendingBalance\n");
            List<Customer> customers = dbHelper.getAllCustomers("");
            for (int i = 0; i < customers.size(); i++) {
                Customer c = customers.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",\"%s\",%.2f\n",
                        c.getId(), c.getName(), c.getMobile(), c.getPendingBalance()));
            }

            osw.write("\n=== EXPENSES ===\n");
            osw.write("ID,Category,Amount,Date,Notes\n");
            List<Expense> expenses = dbHelper.getAllExpenses();
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",%.2f,\"%s\",\"%s\"\n",
                        e.getId(), e.getCategory(), e.getAmount(), e.getExpenseDate(), e.getNotes()));
            }

            osw.flush();
            osw.close();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}