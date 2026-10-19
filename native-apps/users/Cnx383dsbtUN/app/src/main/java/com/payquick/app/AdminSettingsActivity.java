package com.payquick.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class AdminSettingsActivity extends Activity {

    public static final String PREF_ADMIN = "PayQuickAdminPrefs";
    public static final String KEY_MERCHANT_NAME = "merchant_name";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_QR_CODE = "payment_qr_code";
    public static final String KEY_MAX_DEPOSIT = "max_deposit_limit";

    private EditText mEtMerchantName;
    private EditText mEtUpiId;
    private EditText mEtQrCode;
    private EditText mEtMaxDeposit;
    private ImageView mIvPreviewQr;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        mPrefs = getSharedPreferences(PREF_ADMIN, Context.MODE_PRIVATE);

        mEtMerchantName = findViewById(R.id.et_admin_merchant_name);
        mEtUpiId = findViewById(R.id.et_admin_upi_id);
        mEtQrCode = findViewById(R.id.et_admin_qr_url);
        mEtMaxDeposit = findViewById(R.id.et_admin_max_deposit);
        mIvPreviewQr = findViewById(R.id.iv_admin_preview_qr);
        Button btnSave = findViewById(R.id.btn_admin_save);
        Button btnBack = findViewById(R.id.btn_admin_back);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Load current values with defaults defined in admin_setting_schema.json
        String currentMerchant = mPrefs.getString(KEY_MERCHANT_NAME, "PayQuick Super Store");
        String currentUpi = mPrefs.getString(KEY_UPI_ID, "payquick@okhdfcbank");
        String currentQr = mPrefs.getString(KEY_QR_CODE, "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&auto=format&fit=crop&q=80");
        float currentMaxDeposit = mPrefs.getFloat(KEY_MAX_DEPOSIT, 50000.0f);

        mEtMerchantName.setText(currentMerchant);
        mEtUpiId.setText(currentUpi);
        mEtQrCode.setText(currentQr);
        mEtMaxDeposit.setText(String.valueOf((int) currentMaxDeposit));

        ImageLoader.getInstance().displayImage(currentQr, mIvPreviewQr);

        mEtQrCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                ImageLoader.getInstance().displayImage(s.toString().trim(), mIvPreviewQr);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String merchant = mEtMerchantName.getText().toString().trim();
                String upi = mEtUpiId.getText().toString().trim();
                String qr = mEtQrCode.getText().toString().trim();
                String maxDepStr = mEtMaxDeposit.getText().toString().trim();

                float maxDeposit = 50000.0f;
                try {
                    maxDeposit = Float.parseFloat(maxDepStr);
                } catch (Exception ignored) {}

                mPrefs.edit()
                        .putString(KEY_MERCHANT_NAME, merchant)
                        .putString(KEY_UPI_ID, upi)
                        .putString(KEY_QR_CODE, qr)
                        .putFloat(KEY_MAX_DEPOSIT, maxDeposit)
                        .apply();

                Toast.makeText(AdminSettingsActivity.this, "Settings updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}