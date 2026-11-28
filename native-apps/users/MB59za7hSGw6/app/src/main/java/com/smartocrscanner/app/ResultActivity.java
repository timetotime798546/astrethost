package com.smartocrscanner.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ResultActivity extends Activity {

    private EditText etResultText;
    private TextView tvSourceBadge;
    private TextView tvCharCount;
    private ScanHistoryDbHelper dbHelper;
    private ScanModel currentScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        dbHelper = new ScanHistoryDbHelper(this);

        etResultText = findViewById(R.id.etResultText);
        tvSourceBadge = findViewById(R.id.tvSourceBadge);
        tvCharCount = findViewById(R.id.tvCharCount);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnCopy = findViewById(R.id.btnCopy);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnScanAnother = findViewById(R.id.btnScanAnother);
        Button btnClearText = findViewById(R.id.btnClearText);

        long scanId = getIntent().getLongExtra("scan_id", -1);
        if (scanId != -1) {
            currentScan = dbHelper.getScanById(scanId);
        }

        if (currentScan != null) {
            etResultText.setText(currentScan.getText());
            tvSourceBadge.setText(currentScan.getSource());
            updateCharCount(currentScan.getText());
        } else {
            String fallbackText = getIntent().getStringExtra("scanned_text");
            if (fallbackText != null) {
                etResultText.setText(fallbackText);
                updateCharCount(fallbackText);
            }
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChangesIfAny();
                finish();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etResultText.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(ResultActivity.this, "No text to copy", Toast.LENGTH_SHORT).show();
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("OCR Text", text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ResultActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etResultText.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(ResultActivity.this, "No text to share", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareIntent, "Share OCR Text"));
            }
        });

        btnScanAnother.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChangesIfAny();
                Intent intent = new Intent(ResultActivity.this, AddEditMobileActivity.class);
                intent.putExtra("raw_ocr_text", etResultText.getText().toString());
                startActivity(intent);
                finish();
            }
        });

        btnClearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etResultText.setText("");
                updateCharCount("");
            }
        });
    }

    private void updateCharCount(String text) {
        int length = text != null ? text.length() : 0;
        tvCharCount.setText(length + " characters");
    }

    private void saveChangesIfAny() {
        if (currentScan != null && etResultText != null) {
            String updatedText = etResultText.getText().toString();
            if (!updatedText.equals(currentScan.getText())) {
                dbHelper.updateScanText(currentScan.getId(), updatedText);
            }
        }
    }

    @Override
    public void onBackPressed() {
        saveChangesIfAny();
        super.onBackPressed();
    }
}