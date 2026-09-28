package com.cctvrecordbook.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class CustomerDetailsActivity extends Activity {

    private TextView mTxtName;
    private TextView mTxtPhone;
    private TextView mTxtAddress;
    private TextView mTxtDate;
    private TextView mTxtCameras;
    private TextView mTxtSpecs;
    private TextView mTxtTotal;
    private TextView mTxtReceived;
    private TextView mTxtBalance;

    private Button mBtnCall;
    private Button mBtnEdit;
    private Button mBtnDelete;
    private RelativeLayout mProgressOverlay;

    private Customer mCustomer;
    private BackendApi mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mApi = new BackendApi(this);

        mTxtName = findViewById(R.id.txt_detail_name);
        mTxtPhone = findViewById(R.id.txt_detail_phone);
        mTxtAddress = findViewById(R.id.txt_detail_address);
        mTxtDate = findViewById(R.id.txt_detail_date);
        mTxtCameras = findViewById(R.id.txt_detail_cameras);
        mTxtSpecs = findViewById(R.id.txt_detail_specs);
        mTxtTotal = findViewById(R.id.txt_detail_total);
        mTxtReceived = findViewById(R.id.txt_detail_received);
        mTxtBalance = findViewById(R.id.txt_detail_balance);

        mBtnCall = findViewById(R.id.btn_call_cust);
        mBtnEdit = findViewById(R.id.btn_edit_cust);
        mBtnDelete = findViewById(R.id.btn_delete_cust);
        mProgressOverlay = findViewById(R.id.progress_overlay);

        mCustomer = (Customer) getIntent().getSerializableExtra("customer");
        renderDetails();

        mBtnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + mCustomer.phone));
                startActivity(dialIntent);
            }
        });

        mBtnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editIntent = new Intent(CustomerDetailsActivity.this, AddEditCustomerActivity.class);
                editIntent.putExtra("customer", mCustomer);
                startActivity(editIntent);
                finish(); // Close view detailed after directing to update view
            }
        });

        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void renderDetails() {
        if (mCustomer == null) return;

        mTxtName.setText(mCustomer.name);
        mTxtPhone.setText(mCustomer.phone);
        mTxtAddress.setText(mCustomer.address.isEmpty() ? "No address registered." : mCustomer.address);
        mTxtDate.setText(mCustomer.date);
        mTxtCameras.setText(mCustomer.cameraCount + " CCTV Cameras");
        mTxtSpecs.setText(mCustomer.details.isEmpty() ? "No specifications listed." : mCustomer.details);

        mTxtTotal.setText(String.format("₹%.2f", mCustomer.totalAmount));
        mTxtReceived.setText(String.format("₹%.2f", mCustomer.amountReceived));
        mTxtBalance.setText(String.format("₹%.2f", mCustomer.balance));
    }

    private void confirmDelete() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Delete Record? (डिलीट करें?)");
        b.setMessage("Kya aap sach me Rajesh '" + mCustomer.name + "' ka record delete karna chahte hain? Yeh data cloud se permanent delete ho jayega.");
        b.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                executeDeletion();
            }
        });
        b.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.create().show();
    }

    private void executeDeletion() {
        mProgressOverlay.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject response = mApi.deleteRecord(mCustomer.id);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressOverlay.setVisibility(View.GONE);
                            if (response.optBoolean("success")) {
                                Toast.makeText(CustomerDetailsActivity.this, "Record deleted permanently!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(CustomerDetailsActivity.this, "Delete failed: " + response.optString("error"), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressOverlay.setVisibility(View.GONE);
                            Toast.makeText(CustomerDetailsActivity.this, "Error connecting to cloud: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}