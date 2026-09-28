package com.cctvrecordbook.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEditCustomerActivity extends Activity {

    private EditText mEtName;
    private EditText mEtPhone;
    private EditText mEtAddress;
    private EditText mEtDate;
    private EditText mEtCameraCount;
    private EditText mEtCameraDetails;
    private EditText mEtTotalAmount;
    private EditText mEtAmountReceived;
    private TextView mTxtCalculatedBalance;
    private Button mBtnSave;
    private RelativeLayout mProgressOverlay;
    private TextView mToolbarTitle;

    private BackendApi mApi;
    private Customer mExistingCustomer = null;
    private boolean mIsEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        mApi = new BackendApi(this);

        mEtName = findViewById(R.id.et_cust_name);
        mEtPhone = findViewById(R.id.et_cust_phone);
        mEtAddress = findViewById(R.id.et_cust_address);
        mEtDate = findViewById(R.id.et_cust_date);
        mEtCameraCount = findViewById(R.id.et_camera_count);
        mEtCameraDetails = findViewById(R.id.et_camera_details);
        mEtTotalAmount = findViewById(R.id.et_total_amount);
        mEtAmountReceived = findViewById(R.id.et_amount_received);
        mTxtCalculatedBalance = findViewById(R.id.txt_calculated_balance);
        mBtnSave = findViewById(R.id.btn_save_record);
        mProgressOverlay = findViewById(R.id.progress_overlay);
        mToolbarTitle = findViewById(R.id.toolbar_title);

        // Prepopulate current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        mEtDate.setText(sdf.format(new Date()));

        // TextWatcher to calculate balance locally in real-time
        TextWatcher calculationsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLocalBalance();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        mEtTotalAmount.addTextChangedListener(calculationsWatcher);
        mEtAmountReceived.addTextChangedListener(calculationsWatcher);

        // Extract editing bundle if any
        if (getIntent().hasExtra("customer")) {
            mExistingCustomer = (Customer) getIntent().getSerializableExtra("customer");
            mIsEditMode = true;
            mToolbarTitle.setText("Edit Customer Record");
            populateFormFields();
        }

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSave();
            }
        });
    }

    private void populateFormFields() {
        mEtName.setText(mExistingCustomer.name);
        mEtPhone.setText(mExistingCustomer.phone);
        mEtAddress.setText(mExistingCustomer.address);
        mEtDate.setText(mExistingCustomer.date);
        mEtCameraCount.setText(String.valueOf(mExistingCustomer.cameraCount));
        mEtCameraDetails.setText(mExistingCustomer.details);
        mEtTotalAmount.setText(String.valueOf(mExistingCustomer.totalAmount));
        mEtAmountReceived.setText(String.valueOf(mExistingCustomer.amountReceived));
        calculateLocalBalance();
    }

    private double calculateLocalBalance() {
        double total = 0;
        double received = 0;
        try {
            total = Double.parseDouble(mEtTotalAmount.getText().toString());
        } catch (Exception e) {}
        try {
            received = Double.parseDouble(mEtAmountReceived.getText().toString());
        } catch (Exception e) {}

        double balance = total - received;
        mTxtCalculatedBalance.setText(String.format("₹%.2f", balance));
        return balance;
    }

    private void validateAndSave() {
        final String name = mEtName.getText().toString().trim();
        final String phone = mEtPhone.getText().toString().trim();
        final String address = mEtAddress.getText().toString().trim();
        final String date = mEtDate.getText().toString().trim();
        final String specs = mEtCameraDetails.getText().toString().trim();
        final String totalStr = mEtTotalAmount.getText().toString().trim();
        final String receivedStr = mEtAmountReceived.getText().toString().trim();
        final String cameraCountStr = mEtCameraCount.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Customer Name aur Phone Number zaroori hain!", Toast.LENGTH_SHORT).show();
            return;
        }

        int cameraCount = 0;
        try {
            cameraCount = Integer.parseInt(cameraCountStr);
        } catch (Exception e) {}

        final double total = totalStr.isEmpty() ? 0.0 : Double.parseDouble(totalStr);
        final double received = receivedStr.isEmpty() ? 0.0 : Double.parseDouble(receivedStr);
        final double localBalance = total - received;
        final int finalCameraCount = cameraCount;

        setLoading(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Rule 11: Call POST /verify with local calculation details prior to persisting
                    JSONObject valuesJson = new JSONObject();
                    valuesJson.put("total_amount", total);
                    valuesJson.put("amount_received", received);

                    JSONObject verifyResult = mApi.verifyLogic("balance", valuesJson, localBalance);
                    final boolean verified = verifyResult.optBoolean("verified", false);

                    if (!verified) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                showVerifyErrorDialog();
                            }
                        });
                        return;
                    }

                    // Prepares final data map
                    final Customer c = new Customer();
                    c.name = name;
                    c.phone = phone;
                    c.address = address;
                    c.date = date;
                    c.cameraCount = finalCameraCount;
                    c.details = specs;
                    c.totalAmount = total;
                    c.amountReceived = received;
                    c.balance = localBalance;

                    final JSONObject saveResponse;
                    if (mIsEditMode) {
                        saveResponse = mApi.updateRecord(mExistingCustomer.id, c.toJsonObject());
                    } else {
                        saveResponse = mApi.createRecord("cctv_customers", c.toJsonObject());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            if (saveResponse.optBoolean("success")) {
                                Toast.makeText(AddEditCustomerActivity.this, "Record saved successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddEditCustomerActivity.this, "Save failed: " + saveResponse.optString("error"), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Toast.makeText(AddEditCustomerActivity.this, "Network/Save operation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showVerifyErrorDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Verification Mismatch");
        b.setMessage("Local balance calculation did not verify with servers core rules. Check input figures.");
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.create().show();
    }

    private void setLoading(boolean loading) {
        mProgressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        mBtnSave.setEnabled(!loading);
    }
}