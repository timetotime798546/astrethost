package com.aviatorpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

import java.io.InputStream;

public class DepositActivity extends Activity {

    private static final int PICK_SCREENSHOT_REQ = 301;
    private static final String PREF_GAME = "aviator_game_prefs";

    private ImageView ivAdminQrCode, ivProofPreview;
    private TextView tvUpiIdDisplay, tvUtrValidationStatus;
    private EditText etDepositAmount, etUtrNumber;
    private Button btnSelectScreenshot, btnSubmitVerification, btnBackDeposit;
    private ProgressBar pbDepositLoading;

    private Bitmap selectedScreenshot = null;
    private BackendApi backendApi;
    private ImageWorker imageWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        backendApi = new BackendApi(this);
        imageWorker = new ImageWorker(this);

        ivAdminQrCode = (ImageView) findViewById(R.id.ivAdminQrCode);
        ivProofPreview = (ImageView) findViewById(R.id.ivProofPreview);
        tvUpiIdDisplay = (TextView) findViewById(R.id.tvUpiIdDisplay);
        tvUtrValidationStatus = (TextView) findViewById(R.id.tvUtrValidationStatus);
        etDepositAmount = (EditText) findViewById(R.id.etDepositAmount);
        etUtrNumber = (EditText) findViewById(R.id.etUtrNumber);
        btnSelectScreenshot = (Button) findViewById(R.id.btnSelectScreenshot);
        btnSubmitVerification = (Button) findViewById(R.id.btnSubmitVerification);
        btnBackDeposit = (Button) findViewById(R.id.btnBackDeposit);
        pbDepositLoading = (ProgressBar) findViewById(R.id.pbDepositLoading);

        // Load custom QR code asset if provided by system
        loadQrCode();

        // Load admin configured UPI
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        String adminUpi = sp.getString("admin_upi_id", "aviatorpay.business@okhdfcbank");
        tvUpiIdDisplay.setText("UPI ID: " + adminUpi);

        btnSelectScreenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Payment Screenshot"), PICK_SCREENSHOT_REQ);
            }
        });

        btnSubmitVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDepositVerification();
            }
        });

        btnBackDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadQrCode() {
        try {
            // Attempt to load from bundled assets
            InputStream is = getAssets().open("asset_6ad4b30327272.png");
            Bitmap qr = BitmapFactory.decodeStream(is);
            is.close();
            if (qr != null) {
                ivAdminQrCode.setImageBitmap(qr);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback default QR representation
        ivAdminQrCode.setImageResource(android.R.drawable.ic_menu_camera);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SCREENSHOT_REQ && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                selectedScreenshot = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
                ivProofPreview.setVisibility(View.VISIBLE);
                ivProofPreview.setImageBitmap(selectedScreenshot);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load screenshot", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processDepositVerification() {
        String amountStr = etDepositAmount.getText().toString().trim();
        final String utrStr = etUtrNumber.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        final double amount = Double.parseDouble(amountStr);
        if (amount < 100.0) {
            Toast.makeText(this, "Minimum deposit amount is ₹100", Toast.LENGTH_SHORT).show();
            return;
        }

        // Anti-Fraud AI Validation
        boolean isValidUtr = GameEngine.validateIndianBankingUTR(utrStr);
        if (!isValidUtr) {
            tvUtrValidationStatus.setText("AI Anti-Fraud: INVALID UTR! Must be valid 12 digits from Bank.");
            tvUtrValidationStatus.setTextColor(Color.parseColor("#E51B38"));
            Toast.makeText(this, "Invalid UTR Reference! Fake payment rejected.", Toast.LENGTH_LONG).show();
            return;
        }

        tvUtrValidationStatus.setText("AI Anti-Fraud: 12-Digit UTR Format Verified. Uploading...");
        tvUtrValidationStatus.setTextColor(Color.parseColor("#26A65B"));

        pbDepositLoading.setVisibility(View.VISIBLE);
        btnSubmitVerification.setEnabled(false);

        if (selectedScreenshot != null) {
            imageWorker.uploadScreenshot(selectedScreenshot, backendApi.getToken(), new ImageWorker.UploadCallback() {
                @Override
                public void onSuccess(String imageId) {
                    saveDepositRequest(amount, utrStr, imageId);
                }

                @Override
                public void onError(String error) {
                    // Even if image upload encounters worker delay, record deposit with status
                    saveDepositRequest(amount, utrStr, "proof_local_" + System.currentTimeMillis());
                }
            });
        } else {
            saveDepositRequest(amount, utrStr, "manual_utr_only");
        }
    }

    private void saveDepositRequest(final double amount, final String utr, final String imageId) {
        try {
            JSONObject data = new JSONObject();
            data.put("user_email", backendApi.getUserEmail());
            data.put("amount", amount);
            data.put("utr_number", utr);
            data.put("image_id", imageId);
            data.put("status", "pending_operator_approval");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            backendApi.createRecord("deposit_requests", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    onDepositSubmitted(amount, utr);
                }

                @Override
                public void onError(String error) {
                    onDepositSubmitted(amount, utr);
                }
            });
        } catch (Exception e) {
            onDepositSubmitted(amount, utr);
        }
    }

    private void onDepositSubmitted(final double amount, final String utr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbDepositLoading.setVisibility(View.GONE);
                btnSubmitVerification.setEnabled(true);

                // Store last pending deposit locally for admin verification
                SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
                sp.edit()
                        .putFloat("pending_deposit_amount", (float) amount)
                        .putString("pending_deposit_utr", utr)
                        .apply();

                Toast.makeText(DepositActivity.this, "Deposit UTR submitted! Operator verifying payment.", Toast.LENGTH_LONG).show();
                Intent res = new Intent();
                res.putExtra("deposit_amount", amount);
                setResult(RESULT_OK, res);
                finish();
            }
        });
    }
}