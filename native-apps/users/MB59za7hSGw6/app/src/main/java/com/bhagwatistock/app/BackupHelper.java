package com.bhagwatistock.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    public static boolean createBackup(Context context, OutputStream outputStream) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(outputStream));

            // 1. Include SQLite Database files
            File dbFile = context.getDatabasePath("ocr_scans.db");
            if (dbFile.exists()) {
                addFileToZip(dbFile, "databases/" + dbFile.getName(), zos);
            }
            File dbJournal = new File(dbFile.getPath() + "-journal");
            if (dbJournal.exists()) {
                addFileToZip(dbJournal, "databases/" + dbJournal.getName(), zos);
            }
            File dbWal = new File(dbFile.getPath() + "-wal");
            if (dbWal.exists()) {
                addFileToZip(dbWal, "databases/" + dbWal.getName(), zos);
            }
            File dbShm = new File(dbFile.getPath() + "-shm");
            if (dbShm.exists()) {
                addFileToZip(dbShm, "databases/" + dbShm.getName(), zos);
            }

            // 2. Include Shared Preferences
            File sharedPrefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
                addFolderToZip(sharedPrefsDir, "shared_prefs", zos);
            }

            // 3. Include Internal App Files
            File filesDir = context.getFilesDir();
            if (filesDir.exists() && filesDir.isDirectory()) {
                addFolderToZip(filesDir, "files", zos);
            }

            // 4. Include External Files directory
            File extFilesDir = context.getExternalFilesDir(null);
            if (extFilesDir != null && extFilesDir.exists() && extFilesDir.isDirectory()) {
                addFolderToZip(extFilesDir, "external_files", zos);
            }

            zos.flush();
            zos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (zos != null) {
                try {
                    zos.close();
                } catch (Exception ignored) {}
            }
            return false;
        }
    }

    private static void addFolderToZip(File folder, String parentPath, ZipOutputStream zos) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                addFolderToZip(file, parentPath + "/" + file.getName(), zos);
            } else {
                addFileToZip(file, parentPath + "/" + file.getName(), zos);
            }
        }
    }

    private static void addFileToZip(File file, String zipPath, ZipOutputStream zos) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ZipEntry entry = new ZipEntry(zipPath);
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean restoreBackup(Context context, InputStream inputStream) {
        File tempDir = new File(context.getCacheDir(), "restore_temp_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) {
            deleteDir(tempDir);
            tempDir.mkdirs();
        }

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry entry;
            String canonicalTempPath = tempDir.getCanonicalPath();

            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File targetFile = new File(tempDir, entry.getName());
                String canonicalTarget = targetFile.getCanonicalPath();

                // Zip Slip Path Traversal Protection
                if (!canonicalTarget.startsWith(canonicalTempPath + File.separator) && !canonicalTarget.equals(canonicalTempPath)) {
                    throw new SecurityException("ZIP entry attempts path traversal outside target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zis.closeEntry();
            }
            zis.close();

            // Validate database integrity before applying overwrite
            File tempDbFile = new File(tempDir, "databases/ocr_scans.db");
            if (!tempDbFile.exists()) {
                deleteDir(tempDir);
                return false;
            }

            SQLiteDatabase testDb = null;
            try {
                testDb = SQLiteDatabase.openDatabase(tempDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                int version = testDb.getVersion();
                if (version < 1) {
                    testDb.close();
                    deleteDir(tempDir);
                    return false;
                }
                testDb.close();
            } catch (Exception e) {
                if (testDb != null) {
                    try {
                        testDb.close();
                    } catch (Exception ignored) {}
                }
                deleteDir(tempDir);
                return false;
            }

            // Safe database overwrite
            File targetDbFile = context.getDatabasePath("ocr_scans.db");
            File targetDbDir = targetDbFile.getParentFile();
            if (targetDbDir != null && !targetDbDir.exists()) {
                targetDbDir.mkdirs();
            }

            copyFile(tempDbFile, targetDbFile);

            File tempJournal = new File(tempDir, "databases/ocr_scans.db-journal");
            File targetJournal = new File(targetDbFile.getPath() + "-journal");
            if (tempJournal.exists()) {
                copyFile(tempJournal, targetJournal);
            } else if (targetJournal.exists()) {
                targetJournal.delete();
            }

            File tempWal = new File(tempDir, "databases/ocr_scans.db-wal");
            File targetWal = new File(targetDbFile.getPath() + "-wal");
            if (tempWal.exists()) {
                copyFile(tempWal, targetWal);
            } else if (targetWal.exists()) {
                targetWal.delete();
            }

            File tempShm = new File(tempDir, "databases/ocr_scans.db-shm");
            File targetShm = new File(targetDbFile.getPath() + "-shm");
            if (tempShm.exists()) {
                copyFile(tempShm, targetShm);
            } else if (targetShm.exists()) {
                targetShm.delete();
            }

            // Restore Shared Preferences
            File tempPrefs = new File(tempDir, "shared_prefs");
            if (tempPrefs.exists() && tempPrefs.isDirectory()) {
                File targetPrefs = new File(context.getApplicationInfo().dataDir, "shared_prefs");
                copyDirectoryContents(tempPrefs, targetPrefs);
            }

            // Restore Internal Files
            File tempFiles = new File(tempDir, "files");
            if (tempFiles.exists() && tempFiles.isDirectory()) {
                copyDirectoryContents(tempFiles, context.getFilesDir());
            }

            // Restore External Files
            File tempExtFiles = new File(tempDir, "external_files");
            File extFilesDir = context.getExternalFilesDir(null);
            if (tempExtFiles.exists() && tempExtFiles.isDirectory() && extFilesDir != null) {
                copyDirectoryContents(tempExtFiles, extFilesDir);
            }

            deleteDir(tempDir);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            deleteDir(tempDir);
            return false;
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (Exception ignored) {}
            }
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void copyDirectoryContents(File srcDir, File dstDir) throws Exception {
        if (!dstDir.exists()) {
            dstDir.mkdirs();
        }
        File[] files = srcDir.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File destFile = new File(dstDir, file.getName());
            if (file.isDirectory()) {
                copyDirectoryContents(file, destFile);
            } else {
                copyFile(file, destFile);
            }
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }
}