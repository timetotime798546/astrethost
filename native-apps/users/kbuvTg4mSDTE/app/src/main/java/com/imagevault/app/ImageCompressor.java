package com.imagevault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageCompressor {
    private static final int MAX_SIZE_BYTES = 200 * 1024; // 200 KB
    private static final int MAX_DIMENSION = 1280;

    public static byte[] compressUriToUnder200Kb(Context context, Uri uri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);
        if (is != null) is.close();

        int width = opts.outWidth;
        int height = opts.outHeight;
        int sampleSize = 1;

        while ((width / sampleSize) > MAX_DIMENSION || (height / sampleSize) > MAX_DIMENSION) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sampleSize;

        is = context.getContentResolver().openInputStream(uri);
        Bitmap scaledBmp = BitmapFactory.decodeStream(is, null, decodeOpts);
        if (is != null) is.close();

        if (scaledBmp == null) {
            throw new Exception("Unable to decode selected image");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 85;
        scaledBmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        while (baos.size() > MAX_SIZE_BYTES && quality > 15) {
            baos.reset();
            quality -= 10;
            scaledBmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        if (baos.size() > MAX_SIZE_BYTES) {
            int targetW = scaledBmp.getWidth() / 2;
            int targetH = scaledBmp.getHeight() / 2;
            if (targetW > 100 && targetH > 100) {
                Bitmap resized = Bitmap.createScaledBitmap(scaledBmp, targetW, targetH, true);
                baos.reset();
                quality = 75;
                resized.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                while (baos.size() > MAX_SIZE_BYTES && quality > 20) {
                    baos.reset();
                    quality -= 15;
                    resized.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                }
                resized.recycle();
            }
        }

        scaledBmp.recycle();
        return baos.toByteArray();
    }
}