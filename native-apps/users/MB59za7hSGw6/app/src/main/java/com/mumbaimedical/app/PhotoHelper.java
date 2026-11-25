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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoHelper {

    public static final int REQUEST_CAMERA_PERMISSION = 3001;
    public static Uri lastCameraUri = null;

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

    public static Intent getCameraCaptureIntent(Context context, String subfolder) {
        disableStrictModeVmPolicy();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File tempFolder = getPhotoDirectory(context, "temp");
        File tempFile = new File(tempFolder, "temp_camera_" + System.currentTimeMillis() + ".jpg");
        lastCameraUri = Uri.fromFile(tempFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, lastCameraUri);
        return intent;
    }

    public static String getCameraPhotoPath(Context context, String subfolder) {
        if (lastCameraUri == null) return null;
        File tempFile = new File(lastCameraUri.getPath());
        if (!tempFile.exists() || tempFile.length() == 0) return null;

        File folder = getPhotoDirectory(context, subfolder);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = subfolder + "_" + timeStamp + "_" + System.currentTimeMillis() + ".jpg";
        File outputFile = new File(folder, fileName);

        boolean copied = copyFile(tempFile, outputFile);
        tempFile.delete();
        lastCameraUri = null;

        if (copied) {
            return "photos/" + subfolder + "/" + fileName;
        }
        return null;
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
        if (uri == null) return null;
        try {
            File folder = getPhotoDirectory(context, subfolder);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

            String extension = ".jpg";
            String type = context.getContentResolver().getType(uri);
            if (type != null && type.contains("png")) {
                extension = ".png";
            }

            String fileName = subfolder + "_" + timeStamp + "_" + System.currentTimeMillis() + extension;
            File outputFile = new File(folder, fileName);

            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return null;

            OutputStream output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
            output.close();
            input.close();

            return "photos/" + subfolder + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveBitmapImage(Context context, Bitmap bitmap, String subfolder) {
        try {
            if (bitmap == null) return null;

            File folder = getPhotoDirectory(context, subfolder);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = subfolder + "_" + timeStamp + "_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(folder, fileName);

            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out);
            out.flush();
            out.close();

            return "photos/" + subfolder + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean copyFile(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Bitmap rotateBitmapIfRequired(String filePath, Bitmap bitmap) {
        if (bitmap == null || filePath == null) return bitmap;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;
                default:
                    return bitmap;
            }
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Exception e) {
            return bitmap;
        }
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
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                bitmap = rotateBitmapIfRequired(file.getAbsolutePath(), bitmap);
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

        final ImageView ivFull = dialog.findViewById(R.id.ivFullScreenPhoto);
        View btnClose = dialog.findViewById(R.id.btnCloseFullImage);

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int maxDim = 2048;
            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                bitmap = rotateBitmapIfRequired(file.getAbsolutePath(), bitmap);
                ivFull.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error loading image preview", Toast.LENGTH_SHORT).show();
            return;
        }

        ivFull.setOnTouchListener(new View.OnTouchListener() {
            private Matrix matrix = new Matrix();
            private Matrix savedMatrix = new Matrix();
            private int mode = 0;
            private PointF start = new PointF();
            private PointF mid = new PointF();
            private float oldDist = 1f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView) v;
                if (view.getScaleType() != ImageView.ScaleType.MATRIX) {
                    view.setScaleType(ImageView.ScaleType.MATRIX);
                    matrix.set(view.getImageMatrix());
                }

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = 1; // DRAG
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = 2; // ZOOM
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == 1) {
                            matrix.set(savedMatrix);
                            matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                        } else if (mode == 2) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                matrix.set(savedMatrix);
                                float scale = newDist / oldDist;
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }
                        break;
                }
                view.setImageMatrix(matrix);
                return true;
            }

            private float spacing(MotionEvent event) {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            }

            private void midPoint(PointF point, MotionEvent event) {
                float x = event.getX(0) + event.getX(1);
                float y = event.getY(0) + event.getY(1);
                point.set(x / 2, y / 2);
            }
        });

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