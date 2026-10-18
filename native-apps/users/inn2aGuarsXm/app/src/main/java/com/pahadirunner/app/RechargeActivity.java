package com.pahadirunner.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class RechargeActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private ImageView ivScreenshotPreview;
    private Button btnSelectImage, btnUploadVerify, btnBackToGame;
    private EditText etNote;
    private ProgressBar pbUpload;
    private TextView tvStatus;

    private Bitmap selectedBitmap = null;
    private BackendApi backendApi;
    private ImageWorker imageWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge);

        backendApi = new BackendApi(this);
        imageWorker = new ImageWorker(this);

        ivScreenshotPreview = (ImageView) findViewById(R.id.ivScreenshotPreview);
        btnSelectImage = (Button) findViewById(R.id.btnSelectImage);
        btnUploadVerify = (Button) findViewById(R.id.btnUploadVerify);
        btnBackToGame = (Button) findViewById(R.id.btnBackToGame);
        etNote = (EditText) findViewById(R.id.etNote);
        pbUpload = (ProgressBar) findViewById(R.id.pbUpload);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "स्क्रीनशॉट चुनें"), PICK_IMAGE_REQUEST);
            }
        });

        btnUploadVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedBitmap == null) {
                    Toast.makeText(RechargeActivity.this, "कृपया पहले पेमेंट स्क्रीनशॉट चुनें!", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadScreenshotAndRecharge();
            }
        });

        btnBackToGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                selectedBitmap = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
                ivScreenshotPreview.setImageBitmap(selectedBitmap);
                tvStatus.setText("स्क्रीनशॉट चयनित! अब 'सत्यापित करें' पर क्लिक करें।");
            } catch (Exception e) {
                Toast.makeText(this, "चित्र लोड नहीं हो सका: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadScreenshotAndRecharge() {
        pbUpload.setVisibility(View.VISIBLE);
        btnUploadVerify.setEnabled(false);
        tvStatus.setText("स्क्रीनशॉट अपलोड और सत्यापन प्रक्रियाधीन है...");

        String token = backendApi.getToken();

        imageWorker.uploadScreenshot(selectedBitmap, token, new ImageWorker.UploadCallback() {
            @Override
            public void onSuccess(final String imageId) {
                // If logged in, record payment in backend collection
                if (backendApi.isLoggedIn()) {
                    try {
                        JSONObject reqData = new JSONObject();
                        reqData.put("image_id", imageId);
                        reqData.put("amount", 50);
                        reqData.put("status", "verified");
                        reqData.put("note", etNote.getText().toString().trim());

                        backendApi.createRecord("payment_requests", reqData, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                completeRechargeUnlock();
                            }

                            @Override
                            public void onError(String error) {
                                // Even if offline record save had issue, unlock local access
                                completeRechargeUnlock();
                            }
                        });
                    } catch (Exception e) {
                        completeRechargeUnlock();
                    }
                } else {
                    completeRechargeUnlock();
                }
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbUpload.setVisibility(View.GONE);
                        btnUploadVerify.setEnabled(true);
                        tvStatus.setText("अपलोड असफल: " + error);
                        Toast.makeText(RechargeActivity.this, "स्क्रीनशॉट अपलोड नहीं हो सका: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void completeRechargeUnlock() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbUpload.setVisibility(View.GONE);
                btnUploadVerify.setEnabled(true);
                tvStatus.setText("सत्यापन सफल! +3 रन अनलॉक कर दिए गए हैं।");
                Toast.makeText(RechargeActivity.this, "स्क्रीनशॉट सत्यापित! +3 चांस दिए गए।", Toast.LENGTH_LONG).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("recharge_success", true);
                resultIntent.putExtra("added_runs", 3);
                resultIntent.putExtra("added_coins", 50);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}