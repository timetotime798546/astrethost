package com.smartocrscanner.app;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraScanActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String EXTRA_SCANNED_CODE = "scanned_code";
    public static final String EXTRA_BARCODE_FORMAT = "barcode_format";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private MultiFormatReader multiFormatReader;

    private boolean isScanning = true;
    private boolean isFlashOn = false;
    private Button btnFlashlight;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        mainHandler = new Handler(Looper.getMainLooper());
        multiFormatReader = new MultiFormatReader();

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        btnFlashlight = findViewById(R.id.btnFlashlight);
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
        } catch (Exception e) {
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
        if (!isScanning || data == null) {
            return;
        }

        Camera.Size size = camera.getParameters().getPreviewSize();
        int width = size.width;
        int height = size.height;

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            final Result result = multiFormatReader.decodeWithState(bitmap);
            if (result != null && isScanning) {
                isScanning = false;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleScanResult(result);
                    }
                });
            }
        } catch (Exception e) {
            // No barcode detected in current frame, continue scanning
        } finally {
            multiFormatReader.reset();
        }
    }

    private void handleScanResult(Result result) {
        stopCamera();

        Intent intent = new Intent();
        intent.putExtra(EXTRA_SCANNED_CODE, result.getText());
        intent.putExtra(EXTRA_BARCODE_FORMAT, result.getBarcodeFormat().toString());
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