package com.threedclock.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.RectF;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.Calendar;

public class Clock3DView extends View {

    public static final int THEME_GOLD = 0;
    public static final int THEME_NEON = 1;
    public static final int THEME_OBSIDIAN = 2;
    public static final int THEME_STEAMPUNK = 3;

    public static final int MODE_CLOCK = 0;
    public static final int MODE_STOPWATCH = 1;
    public static final int MODE_TIMER = 2;

    private int currentTheme = THEME_GOLD;
    private int currentMode = MODE_CLOCK;

    private float rotationX = 0f;
    private float rotationY = 0f;
    private float targetRotationX = 0f;
    private float targetRotationY = 0f;

    private boolean isSensorEnabled = true;
    private float sensitivity = 1.0f;
    private float depthFactor = 1.0f;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isUserDragging = false;

    private static final float DAMPING = 0.15f;

    private Camera camera;
    private Matrix matrix3d;

    private Paint rimPaint;
    private Paint facePaint;
    private Paint tickPaint;
    private Paint handPaint;
    private Paint shadowPaint;
    private Paint textPaint;
    private Paint subDialPaint;
    private Paint accentPaint;

    private long timerTotalDurationMs = 5 * 60 * 1000;
    private long timerRemainingMs = 5 * 60 * 1000;
    private long timerStartSysTime = 0;
    private boolean isTimerRunning = false;
    private int timerSetMinutes = 5;

    private boolean isStopwatchRunning = false;
    private long stopwatchBaseTime = 0;
    private long stopwatchElapsedMs = 0;

    private ClockCallback callback;

    public interface ClockCallback {
        void onTimerFinished();
        void onTickUpdate(String timeStr, float progress);
    }

    public Clock3DView(Context context) {
        super(context);
        init();
    }

    public Clock3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Clock3DView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        camera = new Camera();
        matrix3d = new Matrix();

        rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subDialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(80);
        shadowPaint.setStyle(Paint.Style.FILL);

        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setCallback(ClockCallback callback) {
        this.callback = callback;
    }

    public void setTheme(int theme) {
        this.currentTheme = theme;
        invalidate();
    }

    public void setMode(int mode) {
        this.currentMode = mode;
        invalidate();
    }

    public int getMode() {
        return currentMode;
    }

