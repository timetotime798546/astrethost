package com.productimagemanager.app;

import android.content.Context;

/**
 * Offline SQLite storage has been removed.
 * All application persistence is performed online via the generic /data API endpoint.
 */
public class ProductDbHelper {

    public ProductDbHelper(Context context) {
        // No-op: Local offline database storage has been disabled.
    }
}