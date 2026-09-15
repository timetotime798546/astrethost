package com.aadhaarcardmaker.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PHOTO_PICKER_REQUEST_CODE = 101;

    // Form inputs
    private EditText inputName;
    private EditText inputAadhaar;
    private EditText inputDob;
    private RadioGroup inputGenderGroup;
    private EditText inputAddress;
    private Button btnSelectPhoto;
    private Button btnDownloadCard;

    // Card preview layouts
    private LinearLayout cardDownloadContainer;
    private ImageView previewPhoto;
    private TextView previewName;
    private TextView previewDob;
    private TextView previewGender;
    private TextView previewAadhaarFront;
    private TextView previewAddress;
    private ImageView previewQrCode;
    private ImageView previewBarcode;
    private TextView previewAadhaarBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind form views
        inputName = (EditText) findViewById(R.id.input_name);
        inputAadhaar = (EditText) findViewById(R.id.input_aadhaar);
        inputDob = (EditText) findViewById(R.id.input_dob);
        inputGenderGroup = (RadioGroup) findViewById(R.id.input_gender_group);
        inputAddress = (EditText) findViewById(R.id.input_address);
        btnSelectPhoto = (Button) findViewById(R.id.btn_select_photo);
        btnDownloadCard = (Button) findViewById(R.id.btn_download_card);

        // Bind preview views
        cardDownloadContainer = (LinearLayout) findViewById(R.id.card_download_container);
        previewPhoto = (ImageView) findViewById(R.id.preview_photo);
        previewName = (TextView) findViewById(R.id.preview_name);
        previewDob = (TextView) findViewById(R.id.preview_dob);
        previewGender = (TextView) findViewById(R.id.preview_gender);
        previewAadhaarFront = (TextView) findViewById(R.id.preview_aadhaar_front);
        previewAddress = (TextView) findViewById(R.id.preview_address);
        previewQrCode = (ImageView) findViewById(R.id.preview_qr_code);
        previewBarcode = (ImageView) findViewById(R.id.preview_barcode);
        previewAadhaarBack = (TextView) findViewById(R.id.preview_aadhaar_back);

        // Generate static barcode & QR code placeholders
        generateMockGraphics();

        // Register action listeners
        setupFormListeners();
    }

    private void generateMockGraphics() {
        try {
            Bitmap qrBitmap = generateMockQRCode();
            Bitmap barcodeBitmap = generateMockBarcode();
            if (qrBitmap != null) {
                previewQrCode.setImageBitmap(qrBitmap);
            }
            if (barcodeBitmap != null) {
                previewBarcode.setImageBitmap(barcodeBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFormListeners() {
        // Name Text Change Listener
        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s.toString().trim();
                previewName.setText(name.isEmpty() ? "Rahul Kumar" : name);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Aadhaar Input Text Formatting & Copy
        inputAadhaar.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) return;
                isUpdating = true;

                // Strip spaces
                String digits = s.toString().replaceAll("[^0-9]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }

                String finalStr = formatted.toString();
                if (finalStr.length() > 14) {
                    finalStr = finalStr.substring(0, 14);
                }

                inputAadhaar.setText(finalStr);
                inputAadhaar.setSelection(finalStr.length());

                String previewNum = finalStr.isEmpty() ? "1234 5678 9012" : finalStr;
                previewAadhaarFront.setText(previewNum);
                previewAadhaarBack.setText(previewNum);

                isUpdating = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Date Picker Trigger Listener
        inputDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Gender Group Listener
        inputGenderGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.gender_male) {
                    previewGender.setText("पुरुष / Male");
                } else if (checkedId == R.id.gender_female) {
                    previewGender.setText("महिला / Female");
                } else if (checkedId == R.id.gender_other) {
                    previewGender.setText("अन्य / Other");
                }
            }
        });

        // Address Input Listener
        inputAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String addr = s.toString().trim();
                previewAddress.setText(addr.isEmpty() ? "S/O: Suresh Kumar, House No. 42, Sector 15, Dwarka, New Delhi - 110075" : addr);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Select Photo Button
        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerPhotoPicker();
            }
        });

        // Download Template Button
        btnDownloadCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                initiateSaveProcess();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 25; // default view 25 years back
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog pickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDayOfMonth) {
                String formattedDay = (selectedDayOfMonth < 10) ? "0" + selectedDayOfMonth : String.valueOf(selectedDayOfMonth);
                int adjustedMonth = selectedMonth + 1;
                String formattedMonth = (adjustedMonth < 10) ? "0" + adjustedMonth : String.valueOf(adjustedMonth);
                
                String dobString = formattedDay + "/" + formattedMonth + "/" + selectedYear;
                inputDob.setText(dobString);
                previewDob.setText(dobString);
            }
        }, year, month, day);

        pickerDialog.show();
    }

    private void triggerPhotoPicker() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            startActivityForResult(pickIntent, PHOTO_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fallbackIntent.setType("image/*");
            startActivityForResult(Intent.createChooser(fallbackIntent, "Select Photo"), PHOTO_PICKER_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 2; // scale down slightly for preview sizing
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, opts);
                    if (bitmap != null) {
                        previewPhoto.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not load selected photo.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initiateSaveProcess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        captureAndSaveTemplate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureAndSaveTemplate();
            } else {
                Toast.makeText(this, "Permission required to save images to older Android versions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void captureAndSaveTemplate() {
        try {
            // Measure & Layout the card container so it renders precisely at its exact dimensions
            cardDownloadContainer.setDrawingCacheEnabled(true);
            Bitmap cardBitmap = Bitmap.createBitmap(cardDownloadContainer.getWidth(), cardDownloadContainer.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(cardBitmap);
            cardDownloadContainer.draw(canvas);
            cardDownloadContainer.setDrawingCacheEnabled(false);

            if (cardBitmap != null) {
                saveImageToExternalStore(cardBitmap);
            } else {
                Toast.makeText(this, "Error: Could not render template bitmap.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Render Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveImageToExternalStore(Bitmap bitmap) {
        String baseFilename = "Aadhaar_Template_" + System.currentTimeMillis() + ".jpg";
        OutputStream outputStream = null;
        Uri imageUri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver contentResolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, baseFilename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/AadhaarTemplates");

                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    outputStream = contentResolver.openOutputStream(imageUri);
                }
            } else {
                java.io.File publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                java.io.File folder = new java.io.File(publicDir, "AadhaarTemplates");
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                java.io.File file = new java.io.File(folder, baseFilename);
                outputStream = new java.io.FileOutputStream(file);
                imageUri = Uri.fromFile(file);
            }

            if (outputStream != null) {
                boolean isCompressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();

                if (isCompressed && imageUri != null) {
                    showCompletionDialog(imageUri);
                } else {
                    Toast.makeText(this, "Compression failed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Unable to establish Storage Output Stream.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving Image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showCompletionDialog(final Uri fileUri) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Saved Successfully! / कार्ड सहेजा गया!");
        builder.setMessage("Your customized Aadhaar Card template has been written successfully to your Pictures directory.");
        
        builder.setPositiveButton("Close / बंद करें", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Share / साझा करें", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpeg");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Share Aadhaar Template Image"));
            }
        });

        builder.show();
    }

    // Dynamic QR Generator (No dependencies)
    private Bitmap generateMockQRCode() {
        int size = 100;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        Random random = new Random(987654321);

        // Standard QR code layout points (top left, top right, bottom left squares)
        canvas.drawRect(0, 0, 25, 25, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(4, 4, 21, 21, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(8, 8, 17, 17, paint);

        canvas.drawRect(75, 0, 100, 25, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(79, 4, 96, 21, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(83, 8, 92, 17, paint);

        canvas.drawRect(0, 75, 25, 100, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(4, 79, 21, 96, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(8, 83, 17, 92, paint);

        // Fill mock bits
        for (int x = 0; x < size; x += 4) {
            for (int y = 0; y < size; y += 4) {
                if ((x < 28 && y < 28) || (x > 72 && y < 28) || (x < 28 && y > 72)) {
                    continue; // Skip placement anchors
                }
                if (random.nextBoolean()) {
                    canvas.drawRect(x, y, x + 4, y + 4, paint);
                }
            }
        }
        return bmp;
    }

    // Dynamic Barcode Generator (No dependencies)
    private Bitmap generateMockBarcode() {
        int width = 300;
        int height = 60;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        Random random = new Random(1234567);
        int cursorX = 15;

        while (cursorX < width - 15) {
            int stripeThickness = random.nextInt(3) + 1; // 1 to 3 width stripes
            paint.setStrokeWidth(stripeThickness);
            canvas.drawLine(cursorX, 0, cursorX, height, paint);
            
            int spacing = random.nextInt(4) + 2; // 2 to 5 width spacing
            cursorX += stripeThickness + spacing;
        }
        return bmp;
    }
}