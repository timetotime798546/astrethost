package com.history3dexplorer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.Matrix;
import android.graphics.Camera;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.ArrayList;
import java.util.List;

public class History3DCarouselView extends View {

    public interface OnEraChangedListener {
        void onEraChanged(int index, HistoryEra era);
        void onEraSelected(int index, HistoryEra era);
    }

    private List<HistoryEra> eras = new ArrayList<>();
    private float scrollAngle = 0.0f;
    private float initialScrollAngle = 0.0f;
    private float dragStartX = 0.0f;
    private float lastX = 0.0f;
    private boolean isDragging = false;
    private int touchSlop;

    private int lastActiveIndex = -1;
    private OnEraChangedListener listener;

    private final float cameraDistance = 1100.0f;
    private float radiusScale;
    private float depthScale;
    private float baseCardWidth;
    private float baseCardHeight;

    private Camera camera;
    private Matrix matrix;
    private Paint paint;

    private int centerX;
    private int centerY;

    public History3DCarouselView(Context context) {
        super(context);
        init();
    }

    public History3DCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        camera = new Camera();
        matrix = new Matrix();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        
        radiusScale = dpToPx(130);
        depthScale = dpToPx(160);
        baseCardWidth = dpToPx(150);
        baseCardHeight = dpToPx(230);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    public void setEras(List<HistoryEra> eras) {
        this.eras = eras;
        invalidate();
        if (eras.size() > 0) {
            notifyActiveEraChanged();
        }
    }

    public void setOnEraChangedListener(OnEraChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2 - dpToPx(5);
    }

    public int getActiveIndex() {
        if (eras.isEmpty()) return -1;
        float interval = (float) (2 * Math.PI / eras.size());
        int index = Math.round(-scrollAngle / interval) % eras.size();
        if (index < 0) index += eras.size();
        return index;
    }

