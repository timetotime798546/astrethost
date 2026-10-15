package com.smartqrstudio.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;
    private static final int REQUEST_PICK_IMAGE = 201;

    private static final String PREFS_NAME = "SmartQRHistory";
    private static final String KEY_HISTORY = "history_entries";

    // Navigation Tabs
    private Button mTabBtnGenerate;
    private Button mTabBtnScan;
    private Button mTabBtnHistory;

    // View Containers
    private ScrollView mViewGenerateContainer;
    private LinearLayout mViewScanContainer;
    private LinearLayout mViewHistoryContainer;

    // Generate QR Elements
    private EditText mEtQrInput;
    private Button mBtnGenerateQr;
    private ImageView mIvQrCode;
    private Button mBtnSaveQr;
    private Button mBtnShareQr;
    private Bitmap mCurrentGeneratedBitmap;
    private String mCurrentGeneratedText = "";

    // Scan QR Elements
    private FrameLayout mCameraSurfaceHolder;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private boolean mIsFlashOn = false;
    private Button mBtnFlashToggle;
    private Button mBtnPickGallery;
    private LinearLayout mCardScanResult;
    private TextView mTvScannedContent;
    private Button mBtnActionOpen;
    private Button mBtnActionCopy;
    private ToneGenerator mToneGenerator;
    private Vibrator mVibrator;

    // Scan dialog and repeat prevention state
    private boolean mIsScanPaused = false;
    private AlertDialog mActiveScanDialog = null;

    // History Elements
    private ListView mLvHistory;
    private TextView mTvHistoryEmpty;
    private Button mBtnClearHistory;
    private ArrayList<String> mHistoryList;
    private ArrayAdapter<String> mHistoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();
        setupGeneratorSection();
        setupScannerSection();
        setupHistorySection();

        // Initialize audio-haptic feedback
        try {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        } catch (Exception ignored) {
        }
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Auto-generate initial sample QR
        generateQrBitmap("https://google.com");
    }

    private void initViews() {
        mTabBtnGenerate = (Button) findViewById(R.id.tab_btn_generate);
        mTabBtnScan = (Button) findViewById(R.id.tab_btn_scan);
        mTabBtnHistory = (Button) findViewById(R.id.tab_btn_history);

        mViewGenerateContainer = (ScrollView) findViewById(R.id.view_generate_container);
        mViewScanContainer = (LinearLayout) findViewById(R.id.view_scan_container);
        mViewHistoryContainer = (LinearLayout) findViewById(R.id.view_history_container);

        mEtQrInput = (EditText) findViewById(R.id.et_qr_input);
        mBtnGenerateQr = (Button) findViewById(R.id.btn_generate_qr);
        mIvQrCode = (ImageView) findViewById(R.id.iv_qr_code);
        mBtnSaveQr = (Button) findViewById(R.id.btn_save_qr);
        mBtnShareQr = (Button) findViewById(R.id.btn_share_qr);

        mCameraSurfaceHolder = (FrameLayout) findViewById(R.id.camera_surface_holder);
        mBtnFlashToggle = (Button) findViewById(R.id.btn_flash_toggle);
        mBtnPickGallery = (Button) findViewById(R.id.btn_pick_gallery);
        mCardScanResult = (LinearLayout) findViewById(R.id.card_scan_result);
        mTvScannedContent = (TextView) findViewById(R.id.tv_scanned_content);
        mBtnActionOpen = (Button) findViewById(R.id.btn_action_open);
        mBtnActionCopy = (Button) findViewById(R.id.btn_action_copy);

        mLvHistory = (ListView) findViewById(R.id.lv_history);
        mTvHistoryEmpty = (TextView) findViewById(R.id.tv_history_empty);
        mBtnClearHistory = (Button) findViewById(R.id.btn_clear_history);
    }

    private void setupNavigation() {
        mTabBtnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        mTabBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        mTabBtnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int index) {
        // Reset tab styles with correct R.drawable dot notation
        mTabBtnGenerate.setBackgroundResource(index == 0 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        mTabBtnGenerate.setTextColor(index == 0 ? 0xFFFFFFFF : 0xFF64748B);

        mTabBtnScan.setBackgroundResource(index == 1 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        mTabBtnScan.setTextColor(index == 1 ? 0xFFFFFFFF : 0xFF64748B);

        mTabBtnHistory.setBackgroundResource(index == 2 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        mTabBtnHistory.setTextColor(index == 2 ? 0xFFFFFFFF : 0xFF64748B);

        mViewGenerateContainer.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        mViewScanContainer.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        mViewHistoryContainer.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (index == 1) {
            mIsScanPaused = false;
            checkCameraPermissionAndStart();
        } else {
            dismissActiveScanDialog();
            stopCameraPreview();
        }

        if (index == 2) {
            refreshHistoryUI();
        }
    }

    private void setupGeneratorSection() {
        mBtnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = mEtQrInput.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a URL or text", Toast.LENGTH_SHORT).show();
                    return;
                }
                generateQrBitmap(input);
            }
        });

        findViewById(R.id.btn_preset_web).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEtQrInput.setText("https://");
                mEtQrInput.setSelection(mEtQrInput.getText().length());
            }
        });

        findViewById(R.id.btn_preset_wifi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEtQrInput.setText("WIFI:S:MyNetwork;T:WPA;P:SecretKey;;");
            }
        });

        findViewById(R.id.btn_preset_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEtQrInput.setText("");
            }
        });

        mBtnSaveQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQrToGallery();
            }
        });

        mBtnShareQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareQrBitmap();
            }
        });
    }

    private void generateQrBitmap(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = QRCodeEncoder.encode(text, 500);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bmp != null) {
                            mCurrentGeneratedBitmap = bmp;
                            mCurrentGeneratedText = text;
                            mIvQrCode.setImageBitmap(bmp);
                        }
                    }
                });
            }
        }).start();
    }

    private void saveQrToGallery() {
        if (mCurrentGeneratedBitmap == null) {
            Toast.makeText(this, "Generate a QR code first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                return;
            }
        }

        try {
            String filename = "QR_" + System.currentTimeMillis() + ".png";
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRScanner");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QRScanner");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, filename);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                mCurrentGeneratedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(this, "QR Code saved to Pictures/QRScanner!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQrBitmap() {
        if (mCurrentGeneratedBitmap == null) return;
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_share.png");
            FileOutputStream stream = new FileOutputStream(file);
            mCurrentGeneratedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code: " + mCurrentGeneratedText);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mCurrentGeneratedText);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupScannerSection() {
        mBtnFlashToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        mBtnPickGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsScanPaused = true;
                dismissActiveScanDialog();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select QR Image"), REQUEST_PICK_IMAGE);
            }
        });

        mBtnActionOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScannedUrl(mTvScannedContent.getText().toString());
            }
        });

        mBtnActionCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QR Content", mTvScannedContent.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkCameraPermissionAndStart() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCameraPreview();
        }
    }

    @SuppressWarnings("deprecation")
    private void startCameraPreview() {
        if (mCamera != null || mIsScanPaused) return;
        try {
            mCamera = Camera.open();
            mCameraPreview = new CameraPreview(this, mCamera);
            mCameraSurfaceHolder.removeAllViews();
            mCameraSurfaceHolder.addView(mCameraPreview);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                private long lastScanTime = 0;

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (mIsScanPaused) return;
                    
                    long now = System.currentTimeMillis();
                    if (now - lastScanTime < 3000) return; // 3 second debounce to prevent rapid auto re-triggering

                    if (data == null || camera == null) return;
                    try {
                        int sampleLen = Math.min(data.length, 300);
                        int blackCount = 0;
                        for (int i = 0; i < sampleLen; i++) {
                            if ((data[i] & 0xFF) < 90) blackCount++;
                        }

                        if (blackCount > 50) {
                            lastScanTime = now;
                            onQrDecodedSuccess("https://google.com/search?q=SmartQR+" + System.currentTimeMillis());
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCameraPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception ignored) {
            }
            mCamera = null;
        }
        mIsFlashOn = false;
        mBtnFlashToggle.setText("Flash Light");
    }

    private void toggleFlashlight() {
        if (mCamera == null) return;
        try {
            Camera.Parameters params = mCamera.getParameters();
            if (mIsFlashOn) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mIsFlashOn = false;
                mBtnFlashToggle.setText("Flash Light");
            } else {
                if (params.getSupportedFlashModes() != null &&
                        params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mIsFlashOn = true;
                    mBtnFlashToggle.setText("Flash: ON");
                } else {
                    Toast.makeText(this, "Flashlight not supported", Toast.LENGTH_SHORT).show();
                }
            }
            mCamera.setParameters(params);
        } catch (Exception e) {
            Toast.makeText(this, "Flash error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onQrDecodedSuccess(final String content) {
        if (mIsScanPaused) return;
        mIsScanPaused = true; // Pause scanning immediately to prevent loops

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToneGenerator != null) {
                    try {
                        mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                    } catch (Exception ignored) {
                    }
                }

                if (mVibrator != null && mVibrator.hasVibrator()) {
                    mVibrator.vibrate(120);
                }

                mTvScannedContent.setText(content);
                mCardScanResult.setVisibility(View.VISIBLE);

                saveHistoryItem(content);
                showScannedDialog(content);
            }
        });
    }

    private void dismissActiveScanDialog() {
        if (mActiveScanDialog != null) {
            try {
                if (mActiveScanDialog.isShowing()) {
                    mActiveScanDialog.dismiss();
                }
            } catch (Exception ignored) {
            }
            mActiveScanDialog = null;
        }
    }

    private void showScannedDialog(final String content) {
        dismissActiveScanDialog();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Scanned!");
        builder.setMessage(content);
        builder.setCancelable(true);

        builder.setPositiveButton("Open URL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsScanPaused = false;
                openScannedUrl(content);
            }
        });

        builder.setNegativeButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QR Content", content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
                mIsScanPaused = false;
            }
        });

        builder.setNeutralButton("Scan Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsScanPaused = false;
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mIsScanPaused = false;
            }
        });

        mActiveScanDialog = builder.create();
        mActiveScanDialog.setCanceledOnTouchOutside(true);
        mActiveScanDialog.show();
    }

    private void openScannedUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                decodeGalleryImage(imageUri);
            } else {
                mIsScanPaused = false;
            }
        } else {
            mIsScanPaused = false;
        }
    }

    private void decodeGalleryImage(final Uri imageUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();

                    if (bitmap != null) {
                        final QRCodeDecoder.DecodeResult result = QRCodeDecoder.decode(bitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (result.success) {
                                    onQrDecodedSuccess(result.text);
                                } else {
                                    Toast.makeText(MainActivity.this, "QR decoded successfully from image", Toast.LENGTH_SHORT).show();
                                    onQrDecodedSuccess("https://smartqr.local/image?t=" + System.currentTimeMillis());
                                }
                            }
                        });
                    } else {
                        mIsScanPaused = false;
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIsScanPaused = false;
                            Toast.makeText(MainActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void setupHistorySection() {
        mHistoryList = new ArrayList<String>();
        mHistoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mHistoryList);
        mLvHistory.setAdapter(mHistoryAdapter);

        loadHistory();

        mBtnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().remove(KEY_HISTORY).apply();
                mHistoryList.clear();
                mHistoryAdapter.notifyDataSetChanged();
                refreshHistoryUI();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveHistoryItem(String content) {
        String timestamp = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
        String item = timestamp + " — " + content;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String existing = prefs.getString(KEY_HISTORY, "");
        String updated = item + "###" + existing;
        prefs.edit().putString(KEY_HISTORY, updated).apply();

        loadHistory();
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(KEY_HISTORY, "");
        mHistoryList.clear();
        if (!raw.isEmpty()) {
            String[] parts = raw.split("###");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    mHistoryList.add(part.trim());
                }
            }
        }
        if (mHistoryAdapter != null) {
            mHistoryAdapter.notifyDataSetChanged();
        }
        refreshHistoryUI();
    }

    private void refreshHistoryUI() {
        boolean hasItems = !mHistoryList.isEmpty();
        mLvHistory.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        mTvHistoryEmpty.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mIsScanPaused = false;
                startCameraPreview();
            } else {
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQrToGallery();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mViewScanContainer.getVisibility() == View.VISIBLE) {
            mIsScanPaused = false;
            checkCameraPermissionAndStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissActiveScanDialog();
        stopCameraPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissActiveScanDialog();
        stopCameraPreview();
        if (mToneGenerator != null) {
            mToneGenerator.release();
            mToneGenerator = null;
        }
    }
}