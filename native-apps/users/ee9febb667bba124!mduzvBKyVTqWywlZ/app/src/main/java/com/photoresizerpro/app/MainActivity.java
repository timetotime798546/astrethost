package com.photoresizerpro.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Select Photo Event
        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        // Preset Button click actions
        preset25.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPresetScale(0.25);
            }
        });

        preset50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPresetScale(0.50);
            }
        });

        preset75.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPresetScale(0.75);
            }
        });

        preset100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPresetScale(1.00);
            }
        });

        // Format selector change listener to toggle quality slider (PNG doesn't use standard quality compression loss)
        formatRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
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
                qualityValText.setText("गुणवत्ता (Compression Quality): " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Trigger Resizing
        btnResize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImageResizingWorkflow();
            }
        });

        // Trigger Image saving
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndSaveImage();
            }
        });

        // Trigger Image Share
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCurrentImage();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "फ़ोटो चुनें (Select Photo)"), PICK_IMAGE_REQUEST);
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
        showLoadingOverlay("फ़ोटो लोड हो रही है...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Extract dimensions cleanly without loading full bitmap into RAM
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

                    // Query original file size
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

                    // Load resized preview bitmap safely (max 1024px limit) to prevent out of memory crash
                    final Bitmap previewBitmap = loadScaledBitmapFromUri(selectedImageUri, 1024);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            if (previewBitmap != null) {
                                originalImageView.setImageBitmap(previewBitmap);
                                placeholderText.setVisibility(View.GONE);

                                // Format metadata display in Hindi
                                String sizeLabel = String.format("मूल आकार: %d x %d px\nप्रारूप: %s\nफ़ाइल साइज़: %.2f KB", 
                                        originalWidth, originalHeight, format != null ? format.toUpperCase() : "अज्ञात", kbSize);
                                originalInfoText.setText(sizeLabel);
                                originalInfoText.setVisibility(View.VISIBLE);

                                // Update manual numeric fields
                                isAutoUpdating = true;
                                widthInput.setText(String.valueOf(originalWidth));
                                heightInput.setText(String.valueOf(originalHeight));
                                isAutoUpdating = false;

                                // Enable action controls
                                btnResize.setEnabled(true);

                                // Clear previous resize outputs
                                currentResizedBitmap = null;
                                finalCompressedData = null;
                                savedUri = null;
                                resizedImageView.setImageBitmap(null);
                                resizedPlaceholderText.setVisibility(View.VISIBLE);
                                resizedInfoText.setText("");
                                btnSave.setEnabled(false);
                                btnShare.setEnabled(false);
                            } else {
                                Toast.makeText(MainActivity.this, "फ़ोटो लोड करने में समस्या हुई।", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "गलती: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void applyPresetScale(double scale) {
        if (originalWidth == 0 || originalHeight == 0) {
            Toast.makeText(this, "कृपया पहले गैलरी से एक फ़ोटो चुनें!", Toast.LENGTH_SHORT).show();
            return;
        }
        int targetW = (int) (originalWidth * scale);
        int targetH = (int) (originalHeight * scale);

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

    private void startImageResizingWorkflow() {
        String wStr = widthInput.getText().toString().trim();
        String hStr = heightInput.getText().toString().trim();

        if (wStr.isEmpty() || hStr.isEmpty()) {
            Toast.makeText(this, "कृपया चौड़ाई और ऊंचाई दर्ज करें!", Toast.LENGTH_SHORT).show();
            return;
        }

        final int targetWidth = Integer.parseInt(wStr);
        final int targetHeight = Integer.parseInt(hStr);

        if (targetWidth <= 0 || targetHeight <= 0) {
            Toast.makeText(this, "आकार शून्य से अधिक होना चाहिए!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth > 5000 || targetHeight > 5000) {
            Toast.makeText(this, "बहुत बड़ा आकार! अधिकतम सीमा 5000px है।", Toast.LENGTH_SHORT).show();
            return;
        }

        // Capture compression parameters
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

        showLoadingOverlay("इमेज का आकार रीसाइज़ किया जा रहा है...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Safe original loaded bitmap (decode to a maximum size of 3072 to avoid crash, but keep resizing exact)
                    Bitmap srcBitmap = loadScaledBitmapFromUri(selectedImageUri, 3072);
                    if (srcBitmap == null) {
                        throw new Exception("इमेज डेटा डिकोड नहीं किया जा सका");
                    }

                    // Create scaled bitmap
                    final Bitmap resizedBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true);

                    // Compress to capture estimated size and Preview data
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
                                String resultInfo = String.format("सफलतापूर्वक रीसाइज़ किया गया!\nनया आकार: %d x %d px\nअनुमानित फ़ाइल साइज़: %.2f KB", 
                                        targetWidth, targetHeight, finalKb);
                                resizedInfoText.setText(resultInfo);

                                btnSave.setEnabled(true);
                                btnShare.setEnabled(true);
                                savedUri = null; // reset save path state
                                Toast.makeText(MainActivity.this, "रीसाइज़ पूरा हुआ! सुरक्षित करने के लिए नीचे बटन दबाएं।", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "त्रुटि: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, "सुरक्षित करने के लिए स्टोरेज परमिशन की आवश्यकता है।", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void executeImageSaveWorkflow() {
        if (currentResizedBitmap == null || finalCompressedData == null) {
            Toast.makeText(this, "सहेजने के लिए कोई रीसाइज़ की गई फ़ोटो नहीं है!", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoadingOverlay("इमेज गैलरी में सेव की जा रही है...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = "Resized_" + System.currentTimeMillis();
                    String mimeType = "image/jpeg";
                    String ext = ".jpg";

                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                    if (finalSelectedFormat.equalsIgnoreCase("PNG")) {
                        compressFormat = Bitmap.CompressFormat.PNG;
                        mimeType = "image/png";
                        ext = ".png";
                    } else if (finalSelectedFormat.equalsIgnoreCase("WEBP")) {
                        compressFormat = Bitmap.CompressFormat.WEBP;
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
                                Toast.makeText(MainActivity.this, "सफलतापूर्वक गैलरी में 'Pictures/PhotoResizer' फोल्डर में सेव की गई!", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        throw new Exception("इमेज सेव करने के लिए Uri नहीं मिला।");
                    }

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            Toast.makeText(MainActivity.this, "सहेजने में विफल: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void shareCurrentImage() {
        if (currentResizedBitmap == null || finalCompressedData == null) {
            Toast.makeText(this, "कृपया पहले फ़ोटो रीसाइज़ करें!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedUri != null) {
            // Share using the already saved public MediaStore Uri directly without re-saving
            triggerShareIntent(savedUri);
        } else {
            // If not saved yet, perform a quick background save to media store to generate shareable Content Uri
            showLoadingOverlay("शेयर करने के लिए लिंक तैयार की जा रही है...");
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
                                Toast.makeText(MainActivity.this, "शेयर करने में समस्या: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        startActivity(Intent.createChooser(shareIntent, "फ़ोटो शेयर करें"));
    }

    private void showLoadingOverlay(String message) {
        progressMessageText.setText(message);
        progressOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoadingOverlay() {
        progressOverlay.setVisibility(View.GONE);
    }
}