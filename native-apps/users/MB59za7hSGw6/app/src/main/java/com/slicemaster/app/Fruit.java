package com.slicemaster.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Fruit {

    public enum Type {
        WATERMELON,
        APPLE,
        BANANA,
        ORANGE,
        STRAWBERRY,
        BOMB
    }

    public Type type;
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float radius;
    public float rotation;
    public float vRot;
    public boolean sliced;
    public boolean isOffscreen;

    // Halves physics for sliced fruit
    public float half1X, half1Y, half1Vx, half1Vy, half1Rot, half1Vrot;
    public float half2X, half2Y, half2Vx, half2Vy, half2Rot, half2Vrot;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    public Fruit(Type type, float x, float y, float vx, float vy, float radius) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.rotation = 0;
        this.vRot = (float) ((Math.random() - 0.5) * 360f); // deg/sec
        this.sliced = false;
        this.isOffscreen = false;
    }

    public void update(float dt, int screenHeight) {
        if (!sliced) {
            x += vx * dt;
            y += vy * dt;
            vy += 1250f * dt; // gravity
            rotation += vRot * dt;

            if (y > screenHeight + radius + 100 && vy > 0) {
                isOffscreen = true;
            }
        } else {
            half1X += half1Vx * dt;
            half1Y += half1Vy * dt;
            half1Vy += 1350f * dt;
            half1Rot += half1Vrot * dt;

            half2X += half2Vx * dt;
            half2Y += half2Vy * dt;
            half2Vy += 1350f * dt;
            half2Rot += half2Vrot * dt;

            if (half1Y > screenHeight + 200 && half2Y > screenHeight + 200) {
                isOffscreen = true;
            }
        }
    }

    public void slice(float cutAngle) {
        if (sliced) return;
        sliced = true;

        float separationForce = 350f;
        float perpAngle = cutAngle + 90f;
        float rad = (float) Math.toRadians(perpAngle);

        float nx = (float) Math.cos(rad);
        float ny = (float) Math.sin(rad);

        half1X = x;
        half1Y = y;
        half1Vx = vx - nx * separationForce;
        half1Vy = vy - ny * separationForce - 100f;
        half1Rot = rotation;
        half1Vrot = vRot - 220f;

        half2X = x;
        half2Y = y;
        half2Vx = vx + nx * separationForce;
        half2Vy = vy + ny * separationForce - 100f;
        half2Rot = rotation;
        half2Vrot = vRot + 220f;
    }

    public int getJuiceColor() {
        switch (type) {
            case WATERMELON:
                return Color.parseColor("#FF1744");
            case APPLE:
                return Color.parseColor("#43A047");
            case BANANA:
                return Color.parseColor("#FFEA00");
            case ORANGE:
                return Color.parseColor("#FF9100");
            case STRAWBERRY:
                return Color.parseColor("#E91E63");
            case BOMB:
            default:
                return Color.parseColor("#FF3D00");
        }
    }

    public int getPoints() {
        switch (type) {
            case WATERMELON:
                return 3;
            case APPLE:
                return 2;
            case BANANA:
                return 2;
            case ORANGE:
                return 1;
            case STRAWBERRY:
                return 4;
            case BOMB:
            default:
                return 0;
        }
    }

    public void draw(Canvas canvas) {
        if (!sliced) {
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);
            drawIntactFruit(canvas);
            canvas.restore();
        } else {
            // Draw half 1
            canvas.save();
            canvas.translate(half1X, half1Y);
            canvas.rotate(half1Rot);
            drawHalf(canvas, true);
            canvas.restore();

            // Draw half 2
            canvas.save();
            canvas.translate(half2X, half2Y);
            canvas.rotate(half2Rot);
            drawHalf(canvas, false);
            canvas.restore();
        }
    }

    private void drawIntactFruit(Canvas canvas) {
        switch (type) {
            case WATERMELON:
                drawWatermelon(canvas);
                break;
            case APPLE:
                drawApple(canvas);
                break;
            case BANANA:
                drawBanana(canvas);
                break;
            case ORANGE:
                drawOrange(canvas);
                break;
            case STRAWBERRY:
                drawStrawberry(canvas);
                break;
            case BOMB:
                drawBomb(canvas);
                break;
        }
    }

    private void drawWatermelon(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        // Outer dark green rind
        paint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawCircle(0, 0, radius, paint);

        // Light green stripe pattern
        paint.setColor(Color.parseColor("#4CAF50"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.14f);
        canvas.drawCircle(0, 0, radius * 0.90f, paint);

        // Highlight shine
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawCircle(-radius * 0.35f, -radius * 0.35f, radius * 0.22f, paint);
    }

    private void drawApple(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#D50000"));
        canvas.drawCircle(0, 0, radius, paint);

        // Stem
        paint.setColor(Color.parseColor("#4E342E"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.14f);
        canvas.drawLine(0, -radius * 0.7f, radius * 0.1f, -radius * 1.25f, paint);

        // Leaf
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#64DD17"));
        rectF.set(radius * 0.05f, -radius * 1.35f, radius * 0.6f, -radius * 0.95f);
        canvas.drawOval(rectF, paint);

        // Apple shine
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawCircle(-radius * 0.35f, -radius * 0.35f, radius * 0.25f, paint);
    }

    private void drawBanana(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFD600"));
        rectF.set(-radius * 1.2f, -radius * 0.8f, radius * 1.2f, radius * 0.8f);
        canvas.drawArc(rectF, 190, 160, true, paint);

        // Banana tips
        paint.setColor(Color.parseColor("#6D4C41"));
        canvas.drawCircle(-radius * 0.9f, 0, radius * 0.15f, paint);
        canvas.drawCircle(radius * 0.9f, 0, radius * 0.15f, paint);
    }

    private void drawOrange(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FF6D00"));
        canvas.drawCircle(0, 0, radius, paint);

        // Texture ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#FFA000"));
        paint.setStrokeWidth(radius * 0.08f);
        canvas.drawCircle(0, 0, radius * 0.8f, paint);

        // Highlight
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawCircle(-radius * 0.35f, -radius * 0.35f, radius * 0.25f, paint);
    }

    private void drawStrawberry(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#E53935"));
        rectF.set(-radius, -radius * 0.9f, radius, radius * 1.2f);
        canvas.drawOval(rectF, paint);

        // Little seeds
        paint.setColor(Color.parseColor("#FFEE58"));
        canvas.drawCircle(-radius * 0.3f, -radius * 0.2f, radius * 0.08f, paint);
        canvas.drawCircle(radius * 0.3f, -radius * 0.2f, radius * 0.08f, paint);
        canvas.drawCircle(0, radius * 0.2f, radius * 0.08f, paint);
        canvas.drawCircle(-radius * 0.2f, radius * 0.5f, radius * 0.08f, paint);
        canvas.drawCircle(radius * 0.2f, radius * 0.5f, radius * 0.08f, paint);

        // Green leafy crown
        paint.setColor(Color.parseColor("#2E7D32"));
        canvas.drawCircle(-radius * 0.3f, -radius * 0.8f, radius * 0.2f, paint);
        canvas.drawCircle(0, -radius * 0.9f, radius * 0.25f, paint);
        canvas.drawCircle(radius * 0.3f, -radius * 0.8f, radius * 0.2f, paint);
    }

    private void drawBomb(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        // Black metallic body
        paint.setColor(Color.parseColor("#212121"));
        canvas.drawCircle(0, 0, radius, paint);

        // Glow inner outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#424242"));
        paint.setStrokeWidth(radius * 0.1f);
        canvas.drawCircle(0, 0, radius * 0.9f, paint);

        // Cap & Fuse
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#757575"));
        rectF.set(-radius * 0.3f, -radius * 1.2f, radius * 0.3f, -radius * 0.85f);
        canvas.drawRect(rectF, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#BCAAA4"));
        paint.setStrokeWidth(radius * 0.12f);
        canvas.drawLine(0, -radius * 1.2f, radius * 0.5f, -radius * 1.6f, paint);

        // Spark at fuse tip
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFD600"));
        canvas.drawCircle(radius * 0.5f, -radius * 1.6f, radius * 0.22f, paint);
        paint.setColor(Color.parseColor("#FF3D00"));
        canvas.drawCircle(radius * 0.5f, -radius * 1.6f, radius * 0.12f, paint);

        // Danger skull / cross mark in red
        paint.setColor(Color.parseColor("#FF1744"));
        paint.setStrokeWidth(radius * 0.15f);
        canvas.drawLine(-radius * 0.3f, -radius * 0.3f, radius * 0.3f, radius * 0.3f, paint);
        canvas.drawLine(radius * 0.3f, -radius * 0.3f, -radius * 0.3f, radius * 0.3f, paint);
    }

    private void drawHalf(Canvas canvas, boolean isFirstHalf) {
        // Draw one semicircle half
        rectF.set(-radius, -radius, radius, radius);
        paint.setStyle(Paint.Style.FILL);

        float startAngle = isFirstHalf ? 180f : 0f;

        if (type == Type.WATERMELON) {
            // Rind
            paint.setColor(Color.parseColor("#1B5E20"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
            // Flesh
            rectF.set(-radius * 0.85f, -radius * 0.85f, radius * 0.85f, radius * 0.85f);
            paint.setColor(Color.parseColor("#FF1744"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
            // Seeds
            paint.setColor(Color.parseColor("#212121"));
            float seedOffset = isFirstHalf ? -radius * 0.35f : radius * 0.35f;
            canvas.drawCircle(0, seedOffset, radius * 0.08f, paint);
            canvas.drawCircle(-radius * 0.35f, seedOffset, radius * 0.08f, paint);
            canvas.drawCircle(radius * 0.35f, seedOffset, radius * 0.08f, paint);
        } else if (type == Type.APPLE) {
            // Skin
            paint.setColor(Color.parseColor("#D50000"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
            // Flesh
            rectF.set(-radius * 0.88f, -radius * 0.88f, radius * 0.88f, radius * 0.88f);
            paint.setColor(Color.parseColor("#FFF9C4"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
            // Core
            paint.setColor(Color.parseColor("#5D4037"));
            float coreY = isFirstHalf ? -radius * 0.25f : radius * 0.25f;
            canvas.drawCircle(0, coreY, radius * 0.1f, paint);
        } else if (type == Type.ORANGE) {
            // Peel
            paint.setColor(Color.parseColor("#FF6D00"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
            // Pulpy center
            rectF.set(-radius * 0.82f, -radius * 0.82f, radius * 0.82f, radius * 0.82f);
            paint.setColor(Color.parseColor("#FFA000"));
            canvas.drawArc(rectF, startAngle, 180, true, paint);
        } else {
            // Banana / Strawberry / Bomb debris
            paint.setColor(getJuiceColor());
            canvas.drawArc(rectF, startAngle, 180, true, paint);
        }
    }
}