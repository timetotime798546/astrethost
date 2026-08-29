package com.pdfcreatorpro.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class PdfViewerActivity extends Activity {

    private TextView tvTitle;
    private TextView tvPageIndicator;
    private ImageView ivPage;
    private Button btnPrev;
    private Button btnNext;
    private Button btnClose;

    private String pdfPath;
    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer renderer;
    private PdfRenderer.Page currentPage;
    private int pageIndex = 0;
    private int pageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        tvTitle = (TextView) findViewById(R.id.tv_viewer_title);
        tvPageIndicator = (TextView) findViewById(R.id.tv_page_indicator);
        ivPage = (ImageView) findViewById(R.id.iv_pdf_page);
        btnPrev = (Button) findViewById(R.id.btn_prev);
        btnNext = (Button) findViewById(R.id.btn_next);
        btnClose = (Button) findViewById(R.id.btn_viewer_close);

        pdfPath = getIntent().getStringExtra("pdf_path");
        if (pdfPath == null || pdfPath.isEmpty()) {
            Toast.makeText(this, "Error: PDF path not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(pdfPath);
        if (!file.exists()) {
            Toast.makeText(this, "Error: PDF file does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(file.getName());

        try {
            openRenderer(file);
            showPage(0);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to render PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageIndex > 0) {
                    showPage(pageIndex - 1);
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageIndex < pageCount - 1) {
                    showPage(pageIndex + 1);
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void openRenderer(File file) throws Exception {
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        renderer = new PdfRenderer(fileDescriptor);
        pageCount = renderer.getPageCount();
    }

    private void showPage(int index) {
        if (renderer == null || pageCount == 0) return;

        // Close current page if previously open
        if (currentPage != null) {
            currentPage.close();
        }

        pageIndex = index;
        currentPage = renderer.openPage(pageIndex);

        // Draw crisp pixels with standard multiplier factor
        int width = (int) (currentPage.getWidth() * 2.0f);
        int height = (int) (currentPage.getHeight() * 2.0f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        ivPage.setImageBitmap(bitmap);

        // Set display metrics and availability toggles
        tvPageIndicator.setText("Page " + (pageIndex + 1) + " of " + pageCount);
        btnPrev.setEnabled(pageIndex > 0);
        btnNext.setEnabled(pageIndex < pageCount - 1);

        // Apply visuals dynamically matching states
        btnPrev.setAlpha(pageIndex > 0 ? 1.0f : 0.5f);
        btnNext.setAlpha(pageIndex < pageCount - 1 ? 1.0f : 0.5f);
    }

    @Override
    protected void onDestroy() {
        try {
            if (currentPage != null) {
                currentPage.close();
            }
            if (renderer != null) {
                renderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}