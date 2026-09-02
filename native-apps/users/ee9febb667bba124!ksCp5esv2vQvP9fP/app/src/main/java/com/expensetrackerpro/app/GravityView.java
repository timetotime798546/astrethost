package com.expensetrackerpro.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GravityView extends View {

    private static class PhysicsBall {
        float x, y;
        float vx, vy;
        float radius;
        int colorStart;
        int colorEnd;
        String name;
        double amount;
        boolean isExpense;
    }

    private final List<PhysicsBall> balls = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private boolean isRunning = false;
    private long lastTime = 0;
    private SoundHelper soundHelper;

    // Physics parameters
    private static final float GRAVITY = 0.5f;
    private static final float BOUNCE = -0.65f;
    private static final float FRICTION = 0.99f;

    public GravityView(Context context) {
        super(context);
        init();
    }

    public GravityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setSoundHelper(SoundHelper soundHelper) {
        this.soundHelper = soundHelper;
    }

    public void setupTransactions(List<Transaction> transactions) {
        balls.clear();
        if (transactions == null || transactions.isEmpty()) {
            addBall("Sample", 50.00, true);
            addBall("Bonus", 150.00, false);
            addBall("Gift", 30.00, false);
        } else {
            int count = Math.min(transactions.size(), 12);
            for (int i = 0; i < count; i++) {
                Transaction tx = transactions.get(i);
                addBall(tx.getCategory(), tx.getAmount(), tx.getType().equals("EXPENSE"));
            }
        }
        triggerSimulation();
    }

    private void addBall(String name, double amount, boolean isExpense) {
        PhysicsBall ball = new PhysicsBall();
        ball.name = name;
        ball.amount = amount;
        ball.isExpense = isExpense;
        ball.radius = (float) (45 + Math.min(amount / 5, 40));
        ball.x = 100 + random.nextInt(400);
        ball.y = 50 + random.nextInt(100);
        ball.vx = (random.nextFloat() - 0.5f) * 12f;
        ball.vy = (random.nextFloat() - 0.5f) * 12f;

        if (isExpense) {
            ball.colorStart = Color.parseColor("#FF5252");
            ball.colorEnd = Color.parseColor("#B71C1C");
        } else {
            ball.colorStart = Color.parseColor("#69F0AE");
            ball.colorEnd = Color.parseColor("#1B5E20");
        }
        balls.add(ball);
    }

    private void triggerSimulation() {
        if (!isRunning) {
            isRunning = true;
            lastTime = System.currentTimeMillis();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 16.0f;
        if (dt > 2.0f) dt = 2.0f;
        lastTime = now;

        int width = getWidth();
        int height = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(Color.parseColor("#121321"));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.parseColor("#44FFFFFF"));
        canvas.drawText("⚡ Interactive 3D Gravity Space - Tap Balls to Toss!", width / 2f, 40, paint);

        for (int i = 0; i < balls.size(); i++) {
            PhysicsBall b = balls.get(i);

            b.vy += GRAVITY * dt;
            b.vx *= FRICTION;
            b.vy *= FRICTION;

            b.x += b.vx * dt;
            b.y += b.vy * dt;

            boolean bounced = false;

            if (b.x < b.radius) {
                b.x = b.radius;
                b.vx = b.vx * BOUNCE;
                bounced = true;
            }
            if (b.x > width - b.radius) {
                b.x = width - b.radius;
                b.vx = b.vx * BOUNCE;
                bounced = true;
            }
            if (b.y < b.radius) {
                b.y = b.radius;
                b.vy = b.vy * BOUNCE;
                bounced = true;
            }
            if (b.y > height - b.radius) {
                b.y = height - b.radius;
                b.vy = b.vy * BOUNCE;
                bounced = true;
            }

            for (int j = i + 1; j < balls.size(); j++) {
                PhysicsBall other = balls.get(j);
                float dx = other.x - b.x;
                float dy = other.y - b.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float minDist = b.radius + other.radius;

                if (dist < minDist) {
                    float overlap = minDist - dist;
                    float nx = dx / dist;
                    float ny = dy / dist;

                    b.x -= nx * overlap * 0.5f;
                    b.y -= ny * overlap * 0.5f;
                    other.x += nx * overlap * 0.5f;
                    other.y += ny * overlap * 0.5f;

                    float kx = b.vx - other.vx;
                    float ky = b.vy - other.vy;
                    float p = nx * kx + ny * ky;

                    b.vx -= p * nx;
                    b.vy -= p * ny;
                    other.vx += p * nx;
                    other.vy += p * ny;

                    bounced = true;
                }
            }

            if (bounced && soundHelper != null && Math.abs(b.vx) + Math.abs(b.vy) > 2.5f) {
                soundHelper.playPhysicsTick();
            }

            RadialGradient shader = new RadialGradient(
                    b.x - b.radius * 0.3f, b.y - b.radius * 0.3f,
                    b.radius * 1.3f,
                    b.colorStart,
                    b.colorEnd,
                    Shader.TileMode.CLAMP
            );
            paint.setShader(shader);
            canvas.drawCircle(b.x, b.y, b.radius, paint);

            canvas.drawText(b.name, b.x, b.y - 2, textPaint);
            canvas.drawText(String.format("$%.0f", b.amount), b.x, b.y + 22, textPaint);
        }

        if (isRunning) {
            postInvalidateDelayed(16);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            for (PhysicsBall b : balls) {
                float dx = b.x - tx;
                float dy = b.y - ty;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= b.radius) {
                    b.vx = (random.nextFloat() - 0.5f) * 35f;
                    b.vy = -18f - random.nextFloat() * 15f;
                    if (soundHelper != null) {
                        soundHelper.playTossSound();
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}