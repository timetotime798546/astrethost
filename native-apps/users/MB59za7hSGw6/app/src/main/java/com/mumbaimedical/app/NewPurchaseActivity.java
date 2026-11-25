package com.mumbaimedical.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewPurchaseActivity extends Activity {

    private static final int REQ_CAPTURE_CAMERA = 1007;
    private static final int REQ_PICK_GALLERY = 1008;

    private DatabaseHelper dbHelper;
    private EditText etSupplierName, etPurQty, etPurRate, etPurPaidAmount;
    private Spinner spPurMedicines;
    private ImageView ivPurchasePhoto;
    private TextView tvPurPhotoStatus;
    private Button btnSavePurchase, btnSelectPurPhoto;

    private List<Long> medicineIds = new ArrayList<>();
    private List<String> medicineNames = new ArrayList<>();
    private String purchasePhotoPath = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        dbHelper = new DatabaseHelper(this);

        etSupplierName = findViewById(R.id.etSupplierName);
        spPurMedicines = findViewById(R.id.spPurMedicines);
        etPurQty = findViewById(R.id.etPurQty);
        etPurRate = findViewById(R.id.etPurRate);
        etPurPaidAmount = findViewById(R.id.etPurPaidAmount);
        ivPurchasePhoto = findViewById(R.id.ivPurchasePhoto);
        tvPurPhotoStatus = findViewById(R.id.tvPurPhotoStatus);
        btnSelectPurPhoto = findViewById(R.id.btnSelectPurPhoto);
        btnSavePurchase = findViewById(R.id.btnSavePurchase);

        loadMedicineSpinner();

        btnSelectPurPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoHelper.showPhotoOptionsDialog(NewPurchaseActivity.this, purchasePhotoPath != null, new PhotoHelper.PhotoPickCallback() {
                    @Override
                    public void onOptionSelected(int option) {
                        if (option == 1) {
                            Intent intent = PhotoHelper.getCameraCaptureIntent(NewPurchaseActivity.this, "purchases");
                            startActivityForResult(intent, REQ_CAPTURE_CAMERA);
                        } else if (option == 2) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, REQ_PICK_GALLERY);
                        } else if (option == 3) {
                            purchasePhotoPath = null;
                            ivPurchasePhoto.setVisibility(View.GONE);
                            tvPurPhotoStatus.setText("No invoice bill photo attached");
                            btnSelectPurPhoto.setText("Add Photo");
                            Toast.makeText(NewPurchaseActivity.this, R.string.photo_removed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ivPurchasePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (purchasePhotoPath != null) {
                    PhotoHelper.showFullScreenImage(NewPurchaseActivity.this, purchasePhotoPath);
                }
            }
        });

        btnSavePurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePurchase();
            }
        });
    }

    private void loadMedicineSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM " + DatabaseHelper.TABLE_MEDICINES + " ORDER BY name ASC", null);

        medicineIds.clear();
        medicineNames.clear();

        while (cursor.moveToNext()) {
            medicineIds.add(cursor.getLong(0));
            medicineNames.add(cursor.getString(1));
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicineNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPurMedicines.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PhotoHelper.REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = PhotoHelper.getCameraCaptureIntent(this, "purchases");
                startActivityForResult(intent, REQ_CAPTURE_CAMERA);
            } else {
                Toast.makeText(this, R.string.camera_permission_denied_msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_CAPTURE_CAMERA) {
                purchasePhotoPath = PhotoHelper.getCameraPhotoPath(this, "purchases");
                if (purchasePhotoPath == null && data != null && data.getExtras() != null) {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    if (bitmap != null) {
                        purchasePhotoPath = PhotoHelper.saveBitmapImage(this, bitmap, "purchases");
                    }
                }
            } else if (requestCode == REQ_PICK_GALLERY && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                purchasePhotoPath = PhotoHelper.saveImageFromUri(this, selectedImageUri, "purchases");
            }

            if (purchasePhotoPath != null) {
                PhotoHelper.displayThumbnail(this, ivPurchasePhoto, purchasePhotoPath, 150, 150);
                tvPurPhotoStatus.setText(getString(R.string.tap_to_view_full));
                btnSelectPurPhoto.setText(getString(R.string.btn_change_photo));
            }
        }
    }

    private void savePurchase() {
        String supplier = etSupplierName.getText().toString().trim();
        int selectedIndex = spPurMedicines.getSelectedItemPosition();
        String qtyStr = etPurQty.getText().toString().trim();
        String rateStr = etPurRate.getText().toString().trim();
        String paidStr = etPurPaidAmount.getText().toString().trim();

        if (supplier.isEmpty() || selectedIndex < 0 || qtyStr.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        double rate = Double.parseDouble(rateStr);
        double totalAmount = qty * rate;
        double paidAmount = paidStr.isEmpty() ? totalAmount : Double.parseDouble(paidStr);
        long medicineId = medicineIds.get(selectedIndex);

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("purchase_date", date);
            cv.put("supplier_name", supplier);
            cv.put("total_amount", totalAmount);
            cv.put("paid_amount", paidAmount);
            cv.put("payment_status", paidAmount >= totalAmount ? "Paid" : "Pending");
            cv.put("image_path", purchasePhotoPath);

            db.insert(DatabaseHelper.TABLE_PURCHASES, null, cv);

            db.execSQL("UPDATE " + DatabaseHelper.TABLE_MEDICINES + " SET quantity = quantity + " + qty + ", purchase_price = " + rate + " WHERE id = " + medicineId);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Purchase Saved & Stock Updated!", Toast.LENGTH_SHORT).show();
            finish();

        } finally {
            db.endTransaction();
        }
    }
}