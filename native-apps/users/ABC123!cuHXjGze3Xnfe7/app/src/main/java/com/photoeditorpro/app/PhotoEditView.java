package com.photoeditorpro.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class PhotoEditView extends View {

    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private Paint paint = new Paint();

    // Drawing properties
    private boolean isDrawingMode = false;
    private int brushColor = 0xFFFF1744; // Default Red
    private float brushSize = 15f;
    private Path currentPath = new Path();
    private Paint drawPaint;

    public static class Stroke {
        public Path path;
        public int color;
        public float strokeWidth;

        public Stroke(Path path, int color, float strokeWidth) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }

    private ArrayList<Stroke> undoList = new ArrayList<>();
    private ArrayList<Stroke> redoList = new ArrayList<>();

    // Filter properties
    private float brightness = 0f; // range -255 to 255
    private float contrast = 1f;   // range 0.5 to 2.0
    private float[] activeColorMatrix = null;

    public PhotoEditView(Context context) {
        super(context);
        init();
    }

    public PhotoEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhotoEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setOriginalBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        this.originalBitmap = bitmap;
        this.undoList.clear();
        this.redoList.clear();
        this.currentPath.reset();
        updateFilteredBitmap();
    }

    public void setColorFilter(float[] matrix) {
        this.activeColorMatrix = matrix;
        updateFilteredBitmap();
    }

    public void setBrightness(float value) {
        this.brightness = value;
        updateFilteredBitmap();
    }

    public void setContrast(float value) {
        this.contrast = value;
        updateFilteredBitmap();
    }

    public void setDrawingMode(boolean isDrawing) {
        this.isDrawingMode = isDrawing;
    }

    public boolean isDrawingMode() {
        return this.isDrawingMode;
    }

    public void setBrushColor(int color) {
        this.brushColor = color;
    }

    public void setBrushSize(float size) {
        this.brushSize = size;
    }

    public void undo() {
        if (!undoList.isEmpty()) {
            Stroke removed = undoList.remove(undoList.size() - 1);
            redoList.add(removed);
            invalidate();
        }
    }

    public void redo() {
        if (!redoList.isEmpty()) {
            Stroke restored = redoList.remove(redoList.size() - 1);
            undoList.add(restored);
            invalidate();
        }
    }

    public void resetAll() {
        this.brightness = 0f;
        this.contrast = 1f;
        this.activeColorMatrix = null;
        this.undoList.clear();
        this.redoList.clear();
        this.currentPath.reset();
        updateFilteredBitmap();
    }

    public void updateFilteredBitmap() {
        if (originalBitmap == null) return;

        Bitmap bmp = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint filterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorMatrix cm = new ColorMatrix();
        if (activeColorMatrix != null) {
            cm.set(activeColorMatrix);
        }

        // Adjust contrast and brightness: R' = contrast * R + brightness
        ColorMatrix bcMatrix = new ColorMatrix(new float[] {
            contrast, 0, 0, 0, brightness,
            0, contrast, 0, 0, brightness,
            0, 0, contrast, 0, brightness,
            0, 0, 0, 1, 0
        });

        cm.postConcat(bcMatrix);
        filterPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(originalBitmap, 0, 0, filterPaint);

        if (filteredBitmap != null) {
            filteredBitmap.recycle();
        }
        filteredBitmap = bmp;
        invalidate();
    }

    public Bitmap exportEditedBitmap() {
        if (originalBitmap == null) return null;

        Bitmap exportBmp = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(exportBmp);
        Paint filterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorMatrix cm = new ColorMatrix();
        if (activeColorMatrix != null) {
            cm.set(activeColorMatrix);
        }

        ColorMatrix bcMatrix = new ColorMatrix(new float[] {
            contrast, 0, 0, 0, brightness,
            0, contrast, 0, 0, brightness,
            0, 0, contrast, 0, brightness,
            0, 0, 0, 1, 0
        });

        cm.postConcat(bcMatrix);
        filterPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(originalBitmap, 0, 0, filterPaint);

        // Apply path strokes in native coordinate space
        Paint exportDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exportDrawPaint.setStyle(Paint.Style.STROKE);
        exportDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        exportDrawPaint.setStrokeCap(Paint.Cap.ROUND);

        for (Stroke s : undoList) {
            exportDrawPaint.setColor(s.color);
            exportDrawPaint.setStrokeWidth(s.strokeWidth);
            canvas.drawPath(s.path, exportDrawPaint);
        }

        return exportBmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (filteredBitmap == null) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float bmpW = filteredBitmap.getWidth();
        float bmpH = filteredBitmap.getHeight();

        float scale = Math.min(viewW / bmpW, viewH / bmpH);
        float dx = (viewW - bmpW * scale) / 2f;
        float dy = (viewH - bmpH * scale) / 2f;

        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);

        // Draw filtered source bitmap
        canvas.drawBitmap(filteredBitmap, 0, 0, paint);

        // Draw drawn strokes
        for (Stroke s : undoList) {
            drawPaint.setColor(s.color);
            drawPaint.setStrokeWidth(s.strokeWidth);
            canvas.drawPath(s.path, drawPaint);
        }

        // Draw current active touch path
        if (isDrawingMode && !currentPath.isEmpty()) {
            drawPaint.setColor(brushColor);
            drawPaint.setStrokeWidth(brushSize);
            canvas.drawPath(currentPath, drawPaint);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingMode || filteredBitmap == null) {
            return false;
        }

        float touchX = event.getX();
        float touchY = event.getY();

        float viewW = getWidth();
        float viewH = getHeight();
        float bmpW = filteredBitmap.getWidth();
        float bmpH = filteredBitmap.getHeight();

        float scale = Math.min(viewW / bmpW, viewH / bmpH);
        float dx = (viewW - bmpW * scale) / 2f;
        float dy = (viewH - bmpH * scale) / 2f;

        // Map touch position to native pixel position on original image
        float imgX = (touchX - dx) / scale;
        float imgY = (touchY - dy) / scale;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(imgX, imgY);
                redoList.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(imgX, imgY);
                break;
            case MotionEvent.ACTION_UP:
                currentPath.lineTo(imgX, imgY);
                undoList.add(new Stroke(currentPath, brushColor, brushSize));
                currentPath = new Path();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
}