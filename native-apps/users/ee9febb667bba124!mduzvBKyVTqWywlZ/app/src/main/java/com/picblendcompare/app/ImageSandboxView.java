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
import java.util.ArrayList;

public class ImageSandboxView extends View {
    private Bitmap bmpOriginalA;
    private Bitmap bmpOriginalB;
    private Bitmap bmpA;
    private Bitmap bmpB;
    
    private String assetNameA = "Placeholder A";
    private String assetNameB = "Placeholder B";
    
    private int currentMode = 0; // 0: Side-By-Side, 1: Split Slider, 2: Alpha Blend
    private float splitRatio = 0.5f;
    private float blendAlpha = 0.5f;
    private int filterType = 0; // 0: None, 1: Grayscale, 2: Sepia, 3: Invert
    private int rotationAngle = 0; // 0, 90, 180, 270
    private boolean isSwapped = false;
    
    private Paint paint;
    private Paint linePaint;
    private Paint handlePaint;
    private Paint arrowPaint;
    
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
    
    private Bitmap loadBitmap(Context context, String resourceName) {
        try {
            int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
            if (resId != 0) {
                Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
                if (bmp != null && bmp.getWidth() > 0 && bmp.getHeight() > 0) {
                    return bmp;
                }
            }
        } catch (Throwable t) {
            // Safe fallback
        }
        return null;
    }
    
    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.FILL);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.DKGRAY);
        arrowPaint.setStrokeWidth(5f);
        arrowPaint.setStyle(Paint.Style.STROKE);
        
        // Scan for all candidate user-supplied assets in priority order
        String[] candidateNames = {
            "asset_6a9f18319c82d",
            "asset_6a9f1839db93c",
            "asset_6a9efd3f547c3",
            "asset_6a9efd696a0df"
        };
        
        ArrayList<Bitmap> loadedBitmaps = new ArrayList<Bitmap>();
        ArrayList<String> loadedNames = new ArrayList<String>();
        
        for (int i = 0; i < candidateNames.length; i++) {
            Bitmap bmp = loadBitmap(context, candidateNames[i]);
            if (bmp != null) {
                loadedBitmaps.add(bmp);
                loadedNames.add(candidateNames[i] + ".png");
            }
        }
        
        // Assign loaded bitmaps or fall back to high-quality generated placeholders
        if (loadedBitmaps.size() >= 1) {
            bmpOriginalA = loadedBitmaps.get(0);
            assetNameA = loadedNames.get(0);
        } else {
            bmpOriginalA = createPlaceholderBitmap(0xFF2C3E50, "Asset A (Original)");
            assetNameA = "Generated Placeholder A";
        }
        
        if (loadedBitmaps.size() >= 2) {
            bmpOriginalB = loadedBitmaps.get(1);
            assetNameB = loadedNames.get(1);
        } else {
            bmpOriginalB = createPlaceholderBitmap(0xFF16A085, "Asset B (Original)");
            assetNameB = "Generated Placeholder B";
        }
        
        bmpA = bmpOriginalA;
        bmpB = bmpOriginalB;
    }
    
    private Bitmap createPlaceholderBitmap(int color, String text) {
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(color);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(24);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, 200, 200, p);
        } catch (Throwable t) {
            try {
                bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(color);
            } catch (Throwable t2) {
                try {
                    bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                } catch (Throwable t3) {
                    bmp = null;
                }
            }
        }
        return bmp;
    }
    
    public String getAssetNameA() {
        return assetNameA;
    }
    
    public String getAssetNameB() {
        return assetNameB;
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
        
        // Explicitly enforce state reset on Paint to avoid leaks across draw modes
        paint.setAlpha(255);
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
            
            if (handlePaint != null) {
                canvas.drawCircle(splitX, h / 2, 40, handlePaint);
            }
            
            if (arrowPaint != null) {
                canvas.drawCircle(splitX, h / 2, 40, arrowPaint);
                canvas.drawLine(splitX - 15, h / 2, splitX + 15, h / 2, arrowPaint);
                canvas.drawLine(splitX - 15, h / 2, splitX - 5, h / 2 - 10, arrowPaint);
                canvas.drawLine(splitX - 15, h / 2, splitX - 5, h / 2 + 10, arrowPaint);
                canvas.drawLine(splitX + 15, h / 2, splitX + 5, h / 2 - 10, arrowPaint);
                canvas.drawLine(splitX + 15, h / 2, splitX + 5, h / 2 + 10, arrowPaint);
            }
            
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