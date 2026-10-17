package com.docscanpro.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfGenerator {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    public static final int A4_WIDTH = 595;
    public static final int A4_HEIGHT = 842;

    public static File createPdfFromBitmap(Bitmap bitmap, File destinationFile) throws IOException {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        float bmpWidth = bitmap.getWidth();
        float bmpHeight = bitmap.getHeight();

        // Fit bitmap nicely within page with 20pt margin
        float margin = 20f;
        float usableWidth = A4_WIDTH - (margin * 2);
        float usableHeight = A4_HEIGHT - (margin * 2);

        float scale = Math.min(usableWidth / bmpWidth, usableHeight / bmpHeight);
        float destWidth = bmpWidth * scale;
        float destHeight = bmpHeight * scale;

        float left = margin + (usableWidth - destWidth) / 2.0f;
        float top = margin + (usableHeight - destHeight) / 2.0f;

        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.restore();

        document.finishPage(page);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destinationFile);
            document.writeTo(fos);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
            document.close();
        }

        return destinationFile;
    }
}