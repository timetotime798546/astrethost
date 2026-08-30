package com.voicestudio.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Environment;
import java.io.File;
import java.io.FileNotFoundException;

public class AudioProvider extends ContentProvider {
    public static final String AUTHORITY = "com.voicestudio.app.provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "audio/wav";
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

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) {
            throw new FileNotFoundException("Context is unavailable");
        }
        File dir = getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        String path = uri.getPath();
        if (path == null) {
            throw new FileNotFoundException("Invalid URI path component");
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        File file = new File(dir, path);
        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        throw new FileNotFoundException("Target resource not found: " + file.getAbsolutePath());
    }
}