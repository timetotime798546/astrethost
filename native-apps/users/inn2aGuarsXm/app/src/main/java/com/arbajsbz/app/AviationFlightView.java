package com.arbajsbz.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AviationFlightView extends View {

    private Paint curvePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint planePaint;
    private Paint glowPaint;

    private float progress = 0.0f; // 0.0f to 1.0f
    private boolean isCrashed = false;
    private float currentMultiplier = 1.00f;

    public AviationFlightView(Context context) {
        super(context);
        init();
    }

    public AviationFlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(Color.parseColor("#FF1E27"));
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(8f);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.parseColor("#80FF1E27"));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(16f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#1C2638"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10f, 10f}, 0));

        planePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planePaint.setColor(Color.parseColor("#E0E6ED"));
        planePaint.setStyle(Paint.Style.FILL);
    }

    public void updateFlight(float p, float multiplier, boolean crashed) {
        this.progress = Math.min(1.0f, Math.max(0f, p));
        this.currentMultiplier = multiplier;
        this.isCrashed = crashed;
        invalidate();
    }

    public void reset() {
        this.progress = 0f;
        this.currentMultiplier = 1.00f;
        this.isCrashed = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        // Draw aviation radar grid
        for (int i = 1; i <= 4; i++) {
            float y = (float) height * i / 5.0f;
            canvas.drawLine(40, y, width - 40, y, gridPaint);
        }
        for (int i = 1; i <= 4; i++) {
            float x = (float) width * i / 5.0f;
            canvas.drawLine(x, 40, x, height - 40, gridPaint);
        }

        float startX = 60f;
        float startY = height - 60f;

        float targetX = startX + (width - 140f) * progress;
        // Parabolic rise
        float targetY = startY - (height - 140f) * (float) Math.pow(progress, 1.35);

        if (progress > 0.01f) {
            Path curvePath = new Path();
            curvePath.moveTo(startX, startY);
            curvePath.quadTo(startX + (targetX - startX) * 0.45f, startY, targetX, targetY);

            // Shaded under-curve gradient
            Path areaPath = new Path(curvePath);
            areaPath.lineTo(targetX, startY);
            areaPath.close();

            LinearGradient fillGrad = new LinearGradient(
                    startX, targetY, startX, startY,
                    Color.parseColor("#44E50914"),
                    Color.parseColor("#05E50914"),
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(fillGrad);
            canvas.drawPath(areaPath, fillPaint);

            // Glowing line & main trajectory
            canvas.drawPath(curvePath, glowPaint);
            canvas.drawPath(curvePath, curvePaint);

            // Render Jet Aircraft Silhouette
            drawAviationJet(canvas, targetX, targetY, isCrashed);
        }
    }

    private void drawAviationJet(Canvas canvas, float x, float y, boolean crashed) {
        canvas.save();
        canvas.translate(x, y);

        if (crashed) {
            // Draw crash flash
            Paint crashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            crashPaint.setColor(Color.parseColor("#FF5252"));
            canvas.drawCircle(0, 0, 22f, crashPaint);
            canvas.restore();
            return;
        }

        // Slight dynamic tilt angle matching trajectory curve
        float angle = -18f - (progress * 15f);
        canvas.rotate(angle);

        Path jet = new Path();
        jet.moveTo(24f, 0f);           // Nose tip
        jet.lineTo(0f, -8f);           // Cockpit top
        jet.lineTo(-8f, -22f);         // Left wing tip
        jet.lineTo(-12f, -8f);
        jet.lineTo(-24f, -4f);         // Fuselage rear
        jet.lineTo(-28f, -14f);        // Tail fin
        jet.lineTo(-30f, 0f);          // Exhaust
        jet.lineTo(-28f, 14f);         // Tail fin
        jet.lineTo(-24f, 4f);
        jet.lineTo(-12f, 8f);
        jet.lineTo(-8f, 22f);          // Right wing tip
        jet.lineTo(0f, 8f);
        jet.close();

        planePaint.setColor(Color.WHITE);
        canvas.drawPath(jet, planePaint);

        // Jet engine burn glow
        Paint thruster = new Paint(Paint.ANTI_ALIAS_FLAG);
        thruster.setColor(Color.parseColor("#FF9100"));
        canvas.drawCircle(-32f, 0f, 6f, thruster);

        canvas.restore();
    }
}