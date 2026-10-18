package com.aviatorpro.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class AviatorFlightView extends View {

    public interface FlightListener {
        void onMultiplierUpdate(double currentMultiplier);
        void onCrashed(double finalMultiplier);
    }

    private FlightListener flightListener;
    private Handler flightHandler;
    private Runnable flightLoop;

    private boolean isFlying = false;
    private boolean isCrashed = false;
    private boolean isWaiting = true;

    private double currentMultiplier = 1.00;
    private double crashPoint = 2.00;
    private long flightStartTime = 0;

    // Paints
    private Paint gridPaint;
    private Paint curvePaint;
    private Paint fillPaint;
    private Paint planeBodyPaint;
    private Paint planeWingPaint;
    private Paint planeWindowPaint;
    private Paint multiplierTextPaint;
    private Paint crashedTextPaint;
    private Paint waitTextPaint;

    // Dimensions & Coordinates
    private float viewWidth = 0;
    private float viewHeight = 0;

    private static class Particle {
        float x, y, vx, vy, alpha;
    }
    private ArrayList<Particle> particles = new ArrayList<Particle>();
    private Random random = new Random();

    public AviatorFlightView(Context context) {
        super(context);
        init();
    }

    public AviatorFlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        flightHandler = new Handler(Looper.getMainLooper());

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#1F2430"));
        gridPaint.setStrokeWidth(2f);

        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(Color.parseColor("#E51B38"));
        curvePaint.setStrokeWidth(8f);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        planeBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planeBodyPaint.setColor(Color.parseColor("#E51B38"));

        planeWingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planeWingPaint.setColor(Color.parseColor("#FFFFFF"));

        planeWindowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planeWindowPaint.setColor(Color.parseColor("#FFD54F"));

        multiplierTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        multiplierTextPaint.setColor(Color.WHITE);
        multiplierTextPaint.setTextSize(64f);
        multiplierTextPaint.setTextAlign(Paint.Align.CENTER);
        multiplierTextPaint.setFakeBoldText(true);

        crashedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crashedTextPaint.setColor(Color.parseColor("#E51B38"));
        crashedTextPaint.setTextSize(48f);
        crashedTextPaint.setTextAlign(Paint.Align.CENTER);
        crashedTextPaint.setFakeBoldText(true);

        waitTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waitTextPaint.setColor(Color.parseColor("#9E9EA7"));
        waitTextPaint.setTextSize(36f);
        waitTextPaint.setTextAlign(Paint.Align.CENTER);

        flightLoop = new Runnable() {
            @Override
            public void run() {
                if (isFlying && !isCrashed) {
                    long elapsed = System.currentTimeMillis() - flightStartTime;
                    // Exponential realistic Aviator curve
                    double t = elapsed / 1000.0;
                    currentMultiplier = 1.00 + Math.pow(t * 0.72, 1.45);

                    if (currentMultiplier >= crashPoint) {
                        currentMultiplier = crashPoint;
                        triggerCrash();
                    } else {
                        if (flightListener != null) {
                            flightListener.onMultiplierUpdate(currentMultiplier);
                        }
                        updateParticles();
                        invalidate();
                        flightHandler.postDelayed(this, 30);
                    }
                }
            }
        };
    }

    public void setFlightListener(FlightListener listener) {
        this.flightListener = listener;
    }

    public void startFlight(double targetCrash) {
        this.crashPoint = targetCrash;
        this.currentMultiplier = 1.00;
        this.isFlying = true;
        this.isCrashed = false;
        this.isWaiting = false;
        this.particles.clear();
        this.flightStartTime = System.currentTimeMillis();

        flightHandler.removeCallbacks(flightLoop);
        flightHandler.post(flightLoop);
        invalidate();
    }

    private void triggerCrash() {
        isFlying = false;
        isCrashed = true;
        if (flightListener != null) {
            flightListener.onCrashed(currentMultiplier);
        }
        invalidate();
    }

    public void resetForNextRound() {
        isFlying = false;
        isCrashed = false;
        isWaiting = true;
        currentMultiplier = 1.00;
        flightHandler.removeCallbacks(flightLoop);
        invalidate();
    }

    public boolean isFlying() {
        return isFlying;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    private void updateParticles() {
        if (particles.size() < 20) {
            Particle p = new Particle();
            p.x = getPlaneX();
            p.y = getPlaneY();
            p.vx = -4f - random.nextFloat() * 5f;
            p.vy = random.nextFloat() * 3f - 1.5f;
            p.alpha = 255;
            particles.add(p);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 18;
            if (p.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    private float getPlaneX() {
        float minX = 70;
        float maxX = viewWidth * 0.85f;
        float progress = (float) Math.min((currentMultiplier - 1.0) / 4.0, 1.0);
        return minX + progress * (maxX - minX);
    }

    private float getPlaneY() {
        float bottomY = viewHeight - 60;
        float topY = viewHeight * 0.22f;
        float progress = (float) Math.min((currentMultiplier - 1.0) / 4.0, 1.0);
        return bottomY - progress * (bottomY - topY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (viewWidth == 0 || viewHeight == 0) return;

        // Background
        canvas.drawColor(Color.parseColor("#121418"));

        // Coordinate Grid
        drawGrid(canvas);

        if (isWaiting) {
            canvas.drawText("WAITING FOR NEXT ROUND...", viewWidth / 2f, viewHeight / 2f - 10, waitTextPaint);
            return;
        }

        float originX = 50f;
        float originY = viewHeight - 40f;
        float planeX = getPlaneX();
        float planeY = getPlaneY();

        // Control point for smooth curved climb
        float ctrlX = originX + (planeX - originX) * 0.7f;
        float ctrlY = originY;

        // Under-curve gradient fill
        Path fillPath = new Path();
        fillPath.moveTo(originX, originY);
        fillPath.quadTo(ctrlX, ctrlY, planeX, planeY);
        fillPath.lineTo(planeX, originY);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                originX, planeY, originX, originY,
                Color.parseColor("#66E51B38"),
                Color.parseColor("#05E51B38"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // Curve stroke
        Path curvePath = new Path();
        curvePath.moveTo(originX, originY);
        curvePath.quadTo(ctrlX, ctrlY, planeX, planeY);
        canvas.drawPath(curvePath, curvePaint);

        // Particles / Smoke
        Paint smokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smokePaint.setColor(Color.parseColor("#FFA000"));
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            smokePaint.setAlpha((int) Math.max(0, p.alpha));
            canvas.drawCircle(p.x, p.y, 6f, smokePaint);
        }

        // Aeroplane
        if (!isCrashed) {
            drawAeroplane(canvas, planeX, planeY);
            // Multiplier live text
            multiplierTextPaint.setColor(Color.WHITE);
            String multStr = String.format("%.2fx", currentMultiplier);
            canvas.drawText(multStr, viewWidth / 2f, viewHeight * 0.40f, multiplierTextPaint);
        } else {
            // FLEW AWAY!
            crashedTextPaint.setColor(Color.parseColor("#E51B38"));
            canvas.drawText("FLEW AWAY!", viewWidth / 2f, viewHeight * 0.38f, crashedTextPaint);
            multiplierTextPaint.setColor(Color.parseColor("#E51B38"));
            String finalStr = String.format("%.2fx", crashPoint);
            canvas.drawText(finalStr, viewWidth / 2f, viewHeight * 0.52f, multiplierTextPaint);
        }
    }

    private void drawGrid(Canvas canvas) {
        float bottom = viewHeight - 40f;
        float left = 50f;

        // Base axes
        canvas.drawLine(left, 30f, left, bottom, gridPaint);
        canvas.drawLine(left, bottom, viewWidth - 20f, bottom, gridPaint);

        // Horizontal guides
        float stepY = (bottom - 30f) / 4f;
        for (int i = 1; i <= 3; i++) {
            float y = bottom - i * stepY;
            canvas.drawLine(left, y, viewWidth - 20f, y, gridPaint);
        }
    }

    private void drawAeroplane(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(-18f); // Upward angle of attack

        // Fuselage
        Path body = new Path();
        body.moveTo(25f, 0f);
        body.lineTo(-20f, -8f);
        body.lineTo(-30f, -18f); // Rudder top
        body.lineTo(-25f, 0f);
        body.lineTo(-20f, 6f);
        body.close();
        canvas.drawPath(body, planeBodyPaint);

        // Main Wing
        Path wing = new Path();
        wing.moveTo(5f, -2f);
        wing.lineTo(-12f, -22f);
        wing.lineTo(-5f, -22f);
        wing.lineTo(12f, -2f);
        wing.close();
        canvas.drawPath(wing, planeWingPaint);

        // Cockpit canopy
        canvas.drawCircle(8f, -3f, 4f, planeWindowPaint);

        // Propeller spinner
        planeBodyPaint.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawCircle(26f, 0f, 4f, planeBodyPaint);
        planeBodyPaint.setColor(Color.parseColor("#E51B38"));

        canvas.restore();
    }
}