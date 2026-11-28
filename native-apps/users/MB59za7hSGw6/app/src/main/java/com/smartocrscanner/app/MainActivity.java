package com.smartocrscanner.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_PICK_IMAGE = 102;

    private TextView tvTotalScans;
    private TextView tvEmptyRecent;
    private LinearLayout containerRecentScans;
    private FrameLayout layoutLoading;

    private ScanHistoryDbHelper dbHelper;
    private Uri currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ScanHistoryDbHelper(this);

        tvTotalScans = findViewById(R.id.tvTotalScans);
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent);
        containerRecentScans = findViewById(R.id.containerRecentScans);
        layoutLoading = findViewById(R.id.layoutLoading);

        LinearLayout btnCameraScan = findViewById(R.id.btnCameraScan);
        LinearLayout btnGalleryUpload = findViewById(R.id.btnGalleryUpload);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);

        btnCameraScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndLaunch();
            }
        });

        btnGalleryUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryPicker();
            }
        });

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        int total = dbHelper.getScanCount();
        tvTotalScans.setText(String.valueOf(total));

        containerRecentScans.removeAllViews();
        List<ScanModel> scans = dbHelper.getAllScans();

        if (scans.isEmpty()) {
            tvEmptyRecent.setVisibility(View.VISIBLE);
        } else {
            tvEmptyRecent.setVisibility(View.GONE);
            int maxShow = Math.min(scans.size(), 4);
            for (int i = 0; i < maxShow; i++) {
                final ScanModel scan = scans.get(i);
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_history, containerRecentScans, false);

                TextView tvSource = itemView.findViewById(R.id.itemSource);
                TextView tvTimestamp = itemView.findViewById(R.id.itemTimestamp);
                TextView tvSnippet = itemView.findViewById(R.id.itemTextSnippet);
                Button btnCopy = itemView.findViewById(R.id.btnItemCopy);
                Button btnShare = itemView.findViewById(R.id.btnItemShare);
                Button btnDelete = itemView.findViewById(R.id.btnItemDelete);

                tvSource.setText(scan.getSource());
                tvTimestamp.setText(scan.getTimestamp());
                tvSnippet.setText(scan.getText());

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.putExtra("scan_id", scan.getId());
                        startActivity(intent);
                    }
                });

                btnCopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("OCR Text", scan.getText());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                btnShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, scan.getText());
                        startActivity(Intent.createChooser(shareIntent, "Share OCR Text"));
                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteScan(scan.getId());
                        loadDashboardData();
                        Toast.makeText(MainActivity.this, "Scan deleted", Toast.LENGTH_SHORT).show();
                    }
                });

                containerRecentScans.addView(itemView);
            }
        }
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Failed to create temp file for camera capture", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "com.smartocrscanner.app.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera application found on device", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (currentPhotoUri != null) {
                    processImageWithMLKit(currentPhotoUri, "Camera");
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    processImageWithMLKit(selectedImageUri, "Gallery");
                }
            }
        }
    }

    private void processImageWithMLKit(Uri imageUri, final String source) {
        layoutLoading.setVisibility(View.VISIBLE);

        InputImage image;
        try {
            image = InputImage.fromFilePath(this, imageUri);
        } catch (IOException e) {
            layoutLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to read image file", Toast.LENGTH_SHORT).show();
            return;
        }

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        layoutLoading.setVisibility(View.GONE);
                        String resultText = visionText.getText();

                        if (resultText == null || resultText.trim().isEmpty()) {
                            Toast.makeText(MainActivity.this, getString(R.string.no_readable_text), Toast.LENGTH_LONG).show();
                        } else {
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                            long scanId = dbHelper.insertScan(resultText, source, timestamp);

                            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                            intent.putExtra("scan_id", scanId);
                            startActivity(intent);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        layoutLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "OCR Recognition Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}