package com.aabinstaller.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class ApkFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.aabinstaller.app.fileprovider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) {
            throw new FileNotFoundException("Context is null");
        }
        String path = uri.getLastPathSegment();
        if (path == null) {
            throw new FileNotFoundException("Invalid URI path");
        }

        File file = new File(getContext().getCacheDir(), path);
        if (!file.exists()) {
            file = new File(getContext().getFilesDir(), path);
        }
        if (!file.exists()) {
            File exportDir = new File(getContext().getFilesDir(), "converted_apks");
            file = new File(exportDir, path);
        }

        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }

        throw new FileNotFoundException("File not found: " + path);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] cols = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(cols);
        if (getContext() == null) {
            return cursor;
        }

        String path = uri.getLastPathSegment();
        if (path != null) {
            File file = new File(getContext().getCacheDir(), path);
            if (!file.exists()) {
                file = new File(getContext().getFilesDir(), path);
            }
            if (!file.exists()) {
                File exportDir = new File(getContext().getFilesDir(), "converted_apks");
                file = new File(exportDir, path);
            }

            if (file.exists()) {
                Object[] values = new Object[cols.length];
                for (int i = 0; i < cols.length; i++) {
                    if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                        values[i] = file.getName();
                    } else if (OpenableColumns.SIZE.equals(cols[i])) {
                        values[i] = file.length();
                    } else {
                        values[i] = null;
                    }
                }
                cursor.addRow(values);
            }
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}