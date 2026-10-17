package com.docscanpro.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class TouchCanvasView extends View {

    public enum Mode {
        NONE,
        REDACT_DRAW,
        REDACT_RECT
    }

    private Bitmap currentBitmap;
    private Mode mode = Mode.NONE;

    private Paint imagePaint;
    private Paint redactDrawPaint;
    private Paint redactRectPaint;

    private List<Path> redactPaths = new ArrayList<Path>();
    private List<RectF> redactRectangles = new ArrayList<RectF>();

    private Path currentPath = new Path();
    private float startX, startY, currentX, currentY;
    private boolean isDrawingRect = false;

    public TouchCanvasView(Context context) {
        super(context);
        init();
    }

    public TouchCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        redactDrawPaint = new Paint();
        redactDrawPaint.setColor(Color.BLACK);
        redactDrawPaint.setStyle(Paint.Style.STROKE);
        redactDrawPaint.setStrokeWidth(36f);
        redactDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        redactDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        redactDrawPaint.setAntiAlias(true);

        redactRectPaint = new Paint();
        redactRectPaint.setColor(Color.BLACK);
        redactRectPaint.setStyle(Paint.Style.FILL);
        redactRectPaint.setAntiAlias(true);
    }

    public void setBitmap(Bitmap bitmap) {
        this.currentBitmap = bitmap;
        redactPaths.clear();
        redactRectangles.clear();
        invalidate();
    }

    public Bitmap getBitmap() {
        return currentBitmap;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void clearRedactions() {
        redactPaths.clear();
        redactRectangles.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentBitmap == null) return;

        // Draw the base bitmap scaled inside view bounds
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float bmpWidth = currentBitmap.getWidth();
        float bmpHeight = currentBitmap.getHeight();

        float scale = Math.min(viewWidth / bmpWidth, viewHeight / bmpHeight);
        float dx = (viewWidth - bmpWidth * scale) / 2.0f;
        float dy = (viewHeight - bmpHeight * scale) / 2.0f;

        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);

        canvas.drawBitmap(currentBitmap, 0, 0, imagePaint);

        // Draw recorded freehand redactions
        for (int i = 0; i < redactPaths.size(); i++) {
            canvas.drawPath(redactPaths.get(i), redactDrawPaint);
        }

        // Draw active drawing path
        if (mode == Mode.REDACT_DRAW) {
            canvas.drawPath(currentPath, redactDrawPaint);
        }

        // Draw recorded rectangle redactions
        for (int i = 0; i < redactRectangles.size(); i++) {
            canvas.drawRect(redactRectangles.get(i), redactRectPaint);
        }

        // Draw active rectangle preview
        if (isDrawingRect && mode == Mode.REDACT_RECT) {
            float left = Math.min(startX, currentX);
            float top = Math.min(startY, currentY);
            float right = Math.max(startX, currentX);
            float bottom = Math.max(startY, currentY);
            canvas.drawRect(left, top, right, bottom, redactRectPaint);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mode == Mode.NONE || currentBitmap == null) {
            return super.onTouchEvent(event);
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float bmpWidth = currentBitmap.getWidth();
        float bmpHeight = currentBitmap.getHeight();

        float scale = Math.min(viewWidth / bmpWidth, viewHeight / bmpHeight);
        float dx = (viewWidth - bmpWidth * scale) / 2.0f;
        float dy = (viewHeight - bmpHeight * scale) / 2.0f;

        // Transform touch to bitmap coordinates
        float touchX = (event.getX() - dx) / scale;
        float touchY = (event.getY() - dy) / scale;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mode == Mode.REDACT_DRAW) {
                    currentPath.reset();
                    currentPath.moveTo(touchX, touchY);
                } else if (mode == Mode.REDACT_RECT) {
                    startX = touchX;
                    startY = touchY;
                    currentX = touchX;
                    currentY = touchY;
                    isDrawingRect = true;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mode == Mode.REDACT_DRAW) {
                    currentPath.lineTo(touchX, touchY);
                } else if (mode == Mode.REDACT_RECT) {
                    currentX = touchX;
                    currentY = touchY;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (mode == Mode.REDACT_DRAW) {
                    currentPath.lineTo(touchX, touchY);
                    redactPaths.add(new Path(currentPath));
                    currentPath.reset();
                } else if (mode == Mode.REDACT_RECT) {
                    isDrawingRect = false;
                    float left = Math.min(startX, touchX);
                    float top = Math.min(startY, touchY);
                    float right = Math.max(startX, touchX);
                    float bottom = Math.max(startY, touchY);
                    if (Math.abs(right - left) > 5 && Math.abs(bottom - top) > 5) {
                        redactRectangles.add(new RectF(left, top, right, bottom));
                    }
                }
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    public Bitmap generateComposedBitmap() {
        if (currentBitmap == null) return null;
        Bitmap result = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(currentBitmap, 0, 0, imagePaint);

        for (int i = 0; i < redactPaths.size(); i++) {
            canvas.drawPath(redactPaths.get(i), redactDrawPaint);
        }

        for (int i = 0; i < redactRectangles.size(); i++) {
            canvas.drawRect(redactRectangles.get(i), redactRectPaint);
        }

        return result;
    }
}