    public void setSensorEnabled(boolean enabled) {
        this.isSensorEnabled = enabled;
        if (!enabled) {
            targetRotationX = 0f;
            targetRotationY = 0f;
        }
        invalidate();
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setDepthFactor(float depthFactor) {
        this.depthFactor = depthFactor;
        invalidate();
    }

    public void updateSensorTilt(float ax, float ay) {
        if (isSensorEnabled && !isUserDragging) {
            targetRotationY = -ax * 2.5f * sensitivity;
            targetRotationX = (ay - 6.0f) * 2.5f * sensitivity;

            targetRotationX = Math.max(-35f, Math.min(35f, targetRotationX));
            targetRotationY = Math.max(-35f, Math.min(35f, targetRotationY));

            invalidate();
        }
    }

    public void startStopwatch() {
        if (!isStopwatchRunning) {
            stopwatchBaseTime = SystemClock.elapsedRealtime() - stopwatchElapsedMs;
            isStopwatchRunning = true;
            invalidate();
        }
    }

    public void pauseStopwatch() {
        if (isStopwatchRunning) {
            stopwatchElapsedMs = SystemClock.elapsedRealtime() - stopwatchBaseTime;
            isStopwatchRunning = false;
        }
    }

    public void resetStopwatch() {
        isStopwatchRunning = false;
        stopwatchElapsedMs = 0;
        invalidate();
    }

    public void setTimerMinutes(int minutes) {
        this.timerSetMinutes = minutes;
        if (!isTimerRunning) {
            this.timerTotalDurationMs = minutes * 60 * 1000;
            this.timerRemainingMs = this.timerTotalDurationMs;
            invalidate();
        }
    }

    public void startTimer() {
        if (!isTimerRunning && timerRemainingMs > 0) {
            timerStartSysTime = SystemClock.elapsedRealtime();
            isTimerRunning = true;
            invalidate();
        }
    }

    public void pauseTimer() {
        if (isTimerRunning) {
            timerRemainingMs = Math.max(0, timerRemainingMs - (SystemClock.elapsedRealtime() - timerStartSysTime));
            isTimerRunning = false;
        }
    }

    public void resetTimer() {
        isTimerRunning = false;
        timerRemainingMs = timerSetMinutes * 60 * 1000;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) * 0.43f;

        if (currentMode == MODE_TIMER && !isTimerRunning) {
            float dx = x - centerX;
            float dy = y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist > radius * 0.5f && dist < radius * 1.3f) {
                double angleRad = Math.atan2(dy, dx);
                double angleDeg = Math.toDegrees(angleRad) + 90.0;
                if (angleDeg < 0) {
                    angleDeg += 360.0;
                }

                int minutes = (int) Math.round((angleDeg / 360.0) * 60.0);
                if (minutes <= 0) minutes = 60;
                setTimerMinutes(minutes);

                if (callback != null) {
                    String timeStr = String.format("%02d:00", minutes);
                    callback.onTickUpdate(timeStr, 1.0f);
                }
                return true;
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isUserDragging = true;
                lastTouchX = x;
                lastTouchY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                targetRotationY += (dx / width) * 110f;
                targetRotationX -= (dy / height) * 110f;

                targetRotationX = Math.max(-40f, Math.min(40f, targetRotationX));
                targetRotationY = Math.max(-40f, Math.min(40f, targetRotationY));

                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isUserDragging = false;
                if (!isSensorEnabled) {
                    targetRotationX = 0f;
                    targetRotationY = 0f;
                }
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) * 0.43f;

        rotationX += (targetRotationX - rotationX) * DAMPING;
        rotationY += (targetRotationY - rotationY) * DAMPING;

        if (isTimerRunning) {
            long now = SystemClock.elapsedRealtime();
            long elapsed = now - timerStartSysTime;
            long rem = timerRemainingMs - elapsed;
            if (rem <= 0) {
                rem = 0;
                isTimerRunning = false;
                if (callback != null) {
                    callback.onTimerFinished();
                }
            } else {
                timerRemainingMs = rem;
                timerStartSysTime = now;
            }
            if (callback != null) {
                long totalSecs = rem / 1000;
                long mins = totalSecs / 60;
                long secs = totalSecs % 60;
                String timeStr = String.format("%02d:%02d", mins, secs);
                float progress = timerTotalDurationMs > 0 ? (float) rem / timerTotalDurationMs : 0;
                callback.onTickUpdate(timeStr, progress);
            }
        }

        long stopwatchTimeToShow = stopwatchElapsedMs;
        if (isStopwatchRunning) {
            stopwatchTimeToShow = SystemClock.elapsedRealtime() - stopwatchBaseTime;
            if (callback != null) {
                long totalMills = stopwatchTimeToShow;
                long mins = (totalMills / 60000) % 60;
                long secs = (totalMills / 1000) % 60;
                long mills = (totalMills / 10) % 100;
                String timeStr = String.format("%02d:%02d.%02d", mins, secs, mills);
                callback.onTickUpdate(timeStr, 0f);
            }
        }

        canvas.save();
        camera.save();
        camera.rotateX(rotationX);
        camera.rotateY(rotationY);
        camera.getMatrix(matrix3d);
        camera.restore();

        matrix3d.preTranslate(-centerX, -centerY);
        matrix3d.postTranslate(centerX, centerY);
        canvas.concat(matrix3d);

        int[] rimGradients = getRimGradientColors(currentTheme);
        int faceBgColor = getFaceBgColor(currentTheme);
        int faceGradientCenter = getFaceGradientCenterColor(currentTheme);
        int tickColor = getTickColor(currentTheme);
        int hourHandColor = getHourHandColor(currentTheme);
        int minHandColor = getMinHandColor(currentTheme);
        int secHandColor = getSecHandColor(currentTheme);
        int accentColor = getAccentColor(currentTheme);

        float rimThickness = 14f * depthFactor;
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(rimThickness);
        
        for (int i = (int) rimThickness; i > 0; i -= 2) {
            float shiftX = rotationY * 0.08f * i;
            float shiftY = -rotationX * 0.08f * i;
            rimPaint.setColor(adjustAlpha(rimGradients[2], 255 - (i * 12)));
            canvas.drawCircle(centerX + shiftX, centerY + shiftY, radius + (i / 2f), rimPaint);
        }

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(16f);
        Shader rimShader = new LinearGradient(
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                rimGradients, null, Shader.TileMode.CLAMP);
        rimPaint.setShader(rimShader);
        canvas.drawCircle(centerX, centerY, radius, rimPaint);
        rimPaint.setShader(null);

        facePaint.setStyle(Paint.Style.FILL);
        Shader faceShader = new RadialGradient(
                centerX, centerY, radius * 1.05f,
                faceGradientCenter, faceBgColor, Shader.TileMode.CLAMP);
        facePaint.setShader(faceShader);
        canvas.drawCircle(centerX, centerY, radius - 8f, facePaint);
        facePaint.setShader(null);

        drawThemeDecorations(canvas, centerX, centerY, radius, accentColor);

        if (currentMode == MODE_STOPWATCH) {
            drawStopwatchSubDials(canvas, centerX, centerY, radius, tickColor, accentColor, stopwatchTimeToShow);
        } else if (currentMode == MODE_TIMER) {
            drawTimerSector(canvas, centerX, centerY, radius, accentColor);
        }

        drawClockTicks(canvas, centerX, centerY, radius, tickColor, currentTheme);

        Calendar cal = Calendar.getInstance();
        float hours = cal.get(Calendar.HOUR);
        float minutes = cal.get(Calendar.MINUTE);
        float seconds = cal.get(Calendar.SECOND);
        float milliseconds = cal.get(Calendar.MILLISECOND);

        float mAngle = (minutes + seconds / 60f) * 6f;
        float hAngle = (hours + minutes / 60f) * 30f;
        float sAngle = (seconds + milliseconds / 1000f) * 6f;

        float shadowDX = 12f + (rotationY * 0.35f);
        float shadowDY = 12f - (rotationX * 0.35f);

        drawHandWithShadow(canvas, centerX, centerY, hAngle, radius * 0.5f, 16f, hourHandColor, shadowDX, shadowDY, false);
        drawHandWithShadow(canvas, centerX, centerY, mAngle, radius * 0.72f, 10f, minHandColor, shadowDX, shadowDY, false);

        if (currentMode == MODE_CLOCK || currentMode == MODE_STOPWATCH) {
            drawHandWithShadow(canvas, centerX, centerY, sAngle, radius * 0.82f, 4f, secHandColor, shadowDX, shadowDY, true);
        }

        accentPaint.setStyle(Paint.Style.FILL);
        accentPaint.setColor(accentColor);
        shadowPaint.setAlpha(120);
        canvas.drawCircle(centerX + shadowDX * 0.4f, centerY + shadowDY * 0.4f, 16f, shadowPaint);
        canvas.drawCircle(centerX, centerY, 14f, accentPaint);
        accentPaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX - 3f, centerY - 3f, 4f, accentPaint);

