package com.docscanpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_PICK_GALLERY = 102;
    private static final int REQUEST_EDIT_DOC = 103;

    private View screenScan;
    private View screenDocuments;
    private View screenSettings;

    private TextView tvNavScan;
    private TextView tvNavDocs;
    private TextView tvNavSettings;

    private LinearLayout layoutDocumentsList;
    private TextView tvEmptyGallery;
    private EditText etSearchDocs;

    private DocumentStorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storageManager = new DocumentStorageManager(this);

        screenScan = findViewById(R.id.screenScan);
        screenDocuments = findViewById(R.id.screenDocuments);
        screenSettings = findViewById(R.id.screenSettings);

        tvNavScan = (TextView) findViewById(R.id.tvNavScan);
        tvNavDocs = (TextView) findViewById(R.id.tvNavDocs);
        tvNavSettings = (TextView) findViewById(R.id.tvNavSettings);

        layoutDocumentsList = (LinearLayout) findViewById(R.id.layoutDocumentsList);
        tvEmptyGallery = (TextView) findViewById(R.id.tvEmptyGallery);
        etSearchDocs = (EditText) findViewById(R.id.etSearchDocs);

        setupBottomNavigation();
        setupScanScreenActions();
        setupSearchAndSettings();

        renderDocumentsList("");
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navTabScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        findViewById(R.id.navTabDocs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        findViewById(R.id.navTabSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int index) {
        screenScan.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        screenDocuments.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        screenSettings.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        tvNavScan.setTextColor(index == 0 ? 0xFF00E5FF : 0xFF94A3B8);
        tvNavScan.setTypeface(null, index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        tvNavDocs.setTextColor(index == 1 ? 0xFF00E5FF : 0xFF94A3B8);
        tvNavDocs.setTypeface(null, index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        tvNavSettings.setTextColor(index == 2 ? 0xFF00E5FF : 0xFF94A3B8);
        tvNavSettings.setTypeface(null, index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        if (index == 1) {
            renderDocumentsList(etSearchDocs.getText().toString());
        }
    }

    private void setupScanScreenActions() {
        findViewById(R.id.btnLaunchCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(MainActivity.this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btnPickGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(pickIntent, "Select Document Picture"), REQUEST_PICK_GALLERY);
            }
        });
    }

    private void setupSearchAndSettings() {
        etSearchDocs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderDocumentsList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnClearAllDocs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Clear All Documents")
                        .setMessage("Are you sure you want to delete all saved scans and PDFs? This cannot be undone.")
                        .setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                storageManager.clearAll();
                                renderDocumentsList("");
                                Toast.makeText(MainActivity.this, "All documents deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void renderDocumentsList(String query) {
        layoutDocumentsList.removeAllViews();
        List<DocumentItem> list = storageManager.getAllDocuments();
        String lowerQuery = query.toLowerCase().trim();

        int displayedCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < list.size(); i++) {
            final DocumentItem item = list.get(i);
            if (!lowerQuery.isEmpty() && !item.getTitle().toLowerCase().contains(lowerQuery)) {
                continue;
            }

            displayedCount++;
            View itemView = inflater.inflate(R.layout.item_document, layoutDocumentsList, false);

            ImageView ivDocThumb = (ImageView) itemView.findViewById(R.id.ivDocThumb);
            TextView tvDocTitle = (TextView) itemView.findViewById(R.id.tvDocTitle);
            TextView tvDocDate = (TextView) itemView.findViewById(R.id.tvDocDate);
            TextView tvDocSize = (TextView) itemView.findViewById(R.id.tvDocSize);

            tvDocTitle.setText(item.getTitle());
            tvDocDate.setText(item.getDate());
            tvDocSize.setText(item.getFormattedSize());

            if (item.getImagePath() != null && new File(item.getImagePath()).exists()) {
                Bitmap thumb = BitmapFactory.decodeFile(item.getImagePath());
                if (thumb != null) {
                    ivDocThumb.setImageBitmap(thumb);
                }
            }

            // Share action
            itemView.findViewById(R.id.btnShareDoc).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharePdfDocument(item);
                }
            });

            // Delete action
            itemView.findViewById(R.id.btnDeleteDoc).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Document")
                            .setMessage("Delete \"" + item.getTitle() + "\"?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int which) {
                                    storageManager.deleteDocument(item.getId());
                                    renderDocumentsList(etSearchDocs.getText().toString());
                                    Toast.makeText(MainActivity.this, "Document deleted", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            layoutDocumentsList.addView(itemView);
        }

        if (displayedCount == 0) {
            tvEmptyGallery.setVisibility(View.VISIBLE);
        } else {
            tvEmptyGallery.setVisibility(View.GONE);
        }
    }

    private void sharePdfDocument(DocumentItem item) {
        if (item.getPdfPath() == null) return;
        File pdfFile = new File(item.getPdfPath());
        if (!pdfFile.exists()) {
            Toast.makeText(this, "PDF file missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
        startActivity(Intent.createChooser(shareIntent, "Share Scanned PDF"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap capturedBmp = (Bitmap) extras.get("data");
                if (capturedBmp != null) {
                    saveTempAndOpenEditor(capturedBmp);
                }
            }
        } else if (requestCode == REQUEST_PICK_GALLERY && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap pickedBmp = BitmapFactory.decodeStream(inputStream);
                    if (pickedBmp != null) {
                        saveTempAndOpenEditor(pickedBmp);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_EDIT_DOC) {
            // Document saved, switch to Documents gallery tab
            switchTab(1);
        }
    }

    private void saveTempAndOpenEditor(Bitmap bitmap) {
        try {
            File tempFile = new File(getCacheDir(), "temp_scan_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
            fos.close();

            Intent editIntent = new Intent(this, DocumentEditorActivity.class);
            editIntent.putExtra(DocumentEditorActivity.EXTRA_IMAGE_PATH, tempFile.getAbsolutePath());
            startActivityForResult(editIntent, REQUEST_EDIT_DOC);

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing editor", Toast.LENGTH_SHORT).show();
        }
    }
}