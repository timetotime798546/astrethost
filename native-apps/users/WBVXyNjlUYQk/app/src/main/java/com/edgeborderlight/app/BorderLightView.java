package com.edgeborderlight.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.view.View;

public class BorderLightView extends View {

    private Paint paint;
    private RectF bounds;
    private Path path;
    
    private float rotationAngle = 0f;
    private float speed = 5f; // default rotation speed increment
    private int borderWidth = 12; // default stroke width
    private int cornerRadius = 45; // default round corner radius

    // Preset color palettes
    private int[] currentColors = {
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA,
            Color.RED
    };

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            rotationAngle += speed;
            if (rotationAngle >= 360f) {
                rotationAngle -= 360f;
            }
            invalidate();
            postOnAnimation(this);
        }
    };

    public BorderLightView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        bounds = new RectF();
        path = new Path();
    }

    public void updateParams(int width, int animationSpeed, int radius, int colorStyle) {
        this.borderWidth = width;
        this.speed = (float) animationSpeed;
        this.cornerRadius = radius;
        this.currentColors = getColorsForStyle(colorStyle);
        
        paint.setStrokeWidth(this.borderWidth);
        invalidate();
    }

    private int[] getColorsForStyle(int style) {
        switch (style) {
            case 1: // Neon Blue-Purple
                return new int[]{Color.BLUE, Color.MAGENTA, Color.CYAN, Color.BLUE};
            case 2: // Fire Accent
                return new int[]{Color.RED, Color.rgb(255, 69, 0), Color.YELLOW, Color.RED};
            case 3: // Aurora Green-Teal
                return new int[]{Color.GREEN, Color.CYAN, Color.rgb(0, 255, 127), Color.GREEN};
            case 4: // Pastel Pink-Yellow
                return new int[]{Color.rgb(255, 182, 193), Color.rgb(255, 255, 224), Color.rgb(221, 160, 221), Color.rgb(255, 182, 193)};
            case 0: // Classic Rainbow
            default:
                return new int[]{
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.CYAN,
                        Color.BLUE,
                        Color.MAGENTA,
                        Color.RED
                };
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePath(w, h);
    }

    private void updatePath(int w, int h) {
        // Adjust the drawing path offset by half standard stroke border to prevent border cropping
        float halfStroke = borderWidth / 2f;
        bounds.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);

        path.reset();
        path.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Apply a dynamic SweepGradient centered exactly in screen core
        SweepGradient gradient = new SweepGradient(w / 2f, h / 2f, currentColors, null);
        
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle, w / 2f, h / 2f);
        gradient.setLocalMatrix(matrix);

        paint.setShader(gradient);
        paint.setStrokeWidth(borderWidth);

        // Recalculate path in case borders change dynamically
        updatePath(w, h);

        canvas.drawPath(path, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postOnAnimation(animator);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(animator);
        super.onDetachedFromWindow();
    }
}