package com.acresizer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
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
    private Button btnRotateInput;

    // Options UI Elements
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

    // Enhancer & Border Styling Controls
    private SeekBar seekBarBrightness;
    private SeekBar seekBarContrast;
    private SeekBar seekBarBorderWidth;
    private Button btnEnhanceAuto, btnEnhanceGray, btnEnhanceReset, btn3DEffect;
    private Button btnBorderWhite, btnBorderBlack, btnBorderRed, btnBorderBlue, btnBorderGold;
    private TextView lblBrightness, lblContrast, lblBorderWidth;

    // Theme Selector Buttons
    private Button btnThemeBlue, btnThemeDark, btnThemeGreen, btnThemePurple;

    // Resized Preview UI Elements
    private ImageView resizedImageView;
    private TextView resizedPlaceholderText;
    private TextView resizedInfoText;
    private Button btnRotateOutput, btnFlipCard;
    private View layoutPreviewFlipContainer;
    private View sidePreviewFront;
    private View sidePreviewBack;

    // Analytics Back Card Elements
    private TextView txtAnalyticsDimensions;
    private TextView txtAnalyticsFormat;
    private TextView txtAnalyticsAspect;
    private TextView txtAnalyticsMemory;

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

    // Orientation State Parameters
    private int baseExifRotation = 0;
    private int manualInputRotation = 0;
    private int manualOutputRotation = 0;

    // Border State Parameter
    private int selectedBorderColor = Color.WHITE;

    // Filter State Parameter
    private boolean isGrayscaleActive = false;
    private boolean is3DStereoscopicActive = false;

    // Final Resized bitmap state
    private Bitmap currentResizedBitmap = null;
    private byte[] finalCompressedData = null;
    private String finalSelectedFormat = "JPEG";
    private Uri savedUri = null;

    // Card Flipping Boolean State
    private boolean isCardFlipped = false;

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
        btnRotateInput = (Button) findViewById(R.id.btnRotateInput);

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

        // Bind Enhancer Layout Elements
        seekBarBrightness = (SeekBar) findViewById(R.id.seekBarBrightness);
        seekBarContrast = (SeekBar) findViewById(R.id.seekBarContrast);
        seekBarBorderWidth = (SeekBar) findViewById(R.id.seekBarBorderWidth);
        btnEnhanceAuto = (Button) findViewById(R.id.btnEnhanceAuto);
        btnEnhanceGray = (Button) findViewById(R.id.btnEnhanceGray);
        btnEnhanceReset = (Button) findViewById(R.id.btnEnhanceReset);
        btn3DEffect = (Button) findViewById(R.id.btn3DEffect);
        btnBorderWhite = (Button) findViewById(R.id.btnBorderWhite);
        btnBorderBlack = (Button) findViewById(R.id.btnBorderBlack);
        btnBorderRed = (Button) findViewById(R.id.btnBorderRed);
        btnBorderBlue = (Button) findViewById(R.id.btnBorderBlue);
        btnBorderGold = (Button) findViewById(R.id.btnBorderGold);
        lblBrightness = (TextView) findViewById(R.id.lblBrightness);
        lblContrast = (TextView) findViewById(R.id.lblContrast);
        lblBorderWidth = (TextView) findViewById(R.id.lblBorderWidth);

        // Bind Theme Selector Layout Elements
        btnThemeBlue = (Button) findViewById(R.id.btnThemeBlue);
        btnThemeDark = (Button) findViewById(R.id.btnThemeDark);
        btnThemeGreen = (Button) findViewById(R.id.btnThemeGreen);
        btnThemePurple = (Button) findViewById(R.id.btnThemePurple);

        // Bind Resized Preview Layout Elements
        resizedImageView = (ImageView) findViewById(R.id.resizedImageView);
        resizedPlaceholderText = (TextView) findViewById(R.id.resizedPlaceholderText);
        resizedInfoText = (TextView) findViewById(R.id.resizedInfoText);
        btnRotateOutput = (Button) findViewById(R.id.btnRotateOutput);
        btnFlipCard = (Button) findViewById(R.id.btnFlipCard);
        layoutPreviewFlipContainer = findViewById(R.id.layoutPreviewFlipContainer);
        sidePreviewFront = findViewById(R.id.sidePreviewFront);
        sidePreviewBack = findViewById(R.id.sidePreviewBack);

        // Bind Analytics Panel Elements
        txtAnalyticsDimensions = (TextView) findViewById(R.id.txtAnalyticsDimensions);
        txtAnalyticsFormat = (TextView) findViewById(R.id.txtAnalyticsFormat);
        txtAnalyticsAspect = (TextView) findViewById(R.id.txtAnalyticsAspect);
        txtAnalyticsMemory = (TextView) findViewById(R.id.txtAnalyticsMemory);

        btnResize = (Button) findViewById(R.id.btnResize);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnShare = (Button) findViewById(R.id.btnShare);

        progressOverlay = (LinearLayout) findViewById(R.id.progressOverlay);
        progressMessageText = (TextView) findViewById(R.id.progressMessageText);

        // Apply interactive 3D perspective touch listener to all main structural cards
        setupInteractive3DTilt(findViewById(R.id.cardInput));
        setupInteractive3DTilt(findViewById(R.id.cardResize));
        setupInteractive3DTilt(findViewById(R.id.cardEnhancer));
        setupInteractive3DTilt(findViewById(R.id.cardPreview));
        setupInteractive3DTilt(findViewById(R.id.themeCard));

        // Show custom Loading overlay on start
        showLoadingOverlay("Initializing 3D Ambient Engine...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideLoadingOverlay();
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                // Perform gentle entry intro slide-up effect
                View rootView = findViewById(R.id.mainRootLayout);
                rootView.setAlpha(0f);
                rootView.animate().alpha(1f).setDuration(600).start();
            }
        }, 1000);

        // 3D Double Sided Card Flip Trigger Animation
        btnFlipCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                execute3DFlipAnimation();
            }
        });

        // Theme Click Events
        btnThemeBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyTheme(1);
            }
        });
        btnThemeDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyTheme(2);
            }
        });
        btnThemeGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyTheme(3);
            }
        });
        btnThemePurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                applyTheme(4);
            }
        });

        // Brightness & Contrast Seek Change Listeners
        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int offset = progress - 100;
                lblBrightness.setText("Brightness Offset: " + (offset > 0 ? "+" : "") + offset);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekBarContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 0.5f;
                if (progress < 50) {
                    scale = 0.5f + (progress / 100f);
                } else {
                    scale = 1.0f + ((progress - 50) / 50f);
                }
                lblContrast.setText(String.format("Contrast Scale: %.2fx", scale));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Border Settings Seek Changer
        seekBarBorderWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblBorderWidth.setText("Border Stroke Width: " + progress + " px");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Filter Action Triggers
        btnEnhanceAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                seekBarBrightness.setProgress(115); // +15
                seekBarContrast.setProgress(75);   // 1.5x
                isGrayscaleActive = false;
                is3DStereoscopicActive = false;
                Toast.makeText(MainActivity.this, "Auto-Vibrant algorithm ready", Toast.LENGTH_SHORT).show();
            }
        });
        btnEnhanceGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                isGrayscaleActive = true;
                is3DStereoscopicActive = false;
                Toast.makeText(MainActivity.this, "Monochrome filter enabled", Toast.LENGTH_SHORT).show();
            }
        });
        btn3DEffect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                is3DStereoscopicActive = true;
                isGrayscaleActive = false;
                Toast.makeText(MainActivity.this, "3D Stereoscopic Red-Cyan effect active", Toast.LENGTH_SHORT).show();
            }
        });
        btnEnhanceReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                seekBarBrightness.setProgress(100);
                seekBarContrast.setProgress(50);
                isGrayscaleActive = false;
                is3DStereoscopicActive = false;
                Toast.makeText(MainActivity.this, "Enhancement matrices initialized", Toast.LENGTH_SHORT).show();
            }
        });

        // Border Outlining Color Clicks
        btnBorderWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                selectedBorderColor = Color.WHITE;
                Toast.makeText(MainActivity.this, "Frame profile: WHITE", Toast.LENGTH_SHORT).show();
            }
        });
        btnBorderBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                selectedBorderColor = Color.BLACK;
                Toast.makeText(MainActivity.this, "Frame profile: BLACK", Toast.LENGTH_SHORT).show();
            }
        });
        btnBorderRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                selectedBorderColor = Color.RED;
                Toast.makeText(MainActivity.this, "Frame profile: RED", Toast.LENGTH_SHORT).show();
            }
        });
        btnBorderBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                selectedBorderColor = Color.parseColor("#3B82F6");
                Toast.makeText(MainActivity.this, "Frame profile: BLUE", Toast.LENGTH_SHORT).show();
            }
        });
        btnBorderGold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                selectedBorderColor = Color.parseColor("#D97706");
                Toast.makeText(MainActivity.this, "Frame profile: GOLD", Toast.LENGTH_SHORT).show();
            }
        });

        // Select Photo Event
        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP2);
                openImagePicker();
            }
        });

        // Rotate Input Photo Event
        btnRotateInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                if (selectedImageUri == null) return;
                
                manualInputRotation = (manualInputRotation + 90) % 360;
                showLoadingOverlay("Rotating active workspace viewport...");
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap baseBmp = loadScaledBitmapFromUri(selectedImageUri, 1024);
                        final Bitmap previewBmp = rotateBitmap(baseBmp, baseExifRotation + manualInputRotation);
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoadingOverlay();
                                if (previewBmp != null) {
                                    originalImageView.setImageBitmap(previewBmp);
                                    
                                    // Swap dimensions
                                    int temp = originalWidth;
                                    originalWidth = originalHeight;
                                    originalHeight = temp;
                                    
                                    String sizeLabel = String.format("Original File Space: %d x %d px (Rotated %d°)", 
                                            originalWidth, originalHeight, manualInputRotation);
                                    originalInfoText.setText(sizeLabel);
                                    
                                    isAutoUpdating = true;
                                    String wVal = widthInput.getText().toString();
                                    String hVal = heightInput.getText().toString();
                                    widthInput.setText(hVal);
                                    heightInput.setText(wVal);
                                    isAutoUpdating = false;
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // Rotate Output Photo Event
        btnRotateOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeviceSound(ToneGenerator.TONE_PROP_BEEP);
                if (currentResizedBitmap == null) return;
                
                manualOutputRotation = (manualOutputRotation + 90) % 360;
                showLoadingOverlay("Spanning transform matrix bounds...");
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap rotatedBmp = rotateBitmap(currentResizedBitmap, 90);
                        try {
                            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                            if (finalSelectedFormat.equalsIgnoreCase("PNG")) {
                                compressFormat = Bitmap.CompressFormat.PNG;
                            } else if (finalSelectedFormat.equalsIgnoreCase("WEBP")) {
                                compressFormat = Bitmap.CompressFormat.WEBP;
                            }
                            
                            rotatedBmp.compress(compressFormat, qualitySeekBar.getProgress(), byteStream);
                            finalCompressedData = byteStream.toByteArray();
                            byteStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoadingOverlay();
                                if (rotatedBmp != null) {
                                    currentResizedBitmap = rotatedBmp;
                                    resizedImageView.setImageBitmap(rotatedBmp);
                                    updateAnalyticsPanel();
                                    
                                    double finalKb = finalCompressedData.length / 1024.0;
                                    String resultInfo = String.format("Resizing completed successfully!\nResolution: %d x %d px\nEstimated File Size: %.2f KB", 
                                            rotatedBmp.getWidth(), rotatedBmp.getHeight(), finalKb);
                                    resizedInfoText.setText(resultInfo);
                                }
                            }
                        });
                    }
                }).start();
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

        // Document Dimension presets
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
                qualityValText.setText("Compression Ratio: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Ask for action confirmation prior to resizing
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

        // Set default theme styling on start
        applyTheme(1);
    }

    /**
     * Tactile 3D Perspective Tilt Algorithm
     * Allows user touches to smoothly pivot view groups in 3D matrix space.
     */
    private void setupInteractive3DTilt(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                float width = v.getWidth();
                float height = v.getHeight();
                if (width == 0 || height == 0) return false;

                // Configure perspective focal depth based on device display metrics density
                float scale = v.getResources().getDisplayMetrics().density;
                v.setCameraDistance(6000 * scale);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        float touchX = event.getX();
                        float touchY = event.getY();

                        // Map touch points to normalized Cartesian coordinates (-0.5 to 0.5)
                        float normX = (touchX / width) - 0.5f;
                        float normY = (touchY / height) - 0.5f;

                        // Clamp inputs to safe bounds
                        if (normX < -0.5f) normX = -0.5f;
                        if (normX > 0.5f) normX = 0.5f;
                        if (normY < -0.5f) normY = -0.5f;
                        if (normY > 0.5f) normY = 0.5f;

                        // Translate coordinate vectors to rotations (Max 15° degrees)
                        float rotX = -normY * 16f;
                        float rotY = normX * 16f;

                        v.setRotationX(rotX);
                        v.setRotationY(rotY);

                        // Dynamic tactile Z axis lift
                        v.setTranslationZ(12 * scale);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Bounce smoothly back to resting coordinate system
                        v.animate()
                                .rotationX(0f)
                                .rotationY(0f)
                                .translationZ(0f)
                                .setDuration(350)
                                .setInterpolator(new OvershootInterpolator(1.2f))
                                .start();
                        break;
                }
                return false; // Propagate down to internal clicks if applicable
            }
        });
    }

    /**
     * Executes double sided 3D flip animation sequence
     */
    private void execute3DFlipAnimation() {
        float density = getResources().getDisplayMetrics().density;
        layoutPreviewFlipContainer.setCameraDistance(8000 * density);

        // Spin view 90 degrees
        layoutPreviewFlipContainer.animate()
                .rotationY(isCardFlipped ? 90f : 90f)
                .setDuration(180)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        // Toggle views at rotation midpoint
                        if (isCardFlipped) {
                            sidePreviewFront.setVisibility(View.VISIBLE);
                            sidePreviewBack.setVisibility(View.GONE);
                            sidePreviewFront.setRotationY(0);
                            isCardFlipped = false;
                        } else {
                            sidePreviewFront.setVisibility(View.GONE);
                            sidePreviewBack.setVisibility(View.VISIBLE);
                            // Counter-rotate back side text so layout displays normally
                            sidePreviewBack.setRotationY(180);
                            isCardFlipped = true;
                        }
                        
                        // Spin to final rest
                        layoutPreviewFlipContainer.setRotationY(isCardFlipped ? 180f : 0f);
                        layoutPreviewFlipContainer.animate()
                                .rotationY(isCardFlipped ? 180f : 0f)
                                .setDuration(180)
                                .start();
                    }
                }).start();
    }

    /**
     * Direct Pixel red-cyan stereoscopic offset algorithm (True 3D Effect)
     */
    private Bitmap apply3DStereoscopicEffect(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        // Dynamic spatial shift offset proportional to resolution
        int offset = Math.max(6, w / 150);

        int[] pixels = new int[w * h];
        int[] resultPixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;

                // Shift red channel left
                int redX = Math.max(0, x - offset);
                int idxRed = y * w + redX;
                int redColor = pixels[idxRed];
                int r = (redColor >> 16) & 0xFF;

                // Shift cyan channel (green and blue) right
                int cyanX = Math.min(w - 1, x + offset);
                int idxCyan = y * w + cyanX;
                int cyanColor = pixels[idxCyan];
                int g = (cyanColor >> 8) & 0xFF;
                int b = cyanColor & 0xFF;

                // Combine channels
                resultPixels[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
        result.setPixels(resultPixels, 0, w, 0, 0, w, h);
        return result;
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

    private int getExifOrientation(Uri uri) {
        int rotation = 0;
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in != null) {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    android.media.ExifInterface exifInterface = new android.media.ExifInterface(in);
                    int orientation = exifInterface.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL);
                    switch (orientation) {
                        case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            break;
                    }
                }
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotation;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private void handlePickedImageUri(Uri uri) {
        selectedImageUri = uri;
        showLoadingOverlay("Parsing file descriptors...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    baseExifRotation = getExifOrientation(selectedImageUri);
                    manualInputRotation = 0;
                    manualOutputRotation = 0;

                    InputStream input = getContentResolver().openInputStream(selectedImageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, options);
                    if (input != null) {
                        input.close();
                    }

                    originalWidth = options.outWidth;
                    originalHeight = options.outHeight;
                    
                    if (baseExifRotation == 90 || baseExifRotation == 270) {
                        int temp = originalWidth;
                        originalWidth = originalHeight;
                        originalHeight = temp;
                    }

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
                    Bitmap baseBitmap = loadScaledBitmapFromUri(selectedImageUri, 1024);
                    final Bitmap previewBitmap = rotateBitmap(baseBitmap, baseExifRotation);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            if (previewBitmap != null) {
                                originalImageView.setImageBitmap(previewBitmap);
                                placeholderText.setVisibility(View.GONE);
                                btnRotateInput.setVisibility(View.VISIBLE);

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
                                btnRotateOutput.setVisibility(View.GONE);
                                resizedInfoText.setText("");
                                btnSave.setEnabled(false);
                                btnShare.setEnabled(false);
                                playDeviceSound(ToneGenerator.TONE_PROP_ACK);
                                updateAnalyticsPanel();
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

    private void promptResizeActionConfirmation() {
        final String wStr = widthInput.getText().toString().trim();
        final String hStr = heightInput.getText().toString().trim();

        if (wStr.isEmpty() || hStr.isEmpty()) {
            Toast.makeText(this, "Please type target Width and Height dimensions!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Image Styling");
        builder.setMessage("Are you sure you want to resize, style, and save this image to " + wStr + " x " + hStr + " px?");
        builder.setPositiveButton("Yes, Process", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                executeImageResizingWorkflow(Integer.parseInt(wStr), Integer.parseInt(hStr));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private Bitmap applyEnhancers(Bitmap src) {
        if (src == null) return null;

        // Brightness Offset (-100 to 100) mapped from SeekBar (0 to 200)
        float brightness = (seekBarBrightness.getProgress() - 100) * 1.5f;

        // Contrast Scaler (0.5x to 3.0x) mapped from SeekBar (0 to 150)
        float contrast = 1.0f;
        int progContrast = seekBarContrast.getProgress();
        if (progContrast < 50) {
            contrast = 0.5f + (progContrast / 100f);
        } else {
            contrast = 1.0f + ((progContrast - 50) / 50f);
        }

        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        float[] matrixArray = new float[] {
            contrast, 0, 0, 0, brightness,
            0, contrast, 0, 0, brightness,
            0, 0, contrast, 0, brightness,
            0, 0, 0, 1, 0
        };

        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix(matrixArray);

        if (isGrayscaleActive) {
            android.graphics.ColorMatrix grayMatrix = new android.graphics.ColorMatrix();
            grayMatrix.setSaturation(0);
            colorMatrix.postConcat(grayMatrix);
        }

        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(src, 0, 0, paint);

        return result;
    }

    private Bitmap applyBorder(Bitmap src) {
        int borderWidthDp = seekBarBorderWidth.getProgress();
        if (borderWidthDp <= 0 || src == null) {
            return src;
        }

        float density = getResources().getDisplayMetrics().density;
        int borderWidthPx = Math.round(borderWidthDp * density);
        if (borderWidthPx <= 0) {
            return src;
        }

        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(selectedBorderColor);
        paint.setStyle(Paint.Style.FILL);

        // Render borders inset
        canvas.drawRect(0, 0, src.getWidth(), borderWidthPx, paint);
        canvas.drawRect(0, src.getHeight() - borderWidthPx, src.getWidth(), src.getHeight(), paint);
        canvas.drawRect(0, 0, borderWidthPx, src.getHeight(), paint);
        canvas.drawRect(src.getWidth() - borderWidthPx, 0, src.getWidth(), src.getHeight(), paint);

        return result;
    }

    private void executeImageResizingWorkflow(final int targetWidth, final int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            Toast.makeText(this, "Dimensions must be greater than zero!", Toast.LENGTH_SHORT).show();
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
        showLoadingOverlay("Rendering 3D Visual Filters & Matrices...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap srcBitmap = loadScaledBitmapFromUri(selectedImageUri, 3072);
                    if (srcBitmap == null) {
                        throw new Exception("Unable to decode file source.");
                    }

                    // Rotate the source based on base EXIF and manual rotations
                    Bitmap rotatedSrc = rotateBitmap(srcBitmap, baseExifRotation + manualInputRotation);

                    // Resize Image
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedSrc, targetWidth, targetHeight, true);
                    if (scaledBitmap != rotatedSrc && rotatedSrc != srcBitmap) {
                        rotatedSrc.recycle();
                    }

                    // Enhancers Filters Implementation
                    Bitmap enhancedBmp = applyEnhancers(scaledBitmap);
                    if (enhancedBmp != scaledBitmap) {
                        scaledBitmap.recycle();
                    }

                    // Border Outlines Implementation
                    Bitmap finalStyledBmp = applyBorder(enhancedBmp);
                    if (finalStyledBmp != enhancedBmp) {
                        enhancedBmp.recycle();
                    }

                    // Dynamic red-cyan separation calculations if stereoscopic 3D filter is selected
                    if (is3DStereoscopicActive) {
                        Bitmap stereoscopicBmp = apply3DStereoscopicEffect(finalStyledBmp);
                        if (stereoscopicBmp != finalStyledBmp) {
                            finalStyledBmp.recycle();
                        }
                        finalStyledBmp = stereoscopicBmp;
                    }

                    final Bitmap finalOutputBitmap = finalStyledBmp;

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                    if (format.equalsIgnoreCase("PNG")) {
                        compressFormat = Bitmap.CompressFormat.PNG;
                    } else if (format.equalsIgnoreCase("WEBP")) {
                        compressFormat = Bitmap.CompressFormat.WEBP;
                    }

                    finalOutputBitmap.compress(compressFormat, quality, byteStream);
                    finalCompressedData = byteStream.toByteArray();
                    byteStream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideLoadingOverlay();
                            if (finalOutputBitmap != null) {
                                currentResizedBitmap = finalOutputBitmap;
                                resizedImageView.setImageBitmap(finalOutputBitmap);
                                resizedPlaceholderText.setVisibility(View.GONE);
                                btnRotateOutput.setVisibility(View.VISIBLE);

                                double finalKb = finalCompressedData.length / 1024.0;
                                String resultInfo = String.format("Resizing completed successfully!\nResolution: %d x %d px\nEstimated File Size: %.2f KB", 
                                        targetWidth, targetHeight, finalKb);
                                resizedInfoText.setText(resultInfo);

                                btnSave.setEnabled(true);
                                btnShare.setEnabled(true);
                                savedUri = null; // reset save path
                                playDeviceSound(ToneGenerator.TONE_PROP_ACK);
                                updateAnalyticsPanel();

                                // Bounce Entry Effect on Preview Card
                                View pCard = findViewById(R.id.cardPreview);
                                pCard.setScaleX(0.95f);
                                pCard.setScaleY(0.95f);
                                pCard.animate().scaleX(1f).scaleY(1f).setDuration(400).setInterpolator(new OvershootInterpolator()).start();

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

    private void updateAnalyticsPanel() {
        if (currentResizedBitmap == null) {
            txtAnalyticsDimensions.setText("Resolution target: N/A");
            txtAnalyticsFormat.setText("Format profile: N/A");
            txtAnalyticsAspect.setText("Aspect ratio balance: Calculating...");
            txtAnalyticsMemory.setText("Allocated Texture Overhead: 0 MB");
            return;
        }

        int w = currentResizedBitmap.getWidth();
        int h = currentResizedBitmap.getHeight();
        txtAnalyticsDimensions.setText("Resolution target: " + w + " x " + h + " px");
        txtAnalyticsFormat.setText("Format profile: " + finalSelectedFormat);
        
        double aspect = (double) w / h;
        txtAnalyticsAspect.setText(String.format("Aspect ratio balance: %.3f (%s)", aspect, (w > h ? "Landscape" : w < h ? "Portrait" : "Square")));

        // Programmatic texture byte footprint calculation (ARGB_8888 = 4 bytes per pixel)
        double mb = (w * h * 4.0) / (1024 * 1024);
        txtAnalyticsMemory.setText(String.format("Allocated Texture Overhead: %.2f MB", mb));
    }

    private void checkPermissionAndSaveImage() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT <= 28) {
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

        showLoadingOverlay("Writing pixel blocks to media library...");

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
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ACResizer");

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

    private void promptPostSaveAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Image Saved Successfully!");
        builder.setMessage("Your resized photo is successfully written under 'Pictures/ACResizer'.\n\nWhat would you like to do next?");
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
            showLoadingOverlay("Preparing temporary link to share...");
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
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ACResizerTemp");

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

    private void applyTheme(int themeId) {
        int headerColor;
        int bgColor;
        int cardColor;
        int textColor;
        int subtextColor;
        int primaryBtnColor;
        int secondaryBtnColor;
        
        switch (themeId) {
            case 2: // Dark Slate Mode
                headerColor = Color.parseColor("#1E293B");
                bgColor = Color.parseColor("#0F172A");
                cardColor = Color.parseColor("#1E293B");
                textColor = Color.parseColor("#F8FAFC");
                subtextColor = Color.parseColor("#94A3B8");
                primaryBtnColor = Color.parseColor("#3B82F6");
                secondaryBtnColor = Color.parseColor("#10B981");
                break;
                
            case 3: // Forest Green Mode
                headerColor = Color.parseColor("#064E3B");
                bgColor = Color.parseColor("#F0FDF4");
                cardColor = Color.parseColor("#FFFFFF");
                textColor = Color.parseColor("#111827");
                subtextColor = Color.parseColor("#064E3B");
                primaryBtnColor = Color.parseColor("#059669");
                secondaryBtnColor = Color.parseColor("#10B981");
                break;
                
            case 4: // Royal Purple Mode
                headerColor = Color.parseColor("#4C1D95");
                bgColor = Color.parseColor("#FAF5FF");
                cardColor = Color.parseColor("#FFFFFF");
                textColor = Color.parseColor("#111827");
                subtextColor = Color.parseColor("#4C1D95");
                primaryBtnColor = Color.parseColor("#7C3AED");
                secondaryBtnColor = Color.parseColor("#10B981");
                break;
                
            case 1: // Classic Blue Mode (Default)
            default:
                headerColor = Color.parseColor("#1E3A8A");
                bgColor = Color.parseColor("#F8FAFC");
                cardColor = Color.parseColor("#FFFFFF");
                textColor = Color.parseColor("#111827");
                subtextColor = Color.parseColor("#4B5563");
                primaryBtnColor = Color.parseColor("#1E3A8A");
                secondaryBtnColor = Color.parseColor("#10B981");
                break;
        }
        
        // Root container and header
        View mainRoot = findViewById(R.id.mainRootLayout);
        if (mainRoot != null) mainRoot.setBackgroundColor(bgColor);
        
        View header = findViewById(R.id.headerBar);
        if (header != null) header.setBackgroundColor(headerColor);
        
        // Dynamically skin and redraw card backgrounds preserving borders and radii
        int[] cardIds = new int[]{R.id.cardInput, R.id.cardResize, R.id.cardPreview, R.id.cardEnhancer, R.id.themeCard};
        float density = getResources().getDisplayMetrics().density;
        int strokeWidth = Math.round(1 * density);
        int cornerRadius = Math.round(12 * density);
        
        for (int id : cardIds) {
            View cardView = findViewById(id);
            if (cardView != null) {
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(cardColor);
                gd.setCornerRadius(cornerRadius);
                gd.setStroke(strokeWidth, Color.parseColor(themeId == 2 ? "#334155" : "#E2E8F0"));
                cardView.setBackground(gd);
            }
        }
        
        // Primary text colors
        int[] primaryTextIds = new int[]{
            R.id.txtHeaderTitle, R.id.lblSelectedPhoto, R.id.lblResizeSettings,
            R.id.lblResizedPreview, R.id.enhancerTitle, R.id.themeTitle
        };
        for (int id : primaryTextIds) {
            TextView tv = (TextView) findViewById(id);
            if (tv != null) {
                if (id == R.id.txtHeaderTitle) {
                    tv.setTextColor(Color.WHITE);
                } else {
                    tv.setTextColor(textColor);
                }
            }
        }
        
        // Secondary labels text colors
        int[] secondaryTextIds = new int[]{
            R.id.lblWidth, R.id.lblHeight, R.id.originalInfoText, R.id.resizedInfoText,
            R.id.lblBrightness, R.id.lblContrast, R.id.lblBorderWidth, R.id.lblBorderColor,
            R.id.qualityValText
        };
        for (int id : secondaryTextIds) {
            TextView tv = (TextView) findViewById(id);
            if (tv != null) {
                tv.setTextColor(subtextColor);
            }
        }
        
        // Apply button tints
        int[] primaryButtons = new int[]{
            R.id.btnSelectPhoto, R.id.btnResize, R.id.btnShare
        };
        for (int id : primaryButtons) {
            Button btn = (Button) findViewById(id);
            if (btn != null && btn.getBackground() != null) {
                btn.getBackground().setColorFilter(primaryBtnColor, PorterDuff.Mode.SRC_IN);
            }
        }
        
        Button btnSaveRef = (Button) findViewById(R.id.btnSave);
        if (btnSaveRef != null && btnSaveRef.getBackground() != null) {
            btnSaveRef.getBackground().setColorFilter(secondaryBtnColor, PorterDuff.Mode.SRC_IN);
        }
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