        drawGlassShine(canvas, centerX, centerY, radius);

        canvas.restore();

        if (isStopwatchRunning || isTimerRunning || currentMode == MODE_CLOCK || Math.abs(targetRotationX - rotationX) > 0.01f || Math.abs(targetRotationY - rotationY) > 0.01f) {
            postInvalidateOnAnimation();
        }
    }

    private void drawHandWithShadow(Canvas canvas, float cx, float cy, float rotation, float length, float strokeWidth, int color, float shadowDX, float shadowDY, boolean isSecondHand) {
        canvas.save();
        canvas.rotate(rotation, cx, cy);

        shadowPaint.setStrokeWidth(strokeWidth);
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);
        shadowPaint.setAlpha(60);
        canvas.drawLine(cx + shadowDX, cy + shadowDY, cx + shadowDX, cy + shadowDY - length, shadowPaint);
        if (isSecondHand) {
            canvas.drawLine(cx + shadowDX, cy + shadowDY, cx + shadowDX, cy + shadowDY + (length * 0.2f), shadowPaint);
        }

        handPaint.setStrokeWidth(strokeWidth);
        handPaint.setColor(color);
        handPaint.setStrokeCap(Paint.Cap.ROUND);

        if (isSecondHand) {
            handPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawLine(cx, cy, cx, cy - length, handPaint);
            canvas.drawLine(cx, cy, cx, cy + (length * 0.25f), handPaint);
            canvas.drawCircle(cx, cy - (length * 0.65f), strokeWidth * 2f, handPaint);
        } else {
            Path handPath = new Path();
            handPath.moveTo(cx - strokeWidth * 0.6f, cy);
            handPath.lineTo(cx - strokeWidth * 0.3f, cy - length * 0.85f);
            handPath.lineTo(cx, cy - length);
            handPath.lineTo(cx + strokeWidth * 0.3f, cy - length * 0.85f);
            handPath.lineTo(cx + strokeWidth * 0.6f, cy);
            handPath.close();

            handPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(handPath, handPaint);

            handPaint.setStyle(Paint.Style.STROKE);
            handPaint.setStrokeWidth(1.5f);
            handPaint.setColor(adjustAlpha(Color.WHITE, 160));
            canvas.drawLine(cx, cy, cx, cy - length + 3f, handPaint);
        }

        canvas.restore();
    }

    private void drawClockTicks(Canvas canvas, float cx, float cy, float radius, int tickColor, int theme) {
        tickPaint.setColor(tickColor);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        float outerDial = radius - 15f;
        float innerDialHour = radius - 40f;
        float innerDialMin = radius - 28f;

        textPaint.setColor(tickColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        for (int i = 0; i < 60; i++) {
            boolean isHour = (i % 5 == 0);
            float angle = i * 6f;
            canvas.save();
            canvas.rotate(angle, cx, cy);

            if (isHour) {
                tickPaint.setStrokeWidth(6f);
                canvas.drawLine(cx, cy - outerDial, cx, cy - innerDialHour, tickPaint);

                int hourValue = (i == 0) ? 12 : i / 5;
                textPaint.setTextSize(radius * 0.15f);

                String numberStr = String.valueOf(hourValue);
                if (theme == THEME_STEAMPUNK) {
                    numberStr = getRomanNumerals(hourValue);
                    textPaint.setTextSize(radius * 0.12f);
                }

                canvas.drawText(numberStr, cx, cy - innerDialHour + (radius * 0.12f), textPaint);
            } else {
                tickPaint.setStrokeWidth(3f);
                canvas.drawLine(cx, cy - outerDial, cx, cy - innerDialMin, tickPaint);
            }
            canvas.restore();
        }
    }

    private void drawThemeDecorations(Canvas canvas, float cx, float cy, float radius, int accentColor) {
        accentPaint.setAntiAlias(true);

        if (currentTheme == THEME_STEAMPUNK) {
            accentPaint.setStyle(Paint.Style.STROKE);
            accentPaint.setStrokeWidth(4f);
            accentPaint.setColor(adjustAlpha(accentColor, 40));

            canvas.drawCircle(cx, cy, radius * 0.65f, accentPaint);
            canvas.drawCircle(cx, cy, radius * 0.45f, accentPaint);

            canvas.save();
            float gearRotation = (SystemClock.elapsedRealtime() / 120f) % 360f;
            canvas.rotate(gearRotation, cx, cy);
            drawGearAesthetic(canvas, cx, cy, radius * 0.3f, 12, accentColor);
            canvas.restore();

        } else if (currentTheme == THEME_NEON) {
            accentPaint.setStyle(Paint.Style.STROKE);
            accentPaint.setStrokeWidth(2f);
            accentPaint.setColor(adjustAlpha(accentColor, 50));
            canvas.drawCircle(cx, cy, radius * 0.7f, accentPaint);

            float chLen = radius * 0.08f;
            canvas.drawLine(cx - radius * 0.7f - chLen, cy, cx - radius * 0.7f + chLen, cy, accentPaint);
            canvas.drawLine(cx + radius * 0.7f - chLen, cy, cx + radius * 0.7f + chLen, cy, accentPaint);
            canvas.drawLine(cx, cy - radius * 0.7f - chLen, cx, cy - radius * 0.7f + chLen, accentPaint);
            canvas.drawLine(cx, cy + radius * 0.7f - chLen, cx, cy + radius * 0.7f + chLen, accentPaint);
        } else if (currentTheme == THEME_GOLD) {
            accentPaint.setStyle(Paint.Style.STROKE);
            accentPaint.setStrokeWidth(2f);
            accentPaint.setColor(adjustAlpha(accentColor, 70));
            canvas.drawCircle(cx, cy, radius * 0.82f, accentPaint);
        }
    }

    private void drawGearAesthetic(Canvas canvas, float cx, float cy, float gearRadius, int teeth, int color) {
        accentPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        accentPaint.setColor(adjustAlpha(color, 25));
        canvas.drawCircle(cx, cy, gearRadius * 0.8f, accentPaint);

        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(4f);
        accentPaint.setColor(adjustAlpha(color, 45));

        for (int i = 0; i < teeth; i++) {
            canvas.save();
            canvas.rotate(i * (360f / teeth), cx, cy);
            RectF tooth = new RectF(cx - 8f, cy - gearRadius - 6f, cx + 8f, cy - gearRadius + 4f);
            canvas.drawRoundRect(tooth, 3f, 3f, accentPaint);
            canvas.restore();
        }
    }

    private void drawStopwatchSubDials(Canvas canvas, float cx, float cy, float radius, int tickColor, int accentColor, long elapsedMs) {
        float subDialY = cy + (radius * 0.45f);
        float subDialRadius = radius * 0.28f;

        subDialPaint.setStyle(Paint.Style.STROKE);
        subDialPaint.setStrokeWidth(3f);
        subDialPaint.setColor(adjustAlpha(tickColor, 100));
        canvas.drawCircle(cx, subDialY, subDialRadius, subDialPaint);

        float msAngle = ((elapsedMs % 1000) / 1000f) * 360f;

        subDialPaint.setColor(adjustAlpha(tickColor, 130));
        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90f, cx, subDialY);
            canvas.drawLine(cx, subDialY - subDialRadius, cx, subDialY - subDialRadius + 8f, subDialPaint);
            canvas.restore();
        }

        canvas.save();
        canvas.rotate(msAngle, cx, subDialY);
        subDialPaint.setColor(accentColor);
        subDialPaint.setStrokeWidth(4f);
        subDialPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        Path pointer = new Path();
        pointer.moveTo(cx - 4f, subDialY);
        pointer.lineTo(cx, subDialY - subDialRadius + 5f);
        pointer.lineTo(cx + 4f, subDialY);
        pointer.close();
        canvas.drawPath(pointer, subDialPaint);
        canvas.restore();

        subDialPaint.setColor(tickColor);
        canvas.drawCircle(cx, subDialY, 6f, subDialPaint);
    }

    private void drawTimerSector(Canvas canvas, float cx, float cy, float radius, int accentColor) {
        float sweepRadius = radius - 20f;
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(12f);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        accentPaint.setColor(adjustAlpha(accentColor, 180));

        float percentage = 0f;
        if (isTimerRunning) {
            long elapsed = SystemClock.elapsedRealtime() - timerStartSysTime;
            long rem = timerRemainingMs - elapsed;
            if (timerTotalDurationMs > 0) {
                percentage = (float) rem / timerTotalDurationMs;
            }
        } else {
            percentage = timerTotalDurationMs > 0 ? (float) timerRemainingMs / timerTotalDurationMs : 1.0f;
        }

        float sweepAngle = percentage * 360f;
        RectF arcBounds = new RectF(cx - sweepRadius, cy - sweepRadius, cx + sweepRadius, cy + sweepRadius);
        canvas.drawArc(arcBounds, -90f, sweepAngle, false, accentPaint);
    }

    private void drawGlassShine(Canvas canvas, float cx, float cy, float radius) {
        float shineOffset = (rotationY * 0.4f) + (rotationX * 0.2f);
        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setStyle(Paint.Style.FILL);

        LinearGradient shineGradient = new LinearGradient(
                cx - radius + shineOffset, cy - radius - shineOffset,
                cx + radius + shineOffset, cy + radius - shineOffset,
                new int[]{
                        adjustAlpha(Color.WHITE, 90),
                        adjustAlpha(Color.WHITE, 15),
                        adjustAlpha(Color.WHITE, 0),
                        adjustAlpha(Color.WHITE, 0),
                        adjustAlpha(Color.WHITE, 40),
                },
                new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP);

        shinePaint.setShader(shineGradient);
        canvas.drawCircle(cx, cy, radius - 8f, shinePaint);
    }

    private int adjustAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private String getRomanNumerals(int num) {
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            case 11: return "XI";
            case 12: return "XII";
            default: return "";
        }
    }

    private int[] getRimGradientColors(int theme) {
        switch (theme) {
            case THEME_NEON:
                return new int[]{Color.parseColor("#00F0FF"), Color.parseColor("#1B1E30"), Color.parseColor("#FF007F")};
            case THEME_OBSIDIAN:
                return new int[]{Color.parseColor("#3a3a3a"), Color.parseColor("#1a1a1a"), Color.parseColor("#121212")};
            case THEME_STEAMPUNK:
                return new int[]{Color.parseColor("#D48D47"), Color.parseColor("#3E2723"), Color.parseColor("#8D6E63")};
            case THEME_GOLD:
            default:
                return new int[]{Color.parseColor("#FFE082"), Color.parseColor("#423212"), Color.parseColor("#FFB300")};
        }
    }

    private int getFaceBgColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#050510");
            case THEME_OBSIDIAN: return Color.parseColor("#121212");
            case THEME_STEAMPUNK: return Color.parseColor("#5D4037");
            case THEME_GOLD:
            default:
                return Color.parseColor("#151515");
        }
    }

    private int getFaceGradientCenterColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#0F0F28");
            case THEME_OBSIDIAN: return Color.parseColor("#252525");
            case THEME_STEAMPUNK: return Color.parseColor("#8D6E63");
            case THEME_GOLD:
            default:
                return Color.parseColor("#322A1E");
        }
    }

    private int getTickColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#00F0FF");
            case THEME_OBSIDIAN: return Color.parseColor("#E0E0E0");
            case THEME_STEAMPUNK: return Color.parseColor("#FFE0B2");
            case THEME_GOLD:
            default:
                return Color.parseColor("#FFE082");
        }
    }

    private int getHourHandColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#00F0FF");
            case THEME_OBSIDIAN: return Color.parseColor("#FFFFFF");
            case THEME_STEAMPUNK: return Color.parseColor("#FFCC80");
            case THEME_GOLD:
            default:
                return Color.parseColor("#FFD54F");
        }
    }

    private int getMinHandColor(int theme) {
        return getHourHandColor(theme);
    }

    private int getSecHandColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#FF007F");
            case THEME_OBSIDIAN: return Color.parseColor("#FF5722");
            case THEME_STEAMPUNK: return Color.parseColor("#FFD54F");
            case THEME_GOLD:
            default:
                return Color.parseColor("#FF3D00");
        }
    }

    private int getAccentColor(int theme) {
        switch (theme) {
            case THEME_NEON: return Color.parseColor("#FF007F");
            case THEME_OBSIDIAN: return Color.parseColor("#757575");
            case THEME_STEAMPUNK: return Color.parseColor("#A1887F");
            case THEME_GOLD:
            default:
                return Color.parseColor("#FFB300");
        }
    }
}