package com.bhagwatistock.app;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

@SuppressWarnings("deprecation")
public class CameraScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String EXTRA_SCANNED_CODE = "scanned_code";
    public static final String EXTRA_BARCODE_FORMAT = "barcode_format";
    public static final String EXTRA_RAW_OCR_TEXT = "raw_ocr_text";

    private static final String TAG = "CameraScanActivity";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    private MultiFormatReader multiFormatReader;
    private TextRecognizer textRecognizer;

    private boolean isScanningBarcode = true;
    private boolean isProcessingOcr = false;
    private boolean isFlashOn = false;

    private Button btnFlashlight;
    private Button btnUseScannedInfo;
    private TextView tvScanStatus;
    private TextView tvOcrLivePreview;
    private Handler mainHandler;

    private String detectedBarcode = "";
    private String latestRawOcrText = "";
    private MobileModel latestExtractedSpecs = null;

    private long lastOcrTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        mainHandler = new Handler(Looper.getMainLooper());
        multiFormatReader = new MultiFormatReader();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnUseScannedInfo = findViewById(R.id.btnUseScannedInfo);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvOcrLivePreview = findViewById(R.id.tvOcrLivePreview);
        Button btnClose = findViewById(R.id.btnCloseScanner);

        btnFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        btnUseScannedInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnFinalScanResult();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DemoLockManager.checkAndShowLock(this);
        if (surfaceHolder != null && surfaceHolder.getSurface() != null) {
            initCamera(surfaceHolder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DemoLockManager.stopPeriodicTimerCheck();
        releaseCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception ignored) {}
            try {
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(this);
                camera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview", e);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void initCamera(SurfaceHolder holder) {
        if (camera != null) {
            return;
        }
        try {
            camera = Camera.open();
            if (camera != null) {
                camera.setDisplayOrientation(90);
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getSupportedFocusModes() != null &&
                        parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                camera.setParameters(parameters);
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(this);
                camera.startPreview();
            }
        } catch (Exception e) {
            Log.e(TAG, "Camera initialization failed", e);
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
        }
    }

    private void toggleFlashlight() {
        if (camera == null) return;
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFlashModes() == null) return;

            if (isFlashOn) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                btnFlashlight.setText("🔦 Flash OFF");
                isFlashOn = false;
            } else {
                if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                } else if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                }
                btnFlashlight.setText("🔦 Flash ON");
                isFlashOn = true;
            }
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "Flashlight toggle failed", e);
        }
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (data == null || camera == null) return;

        Camera.Parameters parameters;
        try {
            parameters = camera.getParameters();
        } catch (Exception e) {
            return;
        }

        final Camera.Size size = parameters.getPreviewSize();
        if (size == null) return;

        // 1. Barcode Scanning using ZXing
        if (isScanningBarcode && (detectedBarcode == null || detectedBarcode.isEmpty())) {
            try {
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, size.width, size.height, 0, 0, size.width, size.height, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = multiFormatReader.decodeWithState(bitmap);
                if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                    detectedBarcode = result.getText().trim();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvScanStatus.setText("Barcode Found: " + detectedBarcode);
                        }
                    });
                }
            } catch (Exception ignored) {
            } finally {
                multiFormatReader.reset();
            }
        }

        // 2. OCR Text Scanning using ML Kit (throttled to 800ms)
        long now = System.currentTimeMillis();
        if (!isProcessingOcr && (now - lastOcrTime > 800)) {
            isProcessingOcr = true;
            lastOcrTime = now;

            InputImage image = InputImage.fromByteArray(
                    data,
                    size.width,
                    size.height,
                    90,
                    InputImage.IMAGE_FORMAT_NV21
            );

            textRecognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            isProcessingOcr = false;
                            if (visionText != null && visionText.getText() != null && !visionText.getText().trim().isEmpty()) {
                                latestRawOcrText = visionText.getText();
                                latestExtractedSpecs = MobileOcrParser.parseTextToMobile(latestRawOcrText);

                                StringBuilder sb = new StringBuilder();
                                if (latestExtractedSpecs.getBrand() != null && !latestExtractedSpecs.getBrand().isEmpty()) {
                                    sb.append("Brand: ").append(latestExtractedSpecs.getBrand()).append(" ");
                                }
                                if (latestExtractedSpecs.getModel() != null && !latestExtractedSpecs.getModel().isEmpty()) {
                                    sb.append("Model: ").append(latestExtractedSpecs.getModel()).append("\n");
                                }
                                if (latestExtractedSpecs.getImei1() != null && !latestExtractedSpecs.getImei1().isEmpty()) {
                                    sb.append("IMEI 1: ").append(latestExtractedSpecs.getImei1()).append("\n");
                                }
                                if (sb.length() == 0) {
                                    sb.append(latestRawOcrText);
                                }

                                tvOcrLivePreview.setText(sb.toString().trim());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            isProcessingOcr = false;
                        }
                    });
        }
    }

    private void returnFinalScanResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCANNED_CODE, detectedBarcode);
        resultIntent.putExtra(EXTRA_RAW_OCR_TEXT, latestRawOcrText);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}