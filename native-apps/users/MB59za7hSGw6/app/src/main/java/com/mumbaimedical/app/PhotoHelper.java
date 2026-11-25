package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoHelper {

    public static final int REQUEST_CAMERA_PERMISSION = 3001;

    public interface PhotoPickCallback {
        void onOptionSelected(int option); // 1: Camera, 2: Gallery, 3: Remove
    }

    public static File getPhotoDirectory(Context context, String subfolder) {
        File photosDir = new File(context.getFilesDir(), "photos" + File.separator + subfolder);
        if (!photosDir.exists()) {
            photosDir.mkdirs();
        }
        return photosDir;
    }

    public static void showPhotoOptionsDialog(final Activity activity, final boolean hasExistingPhoto, final PhotoPickCallback callback) {
        String[] options;
        if (hasExistingPhoto) {
            options = new String[]{
                    activity.getString(R.string.btn_take_photo),
                    activity.getString(R.string.btn_choose_gallery),
                    activity.getString(R.string.btn_remove_photo)
            };
        } else {
            options = new String[]{
                    activity.getString(R.string.btn_take_photo),
                    activity.getString(R.string.btn_choose_gallery)
            };
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.select_photo_option);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    checkCameraAndPick(activity, callback);
                } else if (which == 1) {
                    callback.onOptionSelected(2);
                } else if (which == 2 && hasExistingPhoto) {
                    callback.onOptionSelected(3);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private static void checkCameraAndPick(Activity activity, PhotoPickCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
        }
        callback.onOptionSelected(1);
    }

    public static String saveImageFromUri(Context context, Uri uri, String subfolder) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();

            if (bitmap == null) return null;

            bitmap = resizeBitmap(bitmap, 1280);

            File folder = getPhotoDirectory(context, subfolder);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = subfolder + "_" + timeStamp + "_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(folder, fileName);

            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            out.flush();
            out.close();

            return "photos/" + subfolder + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveBitmapImage(Context context, Bitmap bitmap, String subfolder) {
        try {
            if (bitmap == null) return null;
            bitmap = resizeBitmap(bitmap, 1280);

            File folder = getPhotoDirectory(context, subfolder);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = subfolder + "_" + timeStamp + "_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(folder, fileName);

            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            out.flush();
            out.close();

            return "photos/" + subfolder + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap resizeBitmap(Bitmap original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newWidth = Math.round(ratio * width);
        int newHeight = Math.round(ratio * height);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    public static void deletePhotoFile(Context context, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return;
        try {
            File file = new File(context.getFilesDir(), relativePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {}
    }

    public static boolean displayThumbnail(Context context, ImageView imageView, String relativePath, int reqWidth, int reqHeight) {
        if (relativePath == null || relativePath.isEmpty()) {
            imageView.setVisibility(View.GONE);
            return false;
        }

        File file = new File(context.getFilesDir(), relativePath);
        if (!file.exists()) {
            imageView.setVisibility(View.GONE);
            return false;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
                return true;
            }
        } catch (Exception ignored) {}

        imageView.setVisibility(View.GONE);
        return false;
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

    public static void showFullScreenImage(final Context context, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return;
        File file = new File(context.getFilesDir(), relativePath);
        if (!file.exists()) {
            Toast.makeText(context, "Photo file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFull = dialog.findViewById(R.id.ivFullScreenPhoto);
        View btnClose = dialog.findViewById(R.id.btnCloseFullImage);

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                ivFull.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error loading image preview", Toast.LENGTH_SHORT).show();
            return;
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void disableStrictModeVmPolicy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Exception ignored) {}
        }
    }
}