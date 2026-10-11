package com.patiententrymanager.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditPatientActivity extends Activity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;

    private EditText etName, etAge, etPhone, etVisitDate, etNotes;
    private Spinner spinnerGender;
    private ImageView ivPatientPhoto;
    private Button btnPickGallery, btnCaptureCamera, btnRemovePhoto, btnSavePatient;
    private ProgressBar progressBar;

    private BackendApi backendApi;
    private ImageWorkerApi imageWorkerApi;

    private Patient editingPatient;
    private Bitmap selectedBitmap;
    private String currentImageId = "";
    private boolean imageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_patient);

        backendApi = new BackendApi(this);
        imageWorkerApi = new ImageWorkerApi(this, backendApi);

        etName = (EditText) findViewById(R.id.etPatientName);
        etAge = (EditText) findViewById(R.id.etPatientAge);
        etPhone = (EditText) findViewById(R.id.etPatientPhone);
        etVisitDate = (EditText) findViewById(R.id.etVisitDate);
        etNotes = (EditText) findViewById(R.id.etPatientNotes);
        spinnerGender = (Spinner) findViewById(R.id.spinnerGender);
        ivPatientPhoto = (ImageView) findViewById(R.id.ivPatientFormPhoto);
        btnPickGallery = (Button) findViewById(R.id.btnPickGallery);
        btnCaptureCamera = (Button) findViewById(R.id.btnCaptureCamera);
        btnRemovePhoto = (Button) findViewById(R.id.btnRemovePhoto);
        btnSavePatient = (Button) findViewById(R.id.btnSavePatient);
        progressBar = (ProgressBar) findViewById(R.id.formProgressBar);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, new String[]{"Male", "Female", "Other"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("patient")) {
            editingPatient = (Patient) intent.getSerializableExtra("patient");
            populateFields(editingPatient);
            setTitle("Edit Patient Record");
        } else {
            setTitle("Add New Patient");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            etVisitDate.setText(today);
        }

        btnPickGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
            }
        });

        btnCaptureCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (captureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(AddEditPatientActivity.this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRemovePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedBitmap = null;
                currentImageId = "";
                imageChanged = true;
                ivPatientPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                btnRemovePhoto.setVisibility(View.GONE);
            }
        });

        btnSavePatient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSave();
            }
        });
    }

    private void populateFields(Patient p) {
        etName.setText(p.getName());
        etAge.setText(String.valueOf(p.getAge()));
        etPhone.setText(p.getPhone());
        etVisitDate.setText(p.getVisitDate());
        etNotes.setText(p.getNotes());
        currentImageId = p.getImageId() != null ? p.getImageId() : "";

        if ("Female".equalsIgnoreCase(p.getGender())) {
            spinnerGender.setSelection(1);
        } else if ("Other".equalsIgnoreCase(p.getGender())) {
            spinnerGender.setSelection(2);
        } else {
            spinnerGender.setSelection(0);
        }

        if (!TextUtils.isEmpty(currentImageId)) {
            btnRemovePhoto.setVisibility(View.VISIBLE);
            setLoading(true);
            imageWorkerApi.fetchImage(currentImageId, new ImageWorkerApi.ImageFetchCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    setLoading(false);
                    ivPatientPhoto.setImageBitmap(bitmap);
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri imageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    ivPatientPhoto.setImageBitmap(selectedBitmap);
                    imageChanged = true;
                    btnRemovePhoto.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load selected image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey("data")) {
                    selectedBitmap = (Bitmap) extras.get("data");
                    ivPatientPhoto.setImageBitmap(selectedBitmap);
                    imageChanged = true;
                    btnRemovePhoto.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String visitDate = etVisitDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Patient name is required");
            return;
        }

        int age = 0;
        if (!TextUtils.isEmpty(ageStr)) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (Exception e) {
                etAge.setError("Invalid age");
                return;
            }
        }

        if (imageChanged && selectedBitmap != null) {
            uploadImageThenSaveRecord(name, age, gender, phone, visitDate, notes);
        } else {
            saveRecordToBackend(name, age, gender, phone, visitDate, notes, currentImageId);
        }
    }

    private void uploadImageThenSaveRecord(final String name, final int age, final String gender,
                                          final String phone, final String visitDate, final String notes) {
        setLoading(true);
        imageWorkerApi.uploadImage(selectedBitmap, new ImageWorkerApi.UploadCallback() {
            @Override
            public void onSuccess(String newImageId) {
                saveRecordToBackend(name, age, gender, phone, visitDate, notes, newImageId);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(AddEditPatientActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveRecordToBackend(String name, int age, String gender, String phone,
                                     String visitDate, String notes, String imageId) {
        setLoading(true);
        JSONObject dataObj = new JSONObject();
        try {
            dataObj.put("name", name);
            dataObj.put("age", age);
            dataObj.put("gender", gender);
            dataObj.put("phone", phone);
            dataObj.put("visit_date", visitDate);
            dataObj.put("notes", notes);
            dataObj.put("image_id", imageId != null ? imageId : "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (editingPatient == null) {
            backendApi.createRecord("patients", dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(AddEditPatientActivity.this, "Patient added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AddEditPatientActivity.this, "Error saving patient: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.updateRecord(editingPatient.getId(), dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(AddEditPatientActivity.this, "Patient record updated!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AddEditPatientActivity.this, "Error updating patient: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSavePatient.setEnabled(!loading);
    }
}