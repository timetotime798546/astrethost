package com.photoforgepro.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PhotoCanvasView extends View {

    private Bitmap sourceBitmap;
    private EditState editState = new EditState();
    private boolean splitComparisonMode = false;
    private float splitCurtainPercent = 0.50f; // 0.0 to 1.0
    private boolean isDraggingCurtain = false;

    private Paint basePaint;
    private Paint filteredPaint;
    private Paint linePaint;
    private Paint badgePaint;
    private Paint badgeTextPaint;
    private Paint metaTextPaint;

    private Rect srcRect;
    private RectF dstRect;
    private Matrix drawMatrix;

    // EXIF metadata details
    private String exifIso = "ISO 400";
    private String exifAperture = "f/2.8";
    private String exifShutter = "1/1250s";
    private String exifFocal = "35mm";
    private String exifResolution = "1200 x 900";

    public PhotoCanvasView(Context context) {
        super(context);
        init();
    }

    public PhotoCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        filteredPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(4.0f);

        badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(Color.argb(190, 18, 20, 26));

        badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(26f);
        badgeTextPaint.setFakeBoldText(true);

        metaTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        metaTextPaint.setColor(Color.rgb(0, 170, 255));
        metaTextPaint.setTextSize(22f);

        srcRect = new Rect();
        dstRect = new RectF();
        drawMatrix = new Matrix();

        // Initialize default sample studio asset
        sourceBitmap = ImageProcessor.generateDefaultStudioBitmap();
    }

    public void setSourceBitmap(Bitmap bitmap, String resText) {
        if (bitmap != null) {
            this.sourceBitmap = bitmap;
            this.exifResolution = resText != null ? resText : (bitmap.getWidth() + " x " + bitmap.getHeight());
            invalidate();
        }
    }

    public Bitmap getSourceBitmap() {
        return sourceBitmap;
    }

    public void setEditState(EditState state) {
        this.editState = state;
        updateColorFilter();
        invalidate();
    }

    public void setSplitMode(boolean enabled) {
        this.splitComparisonMode = enabled;
        invalidate();
    }

    public boolean isSplitMode() {
        return splitComparisonMode;
    }

    private void updateColorFilter() {
        ColorMatrix matrix = ImageProcessor.buildColorMatrix(editState);
        filteredPaint.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (sourceBitmap == null || sourceBitmap.isRecycled()) {
            return;
        }

        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW <= 0 || viewH <= 0) return;

        // Calculate aspect ratio fit centered
        int bmpW = sourceBitmap.getWidth();
        int bmpH = sourceBitmap.getHeight();

        float scale = Math.min((float) viewW / bmpW, (float) viewH / bmpH);
        float scaledW = bmpW * scale;
        float scaledH = bmpH * scale;

        float left = (viewW - scaledW) / 2.0f;
        float top = (viewH - scaledH) / 2.0f;
        dstRect.set(left, top, left + scaledW, top + scaledH);
        srcRect.set(0, 0, bmpW, bmpH);

        // Apply rotation if any
        if (editState.rotation != 0) {
            canvas.save();
            canvas.rotate(editState.rotation, viewW / 2.0f, viewH / 2.0f);
        }

        if (!splitComparisonMode) {
            // Render full edited canvas
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, filteredPaint);
            drawVignette(canvas, dstRect);
        } else {
            // Split view mode: Left side Original, Right side Edited
            float curtainX = left + (scaledW * splitCurtainPercent);

            // 1. Draw Left Side: ORIGINAL
            canvas.save();
            canvas.clipRect(left, top, curtainX, top + scaledH);
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, basePaint);
            canvas.restore();

            // 2. Draw Right Side: EDITED
            canvas.save();
            canvas.clipRect(curtainX, top, left + scaledW, top + scaledH);
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, filteredPaint);
            drawVignette(canvas, dstRect);
            canvas.restore();

            // 3. Draw Split Curtain Divider Line & Handle
            canvas.drawLine(curtainX, top, curtainX, top + scaledH, linePaint);

            // Handle knob
            Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            handlePaint.setColor(Color.WHITE);
            canvas.drawCircle(curtainX, top + (scaledH / 2.0f), 24f, handlePaint);

            handlePaint.setColor(Color.rgb(0, 145, 255));
            canvas.drawCircle(curtainX, top + (scaledH / 2.0f), 18f, handlePaint);

            // Left & Right Badges
            canvas.drawRect(left + 16, top + 16, left + 140, top + 60, badgePaint);
            canvas.drawText("BEFORE", left + 28, top + 46, badgeTextPaint);

            canvas.drawRect(left + scaledW - 130, top + 16, left + scaledW - 16, top + 60, badgePaint);
            canvas.drawText("AFTER", left + scaledW - 114, top + 46, badgeTextPaint);
        }

        if (editState.rotation != 0) {
            canvas.restore();
        }

        // Draw Floating EXIF Overlay Badge in Bottom-Left corner
        drawExifOverlay(canvas, left, top + scaledH);
    }

    private void drawVignette(Canvas canvas, RectF bounds) {
        if (editState.vignette > 0.0f) {
            Paint vigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float cx = bounds.centerX();
            float cy = bounds.centerY();
            float radius = (float) Math.hypot(bounds.width() / 2.0f, bounds.height() / 2.0f);
            int darkAlpha = (int) (Math.min(editState.vignette * 2.2f, 220f));

            RadialGradient gradient = new RadialGradient(
                    cx, cy, radius,
                    new int[]{Color.TRANSPARENT, Color.argb(darkAlpha, 0, 0, 0)},
                    new float[]{0.55f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            vigPaint.setShader(gradient);
            canvas.drawRect(bounds, vigPaint);
        }
    }

    private void drawExifOverlay(Canvas canvas, float x, float y) {
        float startX = Math.max(16f, x + 16f);
        float bottomY = Math.min(getHeight() - 16f, y - 16f);

        float boxW = 320f;
        float boxH = 64f;
        float topY = bottomY - boxH;

        RectF bgBox = new RectF(startX, topY, startX + boxW, bottomY);
        canvas.drawRoundRect(bgBox, 12f, 12f, badgePaint);

        String line1 = exifIso + "  ·  " + exifAperture + "  ·  " + exifShutter;
        String line2 = exifFocal + "  ·  " + exifResolution;

        canvas.drawText(line1, startX + 16f, topY + 26f, metaTextPaint);
        canvas.drawText(line2, startX + 16f, topY + 50f, badgeTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!splitComparisonMode) {
            return super.onTouchEvent(event);
        }

        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (dstRect.width() > 0) {
                    float curtainX = dstRect.left + (dstRect.width() * splitCurtainPercent);
                    if (Math.abs(x - curtainX) < 80f) {
                        isDraggingCurtain = true;
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDraggingCurtain && dstRect.width() > 0) {
                    float relativeX = x - dstRect.left;
                    splitCurtainPercent = Math.max(0.05f, Math.min(0.95f, relativeX / dstRect.width()));
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraggingCurtain = false;
                break;
        }

        return super.onTouchEvent(event);
    }
}