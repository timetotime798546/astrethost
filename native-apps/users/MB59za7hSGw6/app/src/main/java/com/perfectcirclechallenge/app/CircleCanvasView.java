package com.perfectcirclechallenge.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CircleCanvasView extends View {

    public static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface DrawingCallback {
        void onDrawingStarted();
        void onDrawingFinished(List<Point> points);
    }

    private Paint userStrokePaint;
    private Paint glowPaint;
    private Paint guidePaint;
    private Paint evaluatedIdealPaint;
    private Paint centerPointPaint;

    private Path userPath;
    private List<Point> touchPoints;
    private DrawingCallback callback;
    private boolean isDrawingEnabled = true;

    private boolean showEvaluatedCircle = false;
    private float evalCenterX = 0;
    private float evalCenterY = 0;
    private float evalRadius = 0;

    public CircleCanvasView(Context context) {
        super(context);
        init();
    }

    public CircleCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        userPath = new Path();
        touchPoints = new ArrayList<Point>();

        // Paint for user drawing stroke
        userStrokePaint = new Paint();
        userStrokePaint.setColor(Color.parseColor("#22D3EE")); // Cyan Accent
        userStrokePaint.setStyle(Paint.Style.STROKE);
        userStrokePaint.setStrokeWidth(12f);
        userStrokePaint.setAntiAlias(true);
        userStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        userStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        // Glow paint behind user stroke
        glowPaint = new Paint();
        glowPaint.setColor(Color.parseColor("#3306B6D4"));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(26f);
        glowPaint.setAntiAlias(true);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);

        // Guide faint circle in center
        guidePaint = new Paint();
        guidePaint.setColor(Color.parseColor("#1E2E42"));
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(3f);
        guidePaint.setPathEffect(new DashPathEffect(new float[]{16, 16}, 0));
        guidePaint.setAntiAlias(true);

        // Evaluated ideal fitted circle paint
        evaluatedIdealPaint = new Paint();
        evaluatedIdealPaint.setColor(Color.parseColor("#8B5CF6")); // Purple Accent
        evaluatedIdealPaint.setStyle(Paint.Style.STROKE);
        evaluatedIdealPaint.setStrokeWidth(4f);
        evaluatedIdealPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        evaluatedIdealPaint.setAntiAlias(true);

        centerPointPaint = new Paint();
        centerPointPaint.setColor(Color.parseColor("#FACC15")); // Gold center
        centerPointPaint.setStyle(Paint.Style.FILL);
        centerPointPaint.setAntiAlias(true);
    }

    public void setDrawingCallback(DrawingCallback callback) {
        this.callback = callback;
    }

    public void resetCanvas() {
        userPath.reset();
        touchPoints.clear();
        showEvaluatedCircle = false;
        isDrawingEnabled = true;
        invalidate();
    }

    public void showFittedCircle(float cx, float cy, float radius) {
        this.evalCenterX = cx;
        this.evalCenterY = cy;
        this.evalRadius = radius;
        this.showEvaluatedCircle = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float viewCenterX = width / 2.0f;
        float viewCenterY = height / 2.0f;
        float defaultGuideRadius = Math.min(width, height) * 0.36f;

        // Render subtle dotted reference guide circle
        canvas.drawCircle(viewCenterX, viewCenterY, defaultGuideRadius, guidePaint);

        // Render ideal evaluated comparison circle if finished
        if (showEvaluatedCircle && evalRadius > 5) {
            canvas.drawCircle(evalCenterX, evalCenterY, evalRadius, evaluatedIdealPaint);
            canvas.drawCircle(evalCenterX, evalCenterY, 7f, centerPointPaint);
        }

        // Render glowing stroke and user path
        if (!userPath.isEmpty()) {
            canvas.drawPath(userPath, glowPaint);
            canvas.drawPath(userPath, userStrokePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                userPath.reset();
                touchPoints.clear();
                showEvaluatedCircle = false;
                userPath.moveTo(x, y);
                touchPoints.add(new Point(x, y));
                if (callback != null) {
                    callback.onDrawingStarted();
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                userPath.lineTo(x, y);
                touchPoints.add(new Point(x, y));
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDrawingEnabled = false;
                invalidate();
                if (callback != null) {
                    callback.onDrawingFinished(new ArrayList<Point>(touchPoints));
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }
}