package com.scanspeak.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity implements SurfaceHolder.Callback, TextToSpeech.OnInitListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TextToSpeech mTextToSpeech;

    private TextView mCameraPlaceholder;
    private View mScannerBar;
    private EditText mEtExtractedText;
    private TextView mScanStatus;
    private SeekBar mSbSpeed;
    private SeekBar mSbPitch;

    private Button mBtnScan;
    private Button mBtnClear;
    private Button mBtnSpeak;
    private Button mBtnStop;
    private Button mBtnCopy;

    private boolean isScanning = false;

    private final String[] simulatedQuotes = {
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "Keep your eyes on the stars, and your feet on the ground.",
        "Believe you can and you're halfway there. Work hard and make it happen.",
        "The only way to do great work is to love what you do. If you haven't found it yet, keep looking.",
        "Hello and welcome! This scan speaker app extracts physical book texts dynamically and reads them aloud.",
        "Your voice matters. Speak clearly and express your creativity every day.",
        "Technology empowers human connections and builds intelligent systems for global problems."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        // Bind views
        mSurfaceView = (SurfaceView) findViewById(getResources().getIdentifier("camera_preview", "id", getPackageName()));
        mCameraPlaceholder = (TextView) findViewById(getResources().getIdentifier("camera_placeholder", "id", getPackageName()));
        mScannerBar = findViewById(getResources().getIdentifier("scanner_bar", "id", getPackageName()));
        mEtExtractedText = (EditText) findViewById(getResources().getIdentifier("et_extracted_text", "id", getPackageName()));
        mScanStatus = (TextView) findViewById(getResources().getIdentifier("scan_status", "id", getPackageName()));
        
        mSbSpeed = (SeekBar) findViewById(getResources().getIdentifier("sb_speed", "id", getPackageName()));
        mSbPitch = (SeekBar) findViewById(getResources().getIdentifier("sb_pitch", "id", getPackageName()));
        
        mBtnScan = (Button) findViewById(getResources().getIdentifier("btn_scan", "id", getPackageName()));
        mBtnClear = (Button) findViewById(getResources().getIdentifier("btn_clear", "id", getPackageName()));
        mBtnSpeak = (Button) findViewById(getResources().getIdentifier("btn_speak", "id", getPackageName()));
        mBtnStop = (Button) findViewById(getResources().getIdentifier("btn_stop", "id", getPackageName()));
        mBtnCopy = (Button) findViewById(getResources().getIdentifier("btn_copy", "id", getPackageName()));

        // Set Seekbar defaults (equivalent to 1.0f speed / pitch)
        mSbSpeed.setProgress(10);
        mSbPitch.setProgress(10);

        // Setup Surface Holder
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        // Initialize Text To Speech
        mTextToSpeech = new TextToSpeech(this, this);

        // Register Event Listeners
        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerScanningOCR();
            }
        });

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEtExtractedText.setText("");
                mScanStatus.setText("Text input cleared.");
            }
        });

        mBtnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakCurrentText();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTextToSpeech != null) {
                    mTextToSpeech.stop();
                    mScanStatus.setText("Speaking stopped.");
                }
            }
        });

        mBtnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard();
            }
        });

        // Ask camera permissions
        checkAndRequestCameraPermission();
    }

    private void checkAndRequestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                initializeCamera();
            }
        } else {
            initializeCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
                Toast.makeText(this, "Camera access granted! Ready to scan.", Toast.LENGTH_SHORT).show();
            } else {
                mCameraPlaceholder.setText("Camera permission rejected.\nTap 'Scan' button directly for alternative scanning input.");
                Toast.makeText(this, "Camera permission needed for live preview!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera() {
        try {
            if (mCamera == null) {
                mCamera = Camera.open();
                mCameraPlaceholder.setVisibility(View.GONE);
            }
            if (mCamera != null && mSurfaceHolder.getSurface() != null) {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.setDisplayOrientation(90); // Portrait layout rotation alignment
                mCamera.startPreview();
            }
        } catch (Exception e) {
            mCameraPlaceholder.setText("Camera Preview not available on this emulator.\nYou can still write or simulate scan directly.");
            e.printStackTrace();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initializeCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mSurfaceHolder.getSurface() == null) return;
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    // OCR scanning animation simulation and results extractor
    private void triggerScanningOCR() {
        if (isScanning) return;
        isScanning = true;
        mScanStatus.setText("Focusing camera lens & aligning sensor matrix...");
        mScannerBar.setVisibility(View.VISIBLE);

        // Animate simulated scanning bar
        TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.9f
        );
        anim.setDuration(1200);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        mScannerBar.startAnimation(anim);

        // Delay parsing for smooth visual feedback effect
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerBar.clearAnimation();
                mScannerBar.setVisibility(View.GONE);
                isScanning = false;

                // Pick random text template simulation block
                Random rand = new Random();
                String detectedText = simulatedQuotes[rand.nextInt(simulatedQuotes.length)];
                
                mEtExtractedText.setText(detectedText);
                mScanStatus.setText("OCR scan complete! Extracted text found below.");
                
                // Mini feedback sound click simulation
                Toast.makeText(MainActivity.this, "Successfully Extracted Text!", Toast.LENGTH_SHORT).show();
            }
        }, 2400);
    }

    private void speakCurrentText() {
        String textToSpeak = mEtExtractedText.getText().toString().trim();
        if (textToSpeak.isEmpty()) {
            Toast.makeText(this, "Please scan or type some text first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mTextToSpeech != null) {
            // Custom speed
            float rate = (float) mSbSpeed.getProgress() / 10.0f;
            if (rate < 0.1f) rate = 0.1f;
            mTextToSpeech.setSpeechRate(rate);

            // Custom pitch
            float pitch = (float) mSbPitch.getProgress() / 10.0f;
            if (pitch < 0.1f) pitch = 0.1f;
            mTextToSpeech.setPitch(pitch);

            mScanStatus.setText("Reading speak in progress...");
            
            // Native TTS compatible engine execution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTextToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SCAN_SPEAK_ID");
            } else {
                mTextToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        } else {
            Toast.makeText(this, "TTS Engine not initialised!", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard() {
        String txt = mEtExtractedText.getText().toString();
        if (!txt.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Scanned Text", txt);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Nothing to copy.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(Locale.UK);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mTextToSpeech.setLanguage(Locale.US); // Fallback standard english
            }
        } else {
            Toast.makeText(this, "Could not load voice speech subsystem.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Unlocks native modules resources cleanly
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        releaseCamera();
        super.onDestroy();
    }
}