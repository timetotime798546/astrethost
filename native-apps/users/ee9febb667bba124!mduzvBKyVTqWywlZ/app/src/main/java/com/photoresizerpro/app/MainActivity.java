package com.photoresizerpro.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_PICK_IMAGE = 1001;

    // Original Image State Data
    private Uri selectedImageUri = null;
    private Bitmap originalBitmap = null;
    private int originalWidth = 0;
    private int originalHeight = 0;
    private long originalSizeBytes = 0;

    // Resized Image State Data
    private Bitmap resizedBitmap = null;
    private byte[] compressedBytes = null;
    private int targetWidth = 0;
    private int targetHeight = 0;
    private String selectedFormatExtension = "jpg";

    // Layout References
    private ImageView ivOriginalPreview;
    private TextView tvOriginalStats;
    private Button btnPickImage;

    private LinearLayout layoutSettings;
    private EditText etWidth;
    private EditText etHeight;
    private CheckBox cbLockRatio;

    private Button btnScale25;
    private Button btnScale50;
    private Button btnScale75;
    private Button btnScaleOriginal;

    private RadioGroup rgFormat;
    private RadioButton rbJpeg;
    private RadioButton rbPng;
    private RadioButton rbWebp;

    private LinearLayout layoutQuality;
    private SeekBar sbQuality;
    private TextView tvQualityValue;

    private Button btnResize;

    private LinearLayout layoutResizedResult;
    private ImageView ivResizedPreview;
    private TextView tvResizedStats;
    private Button btnSave;
    private Button btnShare;

    // Flags to prevent recursive TextWatcher calculations
    private boolean isUpdatingDimensions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        ivOriginalPreview = (ImageView) findViewById(R.id.ivOriginalPreview);
        tvOriginalStats = (TextView) findViewById(R.id.tvOriginalStats);
        btnPickImage = (Button) findViewById(R.id.btnPickImage);

        layoutSettings = (LinearLayout) findViewById(R.id.layoutSettings);
        etWidth = (EditText) findViewById(R.id.etWidth);
        etHeight = (EditText) findViewById(R.id.etHeight);
        cbLockRatio = (CheckBox) findViewById(R.id.cbLockRatio);

        btnScale25 = (Button) findViewById(R.id.btnScale25);
        btnScale50 = (Button) findViewById(R.id.btnScale50);
        btnScale75 = (Button) findViewById(R.id.btnScale75);
        btnScaleOriginal = (Button) findViewById(R.id.btnScaleOriginal);

        rgFormat = (RadioGroup) findViewById(R.id.rgFormat);
        rbJpeg = (RadioButton) findViewById(R.id.rbJpeg);
        rbPng = (RadioButton) findViewById(R.id.rbPng);
        rbWebp = (RadioButton) findViewById(R.id.rbWebp);

        layoutQuality = (LinearLayout) findViewById(R.id.layoutQuality);
        sbQuality = (SeekBar) findViewById(R.id.sbQuality);
        tvQualityValue = (TextView) findViewById(R.id.tvQualityValue);

        btnResize = (Button) findViewById(R.id.btnResize);

        layoutResizedResult = (LinearLayout) findViewById(R.id.layoutResizedResult);
        ivResizedPreview = (ImageView) findViewById(R.id.ivResizedPreview);
        tvResizedStats = (TextView) findViewById(R.id.tvResizedStats);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnShare = (Button) findViewById(R.id.btnShare);
    }

    private void setupListeners() {
        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            }
        });

        // Watch width modifications to auto-calculate proportional height
        etWidth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingDimensions || !cbLockRatio.isChecked() || originalWidth == 0 || originalHeight == 0) {
                    return;
                }
                String val = s.toString().trim();
                if (val.isEmpty()) return;

                try {
                    int newWidth = Integer.parseInt(val);
                    if (newWidth > 0) {
                        isUpdatingDimensions = true;
                        int newHeight = (int) (((double) originalHeight / (double) originalWidth) * newWidth);
                        etHeight.setText(String.valueOf(newHeight));
                        isUpdatingDimensions = false;
                    }
                } catch (NumberFormatException e) {
                    isUpdatingDimensions = false;
                }
            }
        });

        // Watch height modifications to auto-calculate proportional width
        etHeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingDimensions || !cbLockRatio.isChecked() || originalWidth == 0 || originalHeight == 0) {
                    return;
                }
                String val = s.toString().trim();
                if (val.isEmpty()) return;

                try {
                    int newHeight = Integer.parseInt(val);
                    if (newHeight > 0) {
                        isUpdatingDimensions = true;
                        int newWidth = (int) (((double) originalWidth / (double) originalHeight) * newHeight);
                        etWidth.setText(String.valueOf(newWidth));
                        isUpdatingDimensions = false;
                    }
                } catch (NumberFormatException e) {
                    isUpdatingDimensions = false;
                }
            }
        });

        // Quality SeekBar listener
        sbQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvQualityValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Format selector changes
        rgFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbPng) {
                    // PNG does not support lossy compression settings
                    layoutQuality.setVisibility(View.GONE);
                } else {
                    layoutQuality.setVisibility(View.VISIBLE);
                }
            }
        });

        // Dimension presets
        btnScale25.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercentScale(0.25);
            }
        });

        btnScale50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercentScale(0.50);
            }
        });

        btnScale75.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercentScale(0.75);
            }
        });

        btnScaleOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercentScale(1.0);
            }
        });

        // Action Buttons
        btnResize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performResizeAction();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResizedImageToGallery();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResizedImage();
            }
        });
    }

    private void applyPercentScale(double fraction) {
        if (originalWidth == 0 || originalHeight == 0) return;
        isUpdatingDimensions = true;
        int targetW = (int) (originalWidth * fraction);
        int targetH = (int) (originalHeight * fraction);
        etWidth.setText(String.valueOf(targetW));
        etHeight.setText(String.valueOf(targetH));
        isUpdatingDimensions = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            loadOriginalImage();
        }
    }

    private void loadOriginalImage() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Reading source image...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get file size
                    originalSizeBytes = getFileSize(selectedImageUri);

                    // Decode bounds first to check memory safely
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                    if (pfd == null) {
                        throw new IOException("Failed to load FileDescriptor");
                    }
                    FileDescriptor fd = pfd.getFileDescriptor();

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(fd, null, options);

                    originalWidth = options.outWidth;
                    originalHeight = options.outHeight;

                    // Compute sample scale factor to prevent heap space exhaustion during initial view load
                    int scaleFactor = 1;
                    if (originalWidth > 2048 || originalHeight > 2048) {
                        int halfWidth = originalWidth / 2;
                        int halfHeight = originalHeight / 2;
                        while ((halfWidth / scaleFactor) >= 2048 && (halfHeight / scaleFactor) >= 2048) {
                            scaleFactor *= 2;
                        }
                    }

                    BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                    decodeOptions.inSampleSize = scaleFactor;
                    decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    final Bitmap loaded = BitmapFactory.decodeFileDescriptor(fd, null, decodeOptions);
                    pfd.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if (loaded != null) {
                                originalBitmap = loaded;
                                ivOriginalPreview.setImageBitmap(originalBitmap);

                                String sizeStr = formatFileSize(originalSizeBytes);
                                tvOriginalStats.setText("Dimensions: " + originalWidth + " x " + originalHeight + " px\nFile Size: " + sizeStr);

                                // Update resize fields
                                isUpdatingDimensions = true;
                                etWidth.setText(String.valueOf(originalWidth));
                                etHeight.setText(String.valueOf(originalHeight));
                                isUpdatingDimensions = false;

                                // Show layouts
                                layoutSettings.setVisibility(View.VISIBLE);
                                layoutResizedResult.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(MainActivity.this, "Could not read image file structure", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private long getFileSize(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        // Fallback size estimation via stream
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                long size = pfd.getStatSize();
                pfd.close();
                return size;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void performResizeAction() {
        if (originalBitmap == null || selectedImageUri == null) {
            Toast.makeText(this, "Select a source image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String wStr = etWidth.getText().toString().trim();
        String hStr = etHeight.getText().toString().trim();

        if (wStr.isEmpty() || hStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid dimensions", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            targetWidth = Integer.parseInt(wStr);
            targetHeight = Integer.parseInt(hStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dimensions must be numeric integers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth <= 0 || targetHeight <= 0) {
            Toast.makeText(this, "Dimensions must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth > 10000 || targetHeight > 10000) {
            Toast.makeText(this, "Dimensions exceed maximum limit of 10000px", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Processing Resizing Operations...");
        dialog.setCancelable(false);
        dialog.show();

        // Retrieve compression configurations
        final Bitmap.CompressFormat format;
        final int checkedRadio = rgFormat.getCheckedRadioButtonId();
        if (checkedRadio == R.id.rbPng) {
            format = Bitmap.CompressFormat.PNG;
            selectedFormatExtension = "png";
        } else if (checkedRadio == R.id.rbWebp) {
            // WEBP format handling compatible with old & new APIs
            format = Bitmap.CompressFormat.WEBP;
            selectedFormatExtension = "webp";
        } else {
            format = Bitmap.CompressFormat.JPEG;
            selectedFormatExtension = "jpg";
        }

        final int quality = sbQuality.getProgress();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Reload absolute full original bitmap without sub-sampling to get highest quality resize output
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                    if (pfd == null) throw new IOException("Failed to open source descriptor");
                    Bitmap fullOriginal = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                    pfd.close();

                    if (fullOriginal == null) {
                        throw new Exception("Unable to decode source file");
                    }

                    // Perform precise matrix-based rescaling
                    Bitmap scaled = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(scaled);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    paint.setDither(true);

                    float scaleX = (float) targetWidth / fullOriginal.getWidth();
                    float scaleY = (float) targetHeight / fullOriginal.getHeight();

                    Matrix matrix = new Matrix();
                    matrix.postScale(scaleX, scaleY);
                    canvas.drawBitmap(fullOriginal, matrix, paint);

                    // Reclaim memory instantly
                    if (fullOriginal != originalBitmap) {
                        fullOriginal.recycle();
                    }

                    // Compress to target output quality
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    scaled.compress(format, quality, bos);
                    compressedBytes = bos.toByteArray();
                    bos.close();

                    // Create local reference for preview display
                    resizedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if (resizedBitmap != null) {
                                ivResizedPreview.setImageBitmap(resizedBitmap);
                                String stats = "Output Dimensions: " + resizedBitmap.getWidth() + " x " + resizedBitmap.getHeight() + " px\n" +
                                        "Output File Size: " + formatFileSize(compressedBytes.length);
                                tvResizedStats.setText(stats);
                                layoutResizedResult.setVisibility(View.VISIBLE);

                                // Scroll layout down smoothly to show preview result
                                ivResizedPreview.getParent().requestChildFocus(ivResizedPreview, ivResizedPreview);
                            } else {
                                Toast.makeText(MainActivity.this, "Compression process aborted", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Resizing Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void saveResizedImageToGallery() {
        if (compressedBytes == null || resizedBitmap == null) {
            Toast.makeText(this, "Run processing operations first", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Saving image to Gallery...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String filename = "Resized_" + timestamp + "." + selectedFormatExtension;

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + (selectedFormatExtension.equals("jpg") ? "jpeg" : selectedFormatExtension));
                    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

                    // Insert to external directory via media provider
                    final Uri imageCollectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    final Uri itemUri = getContentResolver().insert(imageCollectionUri, values);

                    if (itemUri == null) {
                        throw new IOException("Failed to create MediaStore catalog record");
                    }

                    OutputStream os = getContentResolver().openOutputStream(itemUri);
                    if (os == null) {
                        throw new IOException("Failed to access MediaStore stream");
                    }
                    os.write(compressedBytes);
                    os.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Success! Resized photo saved directly to Gallery.", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Could not save photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void shareResizedImage() {
        if (compressedBytes == null) {
            Toast.makeText(this, "Please process the photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }

            File tempFile = new File(cachePath, "shared_resized_image." + selectedFormatExtension);
            FileOutputStream stream = new FileOutputStream(tempFile);
            stream.write(compressedBytes);
            stream.close();

            // Use standard direct sharing using content cache framework
            Uri shareableUri = Uri.fromFile(tempFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/" + (selectedFormatExtension.equals("jpg") ? "jpeg" : selectedFormatExtension));
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareableUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Resized Image via"));

        } catch (Exception e) {
            Toast.makeText(this, "Sharing Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}