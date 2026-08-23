package com.paintmasterstudio.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class DrawingView extends View {

    public static class DrawAction {
        public Path path;
        public int color;
        public float strokeWidth;
        public boolean isEraser;
        public String shapeType;
        public float startX, startY, endX, endY;

        public DrawAction(Path path, int color, float strokeWidth, boolean isEraser, String shapeType, float startX, float startY, float endX, float endY) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.isEraser = isEraser;
            this.shapeType = shapeType;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    private ArrayList<DrawAction> pathsList = new ArrayList<DrawAction>();
    private ArrayList<DrawAction> redoList = new ArrayList<DrawAction>();

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 10f;
    private boolean isEraserActive = false;
    private String currentShapeType = "FREEHAND"; 
    private int bgColor = Color.WHITE;

    private boolean isDrawing = false;
    private float startX, startY;
    private float endX, endY;
    private Path currentPath;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context) {
        super(context);
        init();
    }

    private void init() {
        currentPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            redrawCanvas();
        }
    }

    public void redrawCanvas() {
        if (mBitmap == null) return;
        mBitmap.eraseColor(bgColor);
        for (int i = 0; i < pathsList.size(); i++) {
            drawActionOnCanvas(mCanvas, pathsList.get(i));
        }
        invalidate();
    }

    private void drawActionOnCanvas(Canvas canvas, DrawAction action) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(action.strokeWidth);

        if (action.isEraser) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            paint.setColor(action.color);
        }

        if (action.shapeType.equals("FREEHAND")) {
            canvas.drawPath(action.path, paint);
        } else if (action.shapeType.equals("LINE")) {
            canvas.drawLine(action.startX, action.startY, action.endX, action.endY, paint);
        } else if (action.shapeType.equals("RECT")) {
            canvas.drawRect(
                Math.min(action.startX, action.endX),
                Math.min(action.startY, action.endY),
                Math.max(action.startX, action.endX),
                Math.max(action.startY, action.endY),
                paint
            );
        } else if (action.shapeType.equals("CIRCLE")) {
            float radius = (float) Math.sqrt(Math.pow(action.endX - action.startX, 2) + Math.pow(action.endY - action.startY, 2));
            canvas.drawCircle(action.startX, action.startY, radius, paint);
        }
    }

    private void drawActionPreview(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(currentStrokeWidth);

        if (isEraserActive) {
            paint.setColor(Color.LTGRAY);
        } else {
            paint.setColor(currentColor);
        }

        if (currentShapeType.equals("FREEHAND")) {
            canvas.drawPath(currentPath, paint);
        } else if (currentShapeType.equals("LINE")) {
            canvas.drawLine(startX, startY, endX, endY, paint);
        } else if (currentShapeType.equals("RECT")) {
            canvas.drawRect(
                Math.min(startX, endX),
                Math.min(startY, endY),
                Math.max(startX, endX),
                Math.max(startY, endY),
                paint
            );
        } else if (currentShapeType.equals("CIRCLE")) {
            float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
            canvas.drawCircle(startX, startY, radius, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        }
        if (isDrawing) {
            drawActionPreview(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawing = true;
                startX = x;
                startY = y;
                endX = x;
                endY = y;
                if (currentShapeType.equals("FREEHAND")) {
                    currentPath.reset();
                    currentPath.moveTo(x, y);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                endX = x;
                endY = y;
                if (currentShapeType.equals("FREEHAND")) {
                    currentPath.lineTo(x, y);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                isDrawing = false;
                endX = x;
                endY = y;
                if (currentShapeType.equals("FREEHAND")) {
                    currentPath.lineTo(x, y);
                }

                Path pathToSave = new Path(currentPath);
                DrawAction action = new DrawAction(
                    pathToSave,
                    currentColor,
                    currentStrokeWidth,
                    isEraserActive,
                    currentShapeType,
                    startX,
                    startY,
                    endX,
                    endY
                );
                pathsList.add(action);
                redoList.clear();

                drawActionOnCanvas(mCanvas, action);
                currentPath.reset();
                invalidate();
                break;
        }
        return true;
    }

    public void setBrushColor(int color) {
        this.currentColor = color;
        this.isEraserActive = false;
    }

    public void setStrokeWidth(float width) {
        this.currentStrokeWidth = width;
    }

    public void setEraserActive(boolean active) {
        this.isEraserActive = active;
    }

    public void setShapeType(String type) {
        this.currentShapeType = type;
    }

    public void setBgColor(int color) {
        this.bgColor = color;
        redrawCanvas();
    }

    public int getBgColor() {
        return bgColor;
    }

    public void undo() {
        if (pathsList.size() > 0) {
            DrawAction removed = pathsList.remove(pathsList.size() - 1);
            redoList.add(removed);
            redrawCanvas();
        }
    }

    public void redo() {
        if (redoList.size() > 0) {
            DrawAction restored = redoList.remove(redoList.size() - 1);
            pathsList.add(restored);
            redrawCanvas();
        }
    }

    public void clearCanvas() {
        pathsList.clear();
        redoList.clear();
        redrawCanvas();
    }

    public Bitmap getCanvasBitmap() {
        Bitmap exportBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas exportCanvas = new Canvas(exportBitmap);
        exportCanvas.drawColor(bgColor);
        for (int i = 0; i < pathsList.size(); i++) {
            drawActionOnCanvas(exportCanvas, pathsList.get(i));
        }
        return exportBitmap;
    }
}
