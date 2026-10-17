package com.docscanpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DocumentEditorActivity extends Activity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    private TouchCanvasView touchCanvasView;
    private TextView tvToolNotice;
    private Button btnToolView;
    private Button btnToolRedactBrush;
    private Button btnToolRedactRect;

    private Bitmap originalBitmap;
    private Bitmap processedBitmap;
    private int currentFilter = ImageProcessingHelper.FILTER_ORIGINAL;
    private float currentRotation = 0f;

    private DocumentStorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        storageManager = new DocumentStorageManager(this);

        touchCanvasView = (TouchCanvasView) findViewById(R.id.touchCanvasView);
        tvToolNotice = (TextView) findViewById(R.id.tvToolNotice);
        btnToolView = (Button) findViewById(R.id.btnToolView);
        btnToolRedactBrush = (Button) findViewById(R.id.btnToolRedactBrush);
        btnToolRedactRect = (Button) findViewById(R.id.btnToolRedactRect);

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath != null && new File(imagePath).exists()) {
            originalBitmap = BitmapFactory.decodeFile(imagePath);
            processedBitmap = originalBitmap;
            touchCanvasView.setBitmap(processedBitmap);
        } else {
            Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btnFilterOriginal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter(ImageProcessingHelper.FILTER_ORIGINAL);
            }
        });

        findViewById(R.id.btnFilterMagic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter(ImageProcessingHelper.FILTER_MAGIC_COLOR);
            }
        });

        findViewById(R.id.btnFilterGray).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter(ImageProcessingHelper.FILTER_GRAYSCALE);
            }
        });

        findViewById(R.id.btnFilterBw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter(ImageProcessingHelper.FILTER_HIGH_CONTRAST_BW);
            }
        });

        findViewById(R.id.btnRotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateImage();
            }
        });

        btnToolView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touchCanvasView.setMode(TouchCanvasView.Mode.NONE);
                updateToolSelection(btnToolView);
                tvToolNotice.setVisibility(View.GONE);
            }
        });

        btnToolRedactBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touchCanvasView.setMode(TouchCanvasView.Mode.REDACT_DRAW);
                updateToolSelection(btnToolRedactBrush);
                tvToolNotice.setText("Draw with finger to black out sensitive text");
                tvToolNotice.setVisibility(View.VISIBLE);
            }
        });

        btnToolRedactRect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touchCanvasView.setMode(TouchCanvasView.Mode.REDACT_RECT);
                updateToolSelection(btnToolRedactRect);
                tvToolNotice.setText("Drag a black rectangle box over sensitive data");
                tvToolNotice.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btnClearRedact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touchCanvasView.clearRedactions();
                Toast.makeText(DocumentEditorActivity.this, "Redactions cleared", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnSavePdf).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSaveDocumentTitle();
            }
        });
    }

    private void updateToolSelection(Button selected) {
        btnToolView.setBackgroundColor(0xFF242B3A);
        btnToolView.setTextColor(0xFFF1F5F9);
        btnToolRedactBrush.setBackgroundColor(0xFF242B3A);
        btnToolRedactBrush.setTextColor(0xFFF1F5F9);
        btnToolRedactRect.setBackgroundColor(0xFF242B3A);
        btnToolRedactRect.setTextColor(0xFFF1F5F9);

        selected.setBackgroundColor(0xFF2E364A);
        selected.setTextColor(0xFF00E5FF);
    }

    private void applyFilter(int filterType) {
        this.currentFilter = filterType;
        Bitmap filtered = ImageProcessingHelper.applyFilter(originalBitmap, filterType);
        if (currentRotation != 0) {
            filtered = ImageProcessingHelper.rotateBitmap(filtered, currentRotation);
        }
        processedBitmap = filtered;
        touchCanvasView.setBitmap(processedBitmap);
    }

    private void rotateImage() {
        currentRotation = (currentRotation + 90f) % 360f;
        applyFilter(currentFilter);
    }

    private void promptSaveDocumentTitle() {
        final EditText input = new EditText(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault());
        String defaultTitle = "Scan_" + sdf.format(new Date());
        input.setText(defaultTitle);
        input.setSelection(defaultTitle.length());
        input.setTextColor(0xFFF1F5F9);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Save Document")
                .setMessage("Enter document name for PDF export:")
                .setView(input)
                .setPositiveButton("Export PDF", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            name = "Document_" + System.currentTimeMillis();
                        }
                        exportAndSave(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void exportAndSave(String docTitle) {
        Bitmap finalBitmap = touchCanvasView.generateComposedBitmap();
        if (finalBitmap == null) {
            Toast.makeText(this, "Failed to render document", Toast.LENGTH_SHORT).show();
            return;
        }

        File docsDir = storageManager.getDocumentsDirectory();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        File savedImageFile = new File(docsDir, "IMG_" + timeStamp + ".png");
        File savedPdfFile = new File(docsDir, "DOC_" + timeStamp + ".pdf");

        try {
            // Save final raster image
            FileOutputStream fos = new FileOutputStream(savedImageFile);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
            fos.close();

            // Export to native PDF
            PdfGenerator.createPdfFromBitmap(finalBitmap, savedPdfFile);

            long totalSize = savedPdfFile.length();
            SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dateStr = dateFmt.format(new Date());

            DocumentItem docItem = new DocumentItem(
                    timeStamp,
                    docTitle,
                    dateStr,
                    savedImageFile.getAbsolutePath(),
                    savedPdfFile.getAbsolutePath(),
                    totalSize
            );

            storageManager.saveDocument(docItem);

            Toast.makeText(this, "PDF Exported Successfully!", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}