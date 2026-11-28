package com.smartocrscanner.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int REQUEST_CAMERA_BARCODE = 101;
    private static final int REQUEST_PICK_IMAGE = 102;
    private static final int REQUEST_CREATE_BACKUP = 103;
    private static final int REQUEST_RESTORE_BACKUP = 104;

    private TextView tvTotalMobiles;
    private TextView tvAvailableStock;
    private TextView tvTotalStockIn;
    private TextView tvTotalStockOut;
    private TextView tvEmptyRecent;
    private LinearLayout containerRecentScans;
    private FrameLayout layoutLoading;

    private ScanHistoryDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ScanHistoryDbHelper(this);

        tvTotalMobiles = findViewById(R.id.tvTotalMobiles);
        tvAvailableStock = findViewById(R.id.tvAvailableStock);
        tvTotalStockIn = findViewById(R.id.tvTotalStockIn);
        tvTotalStockOut = findViewById(R.id.tvTotalStockOut);
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent);
        containerRecentScans = findViewById(R.id.containerRecentScans);
        layoutLoading = findViewById(R.id.layoutLoading);

        LinearLayout btnCameraScan = findViewById(R.id.btnCameraScan);
        LinearLayout btnGalleryUpload = findViewById(R.id.btnGalleryUpload);

        Button btnNavAddMobile = findViewById(R.id.btnNavAddMobile);
        Button btnNavStockIn = findViewById(R.id.btnNavStockIn);
        Button btnNavStockOut = findViewById(R.id.btnNavStockOut);
        Button btnNavAllMobiles = findViewById(R.id.btnNavAllMobiles);
        Button btnNavHistory = findViewById(R.id.btnNavHistory);

        Button btnCreateBackup = findViewById(R.id.btnCreateBackup);
        Button btnRestoreBackup = findViewById(R.id.btnRestoreBackup);

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

        btnNavAddMobile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditMobileActivity.class));
            }
        });

        btnNavStockIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StockInActivity.class));
            }
        });

        btnNavStockOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StockOutActivity.class));
            }
        });

        btnNavAllMobiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AllMobilesActivity.class));
            }
        });

        btnNavHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

        btnCreateBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBackupProcess();
            }
        });

        btnRestoreBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAndStartRestoreProcess();
            }
        });

        if (getIntent().getBooleanExtra("trigger_camera", false)) {
            checkCameraPermissionAndLaunch();
        } else if (getIntent().getBooleanExtra("trigger_gallery", false)) {
            openGalleryPicker();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        int total = dbHelper.getMobileCount("ALL");
        int available = dbHelper.getMobileCount("AVAILABLE");
        int sold = dbHelper.getMobileCount("SOLD");

        tvTotalMobiles.setText(String.valueOf(total));
        tvAvailableStock.setText(String.valueOf(available));
        tvTotalStockIn.setText(String.valueOf(total));
        tvTotalStockOut.setText(String.valueOf(sold));

        containerRecentScans.removeAllViews();
        List<MobileModel> mobiles = dbHelper.getAllMobiles("ALL", null, "ALL");

        if (mobiles.isEmpty()) {
            tvEmptyRecent.setVisibility(View.VISIBLE);
        } else {
            tvEmptyRecent.setVisibility(View.GONE);
            int maxShow = Math.min(mobiles.size(), 4);
            for (int i = 0; i < maxShow; i++) {
                final MobileModel m = mobiles.get(i);
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_mobile_stock, containerRecentScans, false);

                TextView tvTitle = itemView.findViewById(R.id.itemMobileTitle);
                TextView tvStatus = itemView.findViewById(R.id.itemMobileStatusBadge);
                TextView tvSpecs = itemView.findViewById(R.id.itemMobileSpecs);
                TextView tvImei = itemView.findViewById(R.id.itemMobileImei);
                TextView tvPrice = itemView.findViewById(R.id.itemMobilePrice);
                TextView tvDate = itemView.findViewById(R.id.itemMobileDate);

                Button btnStockOut = itemView.findViewById(R.id.btnItemStockOutAction);
                Button btnEdit = itemView.findViewById(R.id.btnItemEditMobile);
                Button btnDelete = itemView.findViewById(R.id.btnItemDeleteMobile);

                tvTitle.setText(m.getBrand() + " " + m.getModel());
                tvStatus.setText(m.getStatus());
                if ("SOLD".equals(m.getStatus())) {
                    tvStatus.setBackgroundColor(getResources().getColor(R.color.status_sold));
                    btnStockOut.setVisibility(View.GONE);
                } else {
                    tvStatus.setBackgroundColor(getResources().getColor(R.color.status_green));
                    btnStockOut.setVisibility(View.VISIBLE);
                }

                String specs = (m.getRam().isEmpty() ? "" : m.getRam() + " RAM | ") +
                               (m.getStorage().isEmpty() ? "" : m.getStorage() + " | ") +
                               m.getColor();
                tvSpecs.setText(specs);
                tvImei.setText("IMEI: " + m.getImei1() + (m.getSerialNumber().isEmpty() ? "" : " | S/N: " + m.getSerialNumber()));
                tvPrice.setText("Cost: ₹" + m.getPurchasePrice() + " | Selling: ₹" + m.getSellingPrice());
                tvDate.setText(m.getStockInDate());

                btnStockOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, StockOutActivity.class);
                        intent.putExtra("preselect_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, AddEditMobileActivity.class);
                        intent.putExtra("mobile_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                        dbHelper.deleteMobile(m.getId(), now);
                        loadDashboardData();
                        Toast.makeText(MainActivity.this, "Mobile deleted", Toast.LENGTH_SHORT).show();
                    }
                });

                containerRecentScans.addView(itemView);
            }
        }
    }

    private void startBackupProcess() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date());
        String fileName = "SmartOCR_Backup_" + timestamp + ".zip";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_CREATE_BACKUP);
    }

    private void confirmAndStartRestoreProcess() {
        new AlertDialog.Builder(this)
                .setTitle("Restore Data Backup")
                .setMessage("Restoring a backup will replace your current inventory records, OCR history, and app files with the contents of the ZIP archive. Are you sure you want to proceed?")
                .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                        startActivityForResult(intent, REQUEST_RESTORE_BACKUP);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performBackupToUri(final Uri destinationUri) {
        layoutLoading.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    OutputStream os = getContentResolver().openOutputStream(destinationUri);
                    if (os != null) {
                        success = BackupHelper.createBackup(MainActivity.this, os);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final boolean isSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        layoutLoading.setVisibility(View.GONE);
                        if (isSuccess) {
                            Toast.makeText(MainActivity.this, "Backup ZIP created successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to create backup ZIP archive.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void performRestoreFromUri(final Uri sourceUri) {
        layoutLoading.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    if (dbHelper != null) {
                        dbHelper.close();
                    }
                    InputStream is = getContentResolver().openInputStream(sourceUri);
                    if (is != null) {
                        success = BackupHelper.restoreBackup(MainActivity.this, is);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final boolean isSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper = new ScanHistoryDbHelper(MainActivity.this);
                        layoutLoading.setVisibility(View.GONE);
                        if (isSuccess) {
                            loadDashboardData();
                            Toast.makeText(MainActivity.this, "App data restored successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Restore failed. Selected file is invalid or corrupted.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraScanner();
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
                launchCameraScanner();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchCameraScanner() {
        Intent intent = new Intent(this, CameraScanActivity.class);
        startActivityForResult(intent, REQUEST_CAMERA_BARCODE);
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
            if (requestCode == REQUEST_CAMERA_BARCODE && data != null) {
                String code = data.getStringExtra(CameraScanActivity.EXTRA_SCANNED_CODE);
                String rawOcr = data.getStringExtra(CameraScanActivity.EXTRA_RAW_OCR_TEXT);
                processCameraScanResult(code, rawOcr);
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                processGalleryImage(selectedImageUri);
            } else if (requestCode == REQUEST_CREATE_BACKUP && data != null && data.getData() != null) {
                performBackupToUri(data.getData());
            } else if (requestCode == REQUEST_RESTORE_BACKUP && data != null && data.getData() != null) {
                performRestoreFromUri(data.getData());
            }
        }
    }

    private void processCameraScanResult(String scannedCode, String rawOcrText) {
        if ((scannedCode == null || scannedCode.trim().isEmpty()) && (rawOcrText == null || rawOcrText.trim().isEmpty())) {
            return;
        }

        if (scannedCode != null && !scannedCode.trim().isEmpty()) {
            MobileModel existing = dbHelper.findMobileByIdentifier(scannedCode);
            if (existing != null) {
                Toast.makeText(this, "Existing Product Found by Barcode!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, AddEditMobileActivity.class);
                intent.putExtra("mobile_id", existing.getId());
                startActivity(intent);
                return;
            }
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        if (rawOcrText != null && !rawOcrText.trim().isEmpty()) {
            dbHelper.insertScan(rawOcrText, "Camera OCR", timestamp);
        }

        Intent intent = new Intent(this, AddEditMobileActivity.class);
        if (scannedCode != null && !scannedCode.trim().isEmpty()) {
            intent.putExtra("scanned_barcode", scannedCode);
        }
        if (rawOcrText != null && !rawOcrText.trim().isEmpty()) {
            intent.putExtra("raw_ocr_text", rawOcrText);
        }
        startActivity(intent);
    }

    private void processGalleryImage(Uri imageUri) {
        layoutLoading.setVisibility(View.VISIBLE);

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException ignored) {}

        String barcodeFromImage = null;
        if (bitmap != null) {
            barcodeFromImage = scanBarcodeFromBitmap(bitmap);
        }

        final String detectedBarcode = barcodeFromImage;

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

                        if (detectedBarcode != null && !detectedBarcode.isEmpty()) {
                            MobileModel existing = dbHelper.findMobileByIdentifier(detectedBarcode);
                            if (existing != null) {
                                Toast.makeText(MainActivity.this, "Existing Product Found by Barcode!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(MainActivity.this, AddEditMobileActivity.class);
                                intent.putExtra("mobile_id", existing.getId());
                                startActivity(intent);
                                return;
                            }
                        }

                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                        if (resultText != null && !resultText.trim().isEmpty()) {
                            dbHelper.insertScan(resultText, "Gallery", timestamp);
                        }

                        Intent intent = new Intent(MainActivity.this, AddEditMobileActivity.class);
                        if (detectedBarcode != null && !detectedBarcode.isEmpty()) {
                            intent.putExtra("scanned_barcode", detectedBarcode);
                        }
                        if (resultText != null) {
                            intent.putExtra("raw_ocr_text", resultText);
                        }
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        layoutLoading.setVisibility(View.GONE);
                        if (detectedBarcode != null && !detectedBarcode.isEmpty()) {
                            processCameraScanResult(detectedBarcode, null);
                        } else {
                            Toast.makeText(MainActivity.this, "OCR Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String scanBarcodeFromBitmap(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);
            return result != null ? result.getText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}