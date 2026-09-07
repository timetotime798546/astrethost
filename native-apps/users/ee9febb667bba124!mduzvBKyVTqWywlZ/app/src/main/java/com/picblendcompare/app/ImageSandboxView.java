package com.picblendcompare.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ImageSandboxView extends View {
    private Bitmap bmpOriginalA;
    private Bitmap bmpOriginalB;
    private Bitmap bmpA;
    private Bitmap bmpB;
    
    private int currentMode = 0; // 0: Side-By-Side, 1: Split Slider, 2: Alpha Blend
    private float splitRatio = 0.5f;
    private float blendAlpha = 0.5f;
    private int filterType = 0; // 0: None, 1: Grayscale, 2: Sepia, 3: Invert
    private int rotationAngle = 0; // 0, 90, 180, 270
    private boolean isSwapped = false;
    
    private Paint paint;
    private Paint linePaint;
    
    public ImageSandboxView(Context context) {
        super(context);
        init(context);
    }
    
    public ImageSandboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageSandboxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        
        // Defensive try-catch blocks with broad Throwable catch to avoid runtime crashes due to invalid asset decodes
        try {
            bmpOriginalA = BitmapFactory.decodeResource(context.getResources(), R.drawable.asset_6a9efd3f547c3);
        } catch (Throwable t) {
            bmpOriginalA = null;
        }

        try {
            bmpOriginalB = BitmapFactory.decodeResource(context.getResources(), R.drawable.asset_6a9efd696a0df);
        } catch (Throwable t) {
            bmpOriginalB = null;
        }
        
        // Ensure bitmaps are valid and have dimensions > 0
        if (bmpOriginalA == null || bmpOriginalA.getWidth() <= 0 || bmpOriginalA.getHeight() <= 0) {
            bmpOriginalA = createPlaceholderBitmap(0xFF2C3E50, "Asset A (Original)");
        }
        if (bmpOriginalB == null || bmpOriginalB.getWidth() <= 0 || bmpOriginalB.getHeight() <= 0) {
            bmpOriginalB = createPlaceholderBitmap(0xFF16A085, "Asset B (Original)");
        }
        
        bmpA = bmpOriginalA;
        bmpB = bmpOriginalB;
    }
    
    private Bitmap createPlaceholderBitmap(int color, String text) {
        try {
            Bitmap bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(color);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(24);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, 200, 200, p);
            return bmp;
        } catch (Throwable t) {
            // Absolute fallback empty bitmap if createBitmap fails due to memory pressure or configuration issues
            try {
                return Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
            } catch (Throwable t2) {
                return null;
            }
        }
    }
    
    public void setMode(int mode) {
        this.currentMode = mode;
        invalidate();
    }
    
    public void setSplitRatio(float ratio) {
        this.splitRatio = ratio;
        invalidate();
    }
    
    public void setBlendAlpha(float alpha) {
        this.blendAlpha = alpha;
        invalidate();
    }
    
    public void setFilterType(int type) {
        this.filterType = type;
        applyColorFilter();
        invalidate();
    }
    
    public void rotateImages() {
        rotationAngle = (rotationAngle + 90) % 360;
        invalidate();
    }
    
    public void swapImages() {
        isSwapped = !isSwapped;
        if (isSwapped) {
            bmpA = bmpOriginalB;
            bmpB = bmpOriginalA;
        } else {
            bmpA = bmpOriginalA;
            bmpB = bmpOriginalB;
        }
        invalidate();
    }
    
    public int getRotationAngle() {
        return rotationAngle;
    }
    
    public boolean isSwapped() {
        return isSwapped;
    }
    
    private void applyColorFilter() {
        if (paint == null) return;
        
        if (filterType == 0) {
            paint.setColorFilter(null);
        } else if (filterType == 1) { // Grayscale
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        } else if (filterType == 2) { // Sepia
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrix sepiaMatrix = new ColorMatrix();
            sepiaMatrix.setScale(1f, 0.95f, 0.82f, 1.0f);
            matrix.postConcat(sepiaMatrix);
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        } else if (filterType == 3) { // Invert
            float[] invertMatrix = {
                -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                 0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                 0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                 0.0f,  0.0f,  0.0f, 1.0f,   0.0f
            };
            paint.setColorFilter(new ColorMatrixColorFilter(invertMatrix));
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || bmpA == null || bmpB == null || bmpA.isRecycled() || bmpB.isRecycled()) return;
        
        canvas.save();
        
        // Ensure active filter values are applied to paint before drawing starts
        applyColorFilter();
        
        if (currentMode == 0) {
            // SIDE BY SIDE
            if (w > h) {
                // Landscape Split
                int halfW = w / 2;
                
                canvas.save();
                canvas.clipRect(0, 0, halfW, h);
                drawFitImage(canvas, bmpA, 0, 0, halfW, h);
                canvas.restore();
                
                canvas.drawLine(halfW, 0, halfW, h, linePaint);
                
                canvas.save();
                canvas.clipRect(halfW, 0, w, h);
                drawFitImage(canvas, bmpB, halfW, 0, halfW, h);
                canvas.restore();
            } else {
                // Portrait Split
                int halfH = h / 2;
                
                canvas.save();
                canvas.clipRect(0, 0, w, halfH);
                drawFitImage(canvas, bmpA, 0, 0, w, halfH);
                canvas.restore();
                
                canvas.drawLine(0, halfH, w, halfH, linePaint);
                
                canvas.save();
                canvas.clipRect(0, halfH, w, h);
                drawFitImage(canvas, bmpB, 0, halfH, w, halfH);
                canvas.restore();
            }
        } else if (currentMode == 1) {
            // SPLIT SLIDER
            paint.setAlpha(255);
            drawFitImage(canvas, bmpA, 0, 0, w, h);
            
            canvas.save();
            int splitX = (int) (w * splitRatio);
            canvas.clipRect(0, 0, splitX, h);
            drawFitImage(canvas, bmpB, 0, 0, w, h);
            canvas.restore();
            
            canvas.drawLine(splitX, 0, splitX, h, linePaint);
            
            Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            handlePaint.setColor(Color.WHITE);
            handlePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(splitX, h / 2, 40, handlePaint);
            
            Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(Color.DKGRAY);
            arrowPaint.setStrokeWidth(5);
            arrowPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(splitX, h / 2, 40, arrowPaint);
            
            canvas.drawLine(splitX - 15, h / 2, splitX + 15, h / 2, arrowPaint);
            canvas.drawLine(splitX - 15, h / 2, splitX - 5, h / 2 - 10, arrowPaint);
            canvas.drawLine(splitX - 15, h / 2, splitX - 5, h / 2 + 10, arrowPaint);
            canvas.drawLine(splitX + 15, h / 2, splitX + 5, h / 2 - 10, arrowPaint);
            canvas.drawLine(splitX + 15, h / 2, splitX + 5, h / 2 + 10, arrowPaint);
            
        } else if (currentMode == 2) {
            // ALPHA BLEND
            paint.setAlpha(255);
            drawFitImage(canvas, bmpA, 0, 0, w, h);
            
            int alphaVal = (int) (blendAlpha * 255);
            paint.setAlpha(alphaVal);
            drawFitImage(canvas, bmpB, 0, 0, w, h);
            
            paint.setAlpha(255); // Reset alpha
        }
        
        canvas.restore();
    }
    
    private void drawFitImage(Canvas canvas, Bitmap bitmap, float left, float top, float width, float height) {
        if (bitmap == null || bitmap.isRecycled()) return;
        
        float bmpW = bitmap.getWidth();
        float bmpH = bitmap.getHeight();
        if (bmpW <= 0 || bmpH <= 0) return;
        
        canvas.save();
        
        float centerX = left + width / 2f;
        float centerY = top + height / 2f;
        canvas.translate(centerX, centerY);
        
        if (rotationAngle != 0) {
            canvas.rotate(rotationAngle);
        }
        
        if (rotationAngle == 90 || rotationAngle == 270) {
            float temp = bmpW;
            bmpW = bmpH;
            bmpH = temp;
        }
        
        // Final sanity check before scaling to prevent NaN or division-by-zero crashes
        if (bmpW <= 0 || bmpH <= 0) {
            canvas.restore();
            return;
        }
        
        float scaleX = width / bmpW;
        float scaleY = height / bmpH;
        float cropScale = Math.max(scaleX, scaleY);
        
        canvas.scale(cropScale, cropScale);
        
        float drawL = -bitmap.getWidth() / 2f;
        float drawT = -bitmap.getHeight() / 2f;
        
        canvas.drawBitmap(bitmap, drawL, drawT, paint);
        
        canvas.restore();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMode == 1) {
            float x = event.getX();
            int width = getWidth();
            if (width <= 0) return true;
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    float ratio = x / width;
                    if (ratio < 0) ratio = 0;
                    if (ratio > 1) ratio = 1;
                    splitRatio = ratio;
                    invalidate();
                    
                    if (sliderChangeListener != null) {
                        sliderChangeListener.onSliderChanged(splitRatio);
                    }
                    return true;
            }
        }
        return super.onTouchEvent(event);
    }
    
    public interface OnSliderChangeListener {
        void onSliderChanged(float ratio);
    }
    
    private OnSliderChangeListener sliderChangeListener;
    
    public void setOnSliderChangeListener(OnSliderChangeListener listener) {
        this.sliderChangeListener = listener;
    }
}