package com.photoresizerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQUEST_IMAGE_PICK = 1001;

    // View References
    private FrameLayout placeholderContainer;
    private LinearLayout placeholderElements;
    private ImageView imagePreview;
    private TextView imageInfoText;
    private Button btnRotateLeft;
    private Button btnRotateRight;
    private Spinner spinnerPresets;
    private EditText inputWidth;
    private EditText inputHeight;
    private Spinner spinnerFormats;
    private LinearLayout layoutQualityControl;
    private TextView textQualityPercentage;
    private SeekBar seekbarQuality;
    private Button btnResizeExecute;
    private Button btnSaveGallery;
    private Button btnShareImage;

    // Bitmaps and States
    private Bitmap originalBitmap = null;
    private Bitmap workingBitmap = null;  // Handles rotations
    private Bitmap resizedBitmap = null;  // Final resized result

    private int originalWidth = 0;
    private int originalHeight = 0;

    // Size Presets
    private static final String PRESET_CUSTOM = "Custom Size (Manual px)";
    private static final String PRESET_US_PASSPORT = "US Passport (2x2 in / 600x600 px)";
    private static final String PRESET_UK_PASSPORT = "UK Passport (35x45 mm / 413x531 px)";
    private static final String PRESET_IN_PASSPORT = "Indian Passport (35x45 mm / 413x531 px)";
    private static final String PRESET_STAMP_SIZE = "Stamp Size (20x25 mm / 236x295 px)";
    private static final String PRESET_FULL_HD = "Full HD Landscape (1920x1080 px)";
    private static final String PRESET_SQUARE = "Square Profile (1080x1080 px)";

    private String[] presetLabels = {
            PRESET_CUSTOM,
            PRESET_US_PASSPORT,
            PRESET_UK_PASSPORT,
            PRESET_IN_PASSPORT,
            PRESET_STAMP_SIZE,
            PRESET_FULL_HD,
            PRESET_SQUARE
    };

    // Format Options
    private static final String FORMAT_JPEG = "JPEG (.jpg)";
    private static final String FORMAT_PNG = "PNG (.png)";
    private static final String FORMAT_WEBP = "WEBP (.webp)";

    private String[] formatLabels = {
            FORMAT_JPEG,
            FORMAT_PNG,
            FORMAT_WEBP
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupListeners();
        updateButtonStates();
    }

    private void initializeUI() {
        placeholderContainer = (FrameLayout) findViewById(R.id.import_placeholder_container);
        placeholderElements = (LinearLayout) findViewById(R.id.placeholder_elements);
        imagePreview = (ImageView) findViewById(R.id.image_preview);
        imageInfoText = (TextView) findViewById(R.id.image_info_text);
        btnRotateLeft = (Button) findViewById(R.id.btn_rotate_left);
        btnRotateRight = (Button) findViewById(R.id.btn_rotate_right);
        spinnerPresets = (Spinner) findViewById(R.id.spinner_presets);
        inputWidth = (EditText) findViewById(R.id.input_width);
        inputHeight = (EditText) findViewById(R.id.input_height);
        spinnerFormats = (Spinner) findViewById(R.id.spinner_formats);
        layoutQualityControl = (LinearLayout) findViewById(R.id.layout_quality_control);
        textQualityPercentage = (TextView) findViewById(R.id.text_quality_percentage);
        seekbarQuality = (SeekBar) findViewById(R.id.seekbar_quality);
        btnResizeExecute = (Button) findViewById(R.id.btn_resize_execute);
        btnSaveGallery = (Button) findViewById(R.id.btn_save_gallery);
        btnShareImage = (Button) findViewById(R.id.btn_share_image);

        // Populate Preset Spinner
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                presetLabels
        );
        spinnerPresets.setAdapter(presetAdapter);

        // Populate Format Spinner
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                formatLabels
        );
        spinnerFormats.setAdapter(formatAdapter);
    }

    private void setupListeners() {
        // Image Selection Action
        placeholderContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importImageFromGallery();
            }
        });

        // Rotation Action
        btnRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateImage(-90);
            }
        });

        btnRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateImage(90);
            }
        });

        // Preset Selections Actions
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPreset = presetLabels[position];
                onPresetSelected(selectedPreset);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Format Selection Action (PNG does not support compression quality options)
        spinnerFormats.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String format = formatLabels[position];
                if (FORMAT_PNG.equals(format)) {
                    layoutQualityControl.setVisibility(View.GONE);
                } else {
                    layoutQualityControl.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Quality slider change listener
        seekbarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textQualityPercentage.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Execute resizing action
        btnResizeExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeResizing();
            }
        });

        // Save Image action
        btnSaveGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResizedImageToGallery(true);
            }
        });

        // Share Image action
        btnShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResizedImage();
            }
        });
    }

    private void updateButtonStates() {
        boolean imageLoaded = (workingBitmap != null);
        btnRotateLeft.setEnabled(imageLoaded);
        btnRotateRight.setEnabled(imageLoaded);
        btnResizeExecute.setEnabled(imageLoaded);

        boolean resizedAvailable = (resizedBitmap != null);
        btnSaveGallery.setEnabled(resizedAvailable);
        btnShareImage.setEnabled(resizedAvailable);
    }

    private void importImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            loadBitmapFromUri(imageUri);
        }
    }

    private void loadBitmapFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            
            // Decodes size first to prevent out-of-memory errors
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) {
                is.close();
            }

            // Downsample high resolution images gracefully to run efficiently on legacy devices
            int limitSize = 2500;
            int scale = 1;
            if (options.outHeight > limitSize || options.outWidth > limitSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(limitSize / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options realOptions = new BitmapFactory.Options();
            realOptions.inSampleSize = scale;

            is = getContentResolver().openInputStream(uri);
            originalBitmap = BitmapFactory.decodeStream(is, null, realOptions);
            if (is != null) {
                is.close();
            }

            if (originalBitmap != null) {
                workingBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                originalWidth = workingBitmap.getWidth();
                originalHeight = workingBitmap.getHeight();

                // Clear previously resized items
                resizedBitmap = null;

                // Update UI state
                placeholderElements.setVisibility(View.GONE);
                imagePreview.setImageBitmap(workingBitmap);
                updateImageInfoText(originalWidth, originalHeight, "Original Loaded");

                // Reset preset to Custom to trigger proper initial size filling
                spinnerPresets.setSelection(0);
                inputWidth.setText(String.valueOf(originalWidth));
                inputHeight.setText(String.valueOf(originalHeight));

                updateButtonStates();
            } else {
                Toast.makeText(this, "Could not load image file.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to import image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateImageInfoText(int w, int h, String statusText) {
        String info = statusText + ": " + w + " × " + h + " px";
        imageInfoText.setText(info);
    }

    private void rotateImage(float degrees) {
        if (workingBitmap == null) return;
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(
                    workingBitmap,
                    0, 0,
                    workingBitmap.getWidth(),
                    workingBitmap.getHeight(),
                    matrix,
                    true
            );
            workingBitmap = rotated;
            imagePreview.setImageBitmap(workingBitmap);

            // Update inputs if they are using loaded measurements
            int newW = workingBitmap.getWidth();
            int newH = workingBitmap.getHeight();
            updateImageInfoText(newW, newH, "Rotated Image");

            // If a manual or custom size was entered, optionally swap fields
            if (spinnerPresets.getSelectedItemPosition() == 0) {
                String tempW = inputWidth.getText().toString();
                String tempH = inputHeight.getText().toString();
                inputWidth.setText(tempH);
                inputHeight.setText(tempW);
            }

        } catch (OutOfMemoryError oom) {
            Toast.makeText(this, "Insufficient memory to rotate image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPresetSelected(String preset) {
        if (preset.equals(PRESET_CUSTOM)) {
            inputWidth.setEnabled(true);
            inputHeight.setEnabled(true);
            if (workingBitmap != null) {
                inputWidth.setText(String.valueOf(workingBitmap.getWidth()));
                inputHeight.setText(String.valueOf(workingBitmap.getHeight()));
            }
        } else if (preset.equals(PRESET_US_PASSPORT)) {
            // US Passport standard is 2x2 inches (600x600 px at 300 DPI)
            inputWidth.setText("600");
            inputHeight.setText("600");
            inputWidth.setEnabled(false);
            inputHeight.setEnabled(false);
        } else if (preset.equals(PRESET_UK_PASSPORT) || preset.equals(PRESET_IN_PASSPORT)) {
            // UK & Indian Passport standard: 35x45 mm (413x531 px at 300 DPI)
            inputWidth.setText("413");
            inputHeight.setText("531");
            inputWidth.setEnabled(false);
            inputHeight.setEnabled(false);
        } else if (preset.equals(PRESET_STAMP_SIZE)) {
            // Standard stamp size: 2.0x2.5 cm (approx 236x295 px at 300 DPI)
            inputWidth.setText("236");
            inputHeight.setText("295");
            inputWidth.setEnabled(false);
            inputHeight.setEnabled(false);
        } else if (preset.equals(PRESET_FULL_HD)) {
            inputWidth.setText("1920");
            inputHeight.setText("1080");
            inputWidth.setEnabled(false);
            inputHeight.setEnabled(false);
        } else if (preset.equals(PRESET_SQUARE)) {
            inputWidth.setText("1080");
            inputHeight.setText("1080");
            inputWidth.setEnabled(false);
            inputHeight.setEnabled(false);
        }
    }

    private void executeResizing() {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please import a photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String wString = inputWidth.getText().toString().trim();
        String hString = inputHeight.getText().toString().trim();

        if (wString.isEmpty() || hString.isEmpty()) {
            Toast.makeText(this, "Please enter valid dimensions.", Toast.LENGTH_SHORT).show();
            return;
        }

        int targetWidth;
        int targetHeight;
        try {
            targetWidth = Integer.parseInt(wString);
            targetHeight = Integer.parseInt(hString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dimensions must be numeric values.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth <= 0 || targetHeight <= 0) {
            Toast.makeText(this, "Dimensions must be greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetWidth > 8000 || targetHeight > 8000) {
            Toast.makeText(this, "To avoid crash, dimensions are limited to max 8000 px.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Scale bitmap using high-quality filtering
            resizedBitmap = Bitmap.createScaledBitmap(workingBitmap, targetWidth, targetHeight, true);
            imagePreview.setImageBitmap(resizedBitmap);

            updateImageInfoText(targetWidth, targetHeight, "Resized Result");
            updateButtonStates();

            Toast.makeText(this, "Photo resized successfully!", Toast.LENGTH_SHORT).show();

        } catch (OutOfMemoryError oom) {
            Toast.makeText(this, "Out of memory. Try smaller dimensions.", Toast.LENGTH_LONG).show();
        }
    }

    private Uri saveResizedImageToGallery(boolean showToast) {
        if (resizedBitmap == null) {
            if (showToast) {
                Toast.makeText(this, "Nothing to save. Please resize first.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        String selectedFormat = spinnerFormats.getSelectedItem().toString();
        Bitmap.CompressFormat format;
        String mimeType;
        String extension;

        if (selectedFormat.equals(FORMAT_PNG)) {
            format = Bitmap.CompressFormat.PNG;
            mimeType = "image/png";
            extension = ".png";
        } else if (selectedFormat.equals(FORMAT_WEBP)) {
            format = Bitmap.CompressFormat.WEBP;
            mimeType = "image/webp";
            extension = ".webp";
        } else {
            format = Bitmap.CompressFormat.JPEG;
            mimeType = "image/jpeg";
            extension = ".jpg";
        }

        int quality = seekbarQuality.getProgress();
        String fileName = "Resized_" + System.currentTimeMillis() + extension;

        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoResizerPro");
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri imageCollectionUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageCollectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            imageCollectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        Uri savedUri = resolver.insert(imageCollectionUri, contentValues);

        if (savedUri == null) {
            if (showToast) {
                Toast.makeText(this, "Failed to create media file.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        try {
            OutputStream out = resolver.openOutputStream(savedUri);
            if (out != null) {
                boolean compressed = resizedBitmap.compress(format, quality, out);
                out.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear();
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(savedUri, contentValues, null, null);
                }

                if (compressed) {
                    if (showToast) {
                        Toast.makeText(this, "Saved to Gallery: Pictures/PhotoResizerPro", Toast.LENGTH_LONG).show();
                    }
                    return savedUri;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resolver.delete(savedUri, null, null);
            if (showToast) {
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

    private void shareResizedImage() {
        if (resizedBitmap == null) {
            Toast.makeText(this, "Please resize an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Silent save so we have a valid content URI to share with other applications
        Uri imageToShare = saveResizedImageToGallery(false);

        if (imageToShare == null) {
            Toast.makeText(this, "Failed to prepare image for sharing.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageToShare);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Resized Image"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}