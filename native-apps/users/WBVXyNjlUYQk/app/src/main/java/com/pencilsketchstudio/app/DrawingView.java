package com.pencilsketchstudio.app;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    
    public static class DrawPath {
        public Path path;
        public int color;
        public int strokeWidth;
        public int opacity; // 0 to 255
        public String toolType; // "HB", "2B", "6B", "COLOR", "ERASER"
        
        public DrawPath(Path path, int color, int strokeWidth, int opacity, String toolType) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.opacity = opacity;
            this.toolType = toolType;
        }
    }

    private List<DrawPath> paths = new ArrayList<>();
    private List<DrawPath> undonePaths = new ArrayList<>();

    private Path currentPath;
    private Paint drawPaint;
    private int currentPaintColor = Color.parseColor("#1C1C1C");
    private int currentStrokeWidth = 12;
    private int currentOpacity = 200; // Default layered graphite setting
    private String currentTool = "2B"; // "HB", "2B", "6B", "COLOR", "ERASER"
    
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;
    private Paint canvasPaint;
    
    private boolean paperTextureEnabled = true;
    private int guideOutline = 0; // 0 = none, 1 = face, 2 = flower, 3 = landscape

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Necessary for BlurMaskFilter and PorterDuff CLEAR on canvas views
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        currentPath = new Path();
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setDither(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(canvasBitmap);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 1. Draw handcrafted paper sketchbook sheet
        if (paperTextureEnabled) {
            drawPaperTexture(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }

        // 2. Draw guidelines
        drawGuideOutline(canvas);

        // 3. Render previous sketches
        for (DrawPath dp : paths) {
            setupPaintForTool(drawPaint, dp.toolType, dp.color, dp.strokeWidth, dp.opacity);
            canvas.drawPath(dp.path, drawPaint);
        }

        // 4. Render current active strokes
        if (currentPath != null && !currentPath.isEmpty()) {
            setupPaintForTool(drawPaint, currentTool, currentPaintColor, currentStrokeWidth, currentOpacity);
            canvas.drawPath(currentPath, drawPaint);
        }
    }

    private void drawPaperTexture(Canvas canvas) {
        // Soft cream/vintage physical sheet color
        canvas.drawColor(Color.parseColor("#FAF6EE"));
        
        // Draft grid spacing & color
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#ECE4D4"));
        gridPaint.setStrokeWidth(1);
        
        int spacing = 70;
        for (int i = 0; i < getWidth(); i += spacing) {
            canvas.drawLine(i, 0, i, getHeight(), gridPaint);
        }
        for (int j = 0; j < getHeight(); j += spacing) {
            canvas.drawLine(0, j, getWidth(), j, gridPaint);
        }
        
        // Simulating wood fiber speckles programmatically
        Paint woodFiberPaint = new Paint();
        woodFiberPaint.setColor(Color.parseColor("#CABFB0"));
        woodFiberPaint.setAlpha(85);
        woodFiberPaint.setStrokeWidth(1);
        
        // Use static seeded random numbers to stop speckles from flickering on redraws
        java.util.Random seedRand = new java.util.Random(5678);
        int speckleAmt = 250;
        for (int i = 0; i < speckleAmt; i++) {
            float rx = seedRand.nextFloat() * getWidth();
            float ry = seedRand.nextFloat() * getHeight();
            float rRad = 1.0f + seedRand.nextFloat() * 1.5f;
            canvas.drawCircle(rx, ry, rRad, woodFiberPaint);
        }
    }

    private void drawGuideOutline(Canvas canvas) {
        if (guideOutline == 0) return;
        
        Paint outlinePaint = new Paint();
        outlinePaint.setColor(Color.parseColor("#B0BEC5")); // Slate draft guidelines
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3);
        outlinePaint.setAntiAlias(true);
        // Realistic dashed carbon sketch marks
        outlinePaint.setPathEffect(new DashPathEffect(new float[]{16, 12}, 0));

        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;

        if (guideOutline == 1) {
            // Human Portrait proportions
            canvas.drawOval(cx - 210, cy - 290, cx + 210, cy + 210, outlinePaint);
            canvas.drawLine(cx - 260, cy - 40, cx + 260, cy - 40, outlinePaint); // Eyes line
            canvas.drawLine(cx, cy - 290, cx, cy + 210, outlinePaint); // Center line
            canvas.drawCircle(cx - 75, cy - 40, 24, outlinePaint);
            canvas.drawCircle(cx + 75, cy - 40, 24, outlinePaint);
            canvas.drawLine(cx - 25, cy + 45, cx + 25, cy + 45, outlinePaint); // Nose
            canvas.drawOval(cx - 65, cy + 115, cx + 65, cy + 135, outlinePaint); // Mouth
        } else if (guideOutline == 2) {
            // Botanical Flower outlines
            canvas.drawCircle(cx, cy, 65, outlinePaint);
            canvas.drawLine(cx, cy + 65, cx, cy + 340, outlinePaint);
            
            Path leafPath = new Path();
            leafPath.moveTo(cx, cy + 170);
            leafPath.quadTo(cx + 130, cy + 110, cx, cy + 250);
            canvas.drawPath(leafPath, outlinePaint);
            
            for (int deg = 0; deg < 360; deg += 45) {
                double rad = Math.toRadians(deg);
                float rx = (float) (cx + Math.cos(rad) * 115);
                float ry = (float) (cy + Math.sin(rad) * 115);
                canvas.drawCircle(rx, ry, 48, outlinePaint);
            }
        } else if (guideOutline == 3) {
            // Mountain Landscape drafts
            canvas.drawLine(0, cy + 100, getWidth(), cy + 100, outlinePaint);
            
            Path peaks = new Path();
            peaks.moveTo(40, cy + 100);
            peaks.lineTo(cx - 90, cy - 130);
            peaks.lineTo(cx + 40, cy + 100);
            peaks.moveTo(cx - 60, cy + 100);
            peaks.lineTo(cx + 140, cy - 190);
            peaks.lineTo(getWidth() - 40, cy + 100);
            canvas.drawPath(peaks, outlinePaint);
            
            canvas.drawCircle(cx + 190, cy - 170, 48, outlinePaint); // Sun
            
            Path pathGuide = new Path();
            pathGuide.moveTo(cx - 40, cy + 100);
            pathGuide.cubicTo(cx - 60, cy + 190, cx + 90, cy + 230, cx - 130, getHeight());
            canvas.drawPath(pathGuide, outlinePaint);
        }
    }

    private void setupPaintForTool(Paint paint, String tool, int color, int strokeWidth, int opacity) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        
        if ("ERASER".equals(tool)) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setStrokeWidth(strokeWidth * 1.6f);
        } else {
            paint.setXfermode(null);
            paint.setColor(color);
            paint.setAlpha(opacity);
            paint.setStrokeWidth(strokeWidth);

            if ("HB".equals(tool)) {
                // HB is hard, precise, light grey graphite
                paint.setStrokeWidth(strokeWidth * 0.55f);
            } else if ("2B".equals(tool)) {
                // Standard medium sketching tool
                paint.setStrokeWidth(strokeWidth * 0.9f);
            } else if ("6B".equals(tool)) {
                // Carbon Charcoal with smooth blurs for smudge painting
                paint.setStrokeWidth(strokeWidth * 1.4f);
                paint.setMaskFilter(new BlurMaskFilter(2.8f, BlurMaskFilter.Blur.NORMAL));
            } else if ("COLOR".equals(tool)) {
                // Colored sketching lead
                paint.setStrokeWidth(strokeWidth * 0.85f);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.moveTo(touchX, touchY);
                undonePaths.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                currentPath.lineTo(touchX, touchY);
                paths.add(new DrawPath(currentPath, currentPaintColor, currentStrokeWidth, currentOpacity, currentTool));
                currentPath = new Path();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void undo() {
        if (paths.size() > 0) {
            DrawPath removed = paths.remove(paths.size() - 1);
            undonePaths.add(removed);
            invalidate();
        }
    }

    public void redo() {
        if (undonePaths.size() > 0) {
            DrawPath restored = undonePaths.remove(undonePaths.size() - 1);
            paths.add(restored);
            invalidate();
        }
    }

    public void clear() {
        paths.clear();
        undonePaths.clear();
        invalidate();
    }

    public void setPaintColor(int color) {
        this.currentPaintColor = color;
    }

    public void setStrokeWidth(int width) {
        this.currentStrokeWidth = width;
    }

    public void setOpacity(int opacity) {
        this.currentOpacity = opacity;
    }

    public void setTool(String tool) {
        this.currentTool = tool;
    }

    public void setPaperTextureEnabled(boolean enabled) {
        this.paperTextureEnabled = enabled;
        invalidate();
    }

    public boolean isPaperTextureEnabled() {
        return paperTextureEnabled;
    }

    public void setGuideOutline(int guideType) {
        this.guideOutline = guideType;
        invalidate();
    }

    public int getGuideOutline() {
        return guideOutline;
    }

    public Bitmap getCanvasBitmap() {
        Bitmap returnedBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        draw(canvas);
        return returnedBitmap;
    }
}