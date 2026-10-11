package com.productimagemanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    public static final int MAX_BYTES = 200 * 1024; // 200 KB limit

    public static byte[] compressAndResizeUri(Context context, Uri uri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        Bitmap original = BitmapFactory.decodeStream(is);
        if (is != null) {
            is.close();
        }
        if (original == null) {
            throw new Exception("Unable to decode selected image");
        }

        return compressAndResizeBitmap(original);
    }

    public static byte[] compressAndResizeBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int maxDim = 800;

        if (width > maxDim || height > maxDim) {
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxDim;
                height = (int) (maxDim / ratio);
            } else {
                height = maxDim;
                width = (int) (maxDim * ratio);
            }
        }

        Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);

        int quality = 85;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        while (baos.size() > MAX_BYTES && quality > 10) {
            baos.reset();
            quality -= 10;
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        // If still over 200 KB, scale down further
        if (baos.size() > MAX_BYTES) {
            scaled = Bitmap.createScaledBitmap(scaled, scaled.getWidth() / 2, scaled.getHeight() / 2, true);
            baos.reset();
            quality = 70;
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        return baos.toByteArray();
    }
}