package com.pdfcreatorpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText etFilename;
    private EditText etTitle;
    private EditText etAuthor;
    private EditText etContent;
    private Button btnGenerate;
    private LinearLayout listContainer;
    private TextView tvEmptyState;

    private File pdfDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bypass FileUriExposedException for robust native sharing of files
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Bind resources
        etFilename = (EditText) findViewById(R.id.et_filename);
        etTitle = (EditText) findViewById(R.id.et_title);
        etAuthor = (EditText) findViewById(R.id.et_author);
        etContent = (EditText) findViewById(R.id.et_content);
        btnGenerate = (Button) findViewById(R.id.btn_generate);
        listContainer = (LinearLayout) findViewById(R.id.list_container);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);

        // Setup storage directory inside app package standard external area
        pdfDir = new File(getExternalFilesDir(null), "PDF_Documents");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        // Load historical PDFs
        refreshDocumentList();

        // Register generate action listener
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePdfDocument();
            }
        });
    }

    private void generatePdfDocument() {
        String filename = etFilename.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String contentText = etContent.getText().toString().trim();

        if (filename.isEmpty()) {
            etFilename.setError("Filename required");
            return;
        }
        if (title.isEmpty()) {
            etTitle.setError("Title required");
            return;
        }
        if (contentText.isEmpty()) {
            etContent.setError("Content required");
            return;
        }

        // Fix document extension formatting safely
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename = filename + ".pdf";
        }

        // Create PDF
        PdfDocument document = new PdfDocument();

        // A4 Paper specifications (width: 595, height: 842 pt)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        int xPosition = 45;
        int yPosition = 50;

        // Draw PDF header banner color block
        paint.setColor(Color.parseColor("#0288D1"));
        canvas.drawRect(45, 35, 550, 40, paint);

        // Draw Document Title
        paint.setColor(Color.BLACK);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        canvas.drawText(title, xPosition, yPosition + 25, paint);
        yPosition += 45;

        // Draw Author Name and Generation Timestamp
        paint.setColor(Color.parseColor("#666666"));
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        String creatorText = "Author: " + (author.isEmpty() ? "Anonymous" : author);
        String dateText = "Created: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText(creatorText + "  |  " + dateText, xPosition, yPosition, paint);
        yPosition += 15;

        // Draw light separator line
        paint.setColor(Color.parseColor("#CCCCCC"));
        canvas.drawLine(45, yPosition, 550, yPosition, paint);
        yPosition += 35;

        // Draw Multiline content paragraphs with boundary wrapping calculation
        paint.setColor(Color.parseColor("#333333"));
        paint.setTextSize(13);
        
        String[] paragraphs = contentText.split("\n");
        int maximumWidth = 505; // 595 minus 45 left margin and 45 right margin

        for (int pIdx = 0; pIdx < paragraphs.length; pIdx++) {
            String para = paragraphs[pIdx];
            if (para.trim().isEmpty()) {
                yPosition += 15; // Empty paragraph spacing
                continue;
            }

            String[] words = para.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (int wIdx = 0; wIdx < words.length; wIdx++) {
                String word = words[wIdx];
                String testLine = currentLine.toString() + (currentLine.length() == 0 ? "" : " ") + word;
                float calculatedWidth = paint.measureText(testLine);

                if (calculatedWidth > maximumWidth) {
                    // Render filled buffer to canvas
                    canvas.drawText(currentLine.toString(), xPosition, yPosition, paint);
                    yPosition += 18;
                    
                    // Start new segment line
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append((currentLine.length() == 0 ? "" : " ")).append(word);
                }
            }
            
            // Flush remaining inline content buffer
            if (currentLine.length() > 0) {
                canvas.drawText(currentLine.toString(), xPosition, yPosition, paint);
                yPosition += 18;
            }
            
            yPosition += 8; // Small margin after each parsed paragraph
        }

        // Close current page document write buffer
        document.finishPage(page);

        // Write document to application environment storage securely
        File outputFile = new File(pdfDir, filename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            document.writeTo(fos);
            Toast.makeText(this, "Successfully created PDF: " + filename, Toast.LENGTH_LONG).show();
            
            // Clear text entry inputs
            etFilename.setText("");
            etTitle.setText("");
            etAuthor.setText("");
            etContent.setText("");
            
            // Reload list
            refreshDocumentList();
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred saving document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ignored) {} 
            document.close();
        }
    }

    private void refreshDocumentList() {
        // Clear current elements dynamic linear layouts container
        listContainer.removeAllViews();
        
        File[] files = pdfDir.listFiles();
        if (files == null || files.length == 0) {
            tvEmptyState.setVisibility(View.VISIBLE);
            listContainer.addView(tvEmptyState);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        // Order files dynamically so newest created documents appear on peak
        for (int i = files.length - 1; i >= 0; i--) {
            final File pdfFile = files[i];
            if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
                continue;
            }

            // Build structural dynamic rows programmatic cards
            LinearLayout rowCard = new LinearLayout(this);
            rowCard.setOrientation(LinearLayout.VERTICAL);
            rowCard.setBackgroundColor(Color.WHITE);
            rowCard.setPadding(14, 14, 14, 14);
            
            // Setup LayoutParams with programmatic margins
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 12);
            rowCard.setLayoutParams(cardParams);

            // Inside Layout with row columns alignment
            LinearLayout horizontalLayout = new LinearLayout(this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
            horizontalLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Item document title representation
            TextView tvDocName = new TextView(this);
            tvDocName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvDocName.setText(pdfFile.getName());
            tvDocName.setTextColor(Color.parseColor("#212121"));
            tvDocName.setTextSize(14);
            tvDocName.setPadding(0, 0, 8, 0);
            horizontalLayout.addView(tvDocName);

            // Actions controls subpanel wrapper
            LinearLayout actionLayout = new LinearLayout(this);
            actionLayout.setOrientation(LinearLayout.HORIZONTAL);

            // Open Document Action Button
            Button btnOpen = new Button(this);
            btnOpen.setText("Open");
            btnOpen.setPadding(12, 0, 12, 0);
            btnOpen.setTextSize(11);
            btnOpen.setTextColor(Color.WHITE);
            btnOpen.setBackgroundColor(Color.parseColor("#0288D1"));
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 65); 
            btnParams.setMargins(0, 0, 8, 0);
            btnOpen.setLayoutParams(btnParams);
            
            btnOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPdfFile(pdfFile);
                }
            });
            actionLayout.addView(btnOpen);

            // Share Document Action Button
            Button btnShare = new Button(this);
            btnShare.setText("Share");
            btnShare.setPadding(12, 0, 12, 0);
            btnShare.setTextSize(11);
            btnShare.setTextColor(Color.WHITE);
            btnShare.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnShare.setLayoutParams(btnParams);
            
            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharePdfFile(pdfFile);
                }
            });
            actionLayout.addView(btnShare);

            // Delete Document Action Button
            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setPadding(12, 0, 12, 0);
            btnDelete.setTextSize(11);
            btnDelete.setTextColor(Color.WHITE);
            btnDelete.setBackgroundColor(Color.parseColor("#E53935"));
            
            LinearLayout.LayoutParams btnDelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 65);
            btnDelete.setLayoutParams(btnDelParams);
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeletion(pdfFile);
                }
            });
            actionLayout.addView(btnDelete);

            horizontalLayout.addView(actionLayout);
            rowCard.addView(horizontalLayout);
            listContainer.addView(rowCard);
        }
    }

    private void openPdfFile(File file) {
        Uri fileUri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Intent chooserIntent = Intent.createChooser(intent, "Open PDF with:");
        try {
            startActivity(chooserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found capable of displaying PDF files", Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdfFile(File file) {
        Uri fileUri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Sharing Generated PDF: " + file.getName());
        intent.putExtra(Intent.EXTRA_TEXT, "Please find attached file document: " + file.getName());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share PDF document:"));
    }

    private void confirmDeletion(final File file) {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Confirm Action");
        deleteDialog.setMessage("Are you sure you want to delete this PDF file ( " + file.getName() + " ) permanently?");
        deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (file.delete()) {
                    Toast.makeText(MainActivity.this, "File successfully removed", Toast.LENGTH_SHORT).show();
                    refreshDocumentList();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to delete file", Toast.LENGTH_SHORT).show();
                }
            }
        });
        deleteDialog.setNegativeButton("Cancel", null);
        deleteDialog.show();
    }
}