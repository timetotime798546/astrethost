package com.kkgstore.store;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ImageUtils {

    public static File getPhotosDir(Context context) {
        File dir = new File(context.getFilesDir(), "udhar_photos");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String saveCompressedImageFromUri(Context context, Uri imageUri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            if (input != null) input.close();

            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;

            if (srcWidth <= 0 || srcHeight <= 0) return null;

            int maxDim = 1280;
            int inSampleSize = 1;

            if (srcWidth > maxDim || srcHeight > maxDim) {
                int halfWidth = srcWidth / 2;
                int halfHeight = srcHeight / 2;
                while ((halfWidth / inSampleSize) >= maxDim || (halfHeight / inSampleSize) >= maxDim) {
                    inSampleSize *= 2;
                }
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;

            input = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, decodeOptions);
            if (input != null) input.close();

            if (bitmap == null) return null;

            int rotation = getExifRotation(context, imageUri);
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w > maxDim || h > maxDim) {
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                int newW = Math.round(w * scale);
                int newH = Math.round(h * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
            }

            File photosDir = getPhotosDir(context);
            String fileName = "udhar_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ".jpg";
            File destFile = new File(photosDir, fileName);

            FileOutputStream out = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            out.flush();
            out.close();

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveCompressedBitmap(Context context, Bitmap srcBitmap) {
        if (srcBitmap == null) return null;
        try {
            int maxDim = 1280;
            Bitmap bitmap = srcBitmap;
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            if (w > maxDim || h > maxDim) {
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                int newW = Math.round(w * scale);
                int newH = Math.round(h * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
            }

            File photosDir = getPhotosDir(context);
            String fileName = "udhar_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ".jpg";
            File destFile = new File(photosDir, fileName);

            FileOutputStream out = new FileOutputStream(destFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            out.flush();
            out.close();

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getExifRotation(Context context, Uri uri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            in.close();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {}
        return 0;
    }

    public static File getPhotoFile(Context context, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return null;
        File file = new File(getPhotosDir(context), fileName);
        if (file.exists()) return file;
        File absFile = new File(fileName);
        if (absFile.exists()) return absFile;
        return null;
    }

    public static Bitmap loadThumbnail(Context context, String fileName, int reqWidth, int reqHeight) {
        File file = getPhotoFile(context, fileName);
        if (file == null || !file.exists()) return null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}