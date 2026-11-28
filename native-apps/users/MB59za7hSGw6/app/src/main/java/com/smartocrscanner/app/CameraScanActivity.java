package com.smartocrscanner.app;

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
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
            autoFocusCamera();
            Log.d(TAG, "Camera open and preview started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera", e);
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null || camera == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception ignored) {}

        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception e) {
            try {
                camera.startPreview();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    private void autoFocusCamera() {
        if (camera != null) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    camera.setParameters(parameters);
                    camera.autoFocus(null);
                }
            } catch (Exception ignored) {}
        }
    }

    private void toggleFlashlight() {
        if (camera == null) return;

        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFlashModes() == null) {
                Toast.makeText(this, "Flashlight not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFlashOn) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                btnFlashlight.setText("🔦 Flash OFF");
                isFlashOn = false;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                btnFlashlight.setText("💡 Flash ON");
                isFlashOn = true;
            }
            camera.setParameters(parameters);
        } catch (Exception e) {
            Toast.makeText(this, "Flash toggle failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (data == null || camera == null) {
            return;
        }

        Camera.Size size = camera.getParameters().getPreviewSize();
        final int width = size.width;
        final int height = size.height;

        // Process 1: Barcode Scanning via ZXing
        if (isScanningBarcode) {
            try {
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, width, height, 0, 0, width, height, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                final Result result = multiFormatReader.decodeWithState(bitmap);
                if (result != null) {
                    detectedBarcode = result.getText();
                    Log.d(TAG, "Barcode detected via camera: " + detectedBarcode);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvScanStatus.setText("Barcode Detected: " + detectedBarcode);
                        }
                    });
                }
            } catch (Exception ignored) {
            } finally {
                multiFormatReader.reset();
            }
        }

        // Process 2: Live Camera OCR via ML Kit Text Recognition
        if (!isProcessingOcr) {
            isProcessingOcr = true;
            try {
                // Convert NV21 byte array to ML Kit InputImage with 90 degree portrait rotation
                InputImage image = InputImage.fromByteArray(
                        data,
                        width,
                        height,
                        90,
                        InputImage.IMAGE_FORMAT_NV21
                );

                textRecognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            if (visionText != null && visionText.getText() != null) {
                                String rawText = visionText.getText().trim();
                                Log.d(TAG, "ML Kit Live Camera OCR output: " + rawText);
                                if (!rawText.isEmpty()) {
                                    latestRawOcrText = rawText;
                                    latestExtractedSpecs = MobileOcrParser.parseTextToMobile(rawText);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateOcrPreviewUi(latestExtractedSpecs, latestRawOcrText);
                                        }
                                    });
                                }
                            }
                            isProcessingOcr = false;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "ML Kit Live Camera OCR processing error", e);
                            isProcessingOcr = false;
                        }
                    });

            } catch (Exception e) {
                Log.e(TAG, "Failed to build InputImage from camera frame", e);
                isProcessingOcr = false;
            }
        }
    }

    private void updateOcrPreviewUi(MobileModel specs, String rawText) {
        StringBuilder sb = new StringBuilder();
        if (specs != null) {
            if (!specs.getBrand().isEmpty()) sb.append("Brand: ").append(specs.getBrand()).append(" | ");
            if (!specs.getModel().isEmpty()) sb.append("Model: ").append(specs.getModel()).append(" | ");
            if (!specs.getImei1().isEmpty()) sb.append("IMEI1: ").append(specs.getImei1()).append(" | ");
            if (!specs.getImei2().isEmpty()) sb.append("IMEI2: ").append(specs.getImei2()).append(" | ");
            if (!specs.getSerialNumber().isEmpty()) sb.append("S/N: ").append(specs.getSerialNumber()).append(" | ");
            if (!specs.getRam().isEmpty()) sb.append("RAM: ").append(specs.getRam()).append(" | ");
            if (!specs.getStorage().isEmpty()) sb.append("ROM: ").append(specs.getStorage()).append(" | ");
            if (!specs.getColor().isEmpty()) sb.append("Color: ").append(specs.getColor()).append(" | ");
        }

        String previewStr = sb.toString();
        if (previewStr.endsWith(" | ")) {
            previewStr = previewStr.substring(0, previewStr.length() - 3);
        }

        if (!previewStr.isEmpty()) {
            tvScanStatus.setText("Printed Text & Mobile Specs Detected!");
            tvOcrLivePreview.setText(previewStr);
        } else {
            tvScanStatus.setText("Reading printed text via camera ML Kit...");
            tvOcrLivePreview.setText(rawText.length() > 120 ? rawText.substring(0, 120) + "..." : rawText);
        }
    }

    private void returnFinalScanResult() {
        stopCamera();

        Intent intent = new Intent();
        if (!detectedBarcode.isEmpty()) {
            intent.putExtra(EXTRA_SCANNED_CODE, detectedBarcode);
        }
        if (!latestRawOcrText.isEmpty()) {
            intent.putExtra(EXTRA_RAW_OCR_TEXT, latestRawOcrText);
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }
}