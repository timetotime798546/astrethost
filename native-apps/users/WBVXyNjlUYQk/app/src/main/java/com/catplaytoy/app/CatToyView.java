package com.catplaytoy.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CatToyView extends View {
    public static final int MODE_LASER = 0;
    public static final int MODE_FISH = 1;

    private int mode = MODE_LASER;
    private Paint paintLaser;
    private Paint paintFish;

    private float laserX, laserY;
    private float laserRadius = 50f;
    private float laserSpeedX = 14f;
    private float laserSpeedY = 17f;

    private float fishX, fishY;
    private float fishAngle = 0;
    private float fishSpeed = 9f;
    private float fishTargetX, fishTargetY;

    private float rippleX, rippleY;
    private float rippleRadius = 0;
    private boolean showRipple = false;

    private OnToyTouchListener listener;

    public interface OnToyTouchListener {
        void onToyCaught(String type);
    }

    public CatToyView(Context context) {
        super(context);
        init();
    }

    public CatToyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLaser = new Paint();
        paintLaser.setColor(Color.RED);
        paintLaser.setAntiAlias(true);

        paintFish = new Paint();
        paintFish.setColor(0xFFFF8C00); // Orange
        paintFish.setAntiAlias(true);

        post(animator);
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode == MODE_LASER) {
            laserX = getWidth() / 2f;
            laserY = getHeight() / 2f;
        } else {
            fishX = getWidth() / 2f;
            fishY = getHeight() / 2f;
            fishTargetX = fishX;
            fishTargetY = fishY;
        }
        invalidate();
    }

    public void setOnToyTouchListener(OnToyTouchListener listener) {
        this.listener = listener;
    }

    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            updatePhysics();
            invalidate();
            postDelayed(this, 16);
        }
    };

    private void updatePhysics() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        if (mode == MODE_LASER) {
            laserX += laserSpeedX;
            laserY += laserSpeedY;

            if (laserX - laserRadius < 0) {
                laserX = laserRadius;
                laserSpeedX = -laserSpeedX + (float) (Math.random() * 4 - 2);
            } else if (laserX + laserRadius > width) {
                laserX = width - laserRadius;
                laserSpeedX = -laserSpeedX + (float) (Math.random() * 4 - 2);
            }

            if (laserY - laserRadius < 0) {
                laserY = laserRadius;
                laserSpeedY = -laserSpeedY + (float) (Math.random() * 4 - 2);
            } else if (laserY + laserRadius > height) {
                laserY = height - laserRadius;
                laserSpeedY = -laserSpeedY + (float) (Math.random() * 4 - 2);
            }

            if (Math.abs(laserSpeedX) < 6) laserSpeedX = laserSpeedX > 0 ? 9 : -9;
            if (Math.abs(laserSpeedY) < 6) laserSpeedY = laserSpeedY > 0 ? 9 : -9;
            if (Math.abs(laserSpeedX) > 28) laserSpeedX = laserSpeedX > 0 ? 22 : -22;
            if (Math.abs(laserSpeedY) > 28) laserSpeedY = laserSpeedY > 0 ? 22 : -22;

        } else if (mode == MODE_FISH) {
            float dx = fishTargetX - fishX;
            float dy = fishTargetY - fishY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < 60 || Math.random() < 0.015) {
                fishTargetX = (float) (Math.random() * (width - 120)) + 60;
                fishTargetY = (float) (Math.random() * (height - 120)) + 60;
            }

            if (dist > 0) {
                float stepX = (dx / dist) * fishSpeed;
                float stepY = (dy / dist) * fishSpeed;
                fishX += stepX;
                fishY += stepY;
                fishAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
            }
        }

        if (showRipple) {
            rippleRadius += 9f;
            if (rippleRadius > 160) {
                showRipple = false;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mode == MODE_LASER) {
            canvas.drawColor(0xFF111111);

            paintLaser.setAlpha(60);
            canvas.drawCircle(laserX, laserY, laserRadius * 2.2f, paintLaser);

            paintLaser.setAlpha(180);
            paintLaser.setColor(Color.RED);
            canvas.drawCircle(laserX, laserY, laserRadius, paintLaser);

            paintLaser.setColor(Color.WHITE);
            paintLaser.setAlpha(255);
            canvas.drawCircle(laserX, laserY, laserRadius * 0.35f, paintLaser);
            paintLaser.setColor(Color.RED);
        } else if (mode == MODE_FISH) {
            canvas.drawColor(0xFF003F5C);

            if (showRipple) {
                Paint ripplePaint = new Paint();
                ripplePaint.setColor(0x99FFFFFF);
                ripplePaint.setStyle(Paint.Style.STROKE);
                ripplePaint.setStrokeWidth(5);
                ripplePaint.setAntiAlias(true);
                canvas.drawCircle(rippleX, rippleY, rippleRadius, ripplePaint);
            }

            canvas.save();
            canvas.translate(fishX, fishY);
            canvas.rotate(fishAngle);

            Path fishPath = new Path();
            fishPath.moveTo(45, 0);
            fishPath.quadTo(12, -22, -28, -6);
            fishPath.lineTo(-46, -18);
            fishPath.lineTo(-38, 0);
            fishPath.lineTo(-46, 18);
            fishPath.lineTo(-28, 6);
            fishPath.quadTo(12, 22, 45, 0);
            fishPath.close();

            canvas.drawPath(fishPath, paintFish);

            Paint eyePaint = new Paint();
            eyePaint.setColor(Color.BLACK);
            eyePaint.setAntiAlias(true);
            canvas.drawCircle(22, -5, 4, eyePaint);

            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            if (mode == MODE_LASER) {
                float dx = tx - laserX;
                float dy = ty - laserY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < laserRadius * 2.4f) {
                    laserX = (float) (Math.random() * (getWidth() - 120)) + 60;
                    laserY = (float) (Math.random() * (getHeight() - 120)) + 60;
                    laserSpeedX = (float) (Math.random() * 24 - 12);
                    laserSpeedY = (float) (Math.random() * 24 - 12);

                    if (listener != null) {
                        listener.onToyCaught("laser");
                    }
                }
            } else if (mode == MODE_FISH) {
                float dx = tx - fishX;
                float dy = ty - fishY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                rippleX = tx;
                rippleY = ty;
                rippleRadius = 0;
                showRipple = true;

                if (dist < 110f) {
                    fishX = (float) (Math.random() * (getWidth() - 120)) + 60;
                    fishY = (float) (Math.random() * (getHeight() - 120)) + 60;
                    fishTargetX = fishX;
                    fishTargetY = fishY;

                    if (listener != null) {
                        listener.onToyCaught("fish");
                    }
                }
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}