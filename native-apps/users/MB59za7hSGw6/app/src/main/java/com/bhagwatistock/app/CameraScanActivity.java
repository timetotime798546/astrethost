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