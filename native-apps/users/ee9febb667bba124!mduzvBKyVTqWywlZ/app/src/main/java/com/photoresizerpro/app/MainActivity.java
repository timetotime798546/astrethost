package com.photoresizerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_CODE = 2002;

    // Original Image Views
    private ImageView originalImageView;
    private TextView placeholderText;
    private TextView originalInfoText;
    private Button btnSelectPhoto;

    // Options UI Elements
    private LinearLayout resizeOptionsContainer;
    private EditText widthInput;
    private EditText heightInput;
    private CheckBox maintainAspect;
    private Button preset25, preset50, preset75, preset100;
    private Button presetPassport, presetSignature;
    private RadioGroup formatRadioGroup;
    private RadioButton radioJpg, radioPng, radioWebp;
    private LinearLayout qualityLayout;
    private SeekBar qualitySeekBar;
    private TextView qualityValText;

    // Resized Preview UI Elements
    private ImageView resizedImageView;
    private TextView resizedPlaceholderText;
    private TextView resizedInfoText;

    // Bottom Action Bar Buttons
    private Button btnResize;
    private Button btnSave;
    private Button btnShare;

    // Full screen progress overlay
    private LinearLayout progressOverlay;
    private TextView progressMessageText;

    // State Variables
    private Uri selectedImageUri = null;
    private int originalWidth = 0;
    private int originalHeight = 0;
    private boolean isAutoUpdating = false;

    // Final Resized bitmap state
    private Bitmap currentResizedBitmap = null;
    private byte[] finalCompressedData = null;
    private String finalSelectedFormat = "JPEG";
    private Uri savedUri = null;

    // Built-in audio tone player
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize sound generator
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Bind layout views
        originalImageView = (ImageView) findViewById(R.id.originalImageView);
        placeholderText = (TextView) findViewById(R.id.placeholderText);
        originalInfoText = (TextView) findViewById(R.id.originalInfoText);
        btnSelectPhoto = (Button) findViewById(R.id.btnSelectPhoto);

        resizeOptionsContainer = (LinearLayout) findViewById(R.id.resizeOptionsContainer);
        widthInput = (EditText) findViewById(R.id.widthInput);
        heightInput = (EditText) findViewById(R.id.heightInput);
        maintainAspect = (CheckBox) findViewById(R.id.maintainAspect);

        preset25 = (Button) findViewById(R.id.preset25);
        preset50 = (Button) findViewById(R.id.preset50);
        preset75 = (Button) findViewById(R.id.preset75);
        preset100 = (Button) findViewById(R.id.preset100);
        presetPassport = (Button) findViewById(R.id.presetPassport);
        presetSignature = (Button) findViewById(R.id.presetSignature);

        formatRadioGroup = (RadioGroup) findViewById(R.id.formatRadioGroup);
        radioJpg = (RadioButton) findViewById(R.id.radioJpg);
        radioPng = (RadioButton) findViewById(R.id.radioPng);
        radioWebp = (RadioButton) findViewById(R.id.radioWebp);

        qualityLayout = (LinearLayout) findViewById(R.id.qualityLayout);
        qualitySeekBar = (SeekBar) findViewById(R.id.qualitySeekBar);
        qualityValText = (TextView) findViewById(R.id.qualityValText);

        resizedImageView = (ImageView) findViewById(R.id.resizedImageView);
        resizedPlaceholderText = (TextView) findViewById(R.id.resizedPlaceholderText);
        resizedInfoText = (TextView) findViewById(R.id.resizedInfoText);

        btnResize = (Button) findViewById(R.id.btnResize);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnShare = (Button) findViewById(R.id.btnShare);

        progressOverlay = (LinearLayout) findViewById(R.id.progressOverlay);
        progressMessageText = (TextView) findViewById(R.id.progressMessageText);

        // Requirements Trigger: Show custom Loading overlay on start
        showLoadingOverlay("Initializing Photo Resizer Pro...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideLoadingOverlay();
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
            }
        }, 1600);

        // Select Photo Event
        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP2);
                openImagePicker();
            }
        });

        // Width and Height live sync based on Aspect Ratio Checkbox
        widthInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isAutoUpdating || !maintainAspect.isChecked() || originalWidth == 0 || originalHeight == 0) {
                    return;
                }
                isAutoUpdating = true;
                try {
                    if (s.length() > 0) {
                        int w = Integer.parseInt(s.toString());
                        int h = (int) (w * ((double) originalHeight / originalWidth));
                        heightInput.setText(String.valueOf(h));
                    } else {
                        heightInput.setText("");
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                isAutoUpdating = false;
            }
        });

        heightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isAutoUpdating || !maintainAspect.isChecked() || originalWidth == 0 || originalHeight == 0) {
                    return;
                }
                isAutoUpdating = true;
                try {
                    if (s.length() > 0) {
                        int h = Integer.parseInt(s.toString());
                        int w = (int) (h * ((double) originalWidth / originalHeight));
                        widthInput.setText(String.valueOf(w));
                    } else {
                        widthInput.setText("");
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                isAutoUpdating = false;
            }
        });

        // Standard scales presets click triggers
        preset25.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyPresetScale(0.25);
            }
        });

        preset50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyPresetScale(0.50);
            }
        });

        preset75.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyPresetScale(0.75);
            }
        });

        preset100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyPresetScale(1.00);
            }
        });

        // Special Requirement Document Dimension presets
        presetPassport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyExactDimensions(350, 450);
                Toast.makeText(MainActivity.this, "Set to Standard Passport size: 350x450 px", Toast.LENGTH_SHORT).show();
            }
        });

        presetSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyExactDimensions(300, 100);
                Toast.makeText(MainActivity.this, "Set to Standard Signature size: 300x100 px", Toast.LENGTH_SHORT).show();
            }
        });

        // Format selector change listener
        formatRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                if (checkedId == R.id.radioPng) {
                    qualityLayout.setVisibility(View.GONE);
                } else {
                    qualityLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // Quality Seekbar slide handler
        qualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityValText.setText("Compression Quality: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Requirements Trigger: Ask for action confirmation prior to resizing
        btnResize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                promptResizeActionConfirmation();
            }
        });

        // Trigger Image saving
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                checkPermissionAndSaveImage();
            }
        });

        // Trigger Image Share
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                shareCurrentImage();
            }
        });
    }

    private void playDeviceSound(int toneType) {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(toneType, 120);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            handlePickedImageUri(data.getData());
        }
    }

    private void handlePickedImageUri(Uri uri) {
        selectedImageUri = uri;
        showLoadingOverlay("Reading selected image details...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = getContentResolver().openInputStream(selectedImageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, options);
                    if (input != null) {
                        input.close();
                    }

                    originalWidth = options.outWidth;
                    originalHeight = options.outHeight;
                    final String format = options.outMimeType;

                    long fileSize = 0;
                    try {
                        android.content.res.AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(selectedImageUri, "r");
                        if (fd != null) {
                            fileSize = fd.getLength();
                            fd.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    final double kbSize = fileSize / 1024.0;
                    final Bitmap previewBitmap = loadScaledBitmapFromUri(selectedImageUri, 1024);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            if (previewBitmap != null) {
                                originalImageView.setImageBitmap(previewBitmap);
                                placeholderText.setVisibility(View.GONE);

                                String sizeLabel = String.format("Original Resolution: %d x %d px\nFormat: %s\nFile Size: %.2f KB", 
                                        originalWidth, originalHeight, format != null ? format.toUpperCase() : "UNKNOWN", kbSize);
                                originalInfoText.setText(sizeLabel);
                                originalInfoText.setVisibility(View.VISIBLE);

                                // Load original values into inputs
                                isAutoUpdating = true;
                                widthInput.setText(String.valueOf(originalWidth));
                                heightInput.setText(String.valueOf(originalHeight));
                                isAutoUpdating = false;

                                btnResize.setEnabled(true);

                                // Clean state
                                currentResizedBitmap = null;
                                finalCompressedData = null;
                                savedUri = null;
                                resizedImageView.setImageBitmap(null);
                                resizedPlaceholderText.setVisibility(View.VISIBLE);
                                resizedInfoText.setText("");
                                btnSave.setEnabled(false);
                                btnShare.setEnabled(false);
                                playDeviceSound(ToneGenerator.TONE_PROP_ACK);
                            } else {
                                Toast.makeText(MainActivity.this, "Error loading chosen image file.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "Error reading metadata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void applyPresetScale(double scale) {
        if (originalWidth == 0 || originalHeight == 0) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }
        int targetW = (int) (originalWidth * scale);
        int targetH = (int) (originalHeight * scale);

        isAutoUpdating = true;
        widthInput.setText(String.valueOf(targetW));
        heightInput.setText(String.valueOf(targetH));
        isAutoUpdating = false;
    }

    private void applyExactDimensions(int targetW, int targetH) {
        if (originalWidth == 0 || originalHeight == 0) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Disable aspect lock momentarily for exact dimension presets
        maintainAspect.setChecked(false);
        isAutoUpdating = true;
        widthInput.setText(String.valueOf(targetW));
        heightInput.setText(String.valueOf(targetH));
        isAutoUpdating = false;
    }

    private Bitmap loadScaledBitmapFromUri(Uri uri, int maxDim) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            int sampleSize = 1;
            int maxVal = Math.max(options.outWidth, options.outHeight);
            if (maxDim > 0 && maxVal > maxDim) {
                while (maxVal / sampleSize > maxDim) {
                    sampleSize *= 2;
                }
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, decodeOptions);
            input.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Requirements Trigger: Ask for action before execution
    private void promptResizeActionConfirmation() {
        final String wStr = widthInput.getText().toString().trim();
        final String hStr = heightInput.getText().toString().trim();

        if (wStr.isEmpty() || hStr.isEmpty()) {
            Toast.makeText(this, "Please type target Width and Height dimensions!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Resize Action");
        builder.setMessage("Are you sure you want to scale this image to " + wStr + " x " + hStr + " px?");
        builder.setPositiveButton("Yes, Resize", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                executeImageResizingWorkflow(Integer.parseInt(wStr), Integer.parseInt(hStr));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void executeImageResizingWorkflow(final int targetWidth, final int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            Toast.makeText(this, "Size dimension values must be greater than zero!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth > 5000 || targetHeight > 5000) {
            Toast.makeText(this, "Dimension exceeds maximum limit of 5000px.", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedId = formatRadioGroup.getCheckedRadioButtonId();
        final String format;
        if (checkedId == R.id.radioPng) {
            format = "PNG";
        } else if (checkedId == R.id.radioWebp) {
            format = "WEBP";
        } else {
            format = "JPEG";
        }
        finalSelectedFormat = format;

        final int quality = qualitySeekBar.getProgress();
        showLoadingOverlay("Resizing photo dimensions...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap srcBitmap = loadScaledBitmapFromUri(selectedImageUri, 3072);
                    if (srcBitmap == null) {
                        throw new Exception("Unable to decode file source.");
                    }

                    final Bitmap resizedBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true);

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                    if (format.equalsIgnoreCase("PNG")) {
                        compressFormat = Bitmap.CompressFormat.PNG;
                    } else if (format.equalsIgnoreCase("WEBP")) {
                        compressFormat = Bitmap.CompressFormat.WEBP;
                    }

                    resizedBitmap.compress(compressFormat, quality, byteStream);
                    finalCompressedData = byteStream.toByteArray();
                    byteStream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            if (resizedBitmap != null) {
                                currentResizedBitmap = resizedBitmap;
                                resizedImageView.setImageBitmap(resizedBitmap);
                                resizedPlaceholderText.setVisibility(View.GONE);

                                double finalKb = finalCompressedData.length / 1024.0;
                                String resultInfo = String.format("Resizing completed successfully!\nResolution: %d x %d px\nEstimated File Size: %.2f KB", 
                                        targetWidth, targetHeight, finalKb);
                                resizedInfoText.setText(resultInfo);

                                btnSave.setEnabled(true);
                                btnShare.setEnabled(true);
                                savedUri = null; // reset save path
                                playDeviceSound(ToneGenerator.TONE_PROP_ACK);
                                Toast.makeText(MainActivity.this, "Resize complete! Ready to save.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "Processing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void checkPermissionAndSaveImage() {
        if (android.os.Build.VERSION.SDK_INT <= 28) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        executeImageSaveWorkflow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                executeImageSaveWorkflow();
            } else {
                Toast.makeText(this, "Storage write permission is required to save resized image.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void executeImageSaveWorkflow() {
        if (currentResizedBitmap == null || finalCompressedData == null) {
            Toast.makeText(this, "No modified image to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoadingOverlay("Saving picture to device library...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = "Resized_" + System.currentTimeMillis();
                    String mimeType = "image/jpeg";
                    String ext = ".jpg";

                    if (finalSelectedFormat.equalsIgnoreCase("PNG")) {
                        mimeType = "image/png";
                        ext = ".png";
                    } else if (finalSelectedFormat.equalsIgnoreCase("WEBP")) {
                        mimeType = "image/webp";
                        ext = ".webp";
                    }

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ext);
                    values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoResizer");

                    ContentResolver resolver = getContentResolver();
                    final Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    if (imageUri != null) {
                        OutputStream outStream = resolver.openOutputStream(imageUri);
                        outStream.write(finalCompressedData);
                        outStream.flush();
                        outStream.close();

                        savedUri = imageUri;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoadingOverlay();
                                playDeviceSound(ToneGenerator.TONE_PROP_ACK);
                                promptPostSaveAction();
                            }
                        });
                    } else {
                        throw new Exception("Media resolver insertion failed.");
                    }

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    // Requirements Trigger: App prompts user for follow-up choice/action dialog after save completes
    private void promptPostSaveAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Image Saved Successfully!");
        builder.setMessage("Your resized photo is successfully written under 'Pictures/PhotoResizer'.\n\nWhat would you like to do next?");
        builder.setPositiveButton("Share Resized Photo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shareCurrentImage();
            }
        });
        builder.setNegativeButton("Stay on Page", null);
        builder.show();
    }

    private void shareCurrentImage() {
        if (currentResizedBitmap == null || finalCompressedData == null) {
            Toast.makeText(this, "Resized file is unavailable to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedUri != null) {
            triggerShareIntent(savedUri);
        } else {
            showLoadingOverlay("Preparing download link to share...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String fileName = "Share_" + System.currentTimeMillis();
                        String mimeType = "image/jpeg";
                        String ext = ".jpg";

                        if (finalSelectedFormat.equalsIgnoreCase("PNG")) {
                            mimeType = "image/png";
                            ext = ".png";
                        } else if (finalSelectedFormat.equalsIgnoreCase("WEBP")) {
                            mimeType = "image/webp";
                            ext = ".webp";
                        }

                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ext);
                        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoResizerTemp");

                        ContentResolver resolver = getContentResolver();
                        final Uri tempUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                        if (tempUri != null) {
                            OutputStream outStream = resolver.openOutputStream(tempUri);
                            outStream.write(finalCompressedData);
                            outStream.flush();
                            outStream.close();

                            savedUri = tempUri;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideLoadingOverlay();
                                    triggerShareIntent(tempUri);
                                }
                            });
                        }
                    } catch (final Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoadingOverlay();
                                Toast.makeText(MainActivity.this, "Sharing initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void triggerShareIntent(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Resized Image"));
    }

    private void showLoadingOverlay(String message) {
        progressMessageText.setText(message);
        progressOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoadingOverlay() {
        progressOverlay.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}