    private void notifyActiveEraChanged() {
        int active = getActiveIndex();
        if (active != lastActiveIndex && active >= 0 && active < eras.size()) {
            lastActiveIndex = active;
            if (listener != null) {
                listener.onEraChanged(active, eras.get(active));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (eras.isEmpty()) return;

        int numEras = eras.size();
        final float[] zValues = new float[numEras];
        for (int i = 0; i < numEras; i++) {
            float baseAngle = (float) (2 * Math.PI / numEras * i);
            float angle = baseAngle + scrollAngle;
            zValues[i] = (float) (Math.cos(angle) - 1.0f) * depthScale;
        }

        Integer[] drawOrder = getDrawOrder(numEras, zValues);

        for (int i = 0; i < numEras; i++) {
            int idx = drawOrder[i];
            float baseAngle = (float) (2 * Math.PI / numEras * idx);
            float angle = baseAngle + scrollAngle;

            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;

            float z = zValues[idx];
            float x = (float) Math.sin(angle) * radiusScale;

            float scaleFactor = cameraDistance / (cameraDistance - z);
            float px = centerX + x * scaleFactor;
            float py = centerY;

            float cardW = baseCardWidth * scaleFactor;
            float cardH = baseCardHeight * scaleFactor;

            RectF rect = new RectF(px - cardW / 2, py - cardH / 2, px + cardW / 2, py + cardH / 2);

            int alpha = (int) (140 + 115 * (1.0f + z / depthScale));
            if (alpha < 40) alpha = 40;
            if (alpha > 255) alpha = 255;

            float tiltY = (float) Math.toDegrees(angle) * 0.55f;

            canvas.save();
            camera.save();
            camera.rotateY(-tiltY);
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(-rect.centerX(), -rect.centerY());
            matrix.postTranslate(rect.centerX(), rect.centerY());
            canvas.concat(matrix);

            HistoryEra era = eras.get(idx);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(era.color);
            paint.setAlpha(alpha);
            canvas.drawRoundRect(rect, dpToPx(12) * scaleFactor, dpToPx(12) * scaleFactor, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpToPx(idx == getActiveIndex() ? 3 : 1) * scaleFactor);
            paint.setColor(idx == getActiveIndex() ? 0xFFFFD700 : 0xFFFFFFFF);
            paint.setAlpha(alpha);
            canvas.drawRoundRect(rect, dpToPx(12) * scaleFactor, dpToPx(12) * scaleFactor, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            paint.setAlpha(alpha);
            paint.setTextSize(dpToPx(14) * scaleFactor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            canvas.drawText(era.year, rect.centerX(), rect.top + dpToPx(28) * scaleFactor, paint);

            drawEraIcon(canvas, idx, rect.centerX(), rect.centerY() - dpToPx(10) * scaleFactor, dpToPx(48) * scaleFactor, paint, alpha);

            paint.setColor(0xFFFFFFFF);
            paint.setAlpha(alpha);
            paint.setTextSize(dpToPx(13) * scaleFactor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            canvas.drawText(era.title, rect.centerX(), rect.bottom - dpToPx(20) * scaleFactor, paint);

            canvas.restore();
        }
    }

    private void drawEraIcon(Canvas canvas, int index, float cx, float cy, float size, Paint p, int alpha) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dpToPx(2) * (size / dpToPx(48)));
        p.setAlpha(alpha);
        
        switch (index % 7) {
            case 0:
                p.setColor(0xFFFFD700);
                p.setAlpha(alpha);
                Path pyramid = new Path();
                pyramid.moveTo(cx, cy - size / 2);
                pyramid.lineTo(cx - size / 2, cy + size / 2);
                pyramid.lineTo(cx + size / 2, cy + size / 2);
                pyramid.close();
                canvas.drawPath(pyramid, p);
                canvas.drawLine(cx, cy - size / 2, cx, cy + size / 2, p);
                canvas.drawLine(cx - size / 4, cy + size / 6, cx + size / 4, cy + size / 6, p);
                break;
                
            case 1:
                p.setColor(0xFF00BFFF);
                p.setAlpha(alpha);
                canvas.drawLine(cx - size/2, cy + size/2, cx + size/2, cy + size/2, p);
                canvas.drawLine(cx - size/2, cy - size/4, cx + size/2, cy - size/4, p);
                float startX = cx - size/2 + size/8;
                float spacing = size / 4;
                for (int i = 0; i < 4; i++) {
                    canvas.drawLine(startX + i * spacing, cy - size/4, startX + i * spacing, cy + size/2, p);
                }
                Path roof = new Path();
                roof.moveTo(cx - size/2, cy - size/4);
                roof.lineTo(cx, cy - size/2);
                roof.lineTo(cx + size/2, cy - size/4);
                roof.close();
                canvas.drawPath(roof, p);
                break;
                
            case 2:
                p.setColor(0xFFFF2400);
                p.setAlpha(alpha);
                RectF bounds1 = new RectF(cx - size/2, cy - size/4, cx + size/2, cy + size/2);
                canvas.drawOval(bounds1, p);
                RectF bounds2 = new RectF(cx - size/3, cy - size/6, cx + size/3, cy + size/3);
                canvas.drawOval(bounds2, p);
                for (int angle = 0; angle < 360; angle += 45) {
                    float rad = (float) Math.toRadians(angle);
                    float x1 = cx + (float) Math.cos(rad) * (size/3);
                    float y1 = cy + (float) Math.sin(rad) * (size/6) + size/8;
                    float x2 = cx + (float) Math.cos(rad) * (size/2);
                    float y2 = cy + (float) Math.sin(rad) * (size/4) + size/8;
                    canvas.drawLine(x1, y1, x2, y2, p);
                }
                break;
                
            case 3:
                p.setColor(0xFFC0C0C0);
                p.setAlpha(alpha);
                Path castle = new Path();
                float left = cx - size/2;
                float right = cx + size/2;
                float bottom = cy + size/2;
                float top = cy - size/3;
                castle.moveTo(left, bottom);
                castle.lineTo(left, top);
                castle.lineTo(left + size/6, top);
                castle.lineTo(left + size/6, top + size/6);
                castle.lineTo(left + size/3, top + size/6);
                castle.lineTo(left + size/3, top);
                castle.lineTo(cx + size/6, top);
                castle.lineTo(cx + size/6, top + size/6);
                castle.lineTo(cx + size/3, top + size/6);
                castle.lineTo(cx + size/3, top);
                castle.lineTo(right, top);
                castle.lineTo(right, bottom);
                castle.close();
                canvas.drawPath(castle, p);
                canvas.drawRect(cx - size/6, cy + size/6, cx + size/6, bottom, p);
                break;
                
            case 4:
                p.setColor(0xFF228B22);
                p.setAlpha(alpha);
                RectF oval = new RectF(cx - size/2, cy - size/2, cx + size/2, cy + size/2);
                canvas.drawOval(oval, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF1E1E1E);
                canvas.drawCircle(cx - size/4, cy, size/10, p);
                p.setColor(0xFFFF0000);
                canvas.drawCircle(cx + size/4, cy - size/6, size/12, p);
                p.setColor(0xFFFFD700);
                canvas.drawCircle(cx + size/5, cy + size/6, size/12, p);
                p.setColor(0xFF0000FF);
                canvas.drawCircle(cx, cy + size/4, size/12, p);
                break;
                
            case 5:
                p.setColor(0xFFCD7F32);
                p.setAlpha(alpha);
                canvas.drawCircle(cx, cy, size/3, p);
                canvas.drawCircle(cx, cy, size/6, p);
                p.setStyle(Paint.Style.FILL);
                for (int a = 0; a < 360; a += 45) {
                    canvas.save();
                    canvas.rotate(a, cx, cy);
                    canvas.drawRect(cx - size/12, cy - size/2, cx + size/12, cy - size/3, p);
                    canvas.restore();
                }
                break;
                
            case 6:
                p.setColor(0xFFE0E0E0);
                p.setAlpha(alpha);
                Path rocket = new Path();
                rocket.moveTo(cx, cy - size/2);
                rocket.cubicTo(cx + size/4, cy - size/4, cx + size/4, cy + size/4, cx + size/6, cy + size/3);
                rocket.lineTo(cx - size/6, cy + size/3);
                rocket.cubicTo(cx - size/4, cy + size/4, cx - size/4, cy - size/4, cx, cy - size/2);
                canvas.drawPath(rocket, p);
                Path lWing = new Path();
                lWing.moveTo(cx - size/6, cy + size/6);
                lWing.lineTo(cx - size/2, cy + size/3);
                lWing.lineTo(cx - size/6, cy + size/3);
                lWing.close();
                canvas.drawPath(lWing, p);
                Path rWing = new Path();
                rWing.moveTo(cx + size/6, cy + size/6);
                rWing.lineTo(cx + size/2, cy + size/3);
                rWing.lineTo(cx + size/6, cy + size/3);
                rWing.close();
                canvas.drawPath(rWing, p);
                p.setColor(0xFFFF4500);
                p.setAlpha(alpha);
                canvas.drawLine(cx, cy + size/3, cx, cy + size/2, p);
                break;
        }
    }

    private Integer[] getDrawOrder(int numEras, float[] zValues) {
        Integer[] drawOrder = new Integer[numEras];
        for (int i = 0; i < numEras; i++) drawOrder[i] = i;
        
        for (int i = 0; i < numEras - 1; i++) {
            for (int j = i + 1; j < numEras; j++) {
                if (zValues[drawOrder[i]] > zValues[drawOrder[j]]) {
                    int temp = drawOrder[i];
                    drawOrder[i] = drawOrder[j];
                    drawOrder[j] = temp;
                }
            } 
        }
        return drawOrder;
    }

    private int findTappedCard(float tx, float ty) {
        int numEras = eras.size();
        if (numEras == 0) return -1;

        final float[] zValues = new float[numEras];
        for (int i = 0; i < numEras; i++) {
            float baseAngle = (float) (2 * Math.PI / numEras * i);
            float angle = baseAngle + scrollAngle;
            zValues[i] = (float) (Math.cos(angle) - 1.0f) * depthScale;
        }

        Integer[] drawOrder = getDrawOrder(numEras, zValues);

        for (int i = numEras - 1; i >= 0; i--) {
            int idx = drawOrder[i];
            float baseAngle = (float) (2 * Math.PI / numEras * idx);
            float angle = baseAngle + scrollAngle;

            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;

            float z = zValues[idx];
            float x = (float) Math.sin(angle) * radiusScale;

            float scaleFactor = cameraDistance / (cameraDistance - z);
            float px = centerX + x * scaleFactor;
            float py = centerY;

            float cardW = baseCardWidth * scaleFactor;
            float cardH = baseCardHeight * scaleFactor;

            RectF rect = new RectF(px - cardW / 2, py - cardH / 2, px + cardW / 2, py + cardH / 2);
            if (rect.contains(tx, ty)) {
                return idx;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (eras.isEmpty()) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartX = x;
                lastX = x;
                initialScrollAngle = scrollAngle;
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - dragStartX;
                if (Math.abs(dx) > touchSlop) {
                    isDragging = true;
                }
                if (isDragging) {
                    float deltaX = x - lastX;
                    scrollAngle += (deltaX / getWidth()) * (float) Math.PI * 1.6f;
                    lastX = x;
                    invalidate();
                    notifyActiveEraChanged();
                    
                    int active = getActiveIndex();
                    if (active != lastActiveIndex) {
                        SoundSynthesizer.playSound(2);
                        lastActiveIndex = active;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    int tappedIdx = findTappedCard(x, y);
                    if (tappedIdx != -1) {
                        int active = getActiveIndex();
                        if (tappedIdx == active) {
                            if (listener != null) {
                                listener.onEraSelected(tappedIdx, eras.get(tappedIdx));
                            }
                        } else {
                            float targetDelta = getAngleDifferenceToFront(tappedIdx);
                            animateScrollBy(targetDelta);
                        }
                    }
                } else {
                    snapToNearest();
                }
                break;
        }
        return true;
    }

    private float getAngleDifferenceToFront(int index) {
        int numEras = eras.size();
        float currentTargetAngle = - (float) (2 * Math.PI / numEras * index);
        float diff = currentTargetAngle - scrollAngle;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return diff;
    }

    private void snapToNearest() {
        int numEras = eras.size();
        float interval = (float) (2 * Math.PI / numEras);
        int nearestIndex = Math.round(-scrollAngle / interval);
        float targetAngle = -nearestIndex * interval;
        float diff = targetAngle - scrollAngle;
        
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        animateScrollBy(diff);
    } 

    private void animateScrollBy(float angleOffset) {
        final float startAngle = scrollAngle;
        final float target = startAngle + angleOffset;
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(380);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                float f = (Float) animation.getAnimatedValue();
                scrollAngle = startAngle + (target - startAngle) * f;
                invalidate();
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                notifyActiveEraChanged();
                SoundSynthesizer.playSound(2);
            }
        });
        animator.start();
    }
}