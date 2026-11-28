package com.smartocrscanner.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditMobileActivity extends Activity {

    private static final int REQ_CAMERA_PERM = 200;
    private static final int REQ_CAMERA_SCANNER = 201;
    private static final int REQ_GALLERY_PICK = 202;

    private TextView tvFormTitle;
    private EditText etBrand, etModel, etModelNumber, etImei1, etImei2, etSerialNumber;
    private EditText etRam, etStorage, etColor, etProductCode, etPurchasePrice, etSellingPrice, etNotes, etRawOcr;

    private ScanHistoryDbHelper dbHelper;
    private long editingMobileId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_mobile);

        dbHelper = new ScanHistoryDbHelper(this);

        tvFormTitle = findViewById(R.id.tvFormTitle);
        etBrand = findViewById(R.id.etBrand);
        etModel = findViewById(R.id.etModel);
        etModelNumber = findViewById(R.id.etModelNumber);
        etImei1 = findViewById(R.id.etImei1);
        etImei2 = findViewById(R.id.etImei2);
        etSerialNumber = findViewById(R.id.etSerialNumber);
        etRam = findViewById(R.id.etRam);
        etStorage = findViewById(R.id.etStorage);
        etColor = findViewById(R.id.etColor);
        etProductCode = findViewById(R.id.etProductCode);
        etPurchasePrice = findViewById(R.id.etPurchasePrice);
        etSellingPrice = findViewById(R.id.etSellingPrice);
        etNotes = findViewById(R.id.etNotes);
        etRawOcr = findViewById(R.id.etRawOcr);

        Button btnBack = findViewById(R.id.btnBackMobileForm);
        Button btnFormCameraScan = findViewById(R.id.btnFormCameraScan);
        Button btnFormGalleryUpload = findViewById(R.id.btnFormGalleryUpload);
        Button btnFormManual = findViewById(R.id.btnFormManual);
        Button btnSaveMobile = findViewById(R.id.btnSaveMobile);

        editingMobileId = getIntent().getLongExtra("mobile_id", -1);
        if (editingMobileId != -1) {
            tvFormTitle.setText("Edit Mobile Entry");
            loadMobileForEdit(editingMobileId);
        } else {
            String barcodeExtra = getIntent().getStringExtra("scanned_barcode");
            if (barcodeExtra != null && !barcodeExtra.trim().isEmpty()) {
                assignBarcodeToAppropriateField(barcodeExtra.trim());
            }

            String rawText = getIntent().getStringExtra("raw_ocr_text");
            if (rawText != null && !rawText.isEmpty()) {
                populateFieldsFromOcr(rawText);
            }
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnFormCameraScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraAndLaunch();
            }
        });

        btnFormGalleryUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnFormManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AddEditMobileActivity.this, "Manual Entry Mode", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveMobile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMobileEntry();
            }
        });
    }

    private void assignBarcodeToAppropriateField(String barcode) {
        if (barcode.matches("\\d{15}")) {
            if (etImei1.getText().toString().isEmpty()) {
                etImei1.setText(barcode);
            } else if (etImei2.getText().toString().isEmpty()) {
                etImei2.setText(barcode);
            } else {
                etProductCode.setText(barcode);
            }
        } else if (barcode.startsWith("SN") || barcode.startsWith("S/N")) {
            etSerialNumber.setText(barcode);
        } else {
            etProductCode.setText(barcode);
        }
    }

    private void loadMobileForEdit(long id) {
        MobileModel m = dbHelper.getMobileById(id);
        if (m != null) {
            etBrand.setText(m.getBrand());
            etModel.setText(m.getModel());
            etModelNumber.setText(m.getModelNumber());
            etImei1.setText(m.getImei1());
            etImei2.setText(m.getImei2());
            etSerialNumber.setText(m.getSerialNumber());
            etRam.setText(m.getRam());
            etStorage.setText(m.getStorage());
            etColor.setText(m.getColor());
            etProductCode.setText(m.getProductCode());
            etPurchasePrice.setText(m.getPurchasePrice() > 0 ? String.valueOf(m.getPurchasePrice()) : "");
            etSellingPrice.setText(m.getSellingPrice() > 0 ? String.valueOf(m.getSellingPrice()) : "");
            etNotes.setText(m.getNotes());
            etRawOcr.setText(m.getRawOcrText());
        }
    }

    private void populateFieldsFromOcr(String rawText) {
        MobileModel parsed = MobileOcrParser.parseTextToMobile(rawText);

        if (etBrand.getText().toString().isEmpty()) etBrand.setText(parsed.getBrand());
        if (etModel.getText().toString().isEmpty()) etModel.setText(parsed.getModel());
        if (etModelNumber.getText().toString().isEmpty()) etModelNumber.setText(parsed.getModelNumber());
        if (etImei1.getText().toString().isEmpty()) etImei1.setText(parsed.getImei1());
        if (etImei2.getText().toString().isEmpty()) etImei2.setText(parsed.getImei2());
        if (etSerialNumber.getText().toString().isEmpty()) etSerialNumber.setText(parsed.getSerialNumber());
        if (etRam.getText().toString().isEmpty()) etRam.setText(parsed.getRam());
        if (etStorage.getText().toString().isEmpty()) etStorage.setText(parsed.getStorage());
        if (etColor.getText().toString().isEmpty()) etColor.setText(parsed.getColor());
        if (etProductCode.getText().toString().isEmpty()) etProductCode.setText(parsed.getProductCode());

        etRawOcr.setText(rawText);
    }

    private void saveMobileEntry() {
        String brand = etBrand.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String imei1 = etImei1.getText().toString().trim();
        String imei2 = etImei2.getText().toString().trim();
        String serial = etSerialNumber.getText().toString().trim();

        if (brand.isEmpty() && model.isEmpty()) {
            Toast.makeText(this, "Please enter Brand or Model name", Toast.LENGTH_SHORT).show();
            return;
        }

        // IMEI 1 Validation
        if (imei1.isEmpty()) {
            Toast.makeText(this, "IMEI 1 is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imei1.length() != 15 || !imei1.matches("\\d{15}")) {
            Toast.makeText(this, "IMEI 1 must contain exactly 15 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // IMEI 2 Validation
        if (imei2.isEmpty()) {
            Toast.makeText(this, "IMEI 2 is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imei2.length() != 15 || !imei2.matches("\\d{15}")) {
            Toast.makeText(this, "IMEI 2 must contain exactly 15 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Both IMEI numbers must be different
        if (imei1.equals(imei2)) {
            Toast.makeText(this, "IMEI 1 and IMEI 2 must be different", Toast.LENGTH_SHORT).show();
            return;
        }

        // Duplicate IMEI / Serial validation
        if (dbHelper.isImeiOrSerialExists(imei1, imei2, serial, editingMobileId)) {
            Toast.makeText(this, "Error: Mobile with duplicate IMEI or Serial Number already exists in stock!", Toast.LENGTH_LONG).show();
            return;
        }

        double purchasePrice = 0.0;
        try {
            String pStr = etPurchasePrice.getText().toString().trim();
            if (!pStr.isEmpty()) purchasePrice = Double.parseDouble(pStr);
        } catch (Exception ignored) {}

        double sellingPrice = 0.0;
        try {
            String sStr = etSellingPrice.getText().toString().trim();
            if (!sStr.isEmpty()) sellingPrice = Double.parseDouble(sStr);
        } catch (Exception ignored) {}

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        MobileModel mobile = new MobileModel();
        if (editingMobileId != -1) {
            mobile.setId(editingMobileId);
            MobileModel existing = dbHelper.getMobileById(editingMobileId);
            if (existing != null) {
                mobile.setStatus(existing.getStatus());
                mobile.setCustomerName(existing.getCustomerName());
                mobile.setCustomerPhone(existing.getCustomerPhone());
                mobile.setStockInDate(existing.getStockInDate());
                mobile.setStockOutDate(existing.getStockOutDate());
            }
        } else {
            mobile.setStatus("AVAILABLE");
            mobile.setStockInDate(timestamp);
        }

        mobile.setBrand(brand);
        mobile.setModel(model);
        mobile.setModelNumber(etModelNumber.getText().toString().trim());
        mobile.setImei1(imei1);
        mobile.setImei2(imei2);
        mobile.setSerialNumber(serial);
        mobile.setRam(etRam.getText().toString().trim());
        mobile.setStorage(etStorage.getText().toString().trim());
        mobile.setColor(etColor.getText().toString().trim());
        mobile.setProductCode(etProductCode.getText().toString().trim());
        mobile.setPurchasePrice(purchasePrice);
        mobile.setSellingPrice(sellingPrice);
        mobile.setNotes(etNotes.getText().toString().trim());
        mobile.setRawOcrText(etRawOcr.getText().toString().trim());

        if (editingMobileId != -1) {
            dbHelper.updateMobile(mobile, timestamp);
            Toast.makeText(this, "Mobile entry updated", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.insertMobile(mobile);
            Toast.makeText(this, "Mobile added to inventory", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void checkCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, CameraScanActivity.class);
            startActivityForResult(intent, REQ_CAMERA_SCANNER);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_GALLERY_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQ_CAMERA_SCANNER) {
                String code = data.getStringExtra(CameraScanActivity.EXTRA_SCANNED_CODE);
                String rawOcr = data.getStringExtra(CameraScanActivity.EXTRA_RAW_OCR_TEXT);
                
                if (code != null && !code.isEmpty()) {
                    MobileModel existing = dbHelper.findMobileByIdentifier(code);
                    if (existing != null && existing.getId() != editingMobileId) {
                        Toast.makeText(this, "Existing Product Found!", Toast.LENGTH_SHORT).show();
                        loadMobileForEdit(existing.getId());
                        editingMobileId = existing.getId();
                        tvFormTitle.setText("Edit Mobile Entry");
                    } else {
                        assignBarcodeToAppropriateField(code);
                    }
                }
                
                if (rawOcr != null && !rawOcr.isEmpty()) {
                    populateFieldsFromOcr(rawOcr);
                }
            } else if (requestCode == REQ_GALLERY_PICK && data.getData() != null) {
                processOcr(data.getData());
            }
        }
    }

    private void processOcr(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            if (visionText != null && visionText.getText() != null) {
                                populateFieldsFromOcr(visionText.getText());
                                Toast.makeText(AddEditMobileActivity.this, "OCR specs extracted successfully", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(AddEditMobileActivity.this, "OCR Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException ignored) {}
    }
}