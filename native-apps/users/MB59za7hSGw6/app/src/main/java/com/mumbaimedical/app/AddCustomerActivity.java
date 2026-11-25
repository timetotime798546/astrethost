package com.mumbaimedical.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AddCustomerActivity extends Activity {

    private static final int REQ_CAPTURE_CAMERA = 1001;
    private static final int REQ_PICK_GALLERY = 1002;

    private DatabaseHelper dbHelper;
    private TextView tvTitle, tvPhotoStatus;
    private EditText etCustName, etCustPhone, etCustAddress;
    private ImageView ivCustomerPhoto;
    private Button btnSaveCustomer, btnSelectCustPhoto;
    private long customerId = -1;
    private String photoPath = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        etCustName = findViewById(R.id.etCustName);
        etCustPhone = findViewById(R.id.etCustPhone);
        etCustAddress = findViewById(R.id.etCustAddress);
        ivCustomerPhoto = findViewById(R.id.ivCustomerPhoto);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        btnSelectCustPhoto = findViewById(R.id.btnSelectCustPhoto);
        btnSaveCustomer = findViewById(R.id.btnSaveCustomer);

        if (getIntent().hasExtra("customer_id")) {
            customerId = getIntent().getLongExtra("customer_id", -1);
            tvTitle.setText("Edit Customer Details");
            btnSaveCustomer.setText("Update Customer Info");
            loadCustomerDetails();
        }

        btnSelectCustPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoHelper.showPhotoOptionsDialog(AddCustomerActivity.this, photoPath != null, new PhotoHelper.PhotoPickCallback() {
                    @Override
                    public void onOptionSelected(int option) {
                        if (option == 1) {
                            PhotoHelper.disableStrictModeVmPolicy();
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQ_CAPTURE_CAMERA);
                        } else if (option == 2) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, REQ_PICK_GALLERY);
                        } else if (option == 3) {
                            photoPath = null;
                            ivCustomerPhoto.setVisibility(View.GONE);
                            tvPhotoStatus.setText("No photo attached");
                            btnSelectCustPhoto.setText("Add Photo");
                            Toast.makeText(AddCustomerActivity.this, R.string.photo_removed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ivCustomerPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (photoPath != null) {
                    PhotoHelper.showFullScreenImage(AddCustomerActivity.this, photoPath);
                }
            }
        });

        btnSaveCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });
    }

    private void loadCustomerDetails() {
        Cursor cursor = dbHelper.getCustomerById(customerId);
        if (cursor != null && cursor.moveToFirst()) {
            etCustName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etCustPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            etCustAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            photoPath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            cursor.close();

            if (photoPath != null && !photoPath.isEmpty()) {
                boolean loaded = PhotoHelper.displayThumbnail(this, ivCustomerPhoto, photoPath, 150, 150);
                if (loaded) {
                    tvPhotoStatus.setText(getString(R.string.tap_to_view_full));
                    btnSelectCustPhoto.setText(getString(R.string.btn_change_photo));
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PhotoHelper.REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PhotoHelper.disableStrictModeVmPolicy();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
            if (requestCode == REQ_CAPTURE_CAMERA && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    photoPath = PhotoHelper.saveBitmapImage(this, bitmap, "customers");
                }
            } else if (requestCode == REQ_PICK_GALLERY && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                photoPath = PhotoHelper.saveImageFromUri(this, selectedImageUri, "customers");
            }

            if (photoPath != null) {
                PhotoHelper.displayThumbnail(this, ivCustomerPhoto, photoPath, 150, 150);
                tvPhotoStatus.setText(getString(R.string.tap_to_view_full));
                btnSelectCustPhoto.setText(getString(R.string.btn_change_photo));
            }
        }
    }

    private void saveCustomer() {
        String name = etCustName.getText().toString().trim();
        String phone = etCustPhone.getText().toString().trim();
        String address = etCustAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Customer Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customerId == -1) {
            long newId = dbHelper.addCustomer(name, phone, address, photoPath);
            if (newId > 0) {
                Toast.makeText(this, "Customer Registered Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving customer", Toast.LENGTH_SHORT).show();
            }
        } else {
            boolean updated = dbHelper.updateCustomer(customerId, name, phone, address, photoPath);
            if (updated) {
                Toast.makeText(this, "Customer Details Updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating customer", Toast.LENGTH_SHORT).show();
            }
        }
    }
}